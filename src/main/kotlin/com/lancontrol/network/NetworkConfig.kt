package com.lancontrol.network

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.prefs.Preferences

object NetworkConfig {
    const val DEFAULT_PORT = 7890
    const val DISCOVERY_PORT = 7891
    const val BUFFER_SIZE = 4096
    const val DISCOVERY_INTERVAL_MS = 5000L
    
    private val prefs = Preferences.userNodeForPackage(NetworkConfig::class.java)
    
    const val DEVICE_NAME = "9A Sınıfı"
    
    val deviceName: String
        get() = DEVICE_NAME
    
    var commandPort: Int
        get() = prefs.getInt("command_port", DEFAULT_PORT)
        set(value) = prefs.putInt("command_port", value)
    
    fun getLocalIP(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filter { !it.isLoopbackAddress && it.hostAddress.contains(".") }
                .map { it.hostAddress }
                .firstOrNull() ?: "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
    
    fun getHostName(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    fun getSubnetPrefix(): String {
        val ip = getLocalIP()
        return ip.substringBeforeLast(".") + "."
    }
}
