// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OpenManusSwarmTest {

    private lateinit var agentService: OpenManusAgentService

    @Before
    fun setup() {
        agentService = OpenManusAgentService()
    }

    @Test
    fun testExtractDeepSeekThinking() {
        val rawResponseWithThink = """
            <think>
            The vehicle reports P0171 and P0174 lean codes simultaneously on Bank 1 and Bank 2.
            This points strongly to unmetered air entering after the MAF sensor, such as an intake boot tear or PCV valve failure.
            Fuel pressure is within normal range (50 PSI).
            </think>
            ### Diagnostic Conclusion
            Inspect the intake boot between MAF and throttle body. Replace cracked PCV hose.
        """.trimIndent()

        val (thinking, finalAnswer) = agentService.extractDeepSeekThinking(rawResponseWithThink)

        assertTrue(thinking.contains("P0171 and P0174"))
        assertTrue(thinking.contains("unmetered air"))
        assertTrue(finalAnswer.startsWith("### Diagnostic Conclusion"))
        assertFalse(finalAnswer.contains("<think>"))
        assertFalse(finalAnswer.contains("</think>"))
    }

    @Test
    fun testExtractDeepSeekThinkingWithoutTags() {
        val standardText = "Direct root cause: Worn spark plug gap on cylinder 3."
        val (thinking, finalAnswer) = agentService.extractDeepSeekThinking(standardText)

        assertEquals("", thinking)
        assertEquals(standardText, finalAnswer)
    }

    @Test
    fun testVolumetricEfficiencyMath() {
        // Test 3.0L engine at 6000 RPM (WOT) with 160.0 g/s MAF and 20°C IAT
        val ve = agentService.calculateVolumetricEfficiency(
            mafGps = 160.0,
            rpm = 6000,
            displacementLiters = 3.0,
            iatCelsius = 20.0
        )

        // Expected WOT VE around 85% - 95%
        assertTrue("Calculated WOT VE ($ve%) should be in normal naturally aspirated range", ve in 80.0..98.0)

        // Test boundary conditions (0 RPM, negative displacement)
        assertEquals(0.0, agentService.calculateVolumetricEfficiency(160.0, 0, 3.0, 20.0), 0.01)
        assertEquals(0.0, agentService.calculateVolumetricEfficiency(160.0, 6000, 0.0, 20.0), 0.01)
    }

    @Test
    fun testAcousticHarmonicOrders() {
        val engineRpm = 750 // Crankshaft fundamental = 12.5 Hz

        // Camshaft tick (0.5x order = 6.25 Hz)
        val camOrder = agentService.calculateAcousticDominantHarmonic(engineRpm, 6.25)
        assertTrue(camOrder.contains("0.5x"))
        assertTrue(camOrder.contains("Valvetrain") || camOrder.contains("Camshaft"))

        // Rod knock / main bearing (1.0x order = 12.5 Hz)
        val rodOrder = agentService.calculateAcousticDominantHarmonic(engineRpm, 12.5)
        assertTrue(rodOrder.contains("1.0x"))
        assertTrue(rodOrder.contains("Crankshaft") || rodOrder.contains("Rod Knock"))

        // 4-Cylinder Firing Pulse (2.0x order = 25.0 Hz)
        val firingOrder = agentService.calculateAcousticDominantHarmonic(engineRpm, 25.0)
        assertTrue(firingOrder.contains("2.0x"))

        // 6-Cylinder Firing Pulse (3.0x order = 37.5 Hz)
        val v6Order = agentService.calculateAcousticDominantHarmonic(engineRpm, 37.5)
        assertTrue(v6Order.contains("3.0x"))

        // 8-Cylinder Firing Pulse (4.0x order = 50.0 Hz)
        val v8Order = agentService.calculateAcousticDominantHarmonic(engineRpm, 50.0)
        assertTrue(v8Order.contains("4.0x"))
    }

    @Test
    fun testSwarmExecutionForDifferentFaultTypes() = runBlocking {
        // Test CAN Bus Fault
        agentService.runAutonomousDiagnosis(
            goal = "Diagnose U0100 Lost Communication With ECM/PCM",
            vehicleContext = "2022 Ford F-150",
            activeDtcs = listOf("U0100")
        )
        val canReport = agentService.state.value.finalReport
        assertNotNull(canReport)
        assertTrue(canReport!!.primaryRootCause.contains("CAN Bus", ignoreCase = true) || canReport.primaryRootCause.contains("Impedance", ignoreCase = true))

        // Test Turbocharger Fault
        agentService.runAutonomousDiagnosis(
            goal = "Diagnose P0299 Turbocharger Underboost Condition",
            vehicleContext = "2020 Volkswagen Golf GTI",
            activeDtcs = listOf("P0299")
        )
        val turboReport = agentService.state.value.finalReport
        assertNotNull(turboReport)
        assertTrue(turboReport!!.primaryRootCause.contains("Turbocharger", ignoreCase = true) || turboReport.primaryRootCause.contains("Wastegate", ignoreCase = true))
    }
}
