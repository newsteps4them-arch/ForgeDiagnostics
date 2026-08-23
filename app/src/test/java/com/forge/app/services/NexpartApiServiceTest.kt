package com.forge.app.services

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection

class NexpartApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var originalApi: NexpartApi

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true }

        val testRetrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val testApi = testRetrofit.create(NexpartApi::class.java)

        // Replace the api field in NexpartClient
        originalApi = NexpartClient.api
        NexpartClient.api = testApi
    }

    @After
    fun tearDown() {
        // Restore the original api field
        NexpartClient.api = originalApi
        mockWebServer.shutdown()
    }

    @Test
    fun `searchB2bInventory returns success response`() = runTest {
        val successJson = """
            {
                "status": "SUCCESS",
                "distributor": "TestDistributor",
                "parts": [
                    {
                        "partNumber": "TEST-123",
                        "brand": "TestBrand",
                        "description": "Test Part",
                        "category": "TestCategory",
                        "wholesalePrice": 10.0,
                        "retailPrice": 15.0,
                        "localStockQty": 5,
                        "regionalHubStockQty": 10,
                        "distributorName": "TestDistributorName",
                        "estimatedDeliveryTime": "Tomorrow",
                        "fitsVin": true
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(successJson)
        )

        val result = NexpartClient.searchB2bInventory("TEST_VIN", "TEST-123", "real_api_key")

        assertEquals(1, result.size)
        assertEquals("TEST-123", result[0].partNumber)
        assertEquals("TestBrand", result[0].brand)
    }

    @Test
    fun `searchB2bInventory handles empty response by returning fallback`() = runTest {
        val emptyResponseJson = """
            {
                "status": "SUCCESS",
                "distributor": "TestDistributor",
                "parts": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(emptyResponseJson)
        )

        val result = NexpartClient.searchB2bInventory("TEST_VIN", "TEST-123", "real_api_key")

        // Assert fallback is returned. Fallback returns 3 items.
        assertEquals(3, result.size)
        // Check if partNumber was overridden to query in the first element
        assertEquals("TEST-123", result[0].partNumber)
        assertEquals("NGK Iridium IX", result[0].brand)
    }

    @Test
    fun `searchB2bInventory handles API error by returning fallback`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
        )

        val result = NexpartClient.searchB2bInventory("TEST_VIN", "TEST-123", "real_api_key")

        // Assert fallback is returned. Fallback returns 3 items.
        assertEquals(3, result.size)
        assertEquals("TEST-123", result[0].partNumber)
        assertEquals("NGK Iridium IX", result[0].brand)
    }
}
