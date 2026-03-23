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

class SyncManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val familyId = FamilyIdManager.obtenerFamilyId(context)
    private val fs = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun col(nombre: String) = fs.collection("familias/$familyId/$nombre")

    // ── Serialización ────────────────────────────────────────────────────────

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
        "dividirAutomatico" to dividirAutomatico, "fechaCompleta" to fechaCompleta
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

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toEvento(): Evento? = try {
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

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toGasto(): Gasto? = try {
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
            fechaCompleta = this["fechaCompleta"] as? Long ?: 0L
        )
    } catch (e: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toItemCompra(): ItemCompra? = try {
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

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.toMensaje(): Mensaje? = try {
        Mensaje(
            id = this["id"] as? String ?: return null,
            idEmisor = this["idEmisor"] as? String ?: "",
            nombreEmisor = this["nombreEmisor"] as? String ?: "",
            texto = this["texto"] as? String ?: "",
            fechaCompleta = this["fechaCompleta"] as? Long ?: 0L,
            leido = this["leido"] as? Boolean ?: false
        )
    } catch (e: Exception) { null }

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

    // ── Listeners en tiempo real ──────────────────────────────────────────────

    fun iniciarListeners(
        onEventosActualizados: () -> Unit,
        onGastosActualizados: () -> Unit,
        onItemsActualizados: () -> Unit,
        onMensajesActualizados: () -> Unit
    ) {
        col("eventos").addSnapshotListener { snap, err ->
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

        col("gastos").addSnapshotListener { snap, err ->
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

        col("items_compra").addSnapshotListener { snap, err ->
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

        col("mensajes").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            scope.launch {
                for (change in snap.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            change.document.data.toMensaje()?.let { db.mensajeDao().insertar(it) }
                        DocumentChange.Type.REMOVED ->
                            db.mensajeDao().eliminarPorId(change.document.id)
                    }
                }
                onMensajesActualizados()
            }
        }

        Log.d("SyncManager", "Listeners activos para familia: $familyId")
    }
}
