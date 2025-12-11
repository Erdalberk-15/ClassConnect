package com.lancontrol.actions

import javafx.animation.RotateTransition
import javafx.animation.Animation
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration

/**
 * Shows donate overlay animation on screen
 */
object DonateOverlay {
    
    private var currentStage: Stage? = null
    private var rotateTransition: RotateTransition? = null
    
    fun show(senderName: String) {
        Platform.runLater {
            try {
                // Close previous if exists
                currentStage?.close()
                
                // Load image
                val imageUrl = javaClass.getResource("/com/lancontrol/images/eb.png")
                if (imageUrl == null) {
                    println("[DonateOverlay] eb.png not found")
                    return@runLater
                }
                
                val image = Image(imageUrl.toExternalForm())
                val imageView = ImageView(image).apply {
                    isPreserveRatio = true
                    fitWidth = 255.0  // 15% smaller than 300
                    fitHeight = 255.0
                }
                
                // Create colored text
                val senderText = Text(senderName).apply {
                    fill = Color.RED
                    font = Font.font("Arial", FontWeight.BOLD, 18.0)
                }
                
                val donatedText = Text(" Donated ").apply {
                    fill = Color.LIMEGREEN
                    font = Font.font("Arial", FontWeight.BOLD, 18.0)
                }
                
                val esadText = Text("Esad Bashar").apply {
                    fill = Color.DODGERBLUE
                    font = Font.font("Arial", FontWeight.BOLD, 18.0)
                }
                
                val textFlow = TextFlow(senderText, donatedText, esadText).apply {
                    style = "-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 8; -fx-background-radius: 5;"
                }
                
                val root = VBox(10.0, imageView, textFlow).apply {
                    alignment = Pos.CENTER
                    style = "-fx-background-color: transparent;"
                }
                
                val scene = Scene(root, 300.0, 330.0).apply {
                    fill = Color.TRANSPARENT
                }
                
                val stage = Stage().apply {
                    initStyle(StageStyle.TRANSPARENT)
                    isAlwaysOnTop = true
                    this.scene = scene
                    // Sol üst köşe, biraz içeride
                    x = 50.0
                    y = 50.0
                }
                
                currentStage = stage
                
                // Play sound once
                playDonateSound()
                
                // Show stage
                stage.show()
                
                // Rotate animation: -15 to +15 degrees, continuous
                rotateTransition = RotateTransition(Duration.millis(800.0), imageView).apply {
                    fromAngle = -15.0
                    toAngle = 15.0
                    cycleCount = Animation.INDEFINITE
                    isAutoReverse = true
                }
                rotateTransition?.play()
                
                // Auto close after 5 seconds
                Thread {
                    Thread.sleep(5000)
                    Platform.runLater {
                        close()
                    }
                }.start()
                
            } catch (e: Exception) {
                println("[DonateOverlay] Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun close() {
        rotateTransition?.stop()
        rotateTransition = null
        currentStage?.close()
        currentStage = null
    }
    
    private fun playDonateSound() {
        try {
            val soundUrl = javaClass.getResource("/com/lancontrol/sounds/donate.mp3")
            if (soundUrl == null) {
                println("[DonateOverlay] donate.mp3 not found")
                return
            }
            
            val media = Media(soundUrl.toExternalForm())
            val player = MediaPlayer(media).apply {
                cycleCount = 2 // Play twice
            }
            player.play()
            
        } catch (e: Exception) {
            println("[DonateOverlay] Sound error: ${e.message}")
        }
    }
}
