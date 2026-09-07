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

class NexpartApiServiceTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Configure NexpartClient to use the MockWebServer
        NexpartClient.setBaseUrl(mockWebServer.url("/").toString())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        // Reset the client back to its default production state
        NexpartClient.resetApi()
    }

    @Test
    fun testSearchB2bInventory_HappyPath_ParsesCorrectly() = runBlocking {
        val jsonResponse = """
            {
                "status": "SUCCESS",
                "distributor": "AUTOZONE_COMMERCIAL_08",
                "parts": [
                    {
                        "partNumber": "TEST-PART-123",
                        "brand": "TestBrand",
                        "description": "Test Description",
                        "category": "Test Category",
                        "wholesalePrice": 10.0,
                        "retailPrice": 15.0,
                        "localStockQty": 5,
                        "regionalHubStockQty": 20,
                        "distributorName": "Test Distributor",
                        "estimatedDeliveryTime": "30 mins",
                        "fitsVin": true
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val result = NexpartClient.searchB2bInventory(
            vin = "TESTVIN123",
            partNumberQuery = "TEST-PART-123",
            apiKeyOverride = "real-api-key"
        )

        val request = mockWebServer.takeRequest()
        assertEquals("/v2/b2b/inventory/search", request.path)

        assertNotNull(result)
        assertEquals(1, result.size)

        val item = result[0]
        assertEquals("TEST-PART-123", item.partNumber)
        assertEquals("TestBrand", item.brand)
        assertEquals(10.0, item.wholesalePrice, 0.0)
        assertEquals(5, item.localStockQty)
        assertTrue(item.fitsVin)
    }

    @Test
    fun testPlaceB2bPartOrder_HappyPath_ParsesCorrectly() = runBlocking {
        val jsonResponse = """
            {
                "orderId": "ORD-123",
                "status": "CONFIRMED",
                "partNumber": "TEST-PART-123",
                "quantityOrdered": 2,
                "totalCost": 20.0,
                "distributorRef": "DIST-123",
                "estimatedArrival": "45 mins"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val result = NexpartClient.placeB2bPartOrder(
            partNumber = "TEST-PART-123",
            quantity = 2,
            workOrderId = "WO-123",
            technicianName = "Test Tech",
            apiKeyOverride = "real-api-key"
        )

        val request = mockWebServer.takeRequest()
        assertEquals("/v2/b2b/orders/create", request.path)

        assertNotNull(result)
        assertEquals("ORD-123", result.orderId)
        assertEquals("CONFIRMED", result.status)
        assertEquals("TEST-PART-123", result.partNumber)
        assertEquals(2, result.quantityOrdered)
        assertEquals(20.0, result.totalCost, 0.0)
        assertEquals("DIST-123", result.distributorRef)
    }

    @Test
    fun testSearchB2bInventory_ErrorPath_Fallback() = runBlocking {
        // Enqueue an error response to trigger the catch block
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val result = NexpartClient.searchB2bInventory(
            vin = "TESTVIN123",
            partNumberQuery = "TEST-PART-123",
            apiKeyOverride = "real-api-key"
        )

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals("TEST-PART-123", result[0].partNumber)
        assertEquals("NGK Iridium IX", result[0].brand)
    }
}
