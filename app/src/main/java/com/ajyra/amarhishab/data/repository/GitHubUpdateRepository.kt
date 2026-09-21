package com.ajyra.amarhishab.data.repository

import android.util.Log
import com.ajyra.amarhishab.model.AppUpdateInfo
import com.ajyra.amarhishab.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GitHubUpdateRepository {

    private const val TAG = "GitHubUpdateRepo"
    const val GITHUB_OWNER = "ajsrabon99"
    const val GITHUB_REPO = "Amar-Hishab-Android"
    const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/ajsrabon99/Amar-Hishab-Android/main/update.json"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkLatestRelease(): NetworkResult<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UPDATE_MANIFEST_URL)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                if (body.isBlank()) {
                    return@withContext NetworkResult.Error(
                        messageEn = "Please check your internet connection and try again.",
                        messageBn = "অনুগ্রহ করে আপনার ইন্টারনেট সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।"
                    )
                }

                val json = JSONObject(body)
                val remoteVersionCode = json.optInt("versionCode", -1)
                val remoteVersionName = json.optString("versionName", "").trim()
                val releaseNotes = json.optString("releaseNotes", "Bug fixes and improvements.")
                val downloadUrl = json.optString("downloadUrl", "").trim()

                if (remoteVersionCode <= 0 || remoteVersionName.isBlank()) {
                    return@withContext NetworkResult.Error(
                        messageEn = "Please check your internet connection and try again.",
                        messageBn = "অনুগ্রহ করে আপনার ইন্টারনেট সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।"
                    )
                }

                val finalDownloadUrl = if (downloadUrl.isNotBlank()) {
                    downloadUrl
                } else {
                    "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
                }

                NetworkResult.Success(
                    AppUpdateInfo(
                        latestVersion = remoteVersionName,
                        latestVersionCode = remoteVersionCode,
                        minSupportedVersion = "1.0.0",
                        updateRequired = false,
                        releaseNotes = releaseNotes,
                        downloadUrl = finalDownloadUrl
                    )
                )
            } else {
                Log.w(TAG, "Server responded with HTTP ${response.code} when fetching update manifest")
                NetworkResult.Error(
                    messageEn = "Please check your internet connection and try again.",
                    messageBn = "অনুগ্রহ করে আপনার ইন্টারনেট সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।"
                )
            }
        } catch (e: java.io.IOException) {
            Log.w(TAG, "Network failure checking updates: ${e.message}")
            NetworkResult.Error(
                messageEn = "Please check your internet connection and try again.",
                messageBn = "অনুগ্রহ করে আপনার ইন্টারনেট সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking updates", e)
            NetworkResult.Error(
                messageEn = "Please check your internet connection and try again.",
                messageBn = "অনুগ্রহ করে আপনার ইন্টারনেট সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।"
            )
        }
    }
}
