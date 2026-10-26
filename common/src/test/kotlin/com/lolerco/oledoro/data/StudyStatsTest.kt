package com.lolerco.oledoro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class StudyStatsTest {

    @Test
    fun testRecordAndPersistDailyStats() {
        val tempFile = Files.createTempFile("oledoro-stats-test-", ".properties")
        try {
            val repo = DesktopStudyStatsRepository(tempFile)
            val date = "2026-10-24"

            // Initially zero
            val initial = repo.getDailyStats(date)
            assertEquals(0L, initial.focusTimeMs)
            assertEquals(0L, initial.breakTimeMs)

            // Record focus and break deltas
            repo.recordTime(focusDeltaMs = 60000L, breakDeltaMs = 0L, date = date)
            repo.recordTime(focusDeltaMs = 30000L, breakDeltaMs = 15000L, date = date)

            val recorded = repo.getDailyStats(date)
            assertEquals(90000L, recorded.focusTimeMs)
            assertEquals(15000L, recorded.breakTimeMs)

            // Re-instantiate repo from the same file to confirm persistence
            val repo2 = DesktopStudyStatsRepository(tempFile)
            val reloaded = repo2.getDailyStats(date)
            assertEquals(90000L, reloaded.focusTimeMs)
            assertEquals(15000L, reloaded.breakTimeMs)

            val (totalF, totalB) = repo2.getTotalStats()
            assertEquals(90000L, totalF)
            assertEquals(15000L, totalB)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun testGetRecentDaysReturnsExpectedCount() {
        val tempFile = Files.createTempFile("oledoro-stats-days-", ".properties")
        try {
            val repo = DesktopStudyStatsRepository(tempFile)
            val recent = repo.getRecentDays(7)
            assertEquals(7, recent.size)
            assertTrue(recent.all { it.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) })
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun testGetAllTrackedDays() {
        val tempFile = Files.createTempFile("oledoro-stats-all-", ".properties")
        try {
            val repo = DesktopStudyStatsRepository(tempFile)
            repo.recordTime(focusDeltaMs = 60000L, breakDeltaMs = 0L, date = "2026-10-22")
            repo.recordTime(focusDeltaMs = 0L, breakDeltaMs = 30000L, date = "2026-10-24")
            val all = repo.getAllTrackedDays()
            assertEquals(2, all.size)
            assertEquals("2026-10-22", all[0].date)
            assertEquals(60000L, all[0].focusTimeMs)
            assertEquals(0L, all[0].breakTimeMs)
            assertEquals("2026-10-24", all[1].date)
            assertEquals(0L, all[1].focusTimeMs)
            assertEquals(30000L, all[1].breakTimeMs)
            assertEquals(30000L, all[1].totalTimeMs)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }
}
