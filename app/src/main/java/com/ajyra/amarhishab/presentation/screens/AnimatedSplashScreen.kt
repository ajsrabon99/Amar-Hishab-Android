package com.ajyra.amarhishab.presentation.screens

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.ajyra.amarhishab.R
import kotlinx.coroutines.delay

/**
 * Official Amar Hishab launch video intro screen.
 *
 * Requirements:
 * - Plays the bundled official intro video once at application launch.
 * - Offline playback from res/raw/intro_video.mp4.
 * - Transitions smoothly to the Dashboard upon completion.
 * - Safely handles errors or timeouts without ever freezing or blocking the user.
 * - Releases VideoView and MediaPlayer resources immediately on completion or exit.
 * - Does not loop and cannot replay during regular dashboard navigation.
 */
@Composable
fun AnimatedSplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    var hasFinished by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    fun finishIntro(reason: String) {
        if (!hasFinished) {
            hasFinished = true
            Log.d("AnimatedSplashScreen", "Intro finished via $reason, proceeding to Dashboard")
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {
            }
            onSplashFinished()
        }
    }

    // Safety timeout: video is 8.0 seconds; 9.5s ensures even slow decoders finish cleanly
    LaunchedEffect(Unit) {
        delay(9500L)
        finishIntro("safety_timeout")
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060D15))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Optional tap to skip directly to dashboard for power users
                finishIntro("user_tap_skip")
            }
            .testTag("animated_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    videoViewRef = this

                    try {
                        val videoUri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.intro_video}")
                        setVideoURI(videoUri)

                        setOnPreparedListener { mp ->
                            try {
                                mp.isLooping = false
                                mp.setVolume(1.0f, 1.0f)
                            } catch (e: Exception) {
                                Log.w("AnimatedSplashScreen", "MediaPlayer prep config note: ${e.message}")
                            }
                            start()
                        }

                        setOnCompletionListener {
                            finishIntro("video_completion")
                        }

                        setOnErrorListener { _, what, extra ->
                            Log.w("AnimatedSplashScreen", "Video playback error ($what, $extra), proceeding to dashboard safely")
                            finishIntro("playback_error")
                            true // Handled safely
                        }
                    } catch (e: Exception) {
                        Log.e("AnimatedSplashScreen", "VideoView setup exception: ${e.message}", e)
                        finishIntro("setup_exception")
                    }
                }
            },
            update = { view ->
                if (!view.isPlaying && !hasFinished) {
                    try {
                        view.start()
                    } catch (_: Exception) {
                    }
                }
            }
        )
    }
}
