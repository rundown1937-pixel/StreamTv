package com.example

import android.Manifest
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.screens.AppSplashScreen
import com.example.ui.screens.MainNavigationScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TVViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private companion object {
        const val LOCAL_NETWORK_PERMISSION_REQUESTED = "local_network_permission_requested"
    }

    private val viewModel: TVViewModel by viewModels()
    private var localNetworkPermissionDialog: AlertDialog? = null

    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onLocalNetworkPermissionResult(granted)
            if (!granted) {
                val wasRequested = getSharedPreferences("stream_pulse_permissions", MODE_PRIVATE)
                    .getBoolean(LOCAL_NETWORK_PERMISSION_REQUESTED, false)
                if (wasRequested && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_LOCAL_NETWORK)) {
                    showLocalNetworkPermissionPrompt()
                } else {
                    Toast.makeText(
                        this,
                        "Local IPTV streams and playlists need local network access.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Defer non-critical analytics and ads off the UI thread and critical startup frame path
        val appContext = applicationContext
        lifecycleScope.launch(Dispatchers.Default) {
            com.example.util.AnalyticsManager.initialize(appContext)
            com.example.util.AnalyticsManager.logScreenView("HOME")
            com.example.util.AdManager.initialize(appContext)
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
                launch {
                    viewModel.localNetworkPermissionRequested.collect { requested ->
                        if (requested) showLocalNetworkPermissionPrompt()
                    }
                }
            }
        }

        handleIntent(intent)

        setContent {
            val themeMode by viewModel.appThemeMode.collectAsState()
            val isLightTheme = themeMode == com.example.ui.theme.AppThemeMode.WHITE
            val insetsController = androidx.compose.runtime.remember(window) {
                WindowCompat.getInsetsController(window, window.decorView)
            }
            androidx.compose.runtime.SideEffect {
                insetsController.isAppearanceLightStatusBars = isLightTheme
                insetsController.isAppearanceLightNavigationBars = isLightTheme
            }

            MyApplicationTheme(themeMode = themeMode) {
                val isLaunchFinished by viewModel.isAppLaunchCountdownFinished.collectAsState()

                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = isLaunchFinished,
                        label = "app_launch_crossfade",
                    ) { ready ->
                        if (!ready) {
                            AppSplashScreen(
                                viewModel = viewModel,
                                onFinished = {
                                    viewModel.completeAppLaunch()
                                },
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
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            // Only clear keep-screen-on if not playing
            if (!viewModel.isPlaying.value) {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (viewModel.isFullscreen.value) {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showLocalNetworkPermissionPrompt() {
        if (Build.VERSION.SDK_INT < 37) {
            viewModel.onLocalNetworkPermissionResult(granted = true)
            return
        }
        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onLocalNetworkPermissionResult(granted = true)
            return
        }
        if (localNetworkPermissionDialog?.isShowing == true) return

        val preferences = getSharedPreferences("stream_pulse_permissions", MODE_PRIVATE)
        val wasRequested = preferences.getBoolean(LOCAL_NETWORK_PERMISSION_REQUESTED, false)
        val permanentlyDenied = wasRequested && !shouldShowRequestPermissionRationale(permission)
        val message = if (permanentlyDenied) {
            "Local network access is disabled. Open app settings to allow playback of IPTV streams and playlists hosted on your Wi-Fi network."
        } else {
            "StreamPulse needs local network access only for IPTV streams and playlists hosted on your Wi-Fi network. Internet streams continue to work without it."
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Local network access")
            .setMessage(message)
            .setNegativeButton("Not now") { _, _ ->
                viewModel.onLocalNetworkPermissionResult(granted = false)
            }
            .setPositiveButton(if (permanentlyDenied) "Open settings" else "Continue") { _, _ ->
                if (permanentlyDenied) {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        },
                    )
                } else {
                    preferences.edit { putBoolean(LOCAL_NETWORK_PERMISSION_REQUESTED, true) }
                    localNetworkPermissionLauncher.launch(permission)
                }
            }
            .setOnCancelListener {
                viewModel.onLocalNetworkPermissionResult(granted = false)
            }
            .create()
        localNetworkPermissionDialog = dialog
        dialog.setOnDismissListener {
            if (localNetworkPermissionDialog === dialog) localNetworkPermissionDialog = null
        }
        dialog.show()
    }

    fun enterSystemPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                return
            }
            try {
                val paramsBuilder = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    paramsBuilder.setAutoEnterEnabled(true)
                }

                val sourceRectHint = android.graphics.Rect()
                if (window.decorView.getGlobalVisibleRect(sourceRectHint)) {
                    paramsBuilder.setSourceRectHint(sourceRectHint)
                }

                enterPictureInPictureMode(paramsBuilder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.isPlaying.value && (viewModel.currentChannel.value != null)) {
            enterSystemPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setPipMode(isInPictureInPictureMode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val channelId = intent.getStringExtra("auto_play_channel_id") ?: return
        lifecycleScope.launch {
            viewModel.completeAppLaunch()
            viewModel.playChannelById(channelId, directLandscape = true)
        }
    }

    override fun onDestroy() {
        localNetworkPermissionDialog?.dismiss()
        localNetworkPermissionDialog = null
        super.onDestroy()
    }
}
