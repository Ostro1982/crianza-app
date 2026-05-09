package com.tudominio.crianza

import androidx.room.*

@Dao
interface ConfiguracionIntegracionDao {
    @Query("SELECT * FROM configuracion_integracion WHERE id = 1")
    suspend fun obtener(): ConfiguracionIntegracion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(config: ConfiguracionIntegracion)
}
