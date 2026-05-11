package com.tudominio.crianza

import android.content.Context

object ModoFamilia {
    private const val PREFS = "crianza_prefs"
    private const val KEY = "modo"
    const val CONVIVENCIA = "juntos"
    const val COPARENTING = "separados"

    fun actual(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, COPARENTING) ?: COPARENTING

    fun esConvivencia(ctx: Context): Boolean = actual(ctx) == CONVIVENCIA

    fun setModo(ctx: Context, modo: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, modo).apply()
    }
}
