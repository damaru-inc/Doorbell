package com.damaru.doorbell

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

enum class ConnectionState {
    CONNECTED, DATA_PRESENT, DISCONNECTED, UNKNOWN
}

class DoorbellModel : ViewModel() {

    private val _connected = mutableStateOf<ConnectionState>(ConnectionState.DISCONNECTED)
    private val _sensorConnected = mutableStateOf<ConnectionState>(ConnectionState.UNKNOWN)

    val connected: State<ConnectionState>
        get() = _connected

    val sensorConnected: State<ConnectionState>
        get() = _sensorConnected


    fun connect(doConnect: Boolean) {
        _connected.value = if (doConnect) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        Log.d("me", "connect: $doConnect $_connected")
    }
}
