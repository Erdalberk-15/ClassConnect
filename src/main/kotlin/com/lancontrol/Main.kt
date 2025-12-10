package com.lancontrol

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import com.lancontrol.network.CommandListener
import com.lancontrol.network.NetworkConfig
import com.lancontrol.network.ScreenStreamer
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

class Main : Application() {
    
    companion object {
        lateinit var instance: Main
        val commandListener = CommandListener()
        private var trayIcon: TrayIcon? = null
        private var primaryStage: Stage? = null
    }
    
    override fun start(stage: Stage) {
        instance = this
        primaryStage = stage
        
        // Keep JavaFX running even when all windows are closed
        Platform.setImplicitExit(false)
        
        // Start the background listener service
        commandListener.start()
        
        // Start screen streaming server
        ScreenStreamer.startServer()
        
        // Setup system tray
        setupSystemTray()
        
        val loader = FXMLLoader(javaClass.getResource("/com/lancontrol/views/main.fxml"))
        val scene = Scene(loader.load(), 900.0, 650.0)
        scene.stylesheets.add(javaClass.getResource("/com/lancontrol/css/style.css")?.toExternalForm() ?: "")
        
        stage.title = "9A Sınıfı"
        stage.scene = scene
        stage.isResizable = true
        
        // Minimize to tray instead of closing
        stage.setOnCloseRequest { event ->
            event.consume()
            stage.hide()
        }
        
        stage.show()
    }
    
    private fun setupSystemTray() {
        if (!SystemTray.isSupported()) {
            println("System tray not supported")
            return
        }
        
        SwingUtilities.invokeLater {
            try {
                val tray = SystemTray.getSystemTray()
                
                // Create tray icon image
                val iconUrl = javaClass.getResource("/com/lancontrol/images/icon.png")
                val image = if (iconUrl != null) {
                    Toolkit.getDefaultToolkit().createImage(iconUrl)
                } else {
                    createDefaultIcon()
                }
                
                // Create popup menu
                val popup = PopupMenu()
                
                val showItem = MenuItem("Aç")
                showItem.addActionListener {
                    Platform.runLater {
                        primaryStage?.show()
                        primaryStage?.toFront()
                    }
                }
                
                val exitItem = MenuItem("Çıkış")
                exitItem.addActionListener {
                    exitApplication()
                }
                
                popup.add(showItem)
                popup.addSeparator()
                popup.add(exitItem)
                
                trayIcon = TrayIcon(image, "ClassConnecter", popup)
                trayIcon?.isImageAutoSize = true
                
                // Double-click to show window
                trayIcon?.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (e.clickCount == 2) {
                            Platform.runLater {
                                primaryStage?.show()
                                primaryStage?.toFront()
                            }
                        }
                    }
                })
                
                tray.add(trayIcon)
                
            } catch (e: Exception) {
                println("Error setting up system tray: ${e.message}")
            }
        }
    }
    
    private fun createDefaultIcon(): Image {
        // Create a simple colored square as default icon
        val size = 16
        val image = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color(61, 90, 128) // Blue color
        g.fillRect(0, 0, size, size)
        g.dispose()
        return image
    }
    
    private fun exitApplication() {
        // Remove tray icon
        trayIcon?.let {
            SystemTray.getSystemTray().remove(it)
        }
        
        // Stop services
        commandListener.stop()
        ScreenStreamer.stopServer()
        
        // Exit JavaFX
        Platform.runLater {
            Platform.exit()
        }
        
        // Exit JVM
        System.exit(0)
    }
    
    override fun stop() {
        commandListener.stop()
        ScreenStreamer.stopServer()
        super.stop()
    }
}

fun main() {
    Application.launch(Main::class.java)
}
