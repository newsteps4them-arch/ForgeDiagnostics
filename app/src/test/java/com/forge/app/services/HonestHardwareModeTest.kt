// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for honest hardware mode:
 *
 * - An ECU that reports "NO DATA" (reachable, nothing to report) must yield
 *   zero DTCs and must be distinguishable from a blank/timeout transport
 *   failure — neither may produce invented codes.
 * - Negative ELM327 responses ("NO DATA", "ERROR", "?") must never decode into
 *   phantom DTCs (e.g. "P0ODA" from "NO DATA").
 * - Demo/simulated content must always carry [DiagnosticDataSource.SIMULATED];
 *   real hardware parses must carry [DiagnosticDataSource.LIVE_HARDWARE].
 */
class HonestHardwareModeTest {

    private fun newModule() = ObdDiagnosticHardwareModule(
        scope = CoroutineScope(Dispatchers.Unconfined),
        usbHardwareService = null,
        telemetryService = null,
        openManusService = null,
        ioDispatcher = Dispatchers.Unconfined,
        mainDispatcher = Dispatchers.Unconfined
    )

    @Test
    fun parseDtcPayload_noDataMeansZeroDtcs() {
        val module = newModule()
        assertTrue(module.parseDtcPayload("NO DATA", "Confirmed").isEmpty())
        assertTrue(module.parseDtcPayload("SEARCHING...\rNO DATA\r>", "Pending").isEmpty())
    }

    @Test
    fun parseDtcPayload_rejectsNegativeElmResponses() {
        val module = newModule()
        assertTrue(module.parseDtcPayload("?", "Confirmed").isEmpty())
        assertTrue(module.parseDtcPayload("ERROR", "Confirmed").isEmpty())
        // "NO DATA" must never decode into the phantom code "P0ODA".
        val codes = module.parseDtcPayload("NO DATA", "Confirmed").map { it.code }
        assertFalse(codes.contains("P0ODA"))
        assertFalse(codes.any { it.contains("ODA") })
    }

    @Test
    fun parseDtcPayload_rejectsNonHexGarbage() {
        val module = newModule()
        assertTrue(module.parseDtcPayload("ZZZZ", "Confirmed").isEmpty())
        assertTrue(module.parseDtcPayload("", "Confirmed").isEmpty())
        assertTrue(module.parseDtcPayload("43", "Confirmed").isEmpty())
    }

    @Test
    fun parseDtcPayload_decodesRealDtcFrameAsLiveHardware() {
        val module = newModule()
        // Genuine Mode 03 response frame: 43 02 01 33 00 00 -> one code, P0133.
        val dtcs = module.parseDtcPayload("43 02 01 33 00 00", "Confirmed")
        assertEquals(1, dtcs.size)
        assertEquals("P0133", dtcs[0].code)
        assertEquals(DiagnosticDataSource.LIVE_HARDWARE, dtcs[0].dataSource)
    }

    @Test
    fun parseDtcPayload_consumesDtcCountByte() {
        val module = newModule()
        // Real ECU reply "43 02 04 20 03 00": count byte 0x02, then two DTCs.
        // The old decoder ignored the count byte and fabricated P0204/P2003
        // while dropping the real faults — it must decode [P0420, P0300].
        val dtcs = module.parseDtcPayload("43 02 04 20 03 00", "Confirmed")
        assertEquals(listOf("P0420", "P0300"), dtcs.map { it.code })
        assertFalse(dtcs.any { it.code == "P0204" || it.code == "P2003" })
        assertTrue(dtcs.all { it.dataSource == DiagnosticDataSource.LIVE_HARDWARE })
    }

    @Test
    fun parseDtcPayload_zeroCountMeansZeroDtcs() {
        val module = newModule()
        // "43 00": ECU reachable, reporting zero stored faults — not an error,
        // and certainly not an excuse to invent codes from the count byte.
        assertTrue(module.parseDtcPayload("43 00", "Confirmed").isEmpty())
        assertTrue(module.parseDtcPayload("47 00", "Pending").isEmpty())
    }

    @Test
    fun parseDtcPayload_countCappedAtAvailableBytes() {
        val module = newModule()
        // Declared count exceeds the bytes actually present: decode what is
        // there (one real code) without fabricating the missing remainder.
        val dtcs = module.parseDtcPayload("43 05 03 00", "Confirmed")
        assertEquals(listOf("P0300"), dtcs.map { it.code })
    }

    @Test
    fun parseDtcPayload_mode07ConsumesCountByte() {
        val module = newModule()
        // Mode 07 (pending) uses the same count-byte framing as Mode 03.
        val dtcs = module.parseDtcPayload("47 01 04 20", "Pending")
        assertEquals(listOf("P0420"), dtcs.map { it.code })
        assertEquals("Pending", dtcs[0].status)
    }

    @Test
    fun isNoDataResponse_distinguishesEmptyEcuFromTransportFailure() {
        val module = newModule()
        // ECU reachable but nothing to report:
        assertTrue(module.isNoDataResponse("NO DATA"))
        assertTrue(module.isNoDataResponse("SEARCHING...\rNO DATA\r>"))
        // Blank/timeout transport failures are NOT "no data" — they are errors:
        assertFalse(module.isNoDataResponse(""))
        assertFalse(module.isNoDataResponse("   "))
    }

    @Test
    fun simulatedTelemetryIsTaggedSimulated() {
        // The simulation loop must tag its output so the UI can banner it.
        val data = ObdTelemetryData(dataSource = DiagnosticDataSource.SIMULATED)
        assertEquals(DiagnosticDataSource.SIMULATED, data.dataSource)
        // Fresh telemetry defaults to UNKNOWN — never silently "live".
        assertEquals(DiagnosticDataSource.UNKNOWN, ObdTelemetryData().dataSource)
    }

    @Test
    fun dataSourceEnum_coversAllProvenanceStates() {
        assertEquals(3, DiagnosticDataSource.values().size)
        assertTrue(DiagnosticDataSource.values().contains(DiagnosticDataSource.LIVE_HARDWARE))
        assertTrue(DiagnosticDataSource.values().contains(DiagnosticDataSource.SIMULATED))
        assertTrue(DiagnosticDataSource.values().contains(DiagnosticDataSource.UNKNOWN))
    }
}
