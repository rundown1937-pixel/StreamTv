package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.screens.MainNavigationScreen
import com.example.ui.theme.MyApplicationTheme
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.viewmodel.TVViewModel
import kotlinx.coroutines.launch

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.screens.AppSplashScreen

class MainActivity : ComponentActivity() {

    private val viewModel: TVViewModel by viewModels()
    private var karateChopDetector: com.example.util.KarateChopDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Permanent Two Karate Chop motion detection
        karateChopDetector = com.example.util.KarateChopDetector(
            context = this,
            sensitivity = com.example.util.KarateChopDetector.Sensitivity.NORMAL,
            onChopProgress = { count, force ->
                viewModel.updateKarateChopProgress(count, force)
            },
            onDoubleChopDetected = {
                runOnUiThread {
                    viewModel.handleKarateChopGesture()
                }
            }
        )
        viewModel.setInAppKarateChopDetector(karateChopDetector)

        // Background service for device motion detection if enabled
        if (viewModel.isKarateChopEnabled.value) {
            com.example.service.KarateChopService.startService(applicationContext)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.enterPipRequest.collect {
                        enterSystemPip()
                    }
                }
                launch {
                    viewModel.isFullscreen.collect { fullscreen ->
                        updateFullscreenOrientation(fullscreen)
                    }
                }
            }
        }

        handleIntent(intent)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val isLaunchFinished by viewModel.isAppLaunchCountdownFinished.collectAsState()

                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = isLaunchFinished,
                        label = "app_launch_crossfade"
                    ) { ready ->
                        if (!ready) {
                            AppSplashScreen(
                                viewModel = viewModel,
                                onFinished = {
                                    viewModel.completeAppLaunch()
                                }
                            )
                        } else {
                            MainNavigationScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun updateFullscreenOrientation(fullscreen: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (fullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!viewModel.isFullscreen.value) {
                viewModel.setFullscreen(true)
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (viewModel.isFullscreen.value) {
                viewModel.setFullscreen(false)
            }
        }
    }

    fun enterSystemPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.isPlaying.value && viewModel.currentChannel.value != null) {
            enterSystemPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setPipMode(isInPictureInPictureMode)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isKarateChopEnabled.value) {
            karateChopDetector?.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        karateChopDetector?.stopListening()
    }

    override fun onDestroy() {
        karateChopDetector?.stopListening()
        karateChopDetector = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra("karate_chop_launched", false)) {
            if (viewModel.isKarateChopEnabled.value) {
                lifecycleScope.launch {
                    viewModel.handleKarateChopGesture()
                }
            }
            return
        }

        val channelId = intent.getStringExtra("auto_play_channel_id") ?: return
        lifecycleScope.launch {
            viewModel.completeAppLaunch()
            viewModel.playChannelById(channelId, directLandscape = true)
        }
    }
}
