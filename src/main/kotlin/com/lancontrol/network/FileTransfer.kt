package com.lancontrol.network

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Handles file transfer between devices
 */
object FileTransfer {
    const val FILE_PORT = 7893
    private var serverSocket: ServerSocket? = null
    private var running = false
    
    // Callback when file is received
    var onFileReceived: ((File) -> Unit)? = null
    
    fun startServer() {
        if (running) return
        running = true
        
        thread(name = "FileTransferServer") {
            try {
                serverSocket = ServerSocket(FILE_PORT)
                serverSocket?.soTimeout = 1000
                println("[FileTransfer] Server started on port $FILE_PORT")
                
                while (running) {
                    try {
                        val client = serverSocket?.accept()
                        client?.let { handleIncomingFile(it) }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Normal timeout
                    }
                }
            } catch (e: Exception) {
                println("[FileTransfer] Server error: ${e.message}")
            }
        }
    }
    
    fun stopServer() {
        running = false
        serverSocket?.close()
    }
    
    private fun handleIncomingFile(socket: Socket) {
        thread {
            try {
                socket.soTimeout = 60000 // 60 sec timeout for large files
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                
                // Read filename length (4 bytes)
                val nameLen = readInt(input)
                
                // Read filename
                val nameBytes = ByteArray(nameLen)
                input.read(nameBytes)
                val fileName = String(nameBytes, Charsets.UTF_8)
                
                // Read file size (8 bytes)
                val fileSize = readLong(input)
                
                // Create downloads folder on Desktop
                val desktopPath = File(System.getProperty("user.home"), "Desktop")
                val downloadsDir = File(desktopPath, "ClassConnecter")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                // Create file
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { fos ->
                    val buffer = ByteArray(8192)
                    var totalRead = 0L
                    
                    while (totalRead < fileSize) {
                        val toRead = minOf(buffer.size.toLong(), fileSize - totalRead).toInt()
                        val read = input.read(buffer, 0, toRead)
                        if (read == -1) break
                        fos.write(buffer, 0, read)
                        totalRead += read
                    }
                }
                
                // Send success response
                output.write(1)
                output.flush()
                
                println("[FileTransfer] Received: $fileName (${fileSize} bytes)")
                
                // Notify callback
                onFileReceived?.invoke(file)
                
            } catch (e: Exception) {
                println("[FileTransfer] Receive error: ${e.message}")
            } finally {
                socket.close()
            }
        }
    }
    
    /**
     * Send a file to target device
     */
    fun sendFile(targetIP: String, file: File, onProgress: (Int) -> Unit, onComplete: (Boolean, String) -> Unit) {
        thread {
            try {
                Socket(targetIP, FILE_PORT).use { socket ->
                    socket.soTimeout = 60000
                    val output = socket.getOutputStream()
                    val input = socket.getInputStream()
                    
                    val fileName = file.name
                    val fileSize = file.length()
                    
                    // Send filename length (4 bytes)
                    writeInt(output, fileName.toByteArray(Charsets.UTF_8).size)
                    
                    // Send filename
                    output.write(fileName.toByteArray(Charsets.UTF_8))
                    
                    // Send file size (8 bytes)
                    writeLong(output, fileSize)
                    
                    // Send file data
                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(8192)
                        var totalSent = 0L
                        var read: Int
                        
                        while (fis.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            totalSent += read
                            val progress = ((totalSent * 100) / fileSize).toInt()
                            onProgress(progress)
                        }
                    }
                    
                    output.flush()
                    
                    // Wait for response
                    val response = input.read()
                    if (response == 1) {
                        onComplete(true, "Dosya gönderildi: $fileName")
                    } else {
                        onComplete(false, "Dosya gönderilemedi")
                    }
                }
            } catch (e: Exception) {
                println("[FileTransfer] Send error: ${e.message}")
                onComplete(false, "Hata: ${e.message}")
            }
        }
    }
    
    private fun readInt(input: java.io.InputStream): Int {
        val bytes = ByteArray(4)
        input.read(bytes)
        return ((bytes[0].toInt() and 0xFF) shl 24) or
               ((bytes[1].toInt() and 0xFF) shl 16) or
               ((bytes[2].toInt() and 0xFF) shl 8) or
               (bytes[3].toInt() and 0xFF)
    }
    
    private fun writeInt(output: java.io.OutputStream, value: Int) {
        output.write(byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        ))
    }
    
    private fun readLong(input: java.io.InputStream): Long {
        val bytes = ByteArray(8)
        input.read(bytes)
        return ((bytes[0].toLong() and 0xFF) shl 56) or
               ((bytes[1].toLong() and 0xFF) shl 48) or
               ((bytes[2].toLong() and 0xFF) shl 40) or
               ((bytes[3].toLong() and 0xFF) shl 32) or
               ((bytes[4].toLong() and 0xFF) shl 24) or
               ((bytes[5].toLong() and 0xFF) shl 16) or
               ((bytes[6].toLong() and 0xFF) shl 8) or
               (bytes[7].toLong() and 0xFF)
    }
    
    private fun writeLong(output: java.io.OutputStream, value: Long) {
        output.write(byteArrayOf(
            (value shr 56).toByte(),
            (value shr 48).toByte(),
            (value shr 40).toByte(),
            (value shr 32).toByte(),
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        ))
    }
}
