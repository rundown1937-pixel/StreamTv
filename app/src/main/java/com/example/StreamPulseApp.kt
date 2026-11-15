package com.example

import android.app.ActivityManager
import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

class StreamPulseApp : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = activityManager?.isLowRamDevice == true
        val memoryClass = activityManager?.memoryClass ?: 128

        // Allocate a safe, compact percentage of RAM for image caching based on device capabilities
        val memoryCachePercent = when {
            isLowRam -> 0.10
            memoryClass <= 192 -> 0.15
            else -> 0.20
        }

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, memoryCachePercent)
                    .strongReferencesEnabled(enable = true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(25L * 1024 * 1024) // 25 MB max disk cache to minimize storage usage
                    .build()
            }
            .components {
                // Reuse a single optimized OkHttpClient instance for all Coil image fetches
                // to enable HTTP connection pooling, socket reuse, and prevent per-image thread allocations
                val sharedImageOkHttpClient = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
                val callFactory: () -> okhttp3.Call.Factory = { sharedImageOkHttpClient }
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = callFactory,
                    ),
                )
            }
            .crossfade(enable = true)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Aggressively free bitmap memory cache when system is under memory pressure or app UI is hidden
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            SingletonImageLoader.get(this).memoryCache?.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        SingletonImageLoader.get(this).memoryCache?.clear()
    }
}
