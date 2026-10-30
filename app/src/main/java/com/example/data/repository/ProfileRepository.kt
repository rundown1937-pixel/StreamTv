package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ActivityLog
import com.example.data.model.UserProfile
import com.example.data.model.WatchHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProfileRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stream_pulse_profiles", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROFILES = "profiles_json"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_PARENTAL_PIN = "parental_pin"
        private const val KEY_ACTIVITY_LOGS = "activity_logs_json"
        private const val KEY_LOCKED_CHANNELS_GLOBAL = "locked_channels_global"
        const val DEFAULT_PIN = "1234"
    }

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<UserProfile?>(null)
    val activeProfile: StateFlow<UserProfile?> = _activeProfile.asStateFlow()

    private val _parentalPin = MutableStateFlow(
        prefs.getString(KEY_PARENTAL_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    )
    val parentalPin: StateFlow<String> = _parentalPin.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    private val _globallyLockedChannelIds = MutableStateFlow<Set<String>>(emptySet())
    val globallyLockedChannelIds: StateFlow<Set<String>> = _globallyLockedChannelIds.asStateFlow()

    init {
        loadLockedChannels()
        loadActivityLogs()
        loadProfiles()
    }

    private fun loadLockedChannels() {
        val set = prefs.getStringSet(KEY_LOCKED_CHANNELS_GLOBAL, setOf("night_club_afterdark")) ?: setOf("night_club_afterdark")
        _globallyLockedChannelIds.value = set
    }

    private fun loadActivityLogs() {
        val raw = prefs.getString(KEY_ACTIVITY_LOGS, null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<ActivityLog>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ActivityLog(
                            id = obj.getString("id"),
                            profileName = obj.getString("profileName"),
                            action = obj.getString("action"),
                            channelName = if (obj.has("channelName") && !obj.isNull("channelName")) obj.getString("channelName") else null,
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
                _activityLogs.value = list
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Default initial logs
        _activityLogs.value = listOf(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                profileName = "Main Family",
                action = "Parental Controls Enabled (Default PIN: 1234)",
                timestamp = System.currentTimeMillis() - 86400000
            ),
            ActivityLog(
                id = UUID.randomUUID().toString(),
                profileName = "Kids TV",
                action = "Child Safe Mode Activated",
                timestamp = System.currentTimeMillis() - 3600000
            )
        )
    }

    private fun loadProfiles() {
        val raw = prefs.getString(KEY_PROFILES, null)
        val list = mutableListOf<UserProfile>()

        if (raw != null) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val favArray = obj.optJSONArray("favorites")
                    val favs = mutableListOf<String>()
                    if (favArray != null) {
                        for (j in 0 until favArray.length()) favs.add(favArray.getString(j))
                    }

                    val histArray = obj.optJSONArray("watchHistory")
                    val history = mutableListOf<WatchHistoryEntry>()
                    if (histArray != null) {
                        for (k in 0 until histArray.length()) {
                            val hObj = histArray.getJSONObject(k)
                            history.add(
                                WatchHistoryEntry(
                                    channelId = hObj.getString("channelId"),
                                    channelName = hObj.getString("channelName"),
                                    timestamp = hObj.getLong("timestamp"),
                                    durationSeconds = hObj.optLong("durationSeconds", 0L)
                                )
                            )
                        }
                    }

                    list.add(
                        UserProfile(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            avatarEmoji = obj.optString("avatarEmoji", "🎬"),
                            avatarBgColorHex = obj.optLong("avatarBgColorHex", 0xFF6366F1),
                            isKidsProfile = obj.optBoolean("isKidsProfile", false),
                            favorites = favs,
                            watchHistory = history
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (list.isEmpty()) {
            // Seed default family profiles
            list.addAll(
                listOf(
                    UserProfile(
                        id = "profile_main",
                        name = "Master Family",
                        avatarEmoji = "👑",
                        avatarBgColorHex = 0xFF6366F1,
                        isKidsProfile = false,
                        favorites = listOf("bbc_news_global", "redbull_tv", "retro_cinema_classic")
                    ),
                    UserProfile(
                        id = "profile_kids",
                        name = "Junior Explorer",
                        avatarEmoji = "🦁",
                        avatarBgColorHex = 0xFF10B981,
                        isKidsProfile = true,
                        favorites = listOf("pbs_kids", "duck_tv_kids", "toon_classics")
                    ),
                    UserProfile(
                        id = "profile_cinema",
                        name = "Movie Buff",
                        avatarEmoji = "🍿",
                        avatarBgColorHex = 0xFFF59E0B,
                        isKidsProfile = false,
                        favorites = listOf("retro_cinema_classic", "filmrise_action")
                    ),
                    UserProfile(
                        id = "profile_sports",
                        name = "Sports Fan",
                        avatarEmoji = "⚡",
                        avatarBgColorHex = 0xFFEF4444,
                        isKidsProfile = false,
                        favorites = listOf("redbull_tv", "sports_grid")
                    )
                )
            )
            saveProfiles(list)
        }

        _profiles.value = list
        val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, list.first().id)
        _activeProfile.value = list.firstOrNull { it.id == activeId } ?: list.first()
    }

    private fun saveProfiles(list: List<UserProfile>) {
        try {
            val array = JSONArray()
            for (p in list) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("avatarEmoji", p.avatarEmoji)
                obj.put("avatarBgColorHex", p.avatarBgColorHex)
                obj.put("isKidsProfile", p.isKidsProfile)

                val favArray = JSONArray()
                for (f in p.favorites) favArray.put(f)
                obj.put("favorites", favArray)

                val histArray = JSONArray()
                for (h in p.watchHistory) {
                    val hObj = JSONObject()
                    hObj.put("channelId", h.channelId)
                    hObj.put("channelName", h.channelName)
                    hObj.put("timestamp", h.timestamp)
                    hObj.put("durationSeconds", h.durationSeconds)
                    histArray.put(hObj)
                }
                obj.put("watchHistory", histArray)

                array.put(obj)
            }
            prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setActiveProfile(profile: UserProfile) {
        _activeProfile.value = profile
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profile.id).apply()
        logActivity("Switched to Profile: ${profile.name}")
    }

    fun toggleFavorite(channelId: String) {
        val current = _activeProfile.value ?: return
        val currentFavs = current.favorites.toMutableList()
        if (currentFavs.contains(channelId)) {
            currentFavs.remove(channelId)
        } else {
            currentFavs.add(channelId)
        }
        val updated = current.copy(favorites = currentFavs)
        updateProfile(updated)
    }

    fun recordWatchHistory(channelId: String, channelName: String, durationSeconds: Long = 60) {
        val current = _activeProfile.value ?: return
        val currentHistory = current.watchHistory.toMutableList()
        // Remove prior entry if existing to push to top
        currentHistory.removeAll { it.channelId == channelId }
        currentHistory.add(
            0,
            WatchHistoryEntry(
                channelId = channelId,
                channelName = channelName,
                timestamp = System.currentTimeMillis(),
                durationSeconds = durationSeconds
            )
        )
        // Keep last 30
        val trimmed = currentHistory.take(30)
        val updated = current.copy(watchHistory = trimmed)
        updateProfile(updated)
        logActivity("Watched $channelName", channelName)
    }

    fun updateProfile(updated: UserProfile) {
        val list = _profiles.value.map { if (it.id == updated.id) updated else it }
        _profiles.value = list
        if (_activeProfile.value?.id == updated.id) {
            _activeProfile.value = updated
        }
        saveProfiles(list)
    }

    fun createProfile(name: String, emoji: String, isKids: Boolean, colorHex: Long) {
        val newProfile = UserProfile(
            id = "profile_${System.currentTimeMillis()}",
            name = name.trim().ifEmpty { "Family Member" },
            avatarEmoji = emoji,
            avatarBgColorHex = colorHex,
            isKidsProfile = isKids
        )
        val list = _profiles.value + newProfile
        _profiles.value = list
        saveProfiles(list)
        logActivity("Created profile: ${newProfile.name}")
    }

    fun deleteProfile(profileId: String) {
        if (_profiles.value.size <= 1) return // Keep at least one
        val list = _profiles.value.filterNot { it.id == profileId }
        _profiles.value = list
        if (_activeProfile.value?.id == profileId) {
            _activeProfile.value = list.first()
            prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, list.first().id).apply()
        }
        saveProfiles(list)
        logActivity("Deleted profile")
    }

    fun setParentalPin(newPin: String): Boolean {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            _parentalPin.value = newPin
            prefs.edit().putString(KEY_PARENTAL_PIN, newPin).apply()
            logActivity("Parental PIN Updated")
            return true
        }
        return false
    }

    fun verifyPin(pinInput: String): Boolean {
        val matches = pinInput == _parentalPin.value
        if (matches) {
            logActivity("Parental PIN Verified Successfully")
        } else {
            logActivity("Failed PIN Attempt")
        }
        return matches
    }

    fun toggleChannelLock(channelId: String) {
        val current = _globallyLockedChannelIds.value.toMutableSet()
        if (current.contains(channelId)) {
            current.remove(channelId)
            logActivity("Channel Unlocked from Parental Lock", channelId)
        } else {
            current.add(channelId)
            logActivity("Channel Locked with Parental PIN", channelId)
        }
        _globallyLockedChannelIds.value = current
        prefs.edit().putStringSet(KEY_LOCKED_CHANNELS_GLOBAL, current).apply()
    }

    fun isChannelLocked(channelId: String, isAdultOrRestricted: Boolean): Boolean {
        val active = _activeProfile.value
        // If profile is Kids, always lock adult/restricted or locked channels
        if (active?.isKidsProfile == true) {
            if (isAdultOrRestricted) return true
        }
        return _globallyLockedChannelIds.value.contains(channelId) || isAdultOrRestricted
    }

    fun logActivity(action: String, channelName: String? = null) {
        val log = ActivityLog(
            id = UUID.randomUUID().toString(),
            profileName = _activeProfile.value?.name ?: "Guest",
            action = action,
            channelName = channelName,
            timestamp = System.currentTimeMillis()
        )
        val updated = (listOf(log) + _activityLogs.value).take(100)
        _activityLogs.value = updated
        saveActivityLogs(updated)
    }

    private fun saveActivityLogs(logs: List<ActivityLog>) {
        try {
            val array = JSONArray()
            for (l in logs) {
                val obj = JSONObject()
                obj.put("id", l.id)
                obj.put("profileName", l.profileName)
                obj.put("action", l.action)
                if (l.channelName != null) obj.put("channelName", l.channelName)
                obj.put("timestamp", l.timestamp)
                array.put(obj)
            }
            prefs.edit().putString(KEY_ACTIVITY_LOGS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
