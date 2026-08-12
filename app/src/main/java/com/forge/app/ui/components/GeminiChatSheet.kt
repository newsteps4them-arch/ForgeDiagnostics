package com.forge.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.forge.app.services.AssistantSkill
import com.forge.app.services.GeminiClient
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val text: String,
    val skillName: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatSheet(
    activeVehicle: String = "2021 Audi S5 (3.0T V6)",
    activeTelemetry: String = "RPM: 2,450 | Temp: 92°C | STFT: +14.2%",
    activeProject: String = "Engine Performance Misfire Diagnostic",
    authAndSyncService: com.forge.app.services.AuthAndSyncService? = null,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var promptInput by remember { mutableStateOf("") }
    var selectedSkill by remember { mutableStateOf(AssistantSkill.GENERAL) }
    var isImageAttached by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val userProfile by (authAndSyncService?.currentUser?.collectAsState() ?: remember { mutableStateOf(com.forge.app.services.UserProfile()) })
    val syncStatus by (authAndSyncService?.syncStatus?.collectAsState() ?: remember { mutableStateOf(com.forge.app.services.SyncStatus()) })

    val messages = remember {
        mutableStateListOf(
            ChatMessage("AI", "### Team Forge Assistant Active (Long-Term Memory Enabled)\n\nSigned in as: **${userProfile.displayName}**\nFirestore Sync DB: `${syncStatus.dbName}`\nActive Vehicle: **$activeVehicle**\nActive Project: **$activeProject**\n\nSelect a chatbot skill above for multi-turn diagnostic reasoning with full persistent chat history across sessions.")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        scrimColor = ForgeBackground.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = ForgeCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "FORGE DIAGNOSTIC ASSISTANT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ForgeCyan,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "MEMORY & FIRESTORE ACTIVE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeAmber
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            messages.clear()
                            messages.add(ChatMessage("AI", "### Chat Memory Reset\n\nDiagnostic thread cleared. New vehicle context active for **$activeVehicle**."))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Memory", tint = ForgeOnSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ForgeCyan)
                    }
                }
            }


            // Context Banner
            Surface(
                color = ForgeBackground,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$activeVehicle | $activeTelemetry",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeOnSurfaceVariant
                    )
                }
            }

            // Skill Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(AssistantSkill.entries.toTypedArray()) { skill ->
                    val isSelected = selectedSkill == skill
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSkill = skill },
                        label = {
                            Text(
                                text = skill.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgeCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = ForgeSurfaceVariant,
                            labelColor = ForgeOnSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ForgeBorder,
                            selectedBorderColor = ForgeCyan
                        )
                    )
                }
            }

            // Model Badge & Description for Selected Skill
            Surface(
                color = ForgeBackground,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedSkill.description,
                        fontSize = 10.sp,
                        color = ForgeOnSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = ForgeCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = selectedSkill.modelName,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Quick Preset Prompts for Active Skill
            val presets = when (selectedSkill) {
                AssistantSkill.REPORTING -> listOf("Generate DVI Inspection Report", "Summarize DTC P0300 Misfire Diagnostic", "Draft Customer Cost Estimate")
                AssistantSkill.WEB_DOCS -> listOf("Lookup TSB for 3.0T V6 Misfire", "Search Spark Plug Torque Specs", "ECU Injector Harness Pinout")
                AssistantSkill.MAPS_SEARCH -> listOf("Find nearby Euro OEM parts distributors", "Locate cylinder head machine shops", "Find auto rebuilder with HPFP injectors")
                AssistantSkill.COMPUTE -> listOf("Calculate RPM from Hex 01 0C [1F 40]", "Compute Oscilloscope Frequency & Duty Cycle", "Calculate STFT + LTFT Fuel Trim")
                AssistantSkill.WORKFLOW -> listOf("Create Work Order for Misfire Repair", "Sync Firestore real-time inventory", "Assign Spark Plug Replacement Task")
                AssistantSkill.VISION -> listOf("Identify High Pressure Fuel Injector", "Inspect Spark Plug Electrodes Wear", "Analyze Brake Rotor Grooving")
                AssistantSkill.VIDEO_DIAG -> listOf("Analyze engine acoustic ticking video", "Inspect belt tensioner flutter clip", "Examine oscilloscope motion recording")
                AssistantSkill.AUDIO_TRANSCRIBE -> listOf("Transcribe intake manifold removal dictation", "Record spark plug inspection voice notes", "Transcribe road test diagnostic log")
                AssistantSkill.DEEP_THINKING -> listOf("Deep fault tree analysis for P0300 misfire", "Diagnose intermittent CAN bus signal drop", "Isolate HPFP vs ignition coil failure")
                AssistantSkill.DIAGRAM_GEN -> listOf("Generate 4K wiring blueprint for ECU J220", "Render exploded 3D CAD schematic of HPFP", "Create 16:9 ignition harness circuit diagram")
                AssistantSkill.IMAGE_EDITOR -> listOf("Annotate spark plug electrode erosion photo", "Highlight cylinder 1 carbon glaze wear", "Add pinout callout arrows to connector photo")
                AssistantSkill.VEO_ANIMATE -> listOf("Animate 16:9 3D fuel injector replacement video", "Generate 9:16 portrait assembly motion sequence", "Render fluid flow path 3D video")
                AssistantSkill.VOICE_LIVE -> listOf("Start live audio session for torque specs", "Ask Team Forge hands-free question", "Start live audio step-by-step repair guide")
                AssistantSkill.AGENT_BROWSER -> listOf("Automate OEM recall lookup on NHTSA portal", "Scrape vehicle service history report", "Run automated browser QA test on workshop portal")
                AssistantSkill.BRAINSTORMING -> listOf("Brainstorm ECU diagnostic architecture", "Propose 3 design options for telemetry sync", "Draft technical spec for fuel rail repair")
                AssistantSkill.VISUAL_COMPANION -> listOf("Render HTML mockup of waveform visualizer", "Show split-view layout for oscilloscope", "Create interactive diagram of ECU pinout")
                AssistantSkill.SPEC_REVIEWER -> listOf("Audit fuel system spec for completeness", "Check design spec for YAGNI & TODOs", "Review implementation plan readiness")
                else -> listOf("P0300 Misfire Diagnostic Procedure", "System Too Lean P0171 Checklist", "OBD CAN Bus Network Status")
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(presets) { preset ->
                    Surface(
                        color = ForgeAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            promptInput = preset
                        }
                    ) {
                        Text(
                            text = preset,
                            fontSize = 11.sp,
                            color = ForgeAmber,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = ForgeBorder, modifier = Modifier.padding(vertical = 8.dp))

            // Chat History
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.sender == "USER"
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = if (isUser) ForgeAmber.copy(alpha = 0.2f) else ForgeSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isUser) ForgeAmber else ForgeBorder),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isUser) "TECHNICIAN" else "FORGE AI (${msg.skillName.uppercase()})",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) ForgeAmber else ForgeCyan
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 13.sp,
                                    color = ForgeOnSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ForgeCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Executing ${selectedSkill.title} agent skill...",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multimodal Image Attachment Preview
            if (isImageAttached) {
                Surface(
                    color = ForgeSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Component Photo Attached (Spark_Plug_Cyl1.jpg)", fontSize = 11.sp, color = ForgeCyan)
                        }
                        IconButton(onClick = { isImageAttached = false }, modifier = Modifier.size(20.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove photo", tint = ForgeCyan)
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isImageAttached = !isImageAttached },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Attach Photo",
                        tint = if (isImageAttached) ForgeCyan else ForgeOnSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("Query ${selectedSkill.title} skill...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeCyan,
                        unfocusedBorderColor = ForgeBorder,
                        focusedContainerColor = ForgeBackground,
                        unfocusedContainerColor = ForgeBackground
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val text = promptInput.trim()
                        if (text.isNotEmpty()) {
                            val activeSkillToUse = selectedSkill
                            val userMsg = ChatMessage("USER", text, activeSkillToUse.title)
                            messages.add(userMsg)
                            promptInput = ""
                            isLoading = true

                            // Map previous message history for multi-turn Gemini API request
                            val conversationHistory = messages.map { it.sender to it.text }

                            coroutineScope.launch {
                                authAndSyncService?.saveChatMessageToLongTermMemory(
                                    sender = "USER",
                                    text = text,
                                    skillName = activeSkillToUse.title,
                                    vehicleVin = activeVehicle,
                                    projectTitle = activeProject
                                )

                                val mockBase64 = if (isImageAttached) "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==" else null
                                val reply = if (activeSkillToUse == AssistantSkill.COMPUTE) {
                                    com.forge.app.services.OpenAiClient.queryOpenAiAssistant(
                                        prompt = text,
                                        vehicleContext = "$activeVehicle ($activeTelemetry)",
                                        systemPrompt = "You are Team Forge High-Performance Diagnostic Engine (GPT-4o)."
                                    )
                                } else {
                                    GeminiClient.queryAssistantMultiTurn(
                                        prompt = text,
                                        history = conversationHistory,
                                        skill = activeSkillToUse,
                                        vehicleContext = activeVehicle,
                                        telemetryContext = activeTelemetry,
                                        projectContext = activeProject,
                                        base64Image = mockBase64
                                    )
                                }
                                val aiMsg = ChatMessage("AI", reply, activeSkillToUse.title)
                                messages.add(aiMsg)

                                authAndSyncService?.saveChatMessageToLongTermMemory(
                                    sender = "AI",
                                    text = reply,
                                    skillName = activeSkillToUse.title,
                                    vehicleVin = activeVehicle,
                                    projectTitle = activeProject
                                )

                                isLoading = false
                                isImageAttached = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

