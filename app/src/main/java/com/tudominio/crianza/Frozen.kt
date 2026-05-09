package com.tudominio.crianza

/**
 * Modo frozen: después de N días, los registros pasan a "evidencia inalterable".
 * No se pueden editar ni borrar desde la UI. Refuerza el valor legal del registro.
 *
 * `frozenDias = 0` desactiva la feature.
 */
object Frozen {
    @Volatile var diasActuales: Int = 0

    fun estaCongelado(fechaCompletaMs: Long): Boolean {
        if (diasActuales <= 0) return false
        val ahora = System.currentTimeMillis()
        val umbralMs = diasActuales * 24L * 60L * 60L * 1000L
        return (ahora - fechaCompletaMs) > umbralMs
    }

    fun mensaje(): String =
        "Bloqueado por modo evidencia (>$diasActuales días desde su creación). Cambios congelados para preservar registro inalterable."
}
