package com.damaru.doorbell

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Date

enum class ConnectionState {
    CONNECTED, DATA_PRESENT, DISCONNECTED, UNKNOWN
}

class DoorbellModel : ViewModel() {

    companion object {
        const val TAG = "Doorbell"
    }

    private val _connected = mutableStateOf<ConnectionState>(ConnectionState.DISCONNECTED)
    private val _sensorConnected = mutableStateOf<ConnectionState>(ConnectionState.UNKNOWN)
    private val _lastMessage = mutableStateOf<String>("")
    private val _lastMessageReceived = mutableStateOf<String>("")
    private val _deliberatelyDisconnected = mutableStateOf<Boolean>(false)

    val connected: State<ConnectionState>
        get() = _connected

    val sensorConnected: State<ConnectionState>
        get() = _sensorConnected

    val lastMessage: State<String>
        get() = _lastMessage

    val lastMessageReceived: State<String>
        get() = _lastMessageReceived

    val deliberatelyDisconnected: State<Boolean>
        get() = _deliberatelyDisconnected

    fun setConnectionStatus(connected: Boolean) {
        if (connected) {
            _connected.value = ConnectionState.CONNECTED
        } else {
            _connected.value = ConnectionState.DISCONNECTED
            _sensorConnected.value = ConnectionState.UNKNOWN
        }
    }

    fun setDeliberatelyDisconnected(connected: Boolean) {
        _deliberatelyDisconnected.value = connected
    }

    fun handleMessage(message : String) {
        _lastMessage.value = message
        val formatter = SimpleDateFormat.getTimeInstance()
        val date = Date()
        _lastMessageReceived.value = formatter.format(date)
    }

    fun handleData() {
        handleMessage("Dog")
        _sensorConnected.value = ConnectionState.DATA_PRESENT
    }

    fun handlePing() {
        handleMessage("Ping")
        _sensorConnected.value = ConnectionState.CONNECTED
    }

    fun handleSensorDisconnect() {
        _sensorConnected.value = ConnectionState.DISCONNECTED
    }

    fun testMode() : Boolean {
        return false
    }
}
