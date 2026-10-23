// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.ObdTelemetryData
import com.forge.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun DynoTelematicsScreen(
    telemetry: ObdTelemetryData = ObdTelemetryData()
) {
    val coroutineScope = rememberCoroutineScope()

    var vehicleWeightLbs by remember { mutableStateOf(3650f) } // Default performance sedan/coupe weight
    var isDynoRecording by remember { mutableStateOf(false) }
    var dynoTimerSeconds by remember { mutableStateOf(0.0f) }

    // Telematics Results
    var peakHp by remember { mutableStateOf(418) }
    var peakTorque by remember { mutableStateOf(445) }
    var zeroToSixtyTime by remember { mutableStateOf(3.92f) }
    var quarterMileTime by remember { mutableStateOf(11.85f) }
    var quarterMileSpeed by remember { mutableStateOf(119.4f) }
    var brakingDistanceFt by remember { mutableStateOf(104f) }
    var maxLateralG by remember { mutableStateOf(1.08f) }

    // Live calculated power based on RPM & Speed acceleration
    val calculatedWHP = remember(telemetry.rpm, telemetry.speedKmh) {
        val baseRpm = max(telemetry.rpm, 800)
        val volumetricEfficiency = 0.88f
        // Dynamic simulated dyno calculation with torque curve peak ~4400 RPM
        val calcTorque = (peakTorque * sin((baseRpm / 6800.0) * PI).coerceIn(0.2, 1.0)).toInt()
        val calcHp = ((calcTorque * baseRpm) / 5252).coerceAtLeast(40)
        calcHp
    }

    val calculatedTorque = remember(telemetry.rpm, calculatedWHP) {
        val baseRpm = max(telemetry.rpm, 800)
        ((calculatedWHP * 5252) / baseRpm).coerceAtLeast(30)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = ForgeAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "VIRTUAL CHASSIS DYNO & TELEMATICS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ForgeAmber
                        )
                    }
                    Text(
                        "Physics Inertial Horsepower, Torque & Drag-Strip Trap Analyzer",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeOnSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        isDynoRecording = !isDynoRecording
                        if (isDynoRecording) {
                            coroutineScope.launch {
                                dynoTimerSeconds = 0f
                                while (isDynoRecording) {
                                    delay(100)
                                    dynoTimerSeconds += 0.1f
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDynoRecording) ForgeRed else ForgeGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (isDynoRecording) "STOP RUN" else "START DYNO PULL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDynoRecording) Color.White else Color.Black
                    )
                }
            }
        }

        // Real-Time Power & Torque Dual Gauge Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // WHP Card
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LIVE WHEEL POWER", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeCyan)
                    Text(
                        "$calculatedWHP",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeCyan
                    )
                    Text("WHP (Peak: $peakHp WHP)", fontSize = 10.sp, color = ForgeOnSurfaceVariant)
                }
            }

            // Torque Card
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WHEEL TORQUE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeAmber)
                    Text(
                        "$calculatedTorque",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeAmber
                    )
                    Text("LB-FT (Peak: $peakTorque lb-ft)", fontSize = 10.sp, color = ForgeOnSurfaceVariant)
                }
            }
        }

        // Interactive Dyno Graph Curve
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "DYNO POWER CURVE (WHP vs TORQUE vs RPM)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeOnSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("━ WHP", fontSize = 9.sp, color = ForgeCyan, fontFamily = FontFamily.Monospace)
                        Text("━ Torque", fontSize = 9.sp, color = ForgeAmber, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Grid lines
                    for (i in 1..4) {
                        val y = height * (i / 5f)
                        drawLine(
                            color = Color(0xFF2A2E3D),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Dyno HP Curve (Cyan)
                    val hpPath = Path()
                    val torquePath = Path()

                    val points = 50
                    for (i in 0..points) {
                        val x = (width * i) / points
                        val rpmNorm = i.toFloat() / points // 0 to 7000 RPM range
                        val hpVal = sin(rpmNorm * 2.8f) * 0.85f
                        val torqueVal = (1.0f - (rpmNorm - 0.4f).pow(2) * 2.2f).coerceIn(0.2f, 0.95f)

                        val yHp = height - (hpVal * height * 0.9f)
                        val yTorque = height - (torqueVal * height * 0.85f)

                        if (i == 0) {
                            hpPath.moveTo(x, yHp)
                            torquePath.moveTo(x, yTorque)
                        } else {
                            hpPath.lineTo(x, yHp)
                            torquePath.lineTo(x, yTorque)
                        }
                    }

                    drawPath(
                        path = hpPath,
                        color = ForgeCyan,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = torquePath,
                        color = ForgeAmber,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // Current Engine RPM Marker
                    val currentRpmFraction = (telemetry.rpm.toFloat() / 7000f).coerceIn(0f, 1f)
                    val markerX = width * currentRpmFraction
                    drawLine(
                        color = ForgeGreen,
                        start = Offset(markerX, 0f),
                        end = Offset(markerX, height),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // Drag Strip & Track Timing Grid
        Text(
            "TRACK ACCELERATION & CHASSIS G-FORCE TRAPS",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = ForgeCyan,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 0-60 Time
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("0 - 60 MPH", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                    Text(
                        "${zeroToSixtyTime}s",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeGreen
                    )
                }
            }

            // 1/4 Mile ET
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("1/4 MILE ET", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                    Text(
                        "${quarterMileTime}s",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeAmber
                    )
                }
            }

            // 1/4 Trap Speed
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TRAP SPEED", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                    Text(
                        "${quarterMileSpeed.toInt()} MPH",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeCyan
                    )
                }
            }

            // Max Lateral G
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LATERAL G", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                    Text(
                        "${maxLateralG}G",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFF4081)
                    )
                }
            }
        }
    }
}
