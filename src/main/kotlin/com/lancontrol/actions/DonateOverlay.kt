package com.lancontrol.actions

import javafx.animation.TranslateTransition
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import java.awt.Toolkit

/**
 * Shows donate overlay animation on screen
 */
object DonateOverlay {
    
    fun show() {
        Platform.runLater {
            try {
                val screenSize = Toolkit.getDefaultToolkit().screenSize
                val screenWidth = screenSize.width.toDouble()
                val screenHeight = screenSize.height.toDouble()
                
                // Load image
                val imageUrl = javaClass.getResource("/com/lancontrol/images/eb.png")
                if (imageUrl == null) {
                    println("[DonateOverlay] eb.png not found")
                    return@runLater
                }
                
                val image = Image(imageUrl.toExternalForm())
                val imageView = ImageView(image).apply {
                    isPreserveRatio = true
                    fitWidth = 400.0
                    fitHeight = 400.0
                }
                
                val root = StackPane(imageView).apply {
                    style = "-fx-background-color: transparent;"
                }
                
                val scene = Scene(root, 400.0, 400.0).apply {
                    fill = javafx.scene.paint.Color.TRANSPARENT
                }
                
                val stage = Stage().apply {
                    initStyle(StageStyle.TRANSPARENT)
                    isAlwaysOnTop = true
                    this.scene = scene
                    x = (screenWidth - 400) / 2
                    y = screenHeight // Start below screen
                }
                
                // Play sound twice
                playDonateSound()
                
                // Show stage
                stage.show()
                
                // Animate from bottom to center
                val transition = TranslateTransition(Duration.millis(1500.0), imageView).apply {
                    fromY = screenHeight
                    toY = -(screenHeight / 2 - 200)
                    setOnFinished {
                        // Wait a bit then close
                        Thread {
                            Thread.sleep(3000)
                            Platform.runLater {
                                stage.close()
                            }
                        }.start()
                    }
                }
                transition.play()
                
            } catch (e: Exception) {
                println("[DonateOverlay] Error: ${e.message}")
                e.printStackTrace()
            }
        }
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
