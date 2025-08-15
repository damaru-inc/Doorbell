package com.damaru.doorbell

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.damaru.doorbell.ui.theme.DoorbellTheme

class MainActivity : ComponentActivity(), ConnectionAware {

    private lateinit var doorbellModel : DoorbellModel
    private lateinit var messagingService: MessagingService
    private var bound: Boolean = false

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Permission granted.")
            } else {
                Log.d(TAG, "Permission denied.")
            }
        }

    companion object {
        val channelId = "doorbell"
        const val TAG = "DoorbelMainActivity"
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MessagingService.LocalBinder
            messagingService = binder.getService()
            bound = true
            messagingService.setActivity(this@MainActivity)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        createNotificationChannel()
        doorbellModel = ViewModelProvider(this).get(DoorbellModel::class.java)

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

        val intent = Intent(this,MessagingService::class.java)
        intent.putExtra("name","DoorbellIntent")
        startService(intent)
    }

    override fun onStart() {
        super.onStart()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

            // Bind to LocalService.
        Intent(this, MessagingService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        bound = false
    }

    @Override
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        val intent = Intent(this,MessagingService::class.java)
        stopService(intent)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library.
        // The code is the letter O, not a zero.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Setting up the notification channel.")
            //val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val name = "doorbell"
            val descriptionText = "doorbell"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            // We get the service to just play the sound.
//            val soundUri = Uri.parse("android.resource://"
//                    + this.packageName + "/"
//                    + R.raw.dingdong)
//            val audioAttributes: AudioAttributes = AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                .build()
            // channel.setSound(soundUri, audioAttributes)
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Set up the notification channel.")
        } else {
            Log.w(TAG, "Couldn't set up the notification channel.")
        }
    }

    fun toggleConnect(doConnect: Boolean) {

        Log.d(DoorbellModel.TAG, "toggleConnect: $doConnect $doorbellModel.connected")
        if (bound) {
            messagingService.toggleConnect(doConnect)
        }
    }

    override var connectionStatus = false
        set(value) {
            Log.d(DoorbellModel.TAG, "setConnectionStatus: $value")
            doorbellModel.setConnectionStatus(value)
        }
    override var deliberatelyDisconnected = false
        set(value) {
            Log.d(DoorbellModel.TAG, "deliberatelyDisconnected")
            doorbellModel.setDeliberatelyDisconnected(false)
        }

    override fun handleData() {
        doorbellModel.handleData()
    }

    override fun handlePing() {
        doorbellModel.handlePing()
    }

    override fun handleSensorDisconnect() {
        Log.d(DoorbellModel.TAG, "handleSensorDisconnect")
        doorbellModel.handleSensorDisconnect()
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

    val status = when (connectionState) {
        ConnectionState.CONNECTED -> "connected."
        ConnectionState.DATA_PRESENT -> "connected."
        ConnectionState.DISCONNECTED -> "disconnected."
        ConnectionState.UNKNOWN -> "unknown"
    }

    return "Sensor's state is $status"
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