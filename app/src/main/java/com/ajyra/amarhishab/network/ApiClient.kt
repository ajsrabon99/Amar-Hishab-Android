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
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "https://amar-hisab.onrender.com/api/v1/"
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

            // Attach Bearer token for secure first-party mobile authentication
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
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
        if (!message.contains("Authorization", ignoreCase = true) &&
            !message.contains("password", ignoreCase = true) &&
            !message.contains("credential", ignoreCase = true) &&
            !message.contains("token", ignoreCase = true)
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

    suspend fun executeBackendLogout(rawToken: String?): Unit = withContext(Dispatchers.IO) {
        try {
            cookieStore.clear()
            if (!rawToken.isNullOrBlank()) {
                val request = Request.Builder()
                    .url("${BASE_URL}auth/logout/")
                    .post("{}".toRequestBody(null))
                    .header("Authorization", "Bearer $rawToken")
                    .header("Accept", "application/json")
                    .build()
                baseOkHttpClient.newCall(request).execute().close()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend logout call note: ${e.message}")
        }
    }
}
