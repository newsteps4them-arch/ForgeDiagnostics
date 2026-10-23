// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DtcExplanationServiceTest {

    @Test
    fun testExplainPrimaryCodes_P0300() = runTest {
        val explanation = DtcExplanationService.explainDtc("P0300")
        assertEquals("P0300", explanation.code)
        assertEquals("Random or Multiple Cylinder Misfire Detected", explanation.standardTitle)
        assertEquals(DtcSeverity.CRITICAL, explanation.severity)
        assertTrue(explanation.systemCategory.contains("Powertrain"))
        assertTrue(explanation.isSafeToDrive.startsWith("NO"))
        assertTrue(explanation.commonSymptoms.isNotEmpty())
        assertTrue(explanation.probableCauses.isNotEmpty())
        assertTrue(explanation.diagnosticSteps.isNotEmpty())
    }

    @Test
    fun testExplainPrimaryCodes_P0171() = runTest {
        val explanation = DtcExplanationService.explainDtc("p0171")
        assertEquals("P0171", explanation.code)
        assertEquals("System Too Lean (Bank 1)", explanation.standardTitle)
        assertEquals(DtcSeverity.HIGH, explanation.severity)
        assertTrue(explanation.systemCategory.contains("Powertrain"))
        assertTrue(explanation.isSafeToDrive.contains("CAUTION"))
    }

    @Test
    fun testExplainExpandedCylinderMisfireCodes() = runTest {
        val codes = listOf("P0301", "P0302", "P0303", "P0304")
        for ((idx, code) in codes.withIndex()) {
            val cyl = idx + 1
            val explanation = DtcExplanationService.explainDtc(code)
            assertEquals(code, explanation.code)
            assertEquals("Cylinder $cyl Misfire Detected", explanation.standardTitle)
            assertEquals(DtcSeverity.CRITICAL, explanation.severity)
            assertTrue(explanation.laymanSummary.contains("Cylinder $cyl"))
        }
    }

    @Test
    fun testExplainExpandedSystemFaults() = runTest {
        // P0172
        val p0172 = DtcExplanationService.explainDtc("P0172")
        assertEquals("P0172", p0172.code)
        assertEquals("System Too Rich (Bank 1)", p0172.standardTitle)

        // P0101
        val p0101 = DtcExplanationService.explainDtc("P0101")
        assertEquals("P0101", p0101.code)
        assertTrue(p0101.standardTitle.contains("Mass Air Flow"))

        // P0335
        val p0335 = DtcExplanationService.explainDtc("P0335")
        assertEquals("P0335", p0335.code)
        assertEquals(DtcSeverity.CRITICAL, p0335.severity)

        // U0100
        val u0100 = DtcExplanationService.explainDtc("U0100")
        assertEquals("U0100", u0100.code)
        assertTrue(u0100.systemCategory.contains("Network Communication"))

        // C0035
        val c0035 = DtcExplanationService.explainDtc("C0035")
        assertEquals("C0035", c0035.code)
        assertTrue(c0035.systemCategory.contains("Chassis"))
    }

    @Test
    fun testSaeJ2012CategoryDecoding() {
        // SAE Standardized vs OEM Specific
        assertTrue(DtcExplanationService.getSaeJ2012Category("P0100").contains("SAE Standardized"))
        assertTrue(DtcExplanationService.getSaeJ2012Category("P1340").contains("OEM / Manufacturer Specific"))
        assertTrue(DtcExplanationService.getSaeJ2012Category("C0035").contains("Chassis"))
        assertTrue(DtcExplanationService.getSaeJ2012Category("B0001").contains("Body"))
        assertTrue(DtcExplanationService.getSaeJ2012Category("U0100").contains("Network Communication"))
    }

    @Test
    fun testGenericArbitraryDtcFallback() = runTest {
        val customCode = "P0999"
        val explanation = DtcExplanationService.explainDtc(customCode)
        assertEquals("P0999", explanation.code)
        assertEquals("Standard Diagnostic Trouble Code (P0999)", explanation.standardTitle)
        assertTrue(explanation.systemCategory.contains("Powertrain"))
        assertTrue(explanation.laymanSummary.contains("P0999"))
    }

    @Test
    fun testExplainMultipleDtcs_EmptyAndPopulated() = runTest {
        // Empty DTC list
        val emptyResult = DtcExplanationService.explainMultipleDtcs(emptyList())
        assertTrue(emptyResult.contains("Zero diagnostic trouble codes"))

        // Multiple DTC list
        val dtcList = listOf(
            DtcInfo("P0171", "System Too Lean Bank 1", "Stored"),
            DtcInfo("P0300", "Random Misfire Detected", "Pending")
        )
        val auditResult = DtcExplanationService.explainMultipleDtcs(dtcList)
        assertTrue(auditResult.contains("Team Forge Combined DTC Diagnostic Audit"))
        assertTrue(auditResult.contains("P0171"))
        assertTrue(auditResult.contains("P0300"))
    }
}
