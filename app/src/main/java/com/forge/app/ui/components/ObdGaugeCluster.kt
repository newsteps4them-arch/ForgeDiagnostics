package com.forge.app.ui.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.ObdTelemetryData
import com.forge.app.ui.theme.*
import kotlin.math.*

/**
 * High-performance, hardware-styled Automotive Radial Gauge Composable
 * with physical spring needle damping, smooth numerical roll transitions, and warning aura pulsing.
 */
@Composable
fun RadialGauge(
    title: String,
    value: Float,
    minValue: Float = 0f,
    maxValue: Float = 8000f,
    unit: String = "RPM",
    primaryColor: Color = ForgeAmber,
    warningThreshold: Float? = 6500f,
    criticalThreshold: Float? = 7200f,
    majorTickStep: Float = 1000f,
    minorTickDivisions: Int = 4,
    multiplierDisplay: String? = "x1000",
    modifier: Modifier = Modifier
) {
    // Physical Spring-damped needle animation for realistic automotive inertia
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(minValue, maxValue),
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 180f
        ),
        label = "gauge_needle_spring"
    )

    val startAngle = 135f
    val sweepAngle = 270f
    val currentFraction = (animatedValue - minValue) / (maxValue - minValue)
    val isCritical = criticalThreshold != null && animatedValue >= criticalThreshold
    val isWarning = warningThreshold != null && animatedValue >= warningThreshold

    // Smooth animated color morphing
    val targetActiveColor = when {
        isCritical -> ForgeRed
        isWarning -> ForgeAmber
        else -> primaryColor
    }

    val activeColor by animateColorAsState(
        targetValue = targetActiveColor,
        animationSpec = tween(durationMillis = 250),
        label = "active_color_morph"
    )

    // Warning / Redline Infinite Pulsing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = ForgeSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCritical) ForgeRed.copy(alpha = pulseAlpha) else ForgeBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (multiplierDisplay != null) {
                    Text(
                        text = multiplierDisplay,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Canvas Gauge Dial with Pulsing Halo & Needle
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.minDimension / 2f) - 10.dp.toPx()

                    // Background Arc Track
                    drawArc(
                        color = Color(0xFF1E2230),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Warning / Redline Zone Background Arc with optional pulse
                    if (warningThreshold != null) {
                        val warnFraction = (warningThreshold - minValue) / (maxValue - minValue)
                        val warnStartAngle = startAngle + (warnFraction * sweepAngle)
                        val warnSweep = (1f - warnFraction) * sweepAngle

                        drawArc(
                            color = if (isCritical) ForgeRed.copy(alpha = pulseAlpha * 0.5f) else ForgeRed.copy(alpha = 0.35f),
                            startAngle = warnStartAngle,
                            sweepAngle = warnSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Active Sweep Arc with Gradient
                    val activeSweep = (currentFraction * sweepAngle).coerceAtLeast(0.01f)
                    val brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.7f),
                            activeColor,
                            if (isCritical) ForgeRed else activeColor
                        ),
                        center = center
                    )

                    drawArc(
                        brush = brush,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Outer Glowing Ring when reaching critical/redline threshold
                    if (isCritical) {
                        drawCircle(
                            color = ForgeRed.copy(alpha = pulseAlpha * 0.15f),
                            radius = radius + 6.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Tick Marks
                    val totalTicks = ((maxValue - minValue) / majorTickStep).toInt()
                    val totalSubTicks = totalTicks * minorTickDivisions

                    for (i in 0..totalSubTicks) {
                        val tickFraction = i.toFloat() / totalSubTicks
                        val angleDeg = startAngle + (tickFraction * sweepAngle)
                        val angleRad = Math.toRadians(angleDeg.toDouble())

                        val isMajor = (i % minorTickDivisions) == 0
                        val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                        val tickColor = if (isMajor) Color(0xFF8C93A8) else Color(0xFF4A5168)
                        val strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                        val outerRadius = radius - 8.dp.toPx()
                        val innerRadius = outerRadius - tickLength

                        val startX = (center.x + outerRadius * cos(angleRad)).toFloat()
                        val startY = (center.y + outerRadius * sin(angleRad)).toFloat()
                        val endX = (center.x + innerRadius * cos(angleRad)).toFloat()
                        val endY = (center.y + innerRadius * sin(angleRad)).toFloat()

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // Needle Indicator with physical angle calculation
                    val needleAngleDeg = startAngle + (currentFraction * sweepAngle)
                    val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
                    val needleLength = radius - 14.dp.toPx()

                    val needleEndX = (center.x + needleLength * cos(needleAngleRad)).toFloat()
                    val needleEndY = (center.y + needleLength * sin(needleAngleRad)).toFloat()

                    // Needle Line
                    drawLine(
                        color = activeColor,
                        start = center,
                        end = Offset(needleEndX, needleEndY),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Center Hub Pivot with multi-ring finish
                    drawCircle(
                        color = Color(0xFF141721),
                        radius = 12.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = activeColor,
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }

                // Digital Center Readout with Smooth Animated Rolling Counter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 42.dp)
                ) {
                    val displayString = if (maxValue >= 1000) "${animatedValue.toInt()}" else String.format("%.1f", animatedValue)

                    AnimatedContent(
                        targetState = displayString,
                        transitionSpec = {
                            (slideInVertically { height -> height / 2 } + fadeIn(tween(100))) togetherWith
                                    (slideOutVertically { height -> -height / 2 } + fadeOut(tween(100)))
                        },
                        label = "digital_counter_anim"
                    ) { targetText ->
                        Text(
                            text = targetText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = activeColor
                        )
                    }

                    Text(
                        text = unit,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Semi-circular / Arc Bar Gauge for Coolant Temp with smooth thermal transitions and overheating alert animations
 */
@Composable
fun CoolantTempGauge(
    coolantTempC: Int,
    modifier: Modifier = Modifier
) {
    val animatedTemp by animateFloatAsState(
        targetValue = coolantTempC.toFloat().coerceIn(40f, 130f),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 140f
        ),
        label = "coolant_spring_anim"
    )

    val isOverheat = animatedTemp >= 105f
    val isCold = animatedTemp < 70f
    val isOptimal = !isOverheat && !isCold

    val targetStatusText = when {
        animatedTemp >= 115f -> "OVERHEATING"
        animatedTemp >= 105f -> "HOT"
        animatedTemp < 70f -> "COLD WARM-UP"
        else -> "OPTIMAL (90°C)"
    }

    val targetStatusColor = when {
        animatedTemp >= 105f -> ForgeRed
        animatedTemp < 70f -> ForgeCyan
        else -> ForgeGreen
    }

    val statusColor by animateColorAsState(
        targetValue = targetStatusColor,
        animationSpec = tween(durationMillis = 300),
        label = "coolant_color_morph"
    )

    // Overheat warning pulsation
    val infiniteTransition = rememberInfiniteTransition(label = "overheat_pulse")
    val overheatPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overheat_alpha"
    )

    Surface(
        color = ForgeSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOverheat) ForgeRed.copy(alpha = overheatPulseAlpha) else ForgeBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = "Coolant Temp",
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "ENGINE COOLANT TEMP",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = if (isOverheat) overheatPulseAlpha * 0.3f else 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    AnimatedContent(
                        targetState = targetStatusText,
                        transitionSpec = {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                        },
                        label = "coolant_status_anim"
                    ) { targetText ->
                        Text(
                            text = targetText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Radial Arc Temperature Meter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height - 10.dp.toPx())
                    val radius = size.height - 15.dp.toPx()

                    // Background Track (180 deg semi-circle: 180° to 360°)
                    drawArc(
                        color = Color(0xFF1E2230),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Cold arc = 60 deg
                    drawArc(
                        color = ForgeCyan.copy(alpha = 0.3f),
                        startAngle = 180f,
                        sweepAngle = 60f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Optimal arc = 70 deg
                    drawArc(
                        color = ForgeGreen.copy(alpha = 0.3f),
                        startAngle = 240f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Danger arc = 50 deg with optional overheat pulse
                    drawArc(
                        color = if (isOverheat) ForgeRed.copy(alpha = overheatPulseAlpha * 0.6f) else ForgeRed.copy(alpha = 0.3f),
                        startAngle = 310f,
                        sweepAngle = 50f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active sweep
                    val fraction = ((animatedTemp - 40f) / 90f).coerceIn(0f, 1f)
                    val activeSweep = fraction * 180f

                    drawArc(
                        color = statusColor,
                        startAngle = 180f,
                        sweepAngle = activeSweep.coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Needle
                    val needleAngleRad = Math.toRadians((180f + activeSweep).toDouble())
                    val needleLength = radius - 8.dp.toPx()
                    val endX = (center.x + needleLength * cos(needleAngleRad)).toFloat()
                    val endY = (center.y + needleLength * sin(needleAngleRad)).toFloat()

                    drawLine(
                        color = statusColor,
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = Color(0xFF141721), radius = 8.dp.toPx(), center = center)
                    drawCircle(color = statusColor, radius = 4.dp.toPx(), center = center)
                }

                // Digital Temperature Readout with animated content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 22.dp)
                ) {
                    val tempString = "${coolantTempC}°C / ${(coolantTempC * 9 / 5) + 32}°F"

                    AnimatedContent(
                        targetState = tempString,
                        transitionSpec = {
                            (slideInVertically { height -> height / 2 } + fadeIn(tween(100))) togetherWith
                                    (slideOutVertically { height -> -height / 2 } + fadeOut(tween(100)))
                        },
                        label = "coolant_val_anim"
                    ) { targetText ->
                        Text(
                            text = targetText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                    }

                    Text(
                        text = "PID 0105 (ECT)",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Complete Real-Time Diagnostic Dashboard Instrument Cluster Composable
 */
@Composable
fun ObdInstrumentCluster(
    telemetry: ObdTelemetryData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dual Main Radial Gauges: Tachometer (RPM) & Speedometer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadialGauge(
                title = "Engine RPM",
                value = telemetry.rpm.toFloat(),
                minValue = 0f,
                maxValue = 8000f,
                unit = "RPM",
                primaryColor = ForgeAmber,
                warningThreshold = 6200f,
                criticalThreshold = 7000f,
                majorTickStep = 1000f,
                multiplierDisplay = "x1000",
                modifier = Modifier.weight(1f)
            )

            RadialGauge(
                title = "Vehicle Speed",
                value = telemetry.speedKmh.toFloat(),
                minValue = 0f,
                maxValue = 260f,
                unit = "KM/H",
                primaryColor = ForgeCyan,
                warningThreshold = 180f,
                criticalThreshold = 220f,
                majorTickStep = 40f,
                multiplierDisplay = "${(telemetry.speedKmh * 0.621371).toInt()} MPH",
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary Thermal & Pressure Arc Gauges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CoolantTempGauge(
                coolantTempC = telemetry.coolantTempC,
                modifier = Modifier.weight(1f)
            )

            // Intake Air Temp & Battery Mini Tile
            Surface(
                color = ForgeSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForgeBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Battery Voltage Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("BATTERY VOLTS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AnimatedContent(
                                targetState = "${telemetry.batteryVoltage} V",
                                transitionSpec = {
                                    (slideInVertically { height -> height / 2 } + fadeIn(tween(100))) togetherWith
                                            (slideOutVertically { height -> -height / 2 } + fadeOut(tween(100)))
                                },
                                label = "battery_val_anim"
                            ) { targetVal ->
                                Text(
                                    targetVal,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (telemetry.batteryVoltage < 12.0f) ForgeRed else ForgeGreen
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Battery",
                            tint = if (telemetry.batteryVoltage < 12.0f) ForgeRed else ForgeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = ForgeBorder, thickness = 0.5.dp)

                    // Throttle Position
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("THROTTLE POS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AnimatedContent(
                                targetState = "${telemetry.throttlePosPct}%",
                                transitionSpec = {
                                    (slideInVertically { height -> height / 2 } + fadeIn(tween(100))) togetherWith
                                            (slideOutVertically { height -> -height / 2 } + fadeOut(tween(100)))
                                },
                                label = "throttle_val_anim"
                            ) { targetVal ->
                                Text(
                                    targetVal,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = ForgeAmber
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Throttle",
                            tint = ForgeAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
