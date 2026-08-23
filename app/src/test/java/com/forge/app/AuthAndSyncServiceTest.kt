package com.forge.app

import com.forge.app.services.AuthAndSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthAndSyncServiceTest {
    @Test
    fun testTriggerFirestoreSync() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = CoroutineScope(testDispatcher)

        val service = AuthAndSyncService(repository = null, scope = testScope)

        val initialSyncedItemsCount = service.syncStatus.value.syncedItemsCount

        service.triggerFirestoreSync()

        val intermediateStatus = service.syncStatus.value
        assertTrue(intermediateStatus.isConnectedToFirestore)
        assertTrue(intermediateStatus.statusText.startsWith("Syncing with Firestore"))
        assertEquals(initialSyncedItemsCount, intermediateStatus.syncedItemsCount)

        advanceTimeBy(801)

        val finalStatus = service.syncStatus.value
        assertTrue(finalStatus.statusText.startsWith("Firestore Synced"))
        assertEquals(initialSyncedItemsCount + 1, finalStatus.syncedItemsCount)
    }
}
