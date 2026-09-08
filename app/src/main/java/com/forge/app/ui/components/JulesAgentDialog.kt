// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.forge.app.services.*
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun JulesAgentDialog(
    julesService: JulesAgentService,
    onDismiss: () -> Unit
) {
    val state by julesService.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var selectedTab by remember { mutableStateOf(0) } // 0: Sessions, 1: New Prompt/PR, 2: Active Session
    var promptInput by remember { mutableStateOf("Fix STFT surge anomaly handler and add freeze-frame logging for Mode 01 PID 0x06.") }
    var titleInput by remember { mutableStateOf("Autonomous Telemetry Hotfix") }
    var selectedSource by remember { mutableStateOf("sources/github/teamforge-automotive/core-telemetry") }
    var autoCreatePr by remember { mutableStateOf(true) }
    var followUpMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            color = ForgeSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = ForgeCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "GOOGLE JULES REST API AGENT",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeCyan
                            )
                            Text(
                                text = "Autonomous Software Engineering & PR Generation",
                                fontSize = 9.sp,
                                color = ForgeOnSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ForgeOnSurfaceVariant)
                    }
                }

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ForgeBackground,
                    contentColor = ForgeCyan
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Sessions (${state.sessions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Create PR", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    if (state.activeSession != null) {
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Chat / Activities", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = ForgeCyan,
                        trackColor = ForgeSurfaceVariant
                    )
                }

                // Tab 0: Sessions List
                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.sessions) { session ->
                            SessionCard(
                                session = session,
                                onClick = {
                                    selectedTab = 2
                                },
                                onOpenPr = {
                                    session.prUrl?.let { url -> uriHandler.openUri(url) }
                                }
                            )
                        }
                    }
                }

                // Tab 1: New Session / PR Generator
                if (selectedTab == 1) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Target Repository Source:",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeAmber
                        )

                        Surface(
                            color = ForgeBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Source, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedSource, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                            }
                        }

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Session / PR Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            label = { Text("Engineering Task Prompt for Jules") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            maxLines = 4
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            var requirePlanApproval by remember { mutableStateOf(false) }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = autoCreatePr,
                                        onCheckedChange = { autoCreatePr = it },
                                        colors = CheckboxDefaults.colors(checkedColor = ForgeCyan)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Auto-Create Pull Request (AUTO_CREATE_PR mode)",
                                        fontSize = 11.sp,
                                        color = ForgeOnSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = requirePlanApproval,
                                        onCheckedChange = { requirePlanApproval = it },
                                        colors = CheckboxDefaults.colors(checkedColor = ForgeAmber)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Require Plan Approval before code modification",
                                        fontSize = 11.sp,
                                        color = ForgeOnSurface
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    julesService.createSession(
                                        prompt = promptInput,
                                        title = titleInput,
                                        sourceName = selectedSource,
                                        autoCreatePr = autoCreatePr,
                                        requirePlanApproval = false
                                    )
                                    selectedTab = 2
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DISPATCH TO JULES & CREATE PR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Tab 2: Active Session Activities & Chat
                if (selectedTab == 2) {
                    val active = state.activeSession ?: state.sessions.firstOrNull()
                    if (active != null) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Header of Active Session
                            Surface(
                                color = ForgeBackground,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(active.title ?: "Autonomous Patch", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ForgeOnSurface)
                                            Text("ID: ${active.id} • ${active.state}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            val prLink = active.prUrl
                                            if (prLink != null) {
                                                Button(
                                                    onClick = { uriHandler.openUri(prLink) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeGreen, contentColor = Color.Black),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("PR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        julesService.deleteSession(active.id)
                                                        selectedTab = 0
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Session", tint = ForgeRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    // Action bar if plan approval needed
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    julesService.approvePlan(active.id)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve Plan (POST :approvePlan)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val url = active.url ?: "https://jules.google.com/session/${active.id}"
                                                uriHandler.openUri(url)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeCyan)
                                        ) {
                                            Text("Open on Jules Web", fontSize = 9.sp)
                                        }
                                    }
                                }
                            }

                            // Activity stream
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.activities) { act ->
                                    val isAgent = act.originator == "agent" || act.author == "JULES_AGENT"
                                    Surface(
                                        color = if (isAgent) ForgeSurfaceVariant.copy(alpha = 0.6f) else ForgeCyan.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAgent) ForgeBorder else ForgeCyan.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val typeLabel = when (act.eventType) {
                                                    ActivityEventType.PLAN_GENERATED -> "📋 PLAN GENERATED"
                                                    ActivityEventType.PLAN_APPROVED -> "✅ PLAN APPROVED"
                                                    ActivityEventType.USER_MESSAGED -> "👤 USER INSTRUCTION"
                                                    ActivityEventType.AGENT_MESSAGED -> "🤖 JULES MESSAGE"
                                                    ActivityEventType.PROGRESS_UPDATED -> "⚡ PROGRESS UPDATE"
                                                    ActivityEventType.SESSION_COMPLETED -> "🎉 SESSION COMPLETED"
                                                    ActivityEventType.SESSION_FAILED -> "❌ SESSION FAILED"
                                                    ActivityEventType.CODE_CHANGES -> "📝 CODE CHANGES (DIFF)"
                                                    ActivityEventType.BASH_OUTPUT -> "💻 BASH OUTPUT"
                                                    ActivityEventType.GENERIC -> if (isAgent) "🤖 JULES AGENT" else "👤 YOU"
                                                }

                                                Text(
                                                    text = typeLabel,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (act.eventType) {
                                                        ActivityEventType.PLAN_APPROVED, ActivityEventType.SESSION_COMPLETED -> ForgeGreen
                                                        ActivityEventType.SESSION_FAILED -> ForgeRed
                                                        ActivityEventType.USER_MESSAGED -> ForgeAmber
                                                        else -> ForgeCyan
                                                    }
                                                )

                                                if (act.createTime != null) {
                                                    Text(
                                                        text = act.createTime.take(19).replace("T", " "),
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = ForgeOnSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Render Plan Steps if present
                                            val plan = act.planGenerated?.plan
                                            if (plan != null && plan.steps.isNotEmpty()) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                        .padding(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    plan.steps.forEach { step ->
                                                        Row(verticalAlignment = Alignment.Top) {
                                                            Text(
                                                                text = "${step.index + 1}. ",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ForgeCyan,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                            Column {
                                                                Text(
                                                                    text = step.title,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = ForgeOnSurface
                                                                )
                                                                if (!step.description.isNullOrBlank()) {
                                                                    Text(
                                                                        text = step.description,
                                                                        fontSize = 9.sp,
                                                                        color = ForgeOnSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = act.displayMessage,
                                                    fontSize = 11.sp,
                                                    color = ForgeOnSurface,
                                                    lineHeight = 15.sp
                                                )
                                            }

                                            // Render Git Patch Diff Artifacts
                                            act.artifacts?.forEach { artifact ->
                                                val patch = artifact.changeSet?.gitPatch
                                                if (patch != null && !patch.unidiffPatch.isNullOrBlank()) {
                                                    Surface(
                                                        color = Color(0xFF0F172A),
                                                        shape = RoundedCornerShape(4.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(6.dp)) {
                                                            if (!patch.suggestedCommitMessage.isNullOrBlank()) {
                                                                Text(
                                                                    text = "commit: ${patch.suggestedCommitMessage}",
                                                                    fontSize = 9.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    color = ForgeGreen,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            Text(
                                                                text = patch.unidiffPatch,
                                                                fontSize = 8.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = ForgeCyan.copy(alpha = 0.9f)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Render Bash Output Artifacts
                                                val bash = artifact.bashOutput
                                                if (bash != null) {
                                                    Surface(
                                                        color = Color.Black,
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(6.dp)) {
                                                            Text(
                                                                text = "$ ${bash.command}",
                                                                fontSize = 9.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = ForgeAmber
                                                            )
                                                            Text(
                                                                text = bash.output,
                                                                fontSize = 8.sp,
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
                            }

                            // Follow-up message input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = followUpMessage,
                                    onValueChange = { followUpMessage = it },
                                    placeholder = { Text("Send instruction to Jules...", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (followUpMessage.isNotBlank()) {
                                            coroutineScope.launch {
                                                julesService.sendMessage(active.id, followUpMessage)
                                                followUpMessage = ""
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: JulesSession,
    onClick: () -> Unit,
    onOpenPr: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = ForgeBackground,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.title ?: "Autonomous Patch",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForgeCyan
                )
                val stateColor = when (session.state) {
                    "COMPLETED" -> ForgeGreen
                    "IN_PROGRESS", "PLANNING" -> ForgeCyan
                    "AWAITING_PLAN_APPROVAL", "AWAITING_USER_FEEDBACK" -> ForgeAmber
                    "QUEUED", "PAUSED" -> ForgeOnSurfaceVariant
                    "FAILED" -> ForgeRed
                    else -> ForgeCyan
                }

                Surface(
                    color = stateColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = session.state,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = stateColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = session.prompt,
                fontSize = 11.sp,
                color = ForgeOnSurfaceVariant,
                maxLines = 2
            )

            if (session.prUrl != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.prTitle ?: "Pull Request Created",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeAmber,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onOpenPr,
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
