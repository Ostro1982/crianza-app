package com.tudominio.crianza

import android.content.Context
import android.net.Uri
import androidx.room.Room
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    const val DB_NAME = "crianza_database"

    fun nombreSugerido(): String {
        val fecha = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        return "crianza_backup_$fecha.db"
    }

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

        context.contentResolver.openInputStream(origen)?.use { input ->
            dbFile.outputStream().use { out -> input.copyTo(out) }
        } ?: error("No se pudo abrir origen")
        Unit
    }
}
