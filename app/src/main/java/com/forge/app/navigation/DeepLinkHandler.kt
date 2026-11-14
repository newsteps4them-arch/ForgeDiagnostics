// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.navigation

import android.net.Uri

sealed class DeepLinkDestination {
    data class DtcDetail(val dtcCode: String) : DeepLinkDestination()
    data class VehicleDetail(val vin: String) : DeepLinkDestination()
    data class ProjectDetail(val projectId: Long) : DeepLinkDestination()
    data class ScreenRoute(val route: String) : DeepLinkDestination()
    data object Unknown : DeepLinkDestination()
}

object DeepLinkHandler {
    private const val SCHEME_CUSTOM = "forge"
    private const val SCHEME_HTTP = "http"
    private const val SCHEME_HTTPS = "https"
    private const val HOST_FORGE = "forgediagnostics.com"
    private const val HOST_APP = "forge.app"

    fun parse(uri: Uri?): DeepLinkDestination {
        if (uri == null) return DeepLinkDestination.Unknown
        return parseUrlString(uri.toString())
    }

    fun parseUrlString(urlString: String?): DeepLinkDestination {
        if (urlString.isNullOrBlank()) return DeepLinkDestination.Unknown
        return try {
            val uri = java.net.URI(urlString)
            val scheme = uri.scheme?.lowercase() ?: return DeepLinkDestination.Unknown
            val host = uri.host?.lowercase() ?: uri.authority?.lowercase()

            val isCustomScheme = scheme == SCHEME_CUSTOM
            val isWebScheme = (scheme == SCHEME_HTTP || scheme == SCHEME_HTTPS) &&
                    (host == HOST_FORGE || host == HOST_APP)

            if (!isCustomScheme && !isWebScheme) {
                return DeepLinkDestination.Unknown
            }

            val path = uri.path ?: ""
            val rawSegments = path.split("/").filter { it.isNotBlank() }

            if (isCustomScheme) {
                val authority = uri.authority?.lowercase()
                if (authority != null && authority != "app" && authority != "main") {
                    return parsePathAndQuery(authority, rawSegments, uri.query)
                }
            }

            if (rawSegments.isEmpty()) {
                return DeepLinkDestination.ScreenRoute("dashboard")
            }

            val relevantSegments = if (rawSegments.firstOrNull()?.lowercase() == "dl") {
                rawSegments.drop(1)
            } else {
                rawSegments
            }

            val firstSegment = relevantSegments.firstOrNull()?.lowercase()
                ?: return DeepLinkDestination.ScreenRoute("dashboard")

            val remainingSegments = relevantSegments.drop(1)

            parsePathAndQuery(firstSegment, remainingSegments, uri.query)
        } catch (e: Exception) {
            DeepLinkDestination.Unknown
        }
    }

    private fun parsePathAndQuery(
        category: String,
        subSegments: List<String>,
        query: String?
    ): DeepLinkDestination {
        val queryMap = parseQueryString(query)
        return when (category) {
            "dtc", "code" -> {
                val code = subSegments.firstOrNull() ?: queryMap["code"] ?: queryMap["q"]
                if (!code.isNullOrBlank()) {
                    DeepLinkDestination.DtcDetail(code.uppercase())
                } else {
                    DeepLinkDestination.ScreenRoute("guided_diag")
                }
            }
            "vehicle", "vin" -> {
                val vin = subSegments.firstOrNull() ?: queryMap["vin"]
                if (!vin.isNullOrBlank()) {
                    DeepLinkDestination.VehicleDetail(vin.uppercase())
                } else {
                    DeepLinkDestination.ScreenRoute("garage")
                }
            }
            "project", "workorder" -> {
                val idStr = subSegments.firstOrNull() ?: queryMap["id"]
                val id = idStr?.toLongOrNull()
                if (id != null) {
                    DeepLinkDestination.ProjectDetail(id)
                } else {
                    DeepLinkDestination.ScreenRoute("dashboard")
                }
            }
            "screen", "route" -> {
                val target = subSegments.firstOrNull() ?: queryMap["name"] ?: "dashboard"
                DeepLinkDestination.ScreenRoute(target.lowercase())
            }
            "live_data", "topology", "guided_diag", "oscilloscope", "terminal",
            "garage", "inventory", "estimator", "dvi", "wiring", "time_clock",
            "crm", "orchestrator", "openmanus", "settings", "dashboard" -> {
                DeepLinkDestination.ScreenRoute(category)
            }
            else -> DeepLinkDestination.Unknown
        }
    }

    private fun parseQueryString(query: String?): Map<String, String> {
        if (query.isNullOrBlank()) return emptyMap()
        return query.split("&").mapNotNull { pair ->
            val parts = pair.split("=")
            if (parts.size >= 2) {
                parts[0].lowercase() to parts[1]
            } else null
        }.toMap()
    }
}
