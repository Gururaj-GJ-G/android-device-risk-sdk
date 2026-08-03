package com.gururajgj.devicerisk

import android.content.Context
import com.gururajgj.devicerisk.api.DeviceRiskRequest
import com.gururajgj.devicerisk.api.DeviceRiskApiClient
import com.gururajgj.devicerisk.detectors.AppRiskScanner
import com.gururajgj.devicerisk.detectors.EmulatorDetector
import com.gururajgj.devicerisk.detectors.EnvironmentDetector
import com.gururajgj.devicerisk.detectors.HookingDetector
import com.gururajgj.devicerisk.detectors.RootDetector
import com.gururajgj.devicerisk.exceptions.DeviceRiskInitException
import com.gururajgj.devicerisk.exceptions.DeviceRiskNetworkException
import com.gururajgj.devicerisk.models.RiskBand
import com.gururajgj.devicerisk.models.SdkMode
import com.gururajgj.devicerisk.scoring.RiskScorer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    DEVICE RISK SDK — v1.0                    ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Main entry point.
 *
 * USAGE:
 *
 * 1. In Application.onCreate():
 *    DeviceRiskSdk.init(this, "your_api_key", DeviceRiskConfig(shadowMode = true))
 *
 * 2. At KYC / disbursal:
 *    val result = DeviceRiskSdk.assess(context, userId)
 *    when (result.riskBand) {
 *        RiskBand.LOW    -> proceedNormally()
 *        RiskBand.MEDIUM -> stepUpVerification()
 *        RiskBand.HIGH   -> holdForReview()
 *    }
 *
 * 3. Always log result.assessmentId — useful for tying an assessment back to
 *    the outcome (fraud confirmed / false positive) in your own review process.
 */
object DeviceRiskSdk {

    private var apiKey    : String?        = null
    private var config    : DeviceRiskConfig? = null
    private var clientId  : String?        = null
    private var apiClient : DeviceRiskApiClient? = null

    /**
     * Initialise the SDK. Call once in your Application.onCreate().
     *
     * @param context  Application context
     * @param apiKey   Your API key, issued by whichever backend you configure via DeviceRiskConfig.apiBaseUrl
     * @param config   SDK configuration (see DeviceRiskConfig)
     */
    fun init(context: Context, apiKey: String, config: DeviceRiskConfig = DeviceRiskConfig()) {
        this.apiKey   = apiKey
        this.config   = config
        this.clientId = getOrCreateClientId(context)
        this.apiClient = DeviceRiskApiClient(
            apiKey    = apiKey,
            baseUrl   = config.apiBaseUrl ?: DeviceRiskApiClient.DEFAULT_BASE_URL,
            timeoutMs = config.timeoutMs
        )
        log("DeviceRiskSdk initialised. Mode=${config.mode} Shadow=${config.shadowMode}")
    }

