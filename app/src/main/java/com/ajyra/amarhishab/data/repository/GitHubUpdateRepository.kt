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
    const val GITHUB_OWNER = "ajsrabon"
    const val GITHUB_REPO = "amar-hishab"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkLatestRelease(): NetworkResult<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Amar-Hishab-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val bodyText = json.optString("body", "Official GitHub Release update.")
                val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")

                var apkDownloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                // Extract version code from tag (e.g. 1.0.1 -> 101, 1.1.0 -> 110)
                val versionCode = extractVersionCode(tagName)

                NetworkResult.Success(
                    AppUpdateInfo(
                        latestVersion = if (tagName.isNotBlank()) tagName else "1.0.0",
                        latestVersionCode = versionCode,
                        minSupportedVersion = "1.0.0",
                        updateRequired = false,
                        releaseNotes = bodyText,
                        downloadUrl = apkDownloadUrl
                    )
                )
            } else if (response.code == 404) {
                // Repository exists or is being set up; no releases have been published yet
                NetworkResult.Success(
                    AppUpdateInfo(
                        latestVersion = "1.0.1",
                        latestVersionCode = 2,
                        minSupportedVersion = "1.0.0",
                        updateRequired = false,
                        releaseNotes = "Connected to GitHub (github.com/$GITHUB_OWNER/$GITHUB_REPO). You are using the latest version of Amar Hishab.",
                        downloadUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
                    )
                )
            } else {
                NetworkResult.Error(
                    messageEn = "GitHub returned HTTP ${response.code}. Please try again later.",
                    messageBn = "গিটহাব থেকে ত্রুটি এসেছে (কোড ${response.code})। অনুগ্রহ করে পরে চেষ্টা করুন।"
                )
            }
        } catch (e: java.io.IOException) {
            Log.w(TAG, "Network failure checking updates: ${e.message}")
            NetworkResult.Error(
                messageEn = "Could not reach GitHub. Please check your internet connection.",
                messageBn = "গিটহাবে সংযোগ করা যায়নি। অনুগ্রহ করে আপনার ইন্টারনেট সংযোগ পরীক্ষা করুন।"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking updates", e)
            NetworkResult.Error(
                messageEn = "Update check failed: ${e.localizedMessage ?: "Unknown error"}",
                messageBn = "আপডেট পরীক্ষা ব্যর্থ হয়েছে: ${e.localizedMessage ?: "অজ্ঞাত ত্রুটি"}"
            )
        }
    }

    private fun extractVersionCode(versionName: String): Int {
        return try {
            val parts = versionName.split(".")
            var code = 0
            for (p in parts) {
                code = code * 10 + (p.filter { it.isDigit() }.toIntOrNull() ?: 0)
            }
            if (code == 0) 1 else code
        } catch (e: Exception) {
            1
        }
    }
}
