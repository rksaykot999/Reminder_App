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

    private val _lastScanTime = MutableStateFlow(prefs.getString(KEY_LAST_SCAN, "Never") ?: "Never")
    val lastScanTime: StateFlow<String> = _lastScanTime.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _cacheSize = MutableStateFlow("0.0 KB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    init {
        updateCacheSize()
    }

    fun updateCacheSize() {
        val size = getCacheSize(getApplication())
        _cacheSize.value = size
    }

    private fun getCacheSize(context: Context): String {
        var size: Long = 0
        size += getDirSize(context.cacheDir)
        size += getDirSize(context.externalCacheDir)
        return formatSize(size)
    }

    private fun getDirSize(dir: java.io.File?): Long {
        var size: Long = 0
        if (dir != null && dir.isDirectory) {
            for (file in dir.listFiles() ?: emptyArray()) {
                size += if (file.isDirectory) getDirSize(file) else file.length()
            }
        } else if (dir != null && dir.isFile) {
            size += dir.length()
        }
        return size
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0.0 KB"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(java.util.Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun clearAppCache() {
        val context = getApplication<Application>()
        try {
            deleteDirContents(context.cacheDir)
            deleteDirContents(context.externalCacheDir)
            updateCacheSize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteDirContents(dir: java.io.File?) {
        if (dir != null && dir.isDirectory) {
            val children = dir.listFiles()
            if (children != null) {
                for (child in children) {
                    if (child.isDirectory) {
                        deleteDirContents(child)
                    }
                    child.delete()
                }
            }
        }
    }

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

    suspend fun performSecurityScan() {
        _isScanning.value = true
        kotlinx.coroutines.delay(2000) // Simulate scan
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val status = "Today at $time"
        prefs.edit().putString(KEY_LAST_SCAN, status).apply()
        _lastScanTime.value = status
        _isScanning.value = false
    }

    companion object {
        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_LOCK_TYPE = "lock_type"
        private const val KEY_IS_APP_LOCKED = "is_app_locked"
        private const val KEY_LOCK_VALUE = "lock_value"
        private const val KEY_LAST_SCAN = "last_scan"
    }
}
