package com.ironpanel.app.data

/** Single entry point from UI → panel JSON API. Throws on transport errors. */
class SubscriptionRepository(private val api: PanelApi = ApiFactory.api) {

    suspend fun loadSnapshot(baseUrl: String, token: String): AppSnapshot {
        val parsed = SubLinkParser.parse(
            if (baseUrl.isBlank()) token else "$baseUrl/s/$token/app.json"
        ) ?: throw IllegalArgumentException("Bad subscription link")
        val url = if (parsed.baseUrl.isBlank()) {
            // Bare token with no base: caller must supply baseUrl.
            throw IllegalArgumentException("Panel address missing")
        } else {
            parsed.appJsonUrl
        }
        val snap = api.appJson(url)
        if (!snap.success) throw IllegalStateException("Panel refused the request")
        return snap
    }

    suspend fun pollStatus(baseUrl: String, token: String): StatusSnapshot {
        val url = "$baseUrl/s/$token/status"
        val st = api.status(url)
        if (!st.success) throw IllegalStateException("Panel refused the request")
        return st
    }
}
