package com.damaru.doorbell

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class MessagingService : Service() {

    companion object {
        const val TAG = "MessagingService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        log("BackgroundTaskService is ready to conquer!")
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
        Log.d("Service Status","Starting Service")
        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        Log.d("Stopping","Stopping Service")
        return super.stopService(name)
    }

    override fun onDestroy() {
        Toast.makeText(
            applicationContext, "Service execution completed",
            Toast.LENGTH_SHORT
        ).show()
        Log.d("Stopped","Service Stopped")
        super.onDestroy()
        log("BackgroundTaskService says goodbye!")
    }

    fun log(str:String){
        Log.d(TAG, "$str")
    }
}