package com.tudominio.crianza.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

class CicloTipoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val tipos = listOf("agenda", "gastos", "compras", "pendientes")
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val actual = prefs[WIDGET_TIPO_KEY] ?: "agenda"
            val siguiente = tipos[(tipos.indexOf(actual) + 1) % tipos.size]
            prefs.toMutablePreferences().apply { this[WIDGET_TIPO_KEY] = siguiente }
        }
        SemillappWidget().update(context, glanceId)
    }
}
