package com.federicogiordano.mirorientatore.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.federicogiordano.mirorientatore.api.synchronizedBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path
import kotlin.concurrent.Volatile

expect fun getDataStoreFile(fileName: String): Path

fun createDataStore(fileName: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { getDataStoreFile(fileName) }
    )

internal const val dataStoreFileName = "mirorientatore_token.preferences_pb"

class TokenStorage(private val dataStore: DataStore<Preferences>) {
    private val tokenKey = stringPreferencesKey("auth_token")

    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[tokenKey] = token
        }
    }

    fun getToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[tokenKey]
        }
    }

    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(tokenKey)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: TokenStorage? = null
        private val lock = Any()

        fun getInstance(dataStore: DataStore<Preferences>): TokenStorage =
            INSTANCE ?: synchronizedBlock(lock) {
                INSTANCE ?: TokenStorage(dataStore).also { INSTANCE = it }
            }
    }
}