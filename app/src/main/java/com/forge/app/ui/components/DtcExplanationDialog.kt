// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.forge.app.services.*
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

enum class DtcExplainerTab(val title: String, val icon: @Composable () -> Unit) {
    PLAIN_ENGLISH("Plain English", { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp)) }),
    TECHNICAL_SPEC("Technical Spec", { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp)) }),
    COMBINED_AUDIT("Combined Audit", { Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(14.dp)) })
}

/**
 * High-performance, human-readable OBD-II Diagnostic Trouble Code Explainer Dialog powered by Gemini API
 */
@Composable
fun DtcExplanationDialog(
    visible: Boolean,
    initialDtcCode: String? = null,
    allDetectedDtcs: List<DtcInfo> = emptyList(),
    activeVehicleName: String = "2021 Audi S5 Sportback (3.0T V6)",
    activeTelemetry: ObdTelemetryData? = null,
    onDismiss: () -> Unit,
    onSendToOpenManus: ((String) -> Unit)? = null,
    onAddTaskToWorkOrder: ((String, String) -> Unit)? = null
) {
    if (!visible) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(DtcExplainerTab.PLAIN_ENGLISH) }
    var selectedCode by remember {
        mutableStateOf(
            initialDtcCode ?: allDetectedDtcs.firstOrNull()?.code ?: "P0300"
        )
    }
    var customCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var explanation by remember { mutableStateOf<DtcExplanation?>(null) }
    var combinedAuditText by remember { mutableStateOf<String?>(null) }
    var isCombinedMode by remember { mutableStateOf(false) }

    // Telemetry summary string
    val telemetrySummary = remember(activeTelemetry) {
        if (activeTelemetry != null) {
            "RPM: ${activeTelemetry.rpm} | ECT: ${activeTelemetry.coolantTempC}°C | STFT: ${activeTelemetry.fuelTrimShortPct}% | LTFT: ${activeTelemetry.fuelTrimLongPct}% | Boost: ${activeTelemetry.boostPressurePsi} PSI"
        } else {
            "RPM: 2,450 | ECT: 92°C | STFT: +14.2% | Boost: 1.2 bar"
        }
    }

    // Function to load single DTC explanation
    fun loadExplanation(code: String) {
        isCombinedMode = false
        isLoading = true
        coroutineScope.launch {
            try {
                val result = DtcExplanationService.explainDtc(
                    dtcCode = code,
                    vehicleContext = activeVehicleName,
                    telemetryContext = telemetrySummary
                )
                explanation = result
            } catch (e: Exception) {
                // error handled in service
            } finally {
                isLoading = false
            }
        }
    }

    // Function to load combined audit for all codes
    fun loadCombinedAudit() {
        isCombinedMode = true
        activeTab = DtcExplainerTab.COMBINED_AUDIT
        isLoading = true
        coroutineScope.launch {
            try {
                val result = DtcExplanationService.explainMultipleDtcs(
                    dtcs = allDetectedDtcs,
                    vehicleContext = activeVehicleName,
                    telemetryContext = telemetrySummary
                )
                combinedAuditText = result
            } catch (e: Exception) {
                combinedAuditText = "Error generating combined audit."
            } finally {
                isLoading = false
            }
        }
    }

    // Initial load trigger
    LaunchedEffect(selectedCode, initialDtcCode) {
        loadExplanation(selectedCode)
    }

    val quickCodes = listOf("P0300", "P0171", "P0420", "P0128", "P0016", "P0700", "P0442")

    // Pulsing transition for live Gemini streaming badge
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gemini_pulse_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            color = ForgeSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ForgeAmber.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
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
                                .background(
                                    Brush.linearGradient(listOf(ForgeAmber.copy(alpha = 0.3f), ForgeCyan.copy(alpha = 0.3f))),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ForgeAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "GEMINI OBD-II CODE EXPLAINER",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    color = ForgeAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, ForgeAmber.copy(alpha = pulseAlpha))
                                ) {
                                    Text(
                                        text = "AI 3.5 FLASH",
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeAmber,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Human-Readable Diagnostic Translation for $activeVehicleName",
                                fontSize = 10.5.sp,
                                color = ForgeOnSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ForgeOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detected Code Selector Bar & Custom Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECT DETECTED CODE OR ENTER ANY OBD-II CODE:",
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Detected codes from active vehicle
                        items(allDetectedDtcs) { dtc ->
                            val isSelected = !isCombinedMode && selectedCode.equals(dtc.code, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCode = dtc.code
                                    loadExplanation(dtc.code)
                                },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(ForgeRed)
                                        )
                                        Text(
                                            text = dtc.code,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForgeRed,
                                    selectedLabelColor = Color.White,
                                    containerColor = ForgeBackground,
                                    labelColor = ForgeRed
                                ),
                                border = BorderStroke(1.dp, if (isSelected) ForgeRed else ForgeRed.copy(alpha = 0.4f))
                            )
                        }

                        // Combined Audit Button if multiple codes exist
                        if (allDetectedDtcs.size > 1) {
                            item {
                                FilterChip(
                                    selected = isCombinedMode,
                                    onClick = { loadCombinedAudit() },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Hub,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isCombinedMode) Color.Black else ForgeCyan
                                            )
                                            Text(
                                                text = "COMBINED AUDIT (${allDetectedDtcs.size} CODES)",
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ForgeCyan,
                                        selectedLabelColor = Color.Black,
                                        containerColor = ForgeBackground,
                                        labelColor = ForgeCyan
                                    ),
                                    border = BorderStroke(1.dp, if (isCombinedMode) ForgeCyan else ForgeCyan.copy(alpha = 0.4f))
                                )
                            }
                        }

                        // Quick popular codes
                        items(quickCodes.filter { q -> allDetectedDtcs.none { it.code.equals(q, ignoreCase = true) } }) { code ->
                            val isSelected = !isCombinedMode && selectedCode.equals(code, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCode = code
                                    loadExplanation(code)
                                },
                                label = {
                                    Text(
                                        text = code,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForgeAmber,
                                    selectedLabelColor = Color.Black,
                                    containerColor = ForgeBackground,
                                    labelColor = ForgeOnSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, if (isSelected) ForgeAmber else ForgeBorder)
                            )
                        }
                    }

                    // Quick custom code input field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = customCodeInput,
                            onValueChange = { customCodeInput = it.uppercase() },
                            placeholder = { Text("Enter custom DTC (e.g. P0455, C0035, U0100)", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForgeAmber,
                                unfocusedBorderColor = ForgeBorder,
                                focusedContainerColor = ForgeBackground,
                                unfocusedContainerColor = ForgeBackground
                            )
                        )

                        Button(
                            onClick = {
                                if (customCodeInput.isNotBlank()) {
                                    selectedCode = customCodeInput.trim()
                                    loadExplanation(selectedCode)
                                }
                            },
                            enabled = customCodeInput.isNotBlank() && !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPLAIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = ForgeBackground,
                    contentColor = ForgeAmber,
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, ForgeBorder, RoundedCornerShape(8.dp))
                ) {
                    DtcExplainerTab.values().forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = {
                                activeTab = tab
                                if (tab == DtcExplainerTab.COMBINED_AUDIT && combinedAuditText == null) {
                                    loadCombinedAudit()
                                }
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    tab.icon()
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            selectedContentColor = ForgeAmber,
                            unselectedContentColor = ForgeOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content View Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = ForgeAmber,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Querying Gemini API (gemini-3.5-flash)...",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeAmber
                            )
                            Text(
                                text = "Translating $selectedCode into human-readable diagnostic analysis...",
                                fontSize = 11.sp,
                                color = ForgeOnSurfaceVariant
                            )
                        }
                    } else if (isCombinedMode || activeTab == DtcExplainerTab.COMBINED_AUDIT) {
                        // Multi-DTC Combined Audit View
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Surface(
                                    color = ForgeBackground,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ForgeCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                                Icon(Icons.Default.Hub, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "MULTI-CODE CORRELATION & INTERACTION AUDIT",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForgeCyan
                                                )
                                            }
                                        }

                                        Text(
                                            text = combinedAuditText ?: "Generating combined analysis...",
                                            fontSize = 12.sp,
                                            color = ForgeOnSurface,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else if (explanation != null) {
                        val expl = explanation!!

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Primary Code & Severity Banner
                            item {
                                Surface(
                                    color = Color(expl.severity.badgeColorHex).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(expl.severity.badgeColorHex).copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = expl.code,
                                                    fontSize = 20.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(expl.severity.badgeColorHex)
                                                )
                                                Surface(
                                                    color = Color(expl.severity.badgeColorHex),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = expl.severity.label,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = expl.systemCategory,
                                                fontSize = 9.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = ForgeOnSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = expl.standardTitle,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            if (activeTab == DtcExplainerTab.PLAIN_ENGLISH) {
                                // Layman's Terms Summary Card
                                item {
                                    Surface(
                                        color = ForgeBackground,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, ForgeAmber.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "WHAT THIS MEANS IN PLAIN ENGLISH",
                                                    fontSize = 10.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForgeAmber
                                                )
                                            }

                                            Text(
                                                text = expl.laymanSummary,
                                                fontSize = 12.5.sp,
                                                color = ForgeOnSurface,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }

                                // Is it Safe to Drive? Callout
                                item {
                                    val isSafe = expl.isSafeToDrive.startsWith("YES", ignoreCase = true)
                                    val isCaution = expl.isSafeToDrive.startsWith("CAUTION", ignoreCase = true)
                                    val safeColor = if (isSafe) ForgeGreen else if (isCaution) ForgeAmber else ForgeRed

                                    Surface(
                                        color = safeColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, safeColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = safeColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "IS IT SAFE TO DRIVE?",
                                                    fontSize = 10.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = safeColor
                                                )
                                            }

                                            Text(
                                                text = expl.isSafeToDrive,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = safeColor
                                            )
                                            Text(
                                                text = expl.safeToDriveReason,
                                                fontSize = 11.5.sp,
                                                color = ForgeOnSurfaceVariant,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }

                                // Top Probable Causes with Likelihood Percentages
                                item {
                                    Surface(
                                        color = ForgeBackground,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, ForgeBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "TOP PROBABLE CAUSES & LIKELIHOOD",
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ForgeCyan
                                            )

                                            expl.probableCauses.forEach { cause ->
                                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = cause.title,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = "${cause.probabilityPct}%",
                                                            fontSize = 11.5.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ForgeCyan
                                                        )
                                                    }

                                                    // Probability Progress Bar
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(4.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(ForgeBorder)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxHeight()
                                                                .fillMaxWidth(cause.probabilityPct / 100f)
                                                                .background(ForgeCyan)
                                                        )
                                                    }

                                                    Text(
                                                        text = cause.explanation,
                                                        fontSize = 10.sp,
                                                        color = ForgeOnSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Symptoms & Financial Cost Estimate
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = ForgeBackground,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, ForgeBorder),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "ESTIMATED REPAIR",
                                                    fontSize = 9.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForgeGreen
                                                )
                                                Text(
                                                    text = expl.estimatedRepairCostRange,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "DIY: ${expl.diyDifficulty}",
                                                    fontSize = 9.sp,
                                                    color = ForgeOnSurfaceVariant
                                                )
                                            }
                                        }

                                        Surface(
                                            color = ForgeBackground,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, ForgeBorder),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "VERIFIED SOURCE",
                                                    fontSize = 9.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForgeAmber
                                                )
                                                Text(
                                                    text = expl.verifiedOemSource,
                                                    fontSize = 10.sp,
                                                    color = ForgeOnSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                // Common Symptoms List
                                item {
                                    Surface(
                                        color = ForgeBackground,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, ForgeBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "COMMON DRIVER SYMPTOMS",
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ForgeAmber
                                            )

                                            expl.commonSymptoms.forEach { symptom ->
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.FiberManualRecord,
                                                        contentDescription = null,
                                                        tint = ForgeAmber,
                                                        modifier = Modifier
                                                            .padding(top = 4.dp)
                                                            .size(6.dp)
                                                    )
                                                    Text(
                                                        text = symptom,
                                                        fontSize = 11.5.sp,
                                                        color = ForgeOnSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Technical Mechanic Specification Tab
                                item {
                                    Surface(
                                        color = ForgeBackground,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, ForgeBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "FACTORY STEP-BY-STEP DIAGNOSTIC PROCEDURES",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ForgeCyan
                                            )

                                            expl.diagnosticSteps.forEachIndexed { index, step ->
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        color = ForgeCyan.copy(alpha = 0.2f),
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "${index + 1}",
                                                                fontSize = 10.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ForgeCyan
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = step,
                                                        fontSize = 11.5.sp,
                                                        color = ForgeOnSurface,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!expl.technicalDetails.isNullOrBlank()) {
                                    item {
                                        Surface(
                                            color = ForgeBackground,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, ForgeBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "ECU TRIGGER THRESHOLDS & PINOUT LOGIC",
                                                    fontSize = 10.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForgeAmber
                                                )
                                                Text(
                                                    text = expl.technicalDetails ?: "",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = ForgeOnSurfaceVariant,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!expl.rawGeminiAnalysis.isNullOrBlank()) {
                                    item {
                                        Surface(
                                            color = ForgeBackground,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, ForgeBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "GEMINI 3.5 FLASH REASONING DUMP",
                                                    fontSize = 10.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForgeGreen
                                                )
                                                Text(
                                                    text = expl.rawGeminiAnalysis ?: "",
                                                    fontSize = 11.sp,
                                                    color = ForgeOnSurface,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy Explanation
                    OutlinedButton(
                        onClick = {
                            val textToCopy = explanation?.let { expl ->
                                "${expl.code}: ${expl.standardTitle}\nSeverity: ${expl.severity.label}\n\nSummary:\n${expl.laymanSummary}\n\nSafe to Drive: ${expl.isSafeToDrive} - ${expl.safeToDriveReason}\n\nRepair Estimate: ${expl.estimatedRepairCostRange}"
                            } ?: combinedAuditText ?: ""

                            clipboardManager.setText(AnnotatedString(textToCopy))
                            Toast.makeText(context, "DTC explanation copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeOnSurface),
                        border = BorderStroke(1.dp, ForgeBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Add Task to Work Order
                    if (onAddTaskToWorkOrder != null && explanation != null) {
                        OutlinedButton(
                            onClick = {
                                explanation?.let { expl ->
                                    onAddTaskToWorkOrder(
                                        "Diagnose & Repair ${expl.code} (${expl.standardTitle})",
                                        "Perform factory test: ${expl.diagnosticSteps.firstOrNull() ?: expl.laymanSummary}"
                                    )
                                    Toast.makeText(context, "Added ${expl.code} task to active Work Order", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeCyan),
                            border = BorderStroke(1.dp, ForgeCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD TO WORK ORDER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Send to OpenManus Agent
                    Button(
                        onClick = {
                            val goal = if (isCombinedMode) {
                                "Perform autonomous multi-fault triage and repair plan for active DTCs: ${allDetectedDtcs.joinToString { it.code }}"
                            } else {
                                "Execute autonomous diagnostic isolation and repair workflow for DTC code $selectedCode on $activeVehicleName"
                            }
                            onSendToOpenManus?.invoke(goal)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AUTONOMOUS AGENT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
