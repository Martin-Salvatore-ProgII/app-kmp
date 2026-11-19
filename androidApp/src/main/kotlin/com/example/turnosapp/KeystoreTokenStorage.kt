package com.example.turnosapp

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.turnosapp.core.session.TokenStorage
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda el JWT cifrado (ADR-0049, ADR-0076). La clave AES vive en el Android Keystore: la genera
 * el sistema y nunca sale de él, así que ni la app puede leerla. En las preferencias de la app
 * queda solo el JWT cifrado, que sin esa clave no sirve.
 *
 * Se usa el Keystore directo, y no `EncryptedSharedPreferences`, porque esa librería está deprecada.
 */
class KeystoreTokenStorage(context: Context) : TokenStorage {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): String? {
        val stored = preferences.getString(TOKEN_KEY, null) ?: return null
        return try {
            decrypt(stored)
        } catch (e: GeneralSecurityException) {
            // Pasa si la clave ya no existe (por ejemplo, datos restaurados de un respaldo en otro
            // dispositivo). El token no se puede recuperar: se borra y el usuario vuelve a iniciar sesión.
            clear()
            null
        } catch (e: IllegalArgumentException) {
            clear()
            null
        }
    }

    override fun save(token: String) {
        preferences.edit().putString(TOKEN_KEY, encrypt(token)).apply()
    }

    override fun clear() {
        preferences.edit().remove(TOKEN_KEY).apply()
    }

    // Se guarda el vector de inicialización (IV) delante del texto cifrado: hace falta para
    // descifrar y no es secreto. AES-GCM, además de cifrar, detecta si alguien modificó el dato.
    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.iv + cipher.doFinal(plainText.encodeToByteArray())
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, bytes, 0, IV_LENGTH_BYTES))
        return cipher.doFinal(bytes, IV_LENGTH_BYTES, bytes.size - IV_LENGTH_BYTES).decodeToString()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as SecretKey?)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "turnosapp-jwt"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
        const val PREFERENCES_NAME = "session"
        const val TOKEN_KEY = "jwt"
    }
}
