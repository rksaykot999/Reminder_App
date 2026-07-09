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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isGuest.value = false
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
                _isGuest.value = false
                
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
            _isGuest.value = false
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
                _isGuest.value = false
                onSuccess()
            } catch (_: Exception) {
                _user.value = null
                _isGuest.value = false
                onSuccess()
            }
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        _isGuest.value = true
        _user.value = null
        onSuccess()
    }

    fun clearError() {
        _error.value = null
    }

    fun updateProfile(displayName: String, photoUri: String?, onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .apply {
                    if (photoUri != null) {
                        setPhotoUri(android.net.Uri.parse(photoUri))
                    }
                }
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _user.value = auth.currentUser
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
        }
    }

    fun changePassword(currentPass: String, newPass: String, onComplete: (Boolean, String) -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, currentPass)
            user.reauthenticate(credential)
                .addOnCompleteListener { reAuthTask ->
                    if (reAuthTask.isSuccessful) {
                        user.updatePassword(newPass)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    onComplete(true, "Password updated successfully")
                                } else {
                                    onComplete(false, updateTask.exception?.message ?: "Update failed")
                                }
                            }
                    } else {
                        onComplete(false, "Re-authentication failed: Check current password")
                    }
                }
        } else {
            onComplete(false, "User not authenticated")
        }
    }
}
