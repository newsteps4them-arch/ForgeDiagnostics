// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.hardware

import com.forge.app.ObdParserAndProtocolTest.ObdProtocolDecoder
import org.junit.Assert.*
import org.junit.Test

/**
 * ISO 26262 / Automotive SPICE Software Robustness & Fault Injection Suite.
 * Validates graceful degradation and error handling when physical OBD/CAN interfaces
 * produce corrupted, partial, noisy, or unexpected bus responses.
 */
class ObdFaultInjectionRobustnessTest {

    @Test
    fun testTruncatedPayloadGracefulHandling() {
        // Truncated RPM payloads
        assertEquals(0, ObdProtocolDecoder.parseRpm(""))
        assertEquals(0, ObdProtocolDecoder.parseRpm("41 0C"))
        assertEquals(0, ObdProtocolDecoder.parseRpm("41 0C 1"))

        // Truncated Speed payloads
        assertEquals(0, ObdProtocolDecoder.parseSpeedKmh(""))
        assertEquals(0, ObdProtocolDecoder.parseSpeedKmh("41 0D"))

        // Truncated Coolant Temp payloads
        assertEquals(0, ObdProtocolDecoder.parseCoolantTempC(""))
        assertEquals(0, ObdProtocolDecoder.parseCoolantTempC("41 05"))

        // Truncated Fuel Trim payloads
        assertEquals(0f, ObdProtocolDecoder.parseFuelTrim(""), 0.001f)
        assertEquals(0f, ObdProtocolDecoder.parseFuelTrim("41 06"), 0.001f)
    }

    @Test
    fun testElm327BusErrorAndNoiseInjection() {
        val busErrors = listOf(
            "NO DATA",
            "CAN ERROR",
            "BUS BUSY",
            "BUFFER FULL",
            "STOPPED",
            "UNABLE TO CONNECT",
            "FB 0C ZZ QQ",
            "??? ERROR"
        )

        for (errorFrame in busErrors) {
            assertFalse("Error frame '$errorFrame' should not be treated as OK", ObdProtocolDecoder.isElmOk(errorFrame))
            val dtcs = ObdProtocolDecoder.parseDtcResponse(errorFrame)
            assertTrue("Error frame '$errorFrame' must return empty DTC list without crashing", dtcs.isEmpty())
        }
    }

    @Test
    fun testDtcParserFaultInjection() {
        // Empty payload
        assertTrue(ObdProtocolDecoder.parseDtcResponse("").isEmpty())

        // Corrupted Mode 03 header
        assertTrue(ObdProtocolDecoder.parseDtcResponse("47 01 33").isEmpty())

        // Partial DTC frame (3 hex digits instead of 4)
        val partialDtcs = ObdProtocolDecoder.parseDtcResponse("43 01 3")
        assertTrue(partialDtcs.isEmpty())

        // Zeroes only (No active DTCs)
        val zeroDtcs = ObdProtocolDecoder.parseDtcResponse("43 00 00 00 00")
        assertTrue(zeroDtcs.isEmpty())

        // Valid multi-DTC frame with padding
        val validDtcs = ObdProtocolDecoder.parseDtcResponse("43 01 33 03 00 00 00")
        assertEquals(2, validDtcs.size)
        assertEquals("P0133", validDtcs[0])
        assertEquals("P0300", validDtcs[1])
    }

    @Test
    fun testUdsNegativeResponseCodeHandling() {
        // ISO 14229 UDS NRC 0x7F Handler simulation
        fun isUdsNegativeResponse(hex: String): Boolean {
            val clean = hex.replace(" ", "").trim()
            return clean.startsWith("7F")
        }

        fun parseUdsNrc(hex: String): Int? {
            val clean = hex.replace(" ", "").trim()
            if (!clean.startsWith("7F") || clean.length < 6) return null
            return clean.substring(4, 6).toIntOrNull(16)
        }

        // Service Not Supported (0x11)
        val nrc11 = "7F 22 11"
        assertTrue(isUdsNegativeResponse(nrc11))
        assertEquals(0x11, parseUdsNrc(nrc11))

        // SubFunction Not Supported (0x12)
        val nrc12 = "7F 19 12"
        assertTrue(isUdsNegativeResponse(nrc12))
        assertEquals(0x12, parseUdsNrc(nrc12))

        // Conditions Not Correct (0x22)
        val nrc22 = "7F 2E 22"
        assertTrue(isUdsNegativeResponse(nrc22))
        assertEquals(0x22, parseUdsNrc(nrc22))

        // Request Out of Range (0x31)
        val nrc31 = "7F 31 31"
        assertTrue(isUdsNegativeResponse(nrc31))
        assertEquals(0x31, parseUdsNrc(nrc31))

        // Positive Response (0x62)
        val positive = "62 F4 0C 1A F8"
        assertFalse(isUdsNegativeResponse(positive))
        assertNull(parseUdsNrc(positive))
    }
}
