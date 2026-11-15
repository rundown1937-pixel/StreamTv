package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistRepositoryTest {

    private fun isValidStreamUri(url: String): Boolean {
        val separatorIndex = url.indexOf("://")
        return separatorIndex > 0 &&
            url.substring(0, separatorIndex).matches(Regex("[A-Za-z][A-Za-z0-9+.-]*")) &&
            url.length > separatorIndex + 3 &&
            url.none { it.isWhitespace() }
    }

    @Test
    fun bundledPlaylistLoadsEveryOriginalNameAndLink() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val channels = PlaylistRepository(context).loadBundledChannels()

        assertEquals(12_811, channels.size)
        assertTrue(
            channels.all {
                it.name.isNotBlank() && isValidStreamUri(it.streamUrl)
            },
        )

        assertEquals("4 Afghanistan", channels.first().name)
        assertEquals(
            "https://4afghls.persiana.live/hls/stream.m3u8",
            channels.first().streamUrl
        )
        assertEquals(6, channels.count { it.streamUrl.startsWith("rtmp://", ignoreCase = true) })
        assertEquals(1, channels.count { it.streamUrl.startsWith("srt://", ignoreCase = true) })
        assertEquals(2, channels.count { it.streamUrl.startsWith("mmsh://", ignoreCase = true) })
    }

    @Test
    fun appOpenLoadsFullSyncedCacheAbove13200Channels() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cacheFile = java.io.File(context.filesDir, "synced_channels_cache.jsonl")
        try {
            cacheFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                for (index in 1..13_250) {
                    val obj = org.json.JSONObject()
                    obj.put("id", "cached_$index")
                    obj.put("name", "Channel $index")
                    obj.put("streamUrl", "https://example.com/live/$index.m3u8")
                    obj.put("logoUrl", "")
                    obj.put("genre", "News")
                    obj.put("country", "US")
                    obj.put("language", "English")
                    obj.put("isLocked", false)
                    obj.put("isFavorite", false)
                    obj.put("resolution", "1080p FHD")
                    obj.put("isAdultOrRestricted", false)
                    obj.put("description", "Cached channel $index")
                    obj.put("httpUserAgent", "")
                    obj.put("httpReferrer", "")
                    writer.write(obj.toString())
                    writer.newLine()
                }
            }

            val repo = PlaylistRepository(context)
            val deadline = System.currentTimeMillis() + 5_000L
            while (!repo.isFullPlaylistLoaded.value && System.currentTimeMillis() < deadline) {
                Thread.sleep(25L)
            }

            assertTrue("Expected full playlist to finish loading", repo.isFullPlaylistLoaded.value)
            assertEquals(13_250, repo.channels.value.size)
            assertEquals("cached_13250", repo.channels.value.last().id)
        } finally {
            cacheFile.delete()
        }
    }
}
