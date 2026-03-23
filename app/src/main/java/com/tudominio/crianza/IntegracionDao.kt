package com.tudominio.crianza

import androidx.room.*

@Dao
interface ConfiguracionIntegracionDao {
    @Query("SELECT * FROM configuracion_integracion WHERE id = 1")
    suspend fun obtener(): ConfiguracionIntegracion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(config: ConfiguracionIntegracion)
}

@Dao
interface FiltroEmailDao {
    @Query("SELECT * FROM filtros_email WHERE activo = 1")
    suspend fun obtenerActivos(): List<FiltroEmail>

    @Query("SELECT * FROM filtros_email")
    suspend fun obtenerTodos(): List<FiltroEmail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(filtro: FiltroEmail)

    @Delete
    suspend fun eliminar(filtro: FiltroEmail)

    @Update
    suspend fun actualizar(filtro: FiltroEmail)
}
