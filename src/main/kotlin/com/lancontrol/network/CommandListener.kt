package com.lancontrol.network

import com.lancontrol.models.CommandPacket
import com.lancontrol.models.ResponsePacket
import com.lancontrol.actions.ActionRegistry
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

/**
 * Background listener service that receives and executes remote commands
 */
class CommandListener {
    private var serverSocket: ServerSocket? = null
    private var running = false
    private var listenerThread: Thread? = null
    
    val onCommandReceived: MutableList<(CommandPacket) -> Unit> = mutableListOf()
    val onCommandExecuted: MutableList<(CommandPacket, Boolean, String) -> Unit> = mutableListOf()
    
    fun start() {
        if (running) return
        running = true
        
        listenerThread = thread(name = "CommandListener") {
            try {
                serverSocket = ServerSocket(NetworkConfig.commandPort)
                serverSocket?.soTimeout = 1000
                println("[Listener] Started on port ${NetworkConfig.commandPort}")
                
                while (running) {
                    try {
                        val client = serverSocket?.accept()
                        client?.let { handleClient(it) }
                    } catch (e: SocketTimeoutException) {
                        // Normal timeout, continue loop
                    }
                }
            } catch (e: Exception) {
                println("[Listener] Error: ${e.message}")
            }
        }
    }
    
    fun stop() {
        running = false
        serverSocket?.close()
        listenerThread?.join(2000)
        println("[Listener] Stopped")
    }
    
    private fun handleClient(socket: Socket) {
        thread {
            try {
                socket.soTimeout = 5000
                val input = socket.getInputStream().bufferedReader().readLine()
                val packet = CommandPacket.fromJson(input)
                
                if (packet == null) {
                    sendResponse(socket, false, "", "Invalid packet format")
                    return@thread
                }
                
                // Check if this command is for us
                val localIP = NetworkConfig.getLocalIP()
                if (packet.targetIP != "*" && packet.targetIP != localIP) {
                    sendResponse(socket, false, packet.commandId, "Not target device")
                    return@thread
                }
                
                // Notify listeners
                onCommandReceived.forEach { it(packet) }
                
                // Execute the action
                val result = ActionRegistry.execute(packet)
                
                // Notify execution complete
                onCommandExecuted.forEach { it(packet, result.first, result.second) }
                
                sendResponse(socket, result.first, packet.commandId, result.second)
                
            } catch (e: Exception) {
                println("[Listener] Client error: ${e.message}")
            } finally {
                socket.close()
            }
        }
    }
    
    private fun sendResponse(socket: Socket, success: Boolean, commandId: String, message: String) {
        try {
            val response = ResponsePacket(
                success = success,
                commandId = commandId,
                responderIP = NetworkConfig.getLocalIP(),
                message = message
            )
            socket.getOutputStream().write((response.toJson() + "\n").toByteArray())
        } catch (e: Exception) {
            println("[Listener] Response error: ${e.message}")
        }
    }
}
