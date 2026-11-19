# Project specific ProGuard / R8 DEX optimization and keep rules

# Optimize DEX bytecode aggressively
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'o'
-overloadaggressively

# Strip verbose/debug logging in release builds to shrink DEX size and improve runtime speed
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Preserve Room Entities (fields & constructors accessed by generated Room adapters)
-keep @androidx.room.Entity class com.example.data.local.** { *; }
-keep @androidx.room.Database class com.example.data.local.** { *; }

# Preserve Moshi annotated fields and generated adapters
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class **JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}

# Suppress harmless optional warnings from OkHttp / Conscrypt / Retrofit
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Preserve Data Models serialized via JSON / Room / State
-keep class com.example.data.model.** { *; }

# Media3 / ExoPlayer keep rules and warning suppression
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.datasource.** { *; }
-dontwarn androidx.media3.**

# Google Mobile Ads (AdMob)
-keep public class com.google.android.gms.ads.** {
    public *;
}
-dontwarn com.google.android.gms.ads.**

# Coil Image Loader
-keep class coil3.** { *; }
-dontwarn coil3.**

