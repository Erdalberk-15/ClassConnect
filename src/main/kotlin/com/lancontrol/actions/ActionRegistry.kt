package com.lancontrol.actions

import com.lancontrol.models.CommandPacket
import com.lancontrol.models.RemoteAction
import java.awt.Desktop
import java.io.File

/**
 * Registry of all executable actions and their handlers
 */
object ActionRegistry {
    
    // Predefined command IDs
    object Commands {
        const val LAUNCH_APP = "LAUNCH_APP"
        const val OPEN_URL = "OPEN_URL"
        const val SHOW_NOTIFICATION = "SHOW_NOTIFICATION"
        const val EXECUTE_CMD = "EXECUTE_CMD"
        const val OPEN_FILE = "OPEN_FILE"
        const val SHUTDOWN = "SHUTDOWN"
        const val RESTART = "RESTART"
        const val LOCK_SCREEN = "LOCK_SCREEN"
        const val VOLUME_UP = "VOLUME_UP"
        const val VOLUME_DOWN = "VOLUME_DOWN"
        const val VOLUME_MUTE = "VOLUME_MUTE"
        const val SCREENSHOT = "SCREENSHOT"
        const val PING = "PING"
        const val DONATE = "DONATE"
    }
    
    private val handlers = mutableMapOf<String, (CommandPacket) -> Pair<Boolean, String>>()
    
    init {
        registerDefaultHandlers()
    }
    
    private fun registerDefaultHandlers() {
        // Ping - simple connectivity test
        register(Commands.PING) { packet ->
            Pair(true, "Pong from ${packet.targetIP}")
        }
        
        // Launch application
        register(Commands.LAUNCH_APP) { packet ->
            val path = packet.params["path"] ?: return@register Pair(false, "No path specified")
            try {
                val file = File(path)
                if (file.exists()) {
                    Desktop.getDesktop().open(file)
                    Pair(true, "Launched: $path")
                } else {
                    Runtime.getRuntime().exec(path)
                    Pair(true, "Executed: $path")
                }
            } catch (e: Exception) {
                Pair(false, "Failed to launch: ${e.message}")
            }
        }
        
        // Open URL in browser
        register(Commands.OPEN_URL) { packet ->
            val url = packet.params["url"] ?: return@register Pair(false, "No URL specified")
            try {
                Desktop.getDesktop().browse(java.net.URI(url))
                Pair(true, "Opened URL: $url")
            } catch (e: Exception) {
                Pair(false, "Failed to open URL: ${e.message}")
            }
        }
        
        // Show system notification
        register(Commands.SHOW_NOTIFICATION) { packet ->
            val title = packet.params["title"] ?: "LAN Control"
            val message = packet.params["message"] ?: "Notification"
            try {
                NotificationHelper.show(title, message)
                Pair(true, "Notification shown")
            } catch (e: Exception) {
                Pair(false, "Failed to show notification: ${e.message}")
            }
        }
        
        // Execute shell command
        register(Commands.EXECUTE_CMD) { packet ->
            val cmd = packet.params["command"] ?: return@register Pair(false, "No command specified")
            try {
                val process = Runtime.getRuntime().exec(arrayOf("cmd", "/c", cmd))
                val output = process.inputStream.bufferedReader().readText()
                Pair(true, output.take(500))
            } catch (e: Exception) {
                Pair(false, "Command failed: ${e.message}")
            }
        }
        
        // Open file
        register(Commands.OPEN_FILE) { packet ->
            val path = packet.params["path"] ?: return@register Pair(false, "No path specified")
            try {
                Desktop.getDesktop().open(File(path))
                Pair(true, "Opened: $path")
            } catch (e: Exception) {
                Pair(false, "Failed to open file: ${e.message}")
            }
        }
        
        // System shutdown
        register(Commands.SHUTDOWN) { _ ->
            try {
                Runtime.getRuntime().exec("shutdown /s /t 30")
                Pair(true, "Shutdown initiated (30 seconds)")
            } catch (e: Exception) {
                Pair(false, "Shutdown failed: ${e.message}")
            }
        }
        
        // System restart
        register(Commands.RESTART) { _ ->
            try {
                Runtime.getRuntime().exec("shutdown /r /t 30")
                Pair(true, "Restart initiated (30 seconds)")
            } catch (e: Exception) {
                Pair(false, "Restart failed: ${e.message}")
            }
        }
        
        // Lock screen
        register(Commands.LOCK_SCREEN) { _ ->
            try {
                Runtime.getRuntime().exec("rundll32.exe user32.dll,LockWorkStation")
                Pair(true, "Screen locked")
            } catch (e: Exception) {
                Pair(false, "Lock failed: ${e.message}")
            }
        }
        
        // Donate overlay
        register(Commands.DONATE) { _ ->
            try {
                DonateOverlay.show()
                Pair(true, "Donate shown")
            } catch (e: Exception) {
                Pair(false, "Donate failed: ${e.message}")
            }
        }
    }
    
    fun register(commandId: String, handler: (CommandPacket) -> Pair<Boolean, String>) {
        handlers[commandId] = handler
    }
    
    fun execute(packet: CommandPacket): Pair<Boolean, String> {
        val handler = handlers[packet.commandId]
            ?: return Pair(false, "Unknown command: ${packet.commandId}")
        
        return try {
            handler(packet)
        } catch (e: Exception) {
            Pair(false, "Execution error: ${e.message}")
        }
    }
    
    fun getAvailableActions(): List<RemoteAction> {
        return listOf(
            RemoteAction("ping", "Ping", "Test connectivity", Commands.PING),
            RemoteAction("notify", "Notification", "Show notification", Commands.SHOW_NOTIFICATION),
            RemoteAction("shutdown", "Shutdown", "Shutdown computer", Commands.SHUTDOWN),
            RemoteAction("restart", "Restart", "Restart computer", Commands.RESTART),
            RemoteAction("lock", "Lock Screen", "Lock the screen", Commands.LOCK_SCREEN),
            RemoteAction("donate", "Donate(EsadBashar)", "Show donate animation", Commands.DONATE)
        )
    }
}
