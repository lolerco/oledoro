package com.lolerco.oledoro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogTest {

    @Test
    fun changelogProvider_hasEntries() {
        assertTrue(ChangelogProvider.entries.isNotEmpty())
    }

    @Test
    fun changelogProvider_latestVersionIs120() {
        assertEquals("1.6.0", ChangelogProvider.latestVersion)
        assertEquals("1.6.0", ChangelogProvider.entries.first().version)
    }

    @Test
    fun changelogProvider_containsExpectedVersionsInOrder() {
        val versions = ChangelogProvider.entries.map { it.version }
        assertEquals(listOf("1.6.0", "1.5.0", "1.4.0", "1.3.0", "1.2.0", "1.1.0", "1.0.0"), versions)
    }

    @Test
    fun changelogProvider_allEntriesHaveHighlightsAndDates() {
        for (entry in ChangelogProvider.entries) {
            assertTrue("Version ${entry.version} should have a non-empty date", entry.date.isNotBlank())
            assertTrue("Version ${entry.version} should have highlights", entry.highlights.isNotEmpty())
            for (highlight in entry.highlights) {
                assertFalse("Highlight should not be blank in version ${entry.version}", highlight.isBlank())
            }
        }
    }
}
