package com.tudominio.crianza.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SemillappWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Si ya hay un tipo guardado, saltar la config (actualiza y cierra)
        val prefs = getSharedPreferences("widget_prefs", MODE_PRIVATE)
        if (prefs.contains("widget_tipo")) {
            confirmarYCerrar(prefs.getString("widget_tipo", "agenda") ?: "agenda")
            return
        }

        setContent { ConfigWidgetScreen(::confirmarYCerrar) }
    }

    private fun confirmarYCerrar(tipo: String) {
        getSharedPreferences("widget_prefs", MODE_PRIVATE)
            .edit().putString("widget_tipo", tipo).apply()

        CoroutineScope(Dispatchers.IO).launch {
            SemillappWidget().updateAll(this@SemillappWidgetConfigActivity)
        }

        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, result)
        finish()
    }
}

@Composable
private fun ConfigWidgetScreen(onElegir: (String) -> Unit) {
    val opciones = listOf(
        Triple("agenda", "Agenda semanal", "Próximos eventos y actividades"),
        Triple("gastos", "Gastos", "Últimos gastos registrados"),
        Triple("compras", "Lista de compras", "Ítems pendientes de comprar"),
        Triple("pendientes", "Pendientes", "Tareas sin completar")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111B0A))
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "\uD83C\uDF31 Semillapp",
            color = Color(0xFFD4EDCA),
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "¿Qué mostrará el widget?",
            color = Color(0xFF8BBF6A),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 28.dp)
        )

        opciones.forEach { (tipo, titulo, subtitulo) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A2E10))
                    .clickable { onElegir(tipo) }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text(titulo, color = Color(0xFFD4EDCA), fontSize = 14.sp)
                Spacer(Modifier.height(3.dp))
                Text(subtitulo, color = Color(0xFF8BBF6A), fontSize = 11.sp)
            }
        }
    }
}
