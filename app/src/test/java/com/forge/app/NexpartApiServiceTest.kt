package com.forge.app

import com.forge.app.services.NexpartApi
import com.forge.app.services.NexpartClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class NexpartApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var originalApi: NexpartApi

    @Before
    fun setup() {
        originalApi = NexpartClient.api
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        NexpartClient.api = retrofit.create(NexpartApi::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
        NexpartClient.api = originalApi
    }

    @Test
    fun testPlaceB2bPartOrderDemoKey() = runBlocking {
        // MockWebServer is not even hit for DEMO keys.
        val result = NexpartClient.placeB2bPartOrder(
            partNumber = "TEST-123",
            quantity = 2,
            apiKeyOverride = "DEMO_KEY"
        )

        assertEquals("TEST-123", result.partNumber)
        assertEquals(2, result.quantityOrdered)
        assertEquals("CONFIRMED_LOCAL_DISPATCH", result.status)
        assertTrue(result.orderId.startsWith("NEX-ORD-"))
        assertEquals(57.0, result.totalCost, 0.01)
    }

    @Test
    fun testPlaceB2bPartOrderSuccess() = runBlocking {
        val successResponse = """
            {
                "orderId": "MOCK-ORD-777",
                "status": "PROCESSED",
                "partNumber": "REAL-456",
                "quantityOrdered": 1,
                "totalCost": 45.99,
                "distributorRef": "Mock Distributor",
                "estimatedArrival": "Tomorrow"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(successResponse)
            .addHeader("Content-Type", "application/json"))

        val result = NexpartClient.placeB2bPartOrder(
            partNumber = "REAL-456",
            quantity = 1,
            apiKeyOverride = "VALID_KEY"
        )

        assertEquals("REAL-456", result.partNumber)
        assertEquals(1, result.quantityOrdered)
        assertEquals("PROCESSED", result.status)
        assertEquals("MOCK-ORD-777", result.orderId)
        assertEquals(45.99, result.totalCost, 0.01)
        assertEquals("Mock Distributor", result.distributorRef)
        assertEquals("Tomorrow", result.estimatedArrival)

        val request = mockWebServer.takeRequest()
        assertEquals("/v2/b2b/orders/create", request.path)
        assertEquals("POST", request.method)
        assertEquals("VALID_KEY", request.getHeader("X-Nexpart-API-Key"))
    }

    @Test
    fun testPlaceB2bPartOrderExceptionFallback() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val result = NexpartClient.placeB2bPartOrder(
            partNumber = "TEST-789",
            quantity = 3,
            apiKeyOverride = "VALID_KEY"
        )

        assertEquals("TEST-789", result.partNumber)
        assertEquals(3, result.quantityOrdered)
        assertEquals("CONFIRMED_LOCAL_DISPATCH", result.status)
        assertTrue(result.orderId.startsWith("NEX-ORD-"))
        assertEquals(85.50, result.totalCost, 0.01)
    }
}
