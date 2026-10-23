package com.forge.app

import com.forge.app.services.AuthAndSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun testSignInWithGoogle() = runTest {
        val authService = AuthAndSyncService()

        val testEmail = "testuser@example.com"
        val testName = "Test User"

        authService.signInWithGoogle(testEmail, testName)

        val currentUser = authService.currentUser.value

        assertTrue(currentUser.uid.startsWith("usr_tf_google_"))
        assertEquals(testName, currentUser.displayName)
        assertEquals(testEmail, currentUser.email)
        assertTrue(currentUser.isAuthenticated)
        assertEquals("Master Workshop Tech & ECU Tuner", currentUser.role)

        val syncStatus = authService.syncStatus.value

        assertTrue(syncStatus.isConnectedToFirestore)
        assertTrue(syncStatus.statusText.contains("Syncing with Firestore"))
    }
}
