package com.lolerco.oledoro.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cross-platform desktop notification manager.
 * Uses native notification systems:
 * - Linux: notify-send (libnotify)
 * - Windows: PowerShell/Toast notifications
 * - macOS: osascript/UserNotifications
 * Falls back to System.out if no native system available.
 */
object DesktopNotificationManager {
    private enum class Platform { LINUX, WINDOWS, MACOS, UNKNOWN }

    private val currentPlatform: Platform
        get() {
            val osName = System.getProperty("os.name").lowercase()
            return when {
                osName.contains("linux") -> Platform.LINUX
                osName.contains("windows") -> Platform.WINDOWS
                osName.contains("mac") -> Platform.MACOS
                else -> Platform.UNKNOWN
            }
        }

    private var initialized = false
    private lateinit var scope: CoroutineScope

    fun initialize(coroutineScope: CoroutineScope) {
        if (initialized) return
        initialized = true
        scope = coroutineScope
        // Verify notification system availability
        when (currentPlatform) {
            Platform.LINUX -> checkCommand("notify-send", "--version")
            Platform.WINDOWS -> {} // PowerShell always available on Windows 10+
            Platform.MACOS -> checkCommand("osascript", "-e", "display notification")
            else -> {}
        }
    }

    private fun checkCommand(vararg args: String): Boolean {
        return try {
            ProcessBuilder(*args).redirectErrorStream(true).start().waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Send a notification when a timer phase completes.
     * @param phase The phase that just completed (e.g., "Focus", "Short Break")
     * @param nextPhase The next phase starting (e.g., "Break", "Focus")
     */
    fun notifyTimerFinished(phase: String, nextPhase: String) {
        val title = "oledoro - $phase completed"
        val message = "Time for $nextPhase"
        
        scope.launch(Dispatchers.IO) {
            sendNotification(title, message, "timer")
        }
    }

    /**
     * Send a notification when a new phase starts.
     */
    fun notifyPhaseStarted(phase: String) {
        val title = "oledoro - $phase started"
        val message = "Session begun"
        
        scope.launch(Dispatchers.IO) {
            sendNotification(title, message, "timer")
        }
    }

    private suspend fun sendNotification(title: String, message: String, iconName: String) {
        withContext(Dispatchers.IO) {
            when (currentPlatform) {
                Platform.LINUX -> sendLinuxNotification(title, message, iconName)
                Platform.WINDOWS -> sendWindowsNotification(title, message)
                Platform.MACOS -> sendMacNotification(title, message)
                else -> println("[$title] $message")
            }
        }
    }

    private fun sendLinuxNotification(title: String, message: String, iconName: String) {
        try {
            // notify-send supports --icon with icon name or path
            // Try to use the app icon from resources or system icon theme
            val iconPath = findIconPath()
            val iconArgs = if (iconPath != null) {
                listOf("--icon", iconPath)
            } else if (iconExists(iconName)) {
                listOf("--icon", iconName)
            } else {
                emptyList()
            }
            
            ProcessBuilder(
                listOf("notify-send", title, message) + iconArgs + listOf("--category", "timer")
            ).start()
        } catch (e: Exception) {
            println("[$title] $message (notify-send failed: ${e.message})")
        }
    }

    private fun findIconPath(): String? {
        // Try system icon theme first (installed via package manager or user)
        // Use PNG for better scaling with notify-send
        val userIconPng = "${System.getProperty("user.home")}/.local/share/icons/hicolor/48x48/apps/oledoro.png"
        if (File(userIconPng).exists()) return userIconPng
        
        val systemIconPng = "/usr/share/icons/hicolor/48x48/apps/oledoro.png"
        if (File(systemIconPng).exists()) return systemIconPng
        
        val userIconSvg = "${System.getProperty("user.home")}/.local/share/icons/hicolor/scalable/apps/oledoro.svg"
        if (File(userIconSvg).exists()) return userIconSvg
        
        val systemIconSvg = "/usr/share/icons/hicolor/scalable/apps/oledoro.svg"
        if (File(systemIconSvg).exists()) return systemIconSvg
        
        // Try to extract from classpath (for development)
        val classLoader = this.javaClass.classLoader
        val resource = classLoader.getResource("oledoro.png")
        if (resource != null) {
            val file = File(resource.toURI())
            if (file.exists()) return file.absolutePath
        }
        
        val resourceSvg = classLoader.getResource("oledoro.svg")
        if (resourceSvg != null) {
            val file = File(resourceSvg.toURI())
            if (file.exists()) return file.absolutePath
        }
        
        return null
    }

    private fun iconExists(name: String): Boolean {
        // Check common icon theme locations
        val iconDirs = listOf(
            "/usr/share/icons",
            "/usr/local/share/icons",
            System.getProperty("user.home") + "/.local/share/icons",
            System.getProperty("user.home") + "/.icons"
        )
        
        return iconDirs.any { dir ->
            File(dir).walkTopDown()
                .any { it.name.startsWith(name) && (it.extension == "png" || it.extension == "svg" || it.extension == "xpm") }
        }
    }

    private fun sendWindowsNotification(title: String, message: String) {
        try {
            // Use PowerShell with a simpler approach - Windows 10+ supports BurntToast or direct toast XML
            // Use a simple message box approach that works everywhere
            val escapedTitle = title.replace("\"", "\\\"")
            val escapedMessage = message.replace("\"", "\\\"")
            val psScript = """
                Add-Type -AssemblyName System.Windows.Forms
                [System.Windows.Forms.MessageBox]::Show("$escapedMessage", "$escapedTitle", "OK", "Information") | Out-Null
            """.trimIndent()
            
            ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command", psScript).start()
        } catch (e: Exception) {
            // Fallback: simple message box or console
            println("[$title] $message (Windows notification failed: ${e.message})")
        }
    }

    private fun sendMacNotification(title: String, message: String) {
        try {
            val escapedTitle = title.replace("\"", "\\\"")
            val escapedMessage = message.replace("\"", "\\\"")
            val script = "display notification \"$escapedMessage\" with title \"$escapedTitle\""
            ProcessBuilder("osascript", "-e", script).start()
        } catch (e: Exception) {
            println("[$title] $message (osascript failed: ${e.message})")
        }
    }

    fun cleanup() {
        // No cleanup needed for stateless notifications
        initialized = false
    }
}