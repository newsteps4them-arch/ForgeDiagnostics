// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app

import com.forge.app.services.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class ObdDiagnosticHardwareModuleTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var telemetryService: ObdTelemetryService
    private lateinit var geminiService: GeminiService
    private lateinit var openManusService: OpenManusAgentService
    private lateinit var hardwareModule: ObdDiagnosticHardwareModule
    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        telemetryService = ObdTelemetryService(testScope)
        geminiService = object : GeminiService() {
            override suspend fun generateDiagnosticAnalysis(prompt: String): String {
                return "Primary Root Cause: Verified Intake Vacuum Infiltration on Bank 1."
            }
        }
        openManusService = OpenManusAgentService(geminiService, Dispatchers.Unconfined)


        hardwareModule = ObdDiagnosticHardwareModule(
            scope = testScope,
            usbHardwareService = null,
            telemetryService = telemetryService,
            openManusService = openManusService,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined
        )

    }

    @After
    fun tearDown() {
        telemetryService.stopTelemetryLoop()
        Dispatchers.resetMain()
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
        hardwareModule.setHardwareInterface(ObdHardwareInterface.SIMULATED)

        hardwareModule.fetchLiveDiagnosticTroubleCodes(
            vehicleName = "2021 Audi S5 Sportback",
            autoTriggerOpenManus = true,
        )

        var attempts = 0
        while (attempts < 50 && (hardwareModule.hardwareState.value.isFetchingDtcs || openManusService.state.value.finalReport == null)) {
            delay(100)
            attempts++
        }

        val state = hardwareModule.hardwareState.value
        assertFalse(state.isFetchingDtcs)
        assertNotNull(state.activeDtcs)

        val dtcCodes = state.activeDtcs.map { it.code }
        assertTrue(dtcCodes.contains("P0300") || dtcCodes.contains("P0171"))

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
        delay(300.milliseconds)

        assertEquals(0, telemetryService.telemetry.value.activeDtcCodes.size)
        assertEquals(0, hardwareModule.hardwareState.value.activeDtcs.size)
    }
}
