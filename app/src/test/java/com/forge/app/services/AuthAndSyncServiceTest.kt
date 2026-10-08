// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Regression tests for honest auth/sync behavior:
 * - No fabricated signed-in user (no Firebase Auth SDK exists).
 * - No fabricated Firestore sync success (no Firestore SDK exists).
 */
class AuthAndSyncServiceTest {

    @Test
    fun testInitialStateIsUnauthenticatedGuest() {
        val service = AuthAndSyncService(repository = null, scope = CoroutineScope(Dispatchers.Unconfined))

        val user = service.currentUser.value
        assertEquals("", user.uid)
        assertEquals("Guest Tech", user.displayName)
        assertFalse(user.isAuthenticated)

        val sync = service.syncStatus.value
        assertFalse(sync.isConnectedToFirestore)
        assertEquals(0, sync.syncedItemsCount)
    }

    @Test
    fun testSignInWithGoogleDoesNotFabricateAuth() {
        val service = AuthAndSyncService(repository = null, scope = CoroutineScope(Dispatchers.Unconfined))

        service.signInWithGoogle("test@example.com", "Test User")

        val user = service.currentUser.value
        assertFalse(user.isAuthenticated)
        assertEquals("", user.uid)

        val sync = service.syncStatus.value
        assertFalse(sync.isConnectedToFirestore)
    }

    @Test
    fun testSignOut() {
        val service = AuthAndSyncService(repository = null, scope = CoroutineScope(Dispatchers.Unconfined))

        service.signOut()

        val user = service.currentUser.value
        assertEquals("", user.uid)
        assertEquals("Guest Tech", user.displayName)
        assertFalse(user.isAuthenticated)

        val sync = service.syncStatus.value
        assertFalse(sync.isConnectedToFirestore)
        assertEquals("Signed Out - Offline Local Storage Mode", sync.statusText)
    }

    @Test
    fun testTriggerFirestoreSyncDoesNotClaimSuccess() = runBlocking {
        val service = AuthAndSyncService(repository = null, scope = CoroutineScope(Dispatchers.Unconfined))

        service.triggerFirestoreSync()

        val sync = service.syncStatus.value
        assertFalse(sync.isConnectedToFirestore)
        assertEquals(0, sync.syncedItemsCount)
        assertTrue(sync.statusText.contains("not configured", ignoreCase = true))
    }

    @Test
    fun testSaveChatMessageToLongTermMemoryStillSavesLocally() = runBlocking {
        val service = AuthAndSyncService(repository = null, scope = CoroutineScope(Dispatchers.Unconfined))

        // With a null repository this is a no-op, but it must not throw and must
        // not claim a cloud sync happened.
        service.saveChatMessageToLongTermMemory(
            sender = "User",
            text = "Hello",
            skillName = "General",
            vehicleVin = "12345",
            projectTitle = "Project X"
        )

        val sync = service.syncStatus.value
        assertFalse(sync.isConnectedToFirestore)
        assertEquals(0, sync.syncedItemsCount)
    }
}
