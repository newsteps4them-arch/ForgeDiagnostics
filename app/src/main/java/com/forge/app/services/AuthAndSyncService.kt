// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import com.forge.app.data.ChatMessageEntity
import com.forge.app.data.ForgeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val uid: String = "",
    val displayName: String = "Guest Tech",
    val email: String = "",
    val photoUrl: String = "",
    val role: String = "Guest",
    /**
     * Defaults to FALSE. This app has no Firebase Auth SDK integrated, so a
     * signed-in user must never be fabricated — see [AuthAndSyncService.signInWithGoogle].
     */
    val isAuthenticated: Boolean = false,
    val firestoreDbId: String = ""
)

data class SyncStatus(
    /**
     * Defaults to FALSE. No Firestore client SDK is integrated in this build,
     * so "sync active" must never be claimed — see [AuthAndSyncService.triggerFirestoreSync].
     */
    val isConnectedToFirestore: Boolean = false,
    val dbName: String = "",
    val lastSyncTime: Long = 0L,
    val syncedItemsCount: Int = 0,
    val statusText: String = "Cloud sync not configured — local Room database is the source of truth"
)

class AuthAndSyncService(
    private val repository: ForgeRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _currentUser = MutableStateFlow(UserProfile())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * Google/Firebase sign-in is NOT implemented in this build (no Firebase Auth
     * SDK is integrated). This function deliberately does NOT fabricate a
     * signed-in user: it leaves the session unauthenticated and says so, instead
     * of inventing a uid and claiming success.
     */
    fun signInWithGoogle(email: String = "", name: String = "") {
        _currentUser.value = UserProfile(
            uid = "",
            displayName = "Guest Tech",
            email = email,
            photoUrl = "",
            role = "Guest",
            isAuthenticated = false,
            firestoreDbId = ""
        )
        _syncStatus.value = _syncStatus.value.copy(
            isConnectedToFirestore = false,
            statusText = "Sign-in unavailable — Firebase Auth is not integrated in this build"
        )
    }

    fun signOut() {
        _currentUser.value = UserProfile()
        _syncStatus.value = _syncStatus.value.copy(
            isConnectedToFirestore = false,
            statusText = "Signed Out - Offline Local Storage Mode"
        )
    }

    /**
     * No Firestore client SDK is integrated in this build, so there is nothing
     * to sync with. This is an honest no-op: it reports "not configured" instead
     * of waiting 800ms and pretending a cloud sync completed.
     */
    fun triggerFirestoreSync() {
        _syncStatus.value = _syncStatus.value.copy(
            isConnectedToFirestore = false,
            statusText = "Cloud sync not configured — data stays in the local Room database"
        )
    }

    suspend fun saveChatMessageToLongTermMemory(
        sender: String,
        text: String,
        skillName: String,
        vehicleVin: String,
        projectTitle: String
    ) {
        val entity = ChatMessageEntity(
            sender = sender,
            text = text,
            skillName = skillName,
            vehicleVin = vehicleVin,
            projectTitle = projectTitle,
            timestamp = System.currentTimeMillis()
        )
        repository?.addChatMessage(entity)
        triggerFirestoreSync()
    }
}
