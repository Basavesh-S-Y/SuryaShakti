package com.suryashakti.solarmonitor.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.suryashakti.solarmonitor.data.AuthState
import com.suryashakti.solarmonitor.data.UserProfile
import com.suryashakti.solarmonitor.util.AuthManager
import com.suryashakti.solarmonitor.util.CloudSyncManager
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        // On ViewModel creation, determine current auth status
        _authState.value = if (AuthManager.isLoggedIn)
            AuthState.LoggedIn(AuthManager.currentUser!!)
        else
            AuthState.LoggedOut
    }

    // ── Email / Password ──────────────────────────────────────────────────

    fun loginWithEmail(email: String, password: String) {
        if (!validateEmail(email)) return
        if (password.isBlank()) { _authState.value = AuthState.Error("Enter your password"); return }

        _authState.value = AuthState.LoggingIn
        viewModelScope.launch {
            AuthManager.loginWithEmail(email, password).fold(
                onSuccess = { user ->
                    saveProfileToCloud(user)
                    _authState.value = AuthState.LoggedIn(user)
                },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Login failed") }
            )
        }
    }

    fun registerWithEmail(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank())  { _authState.value = AuthState.Error("Enter your name"); return }
        if (!validateEmail(email)) return
        if (password.length < 6) { _authState.value = AuthState.Error("Password must be at least 6 characters"); return }
        if (password != confirmPassword) { _authState.value = AuthState.Error("Passwords do not match"); return }

        _authState.value = AuthState.Registering
        viewModelScope.launch {
            AuthManager.registerWithEmail(name, email, password).fold(
                onSuccess = { user ->
                    // Persist name locally so Settings reads it instantly
                    com.suryashakti.solarmonitor.util.PreferenceManager(getApplication())
                        .setUserName(user.displayName.ifBlank { name })
                    saveProfileToCloud(user)
                    _authState.value = AuthState.LoggedIn(user)
                },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        if (!validateEmail(email)) return
        _authState.value = AuthState.SendingReset
        viewModelScope.launch {
            AuthManager.sendPasswordReset(email).fold(
                onSuccess  = { _authState.value = AuthState.ResetSent },
                onFailure  = { _authState.value = AuthState.Error(it.message ?: "Reset failed") }
            )
        }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────

    fun handleGoogleSignInResult(data: android.content.Intent?) {
        _authState.value = AuthState.LoggingIn
        viewModelScope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                AuthManager.handleGoogleSignInResult(account).fold(
                    onSuccess = { user ->
                        saveProfileToCloud(user)
                        _authState.value = AuthState.LoggedIn(user)
                    },
                    onFailure = { _authState.value = AuthState.Error(it.message ?: "Google sign-in failed") }
                )
            } catch (e: ApiException) {
                _authState.value = if (e.statusCode == 12501)
                    AuthState.LoggedOut   // user cancelled — just go back quietly
                else
                    AuthState.Error("Google sign-in failed (code ${e.statusCode})")
            }
        }
    }

    // ── Sign Out ──────────────────────────────────────────────────────────

    fun signOut(context: Context) {
        AuthManager.signOut(context)
        _authState.value = AuthState.LoggedOut
    }

    fun resetState() {
        _authState.value = if (AuthManager.isLoggedIn)
            AuthState.LoggedIn(AuthManager.currentUser!!)
        else
            AuthState.LoggedOut
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun validateEmail(email: String): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Enter a valid email address")
            return false
        }
        return true
    }

    private suspend fun saveProfileToCloud(user: UserProfile) {
        runCatching {
            CloudSyncManager.saveUserProfile(user.uid, user.displayName, user.email)
        }
    }
}

class AuthViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(app) as T
}
