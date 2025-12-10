package com.lancontrol.controllers

import com.lancontrol.Main
import com.lancontrol.actions.ActionRegistry
import com.lancontrol.models.Device
import com.lancontrol.models.CommandPacket
import com.lancontrol.network.CommandSender
import com.lancontrol.network.DeviceDiscovery
import com.lancontrol.network.NetworkConfig
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.scene.layout.FlowPane
import javafx.geometry.Insets
import java.net.URL
import java.util.ResourceBundle

class MainController : Initializable {
    
    @FXML lateinit var deviceListView: ListView<Device>
    @FXML lateinit var actionPane: FlowPane
    @FXML lateinit var logArea: TextArea
    @FXML lateinit var statusLabel: Label
    @FXML lateinit var localIPLabel: Label
    @FXML lateinit var targetIPField: TextField
    private var selectedDevice: Device? = null
    
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        setupUI()
        setupDeviceList()
        setupActionButtons()
        setupCommandListener()
        startDiscovery()
    }
    
    private fun setupUI() {
        localIPLabel.text = "Local IP: ${NetworkConfig.getLocalIP()}"
    }
    
    private fun setupDeviceList() {
        deviceListView.items = DeviceDiscovery.devices
        deviceListView.setCellFactory {
            object : ListCell<Device>() {
                override fun updateItem(item: Device?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                        graphic = null
                    } else {
                        text = "${item.name} (${item.ip})"
                        style = if (item.isOnline()) "-fx-text-fill: green;" else "-fx-text-fill: gray;"
                    }
                }
            }
        }
        
        deviceListView.selectionModel.selectedItemProperty().addListener { _, _, device ->
            selectedDevice = device
        }
    }
    
    private fun setupActionButtons() {
        actionPane.children.clear()
        actionPane.hgap = 10.0
        actionPane.vgap = 10.0
        actionPane.padding = Insets(10.0)
        
        // Add Show Screen button first
        val screenBtn = Button("Ekran Göster").apply {
            prefWidth = 120.0
            prefHeight = 60.0
            styleClass.add("action-button")
            styleClass.add("screen-button")
            tooltip = Tooltip("Seçili cihazın ekranını görüntüle")
            
            setOnAction {
                showRemoteScreen()
            }
        }
        actionPane.children.add(screenBtn)
        
        ActionRegistry.getAvailableActions().forEach { action ->
            val btn = Button(action.name).apply {
                prefWidth = 120.0
                prefHeight = 60.0
                styleClass.add("action-button")
                tooltip = Tooltip(action.description)
                
                setOnAction {
                    executeAction(action.commandId)
                }
            }
            actionPane.children.add(btn)
        }
    }
    
    private fun showRemoteScreen() {
        // Use manual IP if entered, otherwise use selected device
        val ip = targetIPField.text.trim().ifEmpty { selectedDevice?.ip }
        val name = selectedDevice?.name ?: "Manual"
        
        if (ip.isNullOrEmpty()) {
            showAlert("Hata", "Lütfen bir IP girin veya listeden cihaz seçin")
            return
        }
        
        log("[SCREEN] $name ($ip) ekranı görüntüleniyor...")
        updateStatus("Ekran alınıyor...")
        
        ScreenViewerController.showScreen(ip, name)
    }
    
    private fun setupCommandListener() {
        Main.commandListener.onCommandReceived.add { packet ->
            Platform.runLater {
                log("[RECEIVED] ${packet.commandId} from ${packet.senderName} (${packet.senderIP})")
            }
        }
        
        Main.commandListener.onCommandExecuted.add { packet, success, message ->
            Platform.runLater {
                val status = if (success) "SUCCESS" else "FAILED"
                log("[EXECUTED] ${packet.commandId}: $status - $message")
            }
        }
    }
    
    private fun startDiscovery() {
        DeviceDiscovery.start()
        updateStatus("Discovery started")
    }
    
    private fun executeAction(commandId: String) {
        // Use manual IP if entered, otherwise use selected device
        val ip = targetIPField.text.trim().ifEmpty { selectedDevice?.ip }
        val name = selectedDevice?.name ?: "Manual"
        
        if (ip.isNullOrEmpty()) {
            showAlert("Error", "Please enter an IP or select a device from the list")
            return
        }
        
        log("[SENDING] $commandId to $name ($ip)")
        updateStatus("Sending command...")
        
        CommandSender.send(ip, commandId) { response ->
            Platform.runLater {
                if (response != null) {
                    val status = if (response.success) "SUCCESS" else "FAILED"
                    log("[RESPONSE] $status: ${response.message}")
                    updateStatus("Command completed: $status")
                } else {
                    log("[RESPONSE] No response from $ip")
                    updateStatus("No response")
                }
            }
        }
    }
    
    @FXML
    fun onRefreshDevices() {
        log("[DISCOVERY] Scanning network...")
        DeviceDiscovery.scanNetwork()
        updateStatus("Scanning...")
    }
    
    @FXML
    fun onClearLog() {
        logArea.clear()
    }
    
    private fun log(message: String) {
        val timestamp = java.time.LocalTime.now().toString().substringBefore(".")
        logArea.appendText("[$timestamp] $message\n")
    }
    
    private fun updateStatus(status: String) {
        statusLabel.text = status
    }
    
    private fun showAlert(title: String, message: String) {
        Alert(Alert.AlertType.WARNING).apply {
            this.title = title
            headerText = null
            contentText = message
        }.showAndWait()
    }
}
