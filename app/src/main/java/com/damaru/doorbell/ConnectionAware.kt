package com.damaru.doorbell

interface ConnectionAware {
    var connectionStatus : Boolean
    var deliberatelyDisconnected : Boolean
    fun handleData()
    fun handlePing()
    fun handleSensorDisconnect()
}