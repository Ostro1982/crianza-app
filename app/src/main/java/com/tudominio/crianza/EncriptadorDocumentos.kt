package com.tudominio.crianza

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

object EncriptadorDocumentos {

    private const val KEY_ALIAS = "crianza_boveda_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128

    private fun obtenerOCrearClave(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGen.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGen.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Encripta texto y devuelve (contenidoBase64, ivBase64).
     */
    fun encriptar(texto: String): Pair<String, String> {
        val clave = obtenerOCrearClave()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, clave)

        val contenido = cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(contenido, Base64.DEFAULT) to
                Base64.encodeToString(cipher.iv, Base64.DEFAULT)
    }

    /**
     * Desencripta contenido previamente encriptado con encriptar().
     * Devuelve null si falla (clave inválida, datos corruptos, etc.)
     */
    fun desencriptar(contenidoBase64: String, ivBase64: String): String? {
        return try {
            val clave = obtenerOCrearClave()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)
            cipher.init(Cipher.DECRYPT_MODE, clave, GCMParameterSpec(TAG_LENGTH, iv))
            val bytes = cipher.doFinal(Base64.decode(contenidoBase64, Base64.DEFAULT))
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
