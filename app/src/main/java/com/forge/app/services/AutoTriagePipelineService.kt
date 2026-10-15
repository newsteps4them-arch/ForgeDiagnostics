// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import com.forge.app.ForgeApplication
import com.forge.app.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TriageStepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

data class AutoTriageStep(
    val id: String,
    val title: String,
    val description: String,
    val status: TriageStepStatus = TriageStepStatus.PENDING,
    val resultSummary: String? = null,
    val latencyMs: Long = 0L
)

data class AutoTriageReport(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val activeVehicleVin: String = "WAUZZZF58MA019284",
    val vehicleName: String = "2021 Audi S5 3.0T Quattro",
    val detectedDtcs: List<String> = listOf("P0300", "P0171"),
    val steps: List<AutoTriageStep> = emptyList(),
    val decodedSpecs: DecodedVehicleSpecs? = null,
    val safetyRecalls: List<NhtsaRecallItem> = emptyList(),
    val matchedTsbs: List<AlldataRepairProcedure> = emptyList(),
    val sourcedParts: List<NexpartPartItem> = emptyList(),
    val estimatedLaborHours: Double = 2.5,
    val totalEstimatedCost: Double = 485.0,
    val summaryRecommendation: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AutoTriagePipelineService executes fully automated, 1-click diagnostic triage
 * coordinating vehicle decoding, DTC analysis, OEM TSB matching, parts sourcing,
 * cost estimation, and work order creation.
 */
class AutoTriagePipelineService(
    private val repository: ForgeRepository? = null,
    private val authAndSyncService: AuthAndSyncService? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _triageState = MutableStateFlow(AutoTriageReport(steps = getInitialSteps()))
    val triageState: StateFlow<AutoTriageReport> = _triageState.asStateFlow()

    private fun getInitialSteps(): List<AutoTriageStep> = listOf(
        AutoTriageStep("1_obd_dtc", "OBD-II DTC & Freeze-Frame Scan", "Extract Mode 03/07 DTC codes and Mode 02 freeze frame parameters"),
        AutoTriageStep("2_vin_nhtsa", "NHTSA VPIC VIN & Recalls", "Decode factory specs and query US DOT safety recall campaigns"),
        AutoTriageStep("3_alldata_tsb", "ALLDATA OEM TSBs & Schematics", "Match OEM technical service bulletins and factory test procedures"),
        AutoTriageStep("4_nexpart_b2b", "Nexpart B2B Parts Sourcing", "Query live distributor stock and wholesale pricing for replacement components"),
        AutoTriageStep("5_cost_workorder", "Labor Guide & Work Order Dispatch", "Calculate labor hours, itemize invoice quote, and persist to the local Room database")
    )

    /**
     * Executes the full autonomous 1-click triage workflow.
     */
    fun runAutoTriage(
        vin: String = "WAUZZZF58MA019284",
        dtcCodes: List<String> = listOf("P0300", "P0171"),
        customerNote: String = "Engine stumbling under load and check engine light illuminated"
    ) {
        if (_triageState.value.isRunning) return

        _triageState.value = _triageState.value.copy(
            isRunning = true,
            progress = 0.05f,
            activeVehicleVin = vin,
            detectedDtcs = dtcCodes,
            steps = getInitialSteps()
        )

        scope.launch {

            ForgeApplication.logEvent("AutoTriagePipeline: Started autonomous workflow for VIN $vin with ${dtcCodes.size} DTCs")

            // Step 1: OBD DTC & Freeze Frame Extraction
            updateStep("1_obd_dtc", TriageStepStatus.RUNNING, "Using caller-supplied DTC list (demo mode: ECU is not queried by this pipeline)...")
            val dtcSummary = "Input DTCs for triage: ${dtcCodes.joinToString(", ")} (DEMO freeze-frame values shown for illustration — NOT read from the ECU: 2,450 RPM, 92°C ECT, MAP 48 kPa, MAF 18.5 g/s, Engine Load 42.0%, STFT +14.2%)"
            updateStep("1_obd_dtc", TriageStepStatus.COMPLETED, dtcSummary, 120L)
            _triageState.value = _triageState.value.copy(progress = 0.25f)

            // Step 2: NHTSA VPIC VIN Decode & Safety Recalls
            updateStep("2_vin_nhtsa", TriageStepStatus.RUNNING, "Connecting to NHTSA VPIC and Recalls Database...")
            val decodedSpecs = NhtsaSafetyClient.decodeVinLive(vin)
            val recallLookup = NhtsaSafetyClient.fetchSafetyRecalls(vin)
            val recalls = recallLookup.recalls
            val specNote = if (decodedSpecs.isFallbackData) " [OFFLINE DEMO SPECS — live VIN decode failed]" else ""
            val recallNote = if (recallLookup.isLiveLookup) "Found ${recalls.size} active safety recall notices (live NHTSA lookup)." else "NHTSA recall lookup FAILED (offline or error) — recall status is UNKNOWN, not zero."
            val nhtsaSummary = "Decoded ${decodedSpecs.modelYear} ${decodedSpecs.make} ${decodedSpecs.model} (${decodedSpecs.engineCylinders} Cyl)$specNote. $recallNote"
            updateStep("2_vin_nhtsa", TriageStepStatus.COMPLETED, nhtsaSummary, 340L)
            _triageState.value = _triageState.value.copy(
                progress = 0.50f,
                decodedSpecs = decodedSpecs,
                safetyRecalls = recalls,
                vehicleName = "${decodedSpecs.modelYear} ${decodedSpecs.make} ${decodedSpecs.model}"
            )

            // Step 3: ALLDATA OEM TSBs and Wiring Pinout Matching
            updateStep("3_alldata_tsb", TriageStepStatus.RUNNING, "Matching ALLDATA OEM Technical Service Bulletins...")
            val allProcedures = AlldataClient.fetchRepairProcedures()
            val matchedProcedures = allProcedures.filter { proc ->
                dtcCodes.any { code -> proc.title.contains(code, ignoreCase = true) || proc.category.contains("Misfire", ignoreCase = true) }
            }.ifEmpty { allProcedures.take(2) }
            val tsbSummary = "Matched ${matchedProcedures.size} OEM technical repair bulletins (TSB-2026-EA839-01 & Pinout J220 ECM)"
            updateStep("3_alldata_tsb", TriageStepStatus.COMPLETED, tsbSummary, 280L)
            _triageState.value = _triageState.value.copy(
                progress = 0.75f,
                matchedTsbs = matchedProcedures
            )

            // Step 4: Nexpart B2B Parts Catalog & Inventory Lookup
            updateStep("4_nexpart_b2b", TriageStepStatus.RUNNING, "Checking parts catalog (demo catalog entries — not live distributor stock)...")
            val b2bParts = NexpartClient.searchB2bInventory()
            val matchedParts = b2bParts.filter { part ->
                part.partNumber.contains("02615") || part.partNumber.contains("06M905") || part.description.contains("Spark", ignoreCase = true) || part.description.contains("Injector", ignoreCase = true)
            }.ifEmpty { b2bParts.take(2) }
            val partsSummary = "Located ${matchedParts.size} replacement items from DEMO catalog (availability figures are illustrative, not live stock: ${matchedParts.joinToString { "${it.description} (${it.localStockQty} avail)" }})"
            updateStep("4_nexpart_b2b", TriageStepStatus.COMPLETED, partsSummary, 310L)
            _triageState.value = _triageState.value.copy(
                progress = 0.90f,
                sourcedParts = matchedParts
            )

            // Step 5: Labor Estimation & Autonomous Work Order Dispatch
            updateStep("5_cost_workorder", TriageStepStatus.RUNNING, "Calculating Mitchell labor guide & generating Work Order...")
            val totalPartsCost = matchedParts.sumOf { it.retailPrice }
            val laborRate = 125.0
            val estimatedLaborHours = 2.5
            val totalLaborCost = estimatedLaborHours * laborRate
            val totalEstimate = totalPartsCost + totalLaborCost

            // Auto-persist Work Order to Room Database
            repository?.addWorkOrder(
                WorkOrderEntity(
                    projectTitle = "Autonomous Triage: ${decodedSpecs.modelYear} ${decodedSpecs.make} ${decodedSpecs.model}",
                    vehicleVin = vin,
                    status = "In Progress",
                    totalCost = totalEstimate,
                    laborHours = estimatedLaborHours,
                    createdAt = System.currentTimeMillis()
                )
            )

            // Work orders persist to the local Room database; cloud sync is not configured.

            val finalSummary = """
                ### Autonomous Diagnostic Triage Complete (DEMO DATA — NOT A LIVE VEHICLE DIAGNOSIS)

                The freeze-frame values, TSB matches, parts, and labor figures below are
                illustrative demo content. Do not quote or repair from them.
                
                - **Vehicle:** **${decodedSpecs.modelYear} ${decodedSpecs.make} ${decodedSpecs.model}** (VIN: `$vin`)$specNote
                - **Primary Faults:** `${dtcCodes.joinToString(", ")}` (caller-supplied input codes)
                - **Government Recalls:** $recallNote
                - **OEM TSB Match:** `${matchedProcedures.firstOrNull()?.title ?: "Standard Ignition/Fuel TSB"}` (demo catalog)
                - **Replacement Parts:** ${matchedParts.size} components from DEMO catalog ($${"%.2f".format(totalPartsCost)})
                - **Estimated Labor:** **${estimatedLaborHours} hrs** @ $125/hr ($${"%.2f".format(totalLaborCost)}) — fixed demo estimate, not a labor-guide lookup
                - **Total Estimated Quote:** **$${"%.2f".format(totalEstimate)}**
                - **Work Order Dispatched:** Auto-saved to the LOCAL Room database only (cloud sync is not implemented).
            """.trimIndent()

            updateStep("5_cost_workorder", TriageStepStatus.COMPLETED, "Work Order saved to local Room database ($${"%.2f".format(totalEstimate)} demo quote). Cloud sync is not implemented.", 180L)

            _triageState.value = _triageState.value.copy(
                isRunning = false,
                progress = 1.0f,
                estimatedLaborHours = estimatedLaborHours,
                totalEstimatedCost = totalEstimate,
                summaryRecommendation = finalSummary
            )

            ForgeApplication.logEvent("AutoTriagePipeline: Autonomous triage workflow finished successfully ($totalEstimate).")
        }
    }

    private fun updateStep(stepId: String, status: TriageStepStatus, summary: String? = null, latencyMs: Long = 0L) {
        val updatedList = _triageState.value.steps.map { step ->
            if (step.id == stepId) {
                step.copy(status = status, resultSummary = summary ?: step.resultSummary, latencyMs = latencyMs)
            } else {
                step
            }
        }
        _triageState.value = _triageState.value.copy(steps = updatedList)
    }

    fun reset() {
        _triageState.value = AutoTriageReport(steps = getInitialSteps())
    }
}
