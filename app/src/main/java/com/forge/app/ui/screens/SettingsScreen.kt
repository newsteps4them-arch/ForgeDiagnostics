package com.forge.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.forge.app.services.AuthAndSyncService
import com.forge.app.services.UsbHardwareCommunicationService
import com.forge.app.services.UsbConnectionStatus
import androidx.compose.material.icons.filled.Usb
import com.forge.app.ui.theme.*

@Composable
fun SettingsScreen(
    currentConnectionType: String = "SIMULATED",
    authAndSyncService: AuthAndSyncService? = null,
    usbHardwareService: UsbHardwareCommunicationService? = null,
    onConnectionTypeChange: (String) -> Unit,
    onResetDatabase: () -> Unit
) {
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val userProfile by (authAndSyncService?.currentUser?.collectAsState() ?: remember { mutableStateOf(com.forge.app.services.UserProfile()) })
    val syncStatus by (authAndSyncService?.syncStatus?.collectAsState() ?: remember { mutableStateOf(com.forge.app.services.SyncStatus()) })
    val usbState by (usbHardwareService?.hardwareState?.collectAsState() ?: remember { mutableStateOf(com.forge.app.services.UsbHardwareState()) })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = ForgeAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TEAM FORGE SYSTEM & AUTH SETTINGS",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage Google Authentication, real-time Firestore database sync, OBD hardware, & AI memory context.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Google Auth & User Profile Card
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GOOGLE AUTH & USER PROFILE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan, fontWeight = FontWeight.Bold)
                    Surface(
                        color = if (userProfile.isAuthenticated) ForgeGreen.copy(alpha = 0.2f) else ForgeRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (userProfile.isAuthenticated) "AUTHENTICATED" else "SIGNED OUT",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (userProfile.isAuthenticated) ForgeGreen else ForgeRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (userProfile.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = userProfile.photoUrl,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.dp, ForgeCyan, CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = ForgeCyan,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(userProfile.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForgeOnSurface)
                        Text(userProfile.email.ifEmpty { "no-email@forge.app" }, fontSize = 12.sp, color = ForgeOnSurfaceVariant)
                        Text(userProfile.role, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeAmber)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!userProfile.isAuthenticated) {
                        Button(
                            onClick = { authAndSyncService?.signInWithGoogle() },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign In with Google", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { authAndSyncService?.signOut() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sign Out", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Firestore Database Sync Card
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("REAL-TIME FIRESTORE PERSISTENCE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan, fontWeight = FontWeight.Bold)
                    Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = ForgeGreen, modifier = Modifier.size(20.dp))
                }

                Text(
                    text = "Firestore Database ID:\n`${syncStatus.dbName}`",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeAmber
                )

                Text(
                    text = syncStatus.statusText,
                    fontSize = 11.sp,
                    color = ForgeOnSurface
                )

                Button(
                    onClick = { authAndSyncService?.triggerFirestoreSync() },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceVariant, contentColor = ForgeCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Manual Firestore Sync (${syncStatus.syncedItemsCount} items)", fontSize = 12.sp)
                }
            }
        }

        // Connection Mode Selection
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OBD-II HARDWARE & DIAGNOSTIC APP BRIDGES", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan, fontWeight = FontWeight.Bold)
                    Surface(
                        color = ForgeAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = currentConnectionType,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Select your physical interface cable, wireless dongle, or external mobile diagnostic app bridge:",
                    fontSize = 11.sp,
                    color = ForgeOnSurfaceVariant
                )

                val connectionModes = listOf(
                    Triple("SIMULATED", "Bench Simulator", "Built-in realistic CAN-BUS telemetry noise generator"),
                    Triple("BLUETOOTH", "Bluetooth ELM327", "Direct RFCOMM socket for ELM327, vLinker, Viecar, OBDLink MX+"),
                    Triple("USB_OTG", "USB OTG Cable", "Direct FTDI / CH340 / CP2102 serial USB-OBD2 cable connection"),
                    Triple("OBD_SCANNER_WIFI", "Wi-Fi OBD Scanner", "TCP socket stream (192.168.0.10:35000 / Standalone Scanners)"),
                    Triple("TORQUE_PRO", "Torque Pro App", "Intent broadcast bridge with org.prowl.torque PID logging"),
                    Triple("ALFA_OBD", "AlfaOBD FCA App", "Stellantis / FCA body computer & CAN log bridge (com.AlfaOBD)"),
                    Triple("REPAIR2SOLUTIONS", "Repair2Solutions", "Innova OBD2 scanner report & live telemetry sync")
                )

                connectionModes.forEach { (typeKey, title, desc) ->
                    val isSelected = currentConnectionType == typeKey
                    Surface(
                        color = if (isSelected) ForgeAmber.copy(alpha = 0.15f) else ForgeSurfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) ForgeAmber else ForgeBorder
                        ),
                        onClick = { onConnectionTypeChange(typeKey) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onConnectionTypeChange(typeKey) },
                                colors = RadioButtonDefaults.colors(selectedColor = ForgeAmber, unselectedColor = ForgeOnSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ForgeAmber else ForgeOnSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = 10.sp,
                                    color = ForgeOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // USB Hardware Communication Diagnostic Card
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Usb, contentDescription = null, tint = ForgeGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "USB HARDWARE HOST & SCANNER DRIVER",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = when (usbState.status) {
                            UsbConnectionStatus.CONNECTED -> ForgeGreen.copy(alpha = 0.2f)
                            UsbConnectionStatus.CONNECTING, UsbConnectionStatus.SCANNING -> ForgeAmber.copy(alpha = 0.2f)
                            else -> ForgeSurfaceVariant
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = usbState.status.name,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = when (usbState.status) {
                                UsbConnectionStatus.CONNECTED -> ForgeGreen
                                UsbConnectionStatus.CONNECTING, UsbConnectionStatus.SCANNING -> ForgeAmber
                                else -> ForgeOnSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = usbState.statusMessage,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeOnSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { usbHardwareService?.scanUsbDevices(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceVariant, contentColor = ForgeGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scan USB Bus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (usbState.status == UsbConnectionStatus.CONNECTED) {
                        OutlinedButton(
                            onClick = { usbHardwareService?.disconnect() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Disconnect USB", fontSize = 11.sp)
                        }
                    }
                }

                if (usbState.availableDevices.isNotEmpty()) {
                    Text(
                        "DETECTED USB HARDWARE DEVICES:",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeAmber,
                        fontWeight = FontWeight.Bold
                    )

                    usbState.availableDevices.forEach { devInfo ->
                        Surface(
                            color = ForgeSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = devInfo.chipsetVendor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeOnSurface
                                    )
                                    Text(
                                        text = "VID: 0x${devInfo.vendorId.toString(16).uppercase()} | PID: 0x${devInfo.productId.toString(16).uppercase()} | ${devInfo.deviceName}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ForgeOnSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        val usbMgr = context.getSystemService(android.content.Context.USB_SERVICE) as? android.hardware.usb.UsbManager
                                        val deviceObj = usbMgr?.deviceList?.values?.firstOrNull { it.deviceName == devInfo.deviceName }
                                        if (deviceObj != null) {
                                            usbHardwareService?.requestUsbPermission(context, deviceObj)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (devInfo.hasPermission) "Connect" else "Grant Access", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Protocol: ${usbState.detectedObdProtocol}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan)
                    Text("TX: ${usbState.txByteCount}B | RX: ${usbState.rxByteCount}B", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeAmber)
                }
            }
        }

        // Enterprise Automotive API Integration Credentials
        var alldataKeyInput by remember { mutableStateOf(com.forge.app.BuildConfig.ALLDATA_API_KEY) }
        var nexpartKeyInput by remember { mutableStateOf(com.forge.app.BuildConfig.NEXPART_API_KEY) }
        var openAiKeyInput by remember { mutableStateOf(com.forge.app.BuildConfig.OPENAI_API_KEY) }

        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ENTERPRISE API CONNECTORS & INTEGRATIONS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan, fontWeight = FontWeight.Bold)
                Text("Configure live production API authorization keys for OEM manuals, parts distributors, and AI engines:", fontSize = 11.sp, color = ForgeOnSurfaceVariant)

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Google Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = alldataKeyInput,
                    onValueChange = { alldataKeyInput = it },
                    label = { Text("ALLDATA OEM Repair & Wiring API Key") },
                    placeholder = { Text("Live ALLDATA Partner API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nexpartKeyInput,
                    onValueChange = { nexpartKeyInput = it },
                    label = { Text("Nexpart B2B Parts Catalog API Key") },
                    placeholder = { Text("Live Nexpart Distributor Secret") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = openAiKeyInput,
                    onValueChange = { openAiKeyInput = it },
                    label = { Text("OpenAI API Key (GPT-4o / o3-mini Engine)") },
                    placeholder = { Text("sk-proj-...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Reset Data
        Button(
            onClick = onResetDatabase,
            colors = ButtonDefaults.buttonColors(containerColor = ForgeRed, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("RESET LOCAL DATABASE & CLEAR LOGS", fontWeight = FontWeight.Bold)
        }
    }
}

