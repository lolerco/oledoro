package com.jakob.oledoro.desktop

import java.awt.AWTException
import java.awt.Image
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JOptionPane

object DesktopNotificationManager {
    private var trayIcon: TrayIcon? = null
    private var initialized = false
    private val logoImage: Image? by lazy { loadLogo() }

    fun initialize() {
        if (initialized || !SystemTray.isSupported()) return
        try {
            val tray = SystemTray.getSystemTray()
            val iconImage = logoImage ?: Toolkit.getDefaultToolkit().createImage("logo.png")
            trayIcon = TrayIcon(iconImage, "oledoro")
            trayIcon?.setImageAutoSize(true)
            tray.add(trayIcon!!)
            initialized = true
        } catch (e: AWTException) {
            // System tray not available
        } catch (e: Exception) {
            // Ignore initialization errors
        }
    }

    private fun loadLogo(): Image? {
        return try {
            // Try to load from resources
            val resource = this.javaClass.getResource("/logo.png")
            if (resource != null) {
                ImageIO.read(resource.openStream())
            } else {
                // Fallback: create a simple colored icon
                createDefaultIcon()
            }
        } catch (_: Exception) {
            createDefaultIcon()
        }
    }

    private fun createDefaultIcon(): Image {
        // Create a simple 32x32 icon programmatically
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val g = image.graphics
        g.color = java.awt.Color(0xFABD2F) // Gruvbox Yellow
        g.fillOval(4, 4, 24, 24)
        g.color = java.awt.Color.BLACK
        g.drawString(">", 10, 22)
        g.dispose()
        return image
    }

    fun notifyTimerFinished(phase: String, nextPhase: String) {
        val title = "oledoro - $phase completed"
        val message = "Time for $nextPhase"
        
        // Try system tray notification first
        if (initialized && trayIcon != null) {
            trayIcon!!.displayMessage(title, message, TrayIcon.MessageType.INFO)
            return
        }
        
        // Fallback: Swing JOptionPane (less intrusive)
        try {
            val icon = logoImage?.let { ImageIcon(it) }
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE, icon)
        } catch (_: Exception) {
            // Silent fallback - print to console
            println("[$title] $message")
        }
    }

    fun notifyPhaseStarted(phase: String) {
        val title = "oledoro - $phase started"
        val message = "Focus session begun" 
        
        if (initialized && trayIcon != null) {
            trayIcon!!.displayMessage(title, message, TrayIcon.MessageType.INFO)
        }
    }

    fun cleanup() {
        if (initialized && trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon!!)
            } catch (_: Exception) {}
            trayIcon = null
            initialized = false
        }
    }
}