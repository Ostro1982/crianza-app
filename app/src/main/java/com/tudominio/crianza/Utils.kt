@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tudominio.crianza.ui.theme.Red40
import java.text.SimpleDateFormat
import java.util.*

enum class Periodo {
    SEMANA, MES, ANIO, TODO
}

@Composable
fun PeriodoButton(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (seleccionado)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.width(75.dp)
    ) {
        Text(text = texto)
    }
}

fun parseFecha(fechaStr: String): Date {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        sdf.parse(fechaStr) ?: Date()
    } catch (e: Exception) {
        Date()
    }
}

fun esMismaSemana(fecha1: Date, fecha2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = fecha1 }
    val cal2 = Calendar.getInstance().apply { time = fecha2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
}

fun esMismoMes(fecha1: Date, fecha2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = fecha1 }
    val cal2 = Calendar.getInstance().apply { time = fecha2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
}

fun esMismoAnio(fecha1: Date, fecha2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = fecha1 }
    val cal2 = Calendar.getInstance().apply { time = fecha2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}

fun calcularHorasPorPadre(registros: List<RegistroTiempo>): Map<String, Double> {
    val horasPorPadre = mutableMapOf<String, Double>()
    val gruposContados = mutableSetOf<String>()

    registros.forEach { registro ->
        val horaInicio = registro.horaInicio.split(":").map { it.toIntOrNull() ?: 0 }
        val horaFin = registro.horaFin.split(":").map { it.toIntOrNull() ?: 0 }

        if (horaInicio.size >= 2 && horaFin.size >= 2) {
            val minutosInicio = horaInicio[0] * 60 + horaInicio[1]
            val minutosFin = horaFin[0] * 60 + horaFin[1]
            val horas = (minutosFin - minutosInicio) / 60.0

            if (registro.esTodosLosHijos) {
                // Registros del mismo bloque compartido: contar una sola vez por padre
                val clave = "${registro.idPadre}_${registro.fecha}_${registro.horaInicio}_${registro.horaFin}"
                if (clave !in gruposContados) {
                    gruposContados.add(clave)
                    horasPorPadre[registro.idPadre] = horasPorPadre.getOrDefault(registro.idPadre, 0.0) + horas
                }
            } else {
                horasPorPadre[registro.idPadre] = horasPorPadre.getOrDefault(registro.idPadre, 0.0) + horas
            }
        }
    }

    return horasPorPadre
}

fun obtenerFechaActual(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

fun obtenerHoraActual(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

/** Normaliza "8" → "08:00", "8:30" → "08:30", "14:5" → "14:05" */
fun normalizarHora(hora: String): String {
    val limpia = hora.trim()
    return if (limpia.contains(":")) {
        val partes = limpia.split(":")
        "%02d:%02d".format(
            partes[0].toIntOrNull() ?: 0,
            partes.getOrNull(1)?.toIntOrNull() ?: 0
        )
    } else {
        "%02d:00".format(limpia.toIntOrNull() ?: 0)
    }
}

/** Calcula la edad en años a partir de "YYYY-MM-DD". Retorna null si la fecha es inválida. */
fun calcularEdad(fechaNacimiento: String): Int? {
    if (fechaNacimiento.isBlank()) return null
    return try {
        val partes = fechaNacimiento.split("-")
        val anoNac = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val cal = Calendar.getInstance()
        val anoActual = cal.get(Calendar.YEAR)
        var edad = anoActual - anoNac
        val mesNac = partes.getOrNull(1)?.toIntOrNull() ?: 1
        val diaNac = partes.getOrNull(2)?.toIntOrNull() ?: 1
        val mesActual = cal.get(Calendar.MONTH) + 1
        val diaActual = cal.get(Calendar.DAY_OF_MONTH)
        if (mesActual < mesNac || (mesActual == mesNac && diaActual < diaNac)) edad--
        if (edad < 0) null else edad
    } catch (e: Exception) { null }
}

fun textoPeriodo(periodo: Periodo): String {
    return when (periodo) {
        Periodo.SEMANA -> "Esta semana"
        Periodo.MES -> "Este mes"
        Periodo.ANIO -> "Este año"
        Periodo.TODO -> "Todo el historial"
    }
}

@Composable
fun SwipeParaBorrar(
    onEliminar: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onEliminar()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Red40)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        content()
    }
}
