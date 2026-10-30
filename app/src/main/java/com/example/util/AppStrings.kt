package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    SPANISH("es", "Spanish", "Español"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    ARABIC("ar", "Arabic", "العربية")
}

data class LocalizedStrings(
    val appName: String,
    val liveTab: String,
    val favoritesTab: String,
    val channelsTab: String,
    val guideTab: String,
    val offlineTab: String,
    val profilesTab: String,
    val settingsTab: String,
    val searchPlaceholder: String,
    val allChannels: String,
    val liveStreams: String,
    val play: String,
    val clearCache: String,
    val lastWatched: String,
    val offlineRecordings: String,
    val takescreenshot: String,
    val recordBroadcast: String,
    val adultFilter: String,
    val roomCacheManagement: String,
    val autoUpdateNotice: String,
    val languageSelectorTitle: String
)

val EnglishStrings = LocalizedStrings(
    appName = "StreamPulse TV",
    liveTab = "Live",
    favoritesTab = "Favorites",
    channelsTab = "Channels",
    guideTab = "Guide",
    offlineTab = "Offline",
    profilesTab = "Profiles",
    settingsTab = "Settings",
    searchPlaceholder = "Search 12,000+ live channels, sports, news...",
    allChannels = "All Live Channels",
    liveStreams = "Verified Streams",
    play = "Play",
    clearCache = "Clear Channel Cache",
    lastWatched = "Continue Watching",
    offlineRecordings = "Device Storage & Offline Recordings",
    takescreenshot = "Take Screenshot",
    recordBroadcast = "Record Broadcast",
    adultFilter = "18+ Adult Content Filter",
    roomCacheManagement = "Room Database & Storage Cache",
    autoUpdateNotice = "M3U link updates automatically on first open and once every 24 hours.",
    languageSelectorTitle = "App Interface Language"
)

val HindiStrings = LocalizedStrings(
    appName = "StreamPulse TV",
    liveTab = "लाइव",
    favoritesTab = "पसंदीदा",
    channelsTab = "चैनल्स",
    guideTab = "गाइड",
    offlineTab = "ऑफलाइन",
    profilesTab = "प्रोफाइल",
    settingsTab = "सेटिंग्स",
    searchPlaceholder = "12,000+ लाइव चैनल, खेल, समाचार खोजें...",
    allChannels = "सभी लाइव चैनल्स",
    liveStreams = "सत्यापित स्ट्रीम्स",
    play = "चलाएं",
    clearCache = "कैश साफ़ करें",
    lastWatched = "जारी रखें",
    offlineRecordings = "डिवाइस स्टोरेज और ऑफलाइन रिकॉर्डिंग",
    takescreenshot = "स्क्रीनशॉट लें",
    recordBroadcast = "प्रसारण रिकॉर्ड करें",
    adultFilter = "18+ वयस्क सामग्री फ़िल्टर",
    roomCacheManagement = "डेटाबेस और स्टोरेज प्रबंधन",
    autoUpdateNotice = "M3U लिंक पहले खुलने पर और हर 24 घंटे में स्वतः अपडेट होता है।",
    languageSelectorTitle = "ऐप इंटरफ़ेस भाषा"
)

val SpanishStrings = LocalizedStrings(
    appName = "StreamPulse TV",
    liveTab = "En Vivo",
    favoritesTab = "Favoritos",
    channelsTab = "Canales",
    guideTab = "Guía",
    offlineTab = "Desconectado",
    profilesTab = "Perfiles",
    settingsTab = "Ajustes",
    searchPlaceholder = "Buscar 12,000+ canales, deportes, noticias...",
    allChannels = "Todos los Canales en Vivo",
    liveStreams = "Transmisiones Verificadas",
    play = "Reproducir",
    clearCache = "Borrar Caché",
    lastWatched = "Continuar Viendo",
    offlineRecordings = "Grabaciones y Almacenamiento Local",
    takescreenshot = "Captura de Pantalla",
    recordBroadcast = "Grabar Emisión",
    adultFilter = "Filtro de Contenido para Adultos (18+)",
    roomCacheManagement = "Gestión de Base de Datos y Caché",
    autoUpdateNotice = "El enlace M3U se actualiza al iniciar y cada 24 horas.",
    languageSelectorTitle = "Idioma de la Aplicación"
)

