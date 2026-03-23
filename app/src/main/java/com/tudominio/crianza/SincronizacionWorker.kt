package com.tudominio.crianza

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.UUID
import java.util.concurrent.TimeUnit



/**
 * WorkManager que corre en background cada 15 minutos.
 * Revisa Telegram y Email según la configuración, y aplica los comandos a la base de datos.
 */
class SincronizacionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val config = db.configuracionIntegracionDao().obtener() ?: return Result.success()
        val padres = db.familiaDao().obtenerTodosLosPadres()
        val hijos = db.familiaDao().obtenerTodosLosHijos()

        if (padres.size < 2) return Result.success()

        var nuevoUltimoUpdateId = config.ultimoUpdateIdTelegram
        var nuevaUltimaRevision = config.ultimaRevisionEmail

        // ── Telegram ─────────────────────────────────────────────────────────
        if (config.habilitarTelegram && config.telegramBotToken.isNotEmpty()) {
            try {
                val telegramService = TelegramService(config)
                val mensajes = telegramService.obtenerNuevosMensajes(config.ultimoUpdateIdTelegram)

                for (mensaje in mensajes) {
                    val comando = telegramService.parsearComando(mensaje) ?: continue
                    val respuesta = ProcesadorComandos.procesar(comando, padres, hijos, db, config)
                    val chatIdDestino = when (mensaje.idPadre) {
                        "padre1" -> config.telegramChatIdPadre1
                        else -> config.telegramChatIdPadre2
                    }
                    if (chatIdDestino.isNotEmpty()) {
                        telegramService.enviarMensaje(chatIdDestino, respuesta)
                    }
                    // Notificación push si el comando fue exitoso
                    if (respuesta.startsWith("✅")) {
                        val ctx = applicationContext
                        when (comando.tipo) {
                            "evento" -> if (config.notifEventos)
                                NotificacionHelper.notificarEvento(ctx,
                                    comando.datos["titulo"] ?: "Nuevo evento",
                                    comando.datos["fecha"] ?: "")
                            "gasto" -> if (config.notifGastos)
                                NotificacionHelper.notificarGasto(ctx,
                                    comando.datos["descripcion"] ?: "Gasto",
                                    comando.datos["monto"]?.toDoubleOrNull() ?: 0.0)
                            "compensacion" -> if (config.notifCompensaciones)
                                NotificacionHelper.notificar(ctx,
                                    "⚖️ Nueva compensación",
                                    "Monto: ${"%.2f".format(comando.datos["monto"]?.toDoubleOrNull() ?: 0.0)}")
                            "compra" -> if (config.notifCompras)
                                NotificacionHelper.notificarItemCompra(ctx,
                                    comando.datos["descripcion"] ?: "Nuevo ítem")
                        }
                    }
                }

                nuevoUltimoUpdateId = telegramService.obtenerUltimoUpdateId(mensajes)
                    .coerceAtLeast(config.ultimoUpdateIdTelegram)
            } catch (e: Exception) {
                Log.e("SincronizacionWorker", "Error Telegram", e)
            }
        }

        // ── Email ─────────────────────────────────────────────────────────────
        if (config.habilitarEmail && config.emailUser.isNotEmpty()) {
            try {
                val filtros = db.filtroEmailDao().obtenerActivos()
                val emailService = EmailService(config)
                val emails = emailService.obtenerEmailsNuevos(filtros, config.ultimaRevisionEmail)

                for (email in emails) {
                    // ── Invitación de calendario (iCal) ──────────────────────
                    if (email.contenidoIcal != null) {
                        procesarInvitacionCalendario(email.contenidoIcal, db, config)
                        continue
                    }

                    // ── Comandos de texto (puede haber varios) ────────────────
                    val comandosTexto = emailService.extraerComandos(email)
                    for (textoComando in comandosTexto) {
                        val mensajeFictico = TelegramService.MensajeParsado(
                            chatId = "",
                            updateId = 0L,
                            texto = textoComando,
                            idPadre = "padre1"
                        )
                        val comando = TelegramService(config).parsearComando(mensajeFictico) ?: continue
                        val respuesta = ProcesadorComandos.procesar(comando, padres, hijos, db, config)
                        if (respuesta.startsWith("✅")) {
                            val ctx = applicationContext
                            when (comando.tipo) {
                                "evento" -> if (config.notifEventos)
                                    NotificacionHelper.notificarEvento(ctx,
                                        comando.datos["titulo"] ?: "Nuevo evento",
                                        comando.datos["fecha"] ?: "")
                                "gasto" -> if (config.notifGastos)
                                    NotificacionHelper.notificarGasto(ctx,
                                        comando.datos["descripcion"] ?: "Gasto",
                                        comando.datos["monto"]?.toDoubleOrNull() ?: 0.0)
                            }
                        }
                    }
                }

                nuevaUltimaRevision = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e("SincronizacionWorker", "Error Email", e)
            }
        }

        // Guardar el estado del offset de Telegram y la última revisión
        db.configuracionIntegracionDao().guardar(
            config.copy(
                ultimoUpdateIdTelegram = nuevoUltimoUpdateId,
                ultimaRevisionEmail = nuevaUltimaRevision
            )
        )

        return Result.success()
    }

    private suspend fun procesarInvitacionCalendario(
        contenidoIcal: String,
        db: AppDatabase,
        config: ConfiguracionIntegracion
    ) {
        val eventoIcal = ICalParser.parsear(contenidoIcal) ?: return
        // Dedup: no insertar si ya existe un evento con el mismo título y fecha
        if (db.eventoDao().contarDuplicado(eventoIcal.titulo, eventoIcal.fecha) > 0) return
        val evento = Evento(
            id = UUID.randomUUID().toString(),
            titulo = eventoIcal.titulo,
            descripcion = eventoIcal.descripcion,
            fecha = eventoIcal.fecha,
            horaInicio = eventoIcal.horaInicio,
            horaFin = eventoIcal.horaFin,
            ubicacion = eventoIcal.ubicacion,
            origenEmail = true,
            fechaCompleta = System.currentTimeMillis()
        )
        db.eventoDao().insertarEvento(evento)

        // Notificación push local
        if (config.notifEventos) {
            NotificacionHelper.notificarEvento(applicationContext, eventoIcal.titulo, eventoIcal.fecha)
        }

        // Notificar a ambos padres via Telegram si está configurado
        if (config.habilitarTelegram && config.telegramBotToken.isNotEmpty()) {
            val telegram = TelegramService(config)
            val ubicStr = if (eventoIcal.ubicacion.isNotEmpty()) "\n📍 ${eventoIcal.ubicacion}" else ""
            val horaStr = if (eventoIcal.horaInicio != null) " ${eventoIcal.horaInicio}" else ""
            val mensaje = "📅 <b>Nueva invitación agregada al calendario</b>\n" +
                "${eventoIcal.titulo}\n" +
                "📆 ${eventoIcal.fecha}$horaStr$ubicStr\n\n" +
                "Respondé /aceptar o /rechazar [${evento.id.take(8)}] para indicar quién va."
            listOf(config.telegramChatIdPadre1, config.telegramChatIdPadre2)
                .filter { it.isNotEmpty() }
                .forEach { chatId -> telegram.enviarMensaje(chatId, mensaje) }
        }
    }

    companion object {
        private const val WORK_NAME = "crianza_sincronizacion"

        fun iniciar(context: Context) {
            val request = PeriodicWorkRequestBuilder<SincronizacionWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun detener(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
