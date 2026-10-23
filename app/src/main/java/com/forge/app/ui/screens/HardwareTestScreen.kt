package com.forge.app.ui.screens

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.forge.app.hardware.UsbConnection
import kotlinx.coroutines.launch

private const val ACTION_USB_PERMISSION = "com.forge.app.USB_PERMISSION"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareTestScreen() {
    val context = LocalContext.current
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val scope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf("Ready to connect.") }
    var terminalOutput by remember { mutableStateOf(listOf<String>()) }
    var usbConnection by remember { mutableStateOf<UsbConnection?>(null) }
    var availableDevices by remember { mutableStateOf(usbManager.deviceList.values.toList()) }

    // Refresh devices list
    LaunchedEffect(Unit) {
        availableDevices = usbManager.deviceList.values.toList()
    }

    val usbReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.apply {
                                statusText = "Permission granted for ${device.deviceName}"
                                usbConnection = UsbConnection(usbManager, device)
                                scope.launch {
                                    val connected = usbConnection?.connect() ?: false
                                    if (connected) {
                                        statusText = "Connected to USB Hardware successfully!"
                                        terminalOutput = terminalOutput + ">>> CONNECTED to ${device.deviceName}"

                                        // Attempt initial reset command
                                        val response = usbConnection?.sendCommand("AT Z")
                                        terminalOutput = terminalOutput + "Sent: AT Z -> Response: $response"
                                    } else {
                                        statusText = "Failed to establish USB connection."
                                    }
                                }
                            }
                        } else {
                            statusText = "Permission denied for device"
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        onDispose {
            context.unregisterReceiver(usbReceiver)
            scope.launch { usbConnection?.disconnect() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hardware Connection Test", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E24),
                    titleContentColor = Color(0xFFE0E0E0)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "1. Plug in your USB-OTG and OBD-II Cable",
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { availableDevices = usbManager.deviceList.values.toList() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
            ) {
                Text("Refresh USB Devices")
            }

            if (availableDevices.isEmpty()) {
                Text("No USB devices detected.", color = Color.Red)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                    items(availableDevices) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                            onClick = {
                                statusText = "Requesting permission for ${device.deviceName}..."
                                val permissionIntent = PendingIntent.getBroadcast(
                                    context, 0, Intent(ACTION_USB_PERMISSION),
                                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                                )
                                usbManager.requestPermission(device, permissionIntent)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Cable, contentDescription = "USB", tint = Color.White)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(device.deviceName, color = Color.White)
                                    Text("Vendor: ${device.vendorId} Product: ${device.productId}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = Color.DarkGray)

            Text("Status: $statusText", color = Color.Yellow)

            Text("Terminal Output", color = Color.White, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                LazyColumn {
                    items(terminalOutput) { line ->
                        Text(line, color = Color.Green, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = {
                        scope.launch {
                            val res = usbConnection?.sendCommand("01 0C") // Request RPM
                            terminalOutput = terminalOutput + "Sent: 01 0C (RPM) -> Resp: $res"
                        }
                    },
                    enabled = usbConnection != null
                ) {
                    Text("Test RPM")
                }

                Button(
                    onClick = {
                        scope.launch {
                            val res = usbConnection?.sendCommand("09 02") // Request VIN
                            terminalOutput = terminalOutput + "Sent: 09 02 (VIN) -> Resp: $res"
                        }
                    },
                    enabled = usbConnection != null
                ) {
                    Text("Test VIN")
                }
            }
        }
    }
}
