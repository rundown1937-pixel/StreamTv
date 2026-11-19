package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val genre: String = "General",
    val country: String = "US",
    val language: String = "English",
    val isLocked: Boolean = false,
    val isFavorite: Boolean = false,
    val resolution: String = "1080p",
    val isAdultOrRestricted: Boolean = false,
    val description: String = "",
    val httpUserAgent: String? = null,
    val httpReferrer: String? = null,
    val isActive: Boolean = false,
    val isVerified: Boolean = false,
)

enum class ChannelGenre(val displayName: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    NEWS("News"),
    SPORTS("Sports"),
    MOVIES("Movies"),
    SERIES("Series & Drama"),
    ENTERTAINMENT("Entertainment"),
    MUSIC("Music & Radio"),
    KIDS("Kids & Animation"),
    DOCUMENTARY("Documentary"),
    LIFESTYLE("Lifestyle"),
    FOOD("Cooking & Food"),
    TRAVEL("Travel & Outdoors"),
    COMEDY("Comedy"),
    SCIENCE("Science & Tech"),
    EDUCATION("Education & Culture"),
    BUSINESS("Business & Finance"),
    GAMING("Gaming & Esports"),
    RELIGIOUS("Religious & Spiritual"),
    REGIONAL("Regional & Local"),
    CLASSIC("Classic & Retro"),
    GENERAL("General"),
}

fun Channel.isOfflineMetadata(): Boolean {
    val n = name.lowercase(java.util.Locale.ROOT)
    val u = streamUrl.trim()
    val d = description.lowercase(java.util.Locale.ROOT)
    return n.contains("[not 24/7]") || n.contains("(not 24/7)") ||
           n.contains("not 24/7") ||
           n.contains("[offline]") || n.contains("(offline)") ||
           n.contains("[inactive]") || n.contains("(inactive)") ||
           n.contains("[geo-blocked]") || n.contains("(geo-blocked)") ||
           n.contains("geo-blocked") ||
           n.contains("[down]") || n.contains("(down)") ||
           n.contains("[dead]") || n.contains("(dead)") ||
           n.contains("[closed]") || n.contains("(closed)") ||
           n.contains("[blocked]") || n.contains("(blocked)") ||
           n.contains("[unavailable]") || n.contains("(unavailable)") ||
           n.contains("[temp off]") || n.contains("(temp off)") ||
           n.contains("[test]") || n.contains("(test)") ||
           d.contains("offline") ||
           u.isBlank() || !u.startsWith("http", ignoreCase = true) ||
           u.contains("dummy", ignoreCase = true) || u.contains("example.com", ignoreCase = true) ||
           u.contains("localhost", ignoreCase = true) || u.contains("127.0.0.1")
}

