package com.gururajgj.devicerisk.models

enum class SdkMode {
    /** On-device detection + API scoring (default, recommended) */
    HYBRID,
    /** On-device only — no API call. Max privacy, lower accuracy. */
    LOCAL_ONLY,
    /** API only — skip on-device checks */
    API_ONLY
}
