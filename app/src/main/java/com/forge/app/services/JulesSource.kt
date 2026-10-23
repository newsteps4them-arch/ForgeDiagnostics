package com.forge.app.services

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JulesSource represents a repository connected to Jules
 * (https://jules.googleapis.com/v1alpha/sources).
 */
@Serializable
data class JulesSource(
    /**
     * Resource name in the format: "sources/github-myorg-myrepo" or "sources/{sourceId}".
     */
    @SerialName("name")
    val name: String = "",

    /**
     * Unique identifier for the source (e.g., "github-myorg-myrepo").
     */
    @SerialName("id")
    val id: String? = null,

    /**
     * GitHub repository details including owner, repo name, privacy, default branch, and available branches.
     */
    @SerialName("githubRepo")
    val githubRepo: JulesGithubRepo? = null
) {
    val owner: String
        get() = githubRepo?.owner ?: ""

    val repo: String
        get() = githubRepo?.repo ?: ""

    val isPrivate: Boolean
        get() = githubRepo?.isPrivate ?: false

    val defaultBranchName: String
        get() = githubRepo?.defaultBranch?.displayName ?: "main"

    val branchNames: List<String>
        get() = githubRepo?.branches?.map { it.displayName } ?: listOf(defaultBranchName)
}

@Serializable
data class JulesGithubRepo(
    @SerialName("owner")
    val owner: String = "",

    @SerialName("repo")
    val repo: String = "",

    @SerialName("isPrivate")
    val isPrivate: Boolean = false,

    @SerialName("defaultBranch")
    val defaultBranch: JulesBranch? = null,

    @SerialName("branches")
    val branches: List<JulesBranch> = emptyList()
)

@Serializable
data class JulesBranch(
    @SerialName("displayName")
    val displayName: String = "main"
)
