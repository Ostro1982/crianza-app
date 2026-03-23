package com.tudominio.crianza

import android.util.Log
import java.util.Date
import java.util.Properties
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.MimeMultipart
import javax.mail.search.AndTerm
import javax.mail.search.FlagTerm
import javax.mail.search.FromStringTerm
import javax.mail.search.OrTerm
import javax.mail.search.SearchTerm
import javax.mail.search.SubjectTerm

/**
 * Servicio de revisión de emails via IMAP.
 *
 * Los emails deben tener en el asunto: CRIANZA: [comando]
 * O provenir de un remitente/asunto configurado como filtro.
 *
 * El cuerpo del email puede contener los mismos comandos que Telegram:
 *   /gasto [monto] [descripcion]
 *   /tiempo [hijo] [inicio] [fin]
 *   /evento [titulo] [fecha]
 *   /recuerdo [titulo]: [descripcion]
 *   /aceptar
 */
class EmailService(private val config: ConfiguracionIntegracion) {

    data class EmailParsado(
        val de: String,
        val asunto: String,
        val cuerpo: String,
        val fecha: Date,
        val contenidoIcal: String? = null  // contenido .ics si el email tiene invitación
    )

    fun obtenerEmailsNuevos(filtros: List<FiltroEmail>, ultimaRevision: Long): List<EmailParsado> {
        if (config.emailHost.isEmpty() || config.emailUser.isEmpty()) return emptyList()

        var store: Store? = null
        var folder: Folder? = null

        return try {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", config.emailHost)
                put("mail.imaps.port", config.emailPort.toString())
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.timeout", "10000")
            }

            val session = Session.getInstance(props)
            store = session.getStore("imaps")
            store.connect(config.emailHost, config.emailUser, config.emailPassword)

            folder = store.getFolder("INBOX")
            folder.open(Folder.READ_WRITE)

            // Solo emails no leídos
            val termNoLeido = FlagTerm(Flags(Flags.Flag.SEEN), false)

            // Construir términos de búsqueda según filtros activos
            val terminosFiltro = filtros.mapNotNull { filtro ->
                when (filtro.tipo) {
                    "remitente" -> FromStringTerm(filtro.valor) as SearchTerm
                    "asunto" -> SubjectTerm(filtro.valor) as SearchTerm
                    else -> null
                }
            }

            val mensajes = if (terminosFiltro.isEmpty()) {
                folder.search(termNoLeido)
            } else {
                val terminoFiltros = if (terminosFiltro.size == 1) {
                    terminosFiltro.first()
                } else {
                    OrTerm(terminosFiltro.toTypedArray())
                }
                folder.search(AndTerm(termNoLeido, terminoFiltros))
            }

            val emails = mensajes
                .filter { it.receivedDate?.time ?: 0L > ultimaRevision }
                .map { msg ->
                    var cuerpo = ""
                    var ical: String? = null

                    try {
                        when {
                            msg.isMimeType("text/plain") ->
                                cuerpo = msg.content as? String ?: ""

                            msg.isMimeType("text/calendar") ->
                                ical = msg.content as? String

                            msg.isMimeType("multipart/*") -> {
                                val mp = msg.content as MimeMultipart
                                val partes = (0 until mp.count).map { mp.getBodyPart(it) }
                                cuerpo = partes.firstOrNull { it.isMimeType("text/plain") }
                                    ?.content as? String ?: ""
                                // Buscar adjunto .ics o parte text/calendar
                                ical = partes.firstOrNull {
                                    it.isMimeType("text/calendar") ||
                                    it.fileName?.endsWith(".ics", ignoreCase = true) == true
                                }?.content as? String
                            }
                        }
                    } catch (e: Exception) { }

                    // Marcar como leído
                    msg.setFlag(Flags.Flag.SEEN, true)

                    EmailParsado(
                        de = msg.from?.firstOrNull()?.toString() ?: "",
                        asunto = msg.subject ?: "",
                        cuerpo = cuerpo.trim(),
                        fecha = msg.receivedDate ?: Date(),
                        contenidoIcal = ical
                    )
                }

            emails
        } catch (e: Exception) {
            Log.e("EmailService", "Error al revisar emails", e)
            emptyList()
        } finally {
            try { folder?.close(false) } catch (_: Exception) {}
            try { store?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Extrae todos los comandos del email.
     * Detecta automáticamente:
     * - Comandos explícitos: líneas que empiezan con /
     * - Lista de compras: líneas que empiezan con "- ", "• " o "* "
     * - Gastos: líneas con formato "$monto descripcion" o "monto descripcion"
     * - Eventos: líneas con fecha YYYY-MM-DD
     * - Eventos en lenguaje natural español: "los invitamos... el DD de [mes] de YYYY a las HH:MM en [lugar]"
     */
    fun extraerComandos(email: EmailParsado): List<String> {
        val comandos = mutableListOf<String>()

        // Buscar en el asunto primero
        val asuntoUpper = email.asunto.uppercase()
        if (asuntoUpper.startsWith("CRIANZA:")) {
            val cmd = email.asunto.substringAfter(":").trim()
            if (cmd.startsWith("/")) comandos.add(cmd)
        }

        val lineas = email.cuerpo.lines().map { it.trim() }.filter { it.isNotEmpty() }

        for (linea in lineas) {
            when {
                // Comando explícito
                linea.startsWith("/") -> comandos.add(linea)

                // Lista de compras: "- item", "• item", "* item"
                linea.length > 2 && (linea.startsWith("- ") || linea.startsWith("• ") || linea.startsWith("* ")) -> {
                    val item = linea.drop(2).trim()
                    if (item.isNotEmpty()) comandos.add("/compra $item")
                }

                // Gasto: "$150 descripcion" o "150.50 descripcion"
                linea.matches(Regex("""^\$?\d+(\.\d+)?\s+\S.+""")) -> {
                    val limpia = linea.trimStart('$')
                    val partes = limpia.split(Regex("\\s+"), 2)
                    val monto = partes[0].toDoubleOrNull()
                    val desc = partes.getOrNull(1) ?: ""
                    if (monto != null && desc.isNotEmpty()) {
                        comandos.add("/gasto $monto $desc")
                    }
                }

                // Evento: línea con fecha YYYY-MM-DD
                linea.contains(Regex("""\d{4}-\d{2}-\d{2}""")) && linea.length > 10 -> {
                    val fechaMatch = Regex("""\d{4}-\d{2}-\d{2}""").find(linea)
                    if (fechaMatch != null) {
                        val fecha = fechaMatch.value
                        val titulo = linea.replace(fecha, "").trim().trimEnd(':').trim()
                        if (titulo.isNotEmpty()) {
                            comandos.add("/evento $titulo $fecha")
                        }
                    }
                }
            }
        }

        // Detección de eventos en lenguaje natural español (incluye eventos escolares y cumpleaños)
        val eventoNatural = DetectorEventosNaturales.extraer(email.cuerpo, email.asunto)
        if (eventoNatural != null) comandos.add(eventoNatural)

        return comandos.distinct()
    }

    // Compatibilidad: devuelve el primer comando
    fun extraerComando(email: EmailParsado): String? = extraerComandos(email).firstOrNull()
}
