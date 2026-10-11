package com.forge.app

import com.forge.app.services.AuthAndSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for honest auth/sync behavior:
 * - The service must NEVER fabricate a signed-in user (no Firebase Auth SDK exists).
 * - The service must NEVER claim a Firestore sync succeeded (no Firestore SDK exists).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthAndSyncServiceTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun testInitialStateIsUnauthenticatedGuest() = runTest {
        val service = AuthAndSyncService(scope = CoroutineScope(testDispatcher))

        // Honest default: nobody is signed in, nothing is synced.
        val user = service.currentUser.value
        assertEquals("", user.uid)
        assertEquals("Guest Tech", user.displayName)
        assertFalse(user.isAuthenticated)

        val sync = service.syncStatus.value
        assertFalse(sync.isConnectedToFirestore)
        assertEquals(0, sync.syncedItemsCount)
    }

    @Test
    fun testSignOutKeepsHonestGuestState() = runTest {
        val service = AuthAndSyncService(scope = CoroutineScope(testDispatcher))

        service.signOut()

        val currentUser = service.currentUser.value
        assertEquals("", currentUser.uid)
        assertEquals("Guest Tech", currentUser.displayName)
        assertEquals("", currentUser.email)
        assertFalse(currentUser.isAuthenticated)

        val syncStatus = service.syncStatus.value
        assertFalse(syncStatus.isConnectedToFirestore)
        assertEquals("Signed Out - Offline Local Storage Mode", syncStatus.statusText)
    }

    @Test
    fun testSignInWithGoogleDoesNotFabricateAuth() = runTest {
        val authService = AuthAndSyncService(scope = CoroutineScope(testDispatcher))

        authService.signInWithGoogle("testuser@example.com", "Test User")

        // MUST stay unauthenticated: Firebase Auth is not integrated, so success
        // must not be invented.
        val currentUser = authService.currentUser.value
        assertFalse(currentUser.isAuthenticated)
        assertEquals("", currentUser.uid)

        val syncStatus = authService.syncStatus.value
        assertFalse(syncStatus.isConnectedToFirestore)
    }

    @Test
    fun testTriggerFirestoreSyncDoesNotClaimSuccess() = runTest {
        val service = AuthAndSyncService(scope = CoroutineScope(testDispatcher))

        service.triggerFirestoreSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // MUST NOT claim a sync happened: no Firestore SDK is integrated.
        val sync = service.syncStatus.value
        assertFalse(sync.isConnectedToFirestore)
        assertEquals(0, sync.syncedItemsCount)
    }
}
