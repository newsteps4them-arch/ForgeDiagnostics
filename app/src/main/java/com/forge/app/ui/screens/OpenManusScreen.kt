// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.*
import com.forge.app.ui.components.OpenManusTelemetryDashboard
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenManusScreen(
    openManusService: OpenManusAgentService,
    telemetry: ObdTelemetryData,
    activeVehicleName: String = "2021 Audi S5 Sportback",
    hardwareModule: ObdDiagnosticHardwareModule? = null,
    onAddTaskToWorkOrder: ((title: String, desc: String) -> Unit)? = null
) {
    val state by openManusService.state.collectAsState()
    val hardwareState = hardwareModule?.hardwareState?.collectAsState()?.value
    val coroutineScope = rememberCoroutineScope()

    var showAdvancedLogs by remember { mutableStateOf(false) }
    var showTelemetryDashboard by remember { mutableStateOf(true) }
    var showEndpointDialog by remember { mutableStateOf(false) }
    var showHardwareModal by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf(state.customEndpointUrl) }
    var customModelInput by remember { mutableStateOf(state.customModelName) }
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }

    // Automated Trigger: Run autonomous background pipeline if not already generated
    LaunchedEffect(activeVehicleName, telemetry.activeDtcCodes.size) {
        val dtcCodesList = telemetry.activeDtcCodes.map { it.code }
        val telemetrySummary = "RPM=${telemetry.rpm}, Temp=${telemetry.coolantTempC}C, Voltage=${"%.1f".format(telemetry.batteryVoltage)}V, DTCs=${dtcCodesList.joinToString().ifEmpty { "None" }}"
        openManusService.autoDiagnoseIfIdle(
            vehicleContext = activeVehicleName,
            activeDtcs = dtcCodesList,
            telemetrySummary = telemetrySummary
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Banner: Autonomous Agent Status
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ForgeGreen.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ForgeGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoMode,
                                contentDescription = null,
                                tint = ForgeGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "OPENMANUS AUTONOMOUS DIAGNOSTICS",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeGreen,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Background Multi-Agent Automotive Synthesis Engine",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeOnSurfaceVariant
                            )
                        }
                    }

                    // Live Automation Badge
                    Surface(
                        color = if (state.isRunning) ForgeAmber.copy(alpha = 0.2f) else ForgeGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (state.isRunning) ForgeAmber else ForgeGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (state.isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    color = ForgeAmber,
                                    strokeWidth = 1.5.dp
                                )
                                Text(
                                    text = "SYNTHESIZING...",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeAmber
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(ForgeGreen, shape = CircleShape)
                                )
                                Text(
                                    text = "AUTOMATED SYNC",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeGreen
                                )
                            }
                        }
                    }
                }

                // Vehicle Context & Quick Refresh Bar
                Surface(
                    color = ForgeSurfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vehicle: $activeVehicleName | Active DTCs: ${telemetry.activeDtcCodes.size} | ECT: ${telemetry.coolantTempC}°C",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeCyan
                        )

                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    val dtcCodesList = telemetry.activeDtcCodes.map { it.code }
                                    val telemetrySummary = "RPM=${telemetry.rpm}, Temp=${telemetry.coolantTempC}C, Voltage=${"%.1f".format(telemetry.batteryVoltage)}V, DTCs=${dtcCodesList.joinToString().ifEmpty { "None" }}"
                                    openManusService.runAutonomousDiagnosis(
                                        goal = if (dtcCodesList.isNotEmpty()) "Autonomous diagnosis for DTCs [${dtcCodesList.joinToString()}]" else "Full vehicle telemetry diagnostic audit",
                                        vehicleContext = activeVehicleName,
                                        activeDtcs = dtcCodesList,
                                        telemetrySummary = telemetrySummary
                                    )
                                }
                            },
                            enabled = !state.isRunning,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = ForgeAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "RE-ANALYZE",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live OBD Hardware Diagnostic Interface Toolbar
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ForgeCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (hardwareState?.selectedInterface == ObdHardwareInterface.BLUETOOTH_SPP) Icons.Default.Bluetooth else Icons.Default.Usb,
                        contentDescription = null,
                        tint = ForgeCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "OBD HARDWARE INTERFACE: ${hardwareState?.selectedInterface?.name ?: "USB_OTG"}",
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan
                        )
                        Text(
                            text = "${hardwareState?.connectedDeviceName ?: "Ready"} | ${hardwareState?.activeProtocol ?: "ISO 15765-4 CAN"}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeOnSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Interface toggle button (USB vs Bluetooth)
                    OutlinedButton(
                        onClick = {
                            val next = if (hardwareState?.selectedInterface == ObdHardwareInterface.USB_OTG) 
                                ObdHardwareInterface.BLUETOOTH_SPP else ObdHardwareInterface.USB_OTG
                            hardwareModule?.setHardwareInterface(next)
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeCyan)
                    ) {
                        Text(
                            if (hardwareState?.selectedInterface == ObdHardwareInterface.USB_OTG) "SWITCH BT" else "SWITCH USB",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Query DTCs Button
                    Button(
                        onClick = {
                            hardwareModule?.fetchLiveDiagnosticTroubleCodes(
                                vehicleName = activeVehicleName,
                                autoTriggerOpenManus = true
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        if (hardwareState?.isFetchingDtcs == true) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = ForgeBackground, strokeWidth = 1.5.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = ForgeBackground, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "QUERY DTCs",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeBackground
                        )
                    }
                }
            }
        }

        // Main Scrollable Area: FINAL OUTPUTS FIRST
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Real-Time OBD-II Diagnostic Telemetry Streams Dashboard
            item {
                OpenManusTelemetryDashboard(
                    telemetry = telemetry,
                    hardwareModule = hardwareModule,
                    openManusService = openManusService,
                    activeVehicleName = activeVehicleName
                )
            }

            // Live Status Indicator when in progress
            if (state.isRunning) {
                item {
                    Surface(
                        color = ForgeSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ForgeAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = ForgeAmber,
                                strokeWidth = 2.5.dp
                            )
                            Column {
                                Text(
                                    text = "AUTONOMOUS BACKGROUND PIPELINE ACTIVE",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeAmber
                                )
                                Text(
                                    text = state.currentThought,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ForgeOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // PRIMARY HERO COMPONENT: The Final Diagnostic Protocol & Action Plan
            state.finalReport?.let { report ->
                item {
                    Surface(
                        color = ForgeSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, ForgeGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = ForgeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "FINAL DIAGNOSTIC PROTOCOL",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeGreen
                                    )
                                }

                                Surface(
                                    color = ForgeGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, ForgeGreen)
                                ) {
                                    Text(
                                        text = "CONFIDENCE: ${report.confidenceScore}%",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // 1. PRIMARY ROOT CAUSE BOX
                            Surface(
                                color = ForgeBackground,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ForgeAmber.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "PRIMARY IDENTIFIED ROOT CAUSE:",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeAmber
                                    )
                                    Text(
                                        text = report.primaryRootCause,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ForgeOnSurface
                                    )
                                }
                            }

                            // 2. ACTIONABLE STEP-BY-STEP REPAIR PROCEDURES
                            if (report.stepByStepInspectionPlan.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "ACTIONABLE REPAIR & INSPECTION CHECKLIST:",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeCyan
                                    )

                                    report.stepByStepInspectionPlan.forEachIndexed { index, step ->
                                        val isCompleted = completedSteps.contains(index)
                                        Surface(
                                            color = if (isCompleted) ForgeGreen.copy(alpha = 0.1f) else ForgeSurfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, if (isCompleted) ForgeGreen.copy(alpha = 0.5f) else ForgeBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    completedSteps = if (isCompleted) {
                                                        completedSteps - index
                                                    } else {
                                                        completedSteps + index
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (isCompleted) ForgeGreen else ForgeOnSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = step,
                                                    fontSize = 10.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isCompleted) ForgeGreen else ForgeOnSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. RECOMMENDED OEM REPLACEMENT PARTS
                            if (report.recommendedParts.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "RECOMMENDED OEM REPLACEMENT PARTS & SEALS:",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeAmber
                                    )

                                    report.recommendedParts.forEach { part ->
                                        Surface(
                                            color = ForgeBackground,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, ForgeBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Build,
                                                    contentDescription = null,
                                                    tint = ForgeAmber,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = part,
                                                    fontSize = 10.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = ForgeOnSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. SAFETY PRECAUTIONS
                            if (report.safetyCautions.isNotEmpty()) {
                                Surface(
                                    color = ForgeRed.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, ForgeRed.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = ForgeRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "SAFETY & ELECTRICAL PRECAUTIONS:",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ForgeRed
                                            )
                                        }
                                        report.safetyCautions.forEach { caution ->
                                            Text(
                                                text = "• $caution",
                                                fontSize = 9.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = ForgeOnSurface.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. LABOR HOURS & FAST CONVERT TO WORK ORDER
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "EST. LABOR: ${report.estimatedLaborHours} HRS",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ForgeGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Shop Rate: \$140/hr (~ \$${"%.2f".format(report.estimatedLaborHours * 140.0)})",
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ForgeOnSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        report.stepByStepInspectionPlan.forEach { step ->
                                            onAddTaskToWorkOrder?.invoke(
                                                "OpenManus Protocol: $step",
                                                "Autonomous diagnosis generated for ${report.vehicleContext}"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PostAdd,
                                        contentDescription = null,
                                        tint = ForgeBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "ADD ALL TO WORK ORDER",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // COLLAPSIBLE BEHIND-THE-SCENES SECTION (For technicians who want raw telemetry/math)
            item {
                Surface(
                    color = ForgeSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ForgeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedLogs = !showAdvancedLogs },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = ForgeCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "BEHIND-THE-SCENES AGENT EXECUTION LOGS",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeCyan
                                )
                            }

                            Icon(
                                imageVector = if (showAdvancedLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = ForgeCyan
                            )
                        }

                        if (showAdvancedLogs) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = ForgeBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Model backend chip selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AgentModelProvider.values().forEach { provider ->
                                    val isSelected = state.selectedProvider == provider
                                    Surface(
                                        color = if (isSelected) ForgeAmber.copy(alpha = 0.2f) else ForgeSurfaceVariant,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, if (isSelected) ForgeAmber else ForgeBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { openManusService.setModelProvider(provider) }
                                    ) {
                                        Text(
                                            text = when (provider) {
                                                AgentModelProvider.GEMINI_FLASH -> "Gemini 2.5"
                                                AgentModelProvider.GEMINI_PRO -> "Gemini Pro"
                                                AgentModelProvider.LOCAL_OLLAMA -> "Ollama"
                                                AgentModelProvider.HUGGING_FACE -> "HuggingFace"
                                                AgentModelProvider.OPEN_ROUTER -> "OpenRouter"
                                            },
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ForgeAmber else ForgeOnSurface,
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Steps Trace
                            if (state.steps.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.steps.forEach { step ->
                                        StepCard(step)
                                    }
                                }
                            } else {
                                Text(
                                    text = "No intermediate traces recorded. Autonomous background engine directly delivered final protocol.",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ForgeOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Endpoint Configuration Dialog for Local Ollama / Custom API
    if (showEndpointDialog) {
        AlertDialog(
            onDismissRequest = { showEndpointDialog = false },
            title = { Text("Local Ollama Endpoint Configuration", fontFamily = FontFamily.Monospace, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Connect to local AI instance (e.g. Ollama, LocalAI, vLLM on local network):", fontSize = 11.sp)
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("http://localhost:11434") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customModelInput,
                        onValueChange = { customModelInput = it },
                        label = { Text("Model Tag") },
                        placeholder = { Text("deepseek-r1:8b, llama3.2") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openManusService.setCustomEndpoint(customUrlInput, customModelInput)
                        showEndpointDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber)
                ) {
                    Text("Save & Connect", color = ForgeBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndpointDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StepCard(step: OpenManusStep) {
    Surface(
        color = ForgeSurfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ForgeBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(ForgeCyan.copy(alpha = 0.2f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${step.stepNumber}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan
                        )
                    }
                    Text(
                        text = step.agentName,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeOnSurface
                    )
                }

                Text(
                    text = "${step.phase} • ${step.timestamp}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeOnSurfaceVariant
                )
            }

            // Thought
            Text(
                text = "Reasoning: ${step.thought}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = ForgeOnSurfaceVariant
            )

            // Tool Invocations
            if (step.toolInvocations.isNotEmpty()) {
                step.toolInvocations.forEach { tool ->
                    Surface(
                        color = ForgeBackground,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, ForgeCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tool: ${tool.toolName}",
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeCyan
                                )
                                Text(
                                    text = "${tool.durationMs}ms",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ForgeOnSurfaceVariant
                                )
                            }
                            Text(
                                text = tool.outputData,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeOnSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Observation
            if (step.observation.isNotBlank()) {
                Text(
                    text = "Observation: ${step.observation}",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
