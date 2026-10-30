package com.example.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.data.local.ProgramEntity
import com.example.data.model.ProgramReminder
import com.example.receiver.EpgReminderReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class EpgReminderManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stream_pulse_reminders", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_REMINDERS_JSON = "reminders_json"
        const val REMINDER_OFFSET_MS = 5 * 60 * 1000L // 5 minutes before program starts
    }

    private val _reminders = MutableStateFlow<List<ProgramReminder>>(emptyList())
    val reminders: StateFlow<List<ProgramReminder>> = _reminders.asStateFlow()

    init {
        loadReminders()
        cleanupExpiredReminders()
    }

    private fun loadReminders() {
        val raw = prefs.getString(KEY_REMINDERS_JSON, null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<ProgramReminder>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProgramReminder(
                        programId = obj.getString("programId"),
                        channelId = obj.getString("channelId"),
                        channelName = obj.getString("channelName"),
                        programTitle = obj.getString("programTitle"),
                        programStartTime = obj.getLong("programStartTime"),
                        reminderTime = obj.getLong("reminderTime"),
                        category = obj.optString("category", "General")
                    )
                )
            }
            _reminders.value = list
        } catch (e: Exception) {
            Log.e("EpgReminderManager", "Failed to load reminders", e)
        }
    }

    private fun saveReminders(list: List<ProgramReminder>) {
        try {
            val array = JSONArray()
            for (r in list) {
                val obj = JSONObject()
                obj.put("programId", r.programId)
                obj.put("channelId", r.channelId)
                obj.put("channelName", r.channelName)
                obj.put("programTitle", r.programTitle)
                obj.put("programStartTime", r.programStartTime)
                obj.put("reminderTime", r.reminderTime)
                obj.put("category", r.category)
                array.put(obj)
            }
            prefs.edit().putString(KEY_REMINDERS_JSON, array.toString()).apply()
            _reminders.value = list
        } catch (e: Exception) {
            Log.e("EpgReminderManager", "Failed to save reminders", e)
        }
    }

    fun isReminderSet(programId: String): Boolean {
        return _reminders.value.any { it.programId == programId }
    }

    fun scheduleReminder(program: ProgramEntity, channelName: String): Boolean {
        val now = System.currentTimeMillis()
        val targetReminderTime = program.startTime - REMINDER_OFFSET_MS

        val actualTriggerTime = if (targetReminderTime > now) {
            targetReminderTime
        } else if (program.endTime > now) {
            // Program is starting within 5 mins or already in progress
            now + 2000L
        } else {
            // Already ended
            return false
        }

        val reminder = ProgramReminder(
            programId = program.id,
            channelId = program.channelId,
            channelName = channelName,
            programTitle = program.title,
            programStartTime = program.startTime,
            reminderTime = actualTriggerTime,
            category = program.category
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val intent = Intent(context, EpgReminderReceiver::class.java).apply {
                putExtra(EpgReminderReceiver.EXTRA_PROGRAM_ID, program.id)
                putExtra(EpgReminderReceiver.EXTRA_PROGRAM_TITLE, program.title)
                putExtra(EpgReminderReceiver.EXTRA_CHANNEL_ID, program.channelId)
                putExtra(EpgReminderReceiver.EXTRA_CHANNEL_NAME, channelName)
                putExtra(EpgReminderReceiver.EXTRA_START_TIME, program.startTime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                program.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        actualTriggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        actualTriggerTime,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // If exact alarm permission is restricted, fallback to standard inexact alarm
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    actualTriggerTime,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e("EpgReminderManager", "Failed to set alarm", e)
            }
        }

        val updated = _reminders.value.filter { it.programId != program.id } + reminder
        saveReminders(updated)
        return true
    }

    fun cancelReminder(programId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val intent = Intent(context, EpgReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                programId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        val updated = _reminders.value.filter { it.programId != programId }
        saveReminders(updated)
    }

    fun cleanupExpiredReminders() {
        val cutoff = System.currentTimeMillis() - (2 * 3600 * 1000L) // 2 hours after start
        val valid = _reminders.value.filter { it.programStartTime > cutoff }
        if (valid.size != _reminders.value.size) {
            saveReminders(valid)
        }
    }
}
