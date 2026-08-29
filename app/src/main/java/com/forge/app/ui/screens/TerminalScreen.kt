// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.UsbHardwareCommunicationService
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

data class TerminalLine(
    val type: String, // CMD, TX, RX, INFO, ERROR
    val text: String
)

@Composable
fun TerminalScreen(
    usbHardwareService: UsbHardwareCommunicationService? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var cmdInput by remember { mutableStateOf("01 0C") }
    
    val usbTraffic by (usbHardwareService?.trafficLogs?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val usbState by (usbHardwareService?.hardwareState?.collectAsState() ?: remember { mutableStateOf(com.forge.app.services.UsbHardwareState()) })

    val localLogs = remember {
        mutableStateListOf(
            TerminalLine("RX", "ELM327 v2.1 Initialized"),
            TerminalLine("TX", "AT Z"),
            TerminalLine("RX", "ELM327 v2.1 (USB Hardware Link Active)"),
            TerminalLine("TX", "AT SP 6"),
            TerminalLine("RX", "OK (ISO 15765-4 CAN 11/500)"),
            TerminalLine("TX", "01 00"),
            TerminalLine("RX", "41 00 BE 3F A8 13")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = ForgeGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "USB & ELM327 RAW DIAGNOSTIC TERMINAL",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeGreen
                    )
                }
                Text(
                    text = "Status: ${usbState.status.name} • Baud: ${usbState.baudRate} • Protocol: ${usbState.detectedObdProtocol}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Terminal Log View
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (usbTraffic.isNotEmpty()) {
                    items(usbTraffic) { entry ->
                        val color = when (entry.direction) {
                            "TX" -> ForgeAmber
                            "RX" -> ForgeGreen
                            "ERROR" -> ForgeRed
                            else -> ForgeCyan
                        }
                        Text(
                            text = "[${entry.direction}] ${entry.data} ${if (entry.hexDump.isNotBlank()) "(${entry.hexDump})" else ""}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = color
                        )
                    }
                } else {
                    items(localLogs) { line ->
                        val color = when (line.type) {
                            "TX" -> ForgeAmber
                            "RX" -> ForgeGreen
                            else -> ForgeCyan
                        }
                        Text(
                            text = "[${line.type}] ${line.text}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = color
                        )
                    }
                }
            }
        }

        // Quick Command Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (usbHardwareService != null) {
                            usbHardwareService.sendRawCommand("010C")
                        } else {
                            localLogs.add(TerminalLine("TX", "01 0C"))
                            localLogs.add(TerminalLine("RX", "41 0C 0D 80 (864 RPM)"))
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceVariant)
            ) { Text("01 0C (RPM)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) }

            Button(
                onClick = {
                    coroutineScope.launch {
                        if (usbHardwareService != null) {
                            usbHardwareService.sendRawCommand("03")
                        } else {
                            localLogs.add(TerminalLine("TX", "03"))
                            localLogs.add(TerminalLine("RX", "43 02 03 00 01 71"))
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceVariant)
            ) { Text("03 (Get DTCs)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) }

            Button(
                onClick = {
                    coroutineScope.launch {
                        if (usbHardwareService != null) {
                            usbHardwareService.sendRawCommand("AT RV")
                        } else {
                            localLogs.add(TerminalLine("TX", "AT RV"))
                            localLogs.add(TerminalLine("RX", "14.2V"))
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceVariant)
            ) { Text("AT RV (Volts)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
        }

        // Input Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = cmdInput,
                onValueChange = { cmdInput = it.uppercase() },
                placeholder = { Text("Enter OBD PID or AT command...") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForgeGreen,
                    unfocusedBorderColor = ForgeBorder,
                    focusedContainerColor = ForgeSurface,
                    unfocusedContainerColor = ForgeSurface
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val input = cmdInput.trim()
                    if (input.isNotBlank()) {
                        coroutineScope.launch {
                            if (usbHardwareService != null) {
                                usbHardwareService.sendRawCommand(input)
                            } else {
                                localLogs.add(TerminalLine("TX", input))
                                localLogs.add(TerminalLine("RX", "41 ${input.take(2)} OK"))
                            }
                            cmdInput = ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgeGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
