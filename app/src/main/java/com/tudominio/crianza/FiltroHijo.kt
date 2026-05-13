package com.tudominio.crianza

import androidx.compose.runtime.mutableStateOf

/**
 * Filtro global por hijo. "" = todos.
 * Lo escribe el chip de PantallaPrincipal y lo leen las pantallas hijas
 * (Gastos, Calendario, Compras, Widget Semana, Estadísticas).
 */
object FiltroHijo {
    val idActualState = mutableStateOf("")
    val idActual: String get() = idActualState.value
    fun set(id: String) { idActualState.value = id }
    fun aplicar(idsHijos: List<String>): Boolean {
        val id = idActual
        return id.isEmpty() || id in idsHijos
    }
}
