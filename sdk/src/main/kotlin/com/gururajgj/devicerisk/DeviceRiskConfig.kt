package com.gururajgj.devicerisk

import com.gururajgj.devicerisk.models.LogLevel
import com.gururajgj.devicerisk.models.RiskThresholds
import com.gururajgj.devicerisk.models.SdkMode

/**
 * Configuration for the Device Risk SDK.
 *
 * @param mode        Scoring mode: HYBRID (default), LOCAL_ONLY, or API_ONLY
 * @param timeoutMs   API call timeout in milliseconds (default 5000)
 * @param logLevel    Logging verbosity: NONE (production), WARN, DEBUG
 * @param shadowMode  true = pilot mode: score is logged but always returns LOW to your app
 *                    false = live mode: real score returned to your decision logic
 * @param thresholds  Band thresholds (optional, default LOW<31, MEDIUM<61, HIGH>=61)
 * @param apiBaseUrl  Override API base URL (null = use your configured backend)
 */
data class DeviceRiskConfig(
    val mode       : SdkMode       = SdkMode.HYBRID,
    val timeoutMs  : Long          = 5_000L,
    val logLevel   : LogLevel      = LogLevel.WARN,
    val shadowMode : Boolean       = true,
    val thresholds : RiskThresholds = RiskThresholds(),
    val apiBaseUrl : String?       = null
)
