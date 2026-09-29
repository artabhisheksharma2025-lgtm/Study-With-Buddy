package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminEntity
import com.example.data.model.UserEntity
import com.example.data.repository.AdminRepository
import com.example.data.repository.AppRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object SignedOut : AuthUiState
    data class Authenticated(val user: UserEntity) : AuthUiState
}

class AuthViewModel(
    private val repository: AppRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>("Restoring study data from cloud...")
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    private val _showGoogleAccountPicker = MutableStateFlow(false)
    val showGoogleAccountPicker: StateFlow<Boolean> = _showGoogleAccountPicker.asStateFlow()

    fun dismissGoogleAccountPicker() {
        _showGoogleAccountPicker.value = false
    }

    fun openGoogleAccountPicker() {
        _showGoogleAccountPicker.value = true
    }

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            adminRepository.seedAdminDataIfNeeded()
            repository.loggedInUserFlow.collect { user ->
                if (user != null) {
                    _authState.value = AuthUiState.Authenticated(user)
                } else {
                    _authState.value = AuthUiState.SignedOut
                }
            }
        }
    }

    fun signUp(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (fullName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill in all fields."
            return
        }
        if (password != confirmPassword) {
            _errorMessage.value = "Passwords do not match."
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return
        }

        viewModelScope.launch {
            _isAuthenticating.value = true
            _syncStatusMessage.value = "Creating account & initializing cloud sync..."
            val result = repository.registerUser(fullName, username, email, password)
            _isAuthenticating.value = false
            result.onSuccess { user ->
                _successMessage.value = "Account created successfully! Your Study ID: ${user.studyId}"
                _errorMessage.value = null
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Sign up failed."
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onAdminVerified: (AdminEntity) -> Unit = {}
    ) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please enter your email and password."
            return
        }

        viewModelScope.launch {
            val cleanEmail = email.trim()

            // Check if this is an administrator account trying to log in
            if (adminRepository.isAdminEmail(cleanEmail)) {
                _isAuthenticating.value = true
                _syncStatusMessage.value = "Verifying administrator credentials..."
                val adminResult = adminRepository.authenticateAdmin(cleanEmail, password)
                _isAuthenticating.value = false
                adminResult.onSuccess { admin ->
                    _errorMessage.value = null
                    onAdminVerified(admin)
                }.onFailure { ex ->
                    _errorMessage.value = ex.message ?: "Administrator authentication failed."
                }
                return@launch
            }

            // Normal student login: fetches user from cloud, sets same UID/studyId, restores all sessions/goals/streak
            _isAuthenticating.value = true
            _syncStatusMessage.value = "Restoring your study sessions, streak & goals from cloud..."
            val result = repository.loginUser(cleanEmail, password)
            _isAuthenticating.value = false
            result.onSuccess {
                _errorMessage.value = null
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Login failed."
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _errorMessage.value = "Please enter your registered email address."
            return
        }
        _successMessage.value = "Password reset instructions have been sent to $email."
        _errorMessage.value = null
    }

    fun loginWithGoogle(
        context: Context,
        selectedEmail: String? = null,
        selectedName: String? = null
    ) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _syncStatusMessage.value = "Connecting with Google Account..."

            var finalEmail: String? = selectedEmail
            var finalName: String? = selectedName
            var idToken: String? = null

            // If an explicit email was NOT provided, attempt Credential Manager
            if (finalEmail.isNullOrBlank()) {
                try {
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId("633534015482-study-with-buddy.apps.googleusercontent.com")
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(context = context, request = request)
                    val credential = result.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        finalEmail = googleIdTokenCredential.id
                        finalName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.givenName
                        idToken = googleIdTokenCredential.idToken
                    }
                } catch (e: Exception) {
                    Log.d("AuthViewModel", "CredentialManager prompt: ${e.message}")
                }
            }

            // If Credential Manager didn't return an email (common on emulators/devices without Play Services logged in),
            // show the Google Account selector dialog
            if (finalEmail.isNullOrBlank()) {
                _isAuthenticating.value = false
                _showGoogleAccountPicker.value = true
                return@launch
            }

            _showGoogleAccountPicker.value = false
            _syncStatusMessage.value = "Restoring your study sessions, streak & goals for $finalEmail..."

            try {
                val result = repository.signInWithGoogleAccount(
                    email = finalEmail,
                    displayName = finalName ?: finalEmail.substringBefore("@"),
                    idToken = idToken
                )

                _isAuthenticating.value = false
                result.onSuccess {
                    _errorMessage.value = null
                    _successMessage.value = "Welcome! Signed in with Google as $finalEmail"
                }.onFailure { ex ->
                    _errorMessage.value = ex.message ?: "Google Sign-In failed."
                }
            } catch (e: Exception) {
                _isAuthenticating.value = false
                _errorMessage.value = e.message ?: "Google Sign-In failed."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logoutUser()
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
