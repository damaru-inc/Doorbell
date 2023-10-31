package com.damaru.doorbell

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.damaru.doorbell.ui.theme.DoorbellTheme


class MainActivity : ComponentActivity() {

    val channelId = "doorbell"
    val notificationId = 1

    private lateinit var doorbellModel : DoorbellModel
    private lateinit var mqttClientHelper : MqttClientHelper

    private val mediaPlayer by lazy {
        MediaPlayer.create(this, R.raw.dingdong)
    }

    companion object {
        const val TAG = "DoorbellUI"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        createNotificationChannel()
        doorbellModel = ViewModelProvider(this).get(DoorbellModel::class.java)

        doorbellModel.setBellCallback {
            Log.d(TAG, "Bell!!!")
            mediaPlayer?.start()
            //doNotification(applicationContext)
        }

        mqttClientHelper = MqttClientHelper(doorbellModel)

        setContent {
            DoorbellTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }

        mqttClientHelper.connectFromInit()
        val intent = Intent(this,MessagingService::class.java)
        intent.putExtra("name","The messaging service")
        startService(intent)
    }

    private fun doNotification(context: Context) {
        var builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_doorbell)
            .setContentTitle("Dog!")
            .setContentText("Woof!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "No permission to notify.")
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
            Log.d(TAG, "Doing notify. areNotificationsEnabled: " + areNotificationsEnabled)
            notify(notificationId, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Setting up the notification channel.")
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val name = "doorbell"
            val descriptionText = "doorbell"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            // doorbell is the channel id.
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val audioAttributes: AudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.setSound(soundUri, audioAttributes)
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } else {
            Log.w(TAG, "Couldn't set up the notification channel.")
        }
    }

    @Override
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        //var doorbellModel = ViewModelProvider(this).get(DoorbellModel::class.java)
        mqttClientHelper.destroy()

    }


    fun toggleConnect(doConnect: Boolean) {

        Log.d(DoorbellModel.TAG, "toggleConnect: $doConnect $doorbellModel.connected")
        if (doConnect) {
            if (mqttClientHelper.isConnected()) {
                Log.d(DoorbellModel.TAG, "connectClient: already connected.")
                return;
            }
            mqttClientHelper.connect()
        } else {
            if (mqttClientHelper.isConnected()) {
                    mqttClientHelper.disconnect()
            }
//            doorbellModel.setConnectionStatus(false) // redundant?
        }
    }
}

@Composable
fun getCardColors(connectionState: ConnectionState): CardColors {

    return when (connectionState) {
        ConnectionState.CONNECTED -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary)
        ConnectionState.DATA_PRESENT -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary)
        ConnectionState.DISCONNECTED -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary)
        ConnectionState.UNKNOWN -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun getConnectionDescription(connectionState: ConnectionState): String {

    var status = when (connectionState) {
        ConnectionState.CONNECTED -> "connected."
        ConnectionState.DATA_PRESENT -> "connected."
        ConnectionState.DISCONNECTED -> "disconnected."
        ConnectionState.UNKNOWN -> "unknown"
    }

    return "Sensor's state is " + status
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    doorbellModel: DoorbellModel = viewModel()
) {
    val mainActivity = LocalContext.current as MainActivity
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusPane(modifier = modifier.weight(1.0f), doorbellModel = doorbellModel)
        ButtonBar(
            connected = doorbellModel.connected.value,
            onConnect = { checked -> mainActivity.toggleConnect(checked) },
            //onConnect = { checked -> doorbellModel.toggleConnect(checked) },
            sendData = { doorbellModel.handleData() },
            sendPing = { doorbellModel.handlePing() },
            sendDisconnect = { doorbellModel.handleSensorDisconnect() },
            testMode = doorbellModel.testMode(),
            modifier = modifier
        )
    }
}

@Composable
fun StatusPane(
    modifier: Modifier = Modifier,
    doorbellModel: DoorbellModel = viewModel()
) {
    Column (
        modifier = modifier.padding(4.dp)
        //horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = modifier
                .padding(4.dp)
                .fillMaxWidth(1f)
                .align(Alignment.CenterHorizontally),
            shape = CardDefaults.shape,
            colors = getCardColors(doorbellModel.sensorConnected.value)
        ) {
            Text(
                text = "Doggie Doorbell", //doorbellModel.lastMessage.value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 24.dp)
            )
            Text(
                text = doorbellModel.lastMessage.value,
                style = MaterialTheme.typography.titleLarge,
                modifier = modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = doorbellModel.lastMessageReceived.value,
                style = MaterialTheme.typography.titleLarge,
                modifier = modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = getConnectionDescription(doorbellModel.sensorConnected.value),
                style = MaterialTheme.typography.bodyLarge,
                modifier = modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ButtonBar(
    connected: ConnectionState,
    onConnect: (Boolean) -> Unit,
    sendPing: () -> Unit,
    sendData: () -> Unit,
    sendDisconnect: () -> Unit,
    testMode : Boolean,
    modifier: Modifier = Modifier) {
    Column (
        modifier = modifier.fillMaxWidth(1f),
        horizontalAlignment = Alignment.CenterHorizontally
            ){
        Row(
            modifier = modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = "Connect"
            )
            Switch(
                checked = connected == ConnectionState.CONNECTED,
                onCheckedChange = { onConnect(it) }
            )
        }
        if (testMode) {
            Row(
                modifier = modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = { sendPing() }
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Ping"
                    )
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = { sendData() }
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Data"
                    )
                }
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = { sendDisconnect() }
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Disconnect"
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    DoorbellTheme {
        MainScreen()
    }
}