    /**
     * Run a device risk assessment. Call at KYC start, loan disbursal, or high-risk actions.
     *
     * This is a suspend function — call from a coroutine scope (e.g. viewModelScope.launch).
     *
     * @param context    Activity or Application context
     * @param sessionId  Your pseudonymous user/session ID (NOT the user's name or phone number)
     * @return           DeviceRiskResult with riskBand, riskScore, reasons, and assessmentId
     */
    suspend fun assess(context: Context, sessionId: String): DeviceRiskResult {
        checkInitialised()
        val cfg = config!!

        return withContext(Dispatchers.IO) {
            // ── Step 1: On-device detection ──────────────────────────────────
            val appScan     = AppRiskScanner.scan(context)
            val rooted      = RootDetector.isRooted()
                           || RootDetector.rootedPackagesFound(appScan.installedPackages)
            val emulator    = EmulatorDetector.isEmulator(context)
            val hooking     = HookingDetector.isHookingFrameworkPresent(appScan.installedPackages)
            val environment = EnvironmentDetector.scan(context)

            log("Detection: rooted=$rooted emu=$emulator hook=$hooking" +
                " malware=${appScan.knownMalwareCount} riskyApps=${appScan.riskyPermissionAppsCount}")

            // ── Step 2: Score locally ─────────────────────────────────────────
            val localScore = RiskScorer.score(
                input = RiskScorer.ScorerInput(
                    rooted          = rooted,
                    emulator        = emulator,
                    hookingDetected = hooking,
                    appScan         = appScan,
                    environment     = environment
                ),
                config = cfg
            )

            // ── Step 3: API call (if HYBRID mode) ────────────────────────────
            val finalScore = when (cfg.mode) {
                SdkMode.LOCAL_ONLY -> localScore
                SdkMode.HYBRID, SdkMode.API_ONLY -> {
                    try {
                        val request = DeviceRiskRequest(
                            clientId                 = clientId!!,
                            sessionId                = sessionId,
                            sdkVersion               = BuildConfig.SDK_VERSION,
                            rooted                   = rooted,
                            emulator                 = emulator,
                            hookingDetected          = hooking,
                            knownMalwareCount        = appScan.knownMalwareCount,
                            riskyPermissionAppsCount = appScan.riskyPermissionAppsCount,
                            smsDeviceAdminCount      = appScan.smsDeviceAdminCount,
                            remoteControlAppPresent  = appScan.remoteControlAppPresent,
                            overlayAbuseSuspected    = environment.overlayAbusesuspected,
                            notificationListenerPresent = environment.notificationListenerPresent,
                            timestamp                = System.currentTimeMillis()
                        )
                        val response = apiClient!!.assess(request)
                        // Trust API score over local score (API has richer model)
                        RiskScorer.ScorerOutput(
                            score   = response.riskScore,
                            band    = RiskBand.valueOf(response.riskBand),
                            reasons = response.reasons
                        ).also { log("API score=${it.score} band=${it.band}") }
                    } catch (e: DeviceRiskNetworkException) {
                        // Fail-safe: use local score if API unreachable
                        log("API unreachable — using local score. ${e.message}")
                        localScore
                    }
                }
            }

            // ── Step 4: Apply shadow mode ─────────────────────────────────────
            val effectiveBand = if (cfg.shadowMode) RiskBand.LOW else finalScore.band
            val assessmentId  = UUID.randomUUID().toString()

            log("Assessment done: id=$assessmentId score=${finalScore.score} band=${finalScore.band} shadow=${cfg.shadowMode}")

            DeviceRiskResult(
                assessmentId = assessmentId,
                riskScore    = finalScore.score,
                riskBand     = effectiveBand,
                reasons      = finalScore.reasons,
                timestamp    = System.currentTimeMillis(),
                isShadowMode = cfg.shadowMode
            )
        }
    }

    /**
     * Assess using on-device detection only — no API call.
     * Use as fallback when network is unavailable.
     */
    suspend fun assessLocalOnly(context: Context, sessionId: String): DeviceRiskResult {
        checkInitialised()
        val originalMode = config!!.mode
        val localConfig  = config!!.copy(mode = SdkMode.LOCAL_ONLY)
        config = localConfig
        val result = assess(context, sessionId)
        config = config!!.copy(mode = originalMode)
        return result
    }

    /**
     * Java-friendly callback version of assess().
     * Use from Java code or when coroutines are not available.
     */
    fun assessAsync(context: Context, sessionId: String, callback: AssessCallback) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            try {
                val result = assess(context, sessionId)
                callback.onResult(result)
            } catch (e: Exception) {
                callback.onError(e)
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun checkInitialised() {
        if (apiKey == null || config == null) throw DeviceRiskInitException()
    }

    private fun getOrCreateClientId(context: Context): String {
        val prefs = context.getSharedPreferences("device_risk_sdk", Context.MODE_PRIVATE)
        return prefs.getString("client_id", null) ?: run {
            val id = UUID.randomUUID().toString()
            prefs.edit().putString("client_id", id).apply()
            id
        }
    }

    private fun log(msg: String) {
        if (config?.logLevel == com.gururajgj.devicerisk.models.LogLevel.DEBUG
            || config?.logLevel == com.gururajgj.devicerisk.models.LogLevel.WARN) {
            android.util.Log.d("DeviceRiskSdk", msg)
        }
    }
}

/** Java-friendly callback interface for assessAsync() */
interface AssessCallback {
    fun onResult(result: DeviceRiskResult)
    fun onError(error: Exception)
}
