package com.tudominio.crianza

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface CompensacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCompensacion(compensacion: Compensacion)

    @Update
    suspend fun actualizarCompensacion(compensacion: Compensacion)

    @Delete
    suspend fun eliminarCompensacion(compensacion: Compensacion)

    @Query("SELECT * FROM compensaciones ORDER BY fecha DESC")
    suspend fun obtenerTodasLasCompensaciones(): List<Compensacion>

    @Query("DELETE FROM compensaciones WHERE id = :id")
    suspend fun eliminarPorId(id: String)
}
