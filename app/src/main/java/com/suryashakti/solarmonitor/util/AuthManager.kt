package com.suryashakti.solarmonitor.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.suryashakti.solarmonitor.R
import com.suryashakti.solarmonitor.data.UserProfile
import kotlinx.coroutines.tasks.await

object AuthManager {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // ── Current state ─────────────────────────────────────────────────────

    val isLoggedIn: Boolean get() = auth.currentUser != null

    val currentUser: UserProfile?
        get() = auth.currentUser?.toProfile()

    // ── Email / Password ─────────────────────────────────────────────────

    suspend fun loginWithEmail(email: String, password: String): Result<UserProfile> =
        runCatching {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            result.user?.toProfile() ?: error("Login failed: no user returned")
        }.mapError()

    suspend fun registerWithEmail(
        name: String, email: String, password: String
    ): Result<UserProfile> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        // Attach display name
        val update = UserProfileChangeRequest.Builder()
            .setDisplayName(name.trim())
            .build()
        result.user?.updateProfile(update)?.await()
        result.user?.reload()?.await()
        result.user?.toProfile() ?: error("Registration failed: no user returned")
    }.mapError()

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }.mapError()

    // ── Google Sign-In ────────────────────────────────────────────────────

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getGoogleSignInIntent(context: Context): Intent =
        getGoogleSignInClient(context).signInIntent

    suspend fun handleGoogleSignInResult(account: GoogleSignInAccount): Result<UserProfile> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.toProfile() ?: error("Google sign-in failed")
        }.mapError()

    // ── Sign Out ──────────────────────────────────────────────────────────

    fun signOut(context: Context) {
        auth.signOut()
        getGoogleSignInClient(context).signOut()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun FirebaseUser.toProfile() = UserProfile(
        uid           = uid,
        displayName   = displayName ?: "",
        email         = email ?: "",
        photoUrl      = photoUrl?.toString() ?: "",
        isGoogleAccount = providerData.any { it.providerId == "google.com" }
    )

    private fun <T> Result<T>.mapError(): Result<T> = this.recoverCatching { e ->
        throw Exception(friendlyMessage(e.message ?: "Unknown error"))
    }

    private fun friendlyMessage(raw: String): String = when {
        "EMAIL_NOT_FOUND"    in raw || "no user record" in raw.lowercase()
                             -> "No account found with this email."
        "INVALID_PASSWORD"   in raw || "password is invalid" in raw.lowercase()
                             -> "Incorrect password. Please try again."
        "EMAIL_EXISTS"       in raw || "already in use" in raw.lowercase()
                             -> "This email is already registered. Try logging in."
        "WEAK_PASSWORD"      in raw || "least 6 characters" in raw.lowercase()
                             -> "Password must be at least 6 characters."
        "INVALID_EMAIL"      in raw || "badly formatted" in raw.lowercase()
                             -> "Please enter a valid email address."
        "network"            in raw.lowercase() || "timeout" in raw.lowercase()
                             -> "Network error. Please check your connection."
        "TOO_MANY_REQUESTS"  in raw -> "Too many attempts. Please wait a moment."
        else -> raw
    }
}
