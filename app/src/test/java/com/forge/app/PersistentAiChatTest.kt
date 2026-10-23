package com.forge.app

import com.forge.app.services.*
import org.junit.Assert.*
import org.junit.Test

class PersistentAiChatTest {

    @Test
    fun testScreenAiContextRegistryMapping() {
        val routesToTest = listOf(
            "dashboard",
            "live_data",
            "dyno",
            "topology",
            "guided_diag",
            "actuators",
            "oscilloscope",
            "terminal",
            "garage",
            "inventory",
            "estimator",
            "dvi",
            "wiring",
            "time_clock",
            "crm",
            "orchestrator",
            "settings"
        )

        for (route in routesToTest) {
            val context = ScreenAiContextRegistry.getContextForRoute(route)
            assertNotNull("Context info should exist for route $route", context)
            assertEquals("Route should match", route, context.route)
            assertTrue("Title should not be blank", context.title.isNotBlank())
            assertTrue("Specialist name should not be blank", context.specialistName.isNotBlank())
            assertTrue("Suggested prompts should not be empty", context.suggestedPrompts.isNotEmpty())
            assertTrue("Context tag should not be blank", context.contextTag.isNotBlank())
        }
    }

    @Test
    fun testDefaultFallbackContext() {
        val unknownContext = ScreenAiContextRegistry.getContextForRoute("unknown_custom_route_99")
        assertNotNull(unknownContext)
        assertEquals("dashboard", unknownContext.route)
    }

    @Test
    fun testAssistantSkillSpecializations() {
        assertEquals("High-Frequency PID & Fuel Trim Math Specialist", ScreenAiContextRegistry.getContextForRoute("live_data").specialistName)
        assertEquals("Circuit Blueprint & Harness Pinout Specialist", ScreenAiContextRegistry.getContextForRoute("wiring").specialistName)
        assertEquals("Waveform Analysis & Signal Math Specialist", ScreenAiContextRegistry.getContextForRoute("oscilloscope").specialistName)
        assertEquals("Deep Fault Tree & Root-Cause Reasoning Specialist", ScreenAiContextRegistry.getContextForRoute("guided_diag").specialistName)
    }
}
