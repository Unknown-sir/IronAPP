package com.ironpanel.app.data

/**
 * Accepts every form a user may paste — full page URL, any feed URL
 * (…/s/TOKEN, …/s/TOKEN/raw|clash|singbox|hiddify|app.json|status) or the
 * bare token — and normalises it to (baseUrl, token).
 */
object SubLinkParser {
    private val tokenPath = Regex("/s/([A-Za-z0-9_\\-]{8,})")

    data class Parsed(val baseUrl: String, val token: String) {
        val appJsonUrl: String get() = "$baseUrl/s/$token/app.json"
        val statusUrl: String get() = "$baseUrl/s/$token/status"
    }

    fun parse(rawInput: String): Parsed? {
        val input = rawInput.trim().removeSurrounding("\"").removeSurrounding("'")
        if (input.isEmpty()) return null
        // Bare token (IronPanel issues urlsafe ~64-char tokens).
        if (!input.contains("://") && !input.contains("/")) {
            if (input.length >= 8) return Parsed("", input)
            return null
        }
        val match = tokenPath.find(input) ?: return null
        val token = match.groupValues[1]
        val base = input.substringBefore("/s/").trimEnd('/')
        if (base.isEmpty()) return null
        return Parsed(base, token)
    }
}
