package com.gururajgj.devicerisk.models

/**
 * Band thresholds. Defaults: MEDIUM from 31, HIGH from 61.
 * Clients can lower or raise these to tune sensitivity.
 */
data class RiskThresholds(
    val mediumFrom: Int = 31,
    val highFrom: Int   = 61
)
