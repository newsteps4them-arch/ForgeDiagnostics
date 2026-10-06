package com.forge.app.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
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
        testScope.coroutineContext.cancelChildren()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        try {
            val telemetry = service.telemetry.value

            assertEquals(0, telemetry.rpm)
            assertEquals(0, telemetry.speedKmh)
            assertEquals(0, telemetry.coolantTempC)
            assertFalse(telemetry.isConnected)
            assertEquals("SIMULATED", telemetry.connectionType)
            assertTrue(telemetry.activeDtcCodes.isEmpty())
        } finally {
            service.stopTelemetryLoop()
            testScope.coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testParseRpmResponse_ValidData() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        try {
            val rpm = service.parseRpmResponse("41 0C 0D 80")
            assertEquals(864, rpm)
        } finally {
            service.stopTelemetryLoop()
            testScope.coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testParseRpmResponse_InvalidData() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        try {
            assertEquals(null, service.parseRpmResponse("INVALID DATA"))
            assertEquals(null, service.parseRpmResponse("410D00"))
            assertEquals(null, service.parseRpmResponse("41 0C XY ZZ"))
            assertEquals(null, service.parseRpmResponse("NO DATA 41 0C 0D 80"))
        } finally {
            service.stopTelemetryLoop()
            testScope.coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testSetSpeed() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        try {
            service.setSpeed(120)
            assertEquals(120, service.telemetry.value.speedKmh)
            service.setSpeed(-10)
            assertEquals(0, service.telemetry.value.speedKmh)
            service.setSpeed(300)
            assertEquals(240, service.telemetry.value.speedKmh)
        } finally {
            service.stopTelemetryLoop()
            testScope.coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testClearDtcs() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        try {
            assertTrue(service.telemetry.value.activeDtcCodes.isEmpty())
            service.addDtc("P0300", "Random Misfire")
            service.clearDtcs()
            assertTrue(service.telemetry.value.activeDtcCodes.isEmpty())
        } finally {
            service.stopTelemetryLoop()
            testScope.coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testAddDtc() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        try {
            service.addDtc("P1234", "Test Error")

            val dtcs = service.telemetry.value.activeDtcCodes
            assertEquals(1, dtcs.size)
            assertEquals("P1234", dtcs[0].code)
            assertEquals("Test Error", dtcs[0].description)
            assertEquals("Stored", dtcs[0].status)
        } finally {
            service.stopTelemetryLoop()
            testScope.coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testSetConnectionType() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        try {
            service.setConnectionType("BLUETOOTH")
            assertEquals("BLUETOOTH", service.telemetry.value.connectionType)
        } finally {
            service.stopTelemetryLoop()
            testScope.coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testToggleConnection() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        try {
            val initialStatus = service.telemetry.value.isConnected
            service.toggleConnection()
            assertEquals(!initialStatus, service.telemetry.value.isConnected)
            service.toggleConnection()
            assertEquals(initialStatus, service.telemetry.value.isConnected)
        } finally {
            service.stopTelemetryLoop()
            testScope.coroutineContext.cancelChildren()
        }
    }

    @Test
    fun testStartTelemetryLoop_SimulatedUpdates() = runTest {
        val service = ObdTelemetryService(scope = testScope, usbHardwareService = null, ioDispatcher = testDispatcher)
        try {
            service.setConnectionType("SIMULATED")
            service.toggleConnection()
            val initialRpm = service.telemetry.value.rpm
            testScope.advanceTimeBy(350)
            val updatedRpm = service.telemetry.value.rpm
            assertTrue(updatedRpm >= 750 && updatedRpm <= 6800)
            assertTrue(updatedRpm != initialRpm)
        } finally {
            service.stopTelemetryLoop()
            testScope.coroutineContext.cancelChildren()
        }
    }
}
