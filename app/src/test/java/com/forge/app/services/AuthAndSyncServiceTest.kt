package com.forge.app.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthAndSyncServiceTest {

    @Test
    fun testInitialState() {
        val service = AuthAndSyncService(repository = null, scope = CoroutineScope(Dispatchers.Unconfined))

        val user = service.currentUser.value
        assertEquals("usr_tf_lead_8841", user.uid)
        assertTrue(user.isAuthenticated)

        val sync = service.syncStatus.value
        assertTrue(sync.isConnectedToFirestore)
        assertEquals(48, sync.syncedItemsCount)
    }

    @Test
    fun testSignInWithGoogle() {
        val service = AuthAndSyncService(repository = null, scope = CoroutineScope(Dispatchers.Unconfined))

        service.signInWithGoogle("test@example.com", "Test User")

        val user = service.currentUser.value
        assertTrue(user.uid.startsWith("usr_tf_google_"))
        assertEquals("test@example.com", user.email)
        assertEquals("Test User", user.displayName)
        assertTrue(user.isAuthenticated)

        val sync = service.syncStatus.value
        assertTrue(sync.isConnectedToFirestore)
        assertTrue(sync.statusText.contains("Syncing with Firestore"))
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
    fun testTriggerFirestoreSync() = runBlocking {
        val service = AuthAndSyncService(repository = null, scope = CoroutineScope(Dispatchers.Unconfined))

        val initialCount = service.syncStatus.value.syncedItemsCount

        service.triggerFirestoreSync()

        val intermediateSync = service.syncStatus.value
        assertTrue(intermediateSync.statusText.contains("Syncing with Firestore"))
        assertEquals(initialCount, intermediateSync.syncedItemsCount)

        // Wait for the delay(800) to complete in the background coroutine
        Thread.sleep(1200)

        val finalSync = service.syncStatus.value
        assertTrue(finalSync.statusText.contains("Firestore Synced"))
        assertEquals(initialCount + 1, finalSync.syncedItemsCount)
    }

    @Test
    fun testSaveChatMessageToLongTermMemory() = runBlocking {
        val service = AuthAndSyncService(repository = null, scope = CoroutineScope(Dispatchers.Unconfined))

        val initialCount = service.syncStatus.value.syncedItemsCount

        service.saveChatMessageToLongTermMemory(
            sender = "User",
            text = "Hello",
            skillName = "General",
            vehicleVin = "12345",
            projectTitle = "Project X"
        )

        val intermediateSync = service.syncStatus.value
        assertTrue(intermediateSync.statusText.contains("Syncing with Firestore"))

        // Wait for the delay(800) to complete in the background coroutine
        Thread.sleep(1200)

        val finalSync = service.syncStatus.value
        assertEquals(initialCount + 1, finalSync.syncedItemsCount)
    }
}
