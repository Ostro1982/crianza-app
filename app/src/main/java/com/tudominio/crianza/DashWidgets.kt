package com.tudominio.crianza

import android.content.Context

/**
 * Toggle de visibilidad para cada tarjeta del dashboard de PantallaPrincipal.
 * Cada usuario decide qué módulos ver. Default: todas activas.
 * Persistido en SharedPref "crianza_prefs" con clave dash_<id>.
 */
object DashWidgets {
    const val INBOX = "inbox"
    const val EVENTOS = "eventos"
    const val COMPRAS = "compras"
    const val PENDIENTES = "pendientes"
    const val COMPENSACION = "compensacion"
    const val FINANZAS = "finanzas"
    const val CLIMA = "clima"
    const val PLAN_SEMANA = "plan_semana"

    val TODOS: List<Pair<String, String>> = listOf(
        INBOX to "Inbox de mensajes",
        EVENTOS to "Eventos próximos",
        COMPRAS to "Lista de compras",
        PENDIENTES to "Pendientes",
        COMPENSACION to "Compensación",
        FINANZAS to "Resumen finanzas",
        CLIMA to "Clima",
        PLAN_SEMANA to "Plan de la semana"
    )

    fun activo(id: String, ctx: Context): Boolean =
        ctx.getSharedPreferences("crianza_prefs", Context.MODE_PRIVATE)
            .getBoolean("dash_$id", true)

    fun set(id: String, ctx: Context, on: Boolean) {
        ctx.getSharedPreferences("crianza_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("dash_$id", on).apply()
    }
}
