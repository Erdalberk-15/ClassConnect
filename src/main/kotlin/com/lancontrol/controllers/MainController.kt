package com.lancontrol.controllers

import com.lancontrol.Main
import com.lancontrol.actions.ActionRegistry
import com.lancontrol.models.Device
import com.lancontrol.models.CommandPacket
import com.lancontrol.network.CommandSender
import com.lancontrol.network.DeviceDiscovery
import com.lancontrol.network.NetworkConfig
import com.lancontrol.network.FileTransfer
import com.lancontrol.utils.TimeRestriction
import javafx.stage.FileChooser
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
import java.util.Optional

class MainController : Initializable {
    
    @FXML lateinit var deviceListView: ListView<Device>
    @FXML lateinit var actionPane: FlowPane
    @FXML lateinit var logArea: TextArea
    @FXML lateinit var statusLabel: Label
    @FXML lateinit var localIPLabel: Label
    @FXML lateinit var targetIPField: TextField
    @FXML lateinit var lockButton: Button
    private var selectedDevice: Device? = null
    
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        setupUI()
        setupDeviceList()
        setupActionButtons()
        setupCommandListener()
        startDiscovery()
        startLockButtonUpdater()
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
                    text = if (empty || item == null) null else "${item.name} (${item.ip})"
                    graphic = null
                    style = if (item?.isOnline() == true) "-fx-text-fill: green;" else "-fx-text-fill: gray;"
                }
            }
        }
        deviceListView.selectionModel.selectedItemProperty().addListener { _, _, d -> selectedDevice = d }
    }
    
    private fun setupActionButtons() {
        actionPane.children.clear()
        actionPane.hgap = 10.0
        actionPane.vgap = 10.0
        actionPane.padding = Insets(10.0)
        
        fun createButton(text: String, vararg styles: String, action: () -> Unit) = Button(text).apply {
            prefWidth = 120.0; prefHeight = 60.0
            styleClass.addAll("action-button", *styles)
            setOnAction { if (checkTimeRestriction()) action() }
        }
        
        actionPane.children.addAll(
            createButton("Ekran Göster", "screen-button") { showRemoteScreen() },
            createButton("Dosya Gönder", "file-button") { sendFile() },
            createButton("Mesaj Gönder", "message-button") { sendMessage() },
            createButton("VIP Komutlar", "vip-button") { showVIPCommands() }
        )
        
        ActionRegistry.getAvailableActions().filter { 
            it.commandId !in listOf("SHUTDOWN", "RESTART", "LOCK_SCREEN", "PING", "SHOW_NOTIFICATION")
        }.forEach { action ->
            actionPane.children.add(createButton(action.name) { executeAction(action.commandId) }.apply {
                tooltip = Tooltip(action.description)
            })
        }
    }
    
    /**
     * Check if action is allowed based on time restriction
     * Returns true if allowed, false if restricted
     */
    private fun checkTimeRestriction(): Boolean {
        if (!TimeRestriction.isRestricted()) {
            return true
        }
        
        // Show restriction dialog
        val alert = Alert(Alert.AlertType.WARNING)
        alert.title = "Erişim Engellendi"
        alert.headerText = "Ders Saatleri İçinde Bu Uygulamaya Erişim Yetkiniz Yok"
        alert.contentText = "Admin şifresi ile geçici erişim sağlayabilirsiniz."
        
        val adminButton = ButtonType("Admin Girişi")
        val cancelButton = ButtonType("İptal", ButtonBar.ButtonData.CANCEL_CLOSE)
        
        alert.buttonTypes.setAll(adminButton, cancelButton)
        
        val result = alert.showAndWait()
        
        if (result.isPresent && result.get() == adminButton) {
            return showAdminPasswordDialog()
        }
        
        return false
    }
    
    /**
     * Show admin password dialog
     * Returns true if correct password entered
     */
    private fun showAdminPasswordDialog(): Boolean {
        val dialog = TextInputDialog()
        dialog.title = "Admin Girişi"
        dialog.headerText = "Admin Şifresi"
        dialog.contentText = "Şifre:"
        
        // Make it a password field
        val passwordField = PasswordField()
        passwordField.promptText = "Admin şifresi girin"
        dialog.editor.isVisible = false
        dialog.editor.isManaged = false
        dialog.dialogPane.content = VBox(10.0, Label("Admin şifresini girin:"), passwordField)
        
        dialog.showAndWait()
        
        val password = passwordField.text
        
        if (password.isNotEmpty() && TimeRestriction.tryUnlock(password)) {
            showAlert("Başarılı", "5 dakika için erişim sağlandı.")
            log("[ADMIN] Geçici erişim sağlandı (5 dakika)")
            return true
        } else if (password.isNotEmpty()) {
            showAlert("Hata", "Yanlış şifre!")
        }
        
        return false
    }
    
    private fun getTarget(): Pair<String, String>? {
        val ip = targetIPField.text.trim().ifEmpty { selectedDevice?.ip }
        if (ip.isNullOrEmpty()) {
            showAlert("Hata", "Lütfen bir IP girin veya listeden cihaz seçin")
            return null
        }
        return ip to (selectedDevice?.name ?: "Manual")
    }
    
    private fun showRemoteScreen() {
        val (ip, name) = getTarget() ?: return
        log("[SCREEN] $name ($ip) ekranı görüntüleniyor...")
        updateStatus("Ekran alınıyor...")
        ScreenViewerController.showScreen(ip, name)
    }
    
    private fun sendFile() {
        val (ip, name) = getTarget() ?: return
        FileChooser().apply { title = "Gönderilecek Dosyayı Seçin" }
            .showOpenDialog(deviceListView.scene.window)?.let { file ->
                log("[FILE] $name ($ip) cihazına dosya gönderiliyor: ${file.name}")
                updateStatus("Dosya gönderiliyor...")
                FileTransfer.sendFile(ip, file,
                    onProgress = { Platform.runLater { updateStatus("Gönderiliyor: %$it") } },
                    onComplete = { success, msg ->
                        Platform.runLater {
                            if (success) { log("[FILE] Başarılı: $msg"); updateStatus("Dosya gönderildi"); showInfo("Başarılı", msg) }
                            else { log("[FILE] Hata: $msg"); updateStatus("Gönderim başarısız"); showAlert("Hata", msg) }
                        }
                    }
                )
            }
    }
    
    private fun sendMessage() {
        val (ip, name) = getTarget() ?: return
        TextInputDialog().apply { title = "Mesaj Gönder"; headerText = "Gönderilecek mesajı yazın"; contentText = "Mesaj:" }
            .showAndWait().filter { it.isNotEmpty() }.ifPresent { message ->
                log("[MESSAGE] $name ($ip) cihazına mesaj gönderiliyor: $message")
                updateStatus("Mesaj gönderiliyor...")
                CommandSender.send(ip, "SEND_MESSAGE", mapOf("message" to message)) { response ->
                    Platform.runLater {
                        val ok = response?.success == true
                        log("[MESSAGE] Mesaj ${if (ok) "gönderildi" else "gönderilemedi"}")
                        updateStatus(if (ok) "Mesaj gönderildi" else "Mesaj gönderilemedi")
                    }
                }
            }
    }
    
    private fun showVIPCommands() {
        val (ip, name) = getTarget() ?: return
        
        // VIP şifre kontrolü
        if (!checkVIPPassword()) return
        
        VIPCommandsController.show(ip, name) { cmdName, success, message ->
            val status = if (success) "SUCCESS" else "FAILED"
            log("[VIP] $cmdName: $status - $message")
            updateStatus("$cmdName: $status")
        }
    }
    
    private fun checkVIPPassword(): Boolean {
        val dialog = TextInputDialog()
        dialog.title = "VIP Komutlar"
        dialog.headerText = "VIP Şifresini Girin"
        dialog.contentText = "Şifre:"
        
        val passwordField = PasswordField()
        passwordField.promptText = "VIP şifresi"
        dialog.editor.isVisible = false
        dialog.editor.isManaged = false
        dialog.dialogPane.content = VBox(10.0, Label("VIP şifresini girin:"), passwordField)
        
        dialog.showAndWait()
        
        val password = passwordField.text
        if (password == "3169") {
            log("[VIP] VIP erişimi sağlandı")
            return true
        } else if (password.isNotEmpty()) {
            showAlert("Hata", "Yanlış VIP şifresi!")
        }
        return false
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
        
        // Setup file received notification
        FileTransfer.onFileReceived = { file ->
            Platform.runLater {
                log("[FILE] Dosya alındı: ${file.name}")
                showInfo("Dosya Alındı", "Dosya kaydedildi:\n${file.absolutePath}")
            }
        }
    }
    
    private fun startDiscovery() {
        DeviceDiscovery.start()
        updateStatus("Discovery started")
    }
    
    private fun executeAction(commandId: String) {
        val (ip, name) = getTarget() ?: return
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
    
    @FXML
    fun onLockApp() {
        TimeRestriction.lock()
        lockButton.isVisible = false
        log("[ADMIN] Uygulama kilitlendi")
        updateStatus("Kilitlendi")
    }
    
    /**
     * Periodically check if admin is unlocked and show/hide lock button
     */
    private fun startLockButtonUpdater() {
        val timer = java.util.Timer(true)
        timer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                Platform.runLater {
                    lockButton.isVisible = TimeRestriction.isUnlocked()
                }
            }
        }, 0, 1000) // Check every second
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
    
    private fun showInfo(title: String, message: String) {
        Alert(Alert.AlertType.INFORMATION).apply {
            this.title = title
            headerText = null
            contentText = message
        }.showAndWait()
    }
}