val FrenchStrings = LocalizedStrings(
    appName = "StreamPulse TV",
    liveTab = "En Direct",
    favoritesTab = "Favoris",
    channelsTab = "Chaînes",
    guideTab = "Guide",
    offlineTab = "Hors Ligne",
    profilesTab = "Profils",
    settingsTab = "Paramètres",
    searchPlaceholder = "Rechercher 12,000+ chaînes, sports, actualités...",
    allChannels = "Toutes les Chaînes en Direct",
    liveStreams = "Flux Vérifiés",
    play = "Lire",
    clearCache = "Vider le Cache",
    lastWatched = "Reprendre la Lecture",
    offlineRecordings = "Stockage de l'Appareil & Enregistrements",
    takescreenshot = "Prendre une Capture",
    recordBroadcast = "Enregistrer la Diffusion",
    adultFilter = "Filtre Contenu Adulte (18+)",
    roomCacheManagement = "Base de Données & Gestion du Cache",
    autoUpdateNotice = "Le lien M3U se met à jour au premier démarrage et toutes les 24h.",
    languageSelectorTitle = "Langue de l'Interface"
)

val GermanStrings = LocalizedStrings(
    appName = "StreamPulse TV",
    liveTab = "Live",
    favoritesTab = "Favoriten",
    channelsTab = "Sender",
    guideTab = "Programm",
    offlineTab = "Offline",
    profilesTab = "Profile",
    settingsTab = "Einstellungen",
    searchPlaceholder = "12.000+ Live-Sender, Sport, News durchsuchen...",
    allChannels = "Alle Live-Sender",
    liveStreams = "Geprüfte Streams",
    play = "Abspielen",
    clearCache = "Cache leeren",
    lastWatched = "Weiterschauen",
    offlineRecordings = "Gerätespeicher & Offline-Aufnahmen",
    takescreenshot = "Screenshot aufnehmen",
    recordBroadcast = "Sendung aufnehmen",
    adultFilter = "Jugendschutz-Filter (18+)",
    roomCacheManagement = "Datenbank- & Speicherverwaltung",
    autoUpdateNotice = "M3U-Link aktualisiert sich beim Start und alle 24 Stunden.",
    languageSelectorTitle = "App-Sprache"
)

val ArabicStrings = LocalizedStrings(
    appName = "StreamPulse TV",
    liveTab = "مباشر",
    favoritesTab = "المفضلة",
    channelsTab = "القنوات",
    guideTab = "الدليل",
    offlineTab = "بدون إنترنت",
    profilesTab = "الحسابات",
    settingsTab = "الإعدادات",
    searchPlaceholder = "ابحث بين أكثر من 12000 قناة حية، رياضة، أخبار...",
    allChannels = "جميع القنوات المباشرة",
    liveStreams = "بثوث موثوقة",
    play = "تشغيل",
    clearCache = "مسح الذاكرة المؤقتة",
    lastWatched = "متابعة المشاهدة",
    offlineRecordings = "تسجيلات البث وذاكرة الجهاز",
    takescreenshot = "التقاط شاشة",
    recordBroadcast = "تسجيل البث",
    adultFilter = "تصفية محتوى البالغين (+18)",
    roomCacheManagement = "إدارة التخزين وقاعدة البيانات",
    autoUpdateNotice = "يتم تحديث رابط M3U تلقائيًا عند الفتح وكل 24 ساعة.",
    languageSelectorTitle = "لغة واجهة التطبيق"
)

fun getStringsForLanguage(code: String): LocalizedStrings {
    return when (code.lowercase()) {
        "hi" -> HindiStrings
        "es" -> SpanishStrings
        "fr" -> FrenchStrings
        "de" -> GermanStrings
        "ar" -> ArabicStrings
        else -> EnglishStrings
    }
}

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }
