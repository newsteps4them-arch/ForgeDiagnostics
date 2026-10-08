// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.theme.*
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

@Composable
fun OscilloscopeScreen() {
    var channel1Signal by remember { mutableStateOf("CKP Crankshaft 60-2 (5V)") }
    var channel2Signal by remember { mutableStateOf("CMP Camshaft Hall Effect") }
    var voltageScale by remember { mutableFloatStateOf(5.0f) }
    var timebaseMs by remember { mutableFloatStateOf(10.0f) }
    var isRunning by remember { mutableStateOf(true) }
    var triggerLevelVolts by remember { mutableFloatStateOf(2.5f) }
    var showPhosphorGlow by remember { mutableStateOf(true) }
    var activeChannelTab by remember { mutableIntStateOf(0) } // 0 = CH1+CH2, 1 = CH1, 2 = CH2

    // Continuous Real-Time Sweep / Phase Shift Animation
    val infiniteTransition = rememberInfiniteTransition(label = "scope_anim")
    val phaseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning) (2f * Math.PI.toFloat()) else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_offset"
    )

    // Beam sweep indicator
    val sweepXFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning) 1f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_beam"
    )

    // Live Measurements calculation
    val freqHz = (1000f / (timebaseMs * 2.5f)).toInt()
    val dutyCyclePct = 50
    val peakToPeakVolts = voltageScale * 1.8f
    val vRms = peakToPeakVolts * 0.707f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Scope Top Control Bar
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, tint = ForgeCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AUTOMOTIVE DIGITAL OSCILLOSCOPE",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan
                        )
                    }

                    // Run / Pause Button
                    Button(
                        onClick = { isRunning = !isRunning },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) ForgeGreen else ForgeAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRunning) "RUNNING" else "HOLD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Channel Quick Select
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeChannelTab == 0,
                        onClick = { activeChannelTab = 0 },
                        label = { Text("CH1 (Amber) + CH2 (Cyan)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgeAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = activeChannelTab == 1,
                        onClick = { activeChannelTab = 1 },
                        label = { Text("CH1 Only", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgeAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Oscilloscope Screen CRT Canvas
        Surface(
            color = Color(0xFF07090E),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, ForgeBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val gridCols = 10
                    val gridRows = 8

                    // Phosphor Grid Lines with Subdivision Reticle
                    for (i in 1 until gridCols) {
                        val x = w * (i.toFloat() / gridCols)
                        drawLine(
                            color = Color(0xFF161B26),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                    }
                    for (j in 1 until gridRows) {
                        val y = h * (j.toFloat() / gridRows)
                        drawLine(
                            color = Color(0xFF161B26),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    // Main Center Crosshair
                    drawLine(
                        color = Color(0xFF2C3549),
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color(0xFF2C3549),
                        start = Offset(w / 2, 0f),
                        end = Offset(w / 2, h),
                        strokeWidth = 1.5f
                    )

                    // Trigger Level Line (Dashed / Thin Red Indicator)
                    val triggerY = (h / 2) - ((triggerLevelVolts / voltageScale) * (h / 3))
                    drawLine(
                        color = ForgeRed.copy(alpha = 0.5f),
                        start = Offset(0f, triggerY),
                        end = Offset(w, triggerY),
                        strokeWidth = 1f
                    )

                    // CH1 Waveform (Amber - Crank Sensor 60-2 Square Pulse with missing teeth pattern)
                    if (activeChannelTab == 0 || activeChannelTab == 1) {
                        val pathCH1 = Path()
                        val glowPathCH1 = Path()
                        val points = 240

                        for (p in 0 until points) {
                            val x = w * (p.toFloat() / points)
                            val normalizedP = (p.toFloat() / 12f) * (10f / timebaseMs) + (if (isRunning) phaseOffset else 0f)
                            
                            // Simulate missing tooth gap every 58 pulses
                            val isMissingTooth = (normalizedP.toInt() % 16) in 14..15
                            val rawWave = if (isMissingTooth) 0f else if (sin(normalizedP.toDouble()) > 0) 1f else -1f
                            val jitter = if (isRunning) (Random.nextFloat() - 0.5f) * 0.04f else 0f
                            
                            val y = (h / 2) - ((rawWave + jitter) * (h / 3.4f) * (5f / voltageScale))
                            if (p == 0) {
                                pathCH1.moveTo(x, y)
                                glowPathCH1.moveTo(x, y)
                            } else {
                                pathCH1.lineTo(x, y)
                                glowPathCH1.lineTo(x, y)
                            }
                        }

                        // Phosphor glow layer
                        if (showPhosphorGlow) {
                            drawPath(
                                path = glowPathCH1,
                                color = ForgeAmber.copy(alpha = 0.25f),
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        // Sharp trace layer
                        drawPath(
                            path = pathCH1,
                            color = ForgeAmber,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // CH2 Waveform (Cyan - Camshaft Sine / Hall pulse)
                    if (activeChannelTab == 0) {
                        val pathCH2 = Path()
                        val points = 240

                        for (p in 0 until points) {
                            val x = w * (p.toFloat() / points)
                            val normalizedP = (p.toFloat() / 24f) * (10f / timebaseMs) + (if (isRunning) (phaseOffset * 0.5f) else 0f)
                            val sineVal = sin(normalizedP.toDouble()).toFloat()
                            val y = (h / 2) + 40.dp.toPx() - (sineVal * (h / 4.5f) * (5f / voltageScale))
                            
                            if (p == 0) pathCH2.moveTo(x, y) else pathCH2.lineTo(x, y)
                        }

                        drawPath(
                            path = pathCH2,
                            color = ForgeCyan.copy(alpha = 0.2f),
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = pathCH2,
                            color = ForgeCyan,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Sweep Scanline Beam (Moving vertical radar sweep)
                    if (isRunning) {
                        val sweepX = w * sweepXFraction
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = Offset(sweepX, 0f),
                            end = Offset(sweepX, h),
                            strokeWidth = 2f
                        )
                    }
                }

                // Top Trigger / Timebase Overlay Tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CH1: 5.0V/div (DC) • CH2: 2.0V/div (DC)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeAmber
                    )
                    Text(
                        text = "TRIG: T+ 2.50V (AUTO)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeRed
                    )
                }
            }
        }

        // Live Diagnostic Signal Telemetry Cards (Frequency, Duty Cycle, Vpp, RMS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("FREQUENCY", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedContent(
                        targetState = "${freqHz} Hz",
                        transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                        label = "freq_anim"
                    ) { targetText ->
                        Text(targetText, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = ForgeAmber)
                    }
                }
            }

            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("PEAK-TO-PEAK", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedContent(
                        targetState = String.format("%.2f V", peakToPeakVolts),
                        transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                        label = "vpp_anim"
                    ) { targetText ->
                        Text(targetText, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = ForgeCyan)
                    }
                }
            }

            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("DUTY CYCLE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${dutyCyclePct}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = ForgeGreen)
                }
            }
        }

        // Sliders for Voltage Scale & Timebase
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TIMEBASE & VOLTAGE ATTENUATION", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = ForgeAmber)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Timebase: ${timebaseMs.toInt()} ms/div", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurface)
                    Slider(
                        value = timebaseMs,
                        onValueChange = { timebaseMs = it },
                        valueRange = 2.0f..50.0f,
                        modifier = Modifier.width(180.dp),
                        colors = SliderDefaults.colors(thumbColor = ForgeCyan, activeTrackColor = ForgeCyan)
                    )
                }
            }
        }
    }
}
