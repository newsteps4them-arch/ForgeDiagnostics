package com.forge.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.forge.app.services.*
import com.forge.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Diagnostic Dashboard UI Component that visualizes real-time OBD-II sensor data streams
 * retrieved by the OpenManus module and hardware adapters.
 */
@Composable
fun OpenManusTelemetryDashboard(
    telemetry: ObdTelemetryData,
    hardwareModule: ObdDiagnosticHardwareModule? = null,
    openManusService: OpenManusAgentService? = null,
    activeVehicleName: String = "2021 Audi S5 Sportback",
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isStreamPaused by remember { mutableStateOf(false) }
    var selectedUnitSystem by remember { mutableStateOf(UnitSystem.IMPERIAL) }
    var selectedGraphPid by remember { mutableStateOf(GraphPidChannel.RPM) }
    var isExpandedWaveform by remember { mutableStateOf(true) }
    var showDtcExplainerDialog by remember { mutableStateOf(false) }
    var selectedDtcCodeForExplainer by remember { mutableStateOf<String?>(null) }

    // Rolling telemetry history for sparkline waveform
    val rpmHistory = remember { mutableStateListOf<Float>() }
    val boostHistory = remember { mutableStateListOf<Float>() }
    val fuelTrimHistory = remember { mutableStateListOf<Float>() }
    val coolantHistory = remember { mutableStateListOf<Float>() }

    // Capture rolling data points when unpaused
    LaunchedEffect(telemetry.rpm, isStreamPaused) {
        if (!isStreamPaused) {
            rpmHistory.add(telemetry.rpm.toFloat())
            if (rpmHistory.size > 40) rpmHistory.removeAt(0)

            boostHistory.add(telemetry.boostPressurePsi)
            if (boostHistory.size > 40) boostHistory.removeAt(0)

            fuelTrimHistory.add(telemetry.fuelTrimShortPct)
            if (fuelTrimHistory.size > 40) fuelTrimHistory.removeAt(0)

            coolantHistory.add(telemetry.coolantTempC.toFloat())
            if (coolantHistory.size > 40) coolantHistory.removeAt(0)
        }
    }

    Surface(
        color = ForgeSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ForgeGreen.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Live OBD Stream Title & Diagnostic Controls
            val infiniteHeaderTransition = rememberInfiniteTransition(label = "header_stream_pulse")
            val headerPulseAlpha by infiniteHeaderTransition.animateFloat(
                initialValue = if (isStreamPaused) 1.0f else 0.4f,
                targetValue = if (isStreamPaused) 1.0f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "header_pulse_alpha"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                ForgeGreen.copy(alpha = if (isStreamPaused) 0.15f else 0.12f + (headerPulseAlpha * 0.15f)),
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = if (isStreamPaused) ForgeAmber else ForgeGreen.copy(alpha = headerPulseAlpha),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "LIVE OBD-II SENSOR STREAMS",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeGreen,
                                letterSpacing = 0.5.sp
                            )
                            if (!isStreamPaused) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ForgeGreen.copy(alpha = headerPulseAlpha))
                                )
                            }
                        }
                        Text(
                            text = "OpenManus Hardware Ingestion Pipeline • ${if (isStreamPaused) "PAUSED / FROZEN" else "STREAMING (LIVE)"}",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isStreamPaused) ForgeAmber else ForgeGreen.copy(alpha = 0.9f)
                        )
                    }
                }

                // Controls: Unit Toggle & Stream Pause
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Unit Toggle (Imperial / Metric)
                    Surface(
                        color = ForgeSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, ForgeBorder),
                        modifier = Modifier.clickable {
                            selectedUnitSystem = if (selectedUnitSystem == UnitSystem.IMPERIAL) UnitSystem.METRIC else UnitSystem.IMPERIAL
                        }
                    ) {
                        Text(
                            text = if (selectedUnitSystem == UnitSystem.IMPERIAL) "°F / PSI / MPH" else "°C / BAR / KMH",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Pause / Freeze Stream Button
                    IconButton(
                        onClick = { isStreamPaused = !isStreamPaused },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isStreamPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Freeze Stream",
                            tint = if (isStreamPaused) ForgeGreen else ForgeAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Real-Time Sensor Grid (Key OBD-II Mode 01 PIDs)
            val isStreaming = !isStreamPaused

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Engine Speed (RPM) & Vehicle Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // RPM Sensor Card
                    SensorGaugeCard(
                        title = "ENGINE SPEED (PID 010C)",
                        valueText = "${telemetry.rpm}",
                        unitText = "RPM",
                        progress = (telemetry.rpm / 7500f).coerceIn(0f, 1f),
                        accentColor = if (telemetry.rpm > 6200) ForgeRed else ForgeGreen,
                        statusText = if (telemetry.rpm > 6200) "HIGH LOAD" else if (telemetry.rpm > 900) "CRUISE" else "IDLE",
                        isLiveStreaming = isStreaming,
                        modifier = Modifier.weight(1f)
                    )

                    // Vehicle Speed Card
                    val speedDisplay = if (selectedUnitSystem == UnitSystem.IMPERIAL) {
                        (telemetry.speedKmh * 0.621371f).toInt()
                    } else {
                        telemetry.speedKmh
                    }
                    val speedUnit = if (selectedUnitSystem == UnitSystem.IMPERIAL) "MPH" else "KM/H"
                    SensorGaugeCard(
                        title = "VEHICLE SPEED (PID 010D)",
                        valueText = "$speedDisplay",
                        unitText = speedUnit,
                        progress = (telemetry.speedKmh / 240f).coerceIn(0f, 1f),
                        accentColor = ForgeCyan,
                        statusText = if (telemetry.speedKmh == 0) "STATIONARY" else "MOTION",
                        isLiveStreaming = isStreaming,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Coolant Temp (ECT) & Intake Air Temp (IAT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ectDisplay = if (selectedUnitSystem == UnitSystem.IMPERIAL) {
                        (telemetry.coolantTempC * 1.8f + 32).toInt()
                    } else {
                        telemetry.coolantTempC
                    }
                    val ectUnit = if (selectedUnitSystem == UnitSystem.IMPERIAL) "°F" else "°C"
                    val isEctHot = telemetry.coolantTempC > 102
                    val isEctCold = telemetry.coolantTempC < 70

                    SensorGaugeCard(
                        title = "COOLANT TEMP (PID 0105)",
                        valueText = "$ectDisplay",
                        unitText = ectUnit,
                        progress = ((telemetry.coolantTempC - 40f) / 80f).coerceIn(0f, 1f),
                        accentColor = if (isEctHot) ForgeRed else if (isEctCold) ForgeCyan else ForgeGreen,
                        statusText = if (isEctHot) "OVERHEAT WARN" else if (isEctCold) "WARMING UP" else "NOMINAL (90°C)",
                        isLiveStreaming = isStreaming,
                        modifier = Modifier.weight(1f)
                    )

                    val iatDisplay = if (selectedUnitSystem == UnitSystem.IMPERIAL) {
                        (telemetry.intakeAirTempC * 1.8f + 32).toInt()
                    } else {
                        telemetry.intakeAirTempC
                    }
                    val iatUnit = if (selectedUnitSystem == UnitSystem.IMPERIAL) "°F" else "°C"
                    SensorGaugeCard(
                        title = "INTAKE AIR (PID 010F)",
                        valueText = "$iatDisplay",
                        unitText = iatUnit,
                        progress = ((telemetry.intakeAirTempC) / 60f).coerceIn(0f, 1f),
                        accentColor = ForgeAmber,
                        statusText = "AMBIENT + ${(telemetry.intakeAirTempC - 20).coerceAtLeast(0)}°",
                        isLiveStreaming = isStreaming,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Fuel Trims (STFT & LTFT) with Lean/Rich Bi-Directional Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stft = telemetry.fuelTrimShortPct
                    val isStftLean = stft > 10.0f
                    val isStftRich = stft < -10.0f

                    BiDirectionalTrimCard(
                        title = "SHORT FUEL TRIM (STFT)",
                        pidCode = "PID 0106",
                        trimPct = stft,
                        isAlert = isStftLean || isStftRich,
                        isLiveStreaming = isStreaming,
                        modifier = Modifier.weight(1f)
                    )

                    val ltft = telemetry.fuelTrimLongPct
                    val isLtftLean = ltft > 10.0f
                    val isLtftRich = ltft < -10.0f

                    BiDirectionalTrimCard(
                        title = "LONG FUEL TRIM (LTFT)",
                        pidCode = "PID 0107",
                        trimPct = ltft,
                        isAlert = isLtftLean || isLtftRich,
                        isLiveStreaming = isStreaming,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Boost/Vacuum, Throttle Position (TPS), Battery Voltage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val boostVal = if (selectedUnitSystem == UnitSystem.IMPERIAL) {
                        "%.1f".format(telemetry.boostPressurePsi)
                    } else {
                        "%.2f".format(telemetry.boostPressurePsi * 0.0689476f)
                    }
                    val boostUnit = if (selectedUnitSystem == UnitSystem.IMPERIAL) "PSI" else "BAR"

                    SensorGaugeCard(
                        title = "BOOST / VAC (PID 010B)",
                        valueText = boostVal,
                        unitText = boostUnit,
                        progress = ((telemetry.boostPressurePsi + 14.7f) / 35f).coerceIn(0f, 1f),
                        accentColor = ForgeCyan,
                        statusText = if (telemetry.boostPressurePsi > 0.5f) "UNDER BOOST" else "MANIFOLD VAC",
                        isLiveStreaming = isStreaming,
                        modifier = Modifier.weight(1f)
                    )

                    SensorGaugeCard(
                        title = "THROTTLE (PID 0111)",
                        valueText = "${telemetry.throttlePosPct}",
                        unitText = "%",
                        progress = (telemetry.throttlePosPct / 100f).coerceIn(0f, 1f),
                        accentColor = ForgeGreen,
                        statusText = if (telemetry.throttlePosPct > 70) "WOT" else "PARTIAL",
                        isLiveStreaming = isStreaming,
                        modifier = Modifier.weight(1f)
                    )

                    val battVolt = telemetry.batteryVoltage
                    val isBattLow = battVolt < 12.4f
                    SensorGaugeCard(
                        title = "BATTERY VOLTS (PID 0142)",
                        valueText = "%.1f".format(battVolt),
                        unitText = "V",
                        progress = ((battVolt - 10f) / 5f).coerceIn(0f, 1f),
                        accentColor = if (isBattLow) ForgeRed else ForgeAmber,
                        statusText = if (battVolt > 13.5f) "CHARGING" else if (isBattLow) "LOW BATT" else "RESTING",
                        isLiveStreaming = isStreaming,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Real-Time Oscilloscope Sparkline Waveform Visualizer
            Surface(
                color = ForgeBackground,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = ForgeCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "REAL-TIME TELEMETRY WAVEFORM PLOT",
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeCyan
                            )
                        }

                        // PID Stream Selector
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            GraphPidChannel.values().forEach { channel ->
                                val isSelected = selectedGraphPid == channel
                                Surface(
                                    color = if (isSelected) ForgeCyan.copy(alpha = 0.2f) else ForgeSurfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, if (isSelected) ForgeCyan else ForgeBorder),
                                    modifier = Modifier.clickable { selectedGraphPid = channel }
                                ) {
                                    Text(
                                        text = channel.displayName,
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) ForgeCyan else ForgeOnSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Canvas Waveform Render
                    val currentHistory = when (selectedGraphPid) {
                        GraphPidChannel.RPM -> rpmHistory
                        GraphPidChannel.BOOST -> boostHistory
                        GraphPidChannel.FUEL_TRIM -> fuelTrimHistory
                        GraphPidChannel.COOLANT -> coolantHistory
                    }

                    val graphColor = when (selectedGraphPid) {
                        GraphPidChannel.RPM -> ForgeGreen
                        GraphPidChannel.BOOST -> ForgeCyan
                        GraphPidChannel.FUEL_TRIM -> ForgeAmber
                        GraphPidChannel.COOLANT -> ForgeRed
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .background(Color(0xFF070B0E), RoundedCornerShape(4.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp)) {
                            val width = size.width
                            val height = size.height

                            // Draw subtle gridlines
                            val gridColor = Color(0xFF1B2A32)
                            drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = 1f)
                            drawLine(gridColor, Offset(0f, height * 0.5f), Offset(width, height * 0.5f), strokeWidth = 1f)
                            drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = 1f)

                            if (currentHistory.size >= 2) {
                                val minVal = currentHistory.minOrNull() ?: 0f
                                val maxVal = (currentHistory.maxOrNull() ?: 1f).coerceAtLeast(minVal + 1f)
                                val path = Path()
                                val stepX = width / (currentHistory.size - 1)

                                currentHistory.forEachIndexed { index, value ->
                                    val norm = (value - minVal) / (maxVal - minVal)
                                    val x = index * stepX
                                    val y = height - (norm * (height - 8f) + 4f)
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }

                                drawPath(
                                    path = path,
                                    color = graphColor,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Overlay Stats
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val minVal = currentHistory.minOrNull() ?: 0f
                            val maxVal = currentHistory.maxOrNull() ?: 0f
                            val latestVal = currentHistory.lastOrNull() ?: 0f

                            Text(
                                text = "MIN: ${"%.1f".format(minVal)}",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeOnSurfaceVariant
                            )
                            Text(
                                text = "LIVE: ${"%.1f".format(latestVal)}",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = graphColor
                            )
                            Text(
                                text = "MAX: ${"%.1f".format(maxVal)}",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Correlated Fault Codes Quick-Bridge to OpenManus Agent
            if (telemetry.activeDtcCodes.isNotEmpty()) {
                Surface(
                    color = ForgeAmber.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ForgeAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ForgeAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "CORRELATED DTC FAULTS (${telemetry.activeDtcCodes.size})",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeAmber
                                    )
                                    Text(
                                        text = "Sensor telemetry (LTFT ${telemetry.fuelTrimLongPct}%, ECT ${telemetry.coolantTempC}°C) correlated with active DTCs.",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ForgeOnSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Gemini AI Explain Button
                                Button(
                                    onClick = {
                                        selectedDtcCodeForExplainer = telemetry.activeDtcCodes.firstOrNull()?.code
                                        showDtcExplainerDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "AI EXPLAIN",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val dtcs = telemetry.activeDtcCodes.map { it.code }
                                            val telemetrySummary = "RPM=${telemetry.rpm}, Speed=${telemetry.speedKmh}km/h, ECT=${telemetry.coolantTempC}C, IAT=${telemetry.intakeAirTempC}C, Boost=${telemetry.boostPressurePsi}psi, STFT=${telemetry.fuelTrimShortPct}%, LTFT=${telemetry.fuelTrimLongPct}%, Volt=${telemetry.batteryVoltage}V, DTCs=${dtcs.joinToString()}"
                                            openManusService?.runAutonomousDiagnosis(
                                                goal = "Real-time snapshot diagnostic audit for active codes [${dtcs.joinToString()}]",
                                                vehicleContext = activeVehicleName,
                                                activeDtcs = dtcs,
                                                telemetrySummary = telemetrySummary
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "ANALYZE",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Interactive individual DTC tags that open the Gemini Explainer directly
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            telemetry.activeDtcCodes.forEach { dtc ->
                                Surface(
                                    color = ForgeRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, ForgeRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        selectedDtcCodeForExplainer = dtc.code
                                        showDtcExplainerDialog = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = dtc.code,
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = ForgeRed
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Explain ${dtc.code}",
                                            tint = ForgeAmber,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Render Gemini DTC Explainer Dialog
        if (showDtcExplainerDialog) {
            DtcExplanationDialog(
                visible = showDtcExplainerDialog,
                initialDtcCode = selectedDtcCodeForExplainer,
                allDetectedDtcs = telemetry.activeDtcCodes,
                activeVehicleName = activeVehicleName,
                activeTelemetry = telemetry,
                onDismiss = { showDtcExplainerDialog = false },
                onSendToOpenManus = { goal ->
                    coroutineScope.launch {
                        val dtcs = telemetry.activeDtcCodes.map { it.code }
                        openManusService?.runAutonomousDiagnosis(
                            goal = goal,
                            vehicleContext = activeVehicleName,
                            activeDtcs = dtcs
                        )
                    }
                }
            )
        }
    }
}

/**
 * Individual Sensor Metric Card with animated linear bar and pulsing live stream indicator
 */
@Composable
fun SensorGaugeCard(
    title: String,
    valueText: String,
    unitText: String,
    progress: Float,
    accentColor: Color,
    statusText: String,
    isLiveStreaming: Boolean = true,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sensor_progress"
    )

    // Infinite pulsing animation for active OBD-II hardware streaming
    val infiniteTransition = rememberInfiniteTransition(label = "sensor_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isLiveStreaming) 0.35f else 1.0f,
        targetValue = if (isLiveStreaming) 1.0f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isLiveStreaming) 0.15f else 0.0f,
        targetValue = if (isLiveStreaming) 0.55f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow_alpha"
    )

    Surface(
        color = ForgeBackground,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            1.dp,
            if (isLiveStreaming) accentColor.copy(alpha = pulseGlowAlpha) else ForgeBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForgeOnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (isLiveStreaming) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = pulseAlpha))
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = valueText,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isLiveStreaming) accentColor.copy(alpha = 0.85f + (pulseAlpha * 0.15f)) else accentColor
                )
                Text(
                    text = unitText,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 1.5.dp)
                )
            }

            // Linear Progress Bar with pulsing head glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ForgeSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.7f),
                                    accentColor.copy(alpha = if (isLiveStreaming) pulseAlpha else 1f)
                                )
                            )
                        )
                )
            }

            Text(
                text = statusText,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = if (accentColor == ForgeRed) ForgeRed else ForgeOnSurfaceVariant
            )
        }
    }
}

/**
 * Bi-Directional Fuel Trim Meter Card (Centered at 0.0%, Negative = Rich, Positive = Lean)
 */
@Composable
fun BiDirectionalTrimCard(
    title: String,
    pidCode: String,
    trimPct: Float,
    isAlert: Boolean,
    isLiveStreaming: Boolean = true,
    modifier: Modifier = Modifier
) {
    val trimColor = when {
        trimPct > 10.0f -> ForgeAmber // Lean
        trimPct < -10.0f -> ForgeCyan // Rich
        else -> ForgeGreen // Normal
    }

    val infiniteTransition = rememberInfiniteTransition(label = "trim_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isLiveStreaming) 0.35f else 1.0f,
        targetValue = if (isLiveStreaming) 1.0f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trim_pulse_alpha"
    )
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isLiveStreaming) 0.15f else 0.0f,
        targetValue = if (isLiveStreaming) 0.55f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trim_glow_alpha"
    )

    Surface(
        color = ForgeBackground,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            1.dp,
            if (isAlert) ForgeAmber else if (isLiveStreaming) trimColor.copy(alpha = pulseGlowAlpha) else ForgeBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ForgeOnSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLiveStreaming) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(trimColor.copy(alpha = pulseAlpha))
                        )
                    }
                    Text(
                        text = pidCode,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeOnSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = (if (trimPct > 0) "+" else "") + "%.1f".format(trimPct),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isLiveStreaming) trimColor.copy(alpha = 0.85f + (pulseAlpha * 0.15f)) else trimColor
                )
                Text(
                    text = "%",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 1.5.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (trimPct > 5.0f) "LEAN" else if (trimPct < -5.0f) "RICH" else "STOICH",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = trimColor,
                    modifier = Modifier.padding(bottom = 1.5.dp)
                )
            }

            // Bi-Directional Bar (-25% to +25%, center zero)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ForgeSurfaceVariant)
            ) {
                // Center zero marker
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .align(Alignment.Center)
                        .background(ForgeOnSurfaceVariant)
                )

                // Fill from center
                val normalizedTrim = (trimPct / 25f).coerceIn(-1f, 1f)
                val fillFraction = kotlin.math.abs(normalizedTrim) * 0.5f

                if (normalizedTrim > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillFraction)
                            .align(Alignment.CenterStart)
                            .offset(x = 55.dp) // Offset to center right
                            .background(trimColor)
                    )
                } else if (normalizedTrim < 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillFraction)
                            .align(Alignment.Center)
                            .background(trimColor)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("-25% Rich", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                Text("0.0%", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
                Text("+25% Lean", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, color = ForgeOnSurfaceVariant)
            }
        }
    }
}

enum class UnitSystem {
    IMPERIAL,
    METRIC
}

enum class GraphPidChannel(val displayName: String) {
    RPM("RPM"),
    BOOST("BOOST"),
    FUEL_TRIM("STFT"),
    COOLANT("ECT")
}
