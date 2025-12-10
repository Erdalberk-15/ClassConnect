package com.lancontrol.models

import com.google.gson.Gson

/**
 * Command packet structure for LAN communication
 */
data class CommandPacket(
    val commandId: String,           // Unique command identifier (e.g., "LAUNCH_APP", "SHOW_NOTIFICATION")
    val senderIP: String,            // IP of the sending machine
    val senderName: String,          // Friendly name of sender
    val targetIP: String,            // Target machine IP ("*" for broadcast)
    val timestamp: Long,             // Unix timestamp
    val params: Map<String, String> = emptyMap()  // Optional parameters
) {
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): CommandPacket? {
            return try {
                Gson().fromJson(json, CommandPacket::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Response packet sent back to sender
 */
data class ResponsePacket(
    val success: Boolean,
    val commandId: String,
    val responderIP: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String = Gson().toJson(this)
    
    companion object {
        fun fromJson(json: String): ResponsePacket? {
            return try {
                Gson().fromJson(json, ResponsePacket::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
