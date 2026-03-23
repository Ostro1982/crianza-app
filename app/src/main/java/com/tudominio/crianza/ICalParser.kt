package com.tudominio.crianza

import java.text.SimpleDateFormat
import java.util.*

/**
 * Parser simple de archivos iCalendar (.ics).
 * Extrae el primer VEVENT del contenido iCal y lo convierte en un Evento.
 */
object ICalParser {

    data class EventoIcal(
        val titulo: String,
        val fecha: String,       // YYYY-MM-DD
        val horaInicio: String?, // HH:MM
        val horaFin: String?,    // HH:MM
        val descripcion: String,
        val ubicacion: String
    )

    fun parsear(contenido: String): EventoIcal? {
        val lineas = unfold(contenido)

        var dentroDeVEvent = false
        val campos = mutableMapOf<String, String>()

        for (linea in lineas) {
            when {
                linea.trim() == "BEGIN:VEVENT" -> dentroDeVEvent = true
                linea.trim() == "END:VEVENT" -> {
                    if (dentroDeVEvent) break
                }
                dentroDeVEvent -> {
                    val separador = linea.indexOf(':')
                    if (separador > 0) {
                        val clave = linea.substring(0, separador).uppercase().trim()
                        val valor = linea.substring(separador + 1).trim()
                        // DTSTART puede venir como DTSTART;TZID=...:valor
                        val claveBase = clave.substringBefore(';')
                        campos[claveBase] = valor
                    }
                }
            }
        }

        val summary = campos["SUMMARY"]?.unescape() ?: return null

        val (fecha, horaInicio) = parsearFechaHora(campos["DTSTART"])
        val (_, horaFin) = parsearFechaHora(campos["DTEND"])

        return EventoIcal(
            titulo = summary,
            fecha = fecha ?: return null,
            horaInicio = horaInicio,
            horaFin = horaFin,
            descripcion = campos["DESCRIPTION"]?.unescape() ?: "",
            ubicacion = campos["LOCATION"]?.unescape() ?: ""
        )
    }

    /**
     * iCal usa "line folding": líneas largas se parten con CRLF + espacio/tab.
     * Esta función las une de nuevo.
     */
    private fun unfold(contenido: String): List<String> {
        val resultado = mutableListOf<String>()
        val buffer = StringBuilder()
        for (linea in contenido.lines()) {
            if (linea.startsWith(" ") || linea.startsWith("\t")) {
                buffer.append(linea.trimStart())
            } else {
                if (buffer.isNotEmpty()) resultado.add(buffer.toString())
                buffer.clear()
                buffer.append(linea)
            }
        }
        if (buffer.isNotEmpty()) resultado.add(buffer.toString())
        return resultado
    }

    /**
     * Parsea DTSTART/DTEND.
     * Formatos posibles:
     *   20260325T100000Z  → fecha + hora UTC
     *   20260325T100000   → fecha + hora local
     *   20260325          → solo fecha
     */
    private fun parsearFechaHora(valor: String?): Pair<String?, String?> {
        if (valor.isNullOrBlank()) return null to null

        return try {
            when {
                valor.length >= 15 && valor.contains('T') -> {
                    val isUtc = valor.endsWith('Z')
                    val fmt = if (isUtc)
                        SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                    else
                        SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
                    val date = fmt.parse(valor) ?: return null to null
                    val cal = Calendar.getInstance().apply { time = date }
                    val fechaStr = "%04d-%02d-%02d".format(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    val horaStr = "%02d:%02d".format(
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE)
                    )
                    fechaStr to horaStr
                }
                valor.length == 8 -> {
                    val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
                    val date = fmt.parse(valor) ?: return null to null
                    val cal = Calendar.getInstance().apply { time = date }
                    val fechaStr = "%04d-%02d-%02d".format(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    fechaStr to null
                }
                else -> null to null
            }
        } catch (e: Exception) {
            null to null
        }
    }

    private fun String.unescape(): String =
        this.replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
}
