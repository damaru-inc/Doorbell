package com.damaru.doorbell

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MessagingService : Service(), ConnectionAware {

    private lateinit var mqttClientHelper : MqttClientHelper
    private var activity : ConnectionAware? = null
    private val binder = LocalBinder()

    companion object {
        const val TAG = "MessagingService"
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods.
        fun getService(): MessagingService = this@MessagingService
    }

    override fun onBind(intent: Intent?): IBinder? {
        log("onBind: $intent")
        mqttClientHelper.connectFromInit()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        log("onUnbind: $intent")
        activity = null;
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        mqttClientHelper = MqttClientHelper(this)
        log("onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name=intent?.getStringExtra("name")
        Toast.makeText(
            applicationContext, "Service has started running in the background",
            Toast.LENGTH_SHORT
        ).show()
        if (name != null) {
            log("service name: $name")
        }
        log("onStartCommand")
        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        log("stopService")
        return super.stopService(name)
    }

    override fun onDestroy() {
        Toast.makeText(
            applicationContext, "Service execution completed",
            Toast.LENGTH_SHORT
        ).show()
        log("onDestroy")
        mqttClientHelper.destroy()
        super.onDestroy()
    }

    fun log(str:String){
        Log.d(TAG, "$str")
    }

    fun setActivity(connectionAware: ConnectionAware) {
        activity = connectionAware
    }

    //@SuppressLint("MissingPermission")
    private fun doNotification() {
        val builder = NotificationCompat.Builder(applicationContext, MainActivity.channelId)
            .setSmallIcon(R.mipmap.ic_doorbell)
            .setContentTitle("Dog!")
            .setContentText("Woof!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "No permission to notify.")
                return
            }
            val areNotificationsEnabled = areNotificationsEnabled()
            log("Doing notify. areNotificationsEnabled: $areNotificationsEnabled")
            notify(MainActivity.notificationId, builder.build())
        }
    }

    override var connectionStatus = false
         set(value) {
             field = value
             activity?.connectionStatus = value
         }
    override var deliberatelyDisconnected = false
        set(value) {
            field = value
            activity?.deliberatelyDisconnected = value
        }

    override fun handleData() {
        activity?.handleData()
        doNotification()
    }

    override fun handlePing() {
        activity?.handlePing()
    }

    override fun handleSensorDisconnect() {
        activity?.handleSensorDisconnect()
    }

    fun toggleConnect(doConnect: Boolean) {

        Log.d(DoorbellModel.TAG, "toggleConnect: $doConnect")
        if (doConnect) {
            if (mqttClientHelper.isConnected()) {
                Log.d(DoorbellModel.TAG, "connectClient: already connected.")
                return
            }
            mqttClientHelper.connect()
        } else {
            if (mqttClientHelper.isConnected()) {
                mqttClientHelper.disconnect()
            }
        }
    }
}