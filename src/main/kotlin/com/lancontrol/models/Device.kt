package com.lancontrol.models

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty

/**
 * Represents a device on the LAN
 */
data class Device(
    val ip: String,
    val name: String,
    val lastSeen: Long = System.currentTimeMillis()
) {
    val ipProperty = SimpleStringProperty(ip)
    val nameProperty = SimpleStringProperty(name)
    val onlineProperty = SimpleBooleanProperty(true)
    
    fun isOnline(): Boolean = System.currentTimeMillis() - lastSeen < 30000 // 30 sec timeout
}

/**
 * Predefined action that can be triggered on remote machines
 */
data class RemoteAction(
    val id: String,
    val name: String,
    val description: String,
    val commandId: String,
    val params: Map<String, String> = emptyMap(),
    val iconPath: String? = null
)
