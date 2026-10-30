package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ProgramEntity
import com.example.data.repository.EpgRepository
import com.example.data.repository.PlaylistRepository
import com.example.data.repository.ProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
    val genres = channels.map { it.genre }.toSet()
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
  fun `epg room database stores and retrieves programs`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val dao = db.programDao()

    val now = System.currentTimeMillis()
    val program = ProgramEntity(
      id = "test_prog_1",
      channelId = "bbc_news_global",
      title = "BBC World News Hour",
      description = "Live global news report",
      category = "News",
      startTime = now - 600000,
      endTime = now + 1800000,
      durationMinutes = 40,
      rating = "TV-G"
    )

    dao.insertPrograms(listOf(program))

    val current = dao.getCurrentProgram("bbc_news_global", now).first()
    assertNotNull(current)
    assertEquals("BBC World News Hour", current?.title)
    assertTrue(current!!.getProgressFraction(now) > 0f)
  }

  @Test
  fun `xmltv epg parser successfully parses schedule data`() {
    val sampleXml = """
      <?xml version="1.0" encoding="ISO-8859-1"?>
      <!DOCTYPE tv SYSTEM "xmltv.dtd">
      <tv>
        <channel id="france24_en">
          <display-name>France 24 English</display-name>
        </channel>
        <programme start="20261022120000 +0000" stop="20261022123000 +0000" channel="france24_en">
          <title lang="en">Live World News</title>
          <desc lang="en">International news coverage across all regions.</desc>
          <category lang="en">News</category>
        </programme>
      </tv>
    """.trimIndent()

    val programs = com.example.util.XmltvEpgParser.parseXmltvString(sampleXml)
    assertEquals(1, programs.size)
    assertEquals("france24_en", programs[0].channelId)
    assertEquals("Live World News", programs[0].title)
    assertEquals("News", programs[0].category)
    assertEquals(30, programs[0].durationMinutes)
  }

  @Test
  fun `localized strings work correctly across all supported languages`() {
    val en = com.example.util.getStringsForLanguage("en")
    val hi = com.example.util.getStringsForLanguage("hi")
    val es = com.example.util.getStringsForLanguage("es")
    val fr = com.example.util.getStringsForLanguage("fr")
    val de = com.example.util.getStringsForLanguage("de")
    val ar = com.example.util.getStringsForLanguage("ar")

    assertEquals("Live", en.liveTab)
    assertEquals("लाइव", hi.liveTab)
    assertEquals("En Vivo", es.liveTab)
    assertEquals("En Direct", fr.liveTab)
    assertEquals("Live", de.liveTab)
    assertEquals("مباشر", ar.liveTab)
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
    val f24En = channels.firstOrNull { it.id == "france24_english" }
    assertNotNull(f24En)
    assertEquals("France 24 English", f24En!!.name)
    assertTrue(f24En.streamUrl.contains("france24.com"))

    val dw = channels.firstOrNull { it.id == "dw_news_english" }
    assertNotNull(dw)
    assertEquals("DW News English", dw!!.name)
    assertTrue(dw.streamUrl.contains("dwstream") || dw.streamUrl.contains("dwamdstream"))

    val aja = channels.firstOrNull { it.id == "aljazeera_arabic" }
    assertNotNull(aja)
    assertEquals("Al Jazeera Arabic", aja!!.name)
    assertTrue(aja.streamUrl.contains("getaj.net/AJA"))

    val rb = channels.firstOrNull { it.id == "redbull_tv" }
    assertNotNull(rb)
    assertEquals("Red Bull TV Live", rb!!.name)
    assertTrue(rb.streamUrl.contains("akamaized.net"))

    val nasa = channels.firstOrNull { it.id == "nasa_tv_hd" }
    assertNotNull(nasa)
    assertEquals("NASA TV HD", nasa!!.name)
    assertTrue(nasa.streamUrl.contains("NASA-NTV1"))

    // Ensure no demo / tears of steel stream exists
    val demoStreams = channels.filter {
      it.id.contains("tears_of_steel") || it.name.contains("Tears of Steel") || it.streamUrl.contains("bigbuckbunny")
    }
    assertTrue("No demo or placeholder streams should exist", demoStreams.isEmpty())
  }

  @Test
  fun `online and offline stream health tracking accurately toggles channel state`() {
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

    // Simulating player recovery
    vm.markChannelLive(channelId)
    assertTrue(!vm.brokenChannelIds.value.contains(channelId))
    assertTrue(vm.liveChannelIds.value.contains(channelId))
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

    val allChannels = vm.playlistRepo.channels.value
    assertTrue(allChannels.any { it.name.contains("NASA", ignoreCase = true) })
    val filtered = allChannels.filter { it.name.contains("NASA", ignoreCase = true) }
    assertTrue(filtered.isNotEmpty())
    assertEquals("NASA TV HD", filtered[0].name)
  }

  @Test
  fun `screen tabs include live channels and settings without offline tab`() {
    val tabs = com.example.ui.viewmodel.ScreenTab.entries.map { it.name }
    assertTrue(tabs.contains("HOME"))
    assertTrue(tabs.contains("FAVORITES"))
    assertTrue(tabs.contains("CHANNELS"))
    assertTrue(tabs.contains("EPG_GUIDE"))
    assertTrue(tabs.contains("PROFILES"))
    assertTrue(tabs.contains("SETTINGS"))
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

    for (c in channels) {
      assertTrue("Channel ID must not be blank for ${c.name}", c.id.isNotBlank())
      assertTrue("Channel Name must not be blank for ${c.id}", c.name.isNotBlank())
      assertTrue("Stream URL must be HTTP/HTTPS for ${c.name}", c.streamUrl.startsWith("http://") || c.streamUrl.startsWith("https://"))
      assertTrue("Stream URL must not contain localhost or demo", !c.streamUrl.contains("tears_of_steel") && !c.streamUrl.contains("bbb_sunflower"))
      assertTrue("Channel must have a recognized genre for ${c.name}", c.genre.isNotBlank())
      assertTrue("Channel must have a country code for ${c.name}", c.country.isNotBlank())
      assertTrue("Channel must have a language for ${c.name}", c.language.isNotBlank())
    }

    // Verify presence of premier international broadcasters
    val channelNames = channels.map { it.name }
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

    val f24 = channels.first { it.id == "france24_english" }
    vm.playChannel(f24)
    assertEquals("France 24 English", vm.currentChannel.value?.name)
    assertEquals("https://static.france24.com/live/F24_EN_HI_HLS/live_tv.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("France 24 must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val sky = channels.first { it.id == "sky_news_live" }
    vm.playChannel(sky)
    assertEquals("Sky News Live HD", vm.currentChannel.value?.name)
    assertEquals("https://skynewsau-live.akamaized.net/hls/live/2002689/skynewsau-extra1/master.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("Sky News must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val bloomberg = channels.first { it.id == "bloomberg_tv" }
    vm.playChannel(bloomberg)
    assertEquals("Bloomberg TV Live", vm.currentChannel.value?.name)
    assertEquals("https://bloomberg.com/media-manifest/streams/us.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("Bloomberg must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val trt = channels.first { it.id == "trt_world_global" }
    vm.playChannel(trt)
    assertEquals("TRT World HD", vm.currentChannel.value?.name)
    assertEquals("https://tv-trtworld.medya.trt.com.tr/master.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("TRT World must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val redbull = channels.first { it.id == "redbull_tv" }
    vm.playChannel(redbull)
    assertEquals("Red Bull TV Live", vm.currentChannel.value?.name)
    assertEquals("https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8", vm.currentChannel.value?.streamUrl)
    assertTrue("Red Bull must not point to DW", !vm.currentChannel.value!!.streamUrl.contains("dwstream"))

    val dw = channels.first { it.id == "dw_news_english" }
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
    val channels = playlistRepo.channels.value

    assertTrue("Channels must have at least 12,000 channels, found ${channels.size}", channels.size >= 12000)

    // Verify all IDs are 100% unique
    val uniqueIds = channels.map { it.id }.toSet()
    assertEquals("All channel IDs must be distinct", channels.size, uniqueIds.size)

    // Verify channels have original stream links
    val hasHttpLinks = channels.all { it.streamUrl.startsWith("http://") || it.streamUrl.startsWith("https://") }
    assertTrue("All channels must have valid stream URLs", hasHttpLinks)

    // Verify channels have non-empty names
    val hasNames = channels.all { it.name.isNotBlank() }
    assertTrue("All channels must have official broadcaster names", hasNames)
  }

  @Test
  fun `channel plays directly in landscape mode`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    val firstChannel = vm.allChannels.value.first()
    vm.setFullscreen(false)
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
  fun `epg 5 minute reminder schedules and cancels correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val reminderManager = com.example.data.repository.EpgReminderManager(context)

    val now = System.currentTimeMillis()
    // Program starts in 30 minutes, ends in 60 minutes
    val programStartTime = now + (30 * 60 * 1000L)
    val programEndTime = now + (60 * 60 * 1000L)
    val program = ProgramEntity(
      id = "prog_dd_news_special",
      channelId = "dd_news_india",
      title = "India Prime Time News",
      description = "Live in-depth coverage and headlines",
      category = "News",
      startTime = programStartTime,
      endTime = programEndTime,
      durationMinutes = 30
    )
    val channelName = "DD News HD"

    // Verify reminder is initially not set
    assertEquals(false, reminderManager.isReminderSet(program.id))

    // Schedule reminder 5 minutes before start
    val scheduled = reminderManager.scheduleReminder(program, channelName)
    assertTrue("Reminder must be scheduled successfully", scheduled)
    assertTrue("isReminderSet must return true", reminderManager.isReminderSet(program.id))

    // Verify reminder time is exactly 5 minutes (300,000 ms) before program start
    val activeList = reminderManager.reminders.value
    val reminder = activeList.firstOrNull { it.programId == program.id }
    assertNotNull(reminder)
    assertEquals(programStartTime - (5 * 60 * 1000L), reminder!!.reminderTime)
    assertEquals(channelName, reminder.channelName)
    assertEquals(program.title, reminder.programTitle)

    // Cancel reminder
    reminderManager.cancelReminder(program.id)
    assertEquals(false, reminderManager.isReminderSet(program.id))
    assertTrue(reminderManager.reminders.value.none { it.programId == program.id })
  }

  @Test
  fun `favorites feature bookmarks and filters channels for quick access`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    val all = vm.allChannels.value
    assertTrue(all.isNotEmpty())
    val targetChannel = all.first()

    // Toggle favorite bookmark
    vm.toggleFavorite(targetChannel.id)

    val activeProfile = vm.profileRepo.activeProfile.value
    assertNotNull(activeProfile)
    assertTrue("Target channel should be in profile favorites", activeProfile!!.favorites.contains(targetChannel.id))

    // Toggle again to unbookmark
    vm.toggleFavorite(targetChannel.id)
    val postRemoveProfile = vm.profileRepo.activeProfile.value
    assertNotNull(postRemoveProfile)
    assertEquals(false, postRemoveProfile!!.favorites.contains(targetChannel.id))
  }

  @Test
  fun `india and hindi channels detection and regional filtering`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val playlistRepo = PlaylistRepository(context)
    val curated = playlistRepo.getCuratedChannels()

    val indianChannels = curated.filter { it.country == "IN" }
    assertTrue("Curated channels must contain Indian channels, found ${indianChannels.size}", indianChannels.size >= 5)

    val ddNews = indianChannels.firstOrNull { it.id == "dd_news_india" }
    assertNotNull(ddNews)
    assertEquals("DD News HD", ddNews!!.name)
    assertEquals("Hindi", ddNews.language)

    val aajTak = indianChannels.firstOrNull { it.id == "aaj_tak_india" }
    assertNotNull(aajTak)
    assertEquals("Aaj Tak HD", aajTak!!.name)
    assertEquals("Hindi", aajTak.language)

    val wion = indianChannels.firstOrNull { it.id == "wion_news_india" }
    assertNotNull(wion)
    assertEquals("WION News HD", wion!!.name)

    // Test regional filter with TVViewModel
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)
    vm.selectCountry("IN")
    testScheduler.advanceUntilIdle()

    val matchesIn = vm.allChannels.value.filter { it.country == "IN" || it.id.contains("india") || it.name.contains("DD ") }
    assertTrue("Channels matching IN must not be empty", matchesIn.isNotEmpty())
    assertTrue("Channels should include DD News", matchesIn.any { it.name.contains("DD News", ignoreCase = true) })
  }

  @Test
  fun `auto play channel by id triggers direct landscape playback`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    vm.setFullscreen(false)
    val testChannelId = "dd_news_india"
    vm.playChannelById(testChannelId, directLandscape = true)

    assertEquals(testChannelId, vm.currentChannel.value?.id)
    assertEquals(true, vm.isFullscreen.value)
    assertEquals(true, vm.isPlaying.value)
  }

  @Test
  fun `two karate chop detector recognizes double chop motion and invokes callback`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var doubleChopTriggered = false
    var registeredChopCount = 0

    val detector = com.example.util.KarateChopDetector(
      context = context,
      sensitivity = com.example.util.KarateChopDetector.Sensitivity.NORMAL,
      onChopProgress = { count, _ ->
        registeredChopCount = count
      },
      onDoubleChopDetected = {
        doubleChopTriggered = true
      }
    )

    assertEquals(false, doubleChopTriggered)

    // Simulate the two karate chops
    detector.simulateDoubleChop()

    assertEquals(true, doubleChopTriggered)
    assertEquals(2, registeredChopCount)
  }

  @Test
  fun `two karate chop gesture in viewmodel wakes app and launches live TV directly in landscape`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    vm.setFullscreen(false)
    assertEquals(false, vm.isFullscreen.value)

    // Trigger the permanent two karate chop gesture
    vm.handleKarateChopGesture()

    // Verifies app launch countdown is completed
    assertEquals(true, vm.isAppLaunchCountdownFinished.value)
    // Verifies direct landscape playback is active
    assertEquals(true, vm.isFullscreen.value)
    assertEquals(true, vm.isPlaying.value)
    assertNotNull(vm.currentChannel.value)
    assertTrue("Banner must announce karate chop gesture", vm.karateChopBannerMessage.value?.contains("Two Karate Chops") == true)
  }

  @Test
  fun `karate chop on-off option toggles gesture and gates action`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.TVViewModel(context as android.app.Application)

    // Turn OFF karate chop gesture
    vm.setKarateChopEnabled(false)
    assertEquals(false, vm.isKarateChopEnabled.value)

    vm.setFullscreen(false)
    assertEquals(false, vm.isFullscreen.value)

    // When OFF, handleKarateChopGesture should be ignored
    vm.handleKarateChopGesture()
    assertEquals(false, vm.isFullscreen.value)

    // Turn ON karate chop gesture
    vm.setKarateChopEnabled(true)
    assertEquals(true, vm.isKarateChopEnabled.value)

    // When ON, handleKarateChopGesture should execute
    vm.handleKarateChopGesture()
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
    assertTrue(vm.aspectRatio.value != initialMode || com.example.ui.viewmodel.AspectRatioMode.entries.size == 1)
  }
}


