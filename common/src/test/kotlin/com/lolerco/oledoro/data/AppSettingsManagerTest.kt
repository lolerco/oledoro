package com.lolerco.oledoro.data

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class AppSettingsManagerTest {

    private fun withTempSettingsDir(test: (Path) -> Unit) = runTest {
        val tempDir = Files.createTempDirectory("oledoro-test-")
        val originalSettingsFile = File(System.getProperty("user.home"), ".oledoro/desktop.properties")
        val backupExists = originalSettingsFile.exists()
        val backupContent = if (backupExists) Files.readAllBytes(originalSettingsFile.toPath()) else null

        try {
            // Redirect settings to temp dir
            System.setProperty("user.home", tempDir.toString())
            test(tempDir)
        } finally {
            // Restore original
            System.clearProperty("user.home")
            if (backupExists && backupContent != null) {
                Files.write(originalSettingsFile.toPath(), backupContent)
            } else if (!backupExists && originalSettingsFile.exists()) {
                originalSettingsFile.delete()
            }
            // Clean up temp dir
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `defaults loaded when no settings file exists`() = withTempSettingsDir { tempDir ->
        val settings = AppSettingsManager()

        assertEquals(GruvboxColor.YELLOW, settings.themeColor)
        assertEquals(GruvboxColor.AQUA, settings.breakColor)
        assertEquals(GruvboxColor.RED, settings.negativeColor)
        assertEquals(25, settings.focusMinutes)
        assertEquals(5, settings.shortBreakMinutes)
        assertEquals(15, settings.longBreakMinutes)
        assertEquals(4, settings.longBreakInterval)
        assertFalse(settings.isDimmingActive)
        assertEquals(5, settings.dimPercentage)
    }

    @Test
    fun `settings persisted to file and loaded on new instance`() = withTempSettingsDir { tempDir ->
        val settings1 = AppSettingsManager()

        // Modify settings
        settings1.updateThemeColor(GruvboxColor.BLUE)
        settings1.updateBreakColor(GruvboxColor.GREEN)
        settings1.updateNegativeColor(GruvboxColor.ORANGE)
        settings1.updateFocusMinutes(50)
        settings1.updateShortBreakMinutes(10)
        settings1.updateLongBreakMinutes(30)
        settings1.updateLongBreakInterval(2)
        settings1.updateDimmingActive(true)
        settings1.updateDimPercentage(20)

        // Verify in-memory
        assertEquals(GruvboxColor.BLUE, settings1.themeColor)
        assertEquals(GruvboxColor.GREEN, settings1.breakColor)
        assertEquals(GruvboxColor.ORANGE, settings1.negativeColor)
        assertEquals(50, settings1.focusMinutes)
        assertEquals(10, settings1.shortBreakMinutes)
        assertEquals(30, settings1.longBreakMinutes)
        assertEquals(2, settings1.longBreakInterval)
        assertTrue(settings1.isDimmingActive)
        assertEquals(20, settings1.dimPercentage)

        // Create new instance - should load from file
        val settings2 = AppSettingsManager()

        assertEquals(GruvboxColor.BLUE, settings2.themeColor)
        assertEquals(GruvboxColor.GREEN, settings2.breakColor)
        assertEquals(GruvboxColor.ORANGE, settings2.negativeColor)
        assertEquals(50, settings2.focusMinutes)
        assertEquals(10, settings2.shortBreakMinutes)
        assertEquals(30, settings2.longBreakMinutes)
        assertEquals(2, settings2.longBreakInterval)
        assertTrue(settings2.isDimmingActive)
        assertEquals(20, settings2.dimPercentage)

        // Verify file exists
        val settingsFile = File(tempDir.toFile(), ".oledoro/desktop.properties")
        assertTrue(settingsFile.exists())

        val content = Files.readString(settingsFile.toPath())
        assertTrue(content.contains("themeColor=BLUE"))
        assertTrue(content.contains("breakColor=GREEN"))
        assertTrue(content.contains("negativeColor=ORANGE"))
        assertTrue(content.contains("focusMinutes=50"))
        assertTrue(content.contains("shortBreakMinutes=10"))
        assertTrue(content.contains("longBreakMinutes=30"))
        assertTrue(content.contains("longBreakInterval=2"))
        assertTrue(content.contains("isDimmingActive=true"))
        assertTrue(content.contains("dimPercentage=20"))
    }

    @Test
    fun `observable state updates immediately`() = withTempSettingsDir { tempDir ->
            val settings = AppSettingsManager()

            // Change settings and verify immediate update
            settings.updateThemeColor(GruvboxColor.BLUE)
            settings.updateFocusMinutes(45)

            assertEquals(GruvboxColor.BLUE, settings.themeColor)
            assertEquals(45, settings.focusMinutes)
        }

    @Test
    fun `updateColor methods accept valid GruvboxColor enum`() = withTempSettingsDir { tempDir ->
        val settings = AppSettingsManager()

        // Test all colors for themeColor
        for (color in GruvboxColor.values()) {
            settings.updateThemeColor(color)
            assertEquals(color, settings.themeColor)
        }

        // Test all colors for breakColor
        for (color in GruvboxColor.values()) {
            settings.updateBreakColor(color)
            assertEquals(color, settings.breakColor)
        }

        // Test all colors for negativeColor
        for (color in GruvboxColor.values()) {
            settings.updateNegativeColor(color)
            assertEquals(color, settings.negativeColor)
        }
    }

    @Test
    fun `updateTimeMethods accept valid ranges`() = withTempSettingsDir { tempDir ->
        val settings = AppSettingsManager()

        // Focus minutes: 1-90
        settings.updateFocusMinutes(1)
        assertEquals(1, settings.focusMinutes)
        settings.updateFocusMinutes(90)
        assertEquals(90, settings.focusMinutes)

        // Short break: 1-30
        settings.updateShortBreakMinutes(1)
        assertEquals(1, settings.shortBreakMinutes)
        settings.updateShortBreakMinutes(30)
        assertEquals(30, settings.shortBreakMinutes)

        // Long break: 1-60
        settings.updateLongBreakMinutes(1)
        assertEquals(1, settings.longBreakMinutes)
        settings.updateLongBreakMinutes(60)
        assertEquals(60, settings.longBreakMinutes)

        // Interval: 1-10
        settings.updateLongBreakInterval(1)
        assertEquals(1, settings.longBreakInterval)
        settings.updateLongBreakInterval(10)
        assertEquals(10, settings.longBreakInterval)
    }

    @Test
    fun `updateDimmingActive and dimPercentage work`() = withTempSettingsDir { tempDir ->
        val settings = AppSettingsManager()

        assertFalse(settings.isDimmingActive)
        assertEquals(5, settings.dimPercentage)

        settings.updateDimmingActive(true)
        assertTrue(settings.isDimmingActive)

        settings.updateDimPercentage(50)
        assertEquals(50, settings.dimPercentage)

        settings.updateDimmingActive(false)
        assertFalse(settings.isDimmingActive)
    }

    @Test
    fun `toComposeColorDirect returns correct Color for each enum`() {
        for (color in GruvboxColor.values()) {
            val composeColor = color.toComposeColorDirect()
            assertNotNull(composeColor)
            // Verify it's a valid color (non-zero alpha)
            assertTrue("Color should have non-zero value", composeColor.value > 0u)
        }
    }

    @Test
    fun `GruvboxColor enum has all expected colors`() {
        val expectedColors = listOf(
            "YELLOW", "ORANGE", "GREEN", "AQUA", "BLUE", "RED", "CREAM"
        )
        val actualColors = GruvboxColor.values().map { it.name }
        assertEquals(expectedColors, actualColors)
    }

    @Test
    fun `settings file created in correct location`() = withTempSettingsDir { tempDir ->
        val settings = AppSettingsManager()
        settings.updateThemeColor(GruvboxColor.BLUE)

        val settingsFile = File(tempDir.toFile(), ".oledoro/desktop.properties")
        assertTrue(settingsFile.exists())
        assertTrue(settingsFile.isFile)

        // Verify directory structure
        val oledoroDir = File(tempDir.toFile(), ".oledoro")
        assertTrue(oledoroDir.exists())
        assertTrue(oledoroDir.isDirectory)
    }

    @Test
    fun `corrupted settings file falls back to defaults`() = withTempSettingsDir { tempDir ->
        val settingsFile = File(tempDir.toFile(), ".oledoro/desktop.properties")
        settingsFile.parentFile?.mkdirs()
        Files.writeString(settingsFile.toPath(), "invalid=content\nnot=a=valid=property")

        val settings = AppSettingsManager()

        // Should fall back to defaults
        assertEquals(GruvboxColor.YELLOW, settings.themeColor)
        assertEquals(GruvboxColor.AQUA, settings.breakColor)
        assertEquals(GruvboxColor.RED, settings.negativeColor)
        assertEquals(25, settings.focusMinutes)
        assertEquals(5, settings.shortBreakMinutes)
        assertEquals(15, settings.longBreakMinutes)
        assertEquals(4, settings.longBreakInterval)
        assertFalse(settings.isDimmingActive)
        assertEquals(5, settings.dimPercentage)
    }

    @Test
    fun `partial settings file loads available values and defaults for missing`() = withTempSettingsDir { tempDir ->
        val settingsFile = File(tempDir.toFile(), ".oledoro/desktop.properties")
        settingsFile.parentFile?.mkdirs()
        Files.writeString(settingsFile.toPath(), "themeColor=BLUE\nfocusMinutes=42\n")

        val settings = AppSettingsManager()

        // Loaded from file
        assertEquals(GruvboxColor.BLUE, settings.themeColor)
        assertEquals(42, settings.focusMinutes)

        // Defaults for missing
        assertEquals(GruvboxColor.AQUA, settings.breakColor)
        assertEquals(GruvboxColor.RED, settings.negativeColor)
        assertEquals(5, settings.shortBreakMinutes)
        assertEquals(15, settings.longBreakMinutes)
        assertEquals(4, settings.longBreakInterval)
        assertFalse(settings.isDimmingActive)
        assertEquals(5, settings.dimPercentage)
    }
}