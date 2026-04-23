package com.tudominio.crianza.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tudominio.crianza.MainActivity
import java.text.SimpleDateFormat
import java.util.*

val WIDGET_TIPO_KEY = stringPreferencesKey("widget_tipo")

// Paleta por sección
private data class WidgetPaleta(
    val fondo: Color,
    val titulo: Color,
    val textoMain: Color,
    val textoSec: Color,
    val acento: Color,
    val divisor: Color,
    val emoji: String
)

private fun paletaPorTipo(tipo: String): WidgetPaleta = when (tipo) {
    "gastos" -> WidgetPaleta(
        Color(0xFFEDE8D8), Color(0xFF2D2A1E), Color(0xFF2D2A1E),
        Color(0xFF5A5636), Color(0xFFC05050), Color(0xFFB8A888), "\uD83D\uDCB0"
    )
    "compras" -> WidgetPaleta(
        Color(0xFFE8E4D0), Color(0xFF2D2A1E), Color(0xFF2D2A1E),
        Color(0xFF5A5636), Color(0xFFA06080), Color(0xFFBCB89C), "\uD83D\uDED2"
    )
    "pendientes" -> WidgetPaleta(
        Color(0xFFE0ECC0), Color(0xFF2D2A1E), Color(0xFF2D2A1E),
        Color(0xFF5A5636), Color(0xFF6A8A30), Color(0xFFB0C890), "\u2705"
    )
    else -> WidgetPaleta(
        Color(0xFFF0E8D0), Color(0xFF2D2A1E), Color(0xFF2D2A1E),
        Color(0xFF5A5636), Color(0xFF8B7640), Color(0xFFD0C090), "\uD83D\uDCC5"
    )
}

data class LineaWidget(val izq: String, val der: String)

class SemillappWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Pre-fetch los 4 datasets
        val dataAgenda = WidgetDataHelper.getEventosProximos(context)
            .map { LineaWidget("${it.labelDia}  ${it.titulo.take(18)}", it.hora) }
        val dataGastos = WidgetDataHelper.getGastos(context)
            .map { LineaWidget(it.descripcion.take(22), "$ ${it.monto.toLong()}") }
        val dataCompras = WidgetDataHelper.getComprasPendientes(context)
            .map { LineaWidget(it.descripcion.take(26), if (it.cantidad != "1") "x${it.cantidad}" else "") }
        val dataPendientes = WidgetDataHelper.getPendientes(context)
            .map { LineaWidget(it.titulo.take(24), it.asignadoA.take(10)) }

        val fechaHoy = SimpleDateFormat("EEE d MMM", Locale("es", "AR"))
            .format(Date()).replaceFirstChar { it.uppercase() }

        provideContent {
            val prefs = currentState<Preferences>()
            val tipo = prefs[WIDGET_TIPO_KEY] ?: "agenda"

            val lineas = when (tipo) {
                "gastos" -> dataGastos
                "compras" -> dataCompras
                "pendientes" -> dataPendientes
                else -> dataAgenda
            }
            val encabezado = when (tipo) {
                "gastos" -> "Gastos"
                "compras" -> "Lista de compras"
                "pendientes" -> "Pendientes"
                else -> "Agenda"
            }
            val pal = paletaPorTipo(tipo)

            val colorFondo = ColorProvider(pal.fondo)
            val colorTitulo = ColorProvider(pal.titulo)
            val colorMain = ColorProvider(pal.textoMain)
            val colorSec = ColorProvider(pal.textoSec)
            val colorAcento = ColorProvider(pal.acento)
            val colorDivisor = ColorProvider(pal.divisor)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(colorFondo)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.TopStart
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            "${pal.emoji} $encabezado",
                            style = TextStyle(
                                color = colorTitulo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            fechaHoy,
                            style = TextStyle(color = colorSec, fontSize = 10.sp)
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Box(
                            modifier = GlanceModifier
                                .size(36.dp)
                                .cornerRadius(18.dp)
                                .background(colorAcento)
                                .clickable(actionRunCallback<CicloTipoAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "›",
                                style = TextStyle(
                                    color = colorFondo,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(4.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colorDivisor)
                    ) {}
                    Spacer(GlanceModifier.height(4.dp))

                    if (lineas.isEmpty()) {
                        Text(
                            "Sin elementos próximos",
                            style = TextStyle(color = colorSec, fontSize = 11.sp)
                        )
                    } else {
                        lineas.forEach { linea ->
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Vertical.CenterVertically
                            ) {
                                Text(
                                    linea.izq,
                                    modifier = GlanceModifier.defaultWeight(),
                                    style = TextStyle(color = colorMain, fontSize = 11.sp),
                                    maxLines = 1
                                )
                                if (linea.der.isNotBlank()) {
                                    Text(
                                        linea.der,
                                        style = TextStyle(color = colorAcento, fontSize = 10.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
