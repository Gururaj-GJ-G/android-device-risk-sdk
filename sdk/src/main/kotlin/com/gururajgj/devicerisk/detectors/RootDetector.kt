package com.gururajgj.devicerisk.detectors

import android.os.Build
import java.io.File

/**
 * Detects root indicators on the device.
 * Signal: ROOT_01 — score impact +40
 *
 * Checks:
 * - su binary presence in known paths
 * - Known root management apps (Magisk, SuperSU, KingRoot, etc.)
 * - Writable system directories
 * - Abnormal build tags
 */
internal object RootDetector {

    private val SU_PATHS = listOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su",
        "/system/su", "/system/bin/.ext/.su", "/system/usr/we-need-root/su-backup",
        "/system/xbin/mu"
    )

    private val ROOT_PACKAGES = listOf(
        "com.topjohnwu.magisk",          // Magisk
        "eu.chainfire.supersu",           // SuperSU
        "com.noshufou.android.su",        // Superuser
        "com.noshufou.android.su.elite",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.kingroot.kinguser",          // KingRoot
        "com.kingo.root",                 // KingRoot variant
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        "com.alephzain.framaroot"
    )

    private val DANGEROUS_PROPS = mapOf(
        "ro.build.tags" to listOf("test-keys"),
        "ro.debuggable"  to listOf("1"),
        "ro.secure"      to listOf("0")
    )

    fun isRooted(): Boolean {
        return checkSuBinaries()
            || checkBuildTags()
            || checkSystemWritable()
    }

    fun rootedPackagesFound(installedPackages: List<String>): Boolean {
        return installedPackages.any { it in ROOT_PACKAGES }
    }

    private fun checkSuBinaries(): Boolean {
        return SU_PATHS.any { File(it).exists() }
    }

    private fun checkBuildTags(): Boolean {
        val tags = Build.TAGS ?: return false
        return tags.contains("test-keys")
    }

    private fun checkSystemWritable(): Boolean {
        return try {
            val file = File("/system")
            file.canWrite()
        } catch (e: Exception) {
            false
        }
    }
}
