package com.forge.app

import com.forge.app.services.AuthAndSyncService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AuthAndSyncServiceTest {

    @Test
    fun testSignOutResetsUserAndSyncStatus() {
        val service = AuthAndSyncService(scope = CoroutineScope(Dispatchers.Unconfined))

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
    fun testSignInWithGoogleUpdatesUserAndSyncStatus() {
        val service = AuthAndSyncService(scope = CoroutineScope(Dispatchers.Unconfined))

        // Sign out first to ensure state change
        service.signOut()
        assertFalse(service.currentUser.value.isAuthenticated)

        // Action
        service.signInWithGoogle(email = "test@example.com", name = "Test User")

        // Verification - User
        val currentUser = service.currentUser.value
        assertTrue(currentUser.uid.startsWith("usr_tf_google_"))
        assertEquals("Test User", currentUser.displayName)
        assertEquals("test@example.com", currentUser.email)
        assertEquals("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", currentUser.photoUrl)
        assertEquals("Master Workshop Tech & ECU Tuner", currentUser.role)
        assertTrue(currentUser.isAuthenticated)
        assertEquals("ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9", currentUser.firestoreDbId)

        // Verification - Sync (triggerFirestoreSync is called)
        val syncStatus = service.syncStatus.value
        assertTrue(syncStatus.isConnectedToFirestore)
        // Check for either the initial syncing status or the final synced status
        // due to coroutine launch inside triggerFirestoreSync
        assertTrue(
            syncStatus.statusText.contains("Syncing with Firestore") ||
            syncStatus.statusText.contains("Firestore Synced")
        )
    }
}
