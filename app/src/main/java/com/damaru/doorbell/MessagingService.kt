package com.damaru.doorbell

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat


class MessagingService : Service(), ConnectionAware {

    private lateinit var mqttClientHelper : MqttClientHelper
    private var activity : ConnectionAware? = null
    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer : MediaPlayer? = null
    private var vibrator : Vibrator? = null;

    companion object {
        const val TAG = "MessagingService"
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods.
        fun getService(): MessagingService = this@MessagingService
    }

    override fun onBind(intent: Intent?): IBinder? {
        log("onBind: $intent")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        log("onUnbind: $intent")
        activity = null;
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        log("onCreate")
        super.onCreate()
        mqttClientHelper = MqttClientHelper(this)
        mqttClientHelper.connectFromInit()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand intent: $intent")
        val name=intent?.getStringExtra("name")
        Toast.makeText(
            applicationContext, "Service has started",
            Toast.LENGTH_SHORT
        ).show()
        if (name != null) {
            log("service name: $name")
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.dingdong)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        var notification = buildNotification()
        startForeground(1, notification)

        // research this:
//        wakeLock =
//            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
//                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MessagingService::lock").apply {
//                    acquire()
//                }
//            }

        return START_NOT_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        log("stopService")
        return super.stopService(name)
    }

    override fun onDestroy() {
        log("onDestroy")
        super.onDestroy()
//        Toast.makeText(
//            applicationContext, "Service execution completed",
//            Toast.LENGTH_SHORT
//        ).show()
        mqttClientHelper.destroy()
        mediaPlayer?.release()
    }

    fun log(str:String){
        Log.d(TAG, "$str")
    }

    fun setActivity(connectionAware: ConnectionAware) {
        log("setActivity $connectionAware")
        activity = connectionAware
    }

    //@SuppressLint("MissingPermission")
    /*
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

     */

    private fun buildNotification() : Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, MainActivity.channelId)
            .setSmallIcon(R.mipmap.ic_doorbell)
            .setContentTitle("Dog!")
            .setContentText("Woof!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
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
        log("handleData")
        activity?.handleData()
        mediaPlayer?.start()
        vibrator?.vibrate(VibrationEffect.createOneShot(1000, 255))
        //doNotification()
    }

    override fun handlePing() {
        activity?.handlePing()
    }

    override fun handleSensorDisconnect() {
        log("handleSensorDisconnect")
        activity?.handleSensorDisconnect()
    }

    fun toggleConnect(doConnect: Boolean) {
        log("toggleConnect $doConnect")
        if (doConnect) {
            if (mqttClientHelper.isConnected()) {
                Log.d(DoorbellModel.TAG, "connectClient: already connected.")
                return
            }
            mqttClientHelper.connect()
        } else {
            mqttClientHelper.disconnect()
        }
    }
}