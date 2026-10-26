package com.lolerco.oledoro.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.util.Properties

data class DailyStudyStats(
    val date: String, // "YYYY-MM-DD"
    val focusTimeMs: Long = 0L,
    val breakTimeMs: Long = 0L
) {
    val totalTimeMs: Long get() = focusTimeMs + breakTimeMs
}

class DesktopStudyStatsRepository(
    private val statsFile: Path = Paths.get(System.getProperty("user.home"), ".oledoro", "study_stats.properties")
) {
    private val props = Properties()

    // Observable version counter to trigger Compose recomposition on state changes
    var updateTrigger: Long by mutableStateOf(0L)
        private set

    init {
        load()
    }

    @Synchronized
    private fun load() {
        if (Files.exists(statsFile) && Files.size(statsFile) > 0) {
            try {
                Files.newInputStream(statsFile).use { props.load(it) }
            } catch (_: Exception) {}
        }
    }

    @Synchronized
    private fun save() {
        try {
            Files.createDirectories(statsFile.parent)
            Files.newOutputStream(statsFile).use { props.store(it, "oledoro daily study stats") }
            updateTrigger++
        } catch (_: Exception) {}
    }

    @Synchronized
    fun recordTime(focusDeltaMs: Long, breakDeltaMs: Long, date: String = LocalDate.now().toString()) {
        if (focusDeltaMs <= 0L && breakDeltaMs <= 0L) return
        
        val focusKey = "${date}_focus"
        val breakKey = "${date}_break"
        
        val currentFocus = props.getProperty(focusKey, "0").toLongOrNull() ?: 0L
        val currentBreak = props.getProperty(breakKey, "0").toLongOrNull() ?: 0L
        
        props.setProperty(focusKey, (currentFocus + focusDeltaMs).toString())
        props.setProperty(breakKey, (currentBreak + breakDeltaMs).toString())
        save()
    }

    @Synchronized
    fun getDailyStats(date: String): DailyStudyStats {
        val focusKey = "${date}_focus"
        val breakKey = "${date}_break"
        val focus = props.getProperty(focusKey, "0").toLongOrNull() ?: 0L
        val brk = props.getProperty(breakKey, "0").toLongOrNull() ?: 0L
        return DailyStudyStats(date = date, focusTimeMs = focus, breakTimeMs = brk)
    }

    @Synchronized
    fun getRecentDays(days: Int = 7): List<DailyStudyStats> {
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { offset ->
            val dateStr = today.minusDays(offset.toLong()).toString()
            getDailyStats(dateStr)
        }
    }

    @Synchronized
    fun getAllTrackedDays(): List<DailyStudyStats> {
        val dates = props.stringPropertyNames()
            .filter { it.endsWith("_focus") || it.endsWith("_break") }
            .map { it.substringBeforeLast("_") }
            .distinct()
            .sorted()
        val stats = dates.map { getDailyStats(it) }.filter { it.focusTimeMs > 0L || it.breakTimeMs > 0L }
        return if (stats.isEmpty()) {
            listOf(getDailyStats(LocalDate.now().toString()))
        } else {
            stats
        }
    }

    @Synchronized
    fun getTotalStats(): Pair<Long, Long> {
        var totalFocus = 0L
        var totalBreak = 0L
        for (name in props.stringPropertyNames()) {
            if (name.endsWith("_focus")) {
                totalFocus += props.getProperty(name, "0").toLongOrNull() ?: 0L
            } else if (name.endsWith("_break")) {
                totalBreak += props.getProperty(name, "0").toLongOrNull() ?: 0L
            }
        }
        return Pair(totalFocus, totalBreak)
    }
}
