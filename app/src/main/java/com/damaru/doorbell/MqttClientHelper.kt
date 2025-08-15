package com.damaru.doorbell

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish


class MqttClientHelper(connectionAware: ConnectionAware) {

    companion object {
        const val TAG = "MqttClientHelper"
    }

    var connectionAware = connectionAware
    var mqttClient: Mqtt5AsyncClient
    val serverUri = MQTT_URI
    private val clientId: String = "Pixel7"
    var clientIsConnected = false

    init {
        val auth = Mqtt5SimpleAuth.builder()
            .username(MQTT_USER_NAME)
            .password(MQTT_PASSWORD.toByteArray())
            .build();

        val connectedListener = object : MqttClientConnectedListener {
            override fun onConnected(context: MqttClientConnectedContext) {
                Log.i(TAG, "Connected to ${context.clientConfig.serverHost}")
                connected()
                connectionAware.connectionStatus = true;
            }
        }

        val disConnectedListener = object : MqttClientDisconnectedListener {
            override fun onDisconnected(context: MqttClientDisconnectedContext) {
                Log.i(TAG, "Disconnected from ${context.clientConfig.serverHost}")
                connectionAware.connectionStatus = false;
            }
        }

        mqttClient = MqttClient.builder()
            .identifier(clientId)
            .serverHost(MQTT_HOST)
            .serverPort(MQTT_PORT)
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener(connectedListener)
            .addDisconnectedListener(disConnectedListener)
            .useMqttVersion5()
            .simpleAuth(auth)
            .buildAsync()

    }

    fun messageCallback(publish : Mqtt5Publish) {
        val payload = String(publish.payloadAsBytes)
        val topic = publish.topic.toString()
        Log.d(TAG, "Message received : $topic: $payload")

        if (topic.contains("proximity/control")) {
            if (payload.equals("ping") || payload.equals("connected")) {
                connectionAware.handlePing()
            } else {
                connectionAware.handleSensorDisconnect()
            }
        }

        if (topic.equals("proximity/data")) {
            connectionAware.handleData()
        }
    }

    fun connectFromInit() {
        Log.i(TAG, "connectFromInit: clientIsConnected: $clientIsConnected deliberatelyDisconnected: ${connectionAware.deliberatelyDisconnected}")
        if (!connectionAware.deliberatelyDisconnected && !clientIsConnected) {
            mqttClient.connect()
        }
    }
    fun connect() {
        Log.i(TAG, "connect")
        mqttClient.connect()
        connectionAware.deliberatelyDisconnected = false
    }

    fun disconnect() {
        Log.i(TAG, "connect clientIsConnected: $clientIsConnected")
        if (clientIsConnected) {
            mqttClient.disconnect()
            clientIsConnected = false;
            connectionAware.deliberatelyDisconnected = true
        }
    }

    fun connected() {
        Log.i(TAG, "connected clientIsConnected: $clientIsConnected")
        mqttClient.subscribeWith()
            .topicFilter("proximity/#")
            .callback(::messageCallback)
            .send()
        clientIsConnected = true;
        Log.i(TAG, "connected clientIsConnected: $clientIsConnected")
    }

    fun isConnected() : Boolean {
        return clientIsConnected
    }

    fun destroy() {
        mqttClient.disconnect()
    }
}
