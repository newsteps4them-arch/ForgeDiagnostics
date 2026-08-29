// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app

import com.forge.app.services.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ObdDiagnosticHardwareModuleTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var telemetryService: ObdTelemetryService
    private lateinit var geminiService: GeminiService
    private lateinit var openManusService: OpenManusAgentService
    private lateinit var hardwareModule: ObdDiagnosticHardwareModule

    @Before
    fun setup() {
        telemetryService = ObdTelemetryService(testScope)
        geminiService = GeminiService()
        openManusService = OpenManusAgentService(geminiService)
        hardwareModule = ObdDiagnosticHardwareModule(
            scope = testScope,
            usbHardwareService = null,
            telemetryService = telemetryService,
            openManusService = openManusService
        )
    }

    @Test
    fun testInterfaceSwitching() {
        hardwareModule.setHardwareInterface(ObdHardwareInterface.BLUETOOTH_SPP)
        assertEquals(ObdHardwareInterface.BLUETOOTH_SPP, hardwareModule.hardwareState.value.selectedInterface)

        hardwareModule.setHardwareInterface(ObdHardwareInterface.USB_OTG)
        assertEquals(ObdHardwareInterface.USB_OTG, hardwareModule.hardwareState.value.selectedInterface)
    }

    @Test
    fun testFetchLiveDtcCodesAndOpenManusAutoTrigger() = runBlocking {
        // Mock connection first so it actually parses fake data
        hardwareModule.setHardwareInterface(ObdHardwareInterface.SIMULATED)
        delay(100)

        hardwareModule.fetchLiveDiagnosticTroubleCodes(
            vehicleName = "2021 Audi S5 Sportback",
            autoTriggerOpenManus = true
        )
        delay(600)

        val state = hardwareModule.hardwareState.value
        assertFalse(state.isFetchingDtcs)
        // Only assert what we reasonably mock or know about the simulator
        assertNotNull(state.activeDtcs)

        val dtcCodes = state.activeDtcs.map { it.code }
        assertTrue(dtcCodes.contains("P0300") || dtcCodes.contains("P0171"))

        // Verify OpenManus received active DTCs and generated diagnosis
        val agentState = openManusService.state.value
        assertNotNull(agentState.finalReport)
        assertTrue(agentState.finalReport?.primaryRootCause?.isNotBlank() == true)
    }

    @Test
    fun testClearHardwareFaultCodes() = runBlocking {
        // First add DTC
        telemetryService.addDtc("P0300", "Random Misfire")
        assertTrue(telemetryService.telemetry.value.activeDtcCodes.isNotEmpty())

        // Clear codes
        hardwareModule.clearHardwareFaultCodes()
        delay(300)

        assertEquals(0, telemetryService.telemetry.value.activeDtcCodes.size)
        assertEquals(0, hardwareModule.hardwareState.value.activeDtcs.size)
    }
}
