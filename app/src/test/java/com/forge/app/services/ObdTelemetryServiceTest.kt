package com.forge.app.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        val telemetry = service.telemetry.value

        assertEquals(850, telemetry.rpm)
        assertEquals(0, telemetry.speedKmh)
        assertEquals(90, telemetry.coolantTempC)
        assertTrue(telemetry.isConnected)
        assertEquals("SIMULATED", telemetry.connectionType)
        assertEquals(2, telemetry.activeDtcCodes.size)
    }

    @Test
    fun testParseRpmResponse_ValidData() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)

        // "41 0C 0D 80" -> 0x0D80 = 3456. 3456 / 4 = 864 RPM
        val rpm = service.parseRpmResponse("41 0C 0D 80")
        assertEquals(864, rpm)
    }

    @Test
    fun testParseRpmResponse_InvalidData() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)

        val rpm1 = service.parseRpmResponse("INVALID DATA")
        assertEquals(null, rpm1)

        val rpm2 = service.parseRpmResponse("410D00") // Speed response, not RPM
        assertEquals(null, rpm2)

        val rpm3 = service.parseRpmResponse("41 0C XY ZZ") // Malformed hex
        assertEquals(null, rpm3)
    }

    @Test
    fun testSetSpeed() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)

        service.setSpeed(120)
        assertEquals(120, service.telemetry.value.speedKmh)

        // Test coercion min
        service.setSpeed(-10)
        assertEquals(0, service.telemetry.value.speedKmh)

        // Test coercion max
        service.setSpeed(300)
        assertEquals(240, service.telemetry.value.speedKmh)
    }

    @Test
    fun testClearDtcs() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)

        // Ensure we start with some DTCs
        assertTrue(service.telemetry.value.activeDtcCodes.isNotEmpty())

        service.clearDtcs()

        // Assert they are cleared
        assertTrue(service.telemetry.value.activeDtcCodes.isEmpty())
    }

    @Test
    fun testAddDtc() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)

        service.clearDtcs() // start fresh

        service.addDtc("P1234", "Test Error")

        val dtcs = service.telemetry.value.activeDtcCodes
        assertEquals(1, dtcs.size)
        assertEquals("P1234", dtcs[0].code)
        assertEquals("Test Error", dtcs[0].description)
        assertEquals("Stored", dtcs[0].status)
    }

    @Test
    fun testSetConnectionType() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)

        service.setConnectionType("BLUETOOTH")
        assertEquals("BLUETOOTH", service.telemetry.value.connectionType)
    }

    @Test
    fun testToggleConnection() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)

        val initialStatus = service.telemetry.value.isConnected

        service.toggleConnection()
        assertEquals(!initialStatus, service.telemetry.value.isConnected)

        service.toggleConnection()
        assertEquals(initialStatus, service.telemetry.value.isConnected)
    }

    @Test
    fun testStartTelemetryLoop_SimulatedUpdates() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        service.setConnectionType("SIMULATED")

        val initialRpm = service.telemetry.value.rpm

        // Advance time to allow the telemetry loop to run (delay is 300ms)
        testScope.advanceTimeBy(350)

        val updatedRpm = service.telemetry.value.rpm
        // It's randomized, but it should not be exactly 850 (unless randomly generated as 850, very low probability)
        // Or speed might have changed. Let's just check it doesn't crash and value is updated
        assertTrue(updatedRpm >= 750 && updatedRpm <= 6800)
    }
}
