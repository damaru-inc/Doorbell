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


class MqttClientHelper(doorbellModel: DoorbellModel) {

    companion object {
        const val TAG = "MqttClientHelper"
    }

    var doorbellModel = doorbellModel
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
                Log.w(TAG, "Connected to ${context.clientConfig.serverHost}")
                connected()
                doorbellModel.setConnectionStatus(true);
            }
        }

        val disConnectedListener = object : MqttClientDisconnectedListener {
            override fun onDisconnected(context: MqttClientDisconnectedContext) {
                Log.w(TAG, "Disconnected from ${context.clientConfig.serverHost}")
                doorbellModel.setConnectionStatus(false);
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
        Log.d(DoorbellModel.TAG, "Message received : $topic: $payload")

        if (topic.contains("proximity/control")) {
            if (payload.equals("ping") || payload.equals("connected")) {
                doorbellModel.handlePing()
            } else {
                doorbellModel.handleSensorDisconnect()
            }
        }

        if (topic.equals("proximity/data")) {
            doorbellModel.handleData()
        }
    }

    fun connectFromInit() {
        if (!doorbellModel.deliberatelyDisconnected.value) {
            mqttClient.connect()
        }
    }
    fun connect() {
        mqttClient.connect()
        doorbellModel.setDeliberatelyDisconnected(false)
    }

    fun disconnect() {
        mqttClient.disconnect()
        clientIsConnected = false;
        doorbellModel.setDeliberatelyDisconnected(true)
    }

    fun connected() {
        mqttClient.subscribeWith()
            .topicFilter("proximity/#")
            .callback(::messageCallback)
            .send()
        clientIsConnected = true;
    }

    fun isConnected() : Boolean {
        return clientIsConnected
    }

    fun destroy() {
        mqttClient.disconnect()
    }
}
