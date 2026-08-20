package com.forge.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.DiagnosticReportData
import com.forge.app.services.DiagnosticReportService
import com.forge.app.services.DtcInfo
import com.forge.app.services.GeminiClient
import com.forge.app.ui.components.DiagnosticReportDialog
import com.forge.app.ui.components.DtcExplanationDialog
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GuidedDiagnosticsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var dtcInput by remember { mutableStateOf("P0300") }
    var analysisResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDtcExplainerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = ForgeAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI GUIDED DIAGNOSTIC WORKFLOW",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeAmber
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { showDtcExplainerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI EXPLAINER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showReportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT REPORT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter any DTC code or symptom to generate step-by-step OEM test procedures.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Code Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = dtcInput,
                onValueChange = { dtcInput = it.uppercase() },
                label = { Text("DTC Code (e.g., P0300, P0171, P0420)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForgeAmber,
                    unfocusedBorderColor = ForgeBorder,
                    focusedContainerColor = ForgeSurface,
                    unfocusedContainerColor = ForgeSurface
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (dtcInput.isNotBlank()) {
                        isLoading = true
                        coroutineScope.launch {
                            analysisResult = GeminiClient.queryAssistant(
                                prompt = "Provide step-by-step diagnostic procedures for DTC code $dtcInput on 2021 Audi S5 Sportback."
                            )
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            }
        }

        // Fast DTC Preset Chips for 1-Tap Automation
        val presetCodes = listOf("P0300", "P0171", "P0420", "P0016", "U0100")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetCodes.forEach { code ->
                Surface(
                    onClick = {
                        dtcInput = code
                        isLoading = true
                        coroutineScope.launch {
                            analysisResult = GeminiClient.queryAssistant(
                                prompt = "Provide step-by-step OEM diagnostic test procedures, component pinouts, and expected scope waveforms for DTC code $code on 2021 Audi S5 Sportback."
                            )
                            isLoading = false
                        }
                    },
                    color = if (dtcInput == code) ForgeAmber.copy(alpha = 0.2f) else ForgeSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (dtcInput == code) ForgeAmber else ForgeBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = code,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (dtcInput == code) ForgeAmber else ForgeOnSurface,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Result Surface
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OEM TEST PROCEDURE & GUIDANCE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeCyan
                    )

                    if (analysisResult.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    val rpt = DiagnosticReportData(
                                        dtcList = listOf(DtcInfo(dtcInput, "Fault Code Analysis", "Active")),
                                        aiGuidanceSummary = analysisResult
                                    )
                                    val pdf = DiagnosticReportService.generatePdfReport(context, rpt)
                                    DiagnosticReportService.printPdfReport(context, pdf, "OEM_Procedure_$dtcInput")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Print", tint = ForgeAmber, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    DiagnosticReportService.shareTextReport(context, "OEM Diagnostic Procedure for $dtcInput:\n\n$analysisResult", "OEM Procedure $dtcInput")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = ForgeCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ForgeAmber)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Querying OEM Service Bulletins & AI Engine...", fontSize = 12.sp, color = ForgeAmber)
                        }
                    }
                } else {
                    val textToDisplay = if (analysisResult.isBlank()) {
                        "Tap search or enter a DTC code above to analyze causes, symptoms, and pinout checks."
                    } else analysisResult

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = textToDisplay,
                                fontSize = 13.sp,
                                color = ForgeOnSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        DiagnosticReportDialog(
            visible = showReportDialog,
            telemetry = com.forge.app.services.ObdTelemetryData(
                activeDtcCodes = listOf(DtcInfo(dtcInput, "Diagnostic Trouble Code Analysis", "Pending"))
            ),
            onDismiss = { showReportDialog = false }
        )

        if (showDtcExplainerDialog) {
            DtcExplanationDialog(
                visible = showDtcExplainerDialog,
                initialDtcCode = dtcInput.ifBlank { "P0300" },
                allDetectedDtcs = listOf(
                    DtcInfo("P0300", "Random/Multiple Cylinder Misfire Detected", "Stored"),
                    DtcInfo("P0171", "System Too Lean (Bank 1)", "Pending"),
                    DtcInfo("P0420", "Catalyst System Efficiency Below Threshold", "Stored")
                ),
                activeVehicleName = "2021 Audi S5 Sportback (3.0T V6)",
                onDismiss = { showDtcExplainerDialog = false }
            )
        }
    }
}

