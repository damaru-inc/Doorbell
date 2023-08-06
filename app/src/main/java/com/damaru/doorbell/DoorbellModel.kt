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

    private val mqttAndroidClient: MqttAsyncClient =
        MqttAsyncClient(SOLACE_MQTT_HOST, "Pixel7", MemoryPersistence())

    val connected: State<ConnectionState>
        get() = _connected

    val sensorConnected: State<ConnectionState>
        get() = _sensorConnected

    val lastMessage: State<String>
        get() = _lastMessage

    val lastMessageReceived: State<String>
        get() = _lastMessageReceived

    init {
        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                Log.w(TAG, "connectionLost because " + cause?.message)

                Timer("checkingConnection", false).schedule(3000) {
                    Log.d(TAG, "Checking connection, try #1.")
                    if (mqttAndroidClient.isConnected) {
                        Log.d(TAG, "Reconnected.")
                        subscribe()
                    } else {
                        Log.w(TAG, "connectionLost: still down.")
                        Timer("checkingConnection", false).schedule(3000) {
                            Log.d(TAG, "Checking connection, try #2.")
                            if (mqttAndroidClient.isConnected) {
                                Log.d(TAG, "Reconnected.")
                                subscribe()
                            } else {
                                Log.w(TAG, "connectionLost: still down.")
                                Timer("checkingConnection", false).schedule(3000) {
                                    Log.d(TAG, "Checking connection, try #3.")
                                    if (mqttAndroidClient.isConnected) {
                                        Log.d(TAG, "Reconnected.")
                                        subscribe()
                                    } else {
                                        Log.w(TAG, "connectionLost: still down.")
                                        setConnectionStatus(false)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message.toString()
                Log.d(TAG, "Message received : $topic: $payload")

                if (topic != null && topic.contains("proximity/control")) {
                    if (payload.equals("ping") || payload.equals("connected")) {
                        handlePing()
                    } else {
                        handleMessage("Disconnected")
                        _sensorConnected.value = ConnectionState.DISCONNECTED
                    }
                }

                if (topic.equals("proximity/data")) {
                    handleData()
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                TODO("Not yet implemented")
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d(TAG, "connectComplete at $serverURI")
                setConnectionStatus(true)
            }

        })
    }


    fun connectClient() {

        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = SOLACE_CONNECTION_RECONNECT
        mqttConnectOptions.isCleanSession = SOLACE_CONNECTION_CLEAN_SESSION
        mqttConnectOptions.userName = SOLACE_CLIENT_USER_NAME
        mqttConnectOptions.password = SOLACE_CLIENT_PASSWORD.toCharArray()
        mqttConnectOptions.connectionTimeout = SOLACE_CONNECTION_TIMEOUT
        mqttConnectOptions.keepAliveInterval = SOLACE_CONNECTION_KEEP_ALIVE_INTERVAL

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {

                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = MQTT_DISCONNECT_BUFFER_SIZE
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
                    subscribe()
                    setConnectionStatus(true)

                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to connect: " + exception.toString())
                }
            })
        } catch (ex: MqttException) {
            Log.e(TAG, ex.toString())
            setConnectionStatus(false)
            ex.printStackTrace()
        }
    }

    fun subscribe() {
        mqttAndroidClient.subscribe("proximity/#", 0, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.e(TAG, "Subscribed.")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e(TAG, "Subscription failed.")
            }
        })
    }

    fun setConnectionStatus(connected: Boolean) {
        if (connected) {
            _connected.value = ConnectionState.CONNECTED
        } else {
            _connected.value = ConnectionState.DISCONNECTED
            _sensorConnected.value = ConnectionState.UNKNOWN
        }
    }

    fun toggleConnect(doConnect: Boolean) {

        Log.d(TAG, "toggleConnect: $doConnect $_connected")
        if (doConnect) {
            if (mqttAndroidClient.isConnected) {
                Log.d(TAG, "connectClient: already connected.")
                return;
            }
            connectClient()
        } else {
            if (mqttAndroidClient.isConnected) {
                mqttAndroidClient.disconnect()
            }
            setConnectionStatus(false)
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

    fun destroy() {
        toggleConnect(false)
        Log.d(TAG, "destroy: Closing the client.")
        mqttAndroidClient.close()
    }

    fun testMode() : Boolean {
        return false
    }
}
