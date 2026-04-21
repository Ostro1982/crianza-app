package com.tudominio.crianza.widget

import android.content.Context
import com.tudominio.crianza.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

object WidgetDataHelper {

    data class EventoWidget(val labelDia: String, val titulo: String, val hora: String)
    data class GastoWidget(val descripcion: String, val monto: Double, val pagador: String)
    data class CompraWidget(val descripcion: String, val cantidad: String)
    data class PendienteWidget(val titulo: String, val asignadoA: String)

    suspend fun getEventosProximos(context: Context): List<EventoWidget> {
        val db = AppDatabase.getInstance(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoy = Calendar.getInstance()
        val hasta = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 6) }
        return db.eventoDao()
            .obtenerEventosDesde(sdf.format(hoy.time), sdf.format(hasta.time))
            .sortedWith(compareBy({ it.fecha }, { it.horaInicio ?: "99:99" }))
            .take(4)
            .map { EventoWidget(labelDia(it.fecha), it.titulo, it.horaInicio ?: "") }
    }

    suspend fun getGastos(context: Context): List<GastoWidget> {
        val db = AppDatabase.getInstance(context)
        return db.gastoDao().obtenerTodosLosGastos()
            .take(4)
            .map { GastoWidget(it.descripcion, it.monto, it.nombrePagador) }
    }

    suspend fun getComprasPendientes(context: Context): List<CompraWidget> {
        val db = AppDatabase.getInstance(context)
        return db.itemCompraDao().obtenerTodos()
            .filter { !it.comprado }
            .take(4)
            .map { CompraWidget(it.descripcion, it.cantidad) }
    }

    suspend fun getPendientes(context: Context): List<PendienteWidget> {
        val db = AppDatabase.getInstance(context)
        return db.pendienteDao().obtenerTodos()
            .filter { !it.completado }
            .take(4)
            .map { PendienteWidget(it.titulo, it.asignadoA) }
    }

    fun labelDia(fecha: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoyStr = sdf.format(Calendar.getInstance().time)
        val mananaStr = sdf.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time)
        return when (fecha) {
            hoyStr -> "Hoy"
            mananaStr -> "Mañana"
            else -> {
                val parsed = sdf.parse(fecha) ?: return fecha
                val dayFormat = SimpleDateFormat("EEE d", Locale("es", "AR"))
                dayFormat.format(parsed).replaceFirstChar { it.uppercase() }
            }
        }
    }
}
