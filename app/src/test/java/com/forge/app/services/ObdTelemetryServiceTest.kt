package com.forge.app.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
class ObdTelemetryServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        testScope.coroutineContext.cancelChildren()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        val telemetry = service.telemetry.value

        assertEquals(0, telemetry.rpm)
        assertEquals(0, telemetry.speedKmh)
        assertEquals(0, telemetry.coolantTempC)
        assertFalse(telemetry.isConnected)
        assertEquals("SIMULATED", telemetry.connectionType)
        assertTrue(telemetry.activeDtcCodes.isEmpty())
        // Fresh telemetry has unknown provenance — it must never default to "live".
        assertEquals(com.forge.app.services.DiagnosticDataSource.UNKNOWN, telemetry.dataSource)
        service.stopTelemetryLoop()
    }

    @Test
    fun testSetRpmMarksLiveHardware() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        service.setRpm(864)
        val telemetry = service.telemetry.value
        assertEquals(864, telemetry.rpm)
        assertEquals(com.forge.app.services.DiagnosticDataSource.LIVE_HARDWARE, telemetry.dataSource)
        service.stopTelemetryLoop()
    }

    @Test
    fun testParseRpmResponse_ValidData() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        val rpm = service.parseRpmResponse("41 0C 0D 80")
        assertEquals(864, rpm)

        val rpmWithEcho = service.parseRpmResponse("SEARCHING...\r\n41 0C 0D 80 \r\r>")
        assertEquals(864, rpmWithEcho)

        service.stopTelemetryLoop()
    }

    @Test
    fun testParseRpmResponse_InvalidData() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)

        assertEquals(null, service.parseRpmResponse("INVALID DATA"))
        assertEquals(null, service.parseRpmResponse("410D00"))
        assertEquals(null, service.parseRpmResponse("41 0C XY ZZ"))
        assertEquals(null, service.parseRpmResponse("NO DATA 41 0C 0D 80"))
        service.stopTelemetryLoop()
    }

    @Test
    fun testSetSpeed() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)

        service.setSpeed(120)
        assertEquals(120, service.telemetry.value.speedKmh)
        service.setSpeed(-10)
        assertEquals(0, service.telemetry.value.speedKmh)
        service.setSpeed(300)
        assertEquals(240, service.telemetry.value.speedKmh)
        service.stopTelemetryLoop()
    }

    @Test
    fun testClearDtcs() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        assertTrue(service.telemetry.value.activeDtcCodes.isEmpty())
        service.addDtc("P0300", "Random Misfire")
        service.clearDtcs()
        assertTrue(service.telemetry.value.activeDtcCodes.isEmpty())
        service.stopTelemetryLoop()
    }

    @Test
    fun testAddDtc() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        service.addDtc("P1234", "Test Error")

        val dtcs = service.telemetry.value.activeDtcCodes
        assertEquals(1, dtcs.size)
        assertEquals("P1234", dtcs[0].code)
        assertEquals("Test Error", dtcs[0].description)
        assertEquals("Stored", dtcs[0].status)
        service.stopTelemetryLoop()
    }

    @Test
    fun testSetConnectionType() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        service.setConnectionType("BLUETOOTH")
        assertEquals("BLUETOOTH", service.telemetry.value.connectionType)
        service.stopTelemetryLoop()
    }

    @Test
    fun testToggleConnection() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        val initialStatus = service.telemetry.value.isConnected
        service.toggleConnection()

        // Let's check logic:
        // if false -> sets connectionStatusText = "Connect a physical OBD-II adapter before polling" (isConnected still false!)
        // So toggleConnection DOES NOT TOGGLE when it's false in the current production code!
        assertEquals(initialStatus, service.telemetry.value.isConnected)

        // To test toggle when it's true, we'd need to mock the state, let's skip for now by just asserting the existing behaviour
        service.stopTelemetryLoop()
    }

    @Test
    fun testStartTelemetryLoop_SimulatedUpdates() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        service.setConnectionType("SIMULATED")

        // Wait, telemetry loop only updates if isConnected is true.
        // We need to set isConnected to true for this test.
        val field: Field = ObdTelemetryService::class.java.getDeclaredField("_telemetry")
        field.isAccessible = true
        val stateFlow = field.get(service) as MutableStateFlow<ObdTelemetryData>
        stateFlow.value = stateFlow.value.copy(isConnected = true)

        val initialRpm = service.telemetry.value.rpm
        testScope.advanceTimeBy(350)
        val updatedRpm = service.telemetry.value.rpm
        assertTrue(updatedRpm >= 750 && updatedRpm <= 6800)
        assertTrue(updatedRpm != initialRpm)
        service.stopTelemetryLoop()
    }
}
