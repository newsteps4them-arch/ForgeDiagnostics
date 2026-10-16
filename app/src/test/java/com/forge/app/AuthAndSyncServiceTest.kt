// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app

import com.forge.app.services.AuthAndSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthAndSyncServiceTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSignOutResetsUserAndSyncStatus() = runTest {
        val service = AuthAndSyncService(scope = CoroutineScope(testDispatcher))

        // Initial state should be authenticated (based on the class's default initialization)
        assertTrue(service.currentUser.value.isAuthenticated)
        assertTrue(service.syncStatus.value.isConnectedToFirestore)

        // Action
        service.signOut()

        // Verification - User
        val currentUser = service.currentUser.value
        assertEquals("", currentUser.uid)
        assertEquals("Guest Tech", currentUser.displayName)
        assertEquals("", currentUser.email)
        assertEquals("", currentUser.photoUrl)
        assertEquals("Guest", currentUser.role)
        assertFalse(currentUser.isAuthenticated)
        assertEquals("ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9", currentUser.firestoreDbId)

        // Verification - Sync
        val syncStatus = service.syncStatus.value
        assertFalse(syncStatus.isConnectedToFirestore)
        assertEquals("Signed Out - Offline Local Storage Mode", syncStatus.statusText)
    }

    @Test
    fun testSignInWithGoogle() = runTest {
        val authService = AuthAndSyncService(scope = CoroutineScope(testDispatcher))

        val testEmail = "testuser@example.com"
        val testName = "Test User"

        // Sign out first to ensure state change
        authService.signOut()
        assertFalse(authService.currentUser.value.isAuthenticated)

        authService.signInWithGoogle(testEmail, testName)

        val currentUser = authService.currentUser.value

        assertTrue(currentUser.uid.startsWith("usr_tf_google_"))
        assertEquals(testName, currentUser.displayName)
        assertEquals(testEmail, currentUser.email)
        assertTrue(currentUser.isAuthenticated)
        assertEquals("Master Workshop Tech & ECU Tuner", currentUser.role)

        val syncStatus = authService.syncStatus.value

        assertTrue(syncStatus.isConnectedToFirestore)
        assertTrue(
            syncStatus.statusText.contains("Syncing with Firestore") ||
            syncStatus.statusText.contains("Firestore Synced")
        )
    }
}
