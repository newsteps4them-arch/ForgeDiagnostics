package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.forge.app.services.ObdTelemetryData
import com.forge.app.ui.components.DashboardCoachMarkOverlay
import com.forge.app.ui.components.ProjectDashboard
import com.forge.app.ui.theme.*

@Composable
fun DashboardScreen(
    telemetry: ObdTelemetryData,
    projects: List<ProjectEntity> = emptyList(),
    tasks: List<TaskEntity>,
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

        // Live Gauge Metrics Row
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

                        if (telemetry.activeDtcCodes.isNotEmpty()) {
                            TextButton(onClick = onClearDtcs) {
                                Text("CLEAR DTCs", fontSize = 11.sp, color = ForgeRed)
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
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = dtc.code, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = ForgeRed)
                                    Text(text = dtc.description, fontSize = 12.sp, color = ForgeOnSurface)
                                }
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickToolButton("Live Data", Icons.Default.BarChart, "live_data", onNavigate, Modifier.weight(1f))
                QuickToolButton("ECU Topology", Icons.Default.AccountTree, "topology", onNavigate, Modifier.weight(1f))
                QuickToolButton("Oscilloscope", Icons.Default.ShowChart, "oscilloscope", onNavigate, Modifier.weight(1f))
                QuickToolButton("ELM Terminal", Icons.Default.Terminal, "terminal", onNavigate, Modifier.weight(1f))
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
                Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = unit, fontSize = 11.sp, color = color, modifier = Modifier.padding(bottom = 3.dp))
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
