package com.gururajgj.devicerisk.models

/**
 * Risk band returned by the Device Risk SDK.
 * LOW  : 0–30  — proceed normally
 * MEDIUM: 31–60 — step-up verification recommended
 * HIGH : 61–100 — hold for manual review or decline
 */
enum class RiskBand {
    LOW,
    MEDIUM,
    HIGH
}
