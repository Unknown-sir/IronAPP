package com.ironpanel.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ironpanel.app.data.AppSnapshot
import com.ironpanel.app.data.CustomConfig
import com.ironpanel.app.data.SessionStore
import com.ironpanel.app.data.SubLinkParser
import com.ironpanel.app.data.SubscriptionRepository
import com.ironpanel.app.vpn.box.BoxNode
import com.ironpanel.app.vpn.box.ConfigParseException
import com.ironpanel.app.vpn.box.ovpnToNode
import com.ironpanel.app.vpn.box.wireGuardConfToNode
import com.ironpanel.app.vpn.box.xrayLinkToNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface LoadState {
    data object Empty : LoadState
    data object Loading : LoadState
    data class Ready(val snapshot: AppSnapshot, val baseUrl: String, val token: String) : LoadState
    data class Failed(val message: String) : LoadState
}

/** Shared UI state: subscription snapshot, selected protocol, theme. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SessionStore(app)
    private val repo = SubscriptionRepository()

    private val _load = MutableStateFlow<LoadState>(LoadState.Empty)
    val load: StateFlow<LoadState> = _load

    private val _protocol = MutableStateFlow("xray")
    val protocol: StateFlow<String> = _protocol

    val theme = store.theme
    val lastProtocol = store.lastProtocol
    val customConfigs = store.customConfigs

    init {
        viewModelScope.launch {
            val token = store.token.first()
            val base = store.baseUrl.first()
            if (!token.isNullOrBlank() && !base.isNullOrBlank()) {
                refresh(base, token)
            }
        }
        viewModelScope.launch {
            store.lastProtocol.first()?.let { _protocol.value = it }
        }
    }

    fun selectProtocol(p: String) {
        _protocol.value = p
        viewModelScope.launch { store.saveLastProtocol(p) }
    }

    fun import(raw: String, onError: (String) -> Unit) {
        val parsed = SubLinkParser.parse(raw)
        if (parsed == null || parsed.token.isBlank()) {
            onError("Invalid link")
            return
        }
        // Bare token: reuse the stored panel address when we have one.
        viewModelScope.launch {
            val base = parsed.baseUrl.ifBlank { store.baseUrl.first().orEmpty() }
            if (base.isBlank()) {
                onError("Panel address missing — paste the full link")
                return@launch
            }
            refresh(base, parsed.token)
        }
    }

    fun refresh() {
        val current = _load.value as? LoadState.Ready ?: return
        refresh(current.baseUrl, current.token)
    }

    private fun refresh(baseUrl: String, token: String) {
        _load.value = LoadState.Loading
        viewModelScope.launch {
            try {
                val snap = repo.loadSnapshot(baseUrl, token)
                store.saveSubscription(baseUrl, token)
                if (snap.user.protocols.isNotEmpty() &&
                    _protocol.value !in snap.user.protocols
                ) {
                    _protocol.value = snap.user.protocols.first()
                }
                _load.value = LoadState.Ready(snap, baseUrl, token)
            } catch (e: Exception) {
                _load.value = LoadState.Failed(e.message ?: "Load failed")
            }
        }
    }

    fun forget() {
        viewModelScope.launch { store.clearSubscription() }
        _load.value = LoadState.Empty
    }

    /** Validate + persist a hand-added single config (xray|wireguard|openvpn). */
    fun addCustomConfig(name: String, kind: String, payload: String, onError: (String) -> Unit) {
        val cleanName = name.trim().ifEmpty { kind }
        try {
            customNode(kind, payload) // throws with a precise reason
        } catch (e: ConfigParseException) {
            onError(e.message ?: "bad config")
            return
        } catch (e: Exception) {
            onError(e.message ?: "bad config")
            return
        }
        viewModelScope.launch {
            store.addCustom(CustomConfig(name = cleanName, kind = kind, payload = payload.trim()))
        }
    }

    fun deleteCustomConfig(id: String) {
        viewModelScope.launch { store.deleteCustom(id) }
    }

    fun saveTheme(mode: String) {
        viewModelScope.launch { store.saveTheme(mode) }
    }

    companion object {
        /** Convert one hand-pasted config into a core node (shared with connect). */
        fun customNode(kind: String, payload: String): BoxNode {
            val body = payload.trim()
            if (body.isEmpty()) throw ConfigParseException("empty config")
            return when (kind) {
                "wireguard" -> wireGuardConfToNode(body, "custom")
                "openvpn" -> ovpnToNode(body, "custom")
                else -> xrayLinkToNode(body.lineSequence().first { it.isNotBlank() }, "custom")
            }
        }
    }
}
