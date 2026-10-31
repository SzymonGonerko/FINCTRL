package com.finctrl.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SecurityStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "finctrl_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    private val _pinSet = MutableStateFlow(prefs.contains(KEY_HASH))
    val pinSet: StateFlow<Boolean> = _pinSet

    val isPinSet: Boolean get() = _pinSet.value
    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIO, false)
        set(v) = prefs.edit().putBoolean(KEY_BIO, v).apply()
    var localMode: Boolean
        get() = prefs.getBoolean(KEY_LOCAL, false)
        set(v) = prefs.edit().putBoolean(KEY_LOCAL, v).apply()

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash(salt, pin), Base64.NO_WRAP))
            .apply()
        _pinSet.value = true
        _unlocked.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val expected = prefs.getString(KEY_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(expected, hash(salt, pin)).also { if (it) _unlocked.value = true }
    }

    fun clearPin() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).putBoolean(KEY_BIO, false).apply()
        _pinSet.value = false
    }

    fun markUnlocked() { _unlocked.value = true }
    fun lock() { _unlocked.value = false }

    private fun hash(salt: ByteArray, pin: String) =
        MessageDigest.getInstance("SHA-256").digest(salt + pin.toByteArray())

    private companion object {
        const val KEY_SALT = "pin_salt"; const val KEY_HASH = "pin_hash"
        const val KEY_BIO = "bio_enabled"; const val KEY_LOCAL = "local_mode"
    }
}
