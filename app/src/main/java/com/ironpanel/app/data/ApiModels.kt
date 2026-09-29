package com.ironpanel.app.data

import com.google.gson.annotations.SerializedName

/** Mirrors GET /s/<token>/app.json (panel v2.0.11+, see docs/IRONAPP.md). */
data class AppSnapshot(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("min_app_version") val minAppVersion: String = "1.0.0",
    @SerializedName("panel_version") val panelVersion: String = "",
    @SerializedName("server_time") val serverTime: String = "",
    @SerializedName("user") val user: AppUser = AppUser(),
)

data class AppUser(
    @SerializedName("username") val username: String = "",
    @SerializedName("enabled") val enabled: Boolean = false,
    @SerializedName("access_ok") val accessOk: Boolean = false,
    @SerializedName("connectable") val connectable: Boolean = false,
    @SerializedName("access_reason") val accessReason: String = "",
    @SerializedName("access_reason_en") val accessReasonEn: String = "",
    @SerializedName("protocols") val protocols: List<String> = emptyList(),
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("unlimited_time") val unlimitedTime: Boolean = false,
    @SerializedName("remaining_days") val remainingDays: Long? = null,
    @SerializedName("remaining_seconds") val remainingSeconds: Long? = null,
    @SerializedName("usage") val usage: Usage = Usage(),
    @SerializedName("subscription") val subscription: SubUrls = SubUrls(),
    @SerializedName("configs") val configs: Map<String, String> = emptyMap(),
    @SerializedName("xray_links") val xrayLinks: List<String> = emptyList(),
)

data class Usage(
    @SerializedName("used_bytes") val usedBytes: Long = 0,
    @SerializedName("remaining_bytes") val remainingBytes: Long? = null,
    @SerializedName("total_bytes") val totalBytes: Long = 0,
    @SerializedName("used_human") val usedHuman: String = "",
    @SerializedName("remaining_human") val remainingHuman: String = "",
    @SerializedName("total_human") val totalHuman: String = "",
    @SerializedName("unlimited_traffic") val unlimitedTraffic: Boolean = false,
)

data class SubUrls(
    @SerializedName("page") val page: String = "",
    @SerializedName("raw") val raw: String = "",
    @SerializedName("clash") val clash: String = "",
    @SerializedName("singbox") val singbox: String = "",
    @SerializedName("hiddify") val hiddify: String = "",
    @SerializedName("app_json") val appJson: String = "",
    @SerializedName("status") val status: String = "",
)

/** Mirrors GET /s/<token>/status — lightweight quota/expiry poll. */
data class StatusSnapshot(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("username") val username: String = "",
    @SerializedName("access_ok") val accessOk: Boolean = false,
    @SerializedName("connectable") val connectable: Boolean = false,
    @SerializedName("access_reason") val accessReason: String = "",
    @SerializedName("access_reason_en") val accessReasonEn: String = "",
    @SerializedName("protocols") val protocols: List<String> = emptyList(),
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("remaining_days") val remainingDays: Long? = null,
    @SerializedName("remaining_seconds") val remainingSeconds: Long? = null,
    @SerializedName("used_bytes") val usedBytes: Long = 0,
    @SerializedName("remaining_bytes") val remainingBytes: Long? = null,
    @SerializedName("total_bytes") val totalBytes: Long = 0,
    @SerializedName("unlimited_traffic") val unlimitedTraffic: Boolean = false,
    @SerializedName("unlimited_time") val unlimitedTime: Boolean = false,
)
