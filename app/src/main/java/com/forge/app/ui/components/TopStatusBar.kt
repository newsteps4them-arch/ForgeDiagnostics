package com.forge.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.ObdTelemetryData
import com.forge.app.ui.theme.*

@Composable
fun TopStatusBar(
    telemetry: ObdTelemetryData,
    activeVehicleName: String = "2021 Audi S5 Sportback",
    onToggleConnection: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    Surface(
        color = ForgeSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = ForgeBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = ForgeAmber
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TEAM FORGE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = ForgeAmber
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ForgeAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OS v3.2",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ForgeAmber
                                )
                            }
                        }
                        Text(
                            text = activeVehicleName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // OBD Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (telemetry.isConnected) ForgeGreen.copy(alpha = 0.15f) else ForgeRed.copy(alpha = 0.15f))
                            .border(
                                width = 1.dp,
                                color = if (telemetry.isConnected) ForgeGreen else ForgeRed,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onToggleConnection() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (telemetry.isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                                contentDescription = "OBD Link",
                                tint = if (telemetry.isConnected) ForgeGreen else ForgeRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (telemetry.isConnected) "${telemetry.batteryVoltage}V" else "OFFLINE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.isConnected) ForgeGreen else ForgeRed
                            )
                        }
                    }

                    // Gemini AI Assistant FAB
                    IconButton(
                        onClick = onOpenAiChat,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ForgeCyan.copy(alpha = 0.2f))
                            .border(1.dp, ForgeCyan, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = ForgeCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
