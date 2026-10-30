package com.runehearth.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.store by preferencesDataStore("runehearth_prefs")

/** App-level preferences (DataStore). The device id identifies this phone to hosts and is generated once. */
class Prefs(private val context: Context) {
    private val deviceKey = stringPreferencesKey("device_id")
    private val nameKey = stringPreferencesKey("player_name")
    private val themeKey = stringPreferencesKey("theme")
    private val lastHostKey = stringPreferencesKey("last_host")

    suspend fun deviceId(): String {
        context.store.data.first()[deviceKey]?.let { return it }
        val id = UUID.randomUUID().toString()
        context.store.edit { if (it[deviceKey] == null) it[deviceKey] = id }
        return context.store.data.first()[deviceKey] ?: id
    }

    val playerName: Flow<String> = context.store.data.map { it[nameKey] ?: "" }
    suspend fun setPlayerName(n: String) { context.store.edit { it[nameKey] = n.take(40) } }

    val theme: Flow<String> = context.store.data.map { it[themeKey] ?: "medieval" }
    suspend fun setTheme(id: String) { context.store.edit { it[themeKey] = id } }

    val lastHost: Flow<String> = context.store.data.map { it[lastHostKey] ?: "" }
    suspend fun setLastHost(addr: String) { context.store.edit { it[lastHostKey] = addr } }
}
