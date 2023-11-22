package com.damaru.doorbell

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MessagingService : Service() {

    companion object {
        const val TAG = "MessagingService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
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
        testNotification()
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
        super.onDestroy()
    }

    fun log(str:String){
        Log.d(TAG, "$str")
    }

    fun testNotification() =
        runBlocking {
            launch {
                delay(2000L)
                Toast.makeText(
                    applicationContext, "After delay",
                    Toast.LENGTH_SHORT
                ).show()
                doNotification()
                log("After delay")

            }
        }

    private fun doNotification() {
        val builder = NotificationCompat.Builder(applicationContext, MainActivity.channelId)
            .setSmallIcon(R.mipmap.ic_doorbell)
            .setContentTitle("Dog!")
            .setContentText("Woof!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(applicationContext)) {
            // notificationId is a unique int for each notification that you must define

            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(MainActivity.TAG, "No permission to notify.")
                //ActivityCompat.requestPermissions(, Manifest.permission.POST_NOTIFICATIONS)
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            val areNotificationsEnabled = areNotificationsEnabled()
            Log.d(MainActivity.TAG, "Doing notify. areNotificationsEnabled: $areNotificationsEnabled")


            notify(MainActivity.notificationId, builder.build())
        }
    }

}