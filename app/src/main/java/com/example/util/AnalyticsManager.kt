package com.example.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsManager {
    private const val TAG = "AnalyticsManager"
    @Volatile
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (firebaseAnalytics != null) return
        runCatching {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext).apply {
                setAnalyticsCollectionEnabled(true)
            }
            Log.d(TAG, "Firebase Analytics initialized")
        }.onFailure { e ->
            Log.w(TAG, "Firebase Analytics unavailable in current environment: ${e.message}")
        }
    }

    fun logScreenView(screenName: String) {
        runCatching {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }
    }

    fun logChannelPlay(channelId: String, channelName: String, genre: String, country: String) {
        runCatching {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, channelId.take(100))
                putString(FirebaseAnalytics.Param.ITEM_NAME, channelName.take(100))
                putString(FirebaseAnalytics.Param.ITEM_CATEGORY, genre.take(100))
                putString("country", country.take(40))
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        }
    }

    fun logSearch(query: String) {
        if (query.isBlank()) return
        runCatching {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SEARCH_TERM, query.take(100))
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        }
    }

    fun logFavoriteToggle(channelId: String) {
        runCatching {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, channelId.take(100))
            }
            firebaseAnalytics?.logEvent("toggle_favorite", bundle)
        }
    }
}
