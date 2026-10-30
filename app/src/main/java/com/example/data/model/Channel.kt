package com.example.data.model

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
    val httpReferrer: String? = null
)

enum class ChannelGenre(val displayName: String, val iconName: String) {
    ALL("All", "Apps"),
    FAVORITES("Favorites", "Star"),
    NEWS("News", "Public"),
    SPORTS("Sports", "SportsSoccer"),
    MOVIES("Movies", "Movie"),
    MUSIC("Music", "MusicNote"),
    ENTERTAINMENT("Entertainment", "Theaters"),
    KIDS("Kids & Animation", "ChildCare"),
    DOCUMENTARY("Documentary", "MenuBook"),
    LIFESTYLE("Lifestyle", "Spa"),
    COMEDY("Comedy", "SentimentVerySatisfied"),
    SCIENCE("Science & Tech", "Science"),
    RELIGIOUS("Religious & Spiritual", "Church"),
    GENERAL("General", "Tv")
}
