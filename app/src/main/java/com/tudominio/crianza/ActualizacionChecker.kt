package com.tudominio.crianza

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Verifica si hay una nueva versión en GitHub Releases y la descarga si corresponde.
 *
 * SETUP:
 * 1. Crear repo en github.com y subir el código
 * 2. Completar GITHUB_OWNER y GITHUB_REPO abajo
 * 3. Crear releases con tag "v{versionCode}" (ej: "v2", "v3") y subir el APK
 * 4. Cada vez que haya nueva versión: subir versionCode en build.gradle.kts y crear release
 */
object ActualizacionChecker {

    // ── Configurar con tu usuario y repo de GitHub ─────────────────────────────
    private const val GITHUB_OWNER = "Ostro1982"
    private const val GITHUB_REPO  = "crianza-app"
    // ───────────────────────────────────────────────────────────────────────────

    private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val APK_NAME = "crianza.apk"

    data class ActualizacionInfo(
        val versionCode: Int,
        val downloadUrl: String,
        val tagName: String
    )

    // downloadId del último download iniciado (para el BroadcastReceiver)
    var ultimoDownloadId: Long = -1L

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Verifica si hay una versión más nueva en GitHub Releases.
     * Retorna ActualizacionInfo si hay actualización, null si está al día o hay error.
     * Debe llamarse desde un hilo IO (withContext(Dispatchers.IO)).
     */
    suspend fun verificar(): ActualizacionInfo? = withContext(Dispatchers.IO) {
        try {
            if (GITHUB_OWNER == "TU_USUARIO") {
                Log.d("Actualizacion", "GitHub no configurado aún — saltear")
                return@withContext null
            }

            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = org.json.JSONObject(body)

            val tagName = json.optString("tag_name", "") // e.g. "v2"
            val remoteVersionCode = tagName.removePrefix("v").toIntOrNull() ?: return@withContext null

            if (remoteVersionCode <= BuildConfig.VERSION_CODE) {
                Log.d("Actualizacion", "App al día (v${BuildConfig.VERSION_CODE})")
                return@withContext null
            }

            val assets = json.optJSONArray("assets")
            if (assets == null || assets.length() == 0) {
                Log.w("Actualizacion", "Release v$remoteVersionCode sin APK adjunto")
                return@withContext null
            }

            // Tomar el primer asset .apk
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url")
                    break
                }
            }

            if (downloadUrl.isNullOrEmpty()) return@withContext null

            Log.d("Actualizacion", "Nueva versión disponible: $tagName")
            ActualizacionInfo(remoteVersionCode, downloadUrl, tagName)

        } catch (e: Exception) {
            Log.e("Actualizacion", "Error al verificar actualizaciones", e)
            null
        }
    }

    /**
     * Descarga el APK usando DownloadManager.
     * Retorna el downloadId para rastrear el progreso.
     * DownloadManager muestra una notificación de descarga automáticamente.
     */
    fun descargar(context: Context, info: ActualizacionInfo): Long {
        return try {
            val destino = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val archivo = java.io.File(destino, APK_NAME)
            if (archivo.exists()) archivo.delete()

            val request = DownloadManager.Request(android.net.Uri.parse(info.downloadUrl)).apply {
                setTitle("Crianza — Actualizando a ${info.tagName}")
                setDescription("Descargando nueva versión...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_NAME)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)
            ultimoDownloadId = downloadId
            Log.d("Actualizacion", "Descarga iniciada, id=$downloadId")
            downloadId
        } catch (e: Exception) {
            Log.e("Actualizacion", "Error al iniciar descarga", e)
            -1L
        }
    }

    /**
     * Instala el APK descargado usando FileProvider.
     * Llamar desde el BroadcastReceiver de DownloadManager.
     */
    fun instalar(context: Context) {
        try {
            val destino = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val archivo = java.io.File(destino, APK_NAME)
            if (!archivo.exists()) return

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivo
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("Actualizacion", "Error al instalar APK", e)
        }
    }
}
