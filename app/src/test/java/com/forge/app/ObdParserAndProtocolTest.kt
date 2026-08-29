package com.forge.app

import org.junit.Assert.*
import org.junit.Test

class ObdParserAndProtocolTest {

    // Helper parser simulation mimicking automotive ELM327 protocol processing
    object ObdProtocolDecoder {
        fun parseRpm(hexResponse: String): Int {
            // "41 0C 1A F8" -> ( (0x1A * 256) + 0xF8 ) / 4
            val clean = hexResponse.replace(" ", "").trim()
            val data = if (clean.startsWith("410C")) clean.substring(4) else clean
            if (data.length < 4) return 0
            val a = data.substring(0, 2).toInt(16)
            val b = data.substring(2, 4).toInt(16)
            return ((a * 256) + b) / 4
        }

        fun parseSpeedKmh(hexResponse: String): Int {
            // "41 0D 4B" -> 0x4B = 75 km/h
            val clean = hexResponse.replace(" ", "").trim()
            val data = if (clean.startsWith("410D")) clean.substring(4) else clean
            if (data.length < 2) return 0
            return data.substring(0, 2).toInt(16)
        }

        fun parseCoolantTempC(hexResponse: String): Int {
            // "41 05 7B" -> 0x7B (123) - 40 = 83°C
            val clean = hexResponse.replace(" ", "").trim()
            val data = if (clean.startsWith("4105")) clean.substring(4) else clean
            if (data.length < 2) return 0
            return data.substring(0, 2).toInt(16) - 40
        }

        fun parseThrottlePosition(hexResponse: String): Float {
            // "41 11 80" -> 0x80 (128) * 100 / 255 = 50.196%
            val clean = hexResponse.replace(" ", "").trim()
            val data = if (clean.startsWith("4111")) clean.substring(4) else clean
            if (data.length < 2) return 0f
            val a = data.substring(0, 2).toInt(16)
            return (a * 100f) / 255f
        }

        fun parseFuelTrim(hexResponse: String): Float {
            // "41 06 80" -> (0x80 - 128) * 100 / 128 = 0.0%
            // "41 06 90" -> (0x90 (144) - 128) * 100 / 128 = +12.5%
            val clean = hexResponse.replace(" ", "").trim()
            val data = if (clean.startsWith("4106") || clean.startsWith("4107")) clean.substring(4) else clean
            if (data.length < 2) return 0f
            val a = data.substring(0, 2).toInt(16)
            return ((a - 128) * 100f) / 128f
        }

        private fun parseCodeHex(codeHex: String): String? {
            if (codeHex == "0000") return null
            val firstNibble = codeHex[0].digitToInt(16)
            val prefix = when (firstNibble shr 2) {
                0 -> "P"
                1 -> "C"
                2 -> "B"
                3 -> "U"
                else -> "P"
            }
            val firstCharNum = (firstNibble and 0x03).toString()
            val restChars = codeHex.substring(1)
            return "$prefix$firstCharNum$restChars"
        }

        fun parseDtcResponse(hexResponse: String): List<String> {
            // Mode 03 response: "43 01 33 03 00 00 00" -> P0133, P0300
            val clean = hexResponse.replace(" ", "").trim()
            if (!clean.startsWith("43")) return emptyList()
            val dtcs = mutableListOf<String>()
            val payload = clean.substring(2)
            for (i in 0 until payload.length step 4) {
                if (i + 4 <= payload.length) {
                    val codeHex = payload.substring(i, i + 4)
                    parseCodeHex(codeHex)?.let { dtcs.add(it) }
                }
            }
            return dtcs
        }

        fun isElmOk(response: String): Boolean {
            val clean = response.trim().uppercase()
            return clean == "OK" || clean.endsWith("\nOK") || clean.contains("ELM327")
        }
    }

    @Test
    fun testRpmDecoding() {
        // 0x1A * 256 + 0xF8 = 6904; 6904 / 4 = 1726 RPM
        val rpm = ObdProtocolDecoder.parseRpm("41 0C 1A F8")
        assertEquals(1726, rpm)

        // Idle 800 RPM -> 3200 total -> 0x0C80
        val idleRpm = ObdProtocolDecoder.parseRpm("41 0C 0C 80")
        assertEquals(800, idleRpm)
    }

    @Test
    fun testVehicleSpeedDecoding() {
        val speed = ObdProtocolDecoder.parseSpeedKmh("41 0D 64") // 0x64 = 100 km/h
        assertEquals(100, speed)
    }

    @Test
    fun testCoolantTempDecoding() {
        // 130 - 40 = 90°C (0x82 = 130)
        val temp = ObdProtocolDecoder.parseCoolantTempC("41 05 82")
        assertEquals(90, temp)

        // Cold start -10°C (30 - 40 = -10, 0x1E = 30)
        val coldTemp = ObdProtocolDecoder.parseCoolantTempC("41 05 1E")
        assertEquals(-10, coldTemp)
    }

    @Test
    fun testThrottlePositionDecoding() {
        val tpZero = ObdProtocolDecoder.parseThrottlePosition("41 11 00")
        assertEquals(0.0f, tpZero, 0.01f)

        val tpFull = ObdProtocolDecoder.parseThrottlePosition("41 11 FF")
        assertEquals(100.0f, tpFull, 0.01f)

        val tpHalf = ObdProtocolDecoder.parseThrottlePosition("41 11 80")
        assertEquals(50.196f, tpHalf, 0.05f)
    }

    @Test
    fun testFuelTrimDecoding() {
        // 0% trim (0x80 = 128)
        val trimZero = ObdProtocolDecoder.parseFuelTrim("41 06 80")
        assertEquals(0.0f, trimZero, 0.01f)

        // +12.5% lean correction (0x90 = 144)
        val trimRich = ObdProtocolDecoder.parseFuelTrim("41 06 90")
        assertEquals(12.5f, trimRich, 0.01f)

        // -10.15% rich correction (0x73 = 115)
        val trimNegative = ObdProtocolDecoder.parseFuelTrim("41 06 73")
        assertEquals(-10.15f, trimNegative, 0.05f)
    }

    @Test
    fun testMode03DtcDecoding() {
        // Mode 03 response with P0133 (O2 Sensor Slow Response) and P0300 (Random Misfire)
        val response = "43 01 33 03 00 00 00"
        val dtcs = ObdProtocolDecoder.parseDtcResponse(response)
        assertEquals(2, dtcs.size)
        assertEquals("P0133", dtcs[0])
        assertEquals("P0300", dtcs[1])
    }

    @Test
    fun testChassisAndNetworkDtcDecoding() {
        // 0x4000 = C0000, 0x8000 = B0000, 0xC100 = U0100
        val response = "43 C1 00 41 23 00 00"
        val dtcs = ObdProtocolDecoder.parseDtcResponse(response)
        assertEquals(2, dtcs.size)
        assertEquals("U0100", dtcs[0])
        assertEquals("C0123", dtcs[1])
    }

    @Test
    fun testElmResponseValidation() {
        assertTrue(ObdProtocolDecoder.isElmOk("OK"))
        assertTrue(ObdProtocolDecoder.isElmOk("ELM327 v1.5"))
        assertTrue(ObdProtocolDecoder.isElmOk("ATZ\r\rELM327 v2.2\r\nOK"))
        assertFalse(ObdProtocolDecoder.isElmOk("UNABLE TO CONNECT"))
        assertFalse(ObdProtocolDecoder.isElmOk("NO DATA"))
        assertFalse(ObdProtocolDecoder.isElmOk("CAN ERROR"))
    }
}
