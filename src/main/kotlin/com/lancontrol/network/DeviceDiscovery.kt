package com.lancontrol.network

import com.lancontrol.models.Device
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Discovers other devices running LANControl on the network
 */
object DeviceDiscovery {
    private const val DISCOVERY_MSG = "LANCONTROL_DISCOVER"
    private const val RESPONSE_PREFIX = "LANCONTROL_HERE:"
    
    @Volatile private var running = false
    private var discoveryThread: Thread? = null
    private var listenerThread: Thread? = null
    private val executor = Executors.newFixedThreadPool(4)
    
    val devices: ObservableList<Device> = FXCollections.observableArrayList()
    
    fun start() {
        if (running) return
        running = true
        
        listenerThread = thread(name = "DiscoveryListener") {
            try {
                DatagramSocket(NetworkConfig.DISCOVERY_PORT).use { socket ->
                    socket.soTimeout = 1000
                    val buffer = ByteArray(256)
                    val responseBytes = "$RESPONSE_PREFIX${NetworkConfig.deviceName}".toByteArray()
                    
                    while (running) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            
                            if (String(packet.data, 0, packet.length) == DISCOVERY_MSG) {
                                socket.send(DatagramPacket(responseBytes, responseBytes.size, packet.address, packet.port))
                            }
                        } catch (_: SocketTimeoutException) {}
                    }
                }
            } catch (e: Exception) {
                println("[Discovery] Listener error: ${e.message}")
            }
        }
        
        discoveryThread = thread(name = "DiscoveryBroadcast") {
            while (running) {
                scanNetwork()
                Thread.sleep(NetworkConfig.DISCOVERY_INTERVAL_MS)
            }
        }
    }
    
    fun stop() {
        running = false
        executor.shutdown()
        discoveryThread?.join(1000)
        listenerThread?.join(1000)
    }
    
    fun scanNetwork() {
        executor.submit {
            try {
                DatagramSocket().use { socket ->
                    socket.soTimeout = 1500
                    socket.broadcast = true
                    
                    val subnet = NetworkConfig.getSubnetPrefix()
                    val localIP = NetworkConfig.getLocalIP()
                    val message = DISCOVERY_MSG.toByteArray()
                    
                    // Batch send - faster
                    for (i in 1..254) {
                        val ip = "$subnet$i"
                        if (ip != localIP) {
                            try {
                                socket.send(DatagramPacket(message, message.size, InetAddress.getByName(ip), NetworkConfig.DISCOVERY_PORT))
                            } catch (_: Exception) {}
                        }
                    }
                    
                    val buffer = ByteArray(256)
                    val foundDevices = mutableListOf<Device>()
                    
                    while (true) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            
                            val response = String(packet.data, 0, packet.length)
                            if (response.startsWith(RESPONSE_PREFIX)) {
                                foundDevices.add(Device(packet.address.hostAddress, response.removePrefix(RESPONSE_PREFIX)))
                            }
                        } catch (_: SocketTimeoutException) { break }
                    }
                    
                    if (foundDevices.isNotEmpty() || devices.isNotEmpty()) {
                        Platform.runLater {
                            devices.setAll(foundDevices)
                        }
                    }
                }
            } catch (e: Exception) {
                println("[Discovery] Scan error: ${e.message}")
            }
        }
    }
}
