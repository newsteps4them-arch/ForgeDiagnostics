// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkHandlerTest {

    @Test
    fun testNullOrEmptyUriReturnsUnknown() {
        assertEquals(DeepLinkDestination.Unknown, DeepLinkHandler.parse(null))
        assertEquals(DeepLinkDestination.Unknown, DeepLinkHandler.parseUrlString(null))
        assertEquals(DeepLinkDestination.Unknown, DeepLinkHandler.parseUrlString(""))
    }

    @Test
    fun testCustomSchemeDtcParsing() {
        val dest = DeepLinkHandler.parseUrlString("forge://dtc/P0300")
        assertTrue(dest is DeepLinkDestination.DtcDetail)
        assertEquals("P0300", (dest as DeepLinkDestination.DtcDetail).dtcCode)
    }

    @Test
    fun testCustomSchemeVehicleVinParsing() {
        val dest = DeepLinkHandler.parseUrlString("forge://vehicle/WAUZZZF58MA019284")
        assertTrue(dest is DeepLinkDestination.VehicleDetail)
        assertEquals("WAUZZZF58MA019284", (dest as DeepLinkDestination.VehicleDetail).vin)
    }

    @Test
    fun testCustomSchemeProjectParsing() {
        val dest = DeepLinkHandler.parseUrlString("forge://project/42")
        assertTrue(dest is DeepLinkDestination.ProjectDetail)
        assertEquals(42L, (dest as DeepLinkDestination.ProjectDetail).projectId)
    }

    @Test
    fun testWebSchemeDeepLinkParsing() {
        val dest = DeepLinkHandler.parseUrlString("https://forgediagnostics.com/dl/dtc/P0171")
        assertTrue(dest is DeepLinkDestination.DtcDetail)
        assertEquals("P0171", (dest as DeepLinkDestination.DtcDetail).dtcCode)
    }

    @Test
    fun testScreenRouteParsing() {
        val dest = DeepLinkHandler.parseUrlString("forge://screen/live_data")
        assertTrue(dest is DeepLinkDestination.ScreenRoute)
        assertEquals("live_data", (dest as DeepLinkDestination.ScreenRoute).route)
    }

    @Test
    fun testUnknownSchemeReturnsUnknown() {
        val dest = DeepLinkHandler.parseUrlString("randomscheme://dtc/P0300")
        assertEquals(DeepLinkDestination.Unknown, dest)
    }
}
