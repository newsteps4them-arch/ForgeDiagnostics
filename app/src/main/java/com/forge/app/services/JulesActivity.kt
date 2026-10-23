package com.forge.app.services

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JulesActivity represents an event that occurs during a coding session
 * (https://jules.googleapis.com/v1alpha/sessions/{sessionId}/activities).
 */
@Serializable
data class JulesActivity(
    @SerialName("name")
    val name: String = "",

    @SerialName("id")
    val id: String = "",

    @SerialName("originator")
    val originator: String? = "system", // "system", "agent", "user"

    @SerialName("description")
    val description: String? = null,

    @SerialName("createTime")
    val createTime: String? = null,

    // Activity Event Payloads (exactly one is populated per activity)
    @SerialName("planGenerated")
    val planGenerated: PlanGeneratedEvent? = null,

    @SerialName("planApproved")
    val planApproved: PlanApprovedEvent? = null,

    @SerialName("userMessaged")
    val userMessaged: UserMessagedEvent? = null,

    @SerialName("agentMessaged")
    val agentMessaged: AgentMessagedEvent? = null,

    @SerialName("progressUpdated")
    val progressUpdated: ProgressUpdatedEvent? = null,

    @SerialName("sessionCompleted")
    val sessionCompleted: SessionCompletedEvent? = null,

    @SerialName("sessionFailed")
    val sessionFailed: SessionFailedEvent? = null,

    // Artifacts produced during execution
    @SerialName("artifacts")
    val artifacts: List<JulesArtifact>? = null,

    // Legacy/convenience adapter fields
    val type: String = "MESSAGE",
    val author: String = originator ?: "JULES_AGENT",
    val message: String = description ?: ""
) {
    /**
     * Determine the canonical activity category.
     */
    val eventType: ActivityEventType
        get() = when {
            planGenerated != null -> ActivityEventType.PLAN_GENERATED
            planApproved != null -> ActivityEventType.PLAN_APPROVED
            userMessaged != null -> ActivityEventType.USER_MESSAGED
            agentMessaged != null -> ActivityEventType.AGENT_MESSAGED
            progressUpdated != null -> ActivityEventType.PROGRESS_UPDATED
            sessionCompleted != null -> ActivityEventType.SESSION_COMPLETED
            sessionFailed != null -> ActivityEventType.SESSION_FAILED
            artifacts?.any { it.changeSet != null } == true -> ActivityEventType.CODE_CHANGES
            artifacts?.any { it.bashOutput != null } == true -> ActivityEventType.BASH_OUTPUT
            else -> ActivityEventType.GENERIC
        }

    /**
     * Primary display message for user interfaces.
     */
    val displayMessage: String
        get() = when {
            agentMessaged != null -> agentMessaged.agentMessage
            userMessaged != null -> userMessaged.userMessage
            progressUpdated != null -> "${progressUpdated.title}: ${progressUpdated.description ?: ""}"
            planGenerated != null -> "Plan generated with ${planGenerated.plan.steps.size} steps."
            planApproved != null -> "Plan ${planApproved.planId ?: ""} approved."
            sessionCompleted != null -> "Session completed successfully."
            sessionFailed != null -> "Session failed: ${sessionFailed.reason ?: "Unknown error"}"
            description != null -> description
            message.isNotBlank() -> message
            else -> "Activity $id"
        }
}

enum class ActivityEventType {
    PLAN_GENERATED,
    PLAN_APPROVED,
    USER_MESSAGED,
    AGENT_MESSAGED,
    PROGRESS_UPDATED,
    SESSION_COMPLETED,
    SESSION_FAILED,
    CODE_CHANGES,
    BASH_OUTPUT,
    GENERIC
}

@Serializable
data class PlanGeneratedEvent(
    @SerialName("plan")
    val plan: JulesPlan
)

@Serializable
data class JulesPlan(
    @SerialName("id")
    val id: String = "",

    @SerialName("steps")
    val steps: List<JulesPlanStep> = emptyList(),

    @SerialName("createTime")
    val createTime: String? = null
)

@Serializable
data class JulesPlanStep(
    @SerialName("id")
    val id: String = "",

    @SerialName("index")
    val index: Int = 0,

    @SerialName("title")
    val title: String = "",

    @SerialName("description")
    val description: String? = null
)

@Serializable
data class PlanApprovedEvent(
    @SerialName("planId")
    val planId: String? = null
)

@Serializable
data class UserMessagedEvent(
    @SerialName("userMessage")
    val userMessage: String
)

@Serializable
data class AgentMessagedEvent(
    @SerialName("agentMessage")
    val agentMessage: String
)

@Serializable
data class ProgressUpdatedEvent(
    @SerialName("title")
    val title: String = "",

    @SerialName("description")
    val description: String? = null
)

@Serializable
data class SessionCompletedEvent(
    @SerialName("completedAt")
    val completedAt: String? = null
)

@Serializable
data class SessionFailedEvent(
    @SerialName("reason")
    val reason: String? = null
)

@Serializable
data class JulesArtifact(
    @SerialName("changeSet")
    val changeSet: JulesChangeSet? = null,

    @SerialName("bashOutput")
    val bashOutput: JulesBashOutput? = null,

    @SerialName("media")
    val media: JulesMedia? = null
)

@Serializable
data class JulesChangeSet(
    @SerialName("source")
    val source: String? = null,

    @SerialName("gitPatch")
    val gitPatch: JulesGitPatch? = null
)

@Serializable
data class JulesGitPatch(
    @SerialName("baseCommitId")
    val baseCommitId: String? = null,

    @SerialName("unidiffPatch")
    val unidiffPatch: String? = null,

    @SerialName("suggestedCommitMessage")
    val suggestedCommitMessage: String? = null
)

@Serializable
data class JulesBashOutput(
    @SerialName("command")
    val command: String = "",

    @SerialName("output")
    val output: String = "",

    @SerialName("exitCode")
    val exitCode: Int = 0
)

@Serializable
data class JulesMedia(
    @SerialName("mimeType")
    val mimeType: String = "",

    @SerialName("data")
    val data: String = ""
)
