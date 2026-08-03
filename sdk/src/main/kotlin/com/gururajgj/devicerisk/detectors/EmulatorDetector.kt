package com.gururajgj.devicerisk.detectors

import android.content.Context
import android.os.Build

/**
 * Detects if the device is running in an emulator.
 * Signal: EMU_01 — score impact +50
 *
 * Emulators are commonly used for large-scale fraud and bot activity.
 */
internal object EmulatorDetector {

    private val EMULATOR_FINGERPRINTS = listOf(
        "generic", "unknown", "google_sdk", "emulator",
        "Android SDK built for x86", "Genymotion"
    )

    private val EMULATOR_MANUFACTURERS = listOf(
        "unknown", "Genymotion", "Andy", "nox", "amazon"
    )

    private val EMULATOR_MODELS = listOf(
        "sdk", "google_sdk", "sdk_gphone", "sdk_gphone64",
        "Emulator", "Android SDK built for x86",
        "Android SDK built for x86_64"
    )

    private val EMULATOR_HARDWARE = listOf(
        "goldfish", "ranchu", "vbox86", "nox", "ttVM_x86"
    )

    private val EMULATOR_PRODUCTS = listOf(
        "sdk", "sdk_gphone_x86", "sdk_gphone64_arm64",
        "vbox86p", "emulator_x86", "nox", "Andy"
    )

    fun isEmulator(context: Context): Boolean {
        return checkBuildProperties()
            || checkTelephony(context)
    }

    private fun checkBuildProperties(): Boolean {
        val fingerprint = Build.FINGERPRINT?.lowercase() ?: ""
        val model       = Build.MODEL?.lowercase() ?: ""
        val manufacturer= Build.MANUFACTURER?.lowercase() ?: ""
        val hardware    = Build.HARDWARE?.lowercase() ?: ""
        val product     = Build.PRODUCT?.lowercase() ?: ""
        val brand       = Build.BRAND?.lowercase() ?: ""

        if (EMULATOR_FINGERPRINTS.any { fingerprint.contains(it.lowercase()) }) return true
        if (EMULATOR_MODELS.any { model.contains(it.lowercase()) }) return true
        if (EMULATOR_MANUFACTURERS.any { manufacturer.contains(it.lowercase()) }) return true
        if (EMULATOR_HARDWARE.any { hardware.contains(it.lowercase()) }) return true
        if (EMULATOR_PRODUCTS.any { product.contains(it.lowercase()) }) return true
        if (brand == "google" && manufacturer == "google"
            && fingerprint.startsWith("google/sdk_gphone")) return true

        return false
    }

    private fun checkTelephony(context: Context): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE)
                as? android.telephony.TelephonyManager ?: return false
            val networkOp = tm.networkOperatorName?.lowercase() ?: ""
            networkOp == "android" // emulator default operator
        } catch (e: Exception) {
            false
        }
    }
}
