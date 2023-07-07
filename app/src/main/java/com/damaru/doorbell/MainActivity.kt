package com.damaru.doorbell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
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
            modifier = modifier
        )
    }
}

@Composable
fun ServiceStatusCard(
    modifier: Modifier = Modifier,
    title: String,
    connectionState: ConnectionState
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxSize(0.8f),
        shape = CardDefaults.shape,
        colors = getCardColors(connectionState)
    ) {
        Text(
            text = title,
            modifier = modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun StatusPane(
    modifier: Modifier = Modifier,
    doorbellModel: DoorbellModel = viewModel()
) {
    Column (
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ServiceStatusCard(
            modifier = modifier,
            title = "Connected",
            connectionState = doorbellModel.connected.value)
        ServiceStatusCard(
            modifier = modifier,
            title = "Doorbell",
            connectionState = doorbellModel.sensorConnected.value)
    }
}

@Composable
fun ButtonBar(
    connected: ConnectionState,
    onConnect: (Boolean) -> Unit,
    modifier: Modifier = Modifier) {
    Row (
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Text(
            modifier = Modifier.padding(8.dp),
            text = "Connect"
        )
        Switch(
            checked = connected == ConnectionState.CONNECTED,
            onCheckedChange = {onConnect(it)}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    DoorbellTheme {
        MainScreen()
    }
}