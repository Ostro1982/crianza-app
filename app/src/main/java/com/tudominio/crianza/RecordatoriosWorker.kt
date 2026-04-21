package com.tudominio.crianza

import android.content.Context
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.*
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
        val configPrefs = applicationContext.getSharedPreferences("crianza_prefs", Context.MODE_PRIVATE)
        val minutosAntes = configPrefs.getInt("minutos_antes_evento", 0)
        val ahora = System.currentTimeMillis()

        if (config.notifEventos) {
            val todosEventos = db.eventoDao().obtenerTodosLosEventos()
            val eventosHoy = todosEventos.filter { it.fecha == hoy }
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

            // ── Recordatorio configurable antes del evento ───────────────────
            if (minutosAntes > 0) {
                val mañana = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    Date(ahora + 24 * 60 * 60 * 1000L)
                )
                todosEventos.filter { it.fecha == hoy || it.fecha == mañana }.forEach { evento ->
                    val horaStr = evento.horaInicio ?: return@forEach
                    val partes = horaStr.split(":").map { it.toIntOrNull() ?: 0 }
                    val cal = Calendar.getInstance().apply {
                        if (evento.fecha == mañana) add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, partes.getOrElse(0) { 0 })
                        set(Calendar.MINUTE, partes.getOrElse(1) { 0 })
                        set(Calendar.SECOND, 0)
                    }
                    val notifyAt = cal.timeInMillis - minutosAntes * 60 * 1000L
                    val workerInterval = 4 * 60 * 60 * 1000L
                    val key = "rec_${evento.id}_${evento.fecha}"
                    if (key !in notificadosHoy && notifyAt in (ahora - workerInterval)..ahora) {
                        val label = when {
                            minutosAntes < 60 -> "en $minutosAntes min"
                            minutosAntes == 60 -> "en 1 hora"
                            minutosAntes < 1440 -> "en ${minutosAntes / 60} horas"
                            else -> "mañana"
                        }
                        NotificacionHelper.notificar(
                            applicationContext,
                            "⏰ ${evento.titulo} — $label",
                            evento.ubicacion.ifBlank { evento.descripcion.ifBlank { "Sin detalles" } }
                        )
                        notificadosHoy.add(key)
                    }
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
