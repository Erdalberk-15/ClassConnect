package com.lancontrol.actions

import javafx.animation.RotateTransition
import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
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
    private val boldFont = Font.font("Arial", FontWeight.BOLD, 18.0)
    
    fun show(senderName: String) {
        Platform.runLater {
            try {
                close()
                
                val imageUrl = javaClass.getResource("/com/lancontrol/images/eb.png") ?: return@runLater
                val imageView = ImageView(Image(imageUrl.toExternalForm())).apply {
                    isPreserveRatio = true
                    fitWidth = 255.0
                    fitHeight = 255.0
                }
                
                val textFlow = TextFlow(
                    Text(senderName).apply { fill = Color.RED; font = boldFont },
                    Text(" Donated ").apply { fill = Color.LIMEGREEN; font = boldFont },
                    Text("Esad Bashar").apply { fill = Color.DODGERBLUE; font = boldFont }
                ).apply {
                    style = "-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 8; -fx-background-radius: 5;"
                }
                
                val scene = Scene(VBox(10.0, imageView, textFlow).apply {
                    alignment = Pos.CENTER
                    style = "-fx-background-color: transparent;"
                }, 300.0, 330.0).apply { fill = Color.TRANSPARENT }
                
                currentStage = Stage().apply {
                    initStyle(StageStyle.TRANSPARENT)
                    isAlwaysOnTop = true
                    this.scene = scene
                    x = 50.0; y = 50.0
                    show()
                }
                
                playDonateSound()
                
                rotateTransition = RotateTransition(Duration.millis(800.0), imageView).apply {
                    fromAngle = -15.0; toAngle = 15.0
                    cycleCount = Animation.INDEFINITE
                    isAutoReverse = true
                    play()
                }
                
                Timeline(KeyFrame(Duration.seconds(5.0), { close() })).play()
            } catch (e: Exception) {
                println("[DonateOverlay] Error: ${e.message}")
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
            val soundUrl = javaClass.getResource("/com/lancontrol/sounds/donate.mp3") ?: return
            MediaPlayer(Media(soundUrl.toExternalForm())).apply { cycleCount = 2; play() }
        } catch (e: Exception) {
            println("[DonateOverlay] Sound error: ${e.message}")
        }
    }
}
