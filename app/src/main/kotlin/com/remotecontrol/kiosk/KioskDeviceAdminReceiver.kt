package com.remotecontrol.kiosk

import android.app.admin.DeviceAdminReceiver

/**
 * Marker receiver Android requires for device-admin/Device Owner registration.
 * Provisioning targets this class; actual lockdown policy is applied in [KioskAdmin].
 */
class KioskDeviceAdminReceiver : DeviceAdminReceiver()
