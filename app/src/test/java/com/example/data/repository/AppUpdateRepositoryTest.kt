package com.example.data.repository

import org.junit.Assert.*
import org.junit.Test

class AppUpdateRepositoryTest {

    private fun isNewerVersion(current: String, latest: String): Boolean {
        if (latest.isEmpty()) return false
        try {
            val currentParts = current.removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = latest.removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLen) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {
            return latest != current
        }
        return false
    }

    @Test
    fun testVersionComparison_NewerVersionAvailable() {
        assertTrue(isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(isNewerVersion("1.0.1", "1.1.0"))
        assertTrue(isNewerVersion("1.0.1", "v2.0.0"))
        assertTrue(isNewerVersion("1.0", "1.0.1"))
    }

    @Test
    fun testVersionComparison_SameOrOlderVersion() {
        assertFalse(isNewerVersion("1.0.1", "1.0.1"))
        assertFalse(isNewerVersion("1.0.1", "v1.0.1"))
        assertFalse(isNewerVersion("1.0.1", "1.0.0"))
        assertFalse(isNewerVersion("2.0.0", "1.9.9"))
    }
}
