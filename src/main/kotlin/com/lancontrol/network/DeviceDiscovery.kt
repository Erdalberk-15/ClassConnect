package com.lancontrol.network

import com.lancontrol.models.Device
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

/**
 * Discovers other devices running LANControl on the network
 */
object DeviceDiscovery {
    private const val DISCOVERY_MSG = "LANCONTROL_DISCOVER"
    private const val RESPONSE_PREFIX = "LANCONTROL_HERE:"
    
    private var running = false
    private var discoveryThread: Thread? = null
    private var listenerThread: Thread? = null
    
    val devices: ObservableList<Device> = FXCollections.observableArrayList()
    
    fun start() {
        if (running) return
        running = true
        
        // Start listener for discovery requests
        listenerThread = thread(name = "DiscoveryListener") {
            try {
                DatagramSocket(NetworkConfig.DISCOVERY_PORT).use { socket ->
                    socket.soTimeout = 1000
                    val buffer = ByteArray(NetworkConfig.BUFFER_SIZE)
                    
                    while (running) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            
                            val message = String(packet.data, 0, packet.length)
                            if (message == DISCOVERY_MSG) {
                                // Respond with our info
                                val response = "$RESPONSE_PREFIX${NetworkConfig.deviceName}"
                                val responseData = response.toByteArray()
                                val responsePacket = DatagramPacket(
                                    responseData, responseData.size,
                                    packet.address, packet.port
                                )
                                socket.send(responsePacket)
                            }
                        } catch (e: SocketTimeoutException) {
                            // Normal timeout
                        }
                    }
                }
            } catch (e: Exception) {
                println("[Discovery] Listener error: ${e.message}")
            }
        }
        
        // Start periodic discovery broadcasts
        discoveryThread = thread(name = "DiscoveryBroadcast") {
            while (running) {
                scanNetwork()
                Thread.sleep(NetworkConfig.DISCOVERY_INTERVAL_MS)
            }
        }
    }
    
    fun stop() {
        running = false
        discoveryThread?.join(2000)
        listenerThread?.join(2000)
    }
    
    fun scanNetwork() {
        thread {
            try {
                DatagramSocket().use { socket ->
                    socket.soTimeout = 2000
                    socket.broadcast = true
                    
                    val subnet = NetworkConfig.getSubnetPrefix()
                    val message = DISCOVERY_MSG.toByteArray()
                    
                    // Send to all IPs in subnet
                    for (i in 1..254) {
                        val ip = "$subnet$i"
                        if (ip != NetworkConfig.getLocalIP()) {
                            try {
                                val packet = DatagramPacket(
                                    message, message.size,
                                    InetAddress.getByName(ip),
                                    NetworkConfig.DISCOVERY_PORT
                                )
                                socket.send(packet)
                            } catch (e: Exception) {
                                // Skip unreachable
                            }
                        }
                    }
                    
                    // Listen for responses
                    val buffer = ByteArray(NetworkConfig.BUFFER_SIZE)
                    val foundDevices = mutableListOf<Device>()
                    
                    while (true) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            
                            val response = String(packet.data, 0, packet.length)
                            if (response.startsWith(RESPONSE_PREFIX)) {
                                val name = response.removePrefix(RESPONSE_PREFIX)
                                val ip = packet.address.hostAddress
                                foundDevices.add(Device(ip, name))
                            }
                        } catch (e: SocketTimeoutException) {
                            break
                        }
                    }
                    
                    Platform.runLater {
                        devices.clear()
                        devices.addAll(foundDevices)
                    }
                }
            } catch (e: Exception) {
                println("[Discovery] Scan error: ${e.message}")
            }
        }
    }
}
