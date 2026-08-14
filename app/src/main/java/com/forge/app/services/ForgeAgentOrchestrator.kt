package com.forge.app.services

import android.content.Context
import com.forge.app.data.ForgeRepository
import com.forge.app.data.VehicleEntity
import com.forge.app.data.WorkOrderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Team Forge Multi-Tier Specialized Agent Architecture
 */
sealed class ForgeAgentType(val id: String, val displayName: String, val role: String) {
    object FrontendUiAgent : ForgeAgentType("agent_frontend_ui", "UI & Microfrontend Agent", "Manages Compose Micro-views, topology layouts & responsive UI state")
    object ClientHardwareAgent : ForgeAgentType("agent_client_hw", "Client Hardware Agent", "Parses raw USB/OBD-II CAN frames and manages local sensor polling")
    object MiddlewareTelemetryAgent : ForgeAgentType("agent_middleware_telemetry", "Middleware Telemetry Bridge", "Routes real-time PID streams to diagnostic screens & AI context buffers")
    object BackendSyncAgent : ForgeAgentType("agent_backend_sync", "Backend & Persistence Agent", "Handles Room SQLite transactions, Firestore cloud sync & offline queues")
    object ServerAiAgent : ForgeAgentType("agent_server_ai", "Server AI Specialist", "Executes Gemini Multimodal diagnostics, DTC root-cause analysis & parts identification")
}

data class AgentActivityStatus(
    val agentType: ForgeAgentType,
    val isActive: Boolean = true,
    val currentTask: String = "Idle",
    val processedCount: Long = 0L,
    val lastResponseTimeMs: Long = 0L
)

data class AgentOrchestratorState(
    val activeAgents: List<AgentActivityStatus> = listOf(
        AgentActivityStatus(ForgeAgentType.FrontendUiAgent, true, "Rendering Compose Microfrontends"),
        AgentActivityStatus(ForgeAgentType.ClientHardwareAgent, true, "Monitoring USB Serial / CAN Bus"),
        AgentActivityStatus(ForgeAgentType.MiddlewareTelemetryAgent, true, "Routing OBD-II Telemetry Stream"),
        AgentActivityStatus(ForgeAgentType.BackendSyncAgent, true, "Syncing Room DB & Firestore"),
        AgentActivityStatus(ForgeAgentType.ServerAiAgent, true, "Gemini Pro / Flash Ready")
    ),
    val globalContextVehicle: String = "No Active Vehicle Selected",
    val globalContextDtcCount: Int = 0,
    val systemLog: List<String> = listOf("Forge Multi-Agent Orchestrator Initialized")
)

class ForgeAgentOrchestrator(
    private val scope: CoroutineScope,
    private val repository: ForgeRepository,
    private val usbHardwareService: UsbHardwareCommunicationService?,
    private val telemetryService: ObdTelemetryService,
    private val authAndSyncService: AuthAndSyncService,
    private val geminiService: GeminiService
) {

    private val _orchestratorState = MutableStateFlow(AgentOrchestratorState())
    val orchestratorState: StateFlow<AgentOrchestratorState> = _orchestratorState.asStateFlow()

    init {
        observeHardwareAndTelemetry()
    }

    private fun observeHardwareAndTelemetry() {
        scope.launch(Dispatchers.IO) {
            telemetryService.telemetry.collect { telemetryData ->
                val dtcCount = telemetryData.activeDtcCodes.size
                updateAgentTask(ForgeAgentType.ClientHardwareAgent, "Processing ${telemetryData.connectionType} (${telemetryData.rpm} RPM)")
                updateAgentTask(ForgeAgentType.MiddlewareTelemetryAgent, "Streaming PID 010C/010D • DTCs: $dtcCount")

                val updatedLog = _orchestratorState.value.systemLog.toMutableList()
                if (dtcCount > _orchestratorState.value.globalContextDtcCount) {
                    updatedLog.add("[CLIENT HW AGENT] Detected new DTC flag from vehicle ECU")
                }

                _orchestratorState.value = _orchestratorState.value.copy(
                    globalContextDtcCount = dtcCount,
                    systemLog = updatedLog.takeLast(50)
                )
            }
        }
    }

    /**
     * Set active vehicle context across all agents
     */
    fun setActiveVehicleContext(vehicle: VehicleEntity) {
        val label = "${vehicle.year} ${vehicle.make} ${vehicle.model} (${vehicle.vin.takeLast(6)})"
        _orchestratorState.value = _orchestratorState.value.copy(
            globalContextVehicle = label
        )

        updateAgentTask(ForgeAgentType.FrontendUiAgent, "Context updated: $label")
        updateAgentTask(ForgeAgentType.ServerAiAgent, "Vehicle telemetry buffer synchronized for $label")
        addLog("[ORCHESTRATOR] Global Context assigned to $label")
    }

    /**
     * Execute full multi-agent diagnostic audit on a problem description or DTC list
     */
    suspend fun executeMultiAgentDiagnosticAudit(
        problemDescription: String,
        activeDtcs: List<String>
    ): String {
        updateAgentTask(ForgeAgentType.ServerAiAgent, "Analyzing fault codes with Gemini AI...")
        val startTime = System.currentTimeMillis()

        val vehicleInfo = _orchestratorState.value.globalContextVehicle
        val prompt = """
            [MULTI-AGENT HARDWARE DIAGNOSTIC SUITE]
            Active Vehicle Context: $vehicleInfo
            Stored DTCs: ${activeDtcs.joinToString(", ").ifEmpty { "None" }}
            Customer Reported Issue: $problemDescription
            
            Synthesize a diagnostic action plan:
            1. Primary Root Cause Analysis
            2. High-priority Sensor/Oscilloscope Test Points
            3. Recommended Replacement Parts with Estimated Labor Hours
        """.trimIndent()

        val aiResult = geminiService.generateDiagnosticAnalysis(prompt)
        val elapsed = System.currentTimeMillis() - startTime

        updateAgentTask(ForgeAgentType.ServerAiAgent, "Diagnostic Audit Complete (${elapsed}ms)")
        addLog("[SERVER AI AGENT] Generated root-cause plan in ${elapsed}ms")

        return aiResult
    }

    private fun updateAgentTask(agentType: ForgeAgentType, task: String) {
        val currentAgents = _orchestratorState.value.activeAgents.map { status ->
            if (status.agentType.id == agentType.id) {
                status.copy(
                    currentTask = task,
                    processedCount = status.processedCount + 1,
                    lastResponseTimeMs = System.currentTimeMillis()
                )
            } else {
                status
            }
        }
        _orchestratorState.value = _orchestratorState.value.copy(activeAgents = currentAgents)
    }

    private fun addLog(message: String) {
        val updated = _orchestratorState.value.systemLog.toMutableList()
        updated.add(message)
        _orchestratorState.value = _orchestratorState.value.copy(
            systemLog = updated.takeLast(50)
        )
    }
}
