package com.forge.app.services

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JulesSession represents the full response model for a coding session as returned by the
 * Google Jules REST API (https://jules.googleapis.com/v1alpha/sessions).
 */
@Serializable
data class JulesSession(
    /**
     * The unique identifier for the session (e.g. "abc123" or "31415926535897932384").
     */
    @SerialName("id")
    val id: String = "",

    /**
     * The resource name of the session in Google standard format: "sessions/{sessionId}".
     */
    @SerialName("name")
    val name: String = "",

    /**
     * User-specified or auto-generated title describing the session task.
     */
    @SerialName("title")
    val title: String? = null,

    /**
     * The primary prompt/task instruction provided to Jules.
     */
    @SerialName("prompt")
    val prompt: String = "",

    /**
     * Current lifecycle state of the session:
     * QUEUED, PLANNING, AWAITING_PLAN_APPROVAL, AWAITING_USER_FEEDBACK,
     * IN_PROGRESS, PAUSED, COMPLETED, FAILED
     */
    @SerialName("state")
    val state: String = "QUEUED",

    /**
     * Direct URL to view and interact with the session in the Jules web interface.
     */
    @SerialName("url")
    val url: String? = null,

    /**
     * Timestamp of session creation in RFC 3339 format (e.g., "2024-01-15T10:30:00Z").
     */
    @SerialName("createTime")
    val createTime: String? = null,

    /**
     * Timestamp of last update to the session in RFC 3339 format.
     */
    @SerialName("updateTime")
    val updateTime: String? = null,

    /**
     * Indicates whether the user must explicitly approve the generated plan before execution.
     */
    @SerialName("requirePlanApproval")
    val requirePlanApproval: Boolean? = false,

    /**
     * Source repository context configured for this session.
     */
    @SerialName("sourceContext")
    val sourceContext: JulesSourceContext? = null,

    /**
     * Automation mode, such as "AUTO_CREATE_PR" or "MANUAL".
     */
    @SerialName("automationMode")
    val automationMode: String? = "AUTO_CREATE_PR",

    /**
     * Outputs produced by the session upon completion (e.g., Pull Request details).
     */
    @SerialName("outputs")
    val outputs: List<JulesSessionOutput>? = null
) {
    /**
     * Convenient helper property to extract the created Pull Request URL if present.
     */
    val pullRequestUrl: String?
        get() = outputs?.firstOrNull()?.pullRequest?.url

    val prUrl: String?
        get() = outputs?.firstOrNull()?.pullRequest?.url

    /**
     * Convenient helper property to extract the Pull Request title if present.
     */
    val pullRequestTitle: String?
        get() = outputs?.firstOrNull()?.pullRequest?.title

    val prTitle: String?
        get() = outputs?.firstOrNull()?.pullRequest?.title

    /**
     * Convenient helper property to extract the Pull Request description if present.
     */
    val pullRequestDescription: String?
        get() = outputs?.firstOrNull()?.pullRequest?.description

    val status: String
        get() = if (pullRequestUrl != null) "PR_CREATED" else state
}

@Serializable
data class JulesSessionOutput(
    @SerialName("pullRequest")
    val pullRequest: JulesPullRequest? = null
)

@Serializable
data class JulesPullRequest(
    @SerialName("url")
    val url: String,

    @SerialName("title")
    val title: String? = null,

    @SerialName("description")
    val description: String? = null
)

@Serializable
data class JulesSourceContext(
    @SerialName("source")
    val source: String,

    @SerialName("githubRepoContext")
    val githubRepoContext: JulesGithubRepoContext? = null
)

@Serializable
data class JulesGithubRepoContext(
    @SerialName("startingBranch")
    val startingBranch: String = "main"
)
