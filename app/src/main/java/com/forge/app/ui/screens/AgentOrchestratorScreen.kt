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
import com.forge.app.services.AgentActivityStatus
import com.forge.app.services.ForgeAgentOrchestrator
import com.forge.app.services.ForgeAgentType
import com.forge.app.ui.theme.*

@Composable
fun AgentOrchestratorScreen(
    orchestrator: ForgeAgentOrchestrator? = null,
    onNavigate: ((String) -> Unit)? = null
) {
    val state by (orchestrator?.orchestratorState?.collectAsState()
        ?: remember { mutableStateOf(com.forge.app.services.AgentOrchestratorState()) })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Banner
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = ForgeGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "MULTI-TIER AGENT ORCHESTRATOR",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ForgeGreen, shape = CircleShape)
                    )
                }

                Text(
                    "Active Vehicle: ${state.globalContextVehicle} • DTC Flags: ${state.globalContextDtcCount}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeOnSurfaceVariant
                )

                if (onNavigate != null) {
                    Button(
                        onClick = { onNavigate("openmanus") },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PrecisionManufacturing, contentDescription = null, tint = ForgeBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "OPEN OPENMANUS AUTONOMOUS STUDIO",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeBackground
                        )
                    }
                }
            }
        }

        Text(
            "CONFIGURED AGENT LAYER NODES",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = ForgeAmber,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.activeAgents) { agent ->
                AgentCard(agent)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "ORCHESTRATOR SYSTEM TELEMETRY LOG",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.systemLog.takeLast(10)) { log ->
                Surface(
                    color = ForgeSurfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = log,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeOnSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AgentCard(agent: AgentActivityStatus) {
    val accentColor = when (agent.agentType) {
        ForgeAgentType.FrontendUiAgent -> ForgeCyan
        ForgeAgentType.ClientHardwareAgent -> ForgeAmber
        ForgeAgentType.MiddlewareTelemetryAgent -> ForgeGreen
        ForgeAgentType.BackendSyncAgent -> ForgeCyan
        ForgeAgentType.ServerAiAgent -> Color(0xFFFF4081)
        ForgeAgentType.OpenManusAgent -> Color(0xFFFFB300)
        ForgeAgentType.CanBusUdsAgent -> Color(0xFF00E5FF)
        ForgeAgentType.ElectricalPhysicsAgent -> Color(0xFF76FF03)
        ForgeAgentType.NhtsaRecallAgent -> Color(0xFFE040FB)
        ForgeAgentType.JulesEngineeringAgent -> Color(0xFF448AFF)
    }

    Surface(
        color = ForgeSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (agent.isActive) ForgeGreen else ForgeRed, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = agent.agentType.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForgeOnSurface
                    )
                }

                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = agent.agentType.id.uppercase(),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = agent.agentType.role,
                fontSize = 10.sp,
                color = ForgeOnSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Task: ${agent.currentTask}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Ops: ${agent.processedCount}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeOnSurfaceVariant
                )
            }
        }
    }
}
