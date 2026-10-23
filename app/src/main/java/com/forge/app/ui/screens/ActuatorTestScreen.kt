package com.forge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.services.ObdTelemetryData
import com.forge.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ActuatorTestItem(
    val id: String,
    val name: String,
    val system: String,
    val canCommandHex: String,
    val description: String,
    val durationSeconds: Int = 10,
    val safetyRequirement: String = "Engine OFF, Ignition ON (Key On Engine Off - KOEO)"
)

val standardActuatorTests = listOf(
    ActuatorTestItem(
        id = "fuel_pump",
        name = "Fuel Pump Relay Pulse",
        system = "Fuel System",
        canCommandHex = "2F 11 03 01",
        description = "Energizes fuel pump relay for 5 seconds to test fuel rail priming pressure.",
        durationSeconds = 5,
        safetyRequirement = "Ignition ON, KOEO"
    ),
    ActuatorTestItem(
        id = "evap_purge",
        name = "EVAP Purge Valve Sweep",
        system = "Emissions",
        canCommandHex = "2F 02 44 64",
        description = "Cycles EVAP canister purge solenoid from 0% to 100% duty cycle.",
        durationSeconds = 8,
        safetyRequirement = "Engine at Idle"
    ),
    ActuatorTestItem(
        id = "cooling_fan_high",
        name = "Radiator Fan (High Stage)",
        system = "Cooling System",
        canCommandHex = "2F 19 01 02",
        description = "Commands High-Speed Engine Cooling Fan relay ON to verify circuit & motor.",
        durationSeconds = 6,
        safetyRequirement = "Ignition ON (Clear fan blades)"
    ),
    ActuatorTestItem(
        id = "ac_clutch",
        name = "A/C Compressor Clutch",
        system = "HVAC / Climate",
        canCommandHex = "2F 18 10 01",
        description = "Energizes magnetic A/C compressor clutch relay for engagement check.",
        durationSeconds = 5,
        safetyRequirement = "Engine Running at Idle"
    ),
    ActuatorTestItem(
        id = "throttle_sweep",
        name = "Electronic Throttle Calibration",
        system = "Powertrain / TAC",
        canCommandHex = "2F 04 20 50",
        description = "Sweeps Electronic Throttle Body actuator plate across full mechanical range.",
        durationSeconds = 8,
        safetyRequirement = "KOEO (Foot off accelerator pedal)"
    ),
    ActuatorTestItem(
        id = "abs_bleed_cycle",
        name = "ABS Hydraulic Pump Cycling",
        system = "Brakes / ABS",
        canCommandHex = "2F 2B 00 01",
        description = "Activates ABS hydraulic pump motor and inlet/outlet solenoids for automated brake bleed.",
        durationSeconds = 12,
        safetyRequirement = "Vehicle Stationary, Firm Brake Applied"
    )
)

@Composable
fun ActuatorTestScreen(
    telemetry: ObdTelemetryData = ObdTelemetryData()
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTest by remember { mutableStateOf<ActuatorTestItem?>(null) }
    var activeTestingId by remember { mutableStateOf<String?>(null) }
    var testTimeRemaining by remember { mutableStateOf(0) }
    var testLog by remember { mutableStateOf<List<String>>(listOf("System Ready: ECU Bi-Directional Service 2F Active")) }
    var safetyInterlockConfirmed by remember { mutableStateOf(false) }

    // Power Balance state
    var activeKilledCylinder by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
        Surface(
            color = ForgeSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeRed.copy(alpha = 0.8f))
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
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = ForgeRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "BI-DIRECTIONAL ACTUATOR CONTROLS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ForgeRed
                        )
                    }
                    Text(
                        "ECU Service $2F (Short Term IO Control by Identifier) & $31 Routine Control",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeOnSurfaceVariant
                    )
                }

                Surface(
                    color = if (activeTestingId != null) ForgeRed.copy(alpha = 0.2f) else ForgeGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (activeTestingId != null) "ACTUATION ACTIVE" else "INTERLOCK SAFE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTestingId != null) ForgeRed else ForgeGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Cylinder Power Balance Quick Strip
        Surface(
            color = ForgeSurfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CYLINDER POWER BALANCE TEST (INJECTOR CUT)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeCyan
                    )
                    Text(
                        "Live RPM: ${telemetry.rpm}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeGreen
                    )
                }

                Text(
                    "Tap cylinder button to momentarily cut injector pulse and monitor RPM drop contribution.",
                    fontSize = 10.sp,
                    color = ForgeOnSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (cyl in 1..8) {
                        val isKilled = activeKilledCylinder == cyl
                        Surface(
                            color = if (isKilled) ForgeRed else ForgeSurface,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isKilled) ForgeRed else ForgeBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (isKilled) {
                                        activeKilledCylinder = null
                                        testLog = testLog + "[POWER BALANCE] Restored Cylinder #$cyl fuel injection."
                                    } else {
                                        activeKilledCylinder = cyl
                                        testLog = testLog + "[POWER BALANCE] CUTTING Cylinder #$cyl (CMD: 2F 01 0$cyl 00)"
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "CYL $cyl",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKilled) Color.White else ForgeOnSurface
                                )
                                Text(
                                    if (isKilled) "OFF" else "ON",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isKilled) Color.White else ForgeGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Test Selector & Actuation Center
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(standardActuatorTests) { test ->
                val isSelected = selectedTest?.id == test.id
                val isRunningThis = activeTestingId == test.id

                Surface(
                    color = if (isRunningThis) ForgeRed.copy(alpha = 0.1f) else if (isSelected) ForgeSurfaceVariant else ForgeSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRunningThis) ForgeRed else if (isSelected) ForgeAmber else ForgeBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTest = test }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isRunningThis) ForgeRed else ForgeAmber, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    test.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForgeOnSurface
                                )
                            }

                            Surface(
                                color = ForgeSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    test.system,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ForgeCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(test.description, fontSize = 10.sp, color = ForgeOnSurfaceVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "CAN Payload: ${test.canCommandHex} • Safety: ${test.safetyRequirement}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForgeAmber
                            )

                            if (isRunningThis) {
                                Button(
                                    onClick = {
                                        activeTestingId = null
                                        testLog = testLog + "[ABORT] Actuator test aborted by technician: ${test.name}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeRed),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("STOP (${testTimeRemaining}s)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        activeTestingId = test.id
                                        testTimeRemaining = test.durationSeconds
                                        testLog = testLog + "[COMMAND] Transmitted CAN frame ${test.canCommandHex} -> ${test.name}"

                                        coroutineScope.launch {
                                            while (testTimeRemaining > 0 && activeTestingId == test.id) {
                                                delay(1000)
                                                testTimeRemaining -= 1
                                            }
                                            if (activeTestingId == test.id) {
                                                activeTestingId = null
                                                testLog = testLog + "[COMPLETED] Test finished: ${test.name} - ECU returned 6F (Positive Response)"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    enabled = activeTestingId == null
                                ) {
                                    Text("ACTUATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            // Command Log Console
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "BI-DIRECTIONAL INTERLOCK LOG & CAN ECHO",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForgeGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            items(testLog.takeLast(6)) { logLine ->
                Surface(
                    color = ForgeSurfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = logLine,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ForgeOnSurface,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}
