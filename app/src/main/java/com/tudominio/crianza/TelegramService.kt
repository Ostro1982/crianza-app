package com.tudominio.crianza

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente del Telegram Bot API.
 *
 * Comandos soportados (enviados por cualquier padre desde su chat configurado):
 *   /gasto [monto] [descripcion]         → agrega un gasto
 *   /tiempo [hijo] [inicio HH:MM] [fin HH:MM]  → agrega registro de tiempo (hoy)
 *   /evento [titulo] [fecha YYYY-MM-DD]  → agrega evento al calendario
 *   /recuerdo [titulo]: [descripcion]    → agrega un recuerdo
 *   /aceptar                             → acepta la compensación pendiente más reciente
 *   /estado                              → responde con el resumen de compensaciones pendientes
 */
class TelegramService(private val config: ConfiguracionIntegracion) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.telegram.org/bot${config.telegramBotToken}"

    data class MensajeParsado(
        val chatId: String,
        val updateId: Long,
        val texto: String,
        val idPadre: String // "padre1" o "padre2"
    )

    data class ComandoParsado(
        val tipo: String, // "gasto", "tiempo", "evento", "recuerdo", "aceptar", "estado"
        val datos: Map<String, String>,
        val mensajeOrigen: MensajeParsado
    )

    fun obtenerNuevosMensajes(ultimoUpdateId: Long): List<MensajeParsado> {
        return try {
            val url = "$baseUrl/getUpdates?offset=${ultimoUpdateId + 1}&timeout=5"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)

            if (!json.getBoolean("ok")) return emptyList()

            val updates = json.getJSONArray("result")
            val mensajes = mutableListOf<MensajeParsado>()

            for (i in 0 until updates.length()) {
                val update = updates.getJSONObject(i)
                val updateId = update.getLong("update_id")
                val message = update.optJSONObject("message") ?: continue
                val text = message.optString("text", "").trim()
                if (text.isEmpty() || !text.startsWith("/")) continue

                val chatId = message.getJSONObject("chat").getLong("id").toString()
                val idPadre = when (chatId) {
                    config.telegramChatIdPadre1 -> "padre1"
                    config.telegramChatIdPadre2 -> "padre2"
                    else -> continue // ignora chats no configurados
                }

                mensajes.add(MensajeParsado(chatId, updateId, text, idPadre))
            }
            mensajes
        } catch (e: Exception) {
            Log.e("TelegramService", "Error al obtener mensajes", e)
            emptyList()
        }
    }

    fun parsearComando(mensaje: MensajeParsado): ComandoParsado? {
        val partes = mensaje.texto.trim().split("\\s+".toRegex())
        val comando = partes.firstOrNull()?.lowercase() ?: return null

        return when (comando) {
            "/gasto" -> {
                if (partes.size < 3) return null
                val monto = partes[1].toDoubleOrNull() ?: return null
                val descripcion = partes.drop(2).joinToString(" ")
                ComandoParsado("gasto", mapOf("monto" to monto.toString(), "descripcion" to descripcion), mensaje)
            }
            "/tiempo" -> {
                // /tiempo [hijo] [inicio] [fin]   — acepta "8", "8:30", "08:00"
                if (partes.size < 4) return null
                val hijo = partes[1]
                val inicio = normalizarHora(partes[2])
                val fin = normalizarHora(partes[3])
                ComandoParsado("tiempo", mapOf("hijo" to hijo, "inicio" to inicio, "fin" to fin), mensaje)
            }
            "/evento" -> {
                if (partes.size < 3) return null
                val fecha = partes.last()
                val titulo = partes.drop(1).dropLast(1).joinToString(" ")
                ComandoParsado("evento", mapOf("titulo" to titulo, "fecha" to fecha), mensaje)
            }
            "/recuerdo" -> {
                val texto = partes.drop(1).joinToString(" ")
                val separador = texto.indexOf(":")
                val titulo = if (separador > 0) texto.substring(0, separador).trim() else texto
                val desc = if (separador > 0) texto.substring(separador + 1).trim() else ""
                ComandoParsado("recuerdo", mapOf("titulo" to titulo, "descripcion" to desc), mensaje)
            }
            "/compra" -> {
                val texto = partes.drop(1).joinToString(" ")
                val partesCat = texto.split(" #")
                val desc = partesCat.first().trim()
                val cat = partesCat.getOrNull(1)?.trim() ?: ""
                ComandoParsado("compra", mapOf("descripcion" to desc, "categoria" to cat), mensaje)
            }
            "/lista" -> ComandoParsado("lista", emptyMap(), mensaje)
            "/aceptar" -> {
                val idEvento = partes.getOrNull(1)
                ComandoParsado("aceptar", if (idEvento != null) mapOf("idEvento" to idEvento) else emptyMap(), mensaje)
            }
            "/rechazar" -> {
                val idEvento = partes.getOrNull(1)
                ComandoParsado("rechazar", if (idEvento != null) mapOf("idEvento" to idEvento) else emptyMap(), mensaje)
            }
            "/estado" -> ComandoParsado("estado", emptyMap(), mensaje)
            else -> null
        }
    }

    fun enviarMensaje(chatId: String, texto: String) {
        try {
            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", texto)
                put("parse_mode", "HTML")
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/sendMessage")
                .post(body)
                .build()
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("TelegramService", "Error al enviar mensaje", e)
        }
    }

    fun obtenerUltimoUpdateId(mensajes: List<MensajeParsado>): Long {
        return mensajes.maxOfOrNull { it.updateId } ?: config.ultimoUpdateIdTelegram
    }
}
