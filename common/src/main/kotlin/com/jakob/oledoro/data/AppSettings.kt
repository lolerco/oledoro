package com.jakob.oledoro.data

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

    init {
        try {
            if (Files.exists(settingsFile) && Files.size(settingsFile) > 0) {
                Files.newInputStream(settingsFile).use { props.load(it) }
            }
        } catch (_: Exception) {}
    }

    val themeColor: GruvboxColor
        get() = parseColor(props.getProperty("themeColor", GruvboxColor.YELLOW.name))
    val breakColor: GruvboxColor
        get() = parseColor(props.getProperty("breakColor", GruvboxColor.AQUA.name))
    val negativeColor: GruvboxColor
        get() = parseColor(props.getProperty("negativeColor", GruvboxColor.RED.name))
    val focusMinutes: Int
        get() = props.getProperty("focusMinutes", "25").toInt().coerceIn(1, 90)
    val shortBreakMinutes: Int
        get() = props.getProperty("shortBreakMinutes", "5").toInt().coerceIn(1, 30)
    val longBreakMinutes: Int
        get() = props.getProperty("longBreakMinutes", "15").toInt().coerceIn(1, 60)
    val longBreakInterval: Int
        get() = props.getProperty("longBreakInterval", "4").toInt().coerceIn(1, 10)
    val autoBrightenOnFinish: Boolean
        get() = props.getProperty("autoBrightenOnFinish", "true").toBoolean()
    val isDimmingActive: Boolean
        get() = props.getProperty("isDimmingActive", "false").toBoolean()
    val dimPercentage: Int
        get() = props.getProperty("dimPercentage", "5").toInt().coerceIn(1, 50)

    fun setThemeColor(color: GruvboxColor) { props.setProperty("themeColor", color.name); save() }
    fun setBreakColor(color: GruvboxColor) { props.setProperty("breakColor", color.name); save() }
    fun setNegativeColor(color: GruvboxColor) { props.setProperty("negativeColor", color.name); save() }
    fun setFocusMinutes(min: Int) { props.setProperty("focusMinutes", min.coerceIn(1, 90).toString()); save() }
    fun setShortBreakMinutes(min: Int) { props.setProperty("shortBreakMinutes", min.coerceIn(1, 30).toString()); save() }
    fun setLongBreakMinutes(min: Int) { props.setProperty("longBreakMinutes", min.coerceIn(1, 60).toString()); save() }
    fun setLongBreakInterval(interval: Int) { props.setProperty("longBreakInterval", interval.coerceIn(1, 10).toString()); save() }
    fun setAutoBrightenOnFinish(enabled: Boolean) { props.setProperty("autoBrightenOnFinish", enabled.toString()); save() }
    fun setDimmingActive(active: Boolean) { props.setProperty("isDimmingActive", active.toString()); save() }
    fun toggleDimming() { setDimmingActive(!isDimmingActive) }
    fun setDimPercentage(pct: Int) { props.setProperty("dimPercentage", pct.coerceIn(1, 50).toString()); save() }

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
