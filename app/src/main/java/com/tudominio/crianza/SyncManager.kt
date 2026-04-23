package com.tudominio.crianza

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.atomic.AtomicInteger

class SyncManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val fs = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerRegistrations = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    private fun familyId() = FamilyIdManager.obtenerFamilyId(context)
    private fun col(nombre: String) = fs.collection("familias/${familyId()}/$nombre")

    // ── Serialización ────────────────────────────────────────────────────────

    private fun Padre.toMap() = mapOf(
        "id" to id, "nombre" to nombre, "telefono" to telefono,
        "email" to email, "rol" to rol, "fechaNacimiento" to fechaNacimiento
    )

    private fun Hijo.toMap() = mapOf(
        "id" to id, "nombre" to nombre, "fechaNacimiento" to fechaNacimiento
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toPadre(): Padre? {
        return try {
            Padre(
                id = this["id"] as? String ?: return null,
                nombre = this["nombre"] as? String ?: "",
                telefono = this["telefono"] as? String ?: "",
                email = this["email"] as? String ?: "",
                rol = this["rol"] as? String ?: "padre",
                fechaNacimiento = this["fechaNacimiento"] as? String ?: ""
            )
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toHijo(): Hijo? {
        return try {
            Hijo(
                id = this["id"] as? String ?: return null,
                nombre = this["nombre"] as? String ?: "",
                fechaNacimiento = this["fechaNacimiento"] as? String ?: ""
            )
        } catch (e: Exception) { null }
    }

    private fun Evento.toMap() = mapOf(
        "id" to id, "titulo" to titulo, "descripcion" to descripcion,
        "fecha" to fecha, "horaInicio" to (horaInicio ?: ""), "horaFin" to (horaFin ?: ""),
        "color" to color, "fechaCompleta" to fechaCompleta, "ubicacion" to ubicacion,
        "origenEmail" to origenEmail, "asistenciaPadre1" to asistenciaPadre1,
        "asistenciaPadre2" to asistenciaPadre2
    )

    private fun Gasto.toMap() = mapOf(
        "id" to id, "descripcion" to descripcion, "monto" to monto, "fecha" to fecha,
        "idPagador" to idPagador, "nombrePagador" to nombrePagador,
        "idsHijos" to idsHijos, "nombresHijos" to nombresHijos,
        "dividirAutomatico" to dividirAutomatico, "fechaCompleta" to fechaCompleta,
        "categoria" to categoria, "autocompensado" to autocompensado
    )

    private fun ItemCompra.toMap() = mapOf(
        "id" to id, "descripcion" to descripcion, "cantidad" to cantidad,
        "unidad" to unidad, "categoria" to categoria, "subcategoria" to subcategoria,
        "precio" to precio, "comprado" to comprado, "agregadoPor" to agregadoPor,
        "idPagador" to idPagador, "fechaCompleta" to fechaCompleta,
        "esPrivado" to esPrivado, "idPropietario" to idPropietario
    )

    private fun Mensaje.toMap() = mapOf(
        "id" to id, "idEmisor" to idEmisor, "nombreEmisor" to nombreEmisor,
        "texto" to texto, "fechaCompleta" to fechaCompleta, "leido" to leido
    )

    private fun RegistroTiempo.toMap() = mapOf(
        "id" to id, "idHijo" to idHijo, "nombreHijo" to nombreHijo,
        "idPadre" to idPadre, "nombrePadre" to nombrePadre,
        "fecha" to fecha, "horaInicio" to horaInicio, "horaFin" to horaFin,
        "fechaCompleta" to fechaCompleta, "esTodosLosHijos" to esTodosLosHijos,
        "autocompensado" to autocompensado
    )

    private fun Compensacion.toMap() = mapOf(
        "id" to id, "fecha" to fecha,
        "idPagador" to idPagador, "nombrePagador" to nombrePagador,
        "idReceptor" to idReceptor, "nombreReceptor" to nombreReceptor,
        "horasCompensadas" to horasCompensadas, "valorHora" to valorHora,
        "montoTotal" to montoTotal, "fechaCompleta" to fechaCompleta,
        "aceptadoPadre1" to aceptadoPadre1, "aceptadoPadre2" to aceptadoPadre2
    )

    private fun Pendiente.toMap() = mapOf(
        "id" to id, "titulo" to titulo, "completado" to completado,
        "fechaCreacion" to fechaCreacion, "fechaLimite" to fechaLimite,
        "asignadoA" to asignadoA
    )

    private fun RegistroEdicion.toMap() = mapOf(
        "id" to id, "idRegistro" to idRegistro,
        "fechaEdicion" to fechaEdicion,
        "fechaAnterior" to fechaAnterior,
        "horaInicioAnterior" to horaInicioAnterior,
        "horaFinAnterior" to horaFinAnterior,
        "nombreHijoAnterior" to nombreHijoAnterior,
        "autocompensadoAnterior" to autocompensadoAnterior
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toRegistroEdicion(): RegistroEdicion? {
        return try {
            RegistroEdicion(
                id = this["id"] as? String ?: return null,
                idRegistro = this["idRegistro"] as? String ?: return null,
                fechaEdicion = this["fechaEdicion"] as? Long ?: 0L,
                fechaAnterior = this["fechaAnterior"] as? String ?: "",
                horaInicioAnterior = this["horaInicioAnterior"] as? String ?: "",
                horaFinAnterior = this["horaFinAnterior"] as? String ?: "",
                nombreHijoAnterior = this["nombreHijoAnterior"] as? String ?: "",
                autocompensadoAnterior = this["autocompensadoAnterior"] as? Boolean ?: false
            )
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toEvento(): Evento? {
        return try {
            Evento(
                id = this["id"] as? String ?: return null,
                titulo = this["titulo"] as? String ?: "",
                descripcion = this["descripcion"] as? String ?: "",
                fecha = this["fecha"] as? String ?: "",
                horaInicio = (this["horaInicio"] as? String)?.ifEmpty { null },
                horaFin = (this["horaFin"] as? String)?.ifEmpty { null },
                color = (this["color"] as? Long)?.toInt() ?: 0,
                fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
                ubicacion = this["ubicacion"] as? String ?: "",
                origenEmail = this["origenEmail"] as? Boolean ?: false,
                asistenciaPadre1 = this["asistenciaPadre1"] as? String ?: "",
                asistenciaPadre2 = this["asistenciaPadre2"] as? String ?: ""
            )
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toGasto(): Gasto? {
        return try {
            Gasto(
                id = this["id"] as? String ?: return null,
                descripcion = this["descripcion"] as? String ?: "",
                monto = this["monto"] as? Double ?: 0.0,
                fecha = this["fecha"] as? String ?: "",
                idPagador = this["idPagador"] as? String ?: "",
                nombrePagador = this["nombrePagador"] as? String ?: "",
                idsHijos = (this["idsHijos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                nombresHijos = this["nombresHijos"] as? String ?: "",
                dividirAutomatico = this["dividirAutomatico"] as? Boolean ?: true,
                fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
                categoria = this["categoria"] as? String ?: "",
                autocompensado = this["autocompensado"] as? Boolean ?: false
            )
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toItemCompra(): ItemCompra? {
        return try {
            ItemCompra(
                id = this["id"] as? String ?: return null,
                descripcion = this["descripcion"] as? String ?: "",
                cantidad = this["cantidad"] as? String ?: "1",
                unidad = this["unidad"] as? String ?: "",
                categoria = this["categoria"] as? String ?: "",
                subcategoria = this["subcategoria"] as? String ?: "",
                precio = this["precio"] as? Double ?: 0.0,
                comprado = this["comprado"] as? Boolean ?: false,
                agregadoPor = this["agregadoPor"] as? String ?: "",
                idPagador = this["idPagador"] as? String ?: "",
                fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
                esPrivado = this["esPrivado"] as? Boolean ?: false,
                idPropietario = this["idPropietario"] as? String ?: ""
            )
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toMensaje(): Mensaje? {
        return try {
            Mensaje(
                id = this["id"] as? String ?: return null,
                idEmisor = this["idEmisor"] as? String ?: "",
                nombreEmisor = this["nombreEmisor"] as? String ?: "",
                texto = this["texto"] as? String ?: "",
                fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
                leido = this["leido"] as? Boolean ?: false
            )
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toRegistroTiempo(): RegistroTiempo? {
        return try {
            RegistroTiempo(
                id = this["id"] as? String ?: return null,
                idHijo = this["idHijo"] as? String ?: "",
                nombreHijo = this["nombreHijo"] as? String ?: "",
                idPadre = this["idPadre"] as? String ?: "",
                nombrePadre = this["nombrePadre"] as? String ?: "",
                fecha = this["fecha"] as? String ?: "",
                horaInicio = this["horaInicio"] as? String ?: "",
                horaFin = this["horaFin"] as? String ?: "",
                fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
                esTodosLosHijos = this["esTodosLosHijos"] as? Boolean ?: false,
                autocompensado = this["autocompensado"] as? Boolean ?: false
            )
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toCompensacion(): Compensacion? {
        return try {
            Compensacion(
                id = this["id"] as? String ?: return null,
                fecha = this["fecha"] as? String ?: "",
                idPagador = this["idPagador"] as? String ?: "",
                nombrePagador = this["nombrePagador"] as? String ?: "",
                idReceptor = this["idReceptor"] as? String ?: "",
                nombreReceptor = this["nombreReceptor"] as? String ?: "",
                horasCompensadas = this["horasCompensadas"] as? Double ?: 0.0,
                valorHora = this["valorHora"] as? Double ?: 0.0,
                montoTotal = this["montoTotal"] as? Double ?: 0.0,
                fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
                aceptadoPadre1 = this["aceptadoPadre1"] as? Boolean ?: false,
                aceptadoPadre2 = this["aceptadoPadre2"] as? Boolean ?: false
            )
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toPendiente(): Pendiente? {
        return try {
            Pendiente(
                id = this["id"] as? String ?: return null,
                titulo = this["titulo"] as? String ?: "",
                completado = this["completado"] as? Boolean ?: false,
                fechaCreacion = this["fechaCreacion"] as? Long ?: 0L,
                fechaLimite = this["fechaLimite"] as? String ?: "",
                asignadoA = this["asignadoA"] as? String ?: ""
            )
        } catch (e: Exception) { null }
    }

    // ── Escrituras (Room + Firestore) ─────────────────────────────────────────

    suspend fun insertarEvento(evento: Evento) {
        db.eventoDao().insertarEvento(evento)
        col("eventos").document(evento.id).set(evento.toMap())
    }

    suspend fun actualizarEvento(evento: Evento) {
        db.eventoDao().actualizarEvento(evento)
        col("eventos").document(evento.id).set(evento.toMap())
    }

    suspend fun eliminarEvento(evento: Evento) {
        db.eventoDao().eliminarEvento(evento)
        col("eventos").document(evento.id).delete()
    }

    suspend fun insertarGasto(gasto: Gasto) {
        db.gastoDao().insertarGasto(gasto)
        col("gastos").document(gasto.id).set(gasto.toMap())
    }

    suspend fun actualizarGasto(gasto: Gasto) {
        db.gastoDao().actualizarGasto(gasto)
        col("gastos").document(gasto.id).set(gasto.toMap())
    }

    suspend fun eliminarGasto(gasto: Gasto) {
        db.gastoDao().eliminarGasto(gasto)
        col("gastos").document(gasto.id).delete()
    }

    suspend fun insertarItem(item: ItemCompra) {
        db.itemCompraDao().insertar(item)
        if (!item.esPrivado) col("items_compra").document(item.id).set(item.toMap())
    }

    suspend fun actualizarItem(item: ItemCompra) {
        db.itemCompraDao().actualizar(item)
        if (!item.esPrivado) col("items_compra").document(item.id).set(item.toMap())
    }

    suspend fun eliminarItem(item: ItemCompra) {
        db.itemCompraDao().eliminar(item)
        col("items_compra").document(item.id).delete()
    }

    suspend fun insertarMensaje(mensaje: Mensaje) {
        db.mensajeDao().insertar(mensaje)
        col("mensajes").document(mensaje.id).set(mensaje.toMap())
    }

    suspend fun insertarRegistro(registro: RegistroTiempo) {
        db.registroTiempoDao().insertarRegistro(registro)
        col("registros_tiempo").document(registro.id).set(registro.toMap())
    }

    suspend fun actualizarRegistro(registro: RegistroTiempo) {
        val anterior = db.registroTiempoDao().obtenerPorId(registro.id)
        if (anterior != null) {
            val edicion = RegistroEdicion(
                idRegistro = registro.id,
                fechaAnterior = anterior.fecha,
                horaInicioAnterior = anterior.horaInicio,
                horaFinAnterior = anterior.horaFin,
                nombreHijoAnterior = anterior.nombreHijo,
                autocompensadoAnterior = anterior.autocompensado
            )
            db.registroEdicionDao().insertar(edicion)
            col("registros_edicion").document(edicion.id).set(edicion.toMap())
        }
        db.registroTiempoDao().actualizarRegistro(registro)
        col("registros_tiempo").document(registro.id).set(registro.toMap())
    }

    suspend fun eliminarRegistro(registro: RegistroTiempo) {
        // Borrar las ediciones asociadas en Firestore antes de borrar local,
        // si no quedan huerfanas en la nube y reaparecen al re-vincular.
        try {
            val edicionesLocales = db.registroEdicionDao().obtenerPorRegistro(registro.id)
            edicionesLocales.forEach { ed ->
                col("registros_edicion").document(ed.id).delete()
            }
        } catch (_: Exception) {}
        db.registroEdicionDao().eliminarPorRegistro(registro.id)
        db.registroTiempoDao().eliminarRegistro(registro)
        col("registros_tiempo").document(registro.id).delete()
    }

    suspend fun insertarCompensacion(compensacion: Compensacion) {
        db.compensacionDao().insertarCompensacion(compensacion)
        col("compensaciones").document(compensacion.id).set(compensacion.toMap())
    }

    suspend fun actualizarCompensacion(compensacion: Compensacion) {
        db.compensacionDao().actualizarCompensacion(compensacion)
        col("compensaciones").document(compensacion.id).set(compensacion.toMap())
    }

    suspend fun eliminarCompensacion(compensacion: Compensacion) {
        db.compensacionDao().eliminarCompensacion(compensacion)
        col("compensaciones").document(compensacion.id).delete()
    }

    suspend fun insertarPendiente(pendiente: Pendiente) {
        db.pendienteDao().insertar(pendiente)
        col("pendientes").document(pendiente.id).set(pendiente.toMap())
    }

    suspend fun actualizarPendiente(pendiente: Pendiente) {
        db.pendienteDao().actualizar(pendiente)
        col("pendientes").document(pendiente.id).set(pendiente.toMap())
    }

    suspend fun eliminarPendiente(pendiente: Pendiente) {
        db.pendienteDao().eliminar(pendiente)
        col("pendientes").document(pendiente.id).delete()
    }

    // ── Usuarios Google ───────────────────────────────────────────────────────

    fun registrarUsuarioGoogle(usuario: UsuarioGoogle) {
        fs.collection("usuarios").document(usuario.id).set(mapOf(
            "email" to usuario.email,
            "nombre" to usuario.nombre,
            "fotoUrl" to (usuario.fotoUrl ?: ""),
            "familyId" to familyId()
        ))
    }

    // Registra emails de padres locales para que otros puedan encontrarlos sin Google Sign-In
    fun registrarEmailsPadres(padres: List<Padre>) {
        val fid = familyId()
        padres.filter { it.email.isNotBlank() }.forEach { padre ->
            fs.collection("usuarios").document("padre_${padre.id}").set(mapOf(
                "email" to padre.email.lowercase().trim(),
                "nombre" to padre.nombre,
                "fotoUrl" to "",
                "familyId" to fid
            ))
        }
    }

    suspend fun buscarUsuarioPorEmail(email: String): Pair<String, String>? =
        suspendCoroutine { cont ->
            fs.collection("usuarios").whereEqualTo("email", email).get()
                .addOnSuccessListener { snap ->
                    if (snap.isEmpty) cont.resume(null)
                    else {
                        val doc = snap.documents.first()
                        val nombre = doc.getString("nombre") ?: email
                        val fid = doc.getString("familyId")
                        cont.resume(if (fid != null) Pair(nombre, fid) else null)
                    }
                }
                .addOnFailureListener { cont.resume(null) }
        }

    // ── Subida inicial de datos locales a Firestore ───────────────────────────

    suspend fun subirDatosLocalesIfNeeded() {
        if (!FamilyIdManager.necesitaSubidaInicial(context)) return
        db.eventoDao().obtenerTodosLosEventos().forEach { col("eventos").document(it.id).set(it.toMap()) }
        db.gastoDao().obtenerTodosLosGastos().forEach { col("gastos").document(it.id).set(it.toMap()) }
        db.itemCompraDao().obtenerTodos().filter { !it.esPrivado }.forEach { col("items_compra").document(it.id).set(it.toMap()) }
        db.mensajeDao().obtenerTodos().forEach { col("mensajes").document(it.id).set(it.toMap()) }
        db.registroTiempoDao().obtenerTodosLosRegistros().forEach { col("registros_tiempo").document(it.id).set(it.toMap()) }
        db.compensacionDao().obtenerTodasLasCompensaciones().forEach { col("compensaciones").document(it.id).set(it.toMap()) }
        db.pendienteDao().obtenerTodos().forEach { col("pendientes").document(it.id).set(it.toMap()) }
        db.registroEdicionDao().obtenerTodos().forEach { col("registros_edicion").document(it.id).set(it.toMap()) }
        FamilyIdManager.marcarSubidaInicial(context)
    }

    // ── Listeners en tiempo real ──────────────────────────────────────────────

    private var ultimosCallbacks: ListenerCallbacks? = null

    data class ListenerCallbacks(
        val onEventosActualizados: () -> Unit,
        val onGastosActualizados: () -> Unit,
        val onItemsActualizados: () -> Unit,
        val onMensajesActualizados: () -> Unit,
        val onRegistrosActualizados: () -> Unit,
        val onCompensacionesActualizadas: () -> Unit,
        val onPendientesActualizados: () -> Unit,
        val onEdicionesActualizadas: () -> Unit
    )

    fun detenerListeners() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
    }

    fun reiniciarListeners() {
        val cb = ultimosCallbacks ?: return
        detenerListeners()
        iniciarListeners(
            cb.onEventosActualizados, cb.onGastosActualizados,
            cb.onItemsActualizados, cb.onMensajesActualizados,
            cb.onRegistrosActualizados, cb.onCompensacionesActualizadas,
            cb.onPendientesActualizados, cb.onEdicionesActualizadas
        )
    }

    fun iniciarListeners(
        onEventosActualizados: () -> Unit,
        onGastosActualizados: () -> Unit,
        onItemsActualizados: () -> Unit,
        onMensajesActualizados: () -> Unit,
        onRegistrosActualizados: () -> Unit = {},
        onCompensacionesActualizadas: () -> Unit = {},
        onPendientesActualizados: () -> Unit = {},
        onEdicionesActualizadas: () -> Unit = {}
    ) {
        detenerListeners()
        ultimosCallbacks = ListenerCallbacks(
            onEventosActualizados, onGastosActualizados,
            onItemsActualizados, onMensajesActualizados,
            onRegistrosActualizados, onCompensacionesActualizadas,
            onPendientesActualizados, onEdicionesActualizadas
        )

        listenerRegistrations += col("eventos").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            change.document.data.toEvento()?.let { db.eventoDao().insertarEvento(it) }
                        DocumentChange.Type.REMOVED ->
                            db.eventoDao().eliminarPorId(change.document.id)
                    }
                }
                onEventosActualizados()
            }
        }

        listenerRegistrations += col("gastos").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            change.document.data.toGasto()?.let { db.gastoDao().insertarGasto(it) }
                        DocumentChange.Type.REMOVED ->
                            db.gastoDao().eliminarPorId(change.document.id)
                    }
                }
                onGastosActualizados()
            }
        }

        listenerRegistrations += col("items_compra").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            change.document.data.toItemCompra()?.let { db.itemCompraDao().insertar(it) }
                        DocumentChange.Type.REMOVED ->
                            db.itemCompraDao().eliminarPorId(change.document.id)
                    }
                }
                onItemsActualizados()
            }
        }

        listenerRegistrations += col("mensajes").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            change.document.data.toMensaje()?.let { db.mensajeDao().insertarSiNoExiste(it) }
                        DocumentChange.Type.REMOVED ->
                            db.mensajeDao().eliminarPorId(change.document.id)
                    }
                }
                onMensajesActualizados()
            }
        }

        listenerRegistrations += col("registros_tiempo").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            change.document.data.toRegistroTiempo()?.let { db.registroTiempoDao().insertarRegistro(it) }
                        DocumentChange.Type.REMOVED ->
                            db.registroTiempoDao().eliminarPorId(change.document.id)
                    }
                }
                onRegistrosActualizados()
            }
        }

        listenerRegistrations += col("compensaciones").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            change.document.data.toCompensacion()?.let { db.compensacionDao().insertarCompensacion(it) }
                        DocumentChange.Type.REMOVED ->
                            db.compensacionDao().eliminarPorId(change.document.id)
                    }
                }
                onCompensacionesActualizadas()
            }
        }

        listenerRegistrations += col("pendientes").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            change.document.data.toPendiente()?.let { db.pendienteDao().insertar(it) }
                        DocumentChange.Type.REMOVED ->
                            db.pendienteDao().eliminarPorId(change.document.id)
                    }
                }
                onPendientesActualizados()
            }
        }

        listenerRegistrations += col("registros_edicion").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            change.document.data.toRegistroEdicion()?.let {
                                db.registroEdicionDao().insertar(it)
                            }
                        DocumentChange.Type.REMOVED ->
                            db.registroEdicionDao().eliminarPorId(change.document.id)
                    }
                }
                onEdicionesActualizadas()
            }
        }

        // Listener de planificacion semanal (config/planificacion)
        listenerRegistrations += col("config").document("planificacion")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                // Ignorar escrituras propias pendientes
                if (snap.metadata.hasPendingWrites()) return@addSnapshotListener
                snap.data?.let { aplicarPlanificacionDeMap(it) }
            }

        Log.d("SyncManager", "Listeners activos para familia: ${familyId()}")
    }

    // ── Planificación semanal (SharedPreferences → Firestore) ────────────────

    private fun planificacionToMap(): Map<String, Any> {
        val prefs = context.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE)
        val map = mutableMapOf<String, Any>()
        map["ciclo"] = prefs.getInt("dias_fijos_ciclo", 7)
        map["repetir"] = prefs.getBoolean("dias_fijos_repetir", true)
        map["actividades_fijas"] = prefs.getString("actividades_fijas", "") ?: ""
        for (ciclo in listOf(7, 14)) {
            for (idx in 0 until ciclo) {
                val key = "dias_fijos_slots_${ciclo}_$idx"
                val val_ = prefs.getString(key, "") ?: ""
                if (val_.isNotBlank()) map[key] = val_
            }
        }
        prefs.all.entries.filter { it.key.startsWith("actividad_eventos_") }.forEach { (k, v) ->
            if (v is String && v.isNotBlank()) map[k] = v
        }
        return map
    }

    private fun aplicarPlanificacionDeMap(data: Map<String, Any>) {
        val prefs = context.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        (data["ciclo"] as? Long)?.let { editor.putInt("dias_fijos_ciclo", it.toInt()) }
        (data["repetir"] as? Boolean)?.let { editor.putBoolean("dias_fijos_repetir", it) }
        (data["actividades_fijas"] as? String)?.let { editor.putString("actividades_fijas", it) }
        data.entries.filter { it.key.startsWith("dias_fijos_slots_") }.forEach { (k, v) ->
            editor.putString(k, v as? String ?: "")
        }
        data.entries.filter { it.key.startsWith("actividad_eventos_") }.forEach { (k, v) ->
            editor.putString(k, v as? String ?: "")
        }
        editor.apply()
    }

    suspend fun subirPlanificacion() {
        col("config").document("planificacion").set(planificacionToMap()).await()
    }

    // Sube padres/hijos a Firestore sobreescribiendo TODO lo remoto (borra
    // acumulacion historica de vinculaciones anteriores).
    suspend fun subirFamiliaBasica() {
        val padresLocales = db.familiaDao().obtenerTodosLosPadres()
        val hijosLocales = db.familiaDao().obtenerTodosLosHijos()
        // Borrar TODOS los docs remotos antes de subir los locales
        try {
            val padresRemotos = col("padres").get().await()
            padresRemotos.documents.forEach { col("padres").document(it.id).delete().await() }
            val hijosRemotos = col("hijos").get().await()
            hijosRemotos.documents.forEach { col("hijos").document(it.id).delete().await() }
        } catch (_: Exception) {}
        padresLocales.forEach { col("padres").document(it.id).set(it.toMap()).await() }
        hijosLocales.forEach { col("hijos").document(it.id).set(it.toMap()).await() }
    }

    // Borra TODOS los padres/hijos de la familia actual en Firestore.
    // Usar al re-registrar familia para que los borrados no reaparezcan al vincular.
    suspend fun limpiarFamiliaEnFirestore() {
        try {
            val padresSnap = col("padres").get().await()
            padresSnap.documents.forEach { col("padres").document(it.id).delete().await() }
            val hijosSnap = col("hijos").get().await()
            hijosSnap.documents.forEach { col("hijos").document(it.id).delete().await() }
        } catch (_: Exception) {}
    }

    // Descarga todos los datos de la familia actual de Firestore a Room
    suspend fun descargarDatosDeFamilia() {
        val fid = familyId()
        Log.d("SyncManager", "Descargando datos de familia: $fid")

        val padresSnap = col("padres").get().await()
        padresSnap.documents.forEach { doc -> doc.data?.toPadre()?.let { db.familiaDao().insertarPadre(it) } }

        val hijosSnap = col("hijos").get().await()
        hijosSnap.documents.forEach { doc -> doc.data?.toHijo()?.let { db.familiaDao().insertarHijo(it) } }

        val eventosSnap = col("eventos").get().await()
        eventosSnap.documents.forEach { doc -> doc.data?.toEvento()?.let { db.eventoDao().insertarEvento(it) } }

        val gastosSnap = col("gastos").get().await()
        gastosSnap.documents.forEach { doc -> doc.data?.toGasto()?.let { db.gastoDao().insertarGasto(it) } }

        val itemsSnap = col("items_compra").get().await()
        itemsSnap.documents.forEach { doc -> doc.data?.toItemCompra()?.let { db.itemCompraDao().insertar(it) } }

        val mensajesSnap = col("mensajes").get().await()
        mensajesSnap.documents.forEach { doc -> doc.data?.toMensaje()?.let { db.mensajeDao().insertarSiNoExiste(it) } }

        val registrosSnap = col("registros_tiempo").get().await()
        val idsRegistrosValidos = mutableSetOf<String>()
        registrosSnap.documents.forEach { doc ->
            doc.data?.toRegistroTiempo()?.let {
                db.registroTiempoDao().insertarRegistro(it)
                idsRegistrosValidos += it.id
            }
        }

        val compensacionesSnap = col("compensaciones").get().await()
        compensacionesSnap.documents.forEach { doc -> doc.data?.toCompensacion()?.let { db.compensacionDao().insertarCompensacion(it) } }

        val pendientesSnap = col("pendientes").get().await()
        pendientesSnap.documents.forEach { doc -> doc.data?.toPendiente()?.let { db.pendienteDao().insertar(it) } }

        val edicionesSnap = col("registros_edicion").get().await()
        edicionesSnap.documents.forEach { doc ->
            doc.data?.toRegistroEdicion()?.let { ed ->
                // Descarto ediciones huerfanas (cuyo registro_tiempo ya no existe)
                if (ed.idRegistro in idsRegistrosValidos) {
                    db.registroEdicionDao().insertar(ed)
                } else {
                    // Limpio la huerfana de Firestore tambien
                    try { col("registros_edicion").document(doc.id).delete() } catch (_: Exception) {}
                }
            }
        }

        // Planificación semanal
        val planSnap = col("config").document("planificacion").get().await()
        planSnap.data?.let { aplicarPlanificacionDeMap(it) }

        Log.d("SyncManager", "Datos descargados OK de familia: $fid")
    }

    // Sube datos locales a otra familia en Firestore (espera confirmación)
    suspend fun subirDatosAFamilia(otraFamilyId: String) {
        fun otraCol(nombre: String) = fs.collection("familias/$otraFamilyId/$nombre")
        db.familiaDao().obtenerTodosLosPadres().forEach { otraCol("padres").document(it.id).set(it.toMap()).await() }
        db.familiaDao().obtenerTodosLosHijos().forEach { otraCol("hijos").document(it.id).set(it.toMap()).await() }
        db.eventoDao().obtenerTodosLosEventos().forEach { otraCol("eventos").document(it.id).set(it.toMap()).await() }
        db.gastoDao().obtenerTodosLosGastos().forEach { otraCol("gastos").document(it.id).set(it.toMap()).await() }
        db.itemCompraDao().obtenerTodos().filter { !it.esPrivado }.forEach { otraCol("items_compra").document(it.id).set(it.toMap()).await() }
        db.mensajeDao().obtenerTodos().forEach { otraCol("mensajes").document(it.id).set(it.toMap()).await() }
        db.registroTiempoDao().obtenerTodosLosRegistros().forEach { otraCol("registros_tiempo").document(it.id).set(it.toMap()).await() }
        db.compensacionDao().obtenerTodasLasCompensaciones().forEach { otraCol("compensaciones").document(it.id).set(it.toMap()).await() }
        db.pendienteDao().obtenerTodos().forEach { otraCol("pendientes").document(it.id).set(it.toMap()).await() }
        db.registroEdicionDao().obtenerTodos().forEach { otraCol("registros_edicion").document(it.id).set(it.toMap()).await() }
        // Planificación semanal
        fs.collection("familias/$otraFamilyId/config").document("planificacion").set(planificacionToMap()).await()
        Log.d("SyncManager", "Datos subidos a familia: $otraFamilyId")
    }
}
