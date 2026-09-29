package com.ironpanel.app.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.ironpanel.app.data.AppSnapshot
import com.ironpanel.app.data.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Owns connection state, enforces the quota/expiry gate before AND during
 * every session (polls /status every 45s; disconnects the moment the panel
 * reports access_ok=false, e.g. traffic exhausted or account expired).
 */
object VpnManager {
    const val VPN_REQUEST_CODE = 4401

    sealed interface State {
        data object Idle : State
        data object Preparing : State
        data class Connected(val protocol: String, val label: String) : State
        data class Blocked(val reasonFa: String, val reasonEn: String) : State
        data class Error(val message: String) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repo = SubscriptionRepository()
    private var pollJob: Job? = null

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    fun hasSubscription(): Boolean = lastBaseUrl != null && lastToken != null
    var lastBaseUrl: String? = null
        private set
    var lastToken: String? = null
        private set

    fun rememberSubscription(baseUrl: String, token: String) {
        lastBaseUrl = baseUrl
        lastToken = token
    }

    /** Entry point from UI. Returns immediately; progress flows via [state]. */
    fun connect(activity: Activity, snapshot: AppSnapshot, protocol: String) {
        val gate = QuotaGate.check(
            snapshot.user.accessOk, snapshot.user.connectable,
            snapshot.user.accessReason, snapshot.user.accessReasonEn,
        )
        if (gate is QuotaGate.Verdict.Deny) {
            _state.value = State.Blocked(gate.reasonFa, gate.reasonEn)
            return
        }
        val intent = VpnService.prepare(activity)
        if (intent != null) {
            // System VPN consent — MainActivity forwards onActivityResult here.
            activity.startActivityForResult(intent, VPN_REQUEST_CODE)
            PendingConnect.snapshot = snapshot
            PendingConnect.protocol = protocol
            return
        }
        startTunnel(activity, snapshot, protocol)
    }

    fun onVpnPermissionResult(resultOk: Boolean) {
        val snapshot = PendingConnect.snapshot
        val protocol = PendingConnect.protocol
        PendingConnect.snapshot = null
        if (resultOk && snapshot != null && protocol != null) {
            startTunnel(snapshotRefContext!!, snapshot, protocol)
        } else if (!resultOk) {
            _state.value = State.Error("VPN permission denied")
        }
    }

    // Activity context captured for the permission round-trip.
    var snapshotRefContext: Context? = null

    private fun startTunnel(context: Context, snapshot: AppSnapshot, protocol: String) {
        _state.value = State.Preparing
        rememberSubscription(
            snapshot.subscription.page.substringBefore("/s/"), snapshotKey(snapshot)
        )
        scope.launch {
            // Fresh server verdict right before touching any core.
            val verdict = refreshGate()
            if (verdict is QuotaGate.Verdict.Deny) {
                _state.value = State.Blocked(verdict.reasonFa, verdict.reasonEn)
                return@launch
            }
            // Mark our VpnService active for status + foreground notification.
            context.startService(
                Intent(context, IronVpnService::class.java)
                    .setAction(IronVpnService.ACTION_START)
                    .putExtra(IronVpnService.EXTRA_LABEL, "$protocol · ${snapshot.user.username}")
            )
            val result: Result<String> = when (protocol.lowercase()) {
                "wireguard" -> WireGuardConnector.connect(context, snapshot)
                "openvpn" -> OpenVpnConnector.connect(context, snapshot)
                "xray" -> XrayConnector.connect(context, snapshot)
                "hysteria2" -> HysteriaConnector.connect(context, snapshot)
                else -> GenericConnector.connect(context, snapshot, protocol)
            }
            result.fold(
                onSuccess = { label ->
                    _state.value = State.Connected(protocol, label)
                    startPolling(context)
                },
                onFailure = { e ->
                    context.startService(
                        Intent(context, IronVpnService::class.java)
                            .setAction(IronVpnService.ACTION_STOP)
                    )
                    _state.value = State.Error(e.message ?: "Connect failed")
                }
            )
        }
    }

    private fun snapshotKey(snapshot: AppSnapshot): String {
        // token is the last /s/ path segment of the page URL.
        return snapshot.subscription.page.substringAfterLast("/s/").substringBefore("/")
            .ifEmpty { lastToken.orEmpty() }
    }

    fun disconnect(context: Context) {
        scope.launch {
            runCatching { WireGuardConnector.disconnect() }
            runCatching { XrayConnector.disconnect() }
            runCatching { OpenVpnConnector.disconnect() }
            runCatching { HysteriaConnector.disconnect() }
            context.startService(
                Intent(context, IronVpnService::class.java)
                    .setAction(IronVpnService.ACTION_STOP)
            )
            stopPolling()
            _state.value = State.Idle
        }
    }

    fun onServiceStopped() {
        stopPolling()
        if (_state.value is State.Connected || _state.value is State.Preparing) {
            _state.value = State.Idle
        }
    }

    private fun startPolling(context: Context) {
        stopPolling()
        pollJob = scope.launch {
            while (true) {
                delay(45_000)
                val verdict = refreshGate()
                if (verdict is QuotaGate.Verdict.Deny) {
                    _state.value = State.Blocked(verdict.reasonFa, verdict.reasonEn)
                    disconnect(context)
                    break
                }
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun refreshGate(): QuotaGate.Verdict {
        val base = lastBaseUrl ?: return QuotaGate.Verdict.Allow
        val token = lastToken ?: return QuotaGate.Verdict.Allow
        return try {
            QuotaGate.check(repo.pollStatus(base, token))
        } catch (_: Exception) {
            // Poll failure must not kill a live tunnel; server still enforces.
            QuotaGate.Verdict.Allow
        }
    }

    private object PendingConnect {
        var snapshot: AppSnapshot? = null
        var protocol: String? = null
    }
}
