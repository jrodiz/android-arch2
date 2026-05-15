package com.rodiz.arch2.core.session.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.session.domain.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SessionRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SessionRepository {

    private object Keys {
        val UserId = stringPreferencesKey("session_user_id")
        val Token = stringPreferencesKey("session_token")
    }

    override fun observe(): Flow<Session?> = dataStore.data.map { it.toSession() }

    override suspend fun current(): Session? = dataStore.data.first().toSession()

    override suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs[Keys.UserId] = session.userId
            prefs[Keys.Token] = session.token
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.UserId)
            prefs.remove(Keys.Token)
        }
    }

    private fun Preferences.toSession(): Session? {
        val id = this[Keys.UserId] ?: return null
        val token = this[Keys.Token] ?: return null
        return Session(userId = id, token = token)
    }
}
