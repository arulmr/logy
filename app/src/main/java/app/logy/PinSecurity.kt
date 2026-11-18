package app.logy

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(pin: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expected)
}

class PinSecurity(context: Context) {
    private val preferences = context.getSharedPreferences("security", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = preferences.contains(KEY_HASH) && preferences.contains(KEY_SALT)

    fun setPin(pin: String) {
        require(pin.matches(Regex("\\d{4}"))) { "PIN must contain exactly four digits." }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = PinHasher.hash(pin, salt)
        preferences.edit()
            .putString(KEY_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_HASH, Base64.getEncoder().encodeToString(hash))
            .apply()
    }

    fun verify(pin: String): Boolean {
        if (!pin.matches(Regex("\\d{4}"))) return false
        val salt = preferences.getString(KEY_SALT, null)?.let(Base64.getDecoder()::decode) ?: return false
        val expected = preferences.getString(KEY_HASH, null)?.let(Base64.getDecoder()::decode) ?: return false
        return PinHasher.verify(pin, salt, expected)
    }

    fun removePin() {
        preferences.edit().remove(KEY_SALT).remove(KEY_HASH).putBoolean(KEY_BIOMETRIC, false).apply()
    }

    fun isBiometricEnabled(): Boolean = hasPin() && preferences.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BIOMETRIC, enabled && hasPin()).apply()
    }

    private companion object {
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
        const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
