package com.lancontrol.controllers

import com.lancontrol.actions.ActionRegistry
import com.lancontrol.network.CommandSender
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.FlowPane
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage

/**
 * VIP Commands window with dangerous/admin commands
 */
object VIPCommandsController {
    private var stage: Stage? = null
    
    data class VIPCommand(val name: String, val commandId: String, val icon: String, val color: String)
    
    private val vipCommands = listOf(
        VIPCommand("Bilgisayarı Kapat", ActionRegistry.Commands.SHUTDOWN, "⏻", "#f38ba8"),
        VIPCommand("Yeniden Başlat", ActionRegistry.Commands.RESTART, "🔄", "#fab387"),
        VIPCommand("Ekranı Kilitle", ActionRegistry.Commands.LOCK_SCREEN, "🔒", "#a6e3a1"),
        VIPCommand("Bildirim Gönder", ActionRegistry.Commands.SHOW_NOTIFICATION, "🔔", "#89b4fa"),
        VIPCommand("Ping", ActionRegistry.Commands.PING, "📡", "#cba6f7")
    )
    
    fun show(targetIP: String, targetName: String, onCommandExecuted: (String, Boolean, String) -> Unit) {
        Platform.runLater {
            if (stage != null) {
                stage?.toFront()
                return@runLater
            }
            
            val root = VBox(20.0).apply {
                padding = Insets(20.0)
                alignment = Pos.TOP_CENTER
                style = "-fx-background-color: #1e1e2e;"
            }
            
            val titleLabel = Label("VIP Komutlar").apply {
                style = "-fx-text-fill: #f38ba8; -fx-font-size: 24px; -fx-font-weight: bold;"
            }
            
            val targetLabel = Label("Hedef: $targetName ($targetIP)").apply {
                style = "-fx-text-fill: #a6adc8; -fx-font-size: 14px;"
            }
            
            val warningLabel = Label("⚠️ Bu komutlar hedef bilgisayarı doğrudan etkiler!").apply {
                style = "-fx-text-fill: #f9e2af; -fx-font-size: 12px;"
            }
            
            val buttonPane = FlowPane().apply {
                hgap = 15.0
                vgap = 15.0
                alignment = Pos.CENTER
                padding = Insets(10.0)
            }
            
            vipCommands.forEach { cmd ->
                val btn = Button("${cmd.icon}\n${cmd.name}").apply {
                    prefWidth = 140.0
                    prefHeight = 80.0
                    style = """
                        -fx-background-color: ${cmd.color};
                        -fx-text-fill: #1e1e2e;
                        -fx-font-weight: bold;
                        -fx-font-size: 13px;
                        -fx-background-radius: 10;
                        -fx-cursor: hand;
                    """.trimIndent()
                    
                    setOnAction {
                        executeVIPCommand(targetIP, cmd, onCommandExecuted)
                    }
                }
                buttonPane.children.add(btn)
            }
            
            val closeBtn = Button("Kapat").apply {
                prefWidth = 100.0
                style = "-fx-background-color: #45475a; -fx-text-fill: #cdd6f4;"
                setOnAction { close() }
            }
            
            root.children.addAll(titleLabel, targetLabel, warningLabel, buttonPane, closeBtn)
            
            stage = Stage().apply {
                title = "VIP Komutlar"
                scene = Scene(root, 500.0, 400.0)
                initModality(Modality.APPLICATION_MODAL)
                isResizable = false
                setOnCloseRequest { stage = null }
                show()
            }
        }
    }
    
    private fun executeVIPCommand(targetIP: String, cmd: VIPCommand, onResult: (String, Boolean, String) -> Unit) {
        CommandSender.send(targetIP, cmd.commandId) { response ->
            Platform.runLater {
                if (response != null) {
                    onResult(cmd.name, response.success, response.message)
                } else {
                    onResult(cmd.name, false, "Yanıt alınamadı")
                }
            }
        }
    }
    
    fun close() {
        stage?.close()
        stage = null
    }
}
