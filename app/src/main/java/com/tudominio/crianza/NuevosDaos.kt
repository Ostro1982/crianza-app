package com.tudominio.crianza

import androidx.room.*

@Dao
interface ItemCompraDao {
    @Query("SELECT * FROM items_compra ORDER BY comprado ASC, fechaCompleta DESC")
    suspend fun obtenerTodos(): List<ItemCompra>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(item: ItemCompra)

    @Update
    suspend fun actualizar(item: ItemCompra)

    @Delete
    suspend fun eliminar(item: ItemCompra)

    @Query("DELETE FROM items_compra WHERE comprado = 1")
    suspend fun eliminarComprados()

    @Query("SELECT * FROM items_compra WHERE esPrivado = 0 ORDER BY comprado ASC, fechaCompleta DESC")
    suspend fun obtenerCompartidos(): List<ItemCompra>

    @Query("SELECT * FROM items_compra WHERE esPrivado = 1 AND idPropietario = :idPadre ORDER BY comprado ASC, fechaCompleta DESC")
    suspend fun obtenerPrivados(idPadre: String): List<ItemCompra>

    @Query("SELECT COUNT(*) FROM items_compra WHERE descripcion = :desc AND comprado = 0 AND esPrivado = 0")
    suspend fun contarDuplicadoCompartido(desc: String): Int

    @Query("DELETE FROM items_compra WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM items_compra")
    suspend fun eliminarTodos()
}

@Dao
interface DocumentoDao {
    @Query("SELECT * FROM documentos ORDER BY fechaModificacion DESC")
    suspend fun obtenerTodos(): List<Documento>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(documento: Documento)

    @Update
    suspend fun actualizar(documento: Documento)

    @Delete
    suspend fun eliminar(documento: Documento)
}

@Dao
interface CategoriaCompraDao {
    @Query("SELECT * FROM categorias_compra ORDER BY nombre ASC")
    suspend fun obtenerTodas(): List<CategoriaCompra>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(cat: CategoriaCompra)

    @Delete
    suspend fun eliminar(cat: CategoriaCompra)
}

@Dao
interface MensajeDao {
    @Query("SELECT * FROM mensajes ORDER BY fechaCompleta ASC")
    suspend fun obtenerTodos(): List<Mensaje>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(mensaje: Mensaje)

    // Para sincronización Firestore: no sobreescribe mensajes ya leídos localmente
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(mensaje: Mensaje)

    @Query("UPDATE mensajes SET leido = 1 WHERE leido = 0")
    suspend fun marcarTodosLeidos()

    @Query("SELECT COUNT(*) FROM mensajes WHERE leido = 0")
    suspend fun contarNoLeidos(): Int

    @Delete
    suspend fun eliminar(mensaje: Mensaje)

    @Query("DELETE FROM mensajes WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM mensajes")
    suspend fun eliminarTodos()
}

@Dao
interface RegistroEdicionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(edicion: RegistroEdicion)

    @Query("SELECT * FROM registros_edicion WHERE idRegistro = :idRegistro ORDER BY fechaEdicion DESC")
    suspend fun obtenerPorRegistro(idRegistro: String): List<RegistroEdicion>

    @Query("SELECT * FROM registros_edicion ORDER BY fechaEdicion DESC")
    suspend fun obtenerTodos(): List<RegistroEdicion>

    @Query("DELETE FROM registros_edicion WHERE idRegistro = :idRegistro")
    suspend fun eliminarPorRegistro(idRegistro: String)

    @Query("DELETE FROM registros_edicion WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM registros_edicion")
    suspend fun eliminarTodos()
}

@Dao
interface PendienteDao {
    @Query("SELECT * FROM pendientes ORDER BY completado ASC, fechaCreacion DESC")
    suspend fun obtenerTodos(): List<Pendiente>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(pendiente: Pendiente)

    @Update
    suspend fun actualizar(pendiente: Pendiente)

    @Delete
    suspend fun eliminar(pendiente: Pendiente)

    @Query("SELECT * FROM pendientes WHERE completado = 0 ORDER BY fechaCreacion DESC")
    suspend fun obtenerPendientes(): List<Pendiente>

    @Query("DELETE FROM pendientes WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM pendientes")
    suspend fun eliminarTodos()
}
