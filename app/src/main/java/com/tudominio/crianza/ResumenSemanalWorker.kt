package com.tudominio.crianza

import android.content.Context
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Corre diario. Si es domingo 20hs, manda notificación con resumen de la semana:
 * gastos totales, horas por padre, pendientes completados.
 */
class ResumenSemanalWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cal = Calendar.getInstance()
        val esDomingo = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val hora = cal.get(Calendar.HOUR_OF_DAY)
        if (!esDomingo || hora < 19) return Result.success()

        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val prefs = applicationContext.getSharedPreferences("recordatorios", Context.MODE_PRIVATE)
        if (prefs.getString("ultimo_resumen_semanal", "") == hoy) return Result.success()

        val db = AppDatabase.getInstance(applicationContext)
        val fechaInicio = run {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -6)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)
        }

        val gastos = db.gastoDao().obtenerTodosLosGastos()
            .filter { it.fecha in fechaInicio..hoy }
        val totalGastos = gastos.sumOf { it.monto }.toLong()

        val registros = db.registroTiempoDao().obtenerTodosLosRegistros()
            .filter { it.fecha in fechaInicio..hoy }
        val horasPorPadre = registros.groupBy { it.nombrePadre }
            .mapValues { (_, regs) ->
                regs.sumOf { r ->
                    try {
                        val (hiH, hiM) = r.horaInicio.split(":").map { it.toInt() }
                        val (hfH, hfM) = r.horaFin.split(":").map { it.toInt() }
                        ((hfH * 60 + hfM) - (hiH * 60 + hiM)).coerceAtLeast(0)
                    } catch (_: Exception) { 0 }
                } / 60.0
            }

        val pendientesCompletados = db.pendienteDao().obtenerTodos()
            .count { it.completado }

        val cuerpo = buildString {
            append("Gastos: $$totalGastos\n")
            horasPorPadre.forEach { (padre, horas) ->
                append("$padre: ${"%.1f".format(horas)}h\n")
            }
            append("Tareas completadas: $pendientesCompletados")
        }

        NotificacionHelper.notificar(
            applicationContext,
            "📊 Resumen de la semana",
            cuerpo.trim()
        )

        prefs.edit().putString("ultimo_resumen_semanal", hoy).apply()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "crianza_resumen_semanal"

        fun iniciar(context: Context) {
            val request = PeriodicWorkRequestBuilder<ResumenSemanalWorker>(6, TimeUnit.HOURS)
                .setInitialDelay(10, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
