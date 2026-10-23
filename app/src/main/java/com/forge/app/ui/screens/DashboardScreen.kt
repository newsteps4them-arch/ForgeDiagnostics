// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.data.ProjectEntity
import com.forge.app.data.TaskEntity
import com.forge.app.services.AutoTriagePipelineService
import com.forge.app.services.ObdTelemetryData
import com.forge.app.ui.components.AutoTriagePipelineDialog
import com.forge.app.ui.components.DashboardCoachMarkOverlay
import com.forge.app.ui.components.DiagnosticReportDialog
import com.forge.app.ui.components.DtcExplanationDialog
import com.forge.app.ui.components.ObdInstrumentCluster
import com.forge.app.ui.components.ProjectDashboard
import com.forge.app.ui.theme.*

@Composable
fun DashboardScreen(
    telemetry: ObdTelemetryData,
    projects: List<ProjectEntity> = emptyList(),
    tasks: List<TaskEntity>,
    autoTriageService: AutoTriagePipelineService? = null,
    onNavigate: (String) -> Unit,
    onAddTask: (projectId: Long, title: String, description: String, priority: String, category: String) -> Unit = { _, _, _, _, _ -> },
    onUpdateTaskStatus: (task: TaskEntity, newStatus: String) -> Unit = { _, _ -> },
    onDeleteTask: (task: TaskEntity) -> Unit = {},
    onAddProject: (name: String, vehicleVin: String, customerName: String, budget: Double) -> Unit = { _, _, _, _ -> },
    onUpdateProjectStatus: (project: ProjectEntity, newStatus: String) -> Unit = { _, _ -> },
    onClearDtcs: () -> Unit
) {
    var showQuickStartGuide by remember { mutableStateOf(true) }
    var showCoachMarks by remember { mutableStateOf(false) }
    var useRadialClusterView by remember { mutableStateOf(true) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showAutoTriageDialog by remember { mutableStateOf(false) }
    var showDtcExplainerDialog by remember { mutableStateOf(false) }
    var selectedDtcForExplainer by remember { mutableStateOf<String?>(null) }

    val triagePipeline = remember { autoTriageService ?: AutoTriagePipelineService() }
    val triageState by triagePipeline.triageState.collectAsState()

    // Real-Time Anomaly Watchdog (e.g. Fuel Trim surge, Overheating, Low Voltage)
    val hasFuelTrimAnomaly = (telemetry.fuelTrimShortPct + telemetry.fuelTrimLongPct) > 10.0f
    val hasCoolantAnomaly = telemetry.coolantTempC > 95
    val hasBatteryAnomaly = telemetry.batteryVoltage < 12.2f && telemetry.batteryVoltage > 0.0f
    val isAnomalyDetected = hasFuelTrimAnomaly || hasCoolantAnomaly || hasBatteryAnomaly

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First-Time User Onboarding & Quick Start Guide
            if (showQuickStartGuide) {
                item {
                    Surface(
                        color = ForgeSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = "Quick Start",
                                        tint = ForgeCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "FIRST-TIME TECHNICIAN GUIDE",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeCyan
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { showCoachMarks = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HelpOutline,
                                            contentDescription = null,
                                            tint = ForgeCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Start Tour", fontSize = 11.sp, color = ForgeCyan, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(
                                        onClick = { showQuickStartGuide = false },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Got It", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                        Text(
                            text = "Welcome to Team Forge! Here is how to navigate and use your diagnostic workshop suite in 4 simple steps:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OnboardingGuideCard(
                                title = "1. Live Gauges",
                                desc = "View engine RPM, coolant, & battery state.",
                                icon = Icons.Default.Speed,
                                onClick = { onNavigate("live_data") },
                                modifier = Modifier.weight(1f)
                            )
                            OnboardingGuideCard(
                                title = "2. AI Assistant",
                                desc = "Tap the ✨ chat button for instant repair help.",
                                icon = Icons.Default.Psychology,
                                onClick = { onNavigate("guided_diag") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OnboardingGuideCard(
                                title = "3. ECU Network",
                                desc = "Inspect vehicle CAN bus nodes & fault mapping.",
                                icon = Icons.Default.AccountTree,
                                onClick = { onNavigate("topology") },
                                modifier = Modifier.weight(1f)
                            )
                            OnboardingGuideCard(
                                title = "4. Workshop Tasks",
                                desc = "Manage customer repair jobs & parts budget.",
                                icon = Icons.Default.TaskAlt,
                                onClick = {},
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        // Active Vehicle Banner
        item {
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
                            Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = ForgeAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE VEHICLE WORKSPACE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeAmber
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (telemetry.isConnected) ForgeGreen.copy(alpha = 0.2f) else ForgeRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = telemetry.connectionType,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (telemetry.isConnected) ForgeGreen else ForgeRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "2021 Audi S5 Sportback (3.0T V6)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "VIN: WAUZZZF58MA019284 • ${telemetry.connectionStatusText}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeCyan
                    )
                }
            }
        }

        // 1-Click Autonomous Diagnostic Triage & Multi-Service Dispatcher
        item {
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
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = ForgeCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AUTONOMOUS DIAGNOSTIC PIPELINE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeCyan
                            )
                        }
                        Surface(
                            color = ForgeCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "1-CLICK AUTOMATION",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Instantly execute 5-tier triage: OBD DTC extract -> NHTSA VIN & safety recalls -> ALLDATA TSBs -> Nexpart B2B parts check -> Mitchell labor estimate & work order auto-dispatch.",
                        fontSize = 11.sp,
                        color = ForgeOnSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showAutoTriageDialog = true
                                triagePipeline.runAutoTriage(
                                    vin = "WAUZZZF58MA019284",
                                    dtcCodes = telemetry.activeDtcCodes.map { it.code }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN 1-CLICK AUTO-TRIAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showAutoTriageDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("View Pipeline", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Real-Time Anomaly Watchdog Alert (Automated Live Telemetry Spike Trigger)
        if (isAnomalyDetected) {
            item {
                Surface(
                    color = ForgeRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeRed)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = ForgeRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AUTONOMOUS PID ANOMALY WATCHDOG",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeRed
                                )
                            }
                            Text(
                                text = "FREEZE-FRAME CAPTURED",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeAmber
                            )
                        }

                        Text(
                            text = buildString {
                                if (hasFuelTrimAnomaly) append("• Fuel Trim Threshold Exceeded: STFT ${telemetry.fuelTrimShortPct}% + LTFT ${telemetry.fuelTrimLongPct}% (Lean Surge)\n")
                                if (hasCoolantAnomaly) append("• Engine Thermal Warning: Coolant reached ${telemetry.coolantTempC}°C\n")
                                if (hasBatteryAnomaly) append("• Low Voltage Threshold: Charging system at ${telemetry.batteryVoltage}V")
                            }.trimEnd(),
                            fontSize = 11.sp,
                            color = ForgeOnSurface,
                            lineHeight = 15.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onNavigate("guided_diag") },
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeRed, contentColor = Color.White),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Fault Isolation", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onNavigate("oscilloscope") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeAmber),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scope Waveform", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Autonomous OpenManus Live Diagnostic Protocol Banner
        item {
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, tint = ForgeGreen)
                            Text(
                                text = "AUTONOMOUS DIAGNOSTIC PROTOCOL",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeGreen
                            )
                        }
                        Surface(
                            color = ForgeGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen)
                        ) {
                            Text(
                                text = "READY",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Automated behind-the-scenes multi-agent pipeline analyzed OBD PIDs, CAN bus frames, and TSB databases. Final repair plan & OEM parts ready.",
                        fontSize = 11.sp,
                        color = ForgeOnSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = { onNavigate("openmanus") },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VIEW FINAL DIAGNOSTIC REPORT & ACTION PLAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Fast Hands-Free Diagnostic Action Macros Bar
        item {
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FAST AUTOMATION MACROS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MacroButton(
                            title = "AI Protocol",
                            icon = Icons.Default.AutoMode,
                            color = ForgeGreen,
                            onClick = { onNavigate("openmanus") },
                            modifier = Modifier.weight(1f)
                        )
                        MacroButton(
                            title = "DVI PDF",
                            icon = Icons.Default.Assessment,
                            color = ForgeAmber,
                            onClick = { showReportDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        MacroButton(
                            title = "Actuators",
                            icon = Icons.Default.Tune,
                            color = ForgeCyan,
                            onClick = { onNavigate("actuators") },
                            modifier = Modifier.weight(1f)
                        )
                        MacroButton(
                            title = "Inventory",
                            icon = Icons.Default.Inventory2,
                            color = ForgeAmber,
                            onClick = { onNavigate("inventory") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live Gauge Metrics & Instrument Cluster Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME TELEMETRY CLUSTER",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForgeAmber
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = useRadialClusterView,
                        onClick = { useRadialClusterView = true },
                        label = { Text("Gauges", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgeAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = !useRadialClusterView,
                        onClick = { useRadialClusterView = false },
                        label = { Text("Tiles", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgeAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        if (useRadialClusterView) {
            item {
                ObdInstrumentCluster(telemetry = telemetry)
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GaugeTile(
                        title = "ENGINE RPM",
                        value = "${telemetry.rpm}",
                        unit = "RPM",
                        color = ForgeAmber,
                        modifier = Modifier.weight(1f)
                    )
                    GaugeTile(
                        title = "VEHICLE SPEED",
                        value = "${telemetry.speedKmh}",
                        unit = "KM/H",
                        color = ForgeCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GaugeTile(
                        title = "COOLANT TEMP",
                        value = "${telemetry.coolantTempC}",
                        unit = "°C",
                        color = if (telemetry.coolantTempC > 105) ForgeRed else ForgeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    GaugeTile(
                        title = "BATTERY VOLTS",
                        value = "${telemetry.batteryVoltage}",
                        unit = "VDC",
                        color = ForgeGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Diagnostic Fault Codes Section
        item {
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ForgeRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE FAULT CODES (${telemetry.activeDtcCodes.size})",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeRed
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // AI Explain All Button
                            if (telemetry.activeDtcCodes.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        selectedDtcForExplainer = telemetry.activeDtcCodes.firstOrNull()?.code
                                        showDtcExplainerDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("AI EXPLAIN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Generate Report Button
                            Button(
                                onClick = { showReportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("REPORT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            if (telemetry.activeDtcCodes.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onClearDtcs,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeRed),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("CLEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (telemetry.activeDtcCodes.isEmpty()) {
                        Text(
                            text = "No active diagnostic fault codes stored in ECU memory.",
                            fontSize = 13.sp,
                            color = ForgeGreen
                        )
                    } else {
                        telemetry.activeDtcCodes.forEach { dtc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ForgeRed.copy(alpha = 0.1f))
                                    .border(1.dp, ForgeRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedDtcForExplainer = dtc.code
                                        showDtcExplainerDialog = true
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = dtc.code, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = ForgeRed)
                                        Surface(
                                            color = ForgeAmber.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(10.dp))
                                                Text("EXPLAIN", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ForgeAmber)
                                            }
                                        }
                                    }
                                    Text(text = dtc.description, fontSize = 12.sp, color = ForgeOnSurface)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ForgeRed)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = dtc.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Diagnostic Tools
        item {
            Text(
                text = "DIAGNOSTIC SUITE SHORTCUTS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ForgeCyan
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickToolButton("Live Data", Icons.Default.BarChart, "live_data", onNavigate, Modifier.weight(1f))
                QuickToolButton("Topology", Icons.Default.AccountTree, "topology", onNavigate, Modifier.weight(1f))
                QuickToolButton("Scope", Icons.Default.ShowChart, "oscilloscope", onNavigate, Modifier.weight(1f))
                QuickToolButton("Terminal", Icons.Default.Terminal, "terminal", onNavigate, Modifier.weight(1f))
            }
        }

        // Integrated Project & Task Aggregator Dashboard
        item {
            ProjectDashboard(
                projects = projects,
                tasks = tasks,
                onAddTask = onAddTask,
                onUpdateTaskStatus = onUpdateTaskStatus,
                onDeleteTask = onDeleteTask,
                onAddProject = onAddProject,
                onUpdateProjectStatus = onUpdateProjectStatus
            )
        }
    }

    DashboardCoachMarkOverlay(
        visible = showCoachMarks,
        onDismiss = { showCoachMarks = false },
        onNavigateToFeature = onNavigate
    )

    DiagnosticReportDialog(
        visible = showReportDialog,
        telemetry = telemetry,
        onDismiss = { showReportDialog = false }
    )

    if (showDtcExplainerDialog) {
        DtcExplanationDialog(
            visible = showDtcExplainerDialog,
            initialDtcCode = selectedDtcForExplainer,
            allDetectedDtcs = telemetry.activeDtcCodes,
            activeVehicleName = "2021 Audi S5 Sportback (3.0T V6)",
            activeTelemetry = telemetry,
            onDismiss = { showDtcExplainerDialog = false },
            onSendToOpenManus = { onNavigate("openmanus") },
            onAddTaskToWorkOrder = { title, desc ->
                onAddTask(1L, title, desc, "High", "DTC Diagnostic")
            }
        )
    }

    if (showAutoTriageDialog) {
        AutoTriagePipelineDialog(
            triageService = triagePipeline,
            onDismiss = { showAutoTriageDialog = false },
            onNavigateToEstimator = {
                showAutoTriageDialog = false
                onNavigate("estimator")
            },
            onNavigateToParts = {
                showAutoTriageDialog = false
                onNavigate("inventory")
            }
        )
    }
    }
}

@Composable
fun MacroButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = ForgeBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = ForgeOnSurface
            )
        }
    }
}

@Composable
fun OnboardingGuideCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = ForgeBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ForgeCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForgeOnSurface
                )
            }
            Text(
                text = desc,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun GaugeTile(title: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 200),
        label = "tile_color_anim"
    )

    Surface(
        color = ForgeSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn(tween(100))) togetherWith
                                (slideOutVertically { height -> -height / 2 } + fadeOut(tween(100)))
                    },
                    label = "gauge_tile_anim"
                ) { targetVal ->
                    Text(
                        text = targetVal,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = unit, fontSize = 11.sp, color = animatedColor, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
fun QuickToolButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, route: String, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onNavigate(route) },
        color = ForgeSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = ForgeCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = ForgeOnSurface)
        }
    }
}
