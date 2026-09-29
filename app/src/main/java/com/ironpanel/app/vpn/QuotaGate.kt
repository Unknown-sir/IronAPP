package com.ironpanel.app.vpn

import com.ironpanel.app.data.StatusSnapshot

/**
 * Client-side copy of the server gate. The panel ALWAYS enforces quota/expiry
 * itself (403 on feeds + VPN auth hooks); this gate only gives instant UX and
 * auto-disconnects without waiting for a tunnel error.
 */
object QuotaGate {
    sealed interface Verdict {
        data object Allow : Verdict
        data class Deny(val reasonFa: String, val reasonEn: String) : Verdict
    }

    fun check(accessOk: Boolean, connectable: Boolean, reasonFa: String, reasonEn: String): Verdict {
        if (!accessOk) return Verdict.Deny(reasonFa, reasonEn)
        if (!connectable) {
            return Verdict.Deny(
                "پروتکل فعالی برای این کاربر وجود ندارد",
                "no enabled protocol for this user",
            )
        }
        return Verdict.Allow
    }

    fun check(status: StatusSnapshot): Verdict =
        check(status.accessOk, status.connectable, status.accessReason, status.accessReasonEn)
}
