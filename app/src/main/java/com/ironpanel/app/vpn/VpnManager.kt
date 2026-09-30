package com.ironpanel.app.vpn

import android.app.Activity
import android.content.Context
import android.net.VpnService
import com.ironpanel.app.data.AppSnapshot
import com.ironpanel.app.data.SubscriptionRepository
import com.ironpanel.app.vpn.box.ConfigParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Owns connection state for the embedded core. The quota/expiry gate runs
 * before every connect AND during the session (45s /status polls); the
 * panel enforces the same gate server-side, so limits cannot be bypassed.
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
    private var pollContext: Context? = null
    private var tunnelContext: Context? = null

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    var lastBaseUrl: String? = null
        private set
    var lastToken: String? = null
        private set

    fun rememberSubscription(baseUrl: String, token: String) {
        lastBaseUrl = baseUrl
        lastToken = token
    }

    /** Protocols IronAPP tunnels itself (everything else is view-only). */
    fun isEngineProtocol(protocol: String): Boolean =
        protocol.lowercase() in SingBoxConnector.ENGINE_PROTOCOLS

    fun connect(activity: Activity, snapshot: AppSnapshot, protocol: String, linkIndex: Int = 0) {
        if (!isEngineProtocol(protocol)) {
            _state.value = State.Error("protocol $protocol is view-only in IronAPP")
            return
        }
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
            PendingConnect.snapshot = snapshot
            PendingConnect.protocol = protocol
            PendingConnect.linkIndex = linkIndex
            PendingConnect.context = activity
            activity.startActivityForResult(intent, VPN_REQUEST_CODE)
            return
        }
        startTunnel(activity, snapshot, protocol, linkIndex)
    }

    fun onVpnPermissionResult(resultOk: Boolean) {
        val snapshot = PendingConnect.snapshot
        val protocol = PendingConnect.protocol
        val context = PendingConnect.context
        PendingConnect.snapshot = null
        PendingConnect.context = null
        if (resultOk && snapshot != null && protocol != null && context != null) {
            startTunnel(context, snapshot, protocol, PendingConnect.linkIndex)
        } else if (!resultOk) {
            _state.value = State.Error("VPN permission denied")
        }
    }

    private fun startTunnel(context: Context, snapshot: AppSnapshot, protocol: String, linkIndex: Int) {
        _state.value = State.Preparing
        tunnelContext = context.applicationContext
        rememberSubscription(
            snapshot.user.subscription.page.substringBefore("/s/"), snapshotKey(snapshot)
        )
        scope.launch {
            val verdict = refreshGate()
            if (verdict is QuotaGate.Verdict.Deny) {
                _state.value = State.Blocked(verdict.reasonFa, verdict.reasonEn)
                return@launch
            }
            try {
                SingBoxConnector.connect(context, snapshot, protocol, linkIndex)
                // IronVpnService confirms via onCoreStarted(); time out just in case.
                delay(30_000)
                if (_state.value is State.Preparing) {
                    if (IronVpnService.runningLabel != null) {
                        _state.value = State.Connected(
                            protocol, IronVpnService.runningLabel ?: protocol
                        )
                        startPolling(context)
                    } else {
                        _state.value = State.Error("core did not start")
                    }
                }
            } catch (e: ConfigParseException) {
                _state.value = State.Error(e.message ?: "bad config")
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "connect failed")
            }
        }
    }

    /** Called by IronVpnService once the core is up. */
    fun onCoreStarted() {
        scope.launch {
            if (_state.value is State.Preparing) {
                val label = IronVpnService.runningLabel ?: "connected"
                _state.value = State.Connected(label.substringBefore(" ·"), label)
                tunnelContext?.let { startPolling(it) }
            }
        }
    }

    /** Called by IronVpnService when the core fails to start. */
    fun onCoreFailed(message: String) {
        scope.launch {
            if (_state.value is State.Preparing) {
                _state.value = State.Error(message)
            }
        }
    }

    private fun snapshotKey(snapshot: AppSnapshot): String {
        return snapshot.user.subscription.page.substringAfterLast("/s/").substringBefore("/")
            .ifEmpty { lastToken.orEmpty() }
    }

    fun disconnect(context: Context) {
        stopPolling()
        try {
            SingBoxConnector.disconnect(context)
        } catch (_: Exception) {
        }
        _state.value = State.Idle
    }

    fun onServiceStopped() {
        stopPolling()
        if (_state.value is State.Connected || _state.value is State.Preparing) {
            _state.value = State.Idle
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun startPolling(context: Context) {
        stopPolling()
        pollContext = context.applicationContext
        pollJob = scope.launch {
            while (true) {
                delay(45_000)
                val verdict = refreshGate()
                if (verdict is QuotaGate.Verdict.Deny) {
                    _state.value = State.Blocked(verdict.reasonFa, verdict.reasonEn)
                    pollContext?.let { disconnect(it) }
                    break
                }
            }
        }
    }

    private suspend fun refreshGate(): QuotaGate.Verdict {
        val base = lastBaseUrl ?: return QuotaGate.Verdict.Allow
        val token = lastToken ?: return QuotaGate.Verdict.Allow
        return try {
            QuotaGate.check(repo.pollStatus(base, token))
        } catch (_: Exception) {
            QuotaGate.Verdict.Allow
        }
    }

    private object PendingConnect {
        var snapshot: AppSnapshot? = null
        var protocol: String? = null
        var linkIndex: Int = 0
        var context: Context? = null
    }
}
