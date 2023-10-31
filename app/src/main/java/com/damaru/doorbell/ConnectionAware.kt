package com.damaru.doorbell

interface ConnectionAware {
    fun handleData()
    fun handlePing()
    fun handleSensorDisconnect()
    fun setConnectionStatus(status : Boolean)
}