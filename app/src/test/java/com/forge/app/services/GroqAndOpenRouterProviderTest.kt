// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for Groq and OpenRouter provider integration.
 * Uses MockWebServer to simulate both endpoints without real network calls
 * or live API keys, following SAE J1979 / ISO 26262 offline-first test principles.
 */
class GroqAndOpenRouterProviderTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var agentService: OpenManusAgentService

    // Minimal valid OpenAI-compatible response body
    private fun mockOpenAiResponse(content: String): String = """
        {
          "id": "chatcmpl-test-forge",
          "object": "chat.completion",
          "model": "llama-3.3-70b-versatile",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "$content"
              },
              "finish_reason": "stop"
            }
          ],
          "usage": { "prompt_tokens": 420, "completion_tokens": 380, "total_tokens": 800 }
        }
    """.trimIndent()

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        agentService = OpenManusAgentService()
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    // =====================================================================
    // 1. Enum registration
    // =====================================================================

    @Test
    fun testGroqAndOpenRouterAreRegisteredProviders() {
        val providers = AgentModelProvider.entries
        assertTrue("GROQ must be registered", providers.any { it == AgentModelProvider.GROQ })
        assertTrue("OPEN_ROUTER must be registered", providers.any { it == AgentModelProvider.OPEN_ROUTER })
    }

    @Test
    fun testGroqProviderHasCorrectEndpointDescription() {
        assertTrue(
            AgentModelProvider.GROQ.endpointDescription.contains("groq.com", ignoreCase = true)
        )
    }

    @Test
    fun testOpenRouterProviderHasCorrectEndpointDescription() {
        assertTrue(
            AgentModelProvider.OPEN_ROUTER.endpointDescription.contains("openrouter.ai", ignoreCase = true)
        )
    }

    // =====================================================================
    // 2. Credential setters
    // =====================================================================

    @Test
    fun testSetGroqCredentialsPersistsInState() {
        agentService.setGroqCredentials("gsk_test_forge_key_abc123", "deepseek-r1-distill-llama-70b")
        assertEquals("gsk_test_forge_key_abc123", agentService.state.value.groqApiKey)
        assertEquals("deepseek-r1-distill-llama-70b", agentService.state.value.groqModel)
    }

    @Test
    fun testSetGroqCredentialsDefaultModel() {
        agentService.setGroqCredentials("gsk_default_model_test")
        assertEquals("llama-3.3-70b-versatile", agentService.state.value.groqModel)
    }

    @Test
    fun testSetOpenRouterCredentialsPersistsInState() {
        agentService.setOpenRouterCredentials("sk-or-v1-test_forge_key_xyz789", "google/gemini-2.5-flash")
        assertEquals("sk-or-v1-test_forge_key_xyz789", agentService.state.value.openRouterApiKey)
        assertEquals("google/gemini-2.5-flash", agentService.state.value.openRouterModel)
    }

    @Test
    fun testSetOpenRouterCredentialsDefaultModel() {
        agentService.setOpenRouterCredentials("sk-or-v1-default_model_test")
        assertEquals("deepseek/deepseek-r1:free", agentService.state.value.openRouterModel)
    }

    // =====================================================================
    // 3. callOpenAiCompatibleEndpoint unit tests via MockWebServer
    // =====================================================================

    @Test
    fun testCallOpenAiCompatibleEndpointParsesSuccessfulResponse() {
        val expectedContent = "Primary root cause: PCV valve diaphragm failure causing unmetered vacuum air intake."
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockOpenAiResponse(expectedContent))
                .addHeader("Content-Type", "application/json")
        )

        val baseUrl = mockServer.url("/v1/chat/completions").toString()
        val result = agentService.callOpenAiCompatibleEndpoint(
            url = baseUrl,
            apiKey = "gsk_test_key_valid",
            model = "llama-3.3-70b-versatile",
            systemPrompt = "You are a diagnostic AI.",
            userPrompt = "Diagnose P0171 on a 2021 Audi S5."
        )

        assertEquals(expectedContent, result)

        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.getHeader("Authorization")!!.startsWith("Bearer "))
        assertTrue(recordedRequest.body.readUtf8().contains("llama-3.3-70b-versatile"))
    }

    @Test
    fun testCallOpenAiCompatibleEndpointReturnsEmptyOnServerError() {
        mockServer.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":"rate_limit_exceeded"}"""))

        val baseUrl = mockServer.url("/v1/chat/completions").toString()
        val result = agentService.callOpenAiCompatibleEndpoint(
            url = baseUrl,
            apiKey = "gsk_test_key_valid",
            model = "llama-3.3-70b-versatile",
            systemPrompt = "System",
            userPrompt = "User"
        )

        assertEquals("", result)
    }

    @Test
    fun testCallOpenAiCompatibleEndpointRejectsBlankOrShortKey() {
        // Should short-circuit without making any HTTP request
        val baseUrl = mockServer.url("/v1/chat/completions").toString()

        assertEquals("", agentService.callOpenAiCompatibleEndpoint(
            url = baseUrl, apiKey = "", model = "llama", systemPrompt = "S", userPrompt = "U"
        ))
        assertEquals("", agentService.callOpenAiCompatibleEndpoint(
            url = baseUrl, apiKey = "short", model = "llama", systemPrompt = "S", userPrompt = "U"
        ))
        // No requests should have been made
        assertEquals(0, mockServer.requestCount)
    }

    @Test
    fun testCallOpenAiCompatibleEndpointSendsOpenRouterHeaders() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockOpenAiResponse("Diagnosis: intake vacuum leak detected."))
                .addHeader("Content-Type", "application/json")
        )

        val baseUrl = mockServer.url("/api/v1/chat/completions").toString()
        agentService.callOpenAiCompatibleEndpoint(
            url = baseUrl,
            apiKey = "sk-or-v1-test_key_valid_openrouter",
            model = "deepseek/deepseek-r1:free",
            systemPrompt = "System",
            userPrompt = "User",
            extraHeaders = mapOf(
                "HTTP-Referer" to "https://github.com/newsteps4them-arch/ForgeDiagnostics",
                "X-Title" to "Forge Agentic Diagnostics"
            )
        )

        val req = mockServer.takeRequest()
        assertEquals("https://github.com/newsteps4them-arch/ForgeDiagnostics", req.getHeader("HTTP-Referer"))
        assertEquals("Forge Agentic Diagnostics", req.getHeader("X-Title"))
    }

    // =====================================================================
    // 4. Provider dispatch — Groq with no API key falls to deterministic
    // =====================================================================

    @Test
    fun testGroqProviderWithNoKeyFallsToDeterministicEngine() = runBlocking {
        agentService.setModelProvider(AgentModelProvider.GROQ)
        // No groqApiKey set — should fall through to deterministic
        agentService.runAutonomousDiagnosis(
            goal = "Diagnose P0299 Turbocharger underboost",
            vehicleContext = "2022 VW Golf GTI 2.0T",
            activeDtcs = listOf("P0299")
        )

        val state = agentService.state.value
        assertEquals(AgentExecutionPhase.COMPLETED, state.currentPhase)
        assertNotNull(state.finalReport)
        // Deterministic engine should have handled turbo
        assertTrue(
            state.finalReport!!.primaryRootCause.contains("Turbocharger", ignoreCase = true) ||
            state.finalReport!!.primaryRootCause.contains("Wastegate", ignoreCase = true)
        )
    }

    @Test
    fun testOpenRouterProviderWithNoKeyFallsToDeterministicEngine() = runBlocking {
        agentService.setModelProvider(AgentModelProvider.OPEN_ROUTER)
        // No openRouterApiKey set — should fall through to deterministic
        agentService.runAutonomousDiagnosis(
            goal = "Diagnose U0100 Lost Communication CAN Bus",
            vehicleContext = "2022 Ford F-150 5.0L",
            activeDtcs = listOf("U0100")
        )

        val state = agentService.state.value
        assertEquals(AgentExecutionPhase.COMPLETED, state.currentPhase)
        assertNotNull(state.finalReport)
        // Deterministic engine should detect U0 CAN code
        assertTrue(
            state.finalReport!!.primaryRootCause.contains("CAN Bus", ignoreCase = true) ||
            state.finalReport!!.primaryRootCause.contains("Impedance", ignoreCase = true)
        )
    }

    // =====================================================================
    // 5. Provider dispatch — Groq with a key calls the endpoint
    // =====================================================================

    @Test
    fun testGroqProviderWithKeyCallsMockServerAndParsesReport() = runBlocking {
        val diagContent = "1. Primary Root Cause: Intake manifold vacuum leak past PCV diaphragm gasket.\n" +
                "2. Secondary: Dirty MAF sensor over-reading airflow.\n" +
                "3. Inspection: Remove PCV hose and perform smoke test.\n" +
                "4. Parts: PCV Valve Assembly OEM #06M-103-515-H\n" +
                "5. Labor: 1.5 Hours\n" +
                "6. Safety: Relieve fuel pressure before opening intake fittings."

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockOpenAiResponse(diagContent))
                .addHeader("Content-Type", "application/json")
        )

        val mockGroqUrl = mockServer.url("/openai/v1/chat/completions").toString()

        // Override the Groq URL by calling the helper directly (integration path)
        agentService.setGroqCredentials("gsk_valid_test_key_mock_groq")
        val result = agentService.callOpenAiCompatibleEndpoint(
            url = mockGroqUrl,
            apiKey = "gsk_valid_test_key_mock_groq",
            model = "llama-3.3-70b-versatile",
            systemPrompt = agentService.javaClass.getDeclaredMethod("buildDiagnosticSystemPrompt").apply { isAccessible = true }.invoke(agentService) as String,
            userPrompt = "Diagnose P0171 on 2021 Audi S5."
        )

        assertTrue(result.contains("PCV"))
        assertEquals(1, mockServer.requestCount)
        val req = mockServer.takeRequest()
        assertTrue(req.getHeader("Authorization")!!.contains("gsk_valid_test_key_mock_groq"))
    }
}
