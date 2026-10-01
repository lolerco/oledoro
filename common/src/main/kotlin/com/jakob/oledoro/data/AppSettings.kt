package com.jakob.oledoro.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties

enum class GruvboxColor(val displayName: String, val hex: String) {
    YELLOW("Yellow", "#FABD2F"),
    ORANGE("Orange", "#FE8019"),
    GREEN("Green", "#B8BB26"),
    AQUA("Aqua", "#8EC07C"),
    BLUE("Blue", "#83A598"),
    RED("Red", "#FB4934"),
    CREAM("Cream", "#EBDBB2");

    val color: androidx.compose.ui.graphics.Color get() = toComposeColorDirect()
    fun toComposeColor(): androidx.compose.ui.graphics.Color = toComposeColorDirect()
    fun toComposeColorDirect(): androidx.compose.ui.graphics.Color = when (this) {
        YELLOW -> androidx.compose.ui.graphics.Color(0xFFFABD2F)
        ORANGE -> androidx.compose.ui.graphics.Color(0xFFFE8019)
        GREEN -> androidx.compose.ui.graphics.Color(0xFFB8BB26)
        AQUA -> androidx.compose.ui.graphics.Color(0xFF8EC07C)
        BLUE -> androidx.compose.ui.graphics.Color(0xFF83A598)
        RED -> androidx.compose.ui.graphics.Color(0xFFFB4934)
        CREAM -> androidx.compose.ui.graphics.Color(0xFFEBDBB2)
    }
}

class AppSettingsManager(private val settingsFile: Path = Paths.get(System.getProperty("user.home"), ".oledoro", "desktop.properties")) {
    private val props = Properties()
    
    // Load settings from file first
    private val loadedProps: Properties
        get() {
            if (Files.exists(settingsFile) && Files.size(settingsFile) > 0) {
                try {
                    Files.newInputStream(settingsFile).use { props.load(it) }
                } catch (_: Exception) {}
            }
            return props
        }

    // Observable state for Compose recomposition - initialized after file load
    var themeColor: GruvboxColor by mutableStateOf(parseColor(loadedProps.getProperty("themeColor", GruvboxColor.YELLOW.name)))
        private set
    var breakColor: GruvboxColor by mutableStateOf(parseColor(loadedProps.getProperty("breakColor", GruvboxColor.AQUA.name)))
        private set
    var negativeColor: GruvboxColor by mutableStateOf(parseColor(loadedProps.getProperty("negativeColor", GruvboxColor.RED.name)))
        private set
    var focusMinutes: Int by mutableStateOf(loadedProps.getProperty("focusMinutes", "25").toInt().coerceIn(1, 90))
        private set
    var shortBreakMinutes: Int by mutableStateOf(loadedProps.getProperty("shortBreakMinutes", "5").toInt().coerceIn(1, 30))
        private set
    var longBreakMinutes: Int by mutableStateOf(loadedProps.getProperty("longBreakMinutes", "15").toInt().coerceIn(1, 60))
        private set
    var longBreakInterval: Int by mutableStateOf(loadedProps.getProperty("longBreakInterval", "4").toInt().coerceIn(1, 10))
        private set
    var autoBrightenOnFinish: Boolean by mutableStateOf(loadedProps.getProperty("autoBrightenOnFinish", "true").toBoolean())
        private set
    var isDimmingActive: Boolean by mutableStateOf(loadedProps.getProperty("isDimmingActive", "false").toBoolean())
        private set
    var dimPercentage: Int by mutableStateOf(loadedProps.getProperty("dimPercentage", "5").toInt().coerceIn(1, 50))
        private set

    fun updateThemeColor(color: GruvboxColor) {
        props.setProperty("themeColor", color.name)
        themeColor = color
        save()
    }
    fun updateBreakColor(color: GruvboxColor) {
        props.setProperty("breakColor", color.name)
        breakColor = color
        save()
    }
    fun updateNegativeColor(color: GruvboxColor) {
        props.setProperty("negativeColor", color.name)
        negativeColor = color
        save()
    }
    fun updateFocusMinutes(min: Int) {
        val clamped = min.coerceIn(1, 90)
        props.setProperty("focusMinutes", clamped.toString())
        focusMinutes = clamped
        save()
    }
    fun updateShortBreakMinutes(min: Int) {
        val clamped = min.coerceIn(1, 30)
        props.setProperty("shortBreakMinutes", clamped.toString())
        shortBreakMinutes = clamped
        save()
    }
    fun updateLongBreakMinutes(min: Int) {
        val clamped = min.coerceIn(1, 60)
        props.setProperty("longBreakMinutes", clamped.toString())
        longBreakMinutes = clamped
        save()
    }
    fun updateLongBreakInterval(interval: Int) {
        val clamped = interval.coerceIn(1, 10)
        props.setProperty("longBreakInterval", clamped.toString())
        longBreakInterval = clamped
        save()
    }
    fun updateAutoBrightenOnFinish(enabled: Boolean) {
        props.setProperty("autoBrightenOnFinish", enabled.toString())
        autoBrightenOnFinish = enabled
        save()
    }
    fun updateDimmingActive(active: Boolean) {
        props.setProperty("isDimmingActive", active.toString())
        isDimmingActive = active
        save()
    }
    fun toggleDimming() {
        updateDimmingActive(!isDimmingActive)
    }
    fun updateDimPercentage(pct: Int) {
        val clamped = pct.coerceIn(1, 50)
        props.setProperty("dimPercentage", clamped.toString())
        dimPercentage = clamped
        save()
    }

    private fun save() {
        try {
            Files.createDirectories(settingsFile.parent)
            Files.newOutputStream(settingsFile).use { props.store(it, "oledoro desktop settings") }
        } catch (_: Exception) {}
    }

    private fun parseColor(name: String): GruvboxColor = try { GruvboxColor.valueOf(name) } catch (_: Exception) { GruvboxColor.YELLOW }

    companion object {
        const val DEFAULT_DIM_PERCENTAGE = 5
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_LONG_BREAK_INTERVAL = 4
        const val DEFAULT_AUTO_BRIGHTEN_ON_FINISH = true
    }
}