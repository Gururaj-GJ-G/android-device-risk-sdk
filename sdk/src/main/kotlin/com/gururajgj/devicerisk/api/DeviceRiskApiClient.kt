package com.gururajgj.devicerisk.api

import com.gururajgj.devicerisk.exceptions.DeviceRiskInvalidKeyException
import com.gururajgj.devicerisk.exceptions.DeviceRiskNetworkException
import com.gururajgj.devicerisk.exceptions.DeviceRiskTimeoutException
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

internal class DeviceRiskApiClient(
    private val apiKey     : String,
    private val baseUrl    : String,
    private val timeoutMs  : Long
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.your-backend.example.com"
    }

    fun assess(request: DeviceRiskRequest): DeviceRiskResponse {
        val url = URL("$baseUrl/v1/device-risk")
        val body = buildJson(request)

        return try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod        = "POST"
                connectTimeout       = timeoutMs.toInt()
                readTimeout          = timeoutMs.toInt()
                doOutput             = true
                setRequestProperty("Content-Type",    "application/json")
                setRequestProperty("X-DeviceRisk-Key",   apiKey)
                setRequestProperty("X-DeviceRisk-SDK",   request.sdkVersion)
            }

            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            when {
                code == 401 || code == 403 -> throw DeviceRiskInvalidKeyException()
                code !in 200..299          -> throw DeviceRiskNetworkException("API error $code")
            }

            val responseBody = conn.inputStream.bufferedReader().readText()
            parseResponse(responseBody)

        } catch (e: SocketTimeoutException) {
            throw DeviceRiskTimeoutException()
        } catch (e: IOException) {
            throw DeviceRiskNetworkException("Network error: ${e.message}", e)
        }
    }

    private fun buildJson(r: DeviceRiskRequest): String {
        return JSONObject().apply {
            put("client_id",                    r.clientId)
            put("session_id",                   r.sessionId)
            put("sdk_version",                  r.sdkVersion)
            put("timestamp",                    r.timestamp)
            put("features", JSONObject().apply {
                put("rooted",                       r.rooted)
                put("emulator",                     r.emulator)
                put("hooking_detected",             r.hookingDetected)
                put("known_malware_count",          r.knownMalwareCount)
                put("risky_permission_apps_count",  r.riskyPermissionAppsCount)
                put("sms_device_admin_count",       r.smsDeviceAdminCount)
                put("remote_control_app_present",   r.remoteControlAppPresent)
                put("overlay_abuse_suspected",      r.overlayAbuseSuspected)
                put("notification_listener_present",r.notificationListenerPresent)
            })
        }.toString()
    }

    private fun parseResponse(json: String): DeviceRiskResponse {
        val obj = JSONObject(json)
        val reasonsArray = obj.optJSONArray("reasons") ?: JSONArray()
        val reasons = (0 until reasonsArray.length()).map { reasonsArray.getString(it) }
        return DeviceRiskResponse(
            assessmentId = obj.getString("assessment_id"),
            riskScore    = obj.getInt("risk_score"),
            riskBand     = obj.getString("risk_band"),
            reasons      = reasons
        )
    }
}
