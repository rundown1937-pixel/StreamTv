package com.example.util

import android.util.Log
import android.util.Xml
import com.example.data.local.ProgramEntity
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object XmltvEpgParser {

    private const val TAG = "XmltvEpgParser"

    private val xmltvDateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val xmltvDateSimpleFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Parses an XMLTV XML string into a list of ProgramEntity objects.
     */
    fun parseXmltvString(xmlString: String, maxPrograms: Int = 1000): List<ProgramEntity> {
        return parseXmltvStream(xmlString.byteInputStream(), maxPrograms)
    }

    /**
     * Parses an XMLTV XML stream into a list of ProgramEntity objects.
     */
    fun parseXmltvStream(inputStream: InputStream, maxPrograms: Int = 1000): List<ProgramEntity> {
        val programs = mutableListOf<ProgramEntity>()
        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentChannelId: String? = null
            var currentStart: Long = 0L
            var currentStop: Long = 0L
            var currentTitle: String = ""
            var currentDesc: String = ""
            var currentCategory: String = "General"

            while (eventType != XmlPullParser.END_DOCUMENT && programs.size < maxPrograms) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if ("programme".equals(tagName, ignoreCase = true)) {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            currentStart = parseXmltvTimestamp(startStr)
                            currentStop = parseXmltvTimestamp(stopStr)
                            currentTitle = ""
                            currentDesc = ""
                            currentCategory = "General"
                        } else if ("title".equals(tagName, ignoreCase = true)) {
                            currentTitle = parser.nextText()
                        } else if ("desc".equals(tagName, ignoreCase = true)) {
                            currentDesc = parser.nextText()
                        } else if ("category".equals(tagName, ignoreCase = true)) {
                            currentCategory = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if ("programme".equals(tagName, ignoreCase = true) && !currentChannelId.isNullOrEmpty() && currentStart > 0L) {
                            val durationMins = if (currentStop > currentStart) {
                                ((currentStop - currentStart) / 60000L).toInt()
                            } else {
                                30
                            }
                            programs.add(
                                ProgramEntity(
                                    id = "${currentChannelId}_${currentStart}",
                                    channelId = currentChannelId,
                                    title = currentTitle.ifEmpty { "Broadcast Schedule" },
                                    description = currentDesc.ifEmpty { "Live television programming" },
                                    category = currentCategory,
                                    startTime = currentStart,
                                    endTime = if (currentStop > currentStart) currentStop else currentStart + 1800000L,
                                    durationMinutes = durationMins,
                                    rating = "TV-PG"
                                )
                            )
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XMLTV parsing error: ${e.message}", e)
        }
        return programs
    }

    private fun parseXmltvTimestamp(str: String?): Long {
        if (str.isNullOrBlank()) return 0L
        return try {
            val clean = str.trim()
            if (clean.contains(" ")) {
                xmltvDateFormat.parse(clean)?.time ?: 0L
            } else {
                xmltvDateSimpleFormat.parse(clean.take(14))?.time ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}
