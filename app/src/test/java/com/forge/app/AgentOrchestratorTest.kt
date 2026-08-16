package com.forge.app

import com.forge.app.data.VehicleEntity
import com.forge.app.services.AgentActivityStatus
import com.forge.app.services.AgentOrchestratorState
import com.forge.app.services.ForgeAgentType
import org.junit.Assert.*
import org.junit.Test

class AgentOrchestratorTest {

    @Test
    fun testAllSwarmAgentsConfigured() {
        val state = AgentOrchestratorState()
        assertEquals(6, state.activeAgents.size)

        val agentIds = state.activeAgents.map { it.agentType.id }
        assertTrue(agentIds.contains("agent_frontend_ui"))
        assertTrue(agentIds.contains("agent_client_hw"))
        assertTrue(agentIds.contains("agent_middleware_telemetry"))
        assertTrue(agentIds.contains("agent_backend_sync"))
        assertTrue(agentIds.contains("agent_server_ai"))
        assertTrue(agentIds.contains("agent_jules_dev"))
    }

    @Test
    fun testAgentOrchestratorStateModel() {
        val vehicle = VehicleEntity(
            vin = "1G1YC2D70R5100999",
            make = "Chevrolet",
            model = "Corvette Z06 (C8)",
            year = "2024"
        )
        val vehicleLabel = "${vehicle.year} ${vehicle.make} ${vehicle.model} (${vehicle.vin.takeLast(6)})"

        val state = AgentOrchestratorState(
            globalContextVehicle = vehicleLabel,
            globalContextDtcCount = 2,
            systemLog = listOf("[ORCHESTRATOR] Global Context assigned to $vehicleLabel")
        )

        assertEquals("2024 Chevrolet Corvette Z06 (C8) (100999)", state.globalContextVehicle)
        assertEquals(2, state.globalContextDtcCount)
        assertTrue(state.systemLog.any { it.contains("Corvette Z06") })
    }

    @Test
    fun testAgentActivityStatusTransitions() {
        val status = AgentActivityStatus(
            agentType = ForgeAgentType.MiddlewareTelemetryAgent,
            isActive = true,
            currentTask = "Streaming PID 010C/010D",
            processedCount = 1450L,
            lastResponseTimeMs = 1700000000000L
        )

        assertEquals("agent_middleware_telemetry", status.agentType.id)
        assertEquals("Middleware Telemetry Bridge", status.agentType.displayName)
        assertTrue(status.isActive)
        assertEquals(1450L, status.processedCount)
    }
}
