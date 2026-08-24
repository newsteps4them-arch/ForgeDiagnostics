package com.forge.app.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.forge.app.services.DiagnosticReportData
import com.forge.app.services.DiagnosticReportService
import com.forge.app.services.ObdTelemetryData
import com.forge.app.ui.theme.*
import java.io.File

@Composable
fun DiagnosticReportDialog(
    visible: Boolean,
    telemetry: ObdTelemetryData,
    vehicleName: String = "2021 Audi S5 Sportback",
    vehicleVin: String = "WAUZZZF58MA019284",
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Visual PDF Preview, 1 = Formatted Text
    var technicianNotes by remember { mutableStateOf("Pre-repair diagnostic baseline scan completed. All sensor freeze-frame metrics within nominal baseline tolerance except bank 1 misfire DTC.") }
    var technicianName by remember { mutableStateOf("Team Forge Master Tech") }
    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    val reportData = remember(telemetry, vehicleName, vehicleVin, technicianNotes, technicianName) {
        DiagnosticReportData(
            vehicleName = vehicleName,
            vehicleVin = vehicleVin,
            telemetry = telemetry,
            dtcList = telemetry.activeDtcCodes,
            technicianNotes = technicianNotes,
            technicianName = technicianName
        )
    }

    val textReport = remember(reportData) {
        DiagnosticReportService.generateFormattedTextReport(reportData)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = ForgeSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ForgeAmber)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = ForgeAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "DIAGNOSTIC REPORT GENERATOR",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeAmber
                            )
                            Text(
                                text = "Printable PDF & Formatted Text Export",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ForgeOnSurface
                        )
                    }
                }

                // Tab Switcher (PDF Preview vs Plain Text)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ForgeBackground,
                    contentColor = ForgeAmber
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Visual PDF Layout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Subject, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Plain Text / ASCII", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Save Status Banner if saved
                AnimatedVisibility(visible = saveStatusMsg != null) {
                    saveStatusMsg?.let { msg ->
                        Surface(
                            color = ForgeGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ForgeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = msg, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeGreen)
                                }
                                IconButton(onClick = { saveStatusMsg = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss status message", tint = ForgeGreen, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Main Content Preview Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0C0F17))
                        .border(1.dp, ForgeBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    if (selectedTab == 0) {
                        // High-tech formatted PDF Document Preview
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                // PDF Header banner
                                Surface(
                                    color = Color(0xFF141722),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "TEAM FORGE MOTORSPORTS",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = ForgeAmber
                                            )
                                            Text(
                                                text = "CERTIFIED SCAN",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = ForgeCyan
                                            )
                                        }
                                        Text(
                                            text = "VEHICLE HEALTH & OBD-II SCAN REPORT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Report ID: ${reportData.reportId} • ${reportData.timestamp}",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            item {
                                // Vehicle Info Card
                                Surface(
                                    color = ForgeSurface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("1. VEHICLE & BUS INTERFACE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ForgeAmber)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Model: ${reportData.vehicleName}", fontSize = 11.sp, color = ForgeOnSurface)
                                            Text("VIN: ${reportData.vehicleVin}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Protocol: ${reportData.protocol}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan)
                                    }
                                }
                            }

                            item {
                                // DTCs List
                                Surface(
                                    color = ForgeSurface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (reportData.dtcList.isNotEmpty()) ForgeRed.copy(alpha = 0.5f) else ForgeGreen.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "2. STORED FAULT CODES (${reportData.dtcList.size})",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (reportData.dtcList.isNotEmpty()) ForgeRed else ForgeGreen
                                            )
                                            Text(
                                                text = if (reportData.dtcList.isNotEmpty()) "ATTENTION REQUIRED" else "ALL SYSTEMS OK",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (reportData.dtcList.isNotEmpty()) ForgeRed else ForgeGreen
                                            )
                                        }

                                        if (reportData.dtcList.isEmpty()) {
                                            Text("No diagnostic trouble codes found in ECU memory.", fontSize = 11.sp, color = ForgeGreen)
                                        } else {
                                            reportData.dtcList.forEach { dtc ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(ForgeRed.copy(alpha = 0.1f))
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(dtc.code, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = ForgeRed)
                                                        Text(dtc.description, fontSize = 10.sp, color = ForgeOnSurface)
                                                    }
                                                    Text(dtc.status.uppercase(), fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                // Live Sensor Telemetry Snapshot
                                Surface(
                                    color = ForgeSurface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("3. LIVE SENSOR TELEMETRY SNAPSHOT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ForgeCyan)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Engine Speed: ${reportData.telemetry.rpm} RPM", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                                            Text("Speed: ${reportData.telemetry.speedKmh} km/h", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Coolant: ${reportData.telemetry.coolantTempC}°C", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                                            Text("Intake Temp: ${reportData.telemetry.intakeAirTempC}°C", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Boost: ${reportData.telemetry.boostPressurePsi} PSI", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                                            Text("Battery: ${reportData.telemetry.batteryVoltage} VDC", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("STFT: ${reportData.telemetry.fuelTrimShortPct}%", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                                            Text("Oil Press: ${reportData.telemetry.oilPressurePsi} PSI", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                                        }
                                    }
                                }
                            }

                            item {
                                // Technician Notes Input
                                OutlinedTextField(
                                    value = technicianNotes,
                                    onValueChange = { technicianNotes = it },
                                    label = { Text("Technician & Workshop Notes", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ForgeAmber,
                                        unfocusedBorderColor = ForgeBorder,
                                        focusedContainerColor = ForgeSurface,
                                        unfocusedContainerColor = ForgeSurface
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = ForgeOnSurface)
                                )
                            }
                        }
                    } else {
                        // Monospace Raw ASCII Text View
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = textReport,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = Color(0xFFD1D5DB)
                                )
                            }
                        }
                    }
                }

                // Action Bar (Print, Share PDF, Share Text, Save Locally, Copy)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Print PDF (Native PrintManager)
                        Button(
                            onClick = {
                                val pdfFile = DiagnosticReportService.generatePdfReport(context, reportData)
                                DiagnosticReportService.printPdfReport(context, pdfFile, "Forge_Report_${reportData.reportId}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. Share PDF via Intent
                        Button(
                            onClick = {
                                val pdfFile = DiagnosticReportService.generatePdfReport(context, reportData)
                                DiagnosticReportService.shareReportFile(
                                    context = context,
                                    file = pdfFile,
                                    mimeType = "application/pdf",
                                    subject = "Diagnostic Scan Report - ${reportData.vehicleName}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 3. Save PDF Locally to Storage / Documents
                        OutlinedButton(
                            onClick = {
                                val pdfFile = DiagnosticReportService.generatePdfReport(context, reportData)
                                val savedPath = DiagnosticReportService.saveReportLocally(
                                    context = context,
                                    sourceFile = pdfFile,
                                    displayName = "${reportData.reportId}.pdf",
                                    mimeType = "application/pdf"
                                )
                                saveStatusMsg = savedPath
                                Toast.makeText(context, "PDF Report saved locally!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SaveAlt, contentDescription = "Save", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 4. Share / Export Text Report
                        OutlinedButton(
                            onClick = {
                                DiagnosticReportService.shareTextReport(
                                    context = context,
                                    text = textReport,
                                    subject = "Diagnostic Scan Text Report - ${reportData.vehicleName}"
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Share Text", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 5. Copy Text to Clipboard
                        OutlinedButton(
                            onClick = {
                                DiagnosticReportService.copyToClipboard(context, textReport)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForgeOnSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
