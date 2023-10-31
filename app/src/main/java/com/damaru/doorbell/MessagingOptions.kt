package com.damaru.doorbell


// Solace PubSub+ Broker Options

// Fill in your Solace Cloud PubSub+ Broker's 'MQTT Host' and 'Password' options.
// This information can be found under:
// https://console.solace.cloud/services/ -> <your-service> -> 'Connect' -> 'MQTT'
const val SOLACE_CLIENT_USER_NAME = "solace-cloud-client"
const val SOLACE_CLIENT_PASSWORD = "q6mi08kmmvjtidvlcthiee0uqs"
const val SOLACE_MQTT_HOST = "mr-connection-c5749osfr99.messaging.solace.cloud"
const val SOLACE_MQTT_PORT = 1883
const val SOLACE_MQTT_URI = "tcp://$SOLACE_MQTT_HOST:$SOLACE_MQTT_PORT"

// Other options
const val SOLACE_CONNECTION_TIMEOUT = 3
const val SOLACE_CONNECTION_KEEP_ALIVE_INTERVAL = 60
const val SOLACE_CONNECTION_CLEAN_SESSION = true
const val SOLACE_CONNECTION_RECONNECT = true

