package com.forge.app.services

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.forge.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerialName("size") val size: Long = 0L
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList()
)

interface GitHubReleaseApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = "newsteps4them-arch",
        @Path("repo") repo: String = "ForgeDiagnostics"
    ): GitHubRelease

    companion object {
        const val BASE_URL = "https://api.github.com/"

        fun create(okHttpClient: OkHttpClient? = null): GitHubReleaseApiService {
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
            val client = okHttpClient ?: OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(GitHubReleaseApiService::class.java)
        }
    }
}

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(
        val release: GitHubRelease,
        val apkAsset: GitHubAsset,
        val latestVersion: String
    ) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(val progress: Float) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class UpdateManager(
    private val apiService: GitHubReleaseApiService = GitHubReleaseApiService.create(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
) {

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    suspend fun checkForUpdates(currentVersion: String = BuildConfig.VERSION_NAME): UpdateStatus = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        try {
            val release = apiService.getLatestRelease()
            val latestVersion = release.tagName.removePrefix("v").trim()
            val apkAsset = release.assets.find { it.name.endsWith(".apk") }

            if (apkAsset != null && isVersionNewer(installedVersion = currentVersion, latestVersion = latestVersion)) {
                val status = UpdateStatus.UpdateAvailable(
                    release = release,
                    apkAsset = apkAsset,
                    latestVersion = latestVersion
                )
                _updateStatus.value = status
                status
            } else {
                _updateStatus.value = UpdateStatus.UpToDate
                UpdateStatus.UpToDate
            }
        } catch (e: Exception) {
            val errorStatus = UpdateStatus.Error(e.localizedMessage ?: "Failed to check for updates")
            _updateStatus.value = errorStatus
            errorStatus
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        apkAsset: GitHubAsset
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Downloading(0f)
            val request = Request.Builder()
                .url(apkAsset.browserDownloadUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _updateStatus.value = UpdateStatus.Error("Failed to download APK: HTTP ${response.code}")
                return@withContext false
            }

            val body = response.body ?: run {
                _updateStatus.value = UpdateStatus.Error("Download response body was empty")
                return@withContext false
            }

            val apkDir = File(context.cacheDir, "apks").apply { if (!exists()) mkdirs() }
            val apkFile = File(apkDir, "ForgeUpdate.apk")

            body.byteStream().use { inputStream ->
                FileOutputStream(apkFile).use { outputStream ->
                    val totalBytes = body.contentLength()
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            _updateStatus.value = UpdateStatus.Downloading(progress)
                        }
                    }
                    outputStream.flush()
                }
            }

            _updateStatus.value = UpdateStatus.ReadyToInstall(apkFile)
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            true
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error(e.localizedMessage ?: "Error downloading APK")
            false
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun dismissUpdate() {
        _updateStatus.value = UpdateStatus.Idle
    }

    companion object {
        fun isVersionNewer(installedVersion: String, latestVersion: String): Boolean {
            val installedClean = installedVersion.removePrefix("v").trim()
            val latestClean = latestVersion.removePrefix("v").trim()
            if (installedClean == latestClean) return false

            val installedParts = installedClean.split(".", "_", "-")
            val latestParts = latestClean.split(".", "_", "-")

            val maxLen = maxOf(installedParts.size, latestParts.size)
            for (i in 0 until maxLen) {
                val instPart = installedParts.getOrNull(i)?.toLongOrNull() ?: 0L
                val latPart = latestParts.getOrNull(i)?.toLongOrNull() ?: 0L
                if (latPart > instPart) return true
                if (latPart < instPart) return false
            }

            // Fallback string compare if non-numeric parts differ
            return latestClean != installedClean
        }
    }
}
