package com.ironpanel.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ironapp")

/** Persisted subscription + UI prefs. Tokens stay in private app storage only. */
class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("sub_token")
    private val baseKey = stringPreferencesKey("sub_base")
    private val themeKey = stringPreferencesKey("theme") // system|light|dark
    private val lastProtoKey = stringPreferencesKey("last_protocol")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val baseUrl: Flow<String?> = context.dataStore.data.map { it[baseKey] }
    val theme: Flow<String> = context.dataStore.data.map { it[themeKey] ?: "system" }
    val lastProtocol: Flow<String?> = context.dataStore.data.map { it[lastProtoKey] }

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
}
