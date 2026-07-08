package com.example.reminderapp.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide settings shared across every tab, most importantly dark/light mode and lock settings.
 * Backed by SharedPreferences so the choice survives app restarts.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _lockType = MutableStateFlow(prefs.getString(KEY_LOCK_TYPE, "NONE") ?: "NONE")
    val lockType: StateFlow<String> = _lockType.asStateFlow()

    private val _isAppLocked = MutableStateFlow(prefs.getBoolean(KEY_IS_APP_LOCKED, false))
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    fun setLockType(type: String) {
        prefs.edit().putString(KEY_LOCK_TYPE, type).apply()
        _lockType.value = type
        if (type == "NONE") {
            setAppLocked(false)
        }
    }

    fun setAppLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_IS_APP_LOCKED, locked).apply()
        _isAppLocked.value = locked
    }

    fun saveLockValue(value: String) {
        prefs.edit().putString(KEY_LOCK_VALUE, value).apply()
    }

    fun getLockValue(): String {
        return prefs.getString(KEY_LOCK_VALUE, "") ?: ""
    }

    companion object {
        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_LOCK_TYPE = "lock_type"
        private const val KEY_IS_APP_LOCKED = "is_app_locked"
        private const val KEY_LOCK_VALUE = "lock_value"
    }
}
