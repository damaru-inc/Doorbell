package com.damaru.doorbell

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Timer
import kotlin.concurrent.schedule

enum class ConnectionState {
    CONNECTED, DATA_PRESENT, DISCONNECTED, UNKNOWN
}

private const val MQTT_DISCONNECT_BUFFER_SIZE = 100

class DoorbellModel : ViewModel() {

    companion object {
        const val TAG = "Doorbell"
    }

    private val _connected = mutableStateOf<ConnectionState>(ConnectionState.DISCONNECTED)
    private val _sensorConnected = mutableStateOf<ConnectionState>(ConnectionState.UNKNOWN)
    private val _lastMessage = mutableStateOf<String>("")
    private val _lastMessageReceived = mutableStateOf<String>("")

    private var bellCallback : () -> Unit = {}

    val connected: State<ConnectionState>
        get() = _connected

    val sensorConnected: State<ConnectionState>
        get() = _sensorConnected

    val lastMessage: State<String>
        get() = _lastMessage

    val lastMessageReceived: State<String>
        get() = _lastMessageReceived


    fun setConnectionStatus(connected: Boolean) {
        if (connected) {
            _connected.value = ConnectionState.CONNECTED
        } else {
            _connected.value = ConnectionState.DISCONNECTED
            _sensorConnected.value = ConnectionState.UNKNOWN
        }
    }

    fun handleMessage(message : String) {
        _lastMessage.value = message
        //val formatter = SimpleDateFormat("hh:mm:ss")
        val formatter = SimpleDateFormat.getTimeInstance()
        val date = Date()
        _lastMessageReceived.value = formatter.format(date)
    }

    fun handleData() {
        handleMessage("Dog")
        _sensorConnected.value = ConnectionState.DATA_PRESENT
        bellCallback()
    }

    fun handlePing() {
        handleMessage("Ping")
        _sensorConnected.value = ConnectionState.CONNECTED
    }

    fun handleSensorDisconnect() {
        _sensorConnected.value = ConnectionState.DISCONNECTED
    }

    fun setBellCallback(callback : () -> Unit) {
        bellCallback = callback;
    }
    fun testMode() : Boolean {
        return false
    }
}
