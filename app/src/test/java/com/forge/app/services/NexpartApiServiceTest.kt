package com.forge.app.services

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit

class NexpartApiServiceTest {

    @Test
    fun testSearchB2bInventory_ErrorPath_Fallback() = runBlocking {
        // Create an exception throwing proxy for the NexpartApi
        val mockApi = object : NexpartApi {
            override suspend fun searchInventory(
                apiKey: String,
                request: NexpartStockRequest
            ): NexpartStockResponse {
                throw RuntimeException("Simulated network error")
            }

            override suspend fun createOrder(
                apiKey: String,
                request: NexpartOrderRequest
            ): NexpartOrderResponse {
                throw RuntimeException("Simulated network error")
            }
        }

        val originalApi = NexpartClient.api

        try {
            NexpartClient.api = mockApi

            // This call should hit the catch block and return the fallback data
            val result = NexpartClient.searchB2bInventory(
                vin = "TESTVIN123",
                partNumberQuery = "TEST-PART-123",
                apiKeyOverride = "real-api-key"
            )

            assertNotNull(result)
            assertTrue(result.isNotEmpty())
            assertEquals("TEST-PART-123", result[0].partNumber)
            assertEquals("NGK Iridium IX", result[0].brand)

        } finally {
            // Restore original API to avoid breaking other tests
            NexpartClient.api = originalApi
        }
    }
}
