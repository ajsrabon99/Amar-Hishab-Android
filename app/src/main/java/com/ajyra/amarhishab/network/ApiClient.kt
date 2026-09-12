package com.ajyra.amarhishab.network

import android.content.Context
import android.util.Log
import com.ajyra.amarhishab.data.local.EncryptedSessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class AuthExchangeResult(
    val sessionId: String?,
    val token: String?,
    val userDto: UserDto? = null
)

typealias GoogleAuthResult = AuthExchangeResult

object ApiClient {
    const val BASE_URL = "https://amar-hisab.onrender.com/api/v1/"
    private const val EXCHANGE_ENDPOINT = "https://amar-hisab.onrender.com/api/v1/auth/exchange/"
    private const val EXCHANGE_FALLBACK_ENDPOINT = "https://amar-hisab.onrender.com/accounts/app/exchange/"
    private const val DJANGO_ALLAUTH_URL = "https://amar-hisab.onrender.com/accounts/google/login/token/"
    private const val TAG = "ApiClient"

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.getOrPut(url.host) { mutableListOf() }
            for (newCookie in cookies) {
                list.removeAll { it.name == newCookie.name }
                list.add(newCookie)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = cookieStore[url.host] ?: return emptyList()
            val now = System.currentTimeMillis()
            list.removeAll { it.expiresAt < now }
            return list.toList()
        }
    }

    private class AuthInterceptor(
        private val sessionManager: EncryptedSessionManager
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            val token = sessionManager.getAuthToken()
            val sessionId = sessionManager.getSessionId()

            // Only add Token authorization if we have an actual API token (not a Google ID token JWT)
            if (!token.isNullOrBlank() && !token.startsWith("eyJ") && token.length < 100) {
                requestBuilder.header("Authorization", "Token $token")
            }
            // Add Django session cookie if authenticated
            if (!sessionId.isNullOrBlank()) {
                requestBuilder.header("Cookie", "sessionid=$sessionId")
            }
            requestBuilder.header("Accept", "application/json")

            val response = chain.proceed(requestBuilder.build())

            // Only clear session when server explicitly returns 401 Unauthorized for an authenticated call
            if (response.code == 401 && (!sessionId.isNullOrBlank() || !token.isNullOrBlank())) {
                Log.w(TAG, "Server returned 401 Unauthorized. Session expired on backend.")
                sessionManager.clearSession()
            }
            return response
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        // Sanitize sensitive credentials from logs
        if (!message.contains("Authorization", ignoreCase = true) &&
            !message.contains("password", ignoreCase = true) &&
            !message.contains("id_token", ignoreCase = true) &&
            !message.contains("credential", ignoreCase = true) &&
            !message.contains("sessionid", ignoreCase = true)
        ) {
            Log.d(TAG, message)
        }
    }.apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val baseOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .build()

    fun create(context: Context): AmarHishabApiService {
        val sessionManager = EncryptedSessionManager.getInstance(context)

        val client = baseOkHttpClient.newBuilder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(AmarHishabApiService::class.java)
    }

    suspend fun executeAuthCodeExchange(authCode: String): AuthExchangeResult = withContext(Dispatchers.IO) {
        if (authCode.isBlank()) {
            throw IllegalArgumentException("Authorization code cannot be empty.")
        }

        val jsonMedia = "application/json; charset=utf-8".toMediaType()
        val jsonPayload = "{\"code\":\"${authCode.trim()}\"}"
        val requestBody = jsonPayload.toRequestBody(jsonMedia)

        val endpoints = listOf(EXCHANGE_ENDPOINT, EXCHANGE_FALLBACK_ENDPOINT)
        var lastException: IOException? = null

        for (endpoint in endpoints) {
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build()

            val response: Response
            try {
                response = baseOkHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                lastException = IOException("Failed to connect to Amar Hishab authentication server: ${e.message}", e)
                continue
            }

            val httpCode = response.code

            // If 404 and we have another endpoint, try the fallback
            if (httpCode == 404 && endpoint != endpoints.last()) {
                response.close()
                continue
            }

            var sessionId: String? = null
            val setCookieHeaders = response.headers("Set-Cookie")
            for (header in setCookieHeaders) {
                val parsed = Cookie.parse(request.url, header)
                if (parsed != null && parsed.name == "sessionid") {
                    sessionId = parsed.value
                }
            }
            if (sessionId == null) {
                val cookies = cookieStore[request.url.host] ?: emptyList()
                for (c in cookies) {
                    if (c.name == "sessionid") {
                        sessionId = c.value
                        break
                    }
                }
            }

            val rawBody = response.body?.string().orEmpty()

            if (response.isSuccessful || httpCode in 200..299) {
                var token: String? = null
                var userDto: UserDto? = null
                try {
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val adapter = moshi.adapter(AuthExchangeResponseDto::class.java)
                    val dto = adapter.fromJson(rawBody)
                    if (dto != null) {
                        if (sessionId.isNullOrBlank()) {
                            sessionId = dto.sessionId
                        }
                        token = dto.token ?: dto.key
                        userDto = dto.user
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Response JSON parsing note: ${e.message}")
                }

                if (!sessionId.isNullOrBlank() || !token.isNullOrBlank()) {
                    return@withContext AuthExchangeResult(
                        sessionId = sessionId,
                        token = token,
                        userDto = userDto
                    )
                } else {
                    throw IOException("Backend responded without valid session credentials.")
                }
            } else {
                Log.e(TAG, "Code exchange failed with HTTP $httpCode: ${rawBody.take(200)}")
                when (httpCode) {
                    401, 403 -> throw IOException("401 Unauthorized: Authorization code is invalid or expired.")
                    404 -> throw IOException("404 Not Found: Authentication exchange endpoint not found on server ($endpoint).")
                    400 -> throw IOException("400 Bad Request: Invalid authorization code submission.")
                    in 500..599 -> throw IOException("$httpCode Server Error: Amar Hishab server error during code exchange.")
                    else -> throw IOException("Server returned HTTP $httpCode during code exchange.")
                }
            }
        }

        throw lastException ?: IOException("Authentication server unreachable.")
    }

    suspend fun executeBackendLogout(sessionId: String? = null): Unit = withContext(Dispatchers.IO) {
        try {
            cookieStore.clear()
            val requestBuilder = Request.Builder()
                .url("${BASE_URL.removeSuffix("/api/v1/")}/accounts/logout/")
                .post(FormBody.Builder().build())
            if (!sessionId.isNullOrBlank()) {
                requestBuilder.header("Cookie", "sessionid=$sessionId")
            }
            baseOkHttpClient.newCall(requestBuilder.build()).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Backend logout request completed with note: ${e.message}")
        }
    }
}
