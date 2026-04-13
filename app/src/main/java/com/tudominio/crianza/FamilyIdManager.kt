package com.tudominio.crianza

import android.content.Context
import java.util.UUID

object FamilyIdManager {

    private const val PREFS_NAME = "crianza_family"
    private const val KEY_FAMILY_ID = "family_id"
    private const val KEY_IS_LINKED = "is_linked"

    fun obtenerFamilyId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_FAMILY_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_FAMILY_ID, id).apply()
        }
        return id
    }

    fun vincularConCodigo(context: Context, codigoCompleto: String): Boolean {
        val limpio = codigoCompleto.trim()
        val esUuid = limpio.matches(Regex("[0-9a-fA-F\\-]{32,36}"))
        if (!esUuid) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FAMILY_ID, limpio)
            .putBoolean(KEY_IS_LINKED, true)
            .putBoolean("subida_inicial_ok", false)
            .apply()
        return true
    }

    fun estaVinculado(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LINKED, false)
    }

    fun necesitaSubidaInicial(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean("subida_inicial_ok", false)
    }

    fun marcarSubidaInicial(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("subida_inicial_ok", true).apply()
    }

    fun resetearSubidaInicial(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("subida_inicial_ok", false).apply()
    }

    fun desvincular(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nuevoId = UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_FAMILY_ID, nuevoId)
            .putBoolean(KEY_IS_LINKED, false)
            .putBoolean("subida_inicial_ok", false)
            .apply()
    }
}
