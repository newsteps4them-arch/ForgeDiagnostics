package com.forge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.forge.app.services.ObdTelemetryData
import com.forge.app.ui.components.CoolantTempGauge
import com.forge.app.ui.components.ObdInstrumentCluster
import com.forge.app.ui.components.RadialGauge
import com.forge.app.ui.theme.*

data class PidGaugeItem(
    val name: String,
    val pid: String,
    val value: String,
    val unit: String,
    val color: Color,
    val status: String = "OK"
)

enum class LiveDataDisplayMode {
    GAUGE_CLUSTER,
    PID_GRID,
    ADVANCED_TELEMETRY
}

@Composable
fun LiveDataScreen(
    telemetry: ObdTelemetryData,
    onSaveSnapshot: ((ObdTelemetryData) -> Unit)? = null
) {
    var isRecording by remember { mutableStateOf(false) }
    var displayMode by remember { mutableStateOf(LiveDataDisplayMode.GAUGE_CLUSTER) }
    var peakRpmRecorded by remember { mutableStateOf(telemetry.rpm) }
    var snapshotSavedNotice by remember { mutableStateOf(false) }

    LaunchedEffect(telemetry.rpm) {
        if (telemetry.rpm > peakRpmRecorded) {
            peakRpmRecorded = telemetry.rpm
        }
    }

    val pids = listOf(
        PidGaugeItem("Engine RPM", "01 0C", "${telemetry.rpm}", "RPM", ForgeAmber),
        PidGaugeItem("Vehicle Speed", "01 0D", "${telemetry.speedKmh}", "KM/H", ForgeCyan),
        PidGaugeItem("Engine Coolant", "01 05", "${telemetry.coolantTempC}", "°C", if (telemetry.coolantTempC > 105) ForgeRed else ForgeGreen),
        PidGaugeItem("Intake Air Temp", "01 0F", "${telemetry.intakeAirTempC}", "°C", ForgeCyan),
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
            .background(ForgeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stream Header & View Switcher
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = ForgeAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE OBD-II TELEMETRY & GAUGES",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeAmber
                            )
                        }
                        Text(
                            text = "CAN Bus ISO 15765-4 • 500kbps 11-Bit • 100ms Telemetry Frame",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeOnSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { isRecording = !isRecording },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) ForgeRed else ForgeGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isRecording) "LOGGING" else "REC LOG",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Mode Selector Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = displayMode == LiveDataDisplayMode.GAUGE_CLUSTER,
                        onClick = { displayMode = LiveDataDisplayMode.GAUGE_CLUSTER },
                        label = { Text("Radial Gauges", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgeAmber,
                            selectedLabelColor = Color.Black
                        )
                    )

                    FilterChip(
                        selected = displayMode == LiveDataDisplayMode.PID_GRID,
                        onClick = { displayMode = LiveDataDisplayMode.PID_GRID },
                        label = { Text("PID Matrix", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgeAmber,
                            selectedLabelColor = Color.Black
                        )
                    )

                    FilterChip(
                        selected = displayMode == LiveDataDisplayMode.ADVANCED_TELEMETRY,
                        onClick = { displayMode = LiveDataDisplayMode.ADVANCED_TELEMETRY },
                        label = { Text("Thermal & Boost", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        leadingIcon = { Icon(Icons.Default.Thermostat, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgeAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Selected Mode Display
        when (displayMode) {
            LiveDataDisplayMode.GAUGE_CLUSTER -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ObdInstrumentCluster(telemetry = telemetry)
                    }

                    item {
                        // Boost Gauge & Fuel Trim Cluster
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadialGauge(
                                title = "Manifold Boost",
                                value = telemetry.boostPressurePsi,
                                minValue = 0f,
                                maxValue = 30f,
                                unit = "PSI",
                                primaryColor = ForgeCyan,
                                warningThreshold = 22f,
                                criticalThreshold = 26f,
                                majorTickStep = 5f,
                                multiplierDisplay = "MAP: 010B",
                                modifier = Modifier.weight(1f)
                            )

                            RadialGauge(
                                title = "Throttle Angle",
                                value = telemetry.throttlePosPct.toFloat(),
                                minValue = 0f,
                                maxValue = 100f,
                                unit = "%",
                                primaryColor = ForgeAmber,
                                warningThreshold = 85f,
                                criticalThreshold = 95f,
                                majorTickStep = 20f,
                                multiplierDisplay = "TPS: 0111",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        // Telemetry Highlights Footer
                        Surface(
                            color = ForgeSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("PEAK RPM RECORDED", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                                    Text("$peakRpmRecorded RPM", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = ForgeAmber)
                                }
                                Column {
                                    Text("OIL PRESSURE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                                    Text("${telemetry.oilPressurePsi} PSI", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = ForgeGreen)
                                }
                                Column {
                                    Text("INTAKE AIR", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                                    Text("${telemetry.intakeAirTempC}°C", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = ForgeCyan)
                                }
                            }
                        }
                    }
                }
            }

            LiveDataDisplayMode.PID_GRID -> {
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

            LiveDataDisplayMode.ADVANCED_TELEMETRY -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CoolantTempGauge(coolantTempC = telemetry.coolantTempC)
                    }

                    item {
                        Surface(
                            color = ForgeSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "CLOSED LOOP FUEL SYSTEM TELEMETRY",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeGreen
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = ForgeSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("SHORT TERM TRIM (STFT)", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                                            Text(
                                                "${telemetry.fuelTrimShortPct}%",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (kotlin.math.abs(telemetry.fuelTrimShortPct) > 10f) ForgeAmber else ForgeGreen
                                            )
                                        }
                                    }

                                    Surface(
                                        color = ForgeSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("LONG TERM TRIM (LTFT)", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                                            Text(
                                                "${telemetry.fuelTrimLongPct}%",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (kotlin.math.abs(telemetry.fuelTrimLongPct) > 10f) ForgeAmber else ForgeGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
