package com.forge.app

import com.forge.app.services.ObdTelemetryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ObdTelemetryServiceTest(
    private val description: String,
    private val rawResponse: String,
    private val expectedRpm: Int?
) {

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                // Valid cases
                arrayOf("Valid response with spaces", "41 0C 1A F8", 1726),
                arrayOf("Valid response without spaces", "410C1AF8", 1726),
                arrayOf("Valid response with multiple spaces", "41   0C  1A  F8", 1726),
                arrayOf("Valid response with carriage return", "41 0C 1A F8\r", 1726),
                arrayOf("Valid response with line feed", "41 0C 1A F8\n", 1726),
                arrayOf("Valid response with both CR and LF", "41 0C 1A F8\r\n", 1726),
                arrayOf("Valid response idle RPM", "41 0C 0C 80", 800),
                arrayOf("Valid response zero RPM", "41 0C 00 00", 0),

                // Invalid or missing prefixes
                arrayOf("Empty string", "", null),
                arrayOf("Blank string", "   ", null),
                arrayOf("Missing prefix", "1A F8", null),
                arrayOf("Wrong prefix (Speed instead of RPM)", "41 0D 1A F8", null),
                arrayOf("Only prefix, no data", "41 0C", null),

                // Malformed data
                arrayOf("Incomplete data (only A part)", "41 0C 1A", null),
                arrayOf("Invalid hex characters", "41 0C XX YY", null),
                arrayOf("Malformed spacing (still parsable)", "4 1 0 C 1 A F 8", 1726),

                // Edge cases
                arrayOf("Garbage before actual response", "SEARCHING...\n41 0C 1A F8", 1726),
                arrayOf("Response mixed with OK", "41 0C 1A F8\r\nOK", 1726)
            )
        }
    }

    @Test
    fun testParseRpmResponse() {
        val scope = CoroutineScope(Dispatchers.Unconfined + Job())
        val service = ObdTelemetryService(scope, null)

        val actualRpm = service.parseRpmResponse(rawResponse)
        assertEquals("Failed test case: $description", expectedRpm, actualRpm)
    }
}
