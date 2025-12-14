package com.lancontrol

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import com.lancontrol.network.CommandListener
import com.lancontrol.network.NetworkConfig
import com.lancontrol.network.ScreenStreamer
import com.lancontrol.network.FileTransfer
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.SwingUtilities

class Main : Application() {
    
    companion object {
        lateinit var instance: Main
        val commandListener = CommandListener()
        private var trayIcon: TrayIcon? = null
        private var primaryStage: Stage? = null
        private const val APP_NAME = "ClassConnecter"
        private const val INSTALL_DIR = "C:\\ClassConnecter"
    }
    
    override fun start(stage: Stage) {
        instance = this
        primaryStage = stage
        
        // Keep JavaFX running even when all windows are closed
        Platform.setImplicitExit(false)
        
        // Install to fixed location and register for startup
        installAndRegisterStartup()
        
        // Start the background listener service
        commandListener.start()
        
        // Start screen streaming server
        ScreenStreamer.startServer()
        
        // Start file transfer server
        FileTransfer.startServer()
        
        // Setup system tray
        setupSystemTray()
        
        val loader = FXMLLoader(javaClass.getResource("/com/lancontrol/views/main.fxml"))
        val scene = Scene(loader.load(), 900.0, 650.0)
        scene.stylesheets.add(javaClass.getResource("/com/lancontrol/css/style.css")?.toExternalForm() ?: "")
        
        stage.title = "9B Sınıfı"
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
    
    /**
     * Install application to fixed location and register for Windows startup
     */
    private fun installAndRegisterStartup() {
        try {
            val installDir = File(INSTALL_DIR)
            val installedExe = File(installDir, "ClassConnecter.exe")
            
            // Find current EXE or JAR
            val currentFile = findCurrentExecutable()
            
            if (currentFile != null) {
                // Check if we're already running from install location
                val currentPath = File(System.getProperty("user.dir")).absolutePath
                if (!currentPath.equals(INSTALL_DIR, ignoreCase = true)) {
                    // Not running from install dir, copy files there
                    installToFixedLocation(currentFile, installDir)
                }
                
                // Register the installed EXE for startup
                registerStartup(installedExe.absolutePath)
            }
        } catch (e: Exception) {
            println("[INSTALL] Error during installation: ${e.message}")
        }
    }
    
    /**
     * Find the current executable (EXE or JAR)
     */
    private fun findCurrentExecutable(): File? {
        try {
            val currentDir = File(System.getProperty("user.dir"))
            
            // Try to find ClassConnecter.exe first
            val exeFile = File(currentDir, "ClassConnecter.exe")
            if (exeFile.exists()) {
                return exeFile
            }
            
            // Try build/libs directory for JAR
            val buildLibs = File(currentDir, "build/libs")
            val jarFile = buildLibs.listFiles()?.find { 
                it.name.endsWith(".jar") && !it.name.contains("sources") 
            }
            if (jarFile != null) {
                return jarFile
            }
            
            // Fallback: get the JAR from class location
            val jarPath = Main::class.java.protectionDomain.codeSource?.location?.toURI()?.path
            if (jarPath != null) {
                val file = File(jarPath)
                if (file.exists()) {
                    return file
                }
            }
        } catch (e: Exception) {
            println("[INSTALL] Error finding executable: ${e.message}")
        }
        return null
    }
    
    /**
     * Copy application files to fixed install location
     */
    private fun installToFixedLocation(sourceFile: File, installDir: File) {
        try {
            // Create install directory if it doesn't exist
            if (!installDir.exists()) {
                installDir.mkdirs()
                println("[INSTALL] Created directory: $INSTALL_DIR")
            }
            
            val targetFile = File(installDir, sourceFile.name)
            
            // Copy the file if it doesn't exist or is different
            if (!targetFile.exists() || sourceFile.length() != targetFile.length()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                println("[INSTALL] Copied ${sourceFile.name} to $INSTALL_DIR")
            }
            
            // Also copy any related files (icon, etc.) from current directory
            val currentDir = sourceFile.parentFile
            currentDir?.listFiles()?.forEach { file ->
                if (file.isFile && (file.extension in listOf("png", "ico", "dll"))) {
                    val target = File(installDir, file.name)
                    if (!target.exists()) {
                        file.copyTo(target, overwrite = true)
                        println("[INSTALL] Copied ${file.name} to $INSTALL_DIR")
                    }
                }
            }
            
        } catch (e: Exception) {
            println("[INSTALL] Error copying files: ${e.message}")
        }
    }
    
    /**
     * Register application to start automatically with Windows
     */
    private fun registerStartup(appPath: String) {
        try {
            // Add to Windows Registry for startup
            val command = arrayOf(
                "reg", "add",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                "/v", APP_NAME,
                "/t", "REG_SZ",
                "/d", "\"$appPath\"",
                "/f"
            )
            
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                println("[STARTUP] Registered for Windows startup: $appPath")
            } else {
                println("[STARTUP] Failed to register for startup (exit code: $exitCode)")
            }
        } catch (e: Exception) {
            println("[STARTUP] Error registering for startup: ${e.message}")
        }
    }
    
    private fun exitApplication() {
        // Remove tray icon
        trayIcon?.let {
            SystemTray.getSystemTray().remove(it)
        }
        
        // Stop services
        commandListener.stop()
        ScreenStreamer.stopServer()
        FileTransfer.stopServer()
        
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
        FileTransfer.stopServer()
        super.stop()
    }
}

fun main() {
    Application.launch(Main::class.java)
}
