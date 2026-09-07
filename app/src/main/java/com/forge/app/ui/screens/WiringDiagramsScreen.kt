// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.forge.app.services.AlldataClient
import com.forge.app.services.AlldataTsb
import com.forge.app.services.AlldataWiringDiagram
import com.forge.app.ui.theme.*

@Composable
fun WiringDiagramsScreen() {
    var selectedCircuit by remember { mutableStateOf("Engine Control Module (ECM/PCM)") }
    var isLoading by remember { mutableStateOf(false) }

    var diagrams by remember { mutableStateOf<List<AlldataWiringDiagram>>(emptyList()) }
    var tsbs by remember { mutableStateOf<List<AlldataTsb>>(emptyList()) }

    LaunchedEffect(selectedCircuit) {
        isLoading = true
        diagrams = AlldataClient.fetchWiringDiagrams(system = selectedCircuit)
        tsbs = AlldataClient.fetchFactoryTsbs()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Cable, contentDescription = null, tint = ForgeCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ALLDATA OEM ELECTRICAL SCHEMATICS",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ForgeAmber, strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = {
                                isLoading = true
                                // Trigger live refresh
                                diagrams = emptyList()
                                tsbs = emptyList()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Refresh ALLDATA", tint = ForgeCyan)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "System: $selectedCircuit (Connected to ALLDATA OEM Database)",
                    fontSize = 11.sp,
                    color = ForgeAmber,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live Diagrams & Pinouts
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "OEM FACTORY PINOUT SPECIFICATIONS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeAmber,
                    fontWeight = FontWeight.Bold
                )
            }

            diagrams.forEach { diagram ->
                item {
                    Surface(
                        color = ForgeSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(diagram.systemName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForgeOnSurface)
                                Text(diagram.oemRefCode, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan)
                            }

                            HorizontalDivider(color = ForgeBorder)

                            diagram.pinoutDetails.forEach { (pin, description) ->
                                PinoutRow(pin = pin, function = description)
                            }

                            if (diagram.diagramImageUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                AsyncImage(
                                    model = diagram.diagramImageUrl,
                                    contentDescription = "OEM Wiring Schematic",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .border(1.dp, ForgeCyan, RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ALLDATA FACTORY SERVICE BULLETINS (TSBs)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(tsbs) { tsb ->
                Surface(
                    color = ForgeSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tsb.tsbNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForgeCyan, fontFamily = FontFamily.Monospace)
                            Text(tsb.issueDate, fontSize = 10.sp, color = ForgeOnSurfaceVariant)
                        }
                        Text(tsb.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForgeOnSurface)
                        Text(tsb.summary, fontSize = 11.sp, color = ForgeOnSurfaceVariant)
                        Surface(
                            color = ForgeSurfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Correction Procedure: ${tsb.oemCorrectionProcedure}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeAmber,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinoutRow(pin: String, function: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ForgeBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = pin, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ForgeCyan)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = function, fontSize = 12.sp, color = ForgeOnSurface, modifier = Modifier.weight(1f))
    }
}
