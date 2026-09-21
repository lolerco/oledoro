package com.jakob.oledoro.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GruvboxColor(val displayName: String, val color: Color) {
    YELLOW("Yellow", Color(0xFFFABD2F)),
    ORANGE("Orange", Color(0xFFFE8019)),
    GREEN("Green", Color(0xFFB8BB26)),
    AQUA("Aqua", Color(0xFF8EC07C)),
    BLUE("Blue", Color(0xFF83A598)),
    RED("Red", Color(0xFFFB4934)),
    CREAM("Cream", Color(0xFFEBDBB2))
}

class AppSettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeColor = MutableStateFlow(loadThemeColor())
    val themeColor: StateFlow<GruvboxColor> = _themeColor.asStateFlow()

    private val _isDimmingActive = MutableStateFlow(loadDimmingActive())
    val isDimmingActive: StateFlow<Boolean> = _isDimmingActive.asStateFlow()

    private val _dimPercentage = MutableStateFlow(loadDimPercentage())
    val dimPercentage: StateFlow<Int> = _dimPercentage.asStateFlow()

    private val _focusMinutes = MutableStateFlow(loadFocusMinutes())
    val focusMinutes: StateFlow<Int> = _focusMinutes.asStateFlow()

    private val _shortBreakMinutes = MutableStateFlow(loadShortBreakMinutes())
    val shortBreakMinutes: StateFlow<Int> = _shortBreakMinutes.asStateFlow()

    private val _longBreakMinutes = MutableStateFlow(loadLongBreakMinutes())
    val longBreakMinutes: StateFlow<Int> = _longBreakMinutes.asStateFlow()

    private val _longBreakInterval = MutableStateFlow(loadLongBreakInterval())
    val longBreakInterval: StateFlow<Int> = _longBreakInterval.asStateFlow()

    private val _autoBrightenOnFinish = MutableStateFlow(loadAutoBrightenOnFinish())
    val autoBrightenOnFinish: StateFlow<Boolean> = _autoBrightenOnFinish.asStateFlow()

    fun setThemeColor(color: GruvboxColor) {
        _themeColor.value = color
        prefs.edit().putString(KEY_THEME_COLOR, color.name).apply()
    }

    fun setDimmingActive(active: Boolean) {
        _isDimmingActive.value = active
        prefs.edit().putBoolean(KEY_DIMMING_ACTIVE, active).apply()
    }

    fun toggleDimming() {
        setDimmingActive(!_isDimmingActive.value)
    }

    fun setDimPercentage(percentage: Int) {
        val clamped = percentage.coerceIn(1, 50)
        _dimPercentage.value = clamped
        prefs.edit().putInt(KEY_DIM_PERCENTAGE, clamped).apply()
    }

    fun setFocusMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 90)
        _focusMinutes.value = clamped
        prefs.edit().putInt(KEY_FOCUS_MINUTES, clamped).apply()
    }

    fun setShortBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 30)
        _shortBreakMinutes.value = clamped
        prefs.edit().putInt(KEY_SHORT_BREAK_MINUTES, clamped).apply()
    }

    fun setLongBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 60)
        _longBreakMinutes.value = clamped
        prefs.edit().putInt(KEY_LONG_BREAK_MINUTES, clamped).apply()
    }

    fun setLongBreakInterval(interval: Int) {
        val clamped = interval.coerceIn(1, 10)
        _longBreakInterval.value = clamped
        prefs.edit().putInt(KEY_LONG_BREAK_INTERVAL, clamped).apply()
    }

    fun setAutoBrightenOnFinish(enabled: Boolean) {
        _autoBrightenOnFinish.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_BRIGHTEN_ON_FINISH, enabled).apply()
    }

    private fun loadThemeColor(): GruvboxColor {
        val name = prefs.getString(KEY_THEME_COLOR, GruvboxColor.YELLOW.name) ?: GruvboxColor.YELLOW.name
        return try {
            GruvboxColor.valueOf(name)
        } catch (e: Exception) {
            GruvboxColor.YELLOW
        }
    }

    private fun loadDimmingActive(): Boolean {
        return prefs.getBoolean(KEY_DIMMING_ACTIVE, true)
    }

    private fun loadDimPercentage(): Int {
        return prefs.getInt(KEY_DIM_PERCENTAGE, DEFAULT_DIM_PERCENTAGE).coerceIn(1, 50)
    }

    private fun loadFocusMinutes(): Int {
        return prefs.getInt(KEY_FOCUS_MINUTES, DEFAULT_FOCUS_MINUTES).coerceIn(1, 90)
    }

    private fun loadShortBreakMinutes(): Int {
        return prefs.getInt(KEY_SHORT_BREAK_MINUTES, DEFAULT_SHORT_BREAK_MINUTES).coerceIn(1, 30)
    }

    private fun loadLongBreakMinutes(): Int {
        return prefs.getInt(KEY_LONG_BREAK_MINUTES, DEFAULT_LONG_BREAK_MINUTES).coerceIn(1, 60)
    }

    private fun loadLongBreakInterval(): Int {
        return prefs.getInt(KEY_LONG_BREAK_INTERVAL, DEFAULT_LONG_BREAK_INTERVAL).coerceIn(1, 10)
    }

    private fun loadAutoBrightenOnFinish(): Boolean {
        return prefs.getBoolean(KEY_AUTO_BRIGHTEN_ON_FINISH, DEFAULT_AUTO_BRIGHTEN_ON_FINISH)
    }

    companion object {
        private const val PREFS_NAME = "oledoro_prefs"
        private const val KEY_THEME_COLOR = "key_theme_color"
        private const val KEY_DIMMING_ACTIVE = "key_dimming_active"
        private const val KEY_DIM_PERCENTAGE = "key_dim_percentage"
        private const val KEY_FOCUS_MINUTES = "key_focus_minutes"
        private const val KEY_SHORT_BREAK_MINUTES = "key_short_break_minutes"
        private const val KEY_LONG_BREAK_MINUTES = "key_long_break_minutes"
        private const val KEY_LONG_BREAK_INTERVAL = "key_long_break_interval"
        private const val KEY_AUTO_BRIGHTEN_ON_FINISH = "key_auto_brighten_on_finish"

        const val DEFAULT_DIM_PERCENTAGE = 5
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_LONG_BREAK_INTERVAL = 4
        const val DEFAULT_AUTO_BRIGHTEN_ON_FINISH = true
    }
}
