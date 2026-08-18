package com.remotecontrol.kiosk

import android.app.ActivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.remotecontrol.kiosk.ui.KioskApp

class MainActivity : ComponentActivity() {

    private val prefs by lazy { KioskPrefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Skip while in maintenance mode: re-claiming the Home role here would
        // undo exitKiosk() if the process was killed and relaunched mid-maintenance.
        if (!prefs.isMaintenanceMode()) {
            KioskAdmin.applyPolicies(this)
        }

        setContent {
            MaterialTheme {
                KioskApp(
                    prefs = prefs,
                    onExitKiosk = ::exitKiosk,
                    onResumeKiosk = ::resumeKiosk,
                    onSettingsSaved = { KioskAdmin.applyPolicies(this) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
        startLockTaskIfNeeded()
    }

    private fun startLockTaskIfNeeded() {
        if (!KioskAdmin.isDeviceOwner(this)) return
        if (prefs.isMaintenanceMode()) return
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
            startLockTask()
        }
    }

    // Deliberately does NOT finish() the activity: this app is registered as
    // the device's persistent-preferred Home app, so finishing would just
    // make Android relaunch it — which onResume would then immediately
    // re-lock. Instead we drop the Home claim, stop lock task, and let the
    // Compose layer show a maintenance screen until resumeKiosk is called.
    private fun exitKiosk() {
        prefs.setMaintenanceMode(true)
        KioskAdmin.releaseHomeRole(this)
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            stopLockTask()
        }
    }

    private fun resumeKiosk() {
        prefs.setMaintenanceMode(false)
        KioskAdmin.applyPolicies(this)
        startLockTaskIfNeeded()
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
