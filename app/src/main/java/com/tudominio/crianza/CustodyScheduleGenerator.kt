package com.tudominio.crianza

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Genera registros de tiempo (un día = un RegistroTiempo) a partir de un patrón de custodia.
 *
 * Cada patrón es un ciclo de 14 días que mapea índice del ciclo → padre A o B.
 * El generador recorre desde fechaInicio durante mesesGenerados y crea un registro
 * por cada hijo y cada día.
 *
 * El campo `origenSchedule` queda con el id del schedule, lo que permite borrar
 * en lote y regenerar sin tocar registros manuales del usuario.
 */
object CustodyScheduleGenerator {

    /** Catálogo de patrones soportados. */
    object Patrones {
        const val P_223 = "223"
        const val P_5050 = "5050"
        const val P_5225 = "5225"
        const val P_2255 = "2255"
        const val P_WKND_ALT = "wknd_alt"
        /** Marcador para patrones personalizados. El formato real es "custom:AABBA..." */
        const val P_CUSTOM = "custom"

        fun esCustom(patron: String): Boolean = patron.startsWith("$P_CUSTOM:")

        /** Devuelve el largo del ciclo del patrón. Custom = largo de su secuencia. Resto = 14. */
        fun largoCiclo(patron: String): Int {
            if (esCustom(patron)) return patron.removePrefix("$P_CUSTOM:").length.coerceAtLeast(1)
            return 14
        }

        /** Devuelve el padre asignado al día [diaCiclo] según el patrón. */
        fun padreEnDia(patron: String, diaCiclo: Int): PadreEnCiclo {
            // Custom: secuencia explícita
            if (esCustom(patron)) {
                val seq = patron.removePrefix("$P_CUSTOM:")
                if (seq.isEmpty()) return PadreEnCiclo.A
                val idx = ((diaCiclo % seq.length) + seq.length) % seq.length
                return if (seq[idx] == 'A') PadreEnCiclo.A else PadreEnCiclo.B
            }
            val d = ((diaCiclo % 14) + 14) % 14
            return when (patron) {
                // 2-2-3: Lun-Mar A · Mié-Jue B · Vie-Sáb-Dom A · Lun-Mar B · Mié-Jue A · Vie-Sáb-Dom B
                P_223 -> when (d) {
                    0, 1, 4, 5, 6, 9, 10 -> PadreEnCiclo.A
                    else -> PadreEnCiclo.B
                }
                // 50/50: 7 días A, 7 días B
                P_5050 -> if (d < 7) PadreEnCiclo.A else PadreEnCiclo.B
                // 5-2-2-5: días 0-4 A, 5-6 B, 7-8 B, 9-13 A
                P_5225 -> when (d) {
                    in 0..4 -> PadreEnCiclo.A
                    in 5..8 -> PadreEnCiclo.B
                    else -> PadreEnCiclo.A
                }
                // 2-2-5-5: días 0-1 A, 2-3 B, 4-8 A, 9-13 B
                P_2255 -> when (d) {
                    in 0..1 -> PadreEnCiclo.A
                    in 2..3 -> PadreEnCiclo.B
                    in 4..8 -> PadreEnCiclo.A
                    else -> PadreEnCiclo.B
                }
                // Fines alternos: lun-vie A; sáb-dom alterna semana
                // Asume fechaInicio cae en lunes. Día semana del ciclo = d % 7.
                // Sáb-dom de semana 1 (días 5-6) = B; sáb-dom semana 2 (días 12-13) = A.
                P_WKND_ALT -> {
                    val esFinSem = (d % 7) >= 5
                    if (!esFinSem) PadreEnCiclo.A
                    else if (d < 7) PadreEnCiclo.B else PadreEnCiclo.A
                }
                else -> PadreEnCiclo.A
            }
        }

        fun nombreLargo(patron: String): String = when {
            esCustom(patron) -> "Personalizado (${largoCiclo(patron)} días)"
            patron == P_223 -> "2-2-3 (clásico de coparenting)"
            patron == P_5050 -> "50/50 — semana on, semana off"
            patron == P_5225 -> "5-2-2-5"
            patron == P_2255 -> "2-2-5-5"
            patron == P_WKND_ALT -> "Fines de semana alternos"
            else -> patron
        }

        fun descripcionCorta(patron: String): String = when {
            esCustom(patron) -> "Diseñás vos el ciclo: tocá cada día para que sea A o B."
            patron == P_223 -> "Lun-Mar con A · Mié-Jue con B · Fin de semana A. Semana siguiente espejo."
            patron == P_5050 -> "Una semana entera con A, la siguiente entera con B. Pocas transiciones."
            patron == P_5225 -> "Lun-Vie A · sáb-mar B · mié-dom A. Mezcla estabilidad y contacto frecuente."
            patron == P_2255 -> "Lun-Mar A · mié-jue B · vie-mar A · mié-dom B. Estructura predecible."
            patron == P_WKND_ALT -> "Lun-Vie con A. Fines alternados entre A y B."
            else -> ""
        }

        /** Sólo presets fijos. El custom se construye aparte en la UI. */
        fun listaTodos(): List<String> = listOf(P_223, P_5050, P_5225, P_2255, P_WKND_ALT)

        /** Construye un patrón custom a partir de una secuencia A/B. */
        fun construirCustom(seq: String): String {
            val limpio = seq.uppercase().filter { it == 'A' || it == 'B' }.take(28)
            require(limpio.isNotEmpty()) { "La secuencia no puede estar vacía" }
            return "$P_CUSTOM:$limpio"
        }
    }

    enum class PadreEnCiclo { A, B }

    /** Genera la lista de registros sin persistir. La idea es persistir afuera. */
    fun generar(
        schedule: CustodySchedule,
        padreA: Padre,
        padreB: Padre,
        hijos: List<Hijo>
    ): List<RegistroTiempo> {
        if (hijos.isEmpty()) return emptyList()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(schedule.fechaInicio) ?: Date()

        val finCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, schedule.mesesGenerados) }
        val totalDias = ((finCal.timeInMillis - cal.timeInMillis) / (24L * 60L * 60L * 1000L)).toInt()

        val out = mutableListOf<RegistroTiempo>()
        for (offset in 0 until totalDias) {
            val cur = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val fechaTxt = sdf.format(cur.time)
            val cuál = Patrones.padreEnDia(schedule.patron, offset)
            val padre = if (cuál == PadreEnCiclo.A) padreA else padreB
            for (hijo in hijos) {
                out += RegistroTiempo(
                    id = UUID.randomUUID().toString(),
                    idHijo = hijo.id,
                    nombreHijo = hijo.nombre,
                    idPadre = padre.id,
                    nombrePadre = padre.nombre,
                    fecha = fechaTxt,
                    horaInicio = schedule.horaInicio,
                    horaFin = schedule.horaFin,
                    fechaCompleta = cur.timeInMillis,
                    esTodosLosHijos = false,
                    autocompensado = false,
                    origenSchedule = schedule.id
                )
            }
        }
        return out
    }
}
