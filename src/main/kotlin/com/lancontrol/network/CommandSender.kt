package com.lancontrol.network

import com.lancontrol.models.CommandPacket
import com.lancontrol.models.ResponsePacket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Sends commands to remote machines on the LAN
 */
object CommandSender {
    
    fun send(
        targetIP: String,
        commandId: String,
        params: Map<String, String> = emptyMap(),
        onResponse: (ResponsePacket?) -> Unit = {}
    ) {
        thread {
            try {
                val packet = CommandPacket(
                    commandId = commandId,
                    senderIP = NetworkConfig.getLocalIP(),
                    senderName = NetworkConfig.deviceName,
                    targetIP = targetIP,
                    timestamp = System.currentTimeMillis(),
                    params = params
                )
                
                Socket(targetIP, NetworkConfig.commandPort).use { socket ->
                    socket.soTimeout = 5000
                    
                    // Send command
                    socket.getOutputStream().write((packet.toJson() + "\n").toByteArray())
                    socket.getOutputStream().flush()
                    
                    // Read response
                    val response = socket.getInputStream().bufferedReader().readLine()
                    val responsePacket = ResponsePacket.fromJson(response)
                    onResponse(responsePacket)
                }
            } catch (e: Exception) {
                println("[Sender] Error sending to $targetIP: ${e.message}")
                onResponse(null)
            }
        }
    }
    
    fun broadcast(
        commandId: String,
        params: Map<String, String> = emptyMap(),
        onResponse: (String, ResponsePacket?) -> Unit = { _, _ -> }
    ) {
        val subnet = NetworkConfig.getSubnetPrefix()
        for (i in 1..254) {
            val ip = "$subnet$i"
            if (ip != NetworkConfig.getLocalIP()) {
                send(ip, commandId, params) { response ->
                    onResponse(ip, response)
                }
            }
        }
    }
}
