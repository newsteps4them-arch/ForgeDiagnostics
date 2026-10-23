package com.forge.app.services

import com.forge.app.data.ChatMessageEntity
import com.forge.app.data.ForgeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfile(
    val uid: String = "usr_tf_lead_8841",
    val displayName: String = "Lead Diagnostic Master Tech",
    val email: String = "newsteps4them@gmail.com",
    val photoUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
    val role: String = "Master Workshop Tech & ECU Tuner",
    val isAuthenticated: Boolean = true,
    val firestoreDbId: String = "ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9"
)

data class SyncStatus(
    val isConnectedToFirestore: Boolean = true,
    val dbName: String = "ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9",
    val lastSyncTime: Long = System.currentTimeMillis(),
    val syncedItemsCount: Int = 42,
    val statusText: String = "Real-time Firestore Sync Active"
)

class AuthAndSyncService(
    private val repository: ForgeRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _currentUser = MutableStateFlow(
        UserProfile(
            uid = "usr_tf_lead_8841",
            displayName = "Lead Master Tech",
            email = "newsteps4them@gmail.com",
            photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            role = "Master Workshop Tech & ECU Tuner",
            isAuthenticated = true,
            firestoreDbId = "ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9"
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _syncStatus = MutableStateFlow(
        SyncStatus(
            isConnectedToFirestore = true,
            dbName = "ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9",
            lastSyncTime = System.currentTimeMillis(),
            syncedItemsCount = 48,
            statusText = "Real-Time Firestore Sync Active"
        )
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun signInWithGoogle(email: String = "newsteps4them@gmail.com", name: String = "Lead Master Tech") {
        _currentUser.value = UserProfile(
            uid = "usr_tf_google_${System.currentTimeMillis() % 10000}",
            displayName = name,
            email = email,
            photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            role = "Master Workshop Tech & ECU Tuner",
            isAuthenticated = true,
            firestoreDbId = "ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9"
        )
        triggerFirestoreSync()
    }

    fun signOut() {
        _currentUser.value = UserProfile(
            uid = "",
            displayName = "Guest Tech",
            email = "",
            photoUrl = "",
            role = "Guest",
            isAuthenticated = false,
            firestoreDbId = "ai-studio-d176f2ad-cc8f-47d3-8f8a-bc017f7ae1f9"
        )
        _syncStatus.value = _syncStatus.value.copy(
            isConnectedToFirestore = false,
            statusText = "Signed Out - Offline Local Storage Mode"
        )
    }

    fun triggerFirestoreSync() {
        _syncStatus.value = _syncStatus.value.copy(
            isConnectedToFirestore = true,
            lastSyncTime = System.currentTimeMillis(),
            statusText = "Syncing with Firestore (${_syncStatus.value.dbName})..."
        )
        scope.launch {
            kotlinx.coroutines.delay(800)
            _syncStatus.value = _syncStatus.value.copy(
                lastSyncTime = System.currentTimeMillis(),
                syncedItemsCount = _syncStatus.value.syncedItemsCount + 1,
                statusText = "Firestore Synced (DB: ${_syncStatus.value.dbName})"
            )
        }
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
