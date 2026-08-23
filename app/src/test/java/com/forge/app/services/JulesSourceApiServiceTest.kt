package com.forge.app.services

import kotlinx.coroutines.runBlocking
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

class JulesSourceApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: JulesSourceApiService

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        apiService = retrofit.create(JulesSourceApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `listSources parses success response correctly`() = runBlocking {
        val jsonResponse = """
            {
              "sources": [
                {
                  "name": "sources/github-myorg-repo1",
                  "id": "github-myorg-repo1",
                  "githubRepo": {
                    "owner": "myorg",
                    "repo": "repo1",
                    "isPrivate": false,
                    "defaultBranch": { "displayName": "main" },
                    "branches": [ { "displayName": "main" } ]
                  }
                },
                {
                  "name": "sources/github-myorg-repo2",
                  "id": "github-myorg-repo2",
                  "githubRepo": {
                    "owner": "myorg",
                    "repo": "repo2",
                    "isPrivate": true,
                    "defaultBranch": { "displayName": "develop" },
                    "branches": [ { "displayName": "develop" } ]
                  }
                }
              ],
              "nextPageToken": "next_page_token_123"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        val response = apiService.listSources(apiKey = "test_api_key", pageSize = 10, filter = "name=sources/github-myorg*")

        // Verify request details
        val request = mockWebServer.takeRequest()
        assertEquals("/sources?pageSize=10&filter=name%3Dsources%2Fgithub-myorg*", request.path)
        assertEquals("test_api_key", request.getHeader("x-goog-api-key"))

        // Verify response parsing
        assertNotNull(response)
        assertEquals("next_page_token_123", response.nextPageToken)
        assertEquals(2, response.sources.size)

        val firstSource = response.sources[0]
        assertEquals("sources/github-myorg-repo1", firstSource.name)
        assertEquals("github-myorg-repo1", firstSource.id)
        assertEquals("myorg", firstSource.owner)
        assertEquals("repo1", firstSource.repo)
        assertEquals(false, firstSource.isPrivate)
        assertEquals("main", firstSource.defaultBranchName)

        val secondSource = response.sources[1]
        assertEquals("sources/github-myorg-repo2", secondSource.name)
        assertEquals("github-myorg-repo2", secondSource.id)
        assertEquals("myorg", secondSource.owner)
        assertEquals("repo2", secondSource.repo)
        assertEquals(true, secondSource.isPrivate)
        assertEquals("develop", secondSource.defaultBranchName)
    }

    @Test
    fun `listSources parses empty response correctly`() = runBlocking {
        val jsonResponse = """
            {
              "sources": []
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        val response = apiService.listSources(apiKey = "test_api_key")

        // Verify request details
        val request = mockWebServer.takeRequest()
        assertEquals("/sources?pageSize=20", request.path)
        assertEquals("test_api_key", request.getHeader("x-goog-api-key"))

        // Verify response parsing
        assertNotNull(response)
        assertEquals(null, response.nextPageToken)
        assertEquals(0, response.sources.size)
    }
}
