package com.tudominio.crianza

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sincronización con el calendario del dispositivo vía CalendarContract.
 * Funciona con Google Calendar, Samsung Calendar, y cualquier calendario
 * instalado en el dispositivo. No requiere API keys de Google Cloud.
 *
 * PERMISOS REQUERIDOS (en AndroidManifest):
 *   READ_CALENDAR
 *   WRITE_CALENDAR
 *
 * El usuario debe otorgar estos permisos en runtime.
 */
data class CalendarioDispositivo(val id: Long, val nombre: String, val cuenta: String)

object GoogleCalendarService {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Devuelve todos los calendarios disponibles en el dispositivo.
     */
    fun obtenerCalendarios(context: Context): List<CalendarioDispositivo> {
        val lista = mutableListOf<CalendarioDispositivo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME
        )
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, projection, null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val nombre = cursor.getString(1) ?: "Calendario"
                    val cuenta = cursor.getString(2) ?: ""
                    lista.add(CalendarioDispositivo(id, nombre, cuenta))
                }
            }
        } catch (e: SecurityException) {
            Log.e("GoogleCalendar", "Sin permiso de calendario", e)
        }
        return lista
    }

    /**
     * Obtiene el ID del primer calendario disponible (preferiblemente Google).
     */
    fun obtenerIdCalendario(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val cursor: Cursor? = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val isPrimary = it.getInt(2) == 1
                if (isPrimary) return id
            }
            if (it.moveToFirst()) return it.getLong(0)
        }
        return null
    }

    /**
     * Importa eventos del calendario del dispositivo hacia la app.
     * Solo trae eventos de los próximos 90 días.
     * Si calendarId != null, filtra por ese calendario específico.
     */
    fun importarEventos(context: Context, calendarId: Long? = null): List<Evento> {
        val eventos = mutableListOf<Evento>()
        val ahora = System.currentTimeMillis()
        val en90dias = ahora + (90L * 24 * 60 * 60 * 1000)

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )
        val selBase = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selection = if (calendarId != null)
            "$selBase AND ${CalendarContract.Events.CALENDAR_ID} = $calendarId"
        else selBase
        val selArgs = arrayOf(ahora.toString(), en90dias.toString())

        val cursor: Cursor? = try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection, selection, selArgs,
                CalendarContract.Events.DTSTART + " ASC"
            )
        } catch (e: SecurityException) {
            Log.e("GoogleCalendar", "Permiso de calendario no otorgado", e)
            return emptyList()
        }

        cursor?.use {
            while (it.moveToNext()) {
                val titulo = it.getString(1) ?: continue
                val dtStart = it.getLong(3)
                val dtEnd = it.getLong(4)
                val ubicacion = it.getString(5) ?: ""
                val descripcion = it.getString(2) ?: ""

                val calStart = Calendar.getInstance().apply { timeInMillis = dtStart }
                val fecha = sdf.format(calStart.time)
                val horaInicio = sdfHora.format(calStart.time)

                val calEnd = Calendar.getInstance().apply { timeInMillis = dtEnd }
                val horaFin = sdfHora.format(calEnd.time)

                eventos.add(
                    Evento(
                        id = UUID.randomUUID().toString(),
                        titulo = titulo,
                        descripcion = descripcion,
                        fecha = fecha,
                        horaInicio = horaInicio,
                        horaFin = horaFin,
                        ubicacion = ubicacion,
                        origenEmail = false,
                        fechaCompleta = System.currentTimeMillis()
                    )
                )
            }
        }
        return eventos
    }

    /**
     * Exporta un Evento de la app al calendario del dispositivo.
     * Devuelve el ID del evento creado en el calendario, o null si falla.
     */
    fun exportarEvento(context: Context, evento: Evento): Long? {
        val calendarId = obtenerIdCalendario(context) ?: return null

        return try {
            val fecha = sdf.parse(evento.fecha) ?: return null
            val cal = Calendar.getInstance().apply { time = fecha }

            val inicio: Long
            val fin: Long

            if (evento.horaInicio != null) {
                val partsI = evento.horaInicio.split(":")
                cal.set(Calendar.HOUR_OF_DAY, partsI[0].toInt())
                cal.set(Calendar.MINUTE, partsI.getOrNull(1)?.toInt() ?: 0)
                inicio = cal.timeInMillis

                if (evento.horaFin != null) {
                    val partsF = evento.horaFin.split(":")
                    cal.set(Calendar.HOUR_OF_DAY, partsF[0].toInt())
                    cal.set(Calendar.MINUTE, partsF.getOrNull(1)?.toInt() ?: 0)
                    fin = cal.timeInMillis
                } else {
                    fin = inicio + 60 * 60 * 1000 // 1 hora por defecto
                }
            } else {
                // Evento de día completo
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                inicio = cal.timeInMillis
                fin = inicio + 24 * 60 * 60 * 1000
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, evento.titulo)
                put(CalendarContract.Events.DESCRIPTION, evento.descripcion)
                put(CalendarContract.Events.EVENT_LOCATION, evento.ubicacion)
                put(CalendarContract.Events.DTSTART, inicio)
                put(CalendarContract.Events.DTEND, fin)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri: Uri? = context.contentResolver.insert(
                CalendarContract.Events.CONTENT_URI, values
            )
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            Log.e("GoogleCalendar", "Error al exportar evento", e)
            null
        }
    }
}
