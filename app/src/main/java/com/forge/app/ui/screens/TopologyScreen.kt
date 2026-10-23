// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.theme.*

data class EcuNode(
    val name: String,
    val bus: String, // CAN-H/L, LIN, MOST, Ethernet
    val address: String,
    val dtcCount: Int,
    val status: String, // OK, FAULT, OFFLINE
    val pingMs: Int = 12,
    val rxFramesPerSec: Int = 120
)

@Composable
fun TopologyScreen() {
    val nodes = remember {
        listOf(
            EcuNode("ECM - Engine Control Module", "CAN-High (500k)", "0x7E0", 2, "FAULT", pingMs = 8, rxFramesPerSec = 450),
            EcuNode("TCM - Transmission Control Module", "CAN-High (500k)", "0x7E1", 0, "OK", pingMs = 11, rxFramesPerSec = 220),
            EcuNode("ABS / ESP - Brakes & Stability", "CAN-High (500k)", "0x7E2", 0, "OK", pingMs = 6, rxFramesPerSec = 510),
            EcuNode("BCM - Body Control Module", "CAN-Low (125k)", "0x7E3", 0, "OK", pingMs = 24, rxFramesPerSec = 85),
            EcuNode("ADAS - Front Radar & Camera", "CAN-FD (2M)", "0x7E4", 0, "OK", pingMs = 4, rxFramesPerSec = 980),
            EcuNode("SRS - Airbag Safety Unit", "CAN-High (500k)", "0x7E5", 0, "OK", pingMs = 14, rxFramesPerSec = 110),
            EcuNode("Gateway - Central Gateway", "Ethernet / DoIP", "0x700", 0, "OK", pingMs = 2, rxFramesPerSec = 1600)
        )
    }

    var isScanningNetwork by remember { mutableStateOf(false) }
    var selectedNode by remember { mutableStateOf<EcuNode?>(null) }

    // Packet Burst Flow Animation
    val infiniteTransition = rememberInfiniteTransition(label = "bus_topology_anim")
    val packetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "packet_progress"
    )

    val warningPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fault_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Bus Header Card
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountTree, contentDescription = null, tint = ForgeCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AUTOMOTIVE NETWORK TOPOLOGY (CAN-FD / LIN)",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan
                        )
                    }

                    Button(
                        onClick = { isScanningNetwork = !isScanningNetwork },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanningNetwork) ForgeAmber else ForgeGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isScanningNetwork) Icons.Default.Refresh else Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isScanningNetwork) "SCANNING" else "PING BUS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Active Gateways: 1 • Online Modules: 7/7 • Bus Load: 34% (Healthy)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeOnSurface
                )
            }
        }

        // Live CAN Bus Transmission Canvas Diagram with flowing glowing data packets
        Surface(
            color = Color(0xFF0C0F17),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Main Backbone Highway Line
                    val busY = h / 2f
                    drawLine(
                        color = Color(0xFF222B3D),
                        start = Offset(20.dp.toPx(), busY),
                        end = Offset(w - 20.dp.toPx(), busY),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = ForgeCyan.copy(alpha = 0.5f),
                        start = Offset(20.dp.toPx(), busY),
                        end = Offset(w - 20.dp.toPx(), busY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Draw Node Tap Drop Lines
                    val nodeCount = nodes.size
                    val spacing = (w - 40.dp.toPx()) / (nodeCount - 1)

                    nodes.forEachIndexed { index, node ->
                        val nodeX = 20.dp.toPx() + (index * spacing)
                        val isFault = node.status == "FAULT"
                        val isTop = index % 2 == 0
                        val nodeY = if (isTop) busY - 30.dp.toPx() else busY + 30.dp.toPx()

                        // Drop Wire
                        drawLine(
                            color = if (isFault) ForgeRed else ForgeCyan,
                            start = Offset(nodeX, busY),
                            end = Offset(nodeX, nodeY),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Node Circle
                        drawCircle(
                            color = if (isFault) ForgeRed.copy(alpha = warningPulseAlpha) else ForgeCyan,
                            radius = 6.dp.toPx(),
                            center = Offset(nodeX, nodeY)
                        )
                    }

                    // Animated Flowing CAN Data Packets along the backbone
                    val packetCount = 4
                    for (k in 0 until packetCount) {
                        val frac = (packetProgress + (k.toFloat() / packetCount)) % 1f
                        val packetX = 20.dp.toPx() + (frac * (w - 40.dp.toPx()))

                        // Glowing packet particle
                        drawCircle(
                            color = ForgeAmber.copy(alpha = 0.3f),
                            radius = 9.dp.toPx(),
                            center = Offset(packetX, busY)
                        )
                        drawCircle(
                            color = ForgeAmber,
                            radius = 4.dp.toPx(),
                            center = Offset(packetX, busY)
                        )
                    }
                }

                // Bus Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("120Ω TERM (HIGH)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan)
                    Text("CAN HIGH-SPEED 500 KBPS BACKBONE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ForgeAmber)
                    Text("120Ω TERM (LOW)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan)
                }
            }
        }

        // ECU Modules List with Live RX/TX & Ping Telemetry
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(nodes) { node ->
                val isFault = node.status == "FAULT"

                Surface(
                    color = ForgeSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isFault) ForgeRed.copy(alpha = warningPulseAlpha) else ForgeBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedNode = node }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isFault) ForgeRed.copy(alpha = warningPulseAlpha) else ForgeGreen)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = node.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeOnSurface
                                )
                                Text(
                                    text = "${node.bus} • Addr: ${node.address} • Ping: ${node.pingMs}ms",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = if (isFault) ForgeRed.copy(alpha = 0.2f) else ForgeGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isFault) "${node.dtcCount} DTC FAULT" else "ONLINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isFault) ForgeRed else ForgeGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${node.rxFramesPerSec} f/s",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeCyan
                            )
                        }
                    }
                }
            }
        }
    }
}
