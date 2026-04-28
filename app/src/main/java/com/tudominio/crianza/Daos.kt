package com.tudominio.crianza

import androidx.room.*

@Dao
interface FamiliaDao {
    @Query("SELECT * FROM padres")
    suspend fun obtenerTodosLosPadres(): List<Padre>

    @Query("SELECT * FROM hijos")
    suspend fun obtenerTodosLosHijos(): List<Hijo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarPadre(padre: Padre)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarHijo(hijo: Hijo)

    @Query("DELETE FROM padres")
    suspend fun eliminarTodosLosPadres()

    @Query("DELETE FROM hijos")
    suspend fun eliminarTodosLosHijos()
}

@Dao
interface RegistroTiempoDao {
    @Query("SELECT * FROM registros_tiempo ORDER BY fecha DESC, horaInicio DESC")
    suspend fun obtenerTodosLosRegistros(): List<RegistroTiempo>

    @Query("SELECT * FROM registros_tiempo WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): RegistroTiempo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRegistro(registro: RegistroTiempo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRegistros(registros: List<RegistroTiempo>)

    @Update
    suspend fun actualizarRegistro(registro: RegistroTiempo)

    @Delete
    suspend fun eliminarRegistro(registro: RegistroTiempo)

    @Query("DELETE FROM registros_tiempo WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    @Query("DELETE FROM registros_tiempo")
    suspend fun eliminarTodos()
}

@Dao
interface ConfiguracionDao {
    @Query("SELECT * FROM configuracion_tiempo WHERE id = 1")
    suspend fun obtenerConfiguracion(): ConfiguracionTiempo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarConfiguracion(configuracion: ConfiguracionTiempo)
}

@Dao
interface RecuerdoDao {
    @Query("SELECT * FROM recuerdos ORDER BY fecha DESC")
    suspend fun obtenerTodosLosRecuerdos(): List<Recuerdo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRecuerdo(recuerdo: Recuerdo)

    @Update
    suspend fun actualizarRecuerdo(recuerdo: Recuerdo)

    @Delete
    suspend fun eliminarRecuerdo(recuerdo: Recuerdo)

    @Query("DELETE FROM recuerdos WHERE id = :id")
    suspend fun eliminarPorId(id: String)
}
