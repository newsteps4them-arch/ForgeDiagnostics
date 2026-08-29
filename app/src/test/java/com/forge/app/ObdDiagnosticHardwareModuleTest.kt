package com.forge.app

import com.forge.app.services.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.StandardTestDispatcher

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
        geminiService = GeminiService()
        openManusService = OpenManusAgentService(geminiService)
        hardwareModule = ObdDiagnosticHardwareModule(
            scope = testScope,
            usbHardwareService = null,
            telemetryService = telemetryService,
            openManusService = openManusService
        )
    }

    @After
    fun tearDown() {
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
        hardwareModule.fetchLiveDiagnosticTroubleCodes(
            vehicleName = "2021 Audi S5 Sportback",
            autoTriggerOpenManus = true
        )
        delay(600)

        val state = hardwareModule.hardwareState.value
        assertFalse(state.isFetchingDtcs)
        assertTrue(state.activeDtcs.isNotEmpty())

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
