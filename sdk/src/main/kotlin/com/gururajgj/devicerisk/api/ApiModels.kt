package com.gururajgj.devicerisk.api

/** Feature vector sent to risk-scoring API */
internal data class DeviceRiskRequest(
    val clientId                : String,
    val sessionId               : String,
    val sdkVersion              : String,
    val rooted                  : Boolean,
    val emulator                : Boolean,
    val hookingDetected         : Boolean,
    val knownMalwareCount       : Int,
    val riskyPermissionAppsCount: Int,
    val smsDeviceAdminCount     : Int,
    val remoteControlAppPresent : Boolean,
    val overlayAbuseSuspected   : Boolean,
    val notificationListenerPresent: Boolean,
    val timestamp               : Long
)

/** Response from risk-scoring API */
internal data class DeviceRiskResponse(
    val assessmentId : String,
    val riskScore    : Int,
    val riskBand     : String,   // "LOW" | "MEDIUM" | "HIGH"
    val reasons      : List<String>
)
