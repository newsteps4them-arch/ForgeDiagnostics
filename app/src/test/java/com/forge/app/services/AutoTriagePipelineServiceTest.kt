package com.forge.app.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse

class AutoTriagePipelineServiceTest {

    private lateinit var triageService: AutoTriagePipelineService
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val baseUrl = mockWebServer.url("/").toString()

        NhtsaSafetyClient.setBaseUrl(baseUrl)
        AlldataClient.setBaseUrl(baseUrl)
        NexpartClient.setBaseUrl(baseUrl)

        triageService = AutoTriagePipelineService(repository = null, authAndSyncService = null)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun queueFallbacks() {
        for (i in 1..10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        }
    }

    private suspend fun waitForPipelineToFinish() {
        var timeout = 100
        while (triageService.triageState.value.progress < 1.0f && timeout > 0) {
            kotlinx.coroutines.delay(100)
            timeout--
        }
    }

    @Test
    fun testInitialState() {
        val state = triageService.triageState.value
        assertFalse(state.isRunning)
        assertEquals(0f, state.progress, 0.0f)
        assertEquals(5, state.steps.size)
        assertTrue(state.steps.all { it.status == TriageStepStatus.PENDING })
    }

    @Test
    fun testRunAutoTriage_success() = runBlocking {
        queueFallbacks()
        triageService.runAutoTriage()

        waitForPipelineToFinish()

        val finalState = triageService.triageState.value

        assertFalse(finalState.isRunning)
        assertEquals(1.0f, finalState.progress, 0.0f)

        assertTrue(finalState.steps.all { it.status == TriageStepStatus.COMPLETED })

        assertNotNull(finalState.decodedSpecs)
        assertEquals("Audi", finalState.decodedSpecs?.make)

        assertTrue(finalState.safetyRecalls.isNotEmpty())
        assertTrue(finalState.matchedTsbs.isNotEmpty())
        assertTrue(finalState.sourcedParts.isNotEmpty())

        assertEquals(2.5, finalState.estimatedLaborHours, 0.0)
        assertTrue(finalState.totalEstimatedCost > 0.0)

        assertNotNull(finalState.summaryRecommendation)
    }

    @Test
    fun testRunAutoTriage_alreadyRunning() = runBlocking {
        queueFallbacks()

        triageService.runAutoTriage()

        var startTimeout = 50
        while (!triageService.triageState.value.isRunning && startTimeout > 0) {
            kotlinx.coroutines.delay(10)
            startTimeout--
        }

        assertTrue(triageService.triageState.value.isRunning)
        val initialProgress = triageService.triageState.value.progress

        // Try to run again while it's already running
        triageService.runAutoTriage(vin = "DIFFERENT_VIN")

        assertEquals("WAUZZZF58MA019284", triageService.triageState.value.activeVehicleVin)

        waitForPipelineToFinish()

        assertFalse(triageService.triageState.value.isRunning)
        assertEquals(1.0f, triageService.triageState.value.progress, 0.0f)
    }

    @Test
    fun testReset() = runBlocking {
        queueFallbacks()
        triageService.runAutoTriage()

        waitForPipelineToFinish()

        assertEquals(1.0f, triageService.triageState.value.progress, 0.0f)

        triageService.reset()

        val state = triageService.triageState.value
        assertFalse(state.isRunning)
        assertEquals(0f, state.progress, 0.0f)
        assertEquals(5, state.steps.size)
        assertTrue(state.steps.all { it.status == TriageStepStatus.PENDING })
    }
}
