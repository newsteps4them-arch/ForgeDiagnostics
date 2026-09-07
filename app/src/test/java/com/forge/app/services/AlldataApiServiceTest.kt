package com.forge.app.services

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AlldataApiServiceTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Configure AlldataClient to use the MockWebServer
        AlldataClient.setBaseUrl(mockWebServer.url("/").toString())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        // Reset the client back to its default production state
        AlldataClient.resetApi()
    }

    @Test
    fun testFetchRepairProcedures_HappyPath_ParsesCorrectly() = runBlocking {
        val jsonResponse = """
            {
                "status": "SUCCESS",
                "vin": "WAUZZZF58MA019284",
                "data": [
                    {
                        "id": "PROC-AUDI-EA839-01",
                        "vin": "WAUZZZF58MA019284",
                        "title": "Audi 3.0T V6 EA839 Direct Injection Coil Pack & Spark Plug Replacement",
                        "category": "Engine Misfire & Ignition",
                        "laborHours": 1.8,
                        "difficulty": "Intermediate",
                        "steps": [
                            "1. Disconnect negative battery terminal. Remove engine acoustic cover.",
                            "2. Unclip coil pack harness connectors (Pins 1-4). Inspect for terminal corrosion or oil ingress."
                        ],
                        "requiredTools": ["14mm Thin-Wall Plug Socket", "T10530 Coil Puller"],
                        "safetyWarnings": ["Ensure engine is cool (below 40°C) before plug removal to prevent aluminum head thread damage."]
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val result = AlldataClient.fetchRepairProcedures(
            vin = "WAUZZZF58MA019284",
            category = "Engine Misfire & Ignition",
            apiKeyOverride = "real-api-key"
        )

        val request = mockWebServer.takeRequest()
        assertEquals("/v1/oem/procedures?vin=WAUZZZF58MA019284&category=Engine%20Misfire%20%26%20Ignition", request.path)

        assertNotNull(result)
        assertEquals(1, result.size)

        val item = result[0]
        assertEquals("PROC-AUDI-EA839-01", item.id)
        assertEquals("Audi 3.0T V6 EA839 Direct Injection Coil Pack & Spark Plug Replacement", item.title)
        assertEquals(1.8, item.laborHours, 0.0)
        assertEquals(2, item.steps.size)
    }

    @Test
    fun testFetchWiringDiagrams_HappyPath_ParsesCorrectly() = runBlocking {
        val jsonResponse = """
            {
                "status": "SUCCESS",
                "vin": "WAUZZZF58MA019284",
                "data": [
                    {
                        "diagramId": "DIAG-EA839-PCM-PINOUT",
                        "systemName": "Engine Control Module (ECM/PCM)",
                        "pinoutDetails": {
                            "Pin 12 (BLK/RED)": "Coil Cylinder 1 Trigger Signal (0-5V Square Wave)",
                            "Pin 14 (BRN)": "ECU Engine Ground Ground (Main Cylinder Head Stud)"
                        },
                        "wireColors": ["BLK/RED - Trigger", "BRN - Ground"],
                        "diagramImageUrl": "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=800",
                        "oemRefCode": "AUDI-WD-EA839-2021-V6"
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val result = AlldataClient.fetchWiringDiagrams(
            vin = "WAUZZZF58MA019284",
            system = "Engine Control Module (ECM/PCM)",
            apiKeyOverride = "real-api-key"
        )

        val request = mockWebServer.takeRequest()
        assertEquals("/v1/oem/diagrams?vin=WAUZZZF58MA019284&system=Engine%20Control%20Module%20%28ECM%2FPCM%29", request.path)

        assertNotNull(result)
        assertEquals(1, result.size)

        val item = result[0]
        assertEquals("DIAG-EA839-PCM-PINOUT", item.diagramId)
        assertEquals(2, item.pinoutDetails.size)
        assertEquals("Coil Cylinder 1 Trigger Signal (0-5V Square Wave)", item.pinoutDetails["Pin 12 (BLK/RED)"])
    }

    @Test
    fun testFetchFactoryTsbs_HappyPath_ParsesCorrectly() = runBlocking {
        val jsonResponse = """
            {
                "status": "SUCCESS",
                "vin": "WAUZZZF58MA019284",
                "data": [
                    {
                        "tsbNumber": "TSB 2058319/4",
                        "title": "Cold Engine Idle Roughness & Cylinder 2 Misfire Under Acceleration",
                        "issueDate": "2024-03-15",
                        "affectedComponents": ["Ignition Coils", "Spark Plugs", "High Pressure Fuel Injectors"],
                        "summary": "Some 2019-2022 Audi vehicles equipped with 3.0T EA839 engines may exhibit intermittent P0300/P0302 misfire codes.",
                        "oemCorrectionProcedure": "Inspect coil pack part revision. If index is 'E', replace with updated Index 'G'."
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val result = AlldataClient.fetchFactoryTsbs(
            vin = "WAUZZZF58MA019284",
            apiKeyOverride = "real-api-key"
        )

        val request = mockWebServer.takeRequest()
        assertEquals("/v1/oem/tsbs?vin=WAUZZZF58MA019284", request.path)

        assertNotNull(result)
        assertEquals(1, result.size)

        val item = result[0]
        assertEquals("TSB 2058319/4", item.tsbNumber)
        assertEquals(3, item.affectedComponents.size)
        assertEquals("Cold Engine Idle Roughness & Cylinder 2 Misfire Under Acceleration", item.title)
    }

    @Test
    fun testFetchRepairProcedures_ErrorPath_Fallback() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val result = AlldataClient.fetchRepairProcedures(
            vin = "WAUZZZF58MA019284",
            category = "Engine Misfire & Ignition",
            apiKeyOverride = "real-api-key"
        )

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals("PROC-AUDI-EA839-01", result[0].id)
    }

    @Test
    fun testFetchWiringDiagrams_ErrorPath_Fallback() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val result = AlldataClient.fetchWiringDiagrams(
            vin = "WAUZZZF58MA019284",
            system = "Engine Control Module (ECM/PCM)",
            apiKeyOverride = "real-api-key"
        )

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals("DIAG-EA839-PCM-PINOUT", result[0].diagramId)
    }

    @Test
    fun testFetchFactoryTsbs_ErrorPath_Fallback() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val result = AlldataClient.fetchFactoryTsbs(
            vin = "WAUZZZF58MA019284",
            apiKeyOverride = "real-api-key"
        )

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals("TSB 2058319/4", result[0].tsbNumber)
    }
}
