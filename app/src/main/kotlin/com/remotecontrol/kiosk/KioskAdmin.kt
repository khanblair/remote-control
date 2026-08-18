package com.remotecontrol.kiosk

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.UserManager

/**
 * Applies the device-owner lockdown policy: pin this app as the only lock-task
 * package, make it the device's home app so Android returns to it after any
 * crash, and block the escape hatches (factory reset, safe boot, extra users)
 * that don't require physical recovery-mode access.
 *
 * No-ops when the app isn't provisioned as device owner, so the app still runs
 * (unlocked) during local development on a regular, unprovisioned device.
 */
object KioskAdmin {

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.devicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun applyPolicies(context: Context) {
        if (!isDeviceOwner(context)) return

        val dpm = context.devicePolicyManager
        val admin = context.adminComponent

        dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
        }

        dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)

        val homeIntentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(
            admin,
            homeIntentFilter,
            ComponentName(context, MainActivity::class.java),
        )
    }

    /**
     * Releases this app's exclusive claim on the Home role so leaving lock
     * task mode actually leaves — without this, finishing/backgrounding the
     * activity while it's still the persistent-preferred Home app just makes
     * Android relaunch it immediately, undoing the exit. Call [applyPolicies]
     * to restore lockdown when maintenance is done.
     */
    fun releaseHomeRole(context: Context) {
        if (!isDeviceOwner(context)) return
        context.devicePolicyManager.clearPackagePersistentPreferredActivities(
            context.adminComponent,
            context.packageName,
        )
        // Belt-and-suspenders: on-device testing showed a plain (non-persistent)
        // preferred-activity entry for MAIN/HOME surviving the call above, which
        // alone was enough to make Android route Home back to this app. This
        // clears that separate list too.
        context.packageManager.clearPackagePreferredActivities(context.packageName)
    }

    private val Context.devicePolicyManager: DevicePolicyManager
        get() = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val Context.adminComponent: ComponentName
        get() = ComponentName(this, KioskDeviceAdminReceiver::class.java)
}
