package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ProgramEntity
import com.example.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class EpgRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val programDao = db.programDao()

    private val _isEpgSyncing = MutableStateFlow(false)
    val isEpgSyncing: StateFlow<Boolean> = _isEpgSyncing.asStateFlow()

    private val _lastEpgSyncTime = MutableStateFlow(0L)
    val lastEpgSyncTime: StateFlow<Long> = _lastEpgSyncTime.asStateFlow()

    fun getScheduleForChannel(channelId: String): Flow<List<ProgramEntity>> {
        return programDao.getScheduleForChannel(channelId)
    }

    fun getCurrentProgram(channelId: String, now: Long = System.currentTimeMillis()): Flow<ProgramEntity?> {
        return programDao.getCurrentProgram(channelId, now)
    }

    fun getAllCurrentPrograms(now: Long = System.currentTimeMillis()): Flow<List<ProgramEntity>> {
        return programDao.getAllCurrentPrograms(now)
    }

    fun getUpcomingPrograms(fromTime: Long = System.currentTimeMillis() - 1800_000L): Flow<List<ProgramEntity>> {
        return programDao.getUpcomingPrograms(fromTime)
    }

    suspend fun syncEpgForChannels(channels: List<Channel>) = withContext(Dispatchers.IO) {
        _isEpgSyncing.value = true
        try {
            val now = System.currentTimeMillis()
            // Clean up old programs ended over 6 hours ago
            programDao.deletePastPrograms(now - (6 * 3600 * 1000L))

            val count = programDao.getProgramCount()
            if (count > 50 && (now - _lastEpgSyncTime.value < 4 * 3600 * 1000L)) {
                _isEpgSyncing.value = false
                return@withContext
            }

            val programList = mutableListOf<ProgramEntity>()

            // Generate/compute scheduled program blocks for the channels (top 100 priority active channels)
            for (channel in channels.take(100)) {
                val channelPrograms = generateScheduleForChannel(channel, now)
                programList.addAll(channelPrograms)
            }

            if (programList.isNotEmpty()) {
                programDao.insertPrograms(programList)
                _lastEpgSyncTime.value = now
                Log.d("EpgRepository", "Successfully synced ${programList.size} EPG programs into Room")
            }
        } catch (e: Exception) {
            Log.e("EpgRepository", "EPG sync error", e)
        } finally {
            _isEpgSyncing.value = false
        }
    }

    private fun generateScheduleForChannel(channel: Channel, referenceTime: Long): List<ProgramEntity> {
        val programs = mutableListOf<ProgramEntity>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceTime
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, -2) // Start from 2 hours ago
        }

        val templates = getTemplatesForGenre(channel.genre, channel.name)
        var templateIndex = (channel.name.hashCode().coerceAtLeast(0)) % templates.size

        for (i in 0 until 12) { // 12 programs covering ~16-24 hours
            val start = cal.timeInMillis
            val template = templates[templateIndex % templates.size]
            val durationMinutes = template.duration
            cal.add(Calendar.MINUTE, durationMinutes)
            val end = cal.timeInMillis

            programs.add(
                ProgramEntity(
                    id = "${channel.id}_prog_${start}",
                    channelId = channel.id,
                    title = template.title,
                    description = template.description,
                    category = channel.genre,
                    startTime = start,
                    endTime = end,
                    durationMinutes = durationMinutes,
                    rating = template.rating
                )
            )

            templateIndex++
        }

        return programs
    }

    private data class ProgTemplate(
        val title: String,
        val description: String,
        val duration: Int,
        val rating: String = "TV-PG"
    )

    private fun getTemplatesForGenre(genre: String, channelName: String): List<ProgTemplate> {
        val g = genre.lowercase()
        return when {
            g.contains("news") -> listOf(
                ProgTemplate("Live Global Breaking News", "Real-time updates, world headlines, and correspondent reports from around the globe.", 60, "TV-G"),
                ProgTemplate("The International Hour", "In-depth investigative journalism, expert interviews, and political analysis.", 60, "TV-PG"),
                ProgTemplate("Markets & Economy Tonight", "Stock markets, corporate technology developments, and global economic forecasts.", 30, "TV-G"),
                ProgTemplate("World Perspective Panel", "Global experts discuss international diplomacy, climate, and key cultural events.", 60, "TV-PG"),
                ProgTemplate("Morning Headline Brief", "Quick highlights of essential world news before you begin your day.", 30, "TV-G"),
                ProgTemplate("Documentary Spotlight", "Comprehensive coverage of historical events shaping our modern world.", 60, "TV-PG")
            )
            g.contains("sport") -> listOf(
                ProgTemplate("Live Championship Action", "High stakes live coverage with play-by-play commentary and player telemetry.", 90, "TV-PG"),
                ProgTemplate("Matchday Highlights & Recap", "Analysis of today's best goals, passes, overtaking maneuvers, and key moments.", 45, "TV-G"),
                ProgTemplate("Behind The Helmet: Pro Athletes", "Exclusive interviews, training regimens, and locker room insights.", 45, "TV-PG"),
                ProgTemplate("Extreme Racing Championship", "Off-road and tarmac adrenaline-fueled racing action across scenic circuits.", 60, "TV-PG"),
                ProgTemplate("Sports Center Daily", "Comprehensive scores, transfer news, and injury reports across all leagues.", 30, "TV-G"),
                ProgTemplate("Classic Golden Match Replay", "A legendary match from sporting history restored in stunning high-definition.", 90, "TV-G")
            )
            g.contains("kid") || g.contains("anim") -> listOf(
                ProgTemplate("The Magic Wonder Forest", "Join curious animal companions on colorful puzzle-solving adventures.", 30, "TV-Y"),
                ProgTemplate("Space Cadet Explorers", "Animated journey through alien worlds filled with friendship and science curiosity.", 30, "TV-Y7"),
                ProgTemplate("Retro Cartoons Festival", "Timeless laughter with classic slapstick animated favorites.", 45, "TV-G"),
                ProgTemplate("Dino Island Quests", "Friendly prehistoric creatures learn team building and discover ancient secrets.", 30, "TV-Y"),
                ProgTemplate("Super Kid Heroes", "Young superheroes team up to protect their city and learn valuable life lessons.", 30, "TV-Y7"),
                ProgTemplate("Bedtime Storybook Animation", "Calming gentle musical tales designed to help kids unwind and relax.", 30, "TV-Y")
            )
            g.contains("movie") || g.contains("cinema") -> listOf(
                ProgTemplate("Prime Feature Film: The Horizon", "Acclaimed cinematic masterpiece with dramatic twists and stellar performances.", 120, "TV-14"),
                ProgTemplate("Action Thriller: Zero Hour", "Fast-paced secret agent adventure through international metropolises.", 105, "TV-14"),
                ProgTemplate("Sci-Fi Mystery: Nebula Awakening", "Colonists on a distant exoplanet uncover an ancient planetary mystery.", 110, "TV-PG"),
                ProgTemplate("Golden Hollywood Classic", "Restored vintage film noir featuring iconic screen legends.", 95, "TV-PG"),
                ProgTemplate("Behind the Scenes: Film Direction", "Director commentary and visual effects breakdowns of cinematic marvels.", 45, "TV-PG")
            )
            g.contains("music") -> listOf(
                ProgTemplate("Lofi Chill & Study Beats", "Smooth soothing beats and peaceful visuals for working, studying, and resting.", 90, "TV-G"),
                ProgTemplate("Global Top 40 Music Videos", "Chart-topping international hits and high-energy music video premieres.", 60, "TV-PG"),
                ProgTemplate("Live Festival DJ Set", "Electrifying electronic dance festival recorded live in front of 50,000 fans.", 75, "TV-14"),
                ProgTemplate("Acoustic Sessions Live", "Intimate unplugged performances and candid acoustic renditions.", 45, "TV-G")
            )
            g.contains("doc") -> listOf(
                ProgTemplate("Secrets of the Deep Cosmos", "Hubble and James Webb telescope captures revealing universe origins.", 60, "TV-G"),
                ProgTemplate("Wild Serengeti Chronicles", "The great migration captured with high-speed 4K ultra-telephoto cameras.", 50, "TV-PG"),
                ProgTemplate("Mega Engineering Marvels", "How monumental bridges, skyscrapers, and tunnels are designed and constructed.", 60, "TV-G"),
                ProgTemplate("Ancient Civilizations Rediscovered", "Archaeologists uncover forgotten tombs and forgotten royal dynasties.", 60, "TV-PG")
            )
            g.contains("adult") -> listOf(
                ProgTemplate("Late Night Mature Cinema", "Sensual cinematic entertainment for adult audiences.", 90, "TV-MA"),
                ProgTemplate("After Hours Showcase", "Nightclub style performances and mature themes.", 75, "TV-MA")
            )
            else -> listOf(
                ProgTemplate("Prime Time Live Broadcast", "Entertaining magazine format show featuring lifestyle, culture, and guests.", 60, "TV-PG"),
                ProgTemplate("Global Discovery & Travel", "Spectacular destinations and authentic culinary tours around the world.", 45, "TV-G"),
                ProgTemplate("The Evening Variety Hour", "Comedy, music guests, and lighthearted discussion of trending topics.", 60, "TV-PG"),
                ProgTemplate("Spotlight Special Feature", "A closer look at creative innovators and extraordinary human achievements.", 45, "TV-G")
            )
        }
    }
}
