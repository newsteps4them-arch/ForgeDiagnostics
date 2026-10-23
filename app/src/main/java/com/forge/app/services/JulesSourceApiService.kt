package com.forge.app.services

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * JulesSourceApiService defines the Retrofit REST client interface
 * specifically for discovering and retrieving repository sources connected to Jules.
 *
 * Base Endpoint: https://jules.googleapis.com/v1alpha/sources
 */
interface JulesSourceApiService {

    companion object {
        const val BASE_URL = "https://jules.googleapis.com/v1alpha/"

        /**
         * Factory function to instantiate the Retrofit JulesSourceApiService client.
         */
        fun create(customApiKey: String? = null): JulesSourceApiService {
            return JulesRetrofitClient.createSourceService(customApiKey)
        }
    }

    /**
     * Lists all sources (repositories) connected to your account.
     * GET https://jules.googleapis.com/v1alpha/sources
     *
     * @param apiKey Google AI Studio / Jules API key passed in the x-goog-api-key header.
     * @param pageSize Maximum number of sources to return per page.
     * @param pageToken Token for pagination to fetch subsequent pages.
     * @param filter Query filter expression (e.g., "name=sources/github-myorg-myrepo" or "name=sources/src1 OR name=sources/src2").
     * @return [ListSourcesResponse] containing the list of [JulesSource] items and nextPageToken.
     */
    @GET("sources")
    suspend fun listSources(
        @Header("x-goog-api-key") apiKey: String,
        @Query("pageSize") pageSize: Int = 20,
        @Query("pageToken") pageToken: String? = null,
        @Query("filter") filter: String? = null
    ): ListSourcesResponse

    /**
     * Retrieves a single source by ID.
     * GET https://jules.googleapis.com/v1alpha/sources/{sourceId}
     *
     * @param apiKey Google AI Studio / Jules API key passed in the x-goog-api-key header.
     * @param sourceId The unique identifier or slug of the source (e.g., "github-myorg-myrepo").
     * @return Full [JulesSource] model including githubRepo, privacy flags, and branches.
     */
    @GET("sources/{sourceId}")
    suspend fun getSource(
        @Header("x-goog-api-key") apiKey: String,
        @Path("sourceId") sourceId: String
    ): JulesSource
}
