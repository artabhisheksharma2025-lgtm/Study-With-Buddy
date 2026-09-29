package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminEntity
import com.example.data.model.UserEntity
import com.example.data.repository.AdminRepository
import com.example.data.repository.AppRepository
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
