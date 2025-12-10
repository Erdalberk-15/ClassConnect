package com.lancontrol.controllers

import com.lancontrol.network.ScreenStreamer
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.stage.Stage
import java.io.ByteArrayInputStream
import kotlin.concurrent.thread

/**
 * Window for viewing remote screen - LIVE
 */
object ScreenViewerController {
    
    private var viewerStage: Stage? = null
    private var imageView: ImageView? = null
    private var statusLabel: Label? = null
    private var fpsLabel: Label? = null
    private var currentTargetIP: String? = null
    
    @Volatile private var streaming = false
    private var streamThread: Thread? = null
    private var refreshInterval = 500L // milliseconds between frames
    
    fun showScreen(targetIP: String, deviceName: String) {
        currentTargetIP = targetIP
        
        Platform.runLater {
            if (viewerStage == null) {
                createViewerWindow()
            }
            
            viewerStage?.title = "Canlı Ekran - $deviceName ($targetIP)"
            viewerStage?.show()
            viewerStage?.toFront()
            
            // Start live streaming automatically
            startStreaming()
        }
    }
    
    private fun createViewerWindow() {
        val root = BorderPane()
        root.style = "-fx-background-color: #1e1e2e;"
        
        // Image view for screenshot
        imageView = ImageView().apply {
            isPreserveRatio = true
            fitWidth = 1280.0
            fitHeight = 720.0
        }
        root.center = imageView
        
        // Control bar
        val controlBar = HBox(15.0).apply {
            style = "-fx-background-color: #2d2d3d; -fx-padding: 10;"
            alignment = javafx.geometry.Pos.CENTER_LEFT
        }
        
        val stopBtn = Button("✕ Kapat").apply {
            style = "-fx-background-color: #f38ba8; -fx-text-fill: #1e1e2e; -fx-font-weight: bold; -fx-min-width: 100;"
            setOnAction {
                closeViewer()
            }
        }
        
        val speedLabel = Label("Hız:").apply {
            style = "-fx-text-fill: #cdd6f4;"
        }
        
        val speedSlider = Slider(100.0, 2000.0, 500.0).apply {
            prefWidth = 150.0
            isShowTickLabels = true
            isShowTickMarks = true
            majorTickUnit = 500.0
            valueProperty().addListener { _, _, newVal ->
                refreshInterval = newVal.toLong()
            }
        }
        
        fpsLabel = Label("").apply {
            style = "-fx-text-fill: #a6e3a1; -fx-font-weight: bold;"
        }
        
        val spacer = Region().apply {
            HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
        }
        
        statusLabel = Label("Bağlanıyor...").apply {
            style = "-fx-text-fill: #a6adc8;"
        }
        
        controlBar.children.addAll(stopBtn, speedLabel, speedSlider, fpsLabel, spacer, statusLabel)
        root.top = controlBar
        
        val scene = Scene(root, 1320.0, 800.0)
        
        viewerStage = Stage().apply {
            this.scene = scene
            isResizable = true
            setOnCloseRequest {
                stopStreaming()
            }
            setOnHidden {
                stopStreaming()
            }
        }
    }
    
    private fun startStreaming() {
        if (streaming) return
        streaming = true
        
        streamThread = thread(name = "ScreenViewer") {
            var frameCount = 0
            var lastFpsUpdate = System.currentTimeMillis()
            
            while (streaming) {
                val targetIP = currentTargetIP ?: break
                val startTime = System.currentTimeMillis()
                
                ScreenStreamer.requestScreenshot(targetIP) { imageBytes ->
                    if (imageBytes != null && streaming) {
                        Platform.runLater {
                            try {
                                val image = Image(ByteArrayInputStream(imageBytes))
                                imageView?.image = image
                                frameCount++
                                
                                // Update FPS every second
                                val now = System.currentTimeMillis()
                                if (now - lastFpsUpdate >= 1000) {
                                    fpsLabel?.text = "$frameCount FPS"
                                    frameCount = 0
                                    lastFpsUpdate = now
                                }
                                
                                statusLabel?.text = "Canlı"
                                statusLabel?.style = "-fx-text-fill: #a6e3a1;"
                            } catch (e: Exception) {
                                statusLabel?.text = "Hata"
                                statusLabel?.style = "-fx-text-fill: #f38ba8;"
                            }
                        }
                    } else if (streaming) {
                        Platform.runLater {
                            statusLabel?.text = "Bağlantı kesildi"
                            statusLabel?.style = "-fx-text-fill: #f38ba8;"
                        }
                    }
                }
                
                // Wait for next frame
                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = (refreshInterval - elapsed).coerceAtLeast(50)
                Thread.sleep(sleepTime)
            }
        }
    }
    
    private fun stopStreaming() {
        streaming = false
        thread {
            streamThread?.join(2000)
            streamThread = null
        }
    }
    
    private fun closeViewer() {
        stopStreaming()
        Platform.runLater {
            imageView?.image = null
            viewerStage?.hide()
        }
    }
}
