package com.lancontrol.actions

import javafx.animation.FadeTransition
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration

/**
 * Shows a message overlay on screen with sender name
 */
object MessageOverlay {
    private val senderFont = Font.font("Segoe UI", FontWeight.BOLD, 24.0)
    private val messageFont = Font.font("Segoe UI", FontWeight.NORMAL, 32.0)
    
    fun show(senderName: String, message: String) {
        Platform.runLater {
            val container = VBox(15.0).apply {
                alignment = Pos.CENTER
                style = "-fx-background-color: rgba(30,30,46,0.95); -fx-background-radius: 15; -fx-padding: 30 50; -fx-border-color: #89b4fa; -fx-border-width: 2; -fx-border-radius: 15;"
                children.addAll(
                    Label(senderName).apply { font = senderFont; textFill = Color.web("#89b4fa"); textAlignment = TextAlignment.CENTER },
                    Label(message).apply { font = messageFont; textFill = Color.WHITE; textAlignment = TextAlignment.CENTER; isWrapText = true; maxWidth = 600.0 }
                )
            }
            
            val scene = Scene(container).apply { fill = Color.TRANSPARENT }
            val screenBounds = Screen.getPrimary().visualBounds
            
            val stage = Stage().apply {
                initStyle(StageStyle.TRANSPARENT)
                isAlwaysOnTop = true
                this.scene = scene
                x = (screenBounds.width - 700) / 2
                y = (screenBounds.height - 200) / 2
                show()
            }
            
            FadeTransition(Duration.millis(300.0), container).apply { fromValue = 0.0; toValue = 1.0; play() }
            
            Timeline(KeyFrame(Duration.seconds(5.0), {
                FadeTransition(Duration.millis(500.0), container).apply {
                    fromValue = 1.0; toValue = 0.0
                    setOnFinished { stage.close() }
                    play()
                }
            })).play()
        }
    }
}
