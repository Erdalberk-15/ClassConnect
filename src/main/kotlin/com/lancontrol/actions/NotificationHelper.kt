package com.lancontrol.actions

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Toolkit
import javax.imageio.ImageIO

/**
 * Helper for showing system notifications
 */
object NotificationHelper {
    
    private var trayIcon: TrayIcon? = null
    
    fun show(title: String, message: String) {
        if (!SystemTray.isSupported()) {
            println("[Notification] System tray not supported")
            return
        }
        
        try {
            if (trayIcon == null) {
                val tray = SystemTray.getSystemTray()
                val image = Toolkit.getDefaultToolkit().createImage(
                    javaClass.getResource("/com/lancontrol/images/icon.png")
                ) ?: Toolkit.getDefaultToolkit().createImage(ByteArray(0))
                
                trayIcon = TrayIcon(image, "LAN Control")
                trayIcon?.isImageAutoSize = true
                tray.add(trayIcon)
            }
            
            trayIcon?.displayMessage(title, message, TrayIcon.MessageType.INFO)
        } catch (e: Exception) {
            println("[Notification] Error: ${e.message}")
        }
    }
    
    fun cleanup() {
        trayIcon?.let {
            SystemTray.getSystemTray().remove(it)
            trayIcon = null
        }
    }
}
