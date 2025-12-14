package com.lancontrol.models

/**
 * Represents a device on the LAN
 */
data class Device(
    val ip: String,
    val name: String,
    val lastSeen: Long = System.currentTimeMillis()
) {
    fun isOnline(): Boolean = System.currentTimeMillis() - lastSeen < 30000
}

/**
 * Predefined action that can be triggered on remote machines
 */
data class RemoteAction(
    val id: String,
    val name: String,
    val description: String,
    val commandId: String
)
