// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app

import org.junit.Assert.*
import org.junit.Test

class CrashlyticsTelemetryTest {

    @Test
    fun testCrashlyticsLoggingAndExceptionDelegation() {
        // Test that logging and recording exceptions execute without uncaught crashes
        try {
            ForgeApplication.logEvent("Simulated diagnostic breadcrumb: Testing OBD-II stream")
            ForgeApplication.setVehicleContext(
                vin = "WAUZZZF58MA019284",
                model = "2021 Audi S5 Sportback",
                protocol = "ISO 15765-4 CAN"
            )
            ForgeApplication.setUserId("tech_user_forge_007")
            ForgeApplication.recordException(
                throwable = IllegalStateException("Simulated non-fatal telemetry parsing anomaly"),
                contextTag = "OBD_PARSER"
            )
            assertTrue(true)
        } catch (e: Throwable) {
            fail("Crashlytics wrapper methods should safely handle uninitialized or testing environments without throwing: ${e.message}")
        }
    }
}
