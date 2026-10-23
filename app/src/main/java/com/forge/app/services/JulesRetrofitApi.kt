package com.forge.app.services

import com.forge.app.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ============================================================================
// Jules REST API DTOs (https://jules.googleapis.com/v1alpha)
// ============================================================================

@Serializable
data class CreateSessionRequest(
    @SerialName("prompt") val prompt: String,
    @SerialName("title") val title: String? = null,
    @SerialName("sourceContext") val sourceContext: SourceContextDto,
    @SerialName("automationMode") val automationMode: String? = "AUTO_CREATE_PR",
    @SerialName("requirePlanApproval") val requirePlanApproval: Boolean? = false
)

@Serializable
data class SourceContextDto(
    @SerialName("source") val source: String,
    @SerialName("githubRepoContext") val githubRepoContext: GithubRepoContextDto? = GithubRepoContextDto("main")
)

@Serializable
data class GithubRepoContextDto(
    @SerialName("startingBranch") val startingBranch: String = "main"
)

@Serializable
data class SendMessageRequest(
    @SerialName("prompt") val prompt: String
)

@Serializable
data class ListSessionsResponse(
    @SerialName("sessions") val sessions: List<JulesSession> = emptyList(),
    @SerialName("nextPageToken") val nextPageToken: String? = null
)

@Serializable
data class ListSourcesResponse(
    @SerialName("sources") val sources: List<JulesSource> = emptyList(),
    @SerialName("nextPageToken") val nextPageToken: String? = null
)

@Serializable
data class ListActivitiesResponse(
    @SerialName("activities") val activities: List<JulesActivity> = emptyList(),
    @SerialName("nextPageToken") val nextPageToken: String? = null
)

// ============================================================================
// Retrofit Service Definition
// ============================================================================

interface JulesApiService {

    companion object {
        const val BASE_URL = "https://jules.googleapis.com/v1alpha/"

        fun create(customApiKey: String? = null): JulesApiService {
            return JulesRetrofitClient.create(customApiKey)
        }
    }

    /**
     * Create a new coding session.
     * POST https://jules.googleapis.com/v1alpha/sessions
     */
    @POST("sessions")
    suspend fun createSession(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: CreateSessionRequest
    ): JulesSession

    /**
     * List all sessions with pagination support.
     * GET https://jules.googleapis.com/v1alpha/sessions?pageSize=10&pageToken=...
     */
    @GET("sessions")
    suspend fun listSessions(
        @Header("x-goog-api-key") apiKey: String,
        @Query("pageSize") pageSize: Int = 10,
        @Query("pageToken") pageToken: String? = null
    ): ListSessionsResponse

    /**
     * Retrieve a specific session by ID.
     * GET https://jules.googleapis.com/v1alpha/sessions/{sessionId}
     */
    @GET("sessions/{sessionId}")
    suspend fun getSession(
        @Header("x-goog-api-key") apiKey: String,
        @Path("sessionId") sessionId: String
    ): JulesSession

    /**
     * Delete a session.
     * DELETE https://jules.googleapis.com/v1alpha/sessions/{sessionId}
     */
    @DELETE("sessions/{sessionId}")
    suspend fun deleteSession(
        @Header("x-goog-api-key") apiKey: String,
        @Path("sessionId") sessionId: String
    ): Response<Unit>

    /**
     * Send a follow-up message/instruction to Jules during an active session.
     * POST https://jules.googleapis.com/v1alpha/sessions/{sessionId}:sendMessage
     */
    @POST("sessions/{sessionId}:sendMessage")
    suspend fun sendMessage(
        @Header("x-goog-api-key") apiKey: String,
        @Path("sessionId") sessionId: String,
        @Body request: SendMessageRequest
    ): Response<Unit>

    /**
     * Approve a pending plan when requirePlanApproval was set to true.
     * POST https://jules.googleapis.com/v1alpha/sessions/{sessionId}:approvePlan
     */
    @POST("sessions/{sessionId}:approvePlan")
    suspend fun approvePlan(
        @Header("x-goog-api-key") apiKey: String,
        @Path("sessionId") sessionId: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<Unit>

    /**
     * List all connected repository sources.
     * GET https://jules.googleapis.com/v1alpha/sources?pageSize=10&filter=...
     */
    @GET("sources")
    suspend fun listSources(
        @Header("x-goog-api-key") apiKey: String,
        @Query("pageSize") pageSize: Int = 20,
        @Query("pageToken") pageToken: String? = null,
        @Query("filter") filter: String? = null
    ): ListSourcesResponse

    /**
     * Retrieve a single source by ID.
     * GET https://jules.googleapis.com/v1alpha/sources/{sourceId}
     */
    @GET("sources/{sourceId}")
    suspend fun getSource(
        @Header("x-goog-api-key") apiKey: String,
        @Path("sourceId") sourceId: String
    ): JulesSource

    /**
     * List activities within a session.
     * GET https://jules.googleapis.com/v1alpha/sessions/{sessionId}/activities
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
     * Retrieve a single activity by ID.
     * GET https://jules.googleapis.com/v1alpha/sessions/{sessionId}/activities/{activityId}
     */
    @GET("sessions/{sessionId}/activities/{activityId}")
    suspend fun getActivity(
        @Header("x-goog-api-key") apiKey: String,
        @Path("sessionId") sessionId: String,
        @Path("activityId") activityId: String
    ): JulesActivity
}

typealias JulesApi = JulesApiService

// ============================================================================
// Retrofit Client Builder
// ============================================================================

object JulesRetrofitClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun create(customApiKey: String? = null): JulesApi {
        val effectiveApiKey = customApiKey?.ifBlank { null }
            ?: (if (BuildConfig.GEMINI_API_KEY.contains("PLACEHOLDER")) "" else BuildConfig.GEMINI_API_KEY)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                if (original.header("x-goog-api-key") == null && effectiveApiKey.isNotBlank()) {
                    requestBuilder.header("x-goog-api-key", effectiveApiKey)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(JulesApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JulesApiService::class.java)
    }

    fun createActivityService(customApiKey: String? = null): JulesActivityApiService {
        val effectiveApiKey = customApiKey?.ifBlank { null }
            ?: (if (BuildConfig.GEMINI_API_KEY.contains("PLACEHOLDER")) "" else BuildConfig.GEMINI_API_KEY)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                if (original.header("x-goog-api-key") == null && effectiveApiKey.isNotBlank()) {
                    requestBuilder.header("x-goog-api-key", effectiveApiKey)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(JulesApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JulesActivityApiService::class.java)
    }

    fun createSourceService(customApiKey: String? = null): JulesSourceApiService {
        val effectiveApiKey = customApiKey?.ifBlank { null }
            ?: (if (BuildConfig.GEMINI_API_KEY.contains("PLACEHOLDER")) "" else BuildConfig.GEMINI_API_KEY)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                if (original.header("x-goog-api-key") == null && effectiveApiKey.isNotBlank()) {
                    requestBuilder.header("x-goog-api-key", effectiveApiKey)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(JulesApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JulesSourceApiService::class.java)
    }
}
