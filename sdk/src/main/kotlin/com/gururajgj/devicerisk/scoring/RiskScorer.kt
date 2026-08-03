package com.gururajgj.devicerisk.scoring

import com.gururajgj.devicerisk.DeviceRiskConfig
import com.gururajgj.devicerisk.models.RiskBand
import com.gururajgj.devicerisk.detectors.AppRiskScanner
import com.gururajgj.devicerisk.detectors.EnvironmentDetector

/**
 * Rules-based risk scorer — v1.0-pilot
 *
 * Signal weights match the DeviceRisk_Device_Risk_Scoring_Model_v1.xlsx
 * Signals: ROOT_01, EMU_01, HOOK_01, MAL_01, PERM_01, PERM_02, RC_01, OVER_01, NOTIF_01
 *
 * NOTE: Weights are expert-defined. Recalibrate after 3–6 months of pilot data.
 */
internal object RiskScorer {

    // ── Score weights (mirrors the Excel scoring model) ────────────────────────
    private const val W_ROOT     = 40
    private const val W_EMU      = 50
    private const val W_HOOK     = 20
    private const val W_MAL      = 50   // per app, capped at 60
    private const val W_PERM_01  = 30
    private const val W_PERM_02  = 20
    private const val W_RC       = 25
    private const val W_OVER     = 25
    private const val W_NOTIF    = 15

    data class ScorerInput(
        val rooted              : Boolean,
        val emulator            : Boolean,
        val hookingDetected     : Boolean,
        val appScan             : AppRiskScanner.AppScanResult,
        val environment         : EnvironmentDetector.EnvironmentResult
    )

    data class ScorerOutput(
        val score   : Int,
        val band    : RiskBand,
        val reasons : List<String>
    )

    fun score(input: ScorerInput, config: DeviceRiskConfig): ScorerOutput {
        var total = 0
        val reasons = mutableListOf<String>()

        if (input.rooted) {
            total += W_ROOT
            reasons += "ROOT_01"
        }
        if (input.emulator) {
            total += W_EMU
            reasons += "EMU_01"
        }
        if (input.hookingDetected) {
            total += W_HOOK
            reasons += "HOOK_01"
        }
        if (input.appScan.knownMalwareCount > 0) {
            total += minOf(W_MAL * input.appScan.knownMalwareCount, 60)
            reasons += "MAL_01"
        }
        if (input.appScan.riskyPermissionAppsCount >= 3) {
            total += W_PERM_01
            reasons += "PERM_01"
        }
        if (input.appScan.smsDeviceAdminCount > 0) {
            total += W_PERM_02
            reasons += "PERM_02"
        }
        if (input.appScan.remoteControlAppPresent) {
            total += W_RC
            reasons += "RC_01"
        }
        if (input.environment.overlayAbusesuspected) {
            total += W_OVER
            reasons += "OVER_01"
        }
        if (input.environment.notificationListenerPresent) {
            total += W_NOTIF
            reasons += "NOTIF_01"
        }

        val finalScore = minOf(total, 100)
        val band = when {
            finalScore >= config.thresholds.highFrom   -> RiskBand.HIGH
            finalScore >= config.thresholds.mediumFrom -> RiskBand.MEDIUM
            else                                        -> RiskBand.LOW
        }

        return ScorerOutput(score = finalScore, band = band, reasons = reasons)
    }
}
