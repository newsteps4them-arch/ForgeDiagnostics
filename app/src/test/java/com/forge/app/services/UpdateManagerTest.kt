package com.forge.app.services

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class UpdateManagerTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: GitHubReleaseApiService

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val client = OkHttpClient.Builder().build()

        apiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubReleaseApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun isVersionNewer_comparesSemanticAndBuildNumbersCorrectly() {
        assertTrue(UpdateManager.isVersionNewer("1.0", "1.1"))
        assertTrue(UpdateManager.isVersionNewer("1.0.0", "1.0.1"))
        assertTrue(UpdateManager.isVersionNewer("1.0.0.5", "1.0.0.12"))
        assertTrue(UpdateManager.isVersionNewer("v1.0.0", "v1.1.0"))

        assertFalse(UpdateManager.isVersionNewer("1.1.0", "1.1.0"))
        assertFalse(UpdateManager.isVersionNewer("1.2.0", "1.1.0"))
        assertFalse(UpdateManager.isVersionNewer("v2.0.0", "v1.9.9"))
    }

    @Test
    fun checkForUpdates_detectsAvailableUpdateFromMockResponse() = runTest {
        val mockJsonResponse = """
            {
              "tag_name": "v1.1.0",
              "name": "Forge Diagnostics v1.1.0",
              "body": "Fixed engine telemetry bugs.",
              "published_at": "2025-01-01T00:00:00Z",
              "assets": [
                {
                  "name": "Forge-debug-1.1.0.apk",
                  "browser_download_url": "https://github.com/newsteps4them-arch/ForgeDiagnostics/releases/download/v1.1.0/Forge-debug-1.1.0.apk",
                  "size": 15420000
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJsonResponse))

        val updateManager = UpdateManager(apiService = apiService)
        val result = updateManager.checkForUpdates(currentVersion = "1.0.0")

        assertTrue(result is UpdateStatus.UpdateAvailable)
        val available = result as UpdateStatus.UpdateAvailable
        assertEquals("1.1.0", available.latestVersion)
        assertEquals("Forge-debug-1.1.0.apk", available.apkAsset.name)
    }

    @Test
    fun checkForUpdates_returnsUpToDateWhenVersionsMatch() = runTest {
        val mockJsonResponse = """
            {
              "tag_name": "v1.0.0",
              "name": "Forge Diagnostics v1.0.0",
              "body": "Current build",
              "assets": [
                {
                  "name": "Forge-debug-1.0.0.apk",
                  "browser_download_url": "https://example.com/Forge-debug-1.0.0.apk",
                  "size": 1000
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJsonResponse))

        val updateManager = UpdateManager(apiService = apiService)
        val result = updateManager.checkForUpdates(currentVersion = "1.0.0")

        assertTrue(result is UpdateStatus.UpToDate)
    }
}
