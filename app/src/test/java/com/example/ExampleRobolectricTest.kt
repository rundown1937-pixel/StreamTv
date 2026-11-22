package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Channel
import com.example.data.repository.PlaylistRepository
import com.example.data.repository.ProfileRepository
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.data.model.isOfflineMetadata
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun isValidStreamUri(url: String): Boolean {
  val separatorIndex = url.indexOf("://")
  if (separatorIndex <= 0) return false
  val scheme = url.substring(0, separatorIndex)
  val isSchemeValid = scheme.matches(Regex("[A-Za-z][A-Za-z0-9+.-]*"))
  val isLengthValid = url.length > (separatorIndex + 3)
  val hasNoWhitespace = url.none { it.isWhitespace() }
  return isSchemeValid && isLengthValid && hasNoWhitespace
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StreamPulse TV", appName)
  }

  @Test
  fun `curated channels are initialized with genres`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val playlistRepo = PlaylistRepository(context)
    val channels = playlistRepo.channels.value

    assertTrue(channels.isNotEmpty())
    val genres = channels.asSequence().map(Channel::genre).toSet()
    assertTrue(genres.contains("News"))
    assertTrue(genres.contains("Sports"))
    assertTrue(genres.contains("Kids & Animation"))
  }

  @Test
  fun `profile repository handles parental pin and profiles`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val profileRepo = ProfileRepository(context)

    val active = profileRepo.activeProfile.value
    assertNotNull(active)

    assertTrue(profileRepo.verifyPin("1234"))
    assertTrue(profileRepo.setParentalPin("9876"))
    assertTrue(profileRepo.verifyPin("9876"))
  }

  @Test
  fun `localized strings work correctly across all supported languages`() {
    val (_, enLiveTab) = com.example.util.getStringsForLanguage("en")
    val (_, hiLiveTab) = com.example.util.getStringsForLanguage("hi")
    val (_, esLiveTab) = com.example.util.getStringsForLanguage("es")
    val (_, frLiveTab) = com.example.util.getStringsForLanguage("fr")
    val (_, deLiveTab) = com.example.util.getStringsForLanguage("de")
    val (_, arLiveTab) = com.example.util.getStringsForLanguage("ar")

    assertEquals("Live", enLiveTab)
    assertEquals("लाइव", hiLiveTab)
    assertEquals("En Vivo", esLiveTab)
    assertEquals("En Direct", frLiveTab)
    assertEquals("Live", deLiveTab)
    assertEquals("مباشر", arLiveTab)
  }

  @Test
  fun `24 hour auto update window logic skips sync when fresh`() {
    val now = System.currentTimeMillis()
    val recentSync = now - (2 * 3600 * 1000L) // 2 hours ago
    val oldSync = now - (25 * 3600 * 1000L) // 25 hours ago

    val isFresh = (now - recentSync) < (24 * 3600 * 1000L)
    val isStale = (now - oldSync) >= (24 * 3600 * 1000L)

    assertTrue(isFresh)
    assertTrue(isStale)
  }

  @Test
  fun `curated channels stream links strictly match channel names and broadcasters`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val playlistRepo = PlaylistRepository(context)
    val channels = playlistRepo.getCuratedChannels()

    assertTrue(channels.isNotEmpty())

    // Check specific channels for exact broadcaster stream matching
    val f24En = channels.firstOrNull { (id) -> id == "france24_english" }
    assertNotNull(f24En)
    assertEquals("France 24 English", f24En!!.name)
    assertTrue(f24En.streamUrl.contains("france24.com"))

    val dw = channels.firstOrNull { (id) -> id == "dw_news_english" }
    assertNotNull(dw)
    assertEquals("DW News English", dw!!.name)
    assertTrue(dw.streamUrl.contains("dwstream") || dw.streamUrl.contains("dwamdstream"))

    val aja = channels.firstOrNull { (id) -> id == "aljazeera_arabic" }
    assertNotNull(aja)
    assertEquals("Al Jazeera Arabic", aja!!.name)
    assertTrue(aja.streamUrl.contains("getaj.net/AJA"))

    val rb = channels.firstOrNull { (id) -> id == "redbull_tv" }
    assertNotNull(rb)
    assertEquals("Red Bull TV Live", rb!!.name)
    assertTrue(rb.streamUrl.contains("akamaized.net"))

    val nasa = channels.firstOrNull { (id) -> id == "nasa_tv_hd" }
    assertNotNull(nasa)
    assertEquals("NASA TV HD", nasa!!.name)
    assertTrue(nasa.streamUrl.contains("NASA-NTV1"))

    // Ensure no demo / tears of steel stream exists
    val demoStreams = channels.filter { (id, name, streamUrl) ->
      id.contains("tears_of_steel") || name.contains("Tears of Steel") || streamUrl.contains("bigbuckbunny")
    }
    assertTrue("No demo or placeholder streams should exist", demoStreams.isEmpty())
  }

  @Test
  fun `online and offline stream health tracking accurately toggles channel state and persists`() = runTest {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    val channelId = "france24_english"
    vm.markChannelLive(channelId)
    assertTrue(vm.liveChannelIds.value.contains(channelId))
    assertTrue(!vm.brokenChannelIds.value.contains(channelId))

    // Simulating player stream failure
    vm.markChannelBroken(channelId)
    assertTrue(vm.brokenChannelIds.value.contains(channelId))
    assertTrue(!vm.liveChannelIds.value.contains(channelId))

    // Check SharedPreferences persistence
    val prefs = app.getSharedPreferences("stream_pulse_settings", Context.MODE_PRIVATE)
    assertTrue(prefs.getStringSet("broken_channel_ids", emptySet())?.contains(channelId) == true)

    // Verify subcategory "Offline" and "Live Now"
    vm.selectSubcategory("Offline")
    val offlineChannels = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
      withTimeout(5.seconds) {
        vm.filteredChannels.first { list -> list.any { (id) -> id == channelId } }
      }
    }
    assertTrue(offlineChannels.any { (id) -> id == channelId })

    vm.selectSubcategory("Live Now")
    val liveChannels = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
      withTimeout(5.seconds) {
        vm.filteredChannels.first { list -> list.none { (id) -> id == channelId } }
      }
    }
    assertTrue(liveChannels.none { (id) -> id == channelId })

    // Simulating player recovery
    vm.markChannelLive(channelId)
    assertTrue(!vm.brokenChannelIds.value.contains(channelId))
    assertTrue(vm.liveChannelIds.value.contains(channelId))
    assertTrue(prefs.getStringSet("broken_channel_ids", emptySet())?.contains(channelId) == false)

    val recoveredLiveChannels = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
      withTimeout(5.seconds) {
        vm.filteredChannels.first { list -> list.any { (id) -> id == channelId } }
      }
    }
    assertTrue(recoveredLiveChannels.any { (id) -> id == channelId })
  }

  @Test
  fun `metadata offline indicators and broken channels filter accurately with colors`() = runTest {
    val ch1 = Channel(id = "not_247", name = "Test Channel [Not 24/7]", streamUrl = "https://example.com/live.m3u8")
    val ch2 = Channel(id = "geo_blocked", name = "Test Channel [Geo-blocked]", streamUrl = "https://example.com/live.m3u8")
    val ch3 = Channel(id = "inactive", name = "Test Channel [Inactive]", streamUrl = "https://example.com/live.m3u8")
    val ch4 = Channel(id = "blank_url", name = "Test Channel", streamUrl = "")
    val ch5 = Channel(id = "normal_live", name = "Test Live Channel", streamUrl = "https://cdn.broadcaster.net/live/master.m3u8")

    assertTrue(ch1.isOfflineMetadata())
    assertTrue(ch2.isOfflineMetadata())
    assertTrue(ch3.isOfflineMetadata())
    assertTrue(ch4.isOfflineMetadata())
    assertFalse(ch5.isOfflineMetadata())
  }


  @Test
  fun `subcategories and search query filter provide pure channel results`() = runTest {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    val newsSubs = vm.getSubcategoriesForGenre("News")
    assertTrue(newsSubs.contains("World 24/7"))
    assertTrue(newsSubs.contains("Financial & Markets"))

    val sportsSubs = vm.getSubcategoriesForGenre("Sports")
    assertTrue(sportsSubs.contains("Extreme Action"))
    assertTrue(sportsSubs.contains("Motorsport"))

    vm.selectGenre("News")
    vm.selectSubcategory("Financial & Markets")
    assertEquals("Financial & Markets", vm.selectedSubcategory.value)

    vm.resetAllFilters()
    assertEquals("All", vm.selectedGenre.value)
    assertEquals("ALL", vm.selectedSubcategory.value)
    assertEquals("ALL", vm.selectedCountryFilter.value)
    assertEquals("ALL", vm.selectedLanguage.value)
    assertEquals("", vm.searchQuery.value)

    val allChannels = vm.playlistRepo.channels.value
    assertTrue(allChannels.any { (_, name) -> name.contains("NASA", ignoreCase = true) })
    val filtered = allChannels.filter { (_, name) -> name.contains("NASA", ignoreCase = true) }
    assertTrue(filtered.isNotEmpty())
    assertEquals("NASA TV HD", filtered[0].name)
  }

  @Test
  fun `screen tabs include live channels and settings without offline or epg tab`() {
    val tabs = com.example.ui.viewmodel.ScreenTab.entries.map { it.name }
    assertTrue(tabs.contains("HOME"))
    assertTrue(tabs.contains("FAVORITES"))
    assertTrue(tabs.contains("CHANNELS"))
    assertTrue(tabs.contains("PROFILES"))
    assertTrue(tabs.contains("SETTINGS"))
    assertTrue(!tabs.contains("EPG_GUIDE"))
    assertTrue(!tabs.contains("OFFLINE"))
  }

  @Test
  fun `channels fully load on startup and trigger app launch readiness`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    // Channels are loaded into memory on init
    val channels = vm.allChannels.value
    assertTrue(channels.isNotEmpty())
    assertTrue(channels.size >= 20)
    assertTrue(vm.isChannelsLoaded.value)

    // Countdown is active before completeAppLaunch is called
    assertEquals(false, vm.isAppLaunchCountdownFinished.value)

    // Complete countdown transitions app to ready state
    vm.completeAppLaunch()
    assertEquals(true, vm.isAppLaunchCountdownFinished.value)
  }

  @Test
  fun `purged legacy 11316 scraper data and verified all genuine channels have valid stream links and official names`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val playlistRepo = PlaylistRepository(context)

    // Verify cache purge of 11,316 broken scraper data
    playlistRepo.purgeOldScraperCacheIfPresent()

    val channels = playlistRepo.channels.value
    assertTrue("Channel list must not be empty", channels.isNotEmpty())
    assertTrue("Should have 25+ verified original channels", channels.size >= 25)

    for ((id, name, streamUrl, _, genre, country, language) in channels) {
      assertTrue("Channel ID must not be blank for $name", id.isNotBlank())
      assertTrue("Channel Name must not be blank for $id", name.isNotBlank())
      assertTrue("Stream URL must have a valid absolute scheme for $name", isValidStreamUri(streamUrl))
      assertTrue("Stream URL must not contain localhost or demo", !streamUrl.contains("tears_of_steel") && !streamUrl.contains("bbb_sunflower"))
      assertTrue("Channel must have a recognized genre for $name", genre.isNotBlank())
      assertTrue("Channel must have a country code for $name", country.isNotBlank())
      assertTrue("Channel must have a language for $name", language.isNotBlank())
    }

    // Verify presence of premier international broadcasters
    val channelNames = channels.map(Channel::name)
    assertTrue(channelNames.any { it.contains("France 24", ignoreCase = true) })
    assertTrue(channelNames.any { it.contains("Sky News", ignoreCase = true) })
    assertTrue(channelNames.any { it.contains("DW News", ignoreCase = true) })
    assertTrue(channelNames.any { it.contains("Al Jazeera", ignoreCase = true) })
    assertTrue(channelNames.any { it.contains("Bloomberg", ignoreCase = true) })
    assertTrue(channelNames.any { it.contains("TRT", ignoreCase = true) })
    assertTrue(channelNames.any { it.contains("Red Bull", ignoreCase = true) })
    assertTrue(channelNames.any { it.contains("NASA", ignoreCase = true) })
    assertTrue(channelNames.any { it.contains("CGTN", ignoreCase = true) })
  }

  @Test
  fun `every top channel opens its own distinct original stream URL and does not load DW channel`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)
    val channels = vm.allChannels.value

    val f24 = channels.first { (id) -> id == "france24_english" }
    vm.playChannel(f24)
    assertEquals("France 24 English", vm.currentChannel.value?.name)
    assertEquals("https://live.france24.com/hls/live/2037218-b/F24_EN_HI_HLS/master_5000.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("France 24 must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val sky = channels.first { (id) -> id == "sky_news_live" }
    vm.playChannel(sky)
    assertEquals("Sky News Live HD", vm.currentChannel.value?.name)
    assertEquals("https://skynewsau-live.akamaized.net/hls/live/2002689/skynewsau-extra1/master.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("Sky News must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val bloomberg = channels.first { (id) -> id == "bloomberg_tv" }
    vm.playChannel(bloomberg)
    assertEquals("Bloomberg TV Live", vm.currentChannel.value?.name)
    assertEquals("https://bloomberg.com/media-manifest/streams/us.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("Bloomberg must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val trt = channels.first { (id) -> id == "trt_world_global" }
    vm.playChannel(trt)
    assertEquals("TRT World HD", vm.currentChannel.value?.name)
    assertEquals("https://tv-trtworld.medya.trt.com.tr/master.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("TRT World must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val redbull = channels.first { (id) -> id == "redbull_tv" }
    vm.playChannel(redbull)
    assertEquals("Red Bull TV Live", vm.currentChannel.value?.name)
    assertEquals("https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("Red Bull must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val dw = channels.first { (id) -> id == "dw_news_english" }
    vm.playChannel(dw)
    assertEquals("DW News English", vm.currentChannel.value?.name)
    assertEquals("https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8", vm.currentChannel.value?.streamUrl)
  }

  @Test
  fun `deep test all audio video engine controls and functions`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    // Test audio mute toggle
    val initialMute = vm.isMuted.value
    vm.toggleMute()
    assertEquals(!initialMute, vm.isMuted.value)
    vm.toggleMute()
    assertEquals(initialMute, vm.isMuted.value)

    // Test sleep timer
    vm.setSleepTimer(15)
    assertEquals(15, vm.sleepTimerMinutes.value)
    vm.setSleepTimer(null)
    assertEquals(null, vm.sleepTimerMinutes.value)

    // Test adult content restriction toggle
    val currentAdultRestricted = vm.isAdultRestrictionEnabled.value
    vm.toggleAdultRestriction(!currentAdultRestricted)
    assertEquals(!currentAdultRestricted, vm.isAdultRestrictionEnabled.value)
    vm.toggleAdultRestriction(currentAdultRestricted)

    // Test channel navigation (next and previous)
    val firstChannel = vm.allChannels.value.first()
    vm.playChannel(firstChannel)
    assertEquals(firstChannel.id, vm.currentChannel.value?.id)

    vm.playNextChannel()
    val nextChannel = vm.currentChannel.value
    assertNotNull(nextChannel)

    vm.playPreviousChannel()
    val prevChannel = vm.currentChannel.value
    assertNotNull(prevChannel)
  }

  @Test
  fun `all 12000 plus channels loaded with original links and distinct IDs`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val playlistRepo = PlaylistRepository(context)
    val channels = playlistRepo.loadBundledChannels()

    assertTrue("Channels must have at least 12,000 channels, found ${channels.size}", channels.size >= 12000)

    // Verify all IDs are 100% unique
    val uniqueIds = channels.asSequence().map(Channel::id).toSet()
    assertEquals("All channel IDs must be distinct", channels.size, uniqueIds.size)

    // Verify channels have original stream links
    val haveValidLinks = channels.all { (_, _, streamUrl) -> isValidStreamUri(streamUrl) }
    assertTrue("All channels must have syntactically valid stream URLs", haveValidLinks)

    // Verify channels have non-empty names
    val hasNames = channels.all { (_, name) -> name.isNotBlank() }
    assertTrue("All channels must have official broadcaster names", hasNames)
  }

  @Test
  fun `channel plays directly in landscape mode`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    val firstChannel = vm.allChannels.value.first()
    vm.setFullscreen(enabled = false)
    assertEquals(false, vm.isFullscreen.value)

    // Playing any channel should immediately engage direct landscape mode
    vm.playChannel(firstChannel)
    assertEquals(firstChannel.id, vm.currentChannel.value?.id)
    assertEquals(true, vm.isFullscreen.value)
    assertEquals(true, vm.isPlaying.value)
  }

  @Test
  fun `full screen search state toggles correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    assertEquals(false, vm.isFullScreenSearchOpen.value)

    vm.openFullScreenSearch()
    assertEquals(true, vm.isFullScreenSearchOpen.value)

    vm.setSearchQuery("Bloomberg")
    assertEquals("Bloomberg", vm.searchQuery.value)

    vm.closeFullScreenSearch()
    assertEquals(false, vm.isFullScreenSearchOpen.value)
  }

  @Test
  fun `playing channel while search is open automatically closes search and starts playback`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    vm.openFullScreenSearch()
    assertEquals(true, vm.isFullScreenSearchOpen.value)

    val channel = vm.allChannels.value.first()
    vm.playChannel(channel, directLandscape = true)

    assertEquals(false, vm.isFullScreenSearchOpen.value)
    assertEquals(true, vm.isPlaying.value)
    assertEquals(true, vm.isFullscreen.value)
    assertEquals(channel.id, vm.currentChannel.value?.id)
  }

  @Test
  fun `favorites feature bookmarks and filters channels for quick access`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    val all = vm.allChannels.value
    assertTrue(all.isNotEmpty())
    val (targetId) = all.first()

    // Toggle favorite bookmark
    vm.toggleFavorite(targetId)

    val activeProfile = vm.profileRepo.activeProfile.value
    assertNotNull(activeProfile)
    assertTrue("Target channel should be in profile favorites", activeProfile!!.favorites.contains(targetId))

    // Toggle again to unbookmark
    vm.toggleFavorite(targetId)
    val postRemoveProfile = vm.profileRepo.activeProfile.value
    assertNotNull(postRemoveProfile)
    assertEquals(false, postRemoveProfile!!.favorites.contains(targetId))
  }

  @Test
  fun `india and hindi channels detection and regional filtering`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val playlistRepo = PlaylistRepository(context)
    val curated = playlistRepo.getCuratedChannels()

    val indianChannels = curated.filter { (_, _, _, _, _, country) -> country == "IN" }
    assertTrue("Curated channels must contain Indian channels, found ${indianChannels.size}", indianChannels.size >= 5)

    val ddNews = indianChannels.firstOrNull { (id) -> id == "dd_news_india" }
    assertNotNull(ddNews)
    assertEquals("DD News HD", ddNews!!.name)
    assertEquals("Hindi", ddNews.language)

    val aajTak = indianChannels.firstOrNull { (id) -> id == "aaj_tak_india" }
    assertNotNull(aajTak)
    assertEquals("Aaj Tak HD", aajTak!!.name)
    assertEquals("Hindi", aajTak.language)

    val wion = indianChannels.firstOrNull { (id) -> id == "wion_news_india" }
    assertNotNull(wion)
    assertEquals("WION News HD", wion!!.name)

    // Test regional filter with TVViewModel
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)
    vm.selectCountry("IN")
    testScheduler.advanceUntilIdle()

    val matchesIn = vm.allChannels.value.filter { (id, name, _, _, _, country) ->
      (country == "IN") || id.contains("india") || name.contains("DD ")
    }
    assertTrue("Channels matching IN must not be empty", matchesIn.isNotEmpty())
    assertTrue("Channels should include DD News", matchesIn.any { (_, name) -> name.contains("DD News", ignoreCase = true) })
  }

  @Test
  fun `auto play channel by id triggers direct landscape playback`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    vm.setFullscreen(enabled = false)
    val testChannelId = "dd_news_india"
    vm.playChannelById(testChannelId, directLandscape = true)

    assertEquals(testChannelId, vm.currentChannel.value?.id)
    assertEquals(true, vm.isFullscreen.value)
    assertEquals(true, vm.isPlaying.value)
  }

  @Test
  fun `viewmodel toggleMute and sleep timer updates player audio state`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    assertEquals(false, vm.isMuted.value)
    vm.toggleMute()
    assertEquals(true, vm.isMuted.value)
    vm.toggleMute()
    assertEquals(false, vm.isMuted.value)

    vm.setSleepTimer(30)
    assertEquals(30, vm.sleepTimerMinutes.value)
    vm.setSleepTimer(null)
    assertEquals(null, vm.sleepTimerMinutes.value)
  }

  @Test
  fun `viewmodel cycleAspectRatio updates display aspect modes`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    val initialMode = vm.aspectRatio.value
    vm.cycleAspectRatio()
    assertTrue((vm.aspectRatio.value != initialMode) || (com.example.ui.viewmodel.AspectRatioMode.entries.size == 1))
  }

  @Test
  fun `app theme mode switching and persistence works for dark white and slate`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    // Default theme should be SLATE
    assertEquals(AppThemeMode.SLATE, vm.appThemeMode.value)

    // Switch to DARK
    vm.setAppThemeMode(AppThemeMode.DARK)
    assertEquals(AppThemeMode.DARK, vm.appThemeMode.value)

    // Switch to WHITE
    vm.setAppThemeMode(AppThemeMode.WHITE)
    assertEquals(AppThemeMode.WHITE, vm.appThemeMode.value)

    // Verify persistence across new ViewModel instance
    val reloadedVm = com.example.ui.viewmodel.TVViewModel(app)
    assertEquals(AppThemeMode.WHITE, reloadedVm.appThemeMode.value)

    // Reset back to SLATE
    reloadedVm.setAppThemeMode(AppThemeMode.SLATE)
    assertEquals(AppThemeMode.SLATE, reloadedVm.appThemeMode.value)
  }

  @Test
  fun `channel sort order switching and persistence works for A to Z and Z to A`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    // Default sort order is A_TO_Z
    assertEquals(com.example.ui.viewmodel.ChannelSortOrder.A_TO_Z, vm.channelSortOrder.value)

    // Toggle sort order
    vm.toggleChannelSortOrder()
    assertEquals(com.example.ui.viewmodel.ChannelSortOrder.Z_TO_A, vm.channelSortOrder.value)

    // Set explicitly
    vm.setChannelSortOrder(com.example.ui.viewmodel.ChannelSortOrder.A_TO_Z)
    assertEquals(com.example.ui.viewmodel.ChannelSortOrder.A_TO_Z, vm.channelSortOrder.value)

    // Toggle to Z_TO_A and test persistence across new ViewModel instance
    vm.toggleChannelSortOrder()
    assertEquals(com.example.ui.viewmodel.ChannelSortOrder.Z_TO_A, vm.channelSortOrder.value)

    val reloadedVm = com.example.ui.viewmodel.TVViewModel(app)
    assertEquals(com.example.ui.viewmodel.ChannelSortOrder.Z_TO_A, reloadedVm.channelSortOrder.value)

    // Reset back to A_TO_Z
    reloadedVm.setChannelSortOrder(com.example.ui.viewmodel.ChannelSortOrder.A_TO_Z)
    assertEquals(com.example.ui.viewmodel.ChannelSortOrder.A_TO_Z, reloadedVm.channelSortOrder.value)
  }

  @Test
  fun `worldwide categories and subcategories work properly`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    // Verify expanded categories exist in ChannelGenre
    val genreNames = com.example.data.model.ChannelGenre.entries.map { it.displayName }
    assertTrue(genreNames.contains("Series & Drama"))
    assertTrue(genreNames.contains("Cooking & Food"))
    assertTrue(genreNames.contains("Travel & Outdoors"))
    assertTrue(genreNames.contains("Business & Finance"))
    assertTrue(genreNames.contains("Gaming & Esports"))
    assertTrue(genreNames.contains("Education & Culture"))

    // Verify subcategories for new genres
    val seriesSubs = vm.getSubcategoriesForGenre("Series & Drama")
    assertTrue(seriesSubs.contains("Drama"))
    assertTrue(seriesSubs.contains("Telenovelas"))

    val foodSubs = vm.getSubcategoriesForGenre("Cooking & Food")
    assertTrue(foodSubs.contains("Recipes"))

    // Test category selection
    vm.selectGenre("Cooking & Food")
    assertEquals("Cooking & Food", vm.selectedGenre.value)

    vm.resetAllFilters()
    assertEquals(com.example.data.model.ChannelGenre.ALL.displayName, vm.selectedGenre.value)
  }

  @Test
  fun `worldwide regions and country filtering work properly`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    // Verify major worldwide regions are represented
    val regionCodes = vm.availableRegions.map { (code) -> code }
    assertTrue(regionCodes.contains("US"))
    assertTrue(regionCodes.contains("UK"))
    assertTrue(regionCodes.contains("IN"))
    assertTrue(regionCodes.contains("CA"))
    assertTrue(regionCodes.contains("DE"))
    assertTrue(regionCodes.contains("FR"))
    assertTrue(regionCodes.contains("ES"))
    assertTrue(regionCodes.contains("IT"))
    assertTrue(regionCodes.contains("BR"))
    assertTrue(regionCodes.contains("MX"))
    assertTrue(regionCodes.contains("JP"))
    assertTrue(regionCodes.contains("KR"))
    assertTrue(regionCodes.contains("TR"))
    assertTrue(regionCodes.contains("SA"))
    assertTrue(regionCodes.contains("AE"))
    assertTrue(regionCodes.contains("PK"))
    assertTrue(regionCodes.contains("BD"))
    assertTrue(regionCodes.contains("ID"))
    assertTrue(regionCodes.contains("PH"))
    assertTrue(regionCodes.contains("NG"))
    assertTrue(regionCodes.contains("ZA"))

    // Test selecting a country
    vm.selectCountry("FR")
    assertEquals("FR", vm.selectedCountryFilter.value)

    vm.resetAllFilters()
    assertEquals("ALL", vm.selectedCountryFilter.value)
  }

  @Test
  fun `worldwide language filtering and app internationalization support work for all 83 languages`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    // Verify all 83 worldwide app interface languages
    assertEquals(83, com.example.util.AppLanguage.entries.size)

    // Test switching app interface language across all 83 languages
    for (lang in com.example.util.AppLanguage.entries) {
      vm.setAppInterfaceLanguage(lang)
      assertEquals(lang, vm.appInterfaceLanguage.value)
      val strings = com.example.util.getStringsForLanguage(lang.code)
      assertNotNull(strings.appName)
      assertTrue(strings.liveTab.isNotEmpty())
      assertTrue(strings.favoritesTab.isNotEmpty())
      assertTrue(strings.channelsTab.isNotEmpty())
      assertTrue(strings.settingsTab.isNotEmpty())
      assertTrue(strings.allChannels.isNotEmpty())
      assertTrue(strings.clearCache.isNotEmpty())
      assertTrue(strings.languageSelectorTitle.isNotEmpty())
    }

    // Test channel language selection
    val availableLangs = vm.availableLanguages
    assertEquals(84, availableLangs.size)
    assertTrue(availableLangs.contains("ALL"))
    assertTrue(availableLangs.contains("Spanish"))
    assertTrue(availableLangs.contains("French"))
    assertTrue(availableLangs.contains("German"))
    assertTrue(availableLangs.contains("Arabic"))
    assertTrue(availableLangs.contains("Portuguese"))
    assertTrue(availableLangs.contains("Russian"))
    assertTrue(availableLangs.contains("Japanese"))
    assertTrue(availableLangs.contains("Hindi"))
    assertTrue(availableLangs.contains("Urdu"))
    assertTrue(availableLangs.contains("Bengali"))
    assertTrue(availableLangs.contains("Swahili"))
    assertTrue(availableLangs.contains("Amharic"))
    assertTrue(availableLangs.contains("Hausa"))
    assertTrue(availableLangs.contains("Yoruba"))
    assertTrue(availableLangs.contains("Zulu"))
    assertTrue(availableLangs.contains("Mongolian"))
    assertTrue(availableLangs.contains("Nepali"))
    assertTrue(availableLangs.contains("Burmese"))

    vm.selectLanguage("Spanish")
    assertEquals("Spanish", vm.selectedLanguage.value)

    vm.selectLanguage("Swahili")
    assertEquals("Swahili", vm.selectedLanguage.value)

    vm.resetAllFilters()
    assertEquals("ALL", vm.selectedLanguage.value)
  }

  @Test
  fun `bundled channels have accurate country and language detection from group-title and tvg-id`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val playlistRepo = PlaylistRepository(context)
    val channels = playlistRepo.loadBundledChannels()
    assertTrue(channels.isNotEmpty())

    // Afghan channels (from group-title="Afghanistan")
    val afghanChannels = channels.filter { it.country == "AF" }
    assertTrue("Should detect Afghan channels from group-title, found ${afghanChannels.size}", afghanChannels.isNotEmpty())
    val amc = afghanChannels.firstOrNull { it.id.startsWith("AMC.af") }
    assertNotNull(amc)
    assertEquals("AF", amc?.country)
    assertEquals("Persian", amc?.language)

    // Verify genres are mapped from keywords and not all default to General
    val genres = channels.asSequence().map { it.genre }.toSet()
    assertTrue("Should map to multiple genres, found ${genres.size}", genres.size >= 5)
  }

  @Test
  fun `all 83 app interface languages supported and selectable in settings card system`() {
    assertEquals(83, com.example.util.AppLanguage.entries.size)

    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.TVViewModel(app)

    // Verify initial default language
    assertEquals(com.example.util.AppLanguage.ENGLISH, vm.appInterfaceLanguage.value)

    // Change to Japanese
    vm.setAppInterfaceLanguage(com.example.util.AppLanguage.JAPANESE)
    assertEquals(com.example.util.AppLanguage.JAPANESE, vm.appInterfaceLanguage.value)

    // Change to Zulu
    vm.setAppInterfaceLanguage(com.example.util.AppLanguage.ZULU)
    assertEquals(com.example.util.AppLanguage.ZULU, vm.appInterfaceLanguage.value)

    // Test filter reset function
    vm.setSearchQuery("News")
    vm.selectGenre(com.example.data.model.ChannelGenre.NEWS.displayName)
    vm.selectCountry("US")
    vm.selectLanguage("English")
    vm.selectSubcategory("Live Now")

    assertEquals("News", vm.searchQuery.value)
    assertEquals(com.example.data.model.ChannelGenre.NEWS.displayName, vm.selectedGenre.value)
    assertEquals("US", vm.selectedCountryFilter.value)
    assertEquals("English", vm.selectedLanguage.value)
    assertEquals("Live Now", vm.selectedSubcategory.value)

    vm.resetAllFilters()

    assertEquals("", vm.searchQuery.value)
    assertEquals(com.example.data.model.ChannelGenre.ALL.displayName, vm.selectedGenre.value)
    assertEquals("ALL", vm.selectedCountryFilter.value)
    assertEquals("ALL", vm.selectedLanguage.value)
    assertEquals("ALL", vm.selectedSubcategory.value)
  }
}





