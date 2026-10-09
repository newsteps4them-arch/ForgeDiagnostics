// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

/**
 * Provenance of diagnostic data shown in the app.
 *
 * Every telemetry snapshot, DTC record, and generated report must carry one of
 * these values so simulated/demo content can NEVER be mistaken for readings
 * taken from a real vehicle through a physical OBD-II adapter.
 */
enum class DiagnosticDataSource {
    /** Data was read from a physically connected OBD-II adapter / ECU. */
    LIVE_HARDWARE,

    /** Explicitly labeled simulation / demo mode. Never a live vehicle. */
    SIMULATED,

    /** Provenance not yet established (e.g. before any poll has run). */
    UNKNOWN
}
