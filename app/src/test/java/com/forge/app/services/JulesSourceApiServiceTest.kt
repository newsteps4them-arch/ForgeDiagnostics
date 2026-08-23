package com.forge.app.services

import com.forge.app.services.JulesSourceApiService
import com.forge.app.services.ListSourcesResponse
import com.forge.app.services.JulesSource
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

class JulesSourceApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: JulesSourceApiService

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        apiService = retrofit.create(JulesSourceApiService::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testListSources() = runBlocking {
        val mockResponseJson = """
        {
          "sources": [
            {
              "name": "sources/github-myorg-myrepo",
              "id": "github-myorg-myrepo",
              "githubRepo": {
                "owner": "myorg",
                "repo": "myrepo",
                "isPrivate": false,
                "defaultBranch": {
                  "displayName": "main"
                },
                "branches": [
                  { "displayName": "main" },
                  { "displayName": "develop" }
                ]
              }
            }
          ],
          "nextPageToken": "token_abc"
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponseJson))

        val response = apiService.listSources(
            apiKey = "test_key",
            pageSize = 10,
            pageToken = null,
            filter = null
        )

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertTrue(recordedRequest.path!!.startsWith("/sources"))
        assertEquals("test_key", recordedRequest.getHeader("x-goog-api-key"))

        assertNotNull(response)
        assertEquals(1, response.sources.size)
        assertEquals("token_abc", response.nextPageToken)

        val source = response.sources[0]
        assertEquals("sources/github-myorg-myrepo", source.name)
        assertEquals("github-myorg-myrepo", source.id)
    }

    @Test
    fun testGetSource() = runBlocking {
        val mockResponseJson = """
        {
          "name": "sources/github-myorg-myrepo",
          "id": "github-myorg-myrepo",
          "githubRepo": {
            "owner": "myorg",
            "repo": "myrepo",
            "isPrivate": false,
            "defaultBranch": {
              "displayName": "main"
            },
            "branches": [
              { "displayName": "main" }
            ]
          }
        }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponseJson))

        val response = apiService.getSource(
            apiKey = "test_key",
            sourceId = "github-myorg-myrepo"
        )

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/sources/github-myorg-myrepo", recordedRequest.path)
        assertEquals("test_key", recordedRequest.getHeader("x-goog-api-key"))

        assertNotNull(response)
        assertEquals("sources/github-myorg-myrepo", response.name)
        assertEquals("github-myorg-myrepo", response.id)
    }


// Adding error condition tests below

    @Test
    fun testListSourcesError() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        try {
            apiService.listSources(
                apiKey = "test_key"
            )
            org.junit.Assert.fail("Expected HTTP 500 error")
        } catch (e: retrofit2.HttpException) {
            assertEquals(500, e.code())
        }
    }

    @Test
    fun testGetSourceError() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

        try {
            apiService.getSource(
                apiKey = "test_key",
                sourceId = "invalid_id"
            )
            org.junit.Assert.fail("Expected HTTP 404 error")
        } catch (e: retrofit2.HttpException) {
            assertEquals(404, e.code())
        }
    }
}
