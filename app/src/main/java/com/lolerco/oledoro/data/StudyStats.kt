package com.lolerco.oledoro.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class AndroidStudyStatsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _updateTrigger = MutableStateFlow(0L)
    val updateTrigger: StateFlow<Long> = _updateTrigger.asStateFlow()

    @Synchronized
    fun recordTime(focusDeltaMs: Long, breakDeltaMs: Long, date: String = LocalDate.now().toString()) {
        if (focusDeltaMs <= 0L && breakDeltaMs <= 0L) return
        
        val focusKey = "${date}_focus"
        val breakKey = "${date}_break"
        
        val currentFocus = prefs.getLong(focusKey, 0L)
        val currentBreak = prefs.getLong(breakKey, 0L)
        
        prefs.edit()
            .putLong(focusKey, currentFocus + focusDeltaMs)
            .putLong(breakKey, currentBreak + breakDeltaMs)
            .apply()
            
        _updateTrigger.value++
    }

    @Synchronized
    fun getDailyStats(date: String): DailyStudyStats {
        val focusKey = "${date}_focus"
        val breakKey = "${date}_break"
        val focus = prefs.getLong(focusKey, 0L)
        val brk = prefs.getLong(breakKey, 0L)
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
    fun getTotalStats(): Pair<Long, Long> {
        var totalFocus = 0L
        var totalBreak = 0L
        for (key in prefs.all.keys) {
            if (key.endsWith("_focus")) {
                totalFocus += (prefs.all[key] as? Long) ?: 0L
            } else if (key.endsWith("_break")) {
                totalBreak += (prefs.all[key] as? Long) ?: 0L
            }
        }
        return Pair(totalFocus, totalBreak)
    }

    companion object {
        private const val PREFS_NAME = "oledoro_study_stats"
    }
}
