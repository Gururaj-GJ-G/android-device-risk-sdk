package com.gururajgj.devicerisk.detectors

import java.io.BufferedReader
import java.io.FileReader

/**
 * Detects hooking / debugging frameworks (Frida, Xposed).
 * Signal: HOOK_01 — score impact +20
 *
 * These tools are used to intercept, tamper, or reverse-engineer apps at runtime.
 */
internal object HookingDetector {

    private val FRIDA_INDICATORS = listOf(
        "frida", "gadget", "libfrida", "frida-agent",
        "re.frida.server", "com.fuzion24.frida"
    )

    private val XPOSED_INDICATORS = listOf(
        "XposedBridge", "de.robv.android.xposed",
        "xposed", "EdXposed", "LSPosed"
    )

    private val XPOSED_PACKAGES = listOf(
        "de.robv.android.xposed.installer",
        "com.solohsu.android.edxp.manager",
        "org.lsposed.manager"
    )

    fun isHookingFrameworkPresent(installedPackages: List<String>): Boolean {
        return checkXposedPackages(installedPackages)
            || checkMapsForFrida()
            || checkDebuggerAttached()
    }

    private fun checkXposedPackages(installedPackages: List<String>): Boolean {
        return installedPackages.any { pkg ->
            XPOSED_PACKAGES.any { xp -> pkg.contains(xp, ignoreCase = true) }
        }
    }

    private fun checkMapsForFrida(): Boolean {
        return try {
            val reader = BufferedReader(FileReader("/proc/self/maps"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.lowercase() ?: continue
                if (FRIDA_INDICATORS.any { l.contains(it) }) {
                    reader.close()
                    return true
                }
            }
            reader.close()
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun checkDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected()
    }
}
