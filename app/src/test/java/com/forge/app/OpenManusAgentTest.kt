// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app

import com.forge.app.services.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OpenManusAgentTest {

    private lateinit var openManusService: OpenManusAgentService

    @Before
    fun setup() {
        openManusService = OpenManusAgentService()
    }

    @Test
    fun testInitialState() {
        val state = openManusService.state.value
        assertFalse(state.isRunning)
        assertEquals(AgentExecutionPhase.IDLE, state.currentPhase)
        assertTrue(state.activeTools.contains("obd_pid"))
        assertTrue(state.activeTools.contains("can_uds"))
        assertTrue(state.activeTools.contains("electrical_circuit"))
        assertTrue(state.activeTools.contains("python_math"))
        assertTrue(state.activeTools.contains("nhtsa_tsb"))
    }

    @Test
    fun testProviderAndToolConfiguration() {
        openManusService.setModelProvider(AgentModelProvider.LOCAL_OLLAMA)
        assertEquals(AgentModelProvider.LOCAL_OLLAMA, openManusService.state.value.selectedProvider)

        openManusService.setCustomEndpoint("http://192.168.1.100:11434", "llama3.2")
        assertEquals("http://192.168.1.100:11434", openManusService.state.value.customEndpointUrl)
        assertEquals("llama3.2", openManusService.state.value.customModelName)

        // Toggle tool off and on
        openManusService.toggleTool("obd_pid")
        assertFalse(openManusService.state.value.activeTools.contains("obd_pid"))

        openManusService.toggleTool("obd_pid")
        assertTrue(openManusService.state.value.activeTools.contains("obd_pid"))
    }

    @Test
    fun testAutonomousDiagnosticLoopExecution() = runBlocking {
        openManusService.runAutonomousDiagnosis(
            goal = "Diagnose P0300 Random Misfire with positive fuel trims",
            vehicleContext = "2021 Audi S5 Sportback",
            activeDtcs = listOf("P0300", "P0171"),
            telemetrySummary = "RPM=750, Temp=92C, Voltage=14.1V"
        )

        val state = openManusService.state.value
        assertFalse(state.isRunning)
        assertEquals(AgentExecutionPhase.COMPLETED, state.currentPhase)
        assertTrue(state.steps.isNotEmpty())
        assertEquals(4, state.steps.size)

        // Check Step 1 Coordinator Planning
        assertEquals("Manus Master Coordinator", state.steps[0].agentName)
        assertEquals("Decompose & Strategy", state.steps[0].phase)

        // Check Step 2 Tool Invocations
        assertEquals("AutoOBD & CAN-Bus Specialist", state.steps[1].agentName)
        assertTrue(state.steps[1].toolInvocations.any { it.toolName == "AutoOBD_PID_Decoder" })
        assertTrue(state.steps[1].toolInvocations.any { it.toolName == "CAN_UDS_Protocol_Analyzer" })

        // Check Step 3 Physics & Simulation Invocations
        assertEquals("Electrical & Math Simulation Agent", state.steps[2].agentName)
        assertTrue(state.steps[2].toolInvocations.any { it.toolName == "Electrical_Circuit_Solver" })
        assertTrue(state.steps[2].toolInvocations.any { it.toolName == "Python_Physics_Simulation_Sandbox" })

        // Check Final Report Synthesis
        val report = state.finalReport
        assertNotNull(report)
        assertTrue(report!!.confidenceScore >= 80)
        assertTrue(report.primaryRootCause.isNotBlank())
        assertTrue(report.stepByStepInspectionPlan.isNotEmpty())
        assertTrue(report.recommendedParts.isNotEmpty())
        assertTrue(report.estimatedLaborHours > 0)
    }
}
