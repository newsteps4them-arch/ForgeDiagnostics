package com.forge.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.theme.*
import kotlin.math.sin

@Composable
fun OscilloscopeScreen() {
    var channel1Signal by remember { mutableStateOf("CKP Crankshaft (5V Square)") }
    var voltageScale by remember { mutableFloatStateOf(5.0f) }
    var timebaseMs by remember { mutableFloatStateOf(10.0f) }

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
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, tint = ForgeCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DIGITAL OSCILLOSCOPE (DUAL CHANNEL)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeCyan
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CH1: $channel1Signal • Scale: ${voltageScale}V/div • Timebase: ${timebaseMs}ms/div",
                    fontSize = 11.sp,
                    color = ForgeAmber
                )
            }
        }

        // Oscilloscope Screen Display (Canvas)
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, ForgeBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val gridCols = 10
                    val gridRows = 8

                    // Draw Oscilloscope Grid Lines
                    for (i in 1 until gridCols) {
                        val x = w * (i.toFloat() / gridCols)
                        drawLine(
                            color = Color(0xFF222222),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                    }
                    for (j in 1 until gridRows) {
                        val y = h * (j.toFloat() / gridRows)
                        drawLine(
                            color = Color(0xFF222222),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    // Center Axes
                    drawLine(
                        color = Color(0xFF444444),
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 1.5f
                    )

                    // Draw Waveform CH1 (Yellow/Amber)
                    val path1 = Path()
                    val points = 200
                    for (p in 0 until points) {
                        val x = w * (p.toFloat() / points)
                        val angle = (p.toFloat() / 15f) * (10f / timebaseMs)
                        val sine = sin(angle.toDouble()).toFloat()
                        val squareVal = if (sine > 0) 0.8f else -0.8f
                        val y = (h / 2) - (squareVal * (h / 3) * (5f / voltageScale))
                        if (p == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
                    }
                    drawPath(
                        path = path1,
                        color = ForgeAmber,
                        style = Stroke(width = 3f)
                    )

                    // Draw Waveform CH2 (Cyan Sine Wave)
                    val path2 = Path()
                    for (p in 0 until points) {
                        val x = w * (p.toFloat() / points)
                        val angle = (p.toFloat() / 20f) * (10f / timebaseMs)
                        val sine = sin(angle.toDouble()).toFloat()
                        val y = (h / 2) - (sine * (h / 4) * (5f / voltageScale))
                        if (p == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
                    }
                    drawPath(
                        path = path2,
                        color = ForgeCyan,
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Controls
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SCOPE CONTROL PANEL", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ForgeAmber, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Voltage Scale: ${voltageScale}V/div", fontSize = 12.sp, color = ForgeOnSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { voltageScale = (voltageScale - 1f).coerceAtLeast(1f) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceVariant)
                        ) { Text("-") }
                        Button(
                            onClick = { voltageScale = (voltageScale + 1f).coerceAtMost(20f) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceVariant)
                        ) { Text("+") }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Timebase: ${timebaseMs}ms/div", fontSize = 12.sp, color = ForgeOnSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { timebaseMs = (timebaseMs - 2f).coerceAtLeast(2f) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceVariant)
                        ) { Text("-") }
                        Button(
                            onClick = { timebaseMs = (timebaseMs + 2f).coerceAtMost(50f) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeSurfaceVariant)
                        ) { Text("+") }
                    }
                }
            }
        }
    }
}
