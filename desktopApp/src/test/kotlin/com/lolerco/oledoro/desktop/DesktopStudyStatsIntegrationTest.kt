package com.lolerco.oledoro.desktop

import com.lolerco.oledoro.data.DesktopStudyStatsRepository
import com.lolerco.oledoro.domain.TimerEngine
import com.lolerco.oledoro.domain.TimerPhase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class DesktopStudyStatsIntegrationTest {

    @Test
    fun timerEngine_recordsTimeToStatsRepositoryOnTick() {
        val tempFile = Files.createTempFile("oledoro-desktop-stats-", ".properties")
        try {
            val repo = DesktopStudyStatsRepository(tempFile)
            val date = "2026-10-24"
            val engine = TimerEngine(
                focusDurationMs = 25 * 60 * 1000L,
                onTimeAccumulated = { fDelta, bDelta ->
                    repo.recordTime(fDelta, bDelta, date = date)
                }
            )

            engine.start()
            engine.tick(1000L)
            engine.tick(2000L)

            val stats = repo.getDailyStats(date)
            assertEquals(3000L, stats.focusTimeMs)
            assertEquals(0L, stats.breakTimeMs)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }
}
