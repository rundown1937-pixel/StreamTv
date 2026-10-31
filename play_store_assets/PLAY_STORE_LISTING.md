# StreamPulse TV — Google Play Store Listing Package

This document contains all the copy, metadata, and asset references required to submit **StreamPulse TV** to the Google Play Console.

---

## 1. App Identity & Store Copy

### App Title (Max 30 characters)
```text
StreamPulse TV: Live Stream
```
*(Character count: 27 / 30 — Complies with Google Play Title Policy: No spammy buzzwords like "Free", "#1", or ALL CAPS)*

### Short Description (Max 80 characters)
```text
Stream live IPTV channels worldwide with EPG guide & studio controls.
```
*(Character count: 69 / 80)*

### Full Description (Max 4,000 characters)
```text
StreamPulse TV transforms your Android device into a high-performance live television broadcast receiver. Experience over 12,000+ verified worldwide channels spanning news, sports, movies, documentaries, and entertainment with zero lag and authentic studio-grade controls.

📺 AUTHENTIC SKEUOMORPHIC BROADCAST CONSOLE
StreamPulse TV features a tactile broadcast studio interface inspired by professional master control rooms:
• Brushed titanium chassis and beveled metallic faceplates
• Illuminated jewel LED status lamps (Ruby "ON AIR", Emerald "LIVE", Amber "STEREO")
• Real-time analog VU audio decibel level meters
• Tactile 3D push-buttons with realistic depression travel
• CRT curved glass monitor bezel with anti-glare specular reflection

⚡ ADVANCED STREAMING ENGINE (AndroidX Media3 ExoPlayer)
• Universal HLS, m3u8, and DASH video stream playback
• Instant landscape playback mode with automatic screen timeout prevention
• Auto-reconnecting live stream recovery and adaptive bitrate switching
• Aspect ratio selector: Fit, Fill, Stretch, and Zoom

📅 REAL-TIME EPG PROGRAM GUIDE (Electronic Program Guide)
• Interactive timeline schedule grid with live show progress tracking
• Browse what's on now and upcoming broadcast schedules
• Filter instantly by category: News, Sports, Movies, Kids, Music, Documentary, and Science
• Quick channel tuner search across 12,000+ stations

🥋 TWO KARATE CHOP QUICK LAUNCH
• Double downward karate-chop motion gesture instantly wakes the app and launches live streaming
• On-device motion sensor calibration with live force feedback meters

🛡️ HOUSEHOLD PROFILES & PARENTAL CONTROLS
• Multi-user profiles with personalized favorites and watch histories
• Child Safe Mode: Automatically filters mature channels and locks 18+ content
• 4-digit PIN protection for master settings and locked broadcasts

💾 OPTIMIZED LOCAL PERFORMANCE
• Device-local Room SQLite database caching for lightning-fast channel loading
• Automatic RAM background optimizer prevents memory bottlenecks during continuous streaming
• Picture-in-Picture (PiP) multitasking support

---
DISCLAIMER & USAGE NOTE:
StreamPulse TV connects to publicly accessible HLS broadcast streams and master IPTV directories. Users are responsible for complying with broadcast copyright and local viewing regulations.
```

---

## 2. Categorization & Target Audience

| Field | Value |
|---|---|
| **App Category** | Video Players & Editors (Secondary: Entertainment) |
| **Content Rating** | Teen (ESRB) / PEGI 12 (Parental controls available in app) |
| **Contains Ads** | No |
| **Target Audience** | Ages 13+ (Family safe when Kids Mode enabled) |
| **App Type** | Free Application |

---

## 3. Google Play Store Graphical Assets (Ready in `/play_store_assets/`)

All graphical assets have been generated, sized, and formatted to Google Play Console specifications:

| Asset Name | File Location | Specifications | Google Play Console Requirement |
|---|---|---|---|
| **High-Res App Icon** | `play_store_assets/icon_512x512.png` | 512 × 512 px, 32-bit PNG, 318 KB | **Mandatory** (Max 1024 KB, square) |
| **Feature Graphic** | `play_store_assets/feature_graphic_1024x500.png` | 1024 × 500 px, 24-bit PNG, 739 KB | **Mandatory** (Promotional banner) |
| **Screenshot 1 (Landscape Player)** | `play_store_assets/screenshot_1_live_player_1920x1080.png` | 1920 × 1080 px (16:9), 32-bit PNG | **Mandatory** (Video Player in Action) |
| **Screenshot 2 (Channel Rack)** | `play_store_assets/screenshot_2_channel_rack_1080x1920.png` | 1080 × 1920 px (9:16), 32-bit PNG | **Mandatory** (Live Channel Grid) |
| **Screenshot 3 (EPG TV Guide)** | `play_store_assets/screenshot_3_epg_guide_1080x1920.png` | 1080 × 1920 px (9:16), 32-bit PNG | **Mandatory** (Program Schedule) |

---

## 4. Technical App Details

- **Package Name (Application ID):** `com.stratatechlabs.streamtv`
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 14 (API 34) / Android 15
- **In-App Adaptive Icons:** Configured in `res/mipmap-*/` and `res/drawable/ic_launcher_foreground.xml` matching the Play Store icon.
