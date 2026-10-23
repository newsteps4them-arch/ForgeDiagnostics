package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.forge.app.BuildConfig
import com.forge.app.services.*
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentConnectionType: String = "SIMULATED",
    authAndSyncService: AuthAndSyncService? = null,
    usbHardwareService: UsbHardwareCommunicationService? = null,
    cloudConnectorsManager: CloudConnectorsManager? = null,
    onConnectionTypeChange: (String) -> Unit,
    onResetDatabase: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val connectorsManager = remember { cloudConnectorsManager ?: CloudConnectorsManager() }
    val hubState by connectorsManager.hubState.collectAsState()

    var apiKeyInput by remember { mutableStateOf(if (BuildConfig.GEMINI_API_KEY.contains("PLACEHOLDER")) "" else BuildConfig.GEMINI_API_KEY) }
    var alldataKeyInput by remember { mutableStateOf(BuildConfig.ALLDATA_API_KEY) }
    var nexpartKeyInput by remember { mutableStateOf(BuildConfig.NEXPART_API_KEY) }
    var openAiKeyInput by remember { mutableStateOf(BuildConfig.OPENAI_API_KEY) }

    // NHTSA Live Recall Lookup Demo state
    var vinQueryInput by remember { mutableStateOf("WAUZZZF58MA019284") }
    var nhtsaDecodedSpecs by remember { mutableStateOf<DecodedVehicleSpecs?>(null) }
    var nhtsaRecallsList by remember { mutableStateOf<List<NhtsaRecallItem>>(emptyList()) }
    var isNhtsaLoading by remember { mutableStateOf(false) }

    val userProfile by (authAndSyncService?.currentUser?.collectAsState() ?: remember { mutableStateOf(UserProfile()) })
    val syncStatus by (authAndSyncService?.syncStatus?.collectAsState() ?: remember { mutableStateOf(SyncStatus()) })
    val usbState by (usbHardwareService?.hardwareState?.collectAsState() ?: remember { mutableStateOf(UsbHardwareState()) })

    val julesService = remember { JulesAgentService() }
    var isJulesDialogOpen by remember { mutableStateOf(false) }
    var julesKeyInput by remember { mutableStateOf(if (BuildConfig.GEMINI_API_KEY.contains("PLACEHOLDER")) "" else BuildConfig.GEMINI_API_KEY) }

    if (isJulesDialogOpen) {
        com.forge.app.ui.components.JulesAgentDialog(
            julesService = julesService,
            onDismiss = { isJulesDialogOpen = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Hub, contentDescription = null, tint = ForgeAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TEAM FORGE CONNECTORS & INTEGRATIONS HUB",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeAmber
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ForgeGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${hubState.healthyCount}/${hubState.totalActiveConnectors} ACTIVE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "High-performance connector pipeline: Google Cloud Project APIs, Firebase Cloud Firestore, US DOT NHTSA Safety Recalls, ALLDATA OEM, Nexpart B2B, OpenAI Compute, and USB/Bluetooth hardware bridges.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Cloud Connectors Health Check & Ping Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = ForgeCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLOUD CONNECTORS & API PIPELINE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { connectorsManager.runFullSystemHealthCheck() },
                        enabled = !hubState.isRunningFullHealthCheck,
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("ping_all_connectors_btn")
                    ) {
                        if (hubState.isRunningFullHealthCheck) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ping & Test All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Grid of Connectors
                hubState.connectors.forEach { connector ->
                    Surface(
                        color = ForgeSurfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, if (connector.status == ConnectorStatus.CONNECTED_HEALTHY) ForgeBorder else ForgeAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (connector.status) {
                                                    ConnectorStatus.CONNECTED_HEALTHY -> ForgeGreen
                                                    ConnectorStatus.TESTING_PING -> ForgeAmber
                                                    else -> ForgeCyan
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = connector.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeOnSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ForgeBackground)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${connector.latencyMs} ms",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = ForgeAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = connector.details,
                                fontSize = 10.sp,
                                color = ForgeOnSurfaceVariant,
                                lineHeight = 14.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Provider: ${connector.provider}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ForgeCyan
                                )
                                Text(
                                    text = connector.category,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ForgeOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live NHTSA Safety Recalls & VPIC VIN Query Connector Card
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ForgeAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "US DOT / NHTSA SAFETY RECALLS API",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = ForgeGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LIVE GOVERNMENT API",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Query real-time safety recalls, campaign numbers, and VIN engineering specifications directly from the National Highway Traffic Safety Administration:",
                    fontSize = 11.sp,
                    color = ForgeOnSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = vinQueryInput,
                        onValueChange = { vinQueryInput = it.uppercase() },
                        label = { Text("Vehicle Identification Number (VIN)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForgeAmber,
                            unfocusedBorderColor = ForgeBorder
                        )
                    )

                    Button(
                        onClick = {
                            if (vinQueryInput.isNotBlank()) {
                                isNhtsaLoading = true
                                coroutineScope.launch {
                                    nhtsaDecodedSpecs = NhtsaSafetyClient.decodeVinLive(vinQueryInput)
                                    nhtsaRecallsList = NhtsaSafetyClient.fetchSafetyRecalls(vinQueryInput)
                                    isNhtsaLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("query_nhtsa_btn")
                    ) {
                        if (isNhtsaLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("Query API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                nhtsaDecodedSpecs?.let { specs ->
                    Surface(
                        color = ForgeBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("DECODED VEHICLE SPECIFICATIONS (VPIC API):", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan, fontWeight = FontWeight.Bold)
                            Text("${specs.modelYear} ${specs.make} ${specs.model} — ${specs.engineCylinders} Cyl ${specs.displacementL} (${specs.driveType})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForgeOnSurface)
                            Text("Plant: ${specs.plantCountry} | Transmission: ${specs.transmissionStyle} | Fuel: ${specs.fuelTypePrimary}", fontSize = 10.sp, color = ForgeOnSurfaceVariant)
                        }
                    }
                }

                if (nhtsaRecallsList.isNotEmpty()) {
                    Text("ACTIVE SAFETY RECALL CAMPAIGNS (${nhtsaRecallsList.size}):", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeRed, fontWeight = FontWeight.Bold)
                    nhtsaRecallsList.forEach { recall ->
                        Surface(
                            color = ForgeRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("NHTSA CAMPAIGN #${recall.nhtsaCampaignNumber}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ForgeRed)
                                    Text(recall.component, fontSize = 9.sp, color = ForgeOnSurfaceVariant, maxLines = 1)
                                }
                                Text(recall.summary, fontSize = 11.sp, color = ForgeOnSurface)
                                Text("Remedy: ${recall.remedy}", fontSize = 10.sp, color = ForgeAmber)
                            }
                        }
                    }
                }
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
                        Text(userProfile.email.ifEmpty { "newsteps4them@gmail.com" }, fontSize = 12.sp, color = ForgeOnSurfaceVariant)
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

        // Google Jules Autonomous Coding Agent Card
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = ForgeCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GOOGLE JULES REST API AGENT",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = ForgeCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "v1alpha",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Automate software engineering workflows, generate Pull Requests (AUTO_CREATE_PR), review telemetry patches, and conduct multi-turn agent sessions directly with Google Jules API.",
                    fontSize = 11.sp,
                    color = ForgeOnSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isJulesDialogOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Jules Console", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                julesService.listSources()
                                isJulesDialogOpen = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Source, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sources", fontSize = 11.sp)
                    }
                }
            }
        }

        // Enterprise Automotive API Integration Credentials
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ENTERPRISE API CONNECTORS & SECRETS CONFIG", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan, fontWeight = FontWeight.Bold)
                Text("Configure live production API authorization keys for Google Cloud, Jules REST API, OEM manuals, parts distributors, and AI engines:", fontSize = 11.sp, color = ForgeOnSurfaceVariant)

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { 
                        apiKeyInput = it 
                        julesService.setApiKey(it)
                    },
                    label = { Text("Google Cloud / Gemini API Key (x-goog-api-key)") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = julesKeyInput,
                    onValueChange = { 
                        julesKeyInput = it
                        julesService.setApiKey(it)
                    },
                    label = { Text("Google Jules REST API Key") },
                    placeholder = { Text("ask_...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = alldataKeyInput,
                    onValueChange = { alldataKeyInput = it },
                    label = { Text("ALLDATA OEM Repair & Wiring API Key") },
                    placeholder = { Text("Live ALLDATA Partner Key") },
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
                    label = { Text("OpenAI API Key (GPT-4o Engine)") },
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
