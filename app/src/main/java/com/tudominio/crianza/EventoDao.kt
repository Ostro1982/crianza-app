package com.tudominio.crianza

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface EventoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEvento(evento: Evento)

    @Update
    suspend fun actualizarEvento(evento: Evento)

    @Delete
    suspend fun eliminarEvento(evento: Evento)

    @Query("SELECT * FROM eventos ORDER BY fecha, horaInicio")
    suspend fun obtenerTodosLosEventos(): List<Evento>

    @Query("SELECT * FROM eventos WHERE fecha = :fecha ORDER BY horaInicio")
    suspend fun obtenerEventosPorFecha(fecha: String): List<Evento>

    @Query("SELECT * FROM eventos WHERE fecha >= :desde AND fecha <= :hasta ORDER BY fecha, horaInicio")
    suspend fun obtenerEventosDesde(desde: String, hasta: String): List<Evento>

    @Query("SELECT COUNT(*) FROM eventos WHERE titulo = :titulo AND fecha = :fecha")
    suspend fun contarDuplicado(titulo: String, fecha: String): Int

    @Query("DELETE FROM eventos WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM eventos WHERE id IN (:ids)")
    suspend fun eliminarPorIds(ids: List<String>)

    @Query("DELETE FROM eventos")
    suspend fun eliminarTodos()
}