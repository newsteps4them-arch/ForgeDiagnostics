package com.forge.app.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObdTelemetryServiceTest {

    private lateinit var testScope: CoroutineScope
    private lateinit var service: ObdTelemetryService

    @Before
    fun setup() {
        testScope = CoroutineScope(Dispatchers.Unconfined + Job())
        service = ObdTelemetryService(testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun testSetSpeed() {
        service.setSpeed(120)
        assertEquals(120, service.telemetry.value.speedKmh)

        service.setSpeed(-10) // should coerce to 0
        assertEquals(0, service.telemetry.value.speedKmh)

        service.setSpeed(300) // should coerce to 240
        assertEquals(240, service.telemetry.value.speedKmh)
    }

    @Test
    fun testClearDtcs() {
        service.clearDtcs()
        assertTrue(service.telemetry.value.activeDtcCodes.isEmpty())
    }

    @Test
    fun testAddDtc() {
        service.clearDtcs()
        service.addDtc("P0123", "Throttle Position Sensor/Switch A Circuit High Input")
        assertEquals(1, service.telemetry.value.activeDtcCodes.size)
        assertEquals("P0123", service.telemetry.value.activeDtcCodes[0].code)
        assertEquals("Throttle Position Sensor/Switch A Circuit High Input", service.telemetry.value.activeDtcCodes[0].description)
        assertEquals("Stored", service.telemetry.value.activeDtcCodes[0].status)
    }

    @Test
    fun testSetConnectionType() {
        service.setConnectionType("USB_OTG")
        assertEquals("USB_OTG", service.telemetry.value.connectionType)
    }

    @Test
    fun testToggleConnection() {
        val initialState = service.telemetry.value.isConnected
        service.toggleConnection()
        assertEquals(!initialState, service.telemetry.value.isConnected)
        service.toggleConnection()
        assertEquals(initialState, service.telemetry.value.isConnected)
    }

    @Test
    fun testParseRpmResponse() {
        // Valid RPM response: 410C 1A F8
        // A = 1A = 26, B = F8 = 248
        // RPM = (26 * 256 + 248) / 4 = 1726
        var result = service.parseRpmResponse("41 0C 1A F8")
        assertEquals(1726, result)

        // Valid RPM response without spaces
        result = service.parseRpmResponse("410C1AF8")
        assertEquals(1726, result)

        // Invalid RPM response (no 410C)
        result = service.parseRpmResponse("410D1AF8")
        assertNull(result)

        // Invalid RPM response (too short after 410C)
        result = service.parseRpmResponse("410C1A")
        assertNull(result)

        // Invalid RPM response (malformed hex)
        result = service.parseRpmResponse("410CZZZZ")
        assertNull(result)

        // Invalid RPM response (empty string)
        result = service.parseRpmResponse("")
        assertNull(result)
    }
}
