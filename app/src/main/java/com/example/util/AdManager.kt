package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    // Use Google official test IDs in DEBUG builds to prevent 0-fill / ad restriction errors during development
    private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/3419835294"
    private const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    val APP_OPEN_AD_ID: String
        get() = if (BuildConfig.DEBUG) TEST_APP_OPEN_ID else try { BuildConfig.ADMOB_APP_OPEN_AD_ID } catch (_: Throwable) { TEST_APP_OPEN_ID }

    val HOME_NATIVE_AD_ID: String
        get() = if (BuildConfig.DEBUG) TEST_NATIVE_ID else try { BuildConfig.ADMOB_HOME_NATIVE_AD_ID } catch (_: Throwable) { TEST_NATIVE_ID }

    val CHANNELS_NATIVE_AD_ID: String
        get() = if (BuildConfig.DEBUG) TEST_NATIVE_ID else try { BuildConfig.ADMOB_CHANNELS_NATIVE_AD_ID } catch (_: Throwable) { TEST_NATIVE_ID }

    val FAVORITES_NATIVE_AD_ID: String
        get() = if (BuildConfig.DEBUG) TEST_NATIVE_ID else try { BuildConfig.ADMOB_FAVORITES_NATIVE_AD_ID } catch (_: Throwable) { TEST_NATIVE_ID }

    val VIDEO_PLAYER_SUPPORT_AD_ID: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_ID else try { BuildConfig.ADMOB_VIDEO_PLAYER_SUPPORT_AD_ID } catch (_: Throwable) { TEST_REWARDED_ID }

    val BACK_BUTTON_AD_ID: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else try { BuildConfig.ADMOB_BACK_BUTTON_AD_ID } catch (_: Throwable) { TEST_INTERSTITIAL_ID }

    private var isInitialized = false
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenAdLoading = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // Set test device identifiers if needed, MobileAds handles test IDs automatically in debug
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob initialized successfully: $status")
                isInitialized = true
                loadAppOpenAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
            isInitialized = true // fallback to simulated ads
        }
    }

    fun loadAppOpenAd(context: Context) {
        if ((appOpenAd != null) || isAppOpenAdLoading) return
        isAppOpenAdLoading = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            APP_OPEN_AD_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenAdLoading = false
                    Log.d(TAG, "App Open Ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isAppOpenAdLoading = false
                    Log.w(TAG, "App Open Ad failed to load (using fallback): ${error.message}")
                }
            },
        )
    }

    fun showInterstitialAd(activity: Activity, adUnitId: String, onAdDismissed: () -> Unit = {}) {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            adUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.show(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial ad failed to load ($adUnitId), proceeding: ${error.message}")
                    onAdDismissed()
                }
            },
        )
    }

    fun showRewardedOrInterstitialAd(activity: Activity, adUnitId: String, onRewarded: () -> Unit = {}) {
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            adUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    rewardedAd.show(activity) { rewardItem ->
                        Log.d(TAG, "Rewarded ad watched: ${rewardItem.amount}")
                        onRewarded()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load, falling back to interstitial: ${error.message}")
                    showInterstitialAd(activity, adUnitId, onAdDismissed = onRewarded)
                }
            },
        )
    }
}

@Composable
fun AdNativeView(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
    var adFailed by remember { mutableStateOf(value = false) }

    DisposableEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                nativeAdState = nativeAd
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("AdNativeView", "Native ad failed to load ($adUnitId): ${error.message}")
                        adFailed = true
                    }
                },
            )
            .build()
        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAdState?.destroy()
        }
    }

    if ((nativeAdState == null) && adFailed) {
        // Fallback Flat Native Ad Card
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E212E))
                .border(1.dp, Color(0xFF2E334D), RoundedCornerShape(12.dp))
                .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/dev?id=StrataTechLab".toUri())
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sponsored • StreamPulse Partner",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ad • High-speed streaming companion",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = "Install",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
    } else {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(88.dp),
            factory = { ctx ->
                val adView = NativeAdView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    setBackgroundColor(0xFF141720.toInt())
                }

                val layout = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    setPadding(16, 12, 16, 12)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val textLayout = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    )
                }

                val headlineView = android.widget.TextView(ctx).apply {
                    textSize = 13f
                    setTextColor(0xFF00FFFF.toInt())
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                textLayout.addView(headlineView)
                adView.headlineView = headlineView

                val bodyView = android.widget.TextView(ctx).apply {
                    textSize = 11f
                    setTextColor(0xFFAAAAAA.toInt())
                    maxLines = 2
                }
                textLayout.addView(bodyView)
                adView.bodyView = bodyView

                layout.addView(textLayout)

                val ctaView = android.widget.Button(ctx).apply {
                    textSize = 11f
                    text = ctx.getString(com.example.R.string.ad_install)
                    setBackgroundColor(0xFF00FFFF.toInt())
                    setTextColor(0xFF00363D.toInt())
                }
                layout.addView(ctaView)
                adView.callToActionView = ctaView

                adView.addView(layout)
                adView
            },
        ) { adView ->
                val nativeAd = nativeAdState ?: return@AndroidView

                (adView.headlineView as? android.widget.TextView)?.text = nativeAd.headline
                (adView.bodyView as? android.widget.TextView)?.text = nativeAd.body
                (adView.callToActionView as? android.widget.Button)?.text = nativeAd.callToAction ?: "Install"

                adView.setNativeAd(nativeAd)
        }
    }
}
