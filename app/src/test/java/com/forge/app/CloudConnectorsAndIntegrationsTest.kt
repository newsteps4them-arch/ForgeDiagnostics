// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app

import com.forge.app.services.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CloudConnectorsAndIntegrationsTest {

    @Test
    fun testCloudConnectorsManagerInitialization() {
        val manager = CloudConnectorsManager()
        val state = manager.hubState.value

        assertEquals("ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9", state.firestoreDbId)
        assertEquals("ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9", state.googleCloudProjectId)
        assertEquals(9, state.totalActiveConnectors)
        assertTrue("All connectors should be initialized", state.connectors.isNotEmpty())
        // Honest initial states: nothing is "healthy" until a real check runs,
        // and Firestore is honestly reported as not integrated.
        val firestoreInit = state.connectors.firstOrNull { it.id == "firebase_firestore" }
        assertEquals(ConnectorStatus.DISCONNECTED, firestoreInit?.status)
        val geminiInit = state.connectors.firstOrNull { it.id == "google_gemini_api" }
        assertEquals(ConnectorStatus.STANDBY, geminiInit?.status)

        val geminiConn = state.connectors.firstOrNull { it.id == "google_gemini_api" }
        assertNotNull("Gemini API connector must exist", geminiConn)
        assertEquals("AI & CLOUD", geminiConn?.category)

        val firestoreConn = state.connectors.firstOrNull { it.id == "firebase_firestore" }
        assertNotNull("Firestore connector must exist", firestoreConn)

        val nhtsaConn = state.connectors.firstOrNull { it.id == "nhtsa_safety_recalls" }
        assertNotNull("NHTSA recalls connector must exist", nhtsaConn)

        val alldataConn = state.connectors.firstOrNull { it.id == "alldata_oem" }
        assertNotNull("ALLDATA connector must exist", alldataConn)
    }

    @Test
    fun testNhtsaVinDecodingAndRecallsFallback() = runBlocking {
        val testVin = "WAUZZZF58MA019284"
        val specs = NhtsaSafetyClient.decodeVinLive(testVin)

        assertNotNull(specs)
        assertEquals(testVin, specs.vin)
        assertTrue(specs.make.isNotBlank())
        assertTrue(specs.model.isNotBlank())
        assertTrue(specs.modelYear.isNotBlank())

        val lookup = NhtsaSafetyClient.fetchSafetyRecalls(testVin)
        assertNotNull(lookup)
        // Honest contract: a failed/offline recall lookup must be empty —
        // recall campaigns must NEVER be fabricated as a fallback.
        if (!lookup.isLiveLookup) {
            assertTrue("Offline recall lookup must be empty, never fabricated", lookup.recalls.isEmpty())
        } else {
            assertTrue(lookup.recalls.all { it.nhtsaCampaignNumber.isNotBlank() && it.summary.isNotBlank() })
        }
    }

    @Test
    fun testCloudConnectorsFullHealthCheck() = runBlocking {
        val manager = CloudConnectorsManager()
        manager.runFullSystemHealthCheck()

        val state = manager.hubState.value
        assertTrue("Healthy count should be greater than 0", state.healthyCount > 0)
        state.connectors.forEach { connector ->
            assertTrue("Latency should be non-negative", connector.latencyMs >= 0)
            assertTrue("Details should not be blank", connector.details.isNotBlank())
        }
    }
}
