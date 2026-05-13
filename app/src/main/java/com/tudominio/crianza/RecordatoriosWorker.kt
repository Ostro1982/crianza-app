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

            // ── Recordatorio por evento ──────────────────────────────────────
            // Lee override individual `recordatorio_evento_<id>` (minutos antes).
            // 0 o ausente = sin recordatorio anticipado.
            val mañana = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                Date(ahora + 24 * 60 * 60 * 1000L)
            )
            todosEventos.filter { it.fecha == hoy || it.fecha == mañana }.forEach { evento ->
                val minutosAntes = configPrefs.getInt("recordatorio_evento_${evento.id}", 0)
                if (minutosAntes <= 0) return@forEach
                val horaStr = evento.horaInicio ?: return@forEach
                val partes = horaStr.split(":").map { it.toIntOrNull() ?: 0 }
                val cal = Calendar.getInstance().apply {
                    if (evento.fecha == mañana) add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, partes.getOrElse(0) { 0 })
                    set(Calendar.MINUTE, partes.getOrElse(1) { 0 })
                    set(Calendar.SECOND, 0)
                }
                val notifyAt = cal.timeInMillis - minutosAntes * 60 * 1000L
                val workerInterval = 20 * 60 * 1000L
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

        // ── Generar gastos recurrentes ───────────────────────────────────
        // Para cada gasto con frecuenciaDias > 0, si pasó N días desde fecha del
        // último gasto con su mismo origen (o sí mismo si origen = ""), creo uno nuevo.
        val sdfDia = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todosGastos = db.gastoDao().obtenerTodosLosGastos()
        val plantillas = todosGastos.filter { it.frecuenciaDias > 0 && it.origenGastoRecurrente.isBlank() }
        val ahoraMsR = System.currentTimeMillis()
        plantillas.forEach { plantilla ->
            val ultimaInstancia = todosGastos
                .filter { it.id == plantilla.id || it.origenGastoRecurrente == plantilla.id }
                .maxByOrNull { it.fechaCompleta } ?: plantilla
            val periodMs = plantilla.frecuenciaDias * 24L * 60L * 60L * 1000L
            val diff = ahoraMsR - ultimaInstancia.fechaCompleta
            if (diff >= periodMs) {
                val nuevaFecha = sdfDia.format(java.util.Date(ultimaInstancia.fechaCompleta + periodMs))
                val nuevo = plantilla.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    fecha = nuevaFecha,
                    fechaCompleta = ultimaInstancia.fechaCompleta + periodMs,
                    origenGastoRecurrente = plantilla.id,
                    frecuenciaDias = 0  // las instancias no son plantillas
                )
                db.gastoDao().insertarGasto(nuevo)
                NotificacionHelper.notificar(
                    applicationContext,
                    "💸 Gasto recurrente creado",
                    "${plantilla.descripcion} · ${Moneda.formatear(plantilla.monto, MonedaConfig.actual)}"
                )
            }
        }

        // ── Reactivar pendientes recurrentes vencidos ───────────────────────
        val ahoraMs = System.currentTimeMillis()
        db.pendienteDao().obtenerTodos()
            .filter { it.completado && it.frecuenciaDias > 0 && it.fechaCompletado > 0 }
            .forEach { p ->
                val tiempoPasadoMs = ahoraMs - p.fechaCompletado
                val umbralMs = p.frecuenciaDias * 24L * 60L * 60L * 1000L
                if (tiempoPasadoMs >= umbralMs) {
                    db.pendienteDao().actualizar(p.copy(completado = false, fechaCompletado = 0L))
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

        // ── Recordatorio de custodia (mañana me toca) ───────────────────────
        // Si hay un registro de tiempo programado para mañana asignado a mi padre actual,
        // notifica una sola vez por día. Toggle: notifCustodia.
        if (config.notifCustodia) {
            val miIdPadre = configPrefs.getString("padre_actual_id", "") ?: ""
            if (miIdPadre.isNotBlank()) {
                val mañana = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    Date(ahoraMs + 24 * 60 * 60 * 1000L)
                )
                val registrosMañana = db.registroTiempoDao().obtenerTodosLosRegistros()
                    .filter { it.fecha == mañana && it.idPadre == miIdPadre }
                if (registrosMañana.isNotEmpty()) {
                    val key = "custodia_$mañana"
                    if (key !in notificadosHoy) {
                        val hijos = registrosMañana
                            .map { it.nombreHijo }
                            .distinct()
                            .joinToString(", ")
                        val rangos = registrosMañana
                            .joinToString(" • ") { "${it.horaInicio}–${it.horaFin}" }
                        NotificacionHelper.notificar(
                            applicationContext,
                            "👶 Mañana te toca: $hijos",
                            rangos
                        )
                        notificadosHoy.add(key)
                    }
                }
            }
        }

        // ── Cumpleaños hoy ────────────────────────────────────────────────
        // Compara mes-día (MM-DD) de fechaNacimiento de hijos y padres con hoy.
        val mmddHoy = if (hoy.length >= 10) hoy.substring(5, 10) else ""
        if (mmddHoy.isNotEmpty()) {
            val hijos = db.familiaDao().obtenerTodosLosHijos()
            hijos.forEach { hijo ->
                val fn = hijo.fechaNacimiento
                if (fn.length >= 10 && fn.substring(5, 10) == mmddHoy) {
                    val key = "cumple_${hijo.id}_$hoy"
                    if (key !in notificadosHoy) {
                        val edad = edadEnAnios(fn, hoy)
                        val titulo = if (edad != null) "🎉 ${hijo.nombre} cumple $edad" else "🎉 Cumpleaños de ${hijo.nombre}"
                        NotificacionHelper.notificar(applicationContext, titulo, "¡Feliz día!")
                        notificadosHoy.add(key)
                    }
                }
            }
            val padresList = db.familiaDao().obtenerTodosLosPadres()
            padresList.forEach { padre ->
                val fn = padre.fechaNacimiento
                if (fn.length >= 10 && fn.substring(5, 10) == mmddHoy) {
                    val key = "cumple_${padre.id}_$hoy"
                    if (key !in notificadosHoy) {
                        NotificacionHelper.notificar(
                            applicationContext,
                            "🎂 Cumpleaños de ${padre.nombre}",
                            "Hoy cumple años"
                        )
                        notificadosHoy.add(key)
                    }
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

    private fun edadEnAnios(fechaNac: String, hoy: String): Int? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val nac = sdf.parse(fechaNac) ?: return null
            val cur = sdf.parse(hoy) ?: return null
            val calNac = Calendar.getInstance().apply { time = nac }
            val calCur = Calendar.getInstance().apply { time = cur }
            var edad = calCur.get(Calendar.YEAR) - calNac.get(Calendar.YEAR)
            if (calCur.get(Calendar.DAY_OF_YEAR) < calNac.get(Calendar.DAY_OF_YEAR)) edad--
            if (edad < 0 || edad > 130) null else edad
        } catch (_: Exception) { null }
    }

    companion object {
        private const val WORK_NAME = "crianza_recordatorios"

        fun iniciar(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecordatoriosWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }
    }
}
