package com.tudominio.crianza

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface GastoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarGasto(gasto: Gasto)

    @Update
    suspend fun actualizarGasto(gasto: Gasto)

    @Delete
    suspend fun eliminarGasto(gasto: Gasto)

    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    suspend fun obtenerTodosLosGastos(): List<Gasto>

    @Query("SELECT * FROM gastos WHERE idPagador = :idPadre")
    suspend fun obtenerGastosPorPagador(idPadre: String): List<Gasto>

    @Query("SELECT SUM(monto) FROM gastos WHERE idPagador = :idPadre")
    suspend fun totalGastadoPor(idPadre: String): Double?

    @Query("SELECT COUNT(*) FROM gastos WHERE descripcion = :desc AND monto = :monto AND fecha = :fecha")
    suspend fun contarDuplicado(desc: String, monto: Double, fecha: String): Int

    @Query("DELETE FROM gastos WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM gastos")
    suspend fun eliminarTodos()
}