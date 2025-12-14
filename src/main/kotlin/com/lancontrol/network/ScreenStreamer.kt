package com.lancontrol.network

import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.concurrent.thread

/**
 * Handles screen capture and streaming
 */
object ScreenStreamer {
    const val SCREEN_PORT = 7892
    private var serverSocket: ServerSocket? = null
    @Volatile private var running = false
    private var streamThread: Thread? = null
    private val executor = Executors.newCachedThreadPool()
    private val robot by lazy { Robot() }
    private val screenRect by lazy { Rectangle(Toolkit.getDefaultToolkit().screenSize) }
    
    fun startServer() {
        if (running) return
        running = true
        
        streamThread = thread(name = "ScreenStreamer") {
            try {
                serverSocket = ServerSocket(SCREEN_PORT).apply { soTimeout = 1000 }
                println("[ScreenStreamer] Server started on port $SCREEN_PORT")
                
                while (running) {
                    try {
                        serverSocket?.accept()?.let { executor.submit { handleClient(it) } }
                    } catch (_: java.net.SocketTimeoutException) {}
                }
            } catch (e: Exception) {
                println("[ScreenStreamer] Error: ${e.message}")
            }
        }
    }
    
    fun stopServer() {
        running = false
        executor.shutdown()
        serverSocket?.close()
        streamThread?.join(1000)
    }
    
    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val output = socket.getOutputStream()
            
            val screenshot = robot.createScreenCapture(screenRect)
            
            // Compress with quality 0.7 for faster transfer
            val baos = ByteArrayOutputStream(100000)
            val writer = ImageIO.getImageWritersByFormatName("jpg").next()
            val param = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = 0.7f
            }
            writer.output = ImageIO.createImageOutputStream(baos)
            writer.write(null, IIOImage(screenshot, null, null), param)
            writer.dispose()
            
            val imageBytes = baos.toByteArray()
            val size = imageBytes.size
            
            output.write(byteArrayOf((size shr 24).toByte(), (size shr 16).toByte(), (size shr 8).toByte(), size.toByte()))
            output.write(imageBytes)
            output.flush()
        } catch (e: Exception) {
            println("[ScreenStreamer] Client error: ${e.message}")
        } finally {
            socket.close()
        }
    }
    
    fun requestScreenshot(targetIP: String, onResult: (ByteArray?) -> Unit) {
        executor.submit {
            try {
                Socket(targetIP, SCREEN_PORT).use { socket ->
                    socket.soTimeout = 10000
                    val input = socket.getInputStream()
                    
                    val sizeBytes = ByteArray(4)
                    input.read(sizeBytes)
                    val size = ((sizeBytes[0].toInt() and 0xFF) shl 24) or
                               ((sizeBytes[1].toInt() and 0xFF) shl 16) or
                               ((sizeBytes[2].toInt() and 0xFF) shl 8) or
                               (sizeBytes[3].toInt() and 0xFF)
                    
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
                onResult(null)
            }
        }
    }
}
