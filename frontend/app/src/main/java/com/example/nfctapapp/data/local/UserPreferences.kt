package com.example.nfctapapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nfctapapp.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_PHONE = stringPreferencesKey("user_phone")
        private val NFC_UID = stringPreferencesKey("nfc_uid")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val LAST_LOGIN_TIMESTAMP = longPreferencesKey("last_login")
        private const val CACHE_EXPIRY_DAYS = 30L
    }

    val cachedUser: Flow<User?> = context.dataStore.data.map { prefs ->
        val userId = prefs[USER_ID] ?: return@map null
        val isLoggedIn = prefs[IS_LOGGED_IN] ?: false
        if (!isLoggedIn) return@map null

        val lastLogin = prefs[LAST_LOGIN_TIMESTAMP] ?: 0L
        val daysSinceLogin = (System.currentTimeMillis() - lastLogin) / (1000 * 60 * 60 * 24)
        if (daysSinceLogin > CACHE_EXPIRY_DAYS) {
            return@map null
        }

        User(
            id = userId,
            name = prefs[USER_NAME] ?: "",
            phone = prefs[USER_PHONE]
        )
    }

    val cachedNfcUid: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[NFC_UID]
    }

    val cachedDeviceId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[DEVICE_ID]
    }

    suspend fun cacheUser(user: User, nfcUid: String, deviceId: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = user.id
            prefs[USER_NAME] = user.name
            prefs[USER_PHONE] = user.phone ?: ""
            prefs[NFC_UID] = nfcUid
            prefs[DEVICE_ID] = deviceId
            prefs[IS_LOGGED_IN] = true
            prefs[LAST_LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun clearCache() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun verifyOffline(nfcUid: String, deviceId: String): User? {
        val prefs = context.dataStore.data.first()
        val cachedNfc = prefs[NFC_UID]
        val cachedDevice = prefs[DEVICE_ID]
        val isLoggedIn = prefs[IS_LOGGED_IN] ?: false

        if (!isLoggedIn || cachedNfc != nfcUid || cachedDevice != deviceId) {
            return null
        }

        val lastLogin = prefs[LAST_LOGIN_TIMESTAMP] ?: 0L
        val daysSinceLogin = (System.currentTimeMillis() - lastLogin) / (1000 * 60 * 60 * 24)
        if (daysSinceLogin > CACHE_EXPIRY_DAYS) {
            return null
        }

        return User(
            id = prefs[USER_ID] ?: return null,
            name = prefs[USER_NAME] ?: "",
            phone = prefs[USER_PHONE]
        )
    }
}
