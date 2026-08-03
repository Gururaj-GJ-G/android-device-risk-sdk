package com.gururajgj.devicerisk.detectors

import android.content.Context
import android.content.pm.PackageManager

/**
 * Scans installed apps for:
 * 1. Known malicious loan / SpyLoan apps (blacklist match)  → MAL_01 +50
 * 2. Apps with loan-style excessive permission combinations  → PERM_01 +30
 * 3. Apps with SMS + DeviceAdmin combo (OTP theft + persist) → PERM_02 +20
 *
 * PRIVACY NOTE: Package names and permission flags only.
 * Message content, contact entries, or files are never read.
 */
internal object AppRiskScanner {

    // ── Known malicious loan app package IDs ──────────────────────────────────
    // Sources: RBI advisories, CERT-In alerts, security vendor reports on SpyLoan campaigns
    private val MALICIOUS_LOAN_PACKAGES = setOf(
        // SpyLoan campaign families (India-focused)
        "com.loanquick.app", "com.instantloan.cash", "com.rupee.loan.easy",
        "com.loan.cashin.fast", "com.money.wallet.loan", "com.easy.rupee.cash",
        "com.quick.cash.loan.india", "com.fastloan.personal", "com.creditloan.rupee",
        "com.insta.loan.rupees", "com.cash.advance.loan", "com.payday.loan.cash",
        "com.loanapp.quickrupee", "com.money.lend.fast", "com.rupee.fast.cash",
        "com.loan.shark.india", "com.micro.loan.india", "com.pocket.loan.rupee",
        "com.flash.cash.loan", "com.speed.loan.cash",
        // Known harassment / extortion-linked packages reported in cybercrime complaints
        "com.kreditbee.unofficial", "com.cashe.unofficial", "com.navi.unofficial",
        "com.stashfin.unofficial", "com.prefr.unofficial",
        // Remote-control / screen-sharing variants used in UPI scams
        "com.anydesk.unofficial", "com.teamviewer.host.unofficial",
        "com.alfacast.screen.mirror.unofficial"
    )

    // ── Remote control / screen sharing apps ──────────────────────────────────
    private val REMOTE_CONTROL_PACKAGES = setOf(
        "com.anydesk.anydeskandroid",
        "com.teamviewer.host",
        "com.teamviewer.teamviewerforremotecontrol",
        "com.alfacast.screen.mirror",
        "com.rsupport.rs.activity.nw",   // RemoteView
        "com.xii.remotedroid",
        "com.mirroring.remote.screen",
        "com.sure.screencast"
    )

    // ── Risky permission combinations (loan-app pattern) ──────────────────────
    // Legitimate lenders do NOT need contacts + SMS + storage + call logs
    private val LOAN_STYLE_DANGEROUS_PERMS = setOf(
        "android.permission.READ_CONTACTS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE"
    )

    private val OTP_THEFT_PERMS = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.BIND_DEVICE_ADMIN"
    )

    data class AppScanResult(
        val installedPackages        : List<String>,
        val knownMalwareCount        : Int,
        val remoteControlAppPresent  : Boolean,
        val riskyPermissionAppsCount : Int,  // 3+ loan-style perms
        val smsDeviceAdminCount      : Int   // SMS + DeviceAdmin combo
    )

    fun scan(context: Context): AppScanResult {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val packageNames = packages.map { it.packageName }

        var knownMalware = 0
        var remoteControl = false
        var riskyPermApps = 0
        var smsAdminApps = 0

        for (pkg in packages) {
            val name = pkg.packageName
            val declaredPerms = pkg.requestedPermissions?.toSet() ?: emptySet()

            // 1. Blacklist match
            if (name in MALICIOUS_LOAN_PACKAGES) knownMalware++

            // 2. Remote control
            if (name in REMOTE_CONTROL_PACKAGES) remoteControl = true

            // 3. Loan-style excessive permissions
            val riskyCount = declaredPerms.count { it in LOAN_STYLE_DANGEROUS_PERMS }
            if (riskyCount >= 3) riskyPermApps++

            // 4. SMS + DeviceAdmin (OTP theft + persistence)
            val hasSms   = declaredPerms.any { it == "android.permission.READ_SMS" || it == "android.permission.RECEIVE_SMS" }
            val hasAdmin = declaredPerms.contains("android.permission.BIND_DEVICE_ADMIN")
            if (hasSms && hasAdmin) smsAdminApps++
        }

        return AppScanResult(
            installedPackages        = packageNames,
            knownMalwareCount        = knownMalware,
            remoteControlAppPresent  = remoteControl,
            riskyPermissionAppsCount = riskyPermApps,
            smsDeviceAdminCount      = smsAdminApps
        )
    }
}
