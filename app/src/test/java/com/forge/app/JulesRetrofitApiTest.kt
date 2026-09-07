// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app

import com.forge.app.services.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class JulesRetrofitApiTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testCreateSessionRequestSerialization() {
        val request = CreateSessionRequest(
            prompt = "Add comprehensive unit tests for the authentication module",
            title = "Add auth tests",
            sourceContext = SourceContextDto(
                source = "sources/github/teamforge-automotive/core-telemetry",
                githubRepoContext = GithubRepoContextDto("main")
            ),
            automationMode = "AUTO_CREATE_PR",
            requirePlanApproval = true
        )

        val jsonStr = json.encodeToString(request)
        assertTrue(jsonStr.contains("Add auth tests"))
        assertTrue(jsonStr.contains("AUTO_CREATE_PR"))
        assertTrue(jsonStr.contains("sources/github/teamforge-automotive/core-telemetry"))
        assertTrue(jsonStr.contains("\"requirePlanApproval\":true"))
    }

    @Test
    fun testJulesSessionDeserialization() {
        val jsonPayload = """
        {
          "name": "sessions/1234567",
          "id": "abc123",
          "prompt": "Add comprehensive unit tests",
          "title": "Add auth tests",
          "state": "COMPLETED",
          "url": "https://jules.google.com/session/abc123",
          "createTime": "2024-01-15T10:30:00Z",
          "updateTime": "2024-01-15T11:45:00Z",
          "outputs": [
            {
              "pullRequest": {
                "url": "https://github.com/myorg/myrepo/pull/42",
                "title": "Add auth tests",
                "description": "Added unit tests for authentication module"
              }
            }
          ]
        }
        """.trimIndent()

        val session: JulesSession = json.decodeFromString(jsonPayload)
        assertEquals("sessions/1234567", session.name)
        assertEquals("abc123", session.id)
        assertEquals("COMPLETED", session.state)
        assertEquals("https://jules.google.com/session/abc123", session.url)
        assertNotNull(session.outputs)
        assertEquals(1, session.outputs?.size)
        assertEquals("https://github.com/myorg/myrepo/pull/42", session.pullRequestUrl)
        assertEquals("Add auth tests", session.pullRequestTitle)
    }

    @Test
    fun testListSessionsResponseDeserialization() {
        val jsonPayload = """
        {
          "sessions": [
            {
              "name": "sessions/1234567",
              "id": "abc123",
              "title": "Add auth tests",
              "state": "COMPLETED",
              "createTime": "2024-01-15T10:30:00Z"
            }
          ],
          "nextPageToken": "token_abc_123"
        }
        """.trimIndent()

        val response: ListSessionsResponse = json.decodeFromString(jsonPayload)
        assertEquals(1, response.sessions.size)
        assertEquals("token_abc_123", response.nextPageToken)
        assertEquals("abc123", response.sessions.first().id)
    }

    @Test
    fun testJulesRetrofitClientInstantiation() {
        val api: JulesApiService = JulesApiService.create("test_api_key")
        assertNotNull(api)
        assertEquals("https://jules.googleapis.com/v1alpha/", JulesApiService.BASE_URL)
        assertEquals("https://jules.googleapis.com/v1alpha/", JulesApi.BASE_URL)

        val activityApi: JulesActivityApiService = JulesActivityApiService.create("test_api_key")
        assertNotNull(activityApi)
        assertEquals("https://jules.googleapis.com/v1alpha/", JulesActivityApiService.BASE_URL)

        val sourceApi: JulesSourceApiService = JulesSourceApiService.create("test_api_key")
        assertNotNull(sourceApi)
        assertEquals("https://jules.googleapis.com/v1alpha/", JulesSourceApiService.BASE_URL)
    }

    @Test
    fun testPlanGeneratedActivityDeserialization() {
        val jsonPayload = """
        {
          "name": "sessions/1234567/activities/act2",
          "id": "act2",
          "originator": "agent",
          "description": "Plan generated",
          "planGenerated": {
            "plan": {
              "id": "plan1",
              "steps": [
                {
                  "id": "step1",
                  "index": 0,
                  "title": "Analyze existing code",
                  "description": "Review the authentication module structure"
                },
                {
                  "id": "step2",
                  "index": 1,
                  "title": "Write unit tests",
                  "description": "Create comprehensive test coverage"
                }
              ],
              "createTime": "2024-01-15T10:31:00Z"
            }
          },
          "createTime": "2024-01-15T10:31:00Z"
        }
        """.trimIndent()

        val activity: JulesActivity = json.decodeFromString(jsonPayload)
        assertEquals("act2", activity.id)
        assertEquals("agent", activity.originator)
        assertEquals(ActivityEventType.PLAN_GENERATED, activity.eventType)
        assertNotNull(activity.planGenerated)
        assertEquals(2, activity.planGenerated?.plan?.steps?.size)
        assertEquals("Analyze existing code", activity.planGenerated?.plan?.steps?.first()?.title)
    }

    @Test
    fun testCodeChangesArtifactActivityDeserialization() {
        val jsonPayload = """
        {
          "name": "sessions/1234567/activities/act3",
          "id": "act3",
          "originator": "agent",
          "description": "Code changes ready",
          "createTime": "2024-01-15T11:00:00Z",
          "artifacts": [
            {
              "changeSet": {
                "source": "sources/github-myorg-myrepo",
                "gitPatch": {
                  "baseCommitId": "a1b2c3d4",
                  "unidiffPatch": "diff --git a/tests/auth.test.js...",
                  "suggestedCommitMessage": "Add unit tests for authentication module"
                }
              }
            }
          ]
        }
        """.trimIndent()

        val activity: JulesActivity = json.decodeFromString(jsonPayload)
        assertEquals("act3", activity.id)
        assertEquals(ActivityEventType.CODE_CHANGES, activity.eventType)
        assertNotNull(activity.artifacts)
        val patch = activity.artifacts?.first()?.changeSet?.gitPatch
        assertNotNull(patch)
        assertEquals("a1b2c3d4", patch?.baseCommitId)
        assertEquals("Add unit tests for authentication module", patch?.suggestedCommitMessage)
    }

    @Test
    fun testBashOutputArtifactActivityDeserialization() {
        val jsonPayload = """
        {
          "name": "sessions/1234567/activities/act4",
          "id": "act4",
          "originator": "agent",
          "description": "Tests executed",
          "createTime": "2024-01-15T11:05:00Z",
          "artifacts": [
            {
              "bashOutput": {
                "command": "npm test",
                "output": "All tests passed (42 passing)",
                "exitCode": 0
              }
            }
          ]
        }
        """.trimIndent()

        val activity: JulesActivity = json.decodeFromString(jsonPayload)
        assertEquals("act4", activity.id)
        assertEquals(ActivityEventType.BASH_OUTPUT, activity.eventType)
        val bash = activity.artifacts?.first()?.bashOutput
        assertNotNull(bash)
        assertEquals("npm test", bash?.command)
        assertEquals(0, bash?.exitCode)
    }

    @Test
    fun testListSourcesResponseDeserialization() {
        val jsonPayload = """
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
                  { "displayName": "develop" },
                  { "displayName": "feature/auth" }
                ]
              }
            },
            {
              "name": "sources/github-myorg-another-repo",
              "id": "github-myorg-another-repo",
              "githubRepo": {
                "owner": "myorg",
                "repo": "another-repo",
                "isPrivate": true,
                "defaultBranch": {
                  "displayName": "main"
                },
                "branches": [
                  { "displayName": "main" }
                ]
              }
            }
          ],
          "nextPageToken": "eyJvZmZzZXQiOjEwfQ=="
        }
        """.trimIndent()

        val response: ListSourcesResponse = json.decodeFromString(jsonPayload)
        assertEquals(2, response.sources.size)
        assertEquals("eyJvZmZzZXQiOjEwfQ==", response.nextPageToken)

        val firstSource = response.sources[0]
        assertEquals("sources/github-myorg-myrepo", firstSource.name)
        assertEquals("github-myorg-myrepo", firstSource.id)
        assertEquals("myorg", firstSource.owner)
        assertEquals("myrepo", firstSource.repo)
        assertFalse(firstSource.isPrivate)
        assertEquals("main", firstSource.defaultBranchName)
        assertEquals(listOf("main", "develop", "feature/auth"), firstSource.branchNames)

        val secondSource = response.sources[1]
        assertEquals("sources/github-myorg-another-repo", secondSource.name)
        assertTrue(secondSource.isPrivate)
        assertEquals(listOf("main"), secondSource.branchNames)
    }

    @Test
    fun testGetSourceDeserialization() {
        val jsonPayload = """
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
              { "displayName": "develop" },
              { "displayName": "feature/auth" },
              { "displayName": "feature/tests" }
            ]
          }
        }
        """.trimIndent()

        val source: JulesSource = json.decodeFromString(jsonPayload)
        assertEquals("sources/github-myorg-myrepo", source.name)
        assertEquals("github-myorg-myrepo", source.id)
        assertEquals("myorg", source.owner)
        assertEquals("myrepo", source.repo)
        assertFalse(source.isPrivate)
        assertEquals("main", source.defaultBranchName)
        assertEquals(4, source.branchNames.size)
        assertTrue(source.branchNames.contains("feature/tests"))
    }
}
