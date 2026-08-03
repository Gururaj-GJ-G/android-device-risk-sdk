package com.gururajgj.devicerisk

import com.gururajgj.devicerisk.models.RiskBand
import java.util.UUID

/**
 * Result returned by DeviceRiskSdk.assess().
 *
 * @param assessmentId  UUID — log this. It is the key for the monthly Outcome Feedback Template.
 * @param riskScore     0–100. Higher = riskier.
 * @param riskBand      LOW / MEDIUM / HIGH
 * @param reasons       Signal IDs that fired, e.g. ["ROOT_01", "MAL_01"]
 * @param timestamp     Unix epoch ms of assessment
 * @param sdkVersion    SDK version string
 * @param isShadowMode  true if shadowMode was active (real score suppressed)
 */
data class DeviceRiskResult(
    val assessmentId : String,
    val riskScore    : Int,
    val riskBand     : RiskBand,
    val reasons      : List<String>,
    val timestamp    : Long,
    val sdkVersion   : String = BuildConfig.SDK_VERSION,
    val isShadowMode : Boolean = false
) {
    companion object {
        /** Fail-safe LOW result — use when SDK errors and you must not block the user */
        fun safe() = DeviceRiskResult(
            assessmentId = UUID.randomUUID().toString(),
            riskScore    = 0,
            riskBand     = RiskBand.LOW,
            reasons      = emptyList(),
            timestamp    = System.currentTimeMillis(),
            isShadowMode = false
        )
    }
}
