// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.AutoTriagePipelineService
import com.forge.app.services.AutoTriageReport
import com.forge.app.services.TriageStepStatus
import com.forge.app.ui.theme.*

@Composable
fun AutoTriagePipelineDialog(
    triageService: AutoTriagePipelineService,
    onDismiss: () -> Unit,
    onNavigateToEstimator: () -> Unit = {},
    onNavigateToParts: () -> Unit = {}
) {
    val report by triageService.triageState.collectAsState()

    AlertDialog(
        onDismissRequest = { if (!report.isRunning) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ForgeCyan.copy(alpha = 0.2f))
                            .border(1.dp, ForgeCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AUTONOMOUS TRIAGE PIPELINE",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan
                        )
                        Text(
                            text = "1-Click Auto-Triage & Multi-Service Dispatcher",
                            fontSize = 10.sp,
                            color = ForgeOnSurfaceVariant
                        )
                    }
                }

                if (!report.isRunning) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ForgeOnSurfaceVariant)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Vehicle & Fault Status Header
                Surface(
                    color = ForgeBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = report.vehicleName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForgeOnSurface
                            )
                            Text(
                                text = "VIN: ${report.activeVehicleVin}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeAmber
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ForgeRed.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${report.detectedDtcs.size} ACTIVE FAULTS",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeRed
                            )
                        }
                    }
                }

                // Linear Progress Bar
                if (report.isRunning) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { report.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ForgeCyan,
                            trackColor = ForgeSurfaceVariant
                        )
                        Text(
                            text = "Orchestrating multi-service pipeline: ${(report.progress * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeCyan
                        )
                    }
                }

                // Pipeline Step Cards
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(report.steps) { step ->
                        Surface(
                            color = ForgeSurfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = when (step.status) {
                                    TriageStepStatus.COMPLETED -> ForgeGreen
                                    TriageStepStatus.RUNNING -> ForgeCyan
                                    TriageStepStatus.FAILED -> ForgeRed
                                    TriageStepStatus.PENDING -> ForgeBorder
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                when (step.status) {
                                    TriageStepStatus.COMPLETED -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForgeGreen, modifier = Modifier.size(16.dp))
                                    TriageStepStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ForgeCyan, strokeWidth = 2.dp)
                                    TriageStepStatus.FAILED -> Icon(Icons.Default.Error, contentDescription = null, tint = ForgeRed, modifier = Modifier.size(16.dp))
                                    TriageStepStatus.PENDING -> Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = ForgeBorder, modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = step.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ForgeOnSurface
                                        )
                                        if (step.latencyMs > 0) {
                                            Text(
                                                text = "${step.latencyMs} ms",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = ForgeAmber
                                            )
                                        }
                                    }

                                    Text(
                                        text = step.resultSummary ?: step.description,
                                        fontSize = 10.sp,
                                        color = if (step.resultSummary != null) ForgeOnSurface else ForgeOnSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary & Quote Box when finished
                if (!report.isRunning && report.progress >= 1.0f) {
                    Surface(
                        color = ForgeGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "WORK ORDER READY FOR SIGN-OFF",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeGreen
                                )
                                Text(
                                    text = "$${"%.2f".format(report.totalEstimatedCost)}",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeAmber
                                )
                            }
                            Text(
                                text = "Matched ${report.matchedTsbs.size} TSBs | Sourced ${report.sourcedParts.size} parts | Recalls: ${report.safetyRecalls.size}",
                                fontSize = 10.sp,
                                color = ForgeOnSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!report.isRunning) {
                Button(
                    onClick = {
                        triageService.runAutoTriage(
                            vin = "WAUZZZF58MA019284",
                            dtcCodes = listOf("P0300", "P0171")
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("run_auto_triage_btn")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (report.progress >= 1.0f) "Re-Run Auto-Triage" else "Start Autonomous Triage", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!report.isRunning) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = ForgeOnSurfaceVariant)
                }
            }
        },
        containerColor = ForgeSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
