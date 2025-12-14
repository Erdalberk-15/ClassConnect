package com.lancontrol.network

import com.lancontrol.models.CommandPacket
import com.lancontrol.models.ResponsePacket
import com.lancontrol.actions.ActionRegistry
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Background listener service that receives and executes remote commands
 */
class CommandListener {
    private var serverSocket: ServerSocket? = null
    @Volatile private var running = false
    private var listenerThread: Thread? = null
    private val executor = Executors.newCachedThreadPool()
    
    val onCommandReceived = mutableListOf<(CommandPacket) -> Unit>()
    val onCommandExecuted = mutableListOf<(CommandPacket, Boolean, String) -> Unit>()
    
    fun start() {
        if (running) return
        running = true
        
        listenerThread = thread(name = "CommandListener") {
            try {
                serverSocket = ServerSocket(NetworkConfig.commandPort).apply { soTimeout = 1000 }
                println("[Listener] Started on port ${NetworkConfig.commandPort}")
                
                while (running) {
                    try {
                        serverSocket?.accept()?.let { executor.submit { handleClient(it) } }
                    } catch (_: SocketTimeoutException) {}
                }
            } catch (e: Exception) {
                println("[Listener] Error: ${e.message}")
            }
        }
    }
    
    fun stop() {
        running = false
        executor.shutdown()
        serverSocket?.close()
        listenerThread?.join(1000)
    }
    
    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 5000
            val packet = CommandPacket.fromJson(socket.getInputStream().bufferedReader().readLine())
            
            if (packet == null) {
                sendResponse(socket, false, "", "Invalid packet")
                return
            }
            
            val localIP = NetworkConfig.getLocalIP()
            if (packet.targetIP != "*" && packet.targetIP != localIP) {
                sendResponse(socket, false, packet.commandId, "Not target")
                return
            }
            
            onCommandReceived.forEach { it(packet) }
            val result = ActionRegistry.execute(packet)
            onCommandExecuted.forEach { it(packet, result.first, result.second) }
            
            sendResponse(socket, result.first, packet.commandId, result.second)
        } catch (e: Exception) {
            println("[Listener] Client error: ${e.message}")
        } finally {
            socket.close()
        }
    }
    
    private fun sendResponse(socket: Socket, success: Boolean, commandId: String, message: String) {
        try {
            val response = ResponsePacket(success, commandId, NetworkConfig.getLocalIP(), message)
            socket.getOutputStream().write((response.toJson() + "\n").toByteArray())
        } catch (_: Exception) {}
    }
}
