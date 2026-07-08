package com.example.reminderapp.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    _isLoading.value = false
                    if (task.isSuccessful) {
                        _user.value = auth.currentUser
                        onSuccess()
                    } else {
                        _error.value = task.exception?.message ?: "Login failed"
                    }
                }
        }
    }

    fun signInWithGoogle(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val credentialManager = CredentialManager.create(context)
                
                // Get the Web Client ID from your google-services.json or Firebase Console
                // For this example, I'll use a placeholder or try to find it.
                // In a real app, this should be in a config file.
                val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(com.example.reminderapp.R.string.default_web_client_id))
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                if (credential is GoogleIdTokenCredential) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener { task ->
                            _isLoading.value = false
                            if (task.isSuccessful) {
                                _user.value = auth.currentUser
                                onSuccess()
                            } else {
                                _error.value = task.exception?.message ?: "Google Sign-In failed"
                            }
                        }
                } else {
                    _isLoading.value = false
                    _error.value = "Unexpected credential type"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.localizedMessage ?: "Google Sign-In cancelled"
            }
        }
    }

    fun signUp(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    _isLoading.value = false
                    if (task.isSuccessful) {
                        _user.value = auth.currentUser
                        onSuccess()
                    } else {
                        _error.value = task.exception?.message ?: "Signup failed"
                    }
                }
        }
    }

    fun signOut(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                auth.signOut()
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                _user.value = null
                onSuccess()
            } catch (_: Exception) {
                _user.value = null
                onSuccess()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
