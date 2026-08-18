package com.remotecontrol.kiosk

import android.content.Context
import androidx.core.content.edit

class KioskPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUrl(): String? = prefs.getString(KEY_URL, null)

    fun getPin(): String = prefs.getString(KEY_PIN, null) ?: DEFAULT_PIN

    fun save(url: String, pin: String) {
        prefs.edit {
            putString(KEY_URL, url)
            putString(KEY_PIN, pin)
        }
    }

    // While true, the device is deliberately unlocked for maintenance —
    // MainActivity must not re-enter lock task mode on its own until this is
    // cleared by resuming the kiosk. See MainActivity.exitKiosk/resumeKiosk.
    fun isMaintenanceMode(): Boolean = prefs.getBoolean(KEY_MAINTENANCE, false)

    fun setMaintenanceMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_MAINTENANCE, enabled) }
    }

    companion object {
        private const val PREFS_NAME = "kiosk_prefs"
        private const val KEY_URL = "url"
        private const val KEY_PIN = "pin"
        private const val KEY_MAINTENANCE = "maintenance_mode"
        private const val DEFAULT_PIN = "1234"
    }
}
