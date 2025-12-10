package com.lancontrol.network

import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import javax.imageio.ImageIO
import kotlin.concurrent.thread

/**
 * Handles screen capture and streaming
 */
object ScreenStreamer {
    const val SCREEN_PORT = 7892
    private var serverSocket: ServerSocket? = null
    private var running = false
    private var streamThread: Thread? = null
    
    fun startServer() {
        if (running) return
        running = true
        
        streamThread = thread(name = "ScreenStreamer") {
            try {
                serverSocket = ServerSocket(SCREEN_PORT)
                serverSocket?.soTimeout = 1000
                println("[ScreenStreamer] Server started on port $SCREEN_PORT")
                
                while (running) {
                    try {
                        val client = serverSocket?.accept()
                        client?.let { handleClient(it) }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Normal timeout
                    }
                }
            } catch (e: Exception) {
                println("[ScreenStreamer] Error: ${e.message}")
            }
        }
    }
    
    fun stopServer() {
        running = false
        serverSocket?.close()
        streamThread?.join(2000)
    }
    
    private fun handleClient(socket: Socket) {
        thread {
            try {
                socket.soTimeout = 10000
                val output = socket.getOutputStream()
                
                // Capture screen
                val robot = Robot()
                val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
                val screenshot = robot.createScreenCapture(screenRect)
                
                // Convert to JPEG bytes
                val baos = ByteArrayOutputStream()
                ImageIO.write(screenshot, "jpg", baos)
                val imageBytes = baos.toByteArray()
                
                // Send size first (4 bytes), then image data
                val size = imageBytes.size
                output.write(byteArrayOf(
                    (size shr 24).toByte(),
                    (size shr 16).toByte(),
                    (size shr 8).toByte(),
                    size.toByte()
                ))
                output.write(imageBytes)
                output.flush()
                
                println("[ScreenStreamer] Sent screenshot (${imageBytes.size} bytes)")
                
            } catch (e: Exception) {
                println("[ScreenStreamer] Client error: ${e.message}")
            } finally {
                socket.close()
            }
        }
    }
    
    /**
     * Request screenshot from remote machine
     */
    fun requestScreenshot(targetIP: String, onResult: (ByteArray?) -> Unit) {
        thread {
            try {
                Socket(targetIP, SCREEN_PORT).use { socket ->
                    socket.soTimeout = 10000
                    val input = socket.getInputStream()
                    
                    // Read size (4 bytes)
                    val sizeBytes = ByteArray(4)
                    input.read(sizeBytes)
                    val size = ((sizeBytes[0].toInt() and 0xFF) shl 24) or
                               ((sizeBytes[1].toInt() and 0xFF) shl 16) or
                               ((sizeBytes[2].toInt() and 0xFF) shl 8) or
                               (sizeBytes[3].toInt() and 0xFF)
                    
                    // Read image data
                    val imageBytes = ByteArray(size)
                    var totalRead = 0
                    while (totalRead < size) {
                        val read = input.read(imageBytes, totalRead, size - totalRead)
                        if (read == -1) break
                        totalRead += read
                    }
                    
                    onResult(imageBytes)
                }
            } catch (e: Exception) {
                println("[ScreenStreamer] Request error: ${e.message}")
                onResult(null)
            }
        }
    }
}
