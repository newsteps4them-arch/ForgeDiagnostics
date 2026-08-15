package com.forge.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.data.ChatMessageEntity
import com.forge.app.data.ForgeRepository
import com.forge.app.services.AssistantSkill
import com.forge.app.services.AuthAndSyncService
import com.forge.app.services.GeminiClient
import com.forge.app.services.ScreenAiContextRegistry
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val text: String,
    val skillName: String = "General",
    val screenTag: String = "GLOBAL",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatSheet(
    currentRoute: String = "dashboard",
    initialPrompt: String? = null,
    activeVehicle: String = "2021 Audi S5 (3.0T V6)",
    activeTelemetry: String = "RPM: 2,450 | Temp: 92°C | STFT: +14.2%",
    activeProject: String = "Engine Performance Misfire Diagnostic",
    repository: ForgeRepository? = null,
    authAndSyncService: AuthAndSyncService? = null,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val screenContext = remember(currentRoute) { ScreenAiContextRegistry.getContextForRoute(currentRoute) }

    var promptInput by remember { mutableStateOf(initialPrompt ?: "") }
    var selectedSkill by remember { mutableStateOf(screenContext.defaultSkill) }
    var isImageAttached by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showOnlyCurrentScreenMemory by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val userProfile by (authAndSyncService?.currentUser?.collectAsState() ?: remember { mutableStateOf(com.forge.app.services.UserProfile()) })
    val syncStatus by (authAndSyncService?.syncStatus?.collectAsState() ?: remember { mutableStateOf(com.forge.app.services.SyncStatus()) })

    // Observe persistent chat messages from Room database
    val persistedEntities by (repository?.chatMessages?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf<List<ChatMessageEntity>>(emptyList()) })

    // Local fallback message list if database is empty or offline
    val localMessages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "AI",
                text = """
                    ### 🛠️ Team Forge AI Specialist Active
                    
                    **Screen Mode:** `${screenContext.title.uppercase()}` (${screenContext.specialistName})  
                    **Vehicle:** **$activeVehicle**  
                    **Live Telemetry:** `$activeTelemetry`  
                    **Firestore Sync DB:** `${syncStatus.dbName}`  
                    
                    I am specialized for this screen and possess long-term persistent memory across all workshop tools. Ask any technical question or select a preset below!
                """.trimIndent(),
                skillName = screenContext.defaultSkill.title,
                screenTag = screenContext.contextTag
            )
        )
    }

    // Combined or database-driven active message list
    val allDisplayMessages = remember(persistedEntities, localMessages.size) {
        if (persistedEntities.isNotEmpty()) {
            persistedEntities.map { entity ->
                ChatMessage(
                    sender = entity.sender,
                    text = entity.text,
                    skillName = entity.skillName,
                    screenTag = if (entity.projectTitle.contains(":")) entity.projectTitle.substringBefore(":") else screenContext.contextTag,
                    timestamp = entity.timestamp
                )
            }
        } else {
            localMessages.toList()
        }
    }

    val filteredMessages = remember(allDisplayMessages, showOnlyCurrentScreenMemory, screenContext.contextTag) {
        if (showOnlyCurrentScreenMemory) {
            allDisplayMessages.filter { it.screenTag.equals(screenContext.contextTag, ignoreCase = true) || it.screenTag.equals("GLOBAL", ignoreCase = true) }
        } else {
            allDisplayMessages
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        scrimColor = ForgeBackground.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(16.dp)
                .testTag("gemini_chat_sheet")
        ) {
            // Sheet Header with Screen Specialization Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ForgeCyan.copy(alpha = 0.2f))
                            .border(1.dp, ForgeCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Specialist",
                            tint = ForgeCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = screenContext.title.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ForgeCyan,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ForgeAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "SPECIALIST",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeAmber
                                )
                            }
                        }
                        Text(
                            text = screenContext.specialistName,
                            fontSize = 10.sp,
                            color = ForgeOnSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("chat_clear_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Memory", tint = ForgeOnSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ForgeCyan)
                    }
                }
            }

            // Context Banner (Active Screen + Connected Vehicle + Telemetry)
            Surface(
                color = ForgeBackground,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$activeVehicle | $activeTelemetry",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeOnSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ForgeGreen.copy(alpha = 0.15f))
                            .border(0.5.dp, ForgeGreen, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ROOM & FIRESTORE SYNC",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeGreen
                        )
                    }
                }
            }

            // Memory Scope Toggle & Screen Specialization Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AGENT SKILLS & SPECIALIZATIONS:",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeAmber,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showOnlyCurrentScreenMemory = !showOnlyCurrentScreenMemory }
                ) {
                    Icon(
                        imageVector = if (showOnlyCurrentScreenMemory) Icons.Default.FilterAlt else Icons.Default.AllInclusive,
                        contentDescription = null,
                        tint = ForgeCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showOnlyCurrentScreenMemory) "This Screen Only" else "All Memory (${allDisplayMessages.size})",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeCyan
                    )
                }
            }

            // Skill Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 3.dp)
            ) {
                items(AssistantSkill.entries.toTypedArray()) { skill ->
                    val isSelected = selectedSkill == skill
                    val isScreenDefault = screenContext.defaultSkill == skill
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSkill = skill },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isScreenDefault) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Recommended",
                                        tint = if (isSelected) Color.Black else ForgeAmber,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = skill.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
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
                        ),
                        modifier = Modifier.testTag("chat_skill_chip_${skill.id}")
                    )
                }
            }

            // Screen Contextual Prompt Presets
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 3.dp)
            ) {
                items(screenContext.suggestedPrompts) { preset ->
                    Surface(
                        color = ForgeAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            promptInput = preset
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ForgeAmber,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = preset,
                                fontSize = 10.sp,
                                color = ForgeAmber
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = ForgeBorder, modifier = Modifier.padding(vertical = 6.dp))

            // Chat Message History Stream
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMessages) { msg ->
                    val isUser = msg.sender == "USER"
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = if (isUser) ForgeAmber.copy(alpha = 0.18f) else ForgeSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isUser) ForgeAmber else ForgeBorder),
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isUser) "TECHNICIAN" else "FORGE AI (${msg.skillName.uppercase()})",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) ForgeAmber else ForgeCyan
                                        )
                                    }
                                    Text(
                                        text = msg.screenTag,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ForgeOnSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 12.sp,
                                    color = ForgeOnSurface,
                                    lineHeight = 17.sp
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
                                modifier = Modifier.size(14.dp),
                                color = ForgeCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Querying ${selectedSkill.title} specialist (${screenContext.title})...",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Component Photo Attached (Diagnostic_Snap.jpg)", fontSize = 11.sp, color = ForgeCyan)
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
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Attach Component Photo",
                        tint = if (isImageAttached) ForgeCyan else ForgeOnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            "Ask ${screenContext.title} specialist...",
                            fontSize = 11.sp,
                            color = ForgeOnSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeCyan,
                        unfocusedBorderColor = ForgeBorder,
                        focusedContainerColor = ForgeBackground,
                        unfocusedContainerColor = ForgeBackground
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        val text = promptInput.trim()
                        if (text.isNotEmpty()) {
                            val activeSkillToUse = selectedSkill
                            val currentScreenTag = screenContext.contextTag
                            promptInput = ""
                            isLoading = true

                            val userMsg = ChatMessage(
                                sender = "USER",
                                text = text,
                                skillName = activeSkillToUse.title,
                                screenTag = currentScreenTag
                            )
                            localMessages.add(userMsg)

                            // Prepare conversation turns for multi-turn reasoning
                            val conversationHistory = allDisplayMessages.map { it.sender to it.text }

                            coroutineScope.launch {
                                // Persist user message to local Room database
                                repository?.addChatMessage(
                                    ChatMessageEntity(
                                        sender = "USER",
                                        text = text,
                                        skillName = activeSkillToUse.title,
                                        vehicleVin = activeVehicle,
                                        projectTitle = "$currentScreenTag: $activeProject",
                                        timestamp = System.currentTimeMillis()
                                    )
                                )

                                // Sync user message to Firestore
                                authAndSyncService?.saveChatMessageToLongTermMemory(
                                    sender = "USER",
                                    text = text,
                                    skillName = activeSkillToUse.title,
                                    vehicleVin = activeVehicle,
                                    projectTitle = "$currentScreenTag: $activeProject"
                                )

                                val mockBase64 = if (isImageAttached) "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==" else null
                                val screenContextText = "${screenContext.title} (${screenContext.specialistName})"

                                val reply = if (activeSkillToUse == AssistantSkill.COMPUTE) {
                                    com.forge.app.services.OpenAiClient.queryOpenAiAssistant(
                                        prompt = text,
                                        vehicleContext = "$activeVehicle ($activeTelemetry)",
                                        systemPrompt = "You are Team Forge High-Performance Diagnostic Engine (GPT-4o) specializing in $screenContextText."
                                    )
                                } else {
                                    GeminiClient.queryAssistantMultiTurn(
                                        prompt = text,
                                        history = conversationHistory,
                                        skill = activeSkillToUse,
                                        vehicleContext = activeVehicle,
                                        telemetryContext = activeTelemetry,
                                        projectContext = activeProject,
                                        screenContext = screenContextText,
                                        base64Image = mockBase64
                                    )
                                }

                                val aiMsg = ChatMessage(
                                    sender = "AI",
                                    text = reply,
                                    skillName = activeSkillToUse.title,
                                    screenTag = currentScreenTag
                                )
                                localMessages.add(aiMsg)

                                // Persist AI message to Room database
                                repository?.addChatMessage(
                                    ChatMessageEntity(
                                        sender = "AI",
                                        text = reply,
                                        skillName = activeSkillToUse.title,
                                        vehicleVin = activeVehicle,
                                        projectTitle = "$currentScreenTag: $activeProject",
                                        timestamp = System.currentTimeMillis()
                                    )
                                )

                                // Sync AI message to Firestore
                                authAndSyncService?.saveChatMessageToLongTermMemory(
                                    sender = "AI",
                                    text = reply,
                                    skillName = activeSkillToUse.title,
                                    vehicleVin = activeVehicle,
                                    projectTitle = "$currentScreenTag: $activeProject"
                                )

                                isLoading = false
                                isImageAttached = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    // Confirmation Dialog for Memory Reset
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Persistent Memory?", color = ForgeCyan, fontWeight = FontWeight.Bold) },
            text = { Text("This will clear your diagnostic chat history in the local Room database and trigger a Firestore memory sync.", color = ForgeOnSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository?.clearChatHistory()
                            localMessages.clear()
                            localMessages.add(
                                ChatMessage(
                                    sender = "AI",
                                    text = "### 🧹 Memory Cleared\n\nDiagnostic thread reset. New context active for **$activeVehicle** on **${screenContext.title}**.",
                                    skillName = screenContext.defaultSkill.title,
                                    screenTag = screenContext.contextTag
                                )
                            )
                            authAndSyncService?.triggerFirestoreSync()
                        }
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeRed, contentColor = Color.White)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = ForgeOnSurfaceVariant)
                }
            },
            containerColor = ForgeSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
