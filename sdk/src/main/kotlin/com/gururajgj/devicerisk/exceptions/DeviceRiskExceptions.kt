package com.gururajgj.devicerisk.exceptions

/** Base class for all Device Risk SDK exceptions */
open class DeviceRiskException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** SDK not initialised — call DeviceRiskSdk.init() in your Application class first */
class DeviceRiskInitException(message: String = "DeviceRiskSdk not initialised. Call DeviceRiskSdk.init() in your Application class.") : DeviceRiskException(message)

/** API key invalid or expired */
class DeviceRiskInvalidKeyException(message: String = "Invalid or expired API key. Contact whoever issued your API key.") : DeviceRiskException(message)

/** Risk API unreachable */
class DeviceRiskNetworkException(message: String, cause: Throwable? = null) : DeviceRiskException(message, cause)

/** Assessment exceeded configured timeout */
class DeviceRiskTimeoutException(message: String = "Assessment timed out. Proceed as LOW (fail-safe).") : DeviceRiskException(message)
