package com.damaru.doorbell

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Date

enum class ConnectionState {
    CONNECTED, DATA_PRESENT, DISCONNECTED, UNKNOWN
}

class DoorbellModel : ViewModel() {

    private val _connected = mutableStateOf<ConnectionState>(ConnectionState.DISCONNECTED)
    private val _sensorConnected = mutableStateOf<ConnectionState>(ConnectionState.UNKNOWN)
    private val _lastMessage = mutableStateOf<String>("")
    private val _lastMessageReceived = mutableStateOf<String>("")

    val connected: State<ConnectionState>
        get() = _connected

    val sensorConnected: State<ConnectionState>
        get() = _sensorConnected

    val lastMessage: State<String>
        get() = _lastMessage

    val lastMessageReceived: State<String>
        get() = _lastMessageReceived

    fun connect(doConnect: Boolean) {

        if (doConnect) {
            _connected.value = ConnectionState.CONNECTED
        } else {
            _connected.value = ConnectionState.DISCONNECTED
            _sensorConnected.value = ConnectionState.UNKNOWN
        }

        Log.d("me", "connect: $doConnect $_connected")
    }

    fun handleMessage(message : String) {
        _lastMessage.value = message
        //val formatter = SimpleDateFormat("hh:mm:ss")
        val formatter = SimpleDateFormat.getTimeInstance()
        val date = Date()
        _lastMessageReceived.value = formatter.format(date)
        _sensorConnected.value = ConnectionState.CONNECTED
    }

    fun handleData() {
        handleMessage("Data")
    }

    fun handlePing() {
        handleMessage("Ping")
    }

    fun handleSensorDisconnect() {
        _sensorConnected.value = ConnectionState.DISCONNECTED
    }

    fun testMode() : Boolean {
        return true
    }
}
