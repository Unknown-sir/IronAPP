package com.ironpanel.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "ironapp")

/** A hand-added single config (no subscription needed). */
data class CustomConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    /** xray | wireguard | openvpn */
    val kind: String = "xray",
    val payload: String = "",
)

/** Persisted subscription + custom configs + UI prefs. Tokens stay in private app storage only. */
class SessionStore(private val context: Context) {
    private val gson = Gson()
    private val tokenKey = stringPreferencesKey("sub_token")
    private val baseKey = stringPreferencesKey("sub_base")
    private val themeKey = stringPreferencesKey("theme") // system|light|dark
    private val lastProtoKey = stringPreferencesKey("last_protocol")
    private val customKey = stringPreferencesKey("custom_configs")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val baseUrl: Flow<String?> = context.dataStore.data.map { it[baseKey] }
    val theme: Flow<String> = context.dataStore.data.map { it[themeKey] ?: "system" }
    val lastProtocol: Flow<String?> = context.dataStore.data.map { it[lastProtoKey] }
    val customConfigs: Flow<List<CustomConfig>> = context.dataStore.data.map { prefs ->
        val raw = prefs[customKey].orEmpty()
        if (raw.isBlank()) return@map emptyList()
        try {
            gson.fromJson<List<CustomConfig>>(
                raw, object : TypeToken<List<CustomConfig>>() {}.type
            ) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveSubscription(baseUrl: String, token: String) {
        context.dataStore.edit {
            it[baseKey] = baseUrl
            it[tokenKey] = token
        }
    }

    suspend fun clearSubscription() {
        context.dataStore.edit {
            it.remove(baseKey)
            it.remove(tokenKey)
        }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { it[themeKey] = theme }
    }

    suspend fun saveLastProtocol(protocol: String) {
        context.dataStore.edit { it[lastProtoKey] = protocol }
    }

    suspend fun addCustom(config: CustomConfig) {
        val current = customConfigs.first().toMutableList()
        current.add(0, config)
        context.dataStore.edit { it[customKey] = gson.toJson(current) }
    }

    suspend fun deleteCustom(id: String) {
        val current = customConfigs.first().filterNot { it.id == id }
        context.dataStore.edit { it[customKey] = gson.toJson(current) }
    }
}
