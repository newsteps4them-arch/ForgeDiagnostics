// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

class ForgeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeFirebaseCrashlytics()
    }

    private fun initializeFirebaseCrashlytics() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            val crashlytics = FirebaseCrashlytics.getInstance()
            // Enable Crashlytics telemetry collection
            crashlytics.setCrashlyticsCollectionEnabled(true)
            
            // Set standard production environment metadata
            crashlytics.setCustomKey("app_id", BuildConfig.APPLICATION_ID)
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
            crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlytics.setCustomKey("environment", if (BuildConfig.DEBUG) "development" else "production")
            
            crashlytics.log("Team Forge Application initialized. Telemetry monitoring active.")
            try { Log.i(TAG, "Firebase Crashlytics SDK initialized successfully.") } catch (_: Throwable) {}
        } catch (t: Throwable) {
            try { Log.w(TAG, "Firebase Crashlytics auto-initialization deferred/handled: ${t.message}") } catch (_: Throwable) {}
        }
    }

    companion object {
        private const val TAG = "ForgeApplication"
        lateinit var instance: ForgeApplication
            private set

        /**
         * Log an operational breadcrumb to Crashlytics
         */
        fun logEvent(message: String) {
            try {
                FirebaseCrashlytics.getInstance().log(message)
            } catch (_: Throwable) {
                // Safe in tests or uninitialized environments
            }
        }

        /**
         * Report non-fatal diagnostic, hardware, or network exceptions
         */
        fun recordException(throwable: Throwable, contextTag: String? = null) {
            try {
                val crashlytics = FirebaseCrashlytics.getInstance()
                if (contextTag != null) {
                    crashlytics.setCustomKey("last_error_tag", contextTag)
                }
                crashlytics.recordException(throwable)
            } catch (_: Throwable) {
                // Safe in tests or uninitialized environments
            }
        }

        /**
         * Set current connected vehicle metadata for crash diagnostics context
         */
        fun setVehicleContext(vin: String, model: String, protocol: String) {
            try {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.setCustomKey("vehicle_vin", vin)
                crashlytics.setCustomKey("vehicle_model", model)
                crashlytics.setCustomKey("obd_protocol", protocol)
            } catch (_: Throwable) {
                // Safe in tests or uninitialized environments
            }
        }

        /**
         * Set technician/user ID for session tracking
         */
        fun setUserId(userId: String) {
            try {
                FirebaseCrashlytics.getInstance().setUserId(userId)
            } catch (t: Throwable) {
                try { Log.d(TAG, "Crashlytics setUserId: $userId") } catch (_: Throwable) {}
            }
        }
    }
}
