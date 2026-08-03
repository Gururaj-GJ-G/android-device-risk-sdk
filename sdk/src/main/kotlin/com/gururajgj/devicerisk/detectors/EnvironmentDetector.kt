package com.gururajgj.devicerisk.detectors

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Detects environment-level risks:
 * - Overlay + Accessibility abuse → OVER_01 +25
 * - Notification listener present → NOTIF_01 +15
 */
internal object EnvironmentDetector {

    data class EnvironmentResult(
        val overlayAbusesuspected    : Boolean,
        val notificationListenerPresent : Boolean
    )

    fun scan(context: Context): EnvironmentResult {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

        var overlaySuspected = false
        var notifListener = false

        for (pkg in packages) {
            val name = pkg.packageName
            // Skip system packages
            if (pkg.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) continue

            val perms = pkg.requestedPermissions?.toSet() ?: emptySet()

            // Overlay + Accessibility = potential keylogger / OTP intercept
            val hasOverlay      = perms.contains("android.permission.SYSTEM_ALERT_WINDOW")
            val hasAccessibility = perms.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")
            if (hasOverlay && hasAccessibility) overlaySuspected = true

            // Notification listener = can read OTP notifications silently
            val hasNotifListener = perms.contains("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")
            if (hasNotifListener) notifListener = true
        }

        // Also check active notification listener services via Settings
        if (!notifListener) {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            if (!enabledListeners.isNullOrEmpty()) notifListener = true
        }

        return EnvironmentResult(
            overlayAbusesuspected     = overlaySuspected,
            notificationListenerPresent = notifListener
        )
    }
}
