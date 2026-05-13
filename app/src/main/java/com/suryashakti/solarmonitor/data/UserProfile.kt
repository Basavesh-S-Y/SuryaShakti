package com.suryashakti.solarmonitor.data

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = 0L,
    val isGoogleAccount: Boolean = false
) {
    val initials: String
        get() {
            val parts = displayName.trim().split(" ")
            return when {
                parts.size >= 2 -> "${parts[0][0]}${parts[1][0]}".uppercase()
                parts.size == 1 && parts[0].isNotEmpty() -> parts[0][0].uppercase()
                email.isNotEmpty() -> email[0].uppercase()
                else -> "U"
            }
        }

    val shortName: String
        get() = displayName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
}

sealed class AuthState {
    object Idle       : AuthState()
    object Checking   : AuthState()
    object LoggingIn  : AuthState()
    object Registering: AuthState()
    object SendingReset: AuthState()
    data class LoggedIn(val user: UserProfile) : AuthState()
    object LoggedOut  : AuthState()
    data class Error(val message: String)      : AuthState()
    object ResetSent  : AuthState()
}

sealed class SyncState {
    object Idle      : SyncState()
    object Syncing   : SyncState()
    data class Success(val uploaded: Int, val downloaded: Int) : SyncState()
    data class Error(val message: String)  : SyncState()
    object NotLoggedIn : SyncState()
}
