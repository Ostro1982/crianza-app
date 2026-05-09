package com.tudominio.crianza

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Backup local de la base SQLite con dos modos:
 *
 * - Modo legacy (`exportar`/`importar`): copia el archivo `.db` tal cual. Sin password,
 *   pero queda en disco del usuario. Mantenido para compatibilidad con backups previos.
 *
 * - Modo encriptado (`exportarConPassword`/`importarConPassword`): empaqueta el `.db`
 *   con AES-GCM 256, clave derivada del password vía PBKDF2 (100k iter, SHA-256).
 *   Formato del archivo:
 *     [magic(8) "NSTYBAK1"] [salt(16)] [iv(12)] [ciphertext+tag]
 *   Sin la password el archivo es indescifrable. Portable entre dispositivos.
 */
object BackupManager {

    const val DB_NAME = "crianza_database"
    private const val MAGIC = "NSTYBAK1"
    private const val PBKDF2_ITER = 100_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    fun nombreSugerido(): String {
        val fecha = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        return "nesty_backup_$fecha.crianzabak"
    }

    fun nombreSugeridoLegacy(): String {
        val fecha = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        return "crianza_backup_$fecha.db"
    }

    // ── Modo legacy (sin password, copia el .db) ─────────────────────────────

    suspend fun exportar(context: Context, destino: Uri): Result<Unit> = runCatching {
        AppDatabase.resetInstance()
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) error("No hay base de datos para exportar")
        context.contentResolver.openOutputStream(destino)?.use { out ->
            dbFile.inputStream().use { it.copyTo(out) }
        } ?: error("No se pudo abrir destino")
        Unit
    }

    suspend fun importar(context: Context, origen: Uri): Result<Unit> = runCatching {
        AppDatabase.resetInstance()
        val dbFile = context.getDatabasePath(DB_NAME)
        context.getDatabasePath("$DB_NAME-wal").delete()
        context.getDatabasePath("$DB_NAME-shm").delete()

        // Si el archivo arranca con MAGIC, exigir password (no implementado en este path).
        val bytes = context.contentResolver.openInputStream(origen)?.use { it.readBytes() }
            ?: error("No se pudo abrir origen")
        if (bytes.size >= MAGIC.length && String(bytes.copyOfRange(0, MAGIC.length)) == MAGIC) {
            error("Este backup está encriptado. Restaurá con password.")
        }
        dbFile.outputStream().use { it.write(bytes) }
        Unit
    }

    // ── Modo encriptado (con password del usuario) ───────────────────────────

    suspend fun exportarConPassword(context: Context, destino: Uri, password: String): Result<Unit> = runCatching {
        require(password.length >= 8) { "Mínimo 8 caracteres" }
        AppDatabase.resetInstance()
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) error("No hay base de datos para exportar")

        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val key = derivar(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

        val plain = dbFile.readBytes()
        val cipherText = cipher.doFinal(plain)

        val out = ByteArrayOutputStream()
        out.write(MAGIC.toByteArray(Charsets.US_ASCII))
        out.write(salt)
        out.write(iv)
        out.write(cipherText)

        context.contentResolver.openOutputStream(destino)?.use { it.write(out.toByteArray()) }
            ?: error("No se pudo abrir destino")
        Unit
    }

    suspend fun importarConPassword(context: Context, origen: Uri, password: String): Result<Unit> = runCatching {
        require(password.length >= 8) { "Mínimo 8 caracteres" }
        AppDatabase.resetInstance()

        val all = context.contentResolver.openInputStream(origen)?.use { it.readBytes() }
            ?: error("No se pudo abrir origen")

        val magicBytes = MAGIC.toByteArray(Charsets.US_ASCII)
        require(all.size > magicBytes.size + SALT_BYTES + IV_BYTES) { "Archivo corrupto o demasiado chico" }
        require(all.copyOfRange(0, magicBytes.size).contentEquals(magicBytes)) {
            "Este archivo no es un backup encriptado de Nesty"
        }

        val salt = all.copyOfRange(magicBytes.size, magicBytes.size + SALT_BYTES)
        val iv = all.copyOfRange(magicBytes.size + SALT_BYTES, magicBytes.size + SALT_BYTES + IV_BYTES)
        val cipherText = all.copyOfRange(magicBytes.size + SALT_BYTES + IV_BYTES, all.size)
        val key = derivar(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plain = try { cipher.doFinal(cipherText) }
        catch (e: Exception) { error("Password incorrecta o archivo dañado") }

        val dbFile = context.getDatabasePath(DB_NAME)
        context.getDatabasePath("$DB_NAME-wal").delete()
        context.getDatabasePath("$DB_NAME-shm").delete()
        dbFile.outputStream().use { it.write(plain) }
        Unit
    }

    private fun derivar(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITER, KEY_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
