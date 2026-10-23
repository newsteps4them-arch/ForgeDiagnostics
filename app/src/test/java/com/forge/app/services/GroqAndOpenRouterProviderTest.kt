// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.

package com.forge.app.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for Groq and OpenRouter provider integration.
 * Uses MockWebServer — no live network/API keys needed.
 * android.util.Log is safe via build.gradle.kts isReturnDefaultValues = true.
 */
class GroqAndOpenRouterProviderTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var testClient: OkHttpClient
    private lateinit var agentService: OpenManusAgentService

    private fun mockOpenAiResponse(content: String): String {
        val escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return """{"id":"chatcmpl-test","object":"chat.completion","model":"llama-3.3-70b-versatile","choices":[{"index":0,"message":{"role":"assistant","content":"$escaped"},"finish_reason":"stop"}],"usage":{"prompt_tokens":420,"completion_tokens":380,"total_tokens":800}}"""
    }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        testClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        agentService = OpenManusAgentService(ioDispatcher = Dispatchers.Unconfined, httpClient = testClient)
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    // --- 1. Enum registration ---

    @Test
    fun testGroqAndOpenRouterAreRegisteredProviders() {
        val providers = AgentModelProvider.entries
        assertTrue(providers.any { it == AgentModelProvider.GROQ })
        assertTrue(providers.any { it == AgentModelProvider.OPEN_ROUTER })
    }

    @Test
    fun testGroqProviderEndpointDescriptionContainsGroqCom() {
        assertTrue(AgentModelProvider.GROQ.endpointDescription.contains("groq.com", ignoreCase = true))
    }

    @Test
    fun testOpenRouterEndpointDescriptionContainsOpenrouterAi() {
        assertTrue(AgentModelProvider.OPEN_ROUTER.endpointDescription.contains("openrouter.ai", ignoreCase = true))
    }

    @Test
    fun testGroqDisplayName() {
        assertEquals("Groq Ultrafast Inference", AgentModelProvider.GROQ.displayName)
    }

    @Test
    fun testOpenRouterDisplayName() {
        assertEquals("OpenRouter Free Tier", AgentModelProvider.OPEN_ROUTER.displayName)
    }

    // --- 2. Credential setters ---

    @Test
    fun testSetGroqCredentialsPersistsKey() {
        agentService.setGroqCredentials("gsk_test_forge_key_abc123", "deepseek-r1-distill-llama-70b")
        assertEquals("gsk_test_forge_key_abc123", agentService.state.value.groqApiKey)
        assertEquals("deepseek-r1-distill-llama-70b", agentService.state.value.groqModel)
    }

    @Test
    fun testSetGroqCredentialsUsesDefaultModel() {
        agentService.setGroqCredentials("gsk_default_model_test")
        assertEquals("llama-3.3-70b-versatile", agentService.state.value.groqModel)
    }

    @Test
    fun testSetOpenRouterCredentialsPersistsKey() {
        agentService.setOpenRouterCredentials("sk-or-v1-test_forge_key_xyz789", "google/gemini-2.5-flash")
        assertEquals("sk-or-v1-test_forge_key_xyz789", agentService.state.value.openRouterApiKey)
        assertEquals("google/gemini-2.5-flash", agentService.state.value.openRouterModel)
    }

    @Test
    fun testSetOpenRouterCredentialsUsesDefaultModel() {
        agentService.setOpenRouterCredentials("sk-or-v1-default_model_test")
        assertEquals("deepseek/deepseek-r1:free", agentService.state.value.openRouterModel)
    }

    // --- 3. State defaults ---

    @Test
    fun testDefaultGroqModelIsLlama() {
        assertEquals("llama-3.3-70b-versatile", agentService.state.value.groqModel)
    }

    @Test
    fun testDefaultOpenRouterModelIsDeepSeekFree() {
        assertEquals("deepseek/deepseek-r1:free", agentService.state.value.openRouterModel)
    }

    @Test
    fun testDefaultGroqApiKeyIsEmpty() {
        assertEquals("", agentService.state.value.groqApiKey)
    }

    @Test
    fun testDefaultOpenRouterApiKeyIsEmpty() {
        assertEquals("", agentService.state.value.openRouterApiKey)
    }

    // --- 4. callOpenAiCompatibleEndpoint via MockWebServer ---

    @Test
    fun testCallOpenAiCompatibleEndpointParsesSuccessfulResponse() {
        val expected = "Primary root cause: PCV valve failure."
        mockServer.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(mockOpenAiResponse(expected))
                .addHeader("Content-Type", "application/json")
        )
        val result = agentService.callOpenAiCompatibleEndpoint(
            url = mockServer.url("/v1/chat/completions").toString(),
            apiKey = "gsk_test_key_valid_12345",
            model = "llama-3.3-70b-versatile",
            systemPrompt = "Diagnostic AI.",
            userPrompt = "Diagnose P0171."
        )
        assertEquals(expected, result)
        val req = mockServer.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(req)
        assertEquals("POST", req!!.method)
        assertTrue(req.getHeader("Authorization")!!.startsWith("Bearer "))
        assertTrue(req.body.readUtf8().contains("llama-3.3-70b-versatile"))
    }

    @Test
    fun testCallOpenAiCompatibleEndpointReturnsEmptyOn429() {
        mockServer.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":"rate_limit_exceeded"}"""))
        val result = agentService.callOpenAiCompatibleEndpoint(
            url = mockServer.url("/v1/chat/completions").toString(),
            apiKey = "gsk_test_key_valid_12345",
            model = "llama-3.3-70b-versatile",
            systemPrompt = "S", userPrompt = "U"
        )
        assertEquals("", result)
    }

    @Test
    fun testCallOpenAiCompatibleEndpointRejectsBlankKey() {
        agentService.callOpenAiCompatibleEndpoint(
            url = mockServer.url("/v1/chat/completions").toString(),
            apiKey = "", model = "llama", systemPrompt = "S", userPrompt = "U"
        )
        assertEquals(0, mockServer.requestCount)
    }

    @Test
    fun testCallOpenAiCompatibleEndpointRejectsShortKey() {
        agentService.callOpenAiCompatibleEndpoint(
            url = mockServer.url("/v1/chat/completions").toString(),
            apiKey = "short", model = "llama", systemPrompt = "S", userPrompt = "U"
        )
        assertEquals(0, mockServer.requestCount)
    }

    @Test
    fun testCallOpenAiCompatibleEndpointSendsOpenRouterHeaders() {
        mockServer.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(mockOpenAiResponse("Vacuum leak detected."))
                .addHeader("Content-Type", "application/json")
        )
        agentService.callOpenAiCompatibleEndpoint(
            url = mockServer.url("/api/v1/chat/completions").toString(),
            apiKey = "sk-or-v1-test_key_valid_openrouter",
            model = "deepseek/deepseek-r1:free",
            systemPrompt = "S", userPrompt = "U",
            extraHeaders = mapOf(
                "HTTP-Referer" to "https://github.com/newsteps4them-arch/ForgeDiagnostics",
                "X-Title" to "Forge Agentic Diagnostics"
            )
        )
        val req = mockServer.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(req)
        assertEquals("https://github.com/newsteps4them-arch/ForgeDiagnostics", req!!.getHeader("HTTP-Referer"))
        assertEquals("Forge Agentic Diagnostics", req.getHeader("X-Title"))
    }

    @Test
    fun testCallOpenAiCompatibleEndpointSendsModelInBody() {
        mockServer.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(mockOpenAiResponse("Response."))
                .addHeader("Content-Type", "application/json")
        )
        agentService.callOpenAiCompatibleEndpoint(
            url = mockServer.url("/v1/chat/completions").toString(),
            apiKey = "sk-or-v1-test_key_modelcheck",
            model = "deepseek/deepseek-r1:free",
            systemPrompt = "Sys", userPrompt = "User"
        )
        val req = mockServer.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(req)
        val bodyText = req!!.body.readUtf8()
        assertTrue(bodyText.contains("deepseek/deepseek-r1:free"))
        assertTrue(bodyText.contains("messages"))
    }

    // --- 5. Provider dispatch with no key falls to deterministic engine ---

    @Test
    fun testGroqNoKeyFallsToDeterministicEngineForTurbo() = runBlocking {
        agentService.setModelProvider(AgentModelProvider.GROQ)
        agentService.runAutonomousDiagnosis(
            goal = "Diagnose P0299 Turbocharger underboost",
            vehicleContext = "2022 VW Golf GTI 2.0T",
            activeDtcs = listOf("P0299")
        )
        val report = agentService.state.value.finalReport
        assertNotNull(report)
        assertEquals(AgentExecutionPhase.COMPLETED, agentService.state.value.currentPhase)
        val cause = report!!.primaryRootCause
        assertTrue(
            "Expected turbo/wastegate/boost content, got: $cause",
            cause.contains("Turbocharger", ignoreCase = true) ||
            cause.contains("Wastegate", ignoreCase = true) ||
            cause.contains("Boost", ignoreCase = true)
        )
    }

    @Test
    fun testOpenRouterNoKeyFallsToDeterministicEngineForCan() = runBlocking {
        agentService.setModelProvider(AgentModelProvider.OPEN_ROUTER)
        agentService.runAutonomousDiagnosis(
            goal = "Diagnose U0100 Lost Communication CAN Bus",
            vehicleContext = "2022 Ford F-150 5.0L",
            activeDtcs = listOf("U0100")
        )
        val report = agentService.state.value.finalReport
        assertNotNull(report)
        assertEquals(AgentExecutionPhase.COMPLETED, agentService.state.value.currentPhase)
        val cause = report!!.primaryRootCause
        assertTrue(
            "Expected CAN/impedance/communication content, got: $cause",
            cause.contains("CAN", ignoreCase = true) ||
            cause.contains("Impedance", ignoreCase = true) ||
            cause.contains("Communication", ignoreCase = true)
        )
    }
}
