package com.ajyra.amarhishab.presentation.components

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ajyra.amarhishab.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

object AdMobManager {
    private const val TAG = "AdMobManager"
    private val isInitialized = AtomicBoolean(false)

    // AdMob IDs requested by user
    const val APP_ID = "ca-app-pub-6208705517596475~3431483621"
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-6208705517596475/3511374677"
    // Google official sample test banner unit ID for safe development/testing
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    val BANNER_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    /**
     * Initializes Google Mobile Ads SDK once at application startup on the Main UI thread.
     */
    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                // Must be invoked on the main UI thread as required by Google Mobile Ads SDK
                MobileAds.initialize(context) { status ->
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Google Mobile Ads initialized successfully on main thread")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "AdMob initialization error: ${e.message}")
            }
        }
    }
}

/**
 * Standard AdMob Banner Ad Composable.
 * Used exclusively on Transaction, Report, Profile, and Savings Goals screens.
 * Properly manages AdView lifecycle, maintains consistent 50dp height to prevent zero-height collapse,
 * and destroys ad on dispose to prevent memory leaks.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobManager.BANNER_AD_UNIT_ID
) {
    val isInspectionMode = LocalInspectionMode.current
    if (isInspectionMode) {
        return
    }

    var currentAdView by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(adUnitId) {
        onDispose {
            try {
                currentAdView?.destroy()
                currentAdView = null
            } catch (e: Exception) {
                // Ignore cleanup error
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    setAdUnitId(adUnitId)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            Log.d("AdMobBanner", "Banner ad loaded successfully ($adUnitId)")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            super.onAdFailedToLoad(error)
                            Log.w(
                                "AdMobBanner",
                                "Banner ad failed to load ($adUnitId): code=${error.code}, message=${error.message}, domain=${error.domain}"
                            )
                        }
                    }
                    val adRequest = AdRequest.Builder().build()
                    loadAd(adRequest)
                    currentAdView = this
                }
            },
            update = {
                // No-op on recomposition to avoid redundant ad requests
            }
        )
    }
}
