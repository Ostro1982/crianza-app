package com.tudominio.crianza

import java.util.UUID

/**
 * Lógica compartida de procesamiento de comandos.
 * Usada por SincronizacionWorker (Telegram/Email) y WhatsAppListenerService.
 */
object ProcesadorComandos {

    suspend fun procesar(
        comando: TelegramService.ComandoParsado,
        padres: List<Padre>,
        hijos: List<Hijo>,
        db: AppDatabase,
        config: ConfiguracionIntegracion
    ): String {
        if (padres.isEmpty()) return "❌ No hay padres configurados"
        val padre = when (comando.mensajeOrigen.idPadre) {
            "padre1" -> padres[0]
            else -> padres.getOrElse(1) { padres[0] }
        }

        return when (comando.tipo) {
            "gasto" -> {
                val monto = comando.datos["monto"]?.toDoubleOrNull() ?: return "❌ Monto inválido"
                val desc = comando.datos["descripcion"] ?: return "❌ Falta descripción"
                val fecha = obtenerFechaActual()
                if (db.gastoDao().contarDuplicado(desc, monto, fecha) > 0) {
                    return "⚠️ Gasto ya registrado hoy: $desc"
                }
                db.gastoDao().insertarGasto(
                    Gasto(
                        id = UUID.randomUUID().toString(),
                        descripcion = desc,
                        monto = monto,
                        fecha = fecha,
                        idPagador = padre.id,
                        nombrePagador = padre.nombre,
                        fechaCompleta = System.currentTimeMillis()
                    )
                )
                "✅ Gasto: $desc — $${"%.2f".format(monto)} (${padre.nombre})"
            }

            "tiempo" -> {
                val nombreHijo = comando.datos["hijo"] ?: return "❌ Falta nombre del hijo"
                val inicio = comando.datos["inicio"] ?: return "❌ Falta hora de inicio"
                val fin = comando.datos["fin"] ?: return "❌ Falta hora de fin"
                val hijo = hijos.firstOrNull {
                    it.nombre.lowercase().contains(nombreHijo.lowercase())
                } ?: return "❌ Hijo \"$nombreHijo\" no encontrado"
                db.registroTiempoDao().insertarRegistro(
                    RegistroTiempo(
                        id = UUID.randomUUID().toString(),
                        idHijo = hijo.id,
                        nombreHijo = hijo.nombre,
                        idPadre = padre.id,
                        nombrePadre = padre.nombre,
                        fecha = obtenerFechaActual(),
                        horaInicio = inicio,
                        horaFin = fin,
                        fechaCompleta = System.currentTimeMillis()
                    )
                )
                "✅ Tiempo: ${hijo.nombre} con ${padre.nombre} ($inicio–$fin)"
            }

            "evento" -> {
                val titulo = comando.datos["titulo"] ?: return "❌ Falta título"
                val fecha = comando.datos["fecha"] ?: obtenerFechaActual()
                if (db.eventoDao().contarDuplicado(titulo, fecha) > 0) {
                    return "⚠️ Evento ya existe: $titulo ($fecha)"
                }
                db.eventoDao().insertarEvento(
                    Evento(
                        id = UUID.randomUUID().toString(),
                        titulo = titulo,
                        fecha = fecha,
                        fechaCompleta = System.currentTimeMillis()
                    )
                )
                "✅ Evento: $titulo ($fecha)"
            }

            "compra" -> {
                val desc = comando.datos["descripcion"] ?: return "❌ Falta descripción"
                val cat = comando.datos["categoria"] ?: ""
                if (db.itemCompraDao().contarDuplicadoCompartido(desc) > 0) {
                    return "⚠️ Ya está en lista: $desc"
                }
                db.itemCompraDao().insertar(
                    ItemCompra(
                        id = UUID.randomUUID().toString(),
                        descripcion = desc,
                        categoria = cat,
                        agregadoPor = padre.nombre,
                        fechaCompleta = System.currentTimeMillis()
                    )
                )
                "✅ Lista: $desc${if (cat.isNotEmpty()) " [$cat]" else ""}"
            }

            "lista" -> {
                val pendientes = db.itemCompraDao().obtenerCompartidos().filter { !it.comprado }
                if (pendientes.isEmpty()) "🛒 Lista vacía"
                else "🛒 Lista (${pendientes.size}):\n" + pendientes.joinToString("\n") { "• ${it.descripcion}" }
            }

            "recuerdo" -> {
                val titulo = comando.datos["titulo"] ?: return "❌ Falta título"
                val desc = comando.datos["descripcion"] ?: ""
                db.recuerdoDao().insertarRecuerdo(
                    Recuerdo(
                        id = UUID.randomUUID().toString(),
                        titulo = titulo,
                        descripcion = desc,
                        fecha = obtenerFechaActual(),
                        fechaCompleta = System.currentTimeMillis()
                    )
                )
                "✅ Recuerdo: $titulo"
            }

            "aceptar" -> {
                val idEvento = comando.datos["idEvento"]
                if (idEvento != null) {
                    val evento = db.eventoDao().obtenerTodosLosEventos()
                        .firstOrNull { it.id.startsWith(idEvento) || it.id == idEvento }
                        ?: return "❌ Evento no encontrado"
                    val actualizado = when (comando.mensajeOrigen.idPadre) {
                        "padre1" -> evento.copy(asistenciaPadre1 = "va")
                        else -> evento.copy(asistenciaPadre2 = "va")
                    }
                    db.eventoDao().actualizarEvento(actualizado)
                    "✅ Marcado como que vas a \"${evento.titulo}\""
                } else {
                    val pendiente = db.compensacionDao().obtenerTodasLasCompensaciones()
                        .filter { !it.confirmada }
                        .maxByOrNull { it.fechaCompleta }
                        ?: return "ℹ️ No hay compensaciones pendientes"
                    val actualizada = when (comando.mensajeOrigen.idPadre) {
                        "padre1" -> pendiente.copy(aceptadoPadre1 = true)
                        else -> pendiente.copy(aceptadoPadre2 = true)
                    }
                    db.compensacionDao().actualizarCompensacion(actualizada)
                    if (actualizada.confirmada) {
                        // Auto-eliminar cuando ambos confirman
                        db.compensacionDao().eliminarCompensacion(actualizada)
                        val otroChatId = when (comando.mensajeOrigen.idPadre) {
                            "padre1" -> config.telegramChatIdPadre2
                            else -> config.telegramChatIdPadre1
                        }
                        if (otroChatId.isNotEmpty() && config.telegramBotToken.isNotEmpty()) {
                            TelegramService(config).enviarMensaje(
                                otroChatId,
                                "✅ Compensación cerrada: $${"%.2f".format(actualizada.montoTotal)}"
                            )
                        }
                        "✅ Compensación confirmada por ambos y eliminada del historial."
                    } else {
                        "✅ Aceptaste. Esperando confirmación del otro padre."
                    }
                }
            }

            "rechazar" -> {
                val idEvento = comando.datos["idEvento"] ?: return "❌ /rechazar [id]"
                val evento = db.eventoDao().obtenerTodosLosEventos()
                    .firstOrNull { it.id.startsWith(idEvento) || it.id == idEvento }
                    ?: return "❌ Evento no encontrado"
                val actualizado = when (comando.mensajeOrigen.idPadre) {
                    "padre1" -> evento.copy(asistenciaPadre1 = "no_va")
                    else -> evento.copy(asistenciaPadre2 = "no_va")
                }
                db.eventoDao().actualizarEvento(actualizado)
                "❌ No vas a \"${evento.titulo}\""
            }

            "estado" -> {
                val pendientes = db.compensacionDao().obtenerTodasLasCompensaciones().filter { !it.confirmada }
                if (pendientes.isEmpty()) "ℹ️ Sin compensaciones pendientes"
                else pendientes.joinToString("\n") { c ->
                    val e1 = if (c.aceptadoPadre1) "✅" else "⏳"
                    val e2 = if (c.aceptadoPadre2) "✅" else "⏳"
                    "${c.fecha}: ${c.nombrePagador}→${c.nombreReceptor} $${"%.2f".format(c.montoTotal)} [$e1$e2]"
                }
            }

            else -> "❓ Comando no reconocido"
        }
    }
}
