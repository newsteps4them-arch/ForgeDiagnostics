package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import com.forge.app.services.ObdTelemetryData
import com.forge.app.ui.theme.*

data class PidGaugeItem(
    val name: String,
    val pid: String,
    val value: String,
    val unit: String,
    val color: Color,
    val status: String = "OK"
)

@Composable
fun LiveDataScreen(telemetry: ObdTelemetryData) {
    var isRecording by remember { mutableStateOf(false) }

    val pids = listOf(
        PidGaugeItem("Engine RPM", "01 0C", "${telemetry.rpm}", "RPM", ForgeAmber),
        PidGaugeItem("Vehicle Speed", "01 0D", "${telemetry.speedKmh}", "KM/H", ForgeCyan),
        PidGaugeItem("Engine Coolant", "01 05", "${telemetry.coolantTempC}", "°C", ForgeGreen),
        PidGaugeItem("Intake Air Temp", "01 0F", "${telemetry.intakeAirTempC}", "°C", ForgeGreen),
        PidGaugeItem("Throttle Position", "01 11", "${telemetry.throttlePosPct}", "%", ForgeAmber),
        PidGaugeItem("Boost Pressure", "01 0B", "${telemetry.boostPressurePsi}", "PSI", ForgeCyan),
        PidGaugeItem("STFT Bank 1", "01 06", "${telemetry.fuelTrimShortPct}%", "%", ForgeGreen),
        PidGaugeItem("LTFT Bank 1", "01 07", "${telemetry.fuelTrimLongPct}%", "%", ForgeGreen),
        PidGaugeItem("Battery Voltage", "01 42", "${telemetry.batteryVoltage}", "V", ForgeGreen),
        PidGaugeItem("Oil Pressure", "01 54", "${telemetry.oilPressurePsi}", "PSI", ForgeAmber)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stream Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LIVE OBD-II TELEMETRY STREAM",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForgeAmber
                )
                Text(
                    text = "ISO 15765-4 CAN • 10 PIDs Active • 100ms Refresh Rate",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { isRecording = !isRecording },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) ForgeRed else ForgeGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isRecording) "STOP LOG" else "REC LOG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(pids) { item ->
                Surface(
                    color = ForgeSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = item.pid, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(item.color.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(text = item.status, fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = item.color)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ForgeOnSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = item.value,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = item.color,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = item.unit, fontSize = 10.sp, color = item.color, modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
