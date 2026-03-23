package com.tudominio.crianza

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface CompensacionDao {
    @Insert
    suspend fun insertarCompensacion(compensacion: Compensacion)

    @Update
    suspend fun actualizarCompensacion(compensacion: Compensacion)

    @Delete
    suspend fun eliminarCompensacion(compensacion: Compensacion)

    @Query("SELECT * FROM compensaciones ORDER BY fecha DESC")
    suspend fun obtenerTodasLasCompensaciones(): List<Compensacion>
}
