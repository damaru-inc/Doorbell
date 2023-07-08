package com.damaru.doorbell

import android.os.Bundle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.damaru.doorbell.ui.theme.DoorbellTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DoorbellTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
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
fun MainScreen(
    modifier: Modifier = Modifier,
    doorbellModel: DoorbellModel = viewModel()
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusPane(modifier = modifier.weight(1.0f), doorbellModel = doorbellModel)
        ButtonBar(
            connected = doorbellModel.connected.value,
            onConnect = { checked -> doorbellModel.connect(checked) },
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
                .weight(0.1f)
                .fillMaxWidth(1f),
            shape = CardDefaults.shape,
            colors = getCardColors(doorbellModel.connected.value)
        ) {
            Text(
                text = "Connected",
                modifier = modifier.align(Alignment.CenterHorizontally)
            )
        }

        Card(
            modifier = modifier
                .padding(4.dp)
                .weight(0.8f)
                .fillMaxWidth(1f)
                .align(Alignment.CenterHorizontally),
            shape = CardDefaults.shape,
            colors = getCardColors(doorbellModel.sensorConnected.value)
        ) {
            Text(
                text = "Doorbell",
                modifier = modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = doorbellModel.lastMessage.value,
                modifier = modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = doorbellModel.lastMessageReceived.value,
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