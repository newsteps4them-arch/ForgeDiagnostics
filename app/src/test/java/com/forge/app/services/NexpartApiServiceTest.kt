package com.forge.app.services

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient

class NexpartApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: NexpartApi

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true }

        val okHttpClient = OkHttpClient.Builder().build()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NexpartApi::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test searchInventory returns successful response`() = runTest {
        val jsonResponse = """
            {
                "status": "SUCCESS",
                "distributor": "AutoZone",
                "parts": [
                    {
                        "partNumber": "NGK-123",
                        "brand": "NGK",
                        "description": "Spark Plug",
                        "category": "Ignition",
                        "wholesalePrice": 5.50,
                        "retailPrice": 10.00,
                        "localStockQty": 4,
                        "regionalHubStockQty": 20,
                        "distributorName": "AutoZone Store #1",
                        "estimatedDeliveryTime": "30 mins",
                        "fitsVin": true
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val request = NexpartStockRequest("VIN123", "NGK-123")
        val response = apiService.searchInventory("test_api_key", request)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/v2/b2b/inventory/search", recordedRequest.path)
        assertEquals("test_api_key", recordedRequest.getHeader("X-Nexpart-API-Key"))

        assertEquals("SUCCESS", response.status)
        assertEquals("AutoZone", response.distributor)
        assertEquals(1, response.parts.size)

        val part = response.parts.first()
        assertEquals("NGK-123", part.partNumber)
        assertEquals("NGK", part.brand)
        assertEquals(4, part.localStockQty)
    }

    @Test
    fun `test createOrder returns successful response`() = runTest {
        val jsonResponse = """
            {
                "orderId": "ORD-12345",
                "status": "CONFIRMED",
                "partNumber": "NGK-123",
                "quantityOrdered": 4,
                "totalCost": 22.00,
                "distributorRef": "AZ-REF-999",
                "estimatedArrival": "14:30"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val request = NexpartOrderRequest("NGK-123", 4, "WO-100", "Tech Bob")
        val response = apiService.createOrder("test_api_key", request)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/v2/b2b/orders/create", recordedRequest.path)
        assertEquals("test_api_key", recordedRequest.getHeader("X-Nexpart-API-Key"))

        assertEquals("ORD-12345", response.orderId)
        assertEquals("CONFIRMED", response.status)
        assertEquals("NGK-123", response.partNumber)
        assertEquals(4, response.quantityOrdered)
        assertEquals(22.00, response.totalCost, 0.01)
    }


    @Test
    fun `test searchB2bInventory fallback local part list`() = runTest {
        val result = NexpartClient.searchB2bInventory(
            vin = "TEST-VIN",
            partNumberQuery = "TEST-PART",
            apiKeyOverride = "DEMO"
        )

        assertNotNull(result)
        assertEquals(3, result.size)

        val firstPart = result[0]
        assertEquals("TEST-PART", firstPart.partNumber)

        val secondPart = result[1]
        assertEquals("BOSCH-0221604115", secondPart.partNumber)
    }

    @Test
    fun `test placeB2bPartOrder fallback local order response`() = runTest {
        val result = NexpartClient.placeB2bPartOrder(
            partNumber = "TEST-PART-ORDER",
            quantity = 2,
            workOrderId = "WO-TEST",
            technicianName = "Tester",
            apiKeyOverride = "DEMO"
        )

        assertNotNull(result)
        assertEquals("CONFIRMED_LOCAL_DISPATCH", result.status)
        assertEquals("TEST-PART-ORDER", result.partNumber)
        assertEquals(2, result.quantityOrdered)
        assertNotNull(result.orderId)
    }

}
