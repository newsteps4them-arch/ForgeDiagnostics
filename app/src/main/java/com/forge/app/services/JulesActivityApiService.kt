// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * JulesActivityApiService defines the Retrofit REST client interface
 * specifically for monitoring progress, retrieving messages, and accessing artifacts
 * from the Jules Activities endpoint.
 *
 * Base Endpoint: https://jules.googleapis.com/v1alpha/sessions/{sessionId}/activities
 */
interface JulesActivityApiService {

    companion object {
        const val BASE_URL = "https://jules.googleapis.com/v1alpha/"

        /**
         * Factory function to instantiate the Retrofit JulesActivityApiService client.
         */
        fun create(customApiKey: String? = null): JulesActivityApiService {
            return JulesRetrofitClient.createActivityService(customApiKey)
        }
    }

    /**
     * Lists all activities (events, messages, plans, diffs, bash outputs) for a session.
     * GET https://jules.googleapis.com/v1alpha/sessions/{sessionId}/activities
     *
     * @param apiKey Google AI Studio / Jules API key passed in the x-goog-api-key header.
     * @param sessionId The unique ID of the target session.
     * @param pageSize Maximum number of activities to return per page (e.g., 20 or 30).
     * @param pageToken Token for pagination to retrieve the next page of activities.
     * @param createTime Filter activities occurring after the specified RFC 3339 timestamp.
     * @return [ListActivitiesResponse] containing the list of [JulesActivity] items and nextPageToken.
     */
    @GET("sessions/{sessionId}/activities")
    suspend fun listActivities(
        @Header("x-goog-api-key") apiKey: String,
        @Path("sessionId") sessionId: String,
        @Query("pageSize") pageSize: Int = 30,
        @Query("pageToken") pageToken: String? = null,
        @Query("createTime") createTime: String? = null
    ): ListActivitiesResponse

    /**
     * Retrieves a single activity by its unique ID.
     * GET https://jules.googleapis.com/v1alpha/sessions/{sessionId}/activities/{activityId}
     *
     * @param apiKey Google AI Studio / Jules API key passed in the x-goog-api-key header.
     * @param sessionId The unique ID of the target session.
     * @param activityId The unique ID of the activity (e.g., "act1", "act2").
     * @return Full [JulesActivity] object including plan steps, messages, or execution artifacts.
     */
    @GET("sessions/{sessionId}/activities/{activityId}")
    suspend fun getActivity(
        @Header("x-goog-api-key") apiKey: String,
        @Path("sessionId") sessionId: String,
        @Path("activityId") activityId: String
    ): JulesActivity
}
