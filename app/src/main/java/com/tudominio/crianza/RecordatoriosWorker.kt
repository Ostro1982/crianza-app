package com.tudominio.crianza

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Worker que corre cada ~4 horas y envía notificaciones push para:
 * - Eventos del día actual
 * - Pendientes con fecha límite hoy o vencidos
 *
 * Usa SharedPreferences para no repetir la misma notificación el mismo día.
 */
class RecordatoriosWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val config = db.configuracionIntegracionDao().obtener() ?: return Result.success()
        val hoy = obtenerFechaActual()

        val prefs = applicationContext.getSharedPreferences("recordatorios", Context.MODE_PRIVATE)
        val ultimoDia = prefs.getString("ultimo_dia", "") ?: ""
        val notificadosHoy = if (ultimoDia == hoy) {
            prefs.getStringSet("notificados", emptySet())?.toMutableSet() ?: mutableSetOf()
        } else {
            // Nuevo día: limpiar
            prefs.edit().putString("ultimo_dia", hoy).putStringSet("notificados", emptySet()).apply()
            mutableSetOf()
        }

        // ── Eventos del día ──────────────────────────────────────────────────
        if (config.notifEventos) {
            val eventosHoy = db.eventoDao().obtenerTodosLosEventos().filter { it.fecha == hoy }
            eventosHoy.forEach { evento ->
                val key = "evento_${evento.id}"
                if (key !in notificadosHoy) {
                    val hora = evento.horaInicio?.let { "$it — " } ?: ""
                    NotificacionHelper.notificar(
                        applicationContext,
                        "📅 Hoy: ${evento.titulo}",
                        "$hora${evento.ubicacion.ifBlank { "Sin ubicación" }}"
                    )
                    notificadosHoy.add(key)
                }
            }
        }

        // ── Pendientes vencidos o del día ────────────────────────────────────
        val pendientes = db.pendienteDao().obtenerPendientes() // solo no completados
        pendientes.forEach { pend ->
            if (pend.fechaLimite.isNotBlank() && pend.fechaLimite <= hoy) {
                val key = "pend_${pend.id}"
                if (key !in notificadosHoy) {
                    val vencido = pend.fechaLimite < hoy
                    NotificacionHelper.notificar(
                        applicationContext,
                        if (vencido) "⚠️ Pendiente vencido" else "📋 Pendiente para hoy",
                        pend.titulo + if (pend.asignadoA.isNotBlank()) " (${pend.asignadoA})" else ""
                    )
                    notificadosHoy.add(key)
                }
            }
        }

        // ── Compras sin comprar hace más de 3 días ──────────────────────────
        val tresDiasMs = 3 * 24 * 60 * 60 * 1000L
        val comprasViejas = db.itemCompraDao().obtenerCompartidos()
            .filter { !it.comprado && (System.currentTimeMillis() - it.fechaCompleta) > tresDiasMs }
        if (comprasViejas.isNotEmpty()) {
            val key = "compras_recordatorio_$hoy"
            if (key !in notificadosHoy) {
                NotificacionHelper.notificar(
                    applicationContext,
                    "🛒 ${comprasViejas.size} compra${if (comprasViejas.size > 1) "s" else ""} pendiente${if (comprasViejas.size > 1) "s" else ""}",
                    comprasViejas.take(3).joinToString(", ") { it.descripcion }
                )
                notificadosHoy.add(key)
            }
        }

        // Guardar estado
        prefs.edit().putStringSet("notificados", notificadosHoy).apply()

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "crianza_recordatorios"

        fun iniciar(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecordatoriosWorker>(4, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
