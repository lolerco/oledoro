package com.lolerco.oledoro.desktop

import com.lolerco.oledoro.data.ChangelogProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopChangelogTest {

    @Test
    fun desktopModule_canAccessChangelogProvider() {
        assertTrue(ChangelogProvider.entries.isNotEmpty())
        assertEquals("1.6.0", ChangelogProvider.latestVersion)
    }

    @Test
    fun desktopModule_changelogContainsDesktopSupportHighlight() {
        val v120 = ChangelogProvider.entries.first { it.version == "1.2.0" }
        assertTrue(v120.highlights.any { it.contains("Desktop support", ignoreCase = true) })
    }
}
