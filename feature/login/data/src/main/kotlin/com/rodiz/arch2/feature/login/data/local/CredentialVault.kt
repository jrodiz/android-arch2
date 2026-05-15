package com.rodiz.arch2.feature.login.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rodiz.arch2.feature.login.domain.model.Credentials
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores user credentials inside EncryptedSharedPreferences, backed by a
 * Keystore-resident master key (AES-256 GCM). Used only to support biometric
 * unlock. If androidx.security.crypto is deprecated in your toolchain, swap
 * to direct AndroidKeyStore + DataStore<EncryptedBytes>.
 */
@Singleton
internal class CredentialVault @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun store(credentials: Credentials) {
        prefs.edit()
            .putString(KEY_EMAIL, credentials.email)
            .putString(KEY_PASSWORD, credentials.password)
            .apply()
    }

    fun load(): Credentials? {
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        return Credentials(email = email, password = password)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun exists(): Boolean = prefs.contains(KEY_EMAIL) && prefs.contains(KEY_PASSWORD)

    private companion object {
        const val FILE_NAME = "credentials_vault"
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"
    }
}
