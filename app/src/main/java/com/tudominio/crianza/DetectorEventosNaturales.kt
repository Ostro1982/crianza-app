package com.tudominio.crianza

/**
 * Detecta invitaciones a eventos en lenguaje natural español.
 * Usado por EmailService y WhatsAppListenerService para detectar
 * invitaciones escolares y familiares en texto libre.
 *
 * LIMITACIÓN WhatsApp: NotificationListenerService solo lee el texto que aparece
 * en la notificación. Las fotos/imágenes llegan como "📷 Imagen" sin contenido real.
 */
object DetectorEventosNaturales {

    /**
     * Intenta extraer un evento de un texto en español.
     * @param texto  cuerpo del mensaje / notificación
     * @param asunto línea de asunto (opcional, para emails)
     * @return comando "/evento ..." listo para ProcesadorComandos, o null si no detectó nada
     */
    fun extraer(texto: String, asunto: String = ""): String? =
        extraerEventoNaturalEspanol(asunto, texto)

    private fun extraerEventoNaturalEspanol(asunto: String, cuerpo: String): String? {
        val textoCompleto = "$asunto $cuerpo".lowercase()

        // ── Palabras clave (ampliadas con términos escolares) ──────────────────
        val palabrasClave = listOf(
            // invitación / convocatoria genérica
            "invitamos", "invitaci", "convocamos", "convocatoria",
            // tipos de evento
            "reunión", "reunion", "acto", "ceremonia", "evento",
            "cita", "jornada", "taller", "charla", "conferencia",
            "celebración", "celebracion", "fiesta", "festejo",
            // términos escolares
            "jardín", "jardin", "primaria", "colegio", "escuela",
            "sala", "grado", "kermesse", "bazar",
            "colación", "colacion", "acto escolar",
            // cumpleaños
            "cumpleaños", "cumpleanos", "cumple"
        )
        if (palabrasClave.none { textoCompleto.contains(it) }) return null

        // ── Detección de cumpleaños: "cumpleaños de [Nombre]" ─────────────────
        // Captura "Cumpleaños de [Nombre]" con o sin tildes en la palabra
        val regexCumple = Regex(
            """cum[pb]le[aá][ñn]os\s+de\s+([A-ZÁÉÍÓÚ][a-záéíóúüñ]+(?:\s+[A-ZÁÉÍÓÚ][a-záéíóúüñ]+)?)""",
            RegexOption.IGNORE_CASE
        )
        val nombreCumple = regexCumple.find("$asunto $cuerpo")?.groupValues?.getOrNull(1)

        // ── Meses en español → número ──────────────────────────────────────────
        val meses = mapOf(
            "enero" to "01", "febrero" to "02", "marzo" to "03",
            "abril" to "04", "mayo" to "05", "junio" to "06",
            "julio" to "07", "agosto" to "08", "septiembre" to "09",
            "setiembre" to "09", "octubre" to "10", "noviembre" to "11",
            "diciembre" to "12"
        )

        // ── Extracción de fecha ────────────────────────────────────────────────
        var fechaIso: String? = null

        // Patrón 1: "el 15/03/2026" o "15/03/2026"
        val regexFechaSlash = Regex("""(\d{1,2})/(\d{1,2})/(\d{4})""")
        regexFechaSlash.find(textoCompleto)?.let { m ->
            val dia = m.groupValues[1].padStart(2, '0')
            val mes = m.groupValues[2].padStart(2, '0')
            val anio = m.groupValues[3]
            fechaIso = "$anio-$mes-$dia"
        }

        // Patrón 2: "el 5 de abril de 2026" / "5 de abril 2026"
        if (fechaIso == null) {
            val regexFechaLarga = Regex("""(\d{1,2})\s+de\s+([a-záéíóúü]+)\s+(?:de\s+)?(\d{4})""")
            regexFechaLarga.find(textoCompleto)?.let { m ->
                val dia = m.groupValues[1].padStart(2, '0')
                val nombreMes = m.groupValues[2]
                val anio = m.groupValues[3]
                val mes = meses[nombreMes]
                if (mes != null) fechaIso = "$anio-$mes-$dia"
            }
        }

        // Sin fecha reconocible → no crear evento
        if (fechaIso == null) return null

        // ── Extracción de hora ─────────────────────────────────────────────────
        val regexHora = Regex("""a las\s+(\d{1,2})(?::(\d{2}))?(?:\s*h(?:s|oras?)?)?""")
        val hora = regexHora.find(textoCompleto)?.let { m ->
            val h = m.groupValues[1].padStart(2, '0')
            val min = m.groupValues[2].ifEmpty { "00" }.padStart(2, '0')
            "$h:$min"
        }

        // ── Extracción de ubicación ────────────────────────────────────────────
        val regexUbicacion = Regex("""en\s+(nuestras?\s+\w+|el\s+[\w\s]{3,30}|la\s+[\w\s]{3,30}|[\w\s]{3,30})(?:[.,;]|$)""")
        val ubicacion = regexUbicacion.find(textoCompleto)?.groupValues?.getOrNull(1)?.trim()

        // ── Título ─────────────────────────────────────────────────────────────
        val titulo = when {
            nombreCumple != null -> "Cumpleaños de $nombreCumple"
            asunto.isNotBlank() && !asunto.uppercase().startsWith("CRIANZA:") -> asunto.trim()
            else -> {
                val primeraMarca = textoCompleto.indexOfFirst { it == '\n' }.takeIf { it > 0 } ?: 80
                textoCompleto.take(primeraMarca).trim().capitalizar()
            }
        }

        // ── Comando final ──────────────────────────────────────────────────────
        val partes = mutableListOf("/evento", titulo, fechaIso!!)
        if (hora != null) partes.add(hora)
        if (!ubicacion.isNullOrBlank()) partes.add(ubicacion.capitalizar())
        return partes.joinToString(" ")
    }

    private fun String.capitalizar(): String =
        replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }
}
