import java.util.Properties
import java.io.FileInputStream
import org.gradle.api.Action
import org.gradle.api.execution.TaskExecutionGraph

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.secrets)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

// Upload-keystore values for release signing. NEVER committed to the repo —
// generate the keystore once (see PLAYSTORE_RELEASE.md) and provide these via
// environment variables or local.properties.
val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE")
    ?: System.getenv("FORGE_RELEASE_STORE_FILE")
val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
    ?: System.getenv("FORGE_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
    ?: System.getenv("FORGE_RELEASE_KEY_ALIAS")
val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
    ?: System.getenv("FORGE_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = !releaseStoreFile.isNullOrBlank()
    && !releaseStorePassword.isNullOrBlank()
    && !releaseKeyAlias.isNullOrBlank()
    && !releaseKeyPassword.isNullOrBlank()

// Fail fast if anyone actually tries to build a release artifact without the
// upload keystore — instead of silently signing with the debug key. This check
// runs only when release tasks are in the task graph, so plain debug builds
// (and CI before the keystore exists) still configure and run.
gradle.taskGraph.whenReady(object : Action<TaskExecutionGraph> {
    override fun execute(graph: TaskExecutionGraph) {
        val buildingRelease = graph.allTasks.any { task ->
            task.name.contains("Release", ignoreCase = true)
        }
        if (buildingRelease && !hasReleaseSigning) {
            throw GradleException(
                "Release signing is not configured. Generate an upload keystore " +
                    "(see PLAYSTORE_RELEASE.md) and set FORGE_RELEASE_STORE_FILE, " +
                    "FORGE_RELEASE_STORE_PASSWORD, FORGE_RELEASE_KEY_ALIAS and " +
                    "FORGE_RELEASE_KEY_PASSWORD."
            )
        }
    }
})

android {
    namespace = "com.forge.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.forgediagnostics.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = localProperties.getProperty("DEBUG_STORE_PASSWORD") ?: System.getenv("DEBUG_STORE_PASSWORD") ?: "android"
            keyAlias = localProperties.getProperty("DEBUG_KEY_ALIAS") ?: System.getenv("DEBUG_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = localProperties.getProperty("DEBUG_KEY_PASSWORD") ?: System.getenv("DEBUG_KEY_PASSWORD") ?: "android"
        }
        // Release signing comes from the upload keystore (see the top-level
        // hasReleaseSigning block). When the keystore is absent the config
        // below holds placeholder values so the project still configures;
        // an actual release build without the keystore is rejected by the
        // taskGraph check above — it can never silently use the debug key.
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            } else {
                storeFile = file("${rootDir}/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.coil.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}