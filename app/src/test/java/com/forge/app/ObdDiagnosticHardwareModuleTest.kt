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
        hardwareModule.disconnect()
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
        assertTrue(state.activeDtcs.isNotEmpty())
        val dtcCodes = state.activeDtcs.map { it.code }
        assertTrue(dtcCodes.contains("P0300"))
        assertTrue(dtcCodes.contains("P0171"))
        assertFalse(dtcCodes.contains("P0133"))
        assertFalse(dtcCodes.contains("C0171"))
        assertNotNull(openManusService.state.value.finalReport)
    }

    @Test
    fun testClearHardwareFaultCodes() = runBlocking {
        hardwareModule.setHardwareInterface(ObdHardwareInterface.SIMULATED)
        telemetryService.addDtc("P0300", "Random Misfire")
        hardwareModule.clearHardwareFaultCodes()
        delay(300.milliseconds)
        assertTrue(telemetryService.telemetry.value.activeDtcCodes.isEmpty())
        assertTrue(hardwareModule.hardwareState.value.activeDtcs.isEmpty())
    }
}
