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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.theme.*

data class CoachMarkStep(
    val id: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val featureTip: String,
    val icon: ImageVector,
    val highlightCategory: String
)

val dashboardCoachSteps = listOf(
    CoachMarkStep(
        id = 1,
        title = "Vehicle Workspace",
        subtitle = "STEP 1 OF 5",
        description = "Monitors the currently connected vehicle's VIN, engine model, OBD-II communication protocol (ISO 15765-4 CAN), and real-time dongle link status.",
        featureTip = "Tip: Status displays ONLINE in green when connected to an OBD Bluetooth reader.",
        icon = Icons.Default.DirectionsCar,
        highlightCategory = "VEHICLE HEADER"
    ),
    CoachMarkStep(
        id = 2,
        title = "Live OBD Telemetry Gauges",
        subtitle = "STEP 2 OF 5",
        description = "Provides real-time PID streams for Engine RPM, Vehicle Speed, Coolant Temperature, and Battery Voltage with color-coded safety thresholds.",
        featureTip = "Tip: Tap 'Live Telemetry' in the side drawer to access oscilloscope graphing and raw PID math.",
        icon = Icons.Default.Speed,
        highlightCategory = "GAUGES"
    ),
    CoachMarkStep(
        id = 3,
        title = "Diagnostic Trouble Codes (DTCs)",
        subtitle = "STEP 3 OF 5",
        description = "Identifies active ECU fault codes (e.g. P0300 Random Misfire), severity indicators, and provides a 'Clear Codes' button after repairs.",
        featureTip = "Tip: Tap 'AI Guided Repair' on any code to get step-by-step OEM repair instructions.",
        icon = Icons.Default.Warning,
        highlightCategory = "FAULT CODES"
    ),
    CoachMarkStep(
        id = 4,
        title = "Workshop Tasks & Budget",
        subtitle = "STEP 4 OF 5",
        description = "Manage customer repair jobs, track parts budget totals, filter tasks by priority (High, Medium, Low), and check off completed work items.",
        featureTip = "Tip: Tap '+ Add Task' to assign new repair steps directly to the active vehicle job.",
        icon = Icons.Default.TaskAlt,
        highlightCategory = "TASKS"
    ),
    CoachMarkStep(
        id = 5,
        title = "AI Repair Assistant & Pro Tools",
        subtitle = "STEP 5 OF 5",
        description = "Access the AI Chatbot (✨) anywhere in the app for verified repair research with dual technical and layman explanations.",
        featureTip = "Tip: Use the side drawer menu to open Wiring Diagrams, ECU Topology, and the Parts Catalog.",
        icon = Icons.Default.Psychology,
        highlightCategory = "AI & TOOLS"
    )
)

@Composable
fun DashboardCoachMarkOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onNavigateToFeature: (String) -> Unit = {}
) {
    if (!visible) return

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = dashboardCoachSteps.getOrElse(currentStepIndex) { dashboardCoachSteps.first() }

    val pulsingScale by rememberInfiniteTransition(label = "CoachPulse").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable { /* Block clicks behind overlay */ }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { 40 }))
                    .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically(targetOffsetY = { -40 }))
            },
            label = "CoachStepTransition"
        ) { currentStep ->
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ForgeCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = ForgeCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = ForgeCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentStep.subtitle,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeCyan
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Skip Tour",
                                tint = ForgeOnSurfaceVariant
                            )
                        }
                    }

                    // Step Icon & Title Block
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = ForgeCyan.copy(alpha = 0.2f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan),
                            modifier = Modifier.scale(pulsingScale)
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentStep.icon,
                                    contentDescription = currentStep.title,
                                    tint = ForgeCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = currentStep.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "FEATURE HIGHLIGHT: ${currentStep.highlightCategory}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeAmber
                            )
                        }
                    }

                    Divider(color = ForgeBorder, thickness = 1.dp)

                    // Step Description & Practical Tip
                    Text(
                        text = currentStep.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )

                    Surface(
                        color = ForgeBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ForgeCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = currentStep.featureTip,
                                fontSize = 11.sp,
                                color = ForgeCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Progress Dots Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dashboardCoachSteps.forEachIndexed { index, _ ->
                            val active = index == currentStepIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (active) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) ForgeCyan else ForgeOnSurfaceVariant.copy(alpha = 0.4f))
                            )
                        }
                    }

                    // Action Controls Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = { currentStepIndex-- },
                                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeOnSurface)
                            ) {
                                Text("Back", fontSize = 12.sp)
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("Skip Tour", fontSize = 12.sp, color = ForgeOnSurfaceVariant)
                            }
                        }

                        Button(
                            onClick = {
                                if (currentStepIndex < dashboardCoachSteps.size - 1) {
                                    currentStepIndex++
                                } else {
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ForgeCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = if (currentStepIndex == dashboardCoachSteps.size - 1) "Get Started" else "Next Step",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
