package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tudominio.crianza.ui.theme.*

data class CoachmarkPaso(
    val key: String,
    val titulo: String,
    val descripcion: String
)

val PASOS_COACHMARK = listOf(
    CoachmarkPaso("inbox",     "Inbox",     "Mensajes recientes entre los adultos. Tocá para ver el chat completo."),
    CoachmarkPaso("eventos",   "Eventos",   "Calendario compartido. Próximos compromisos del hijo o de la familia."),
    CoachmarkPaso("compras",   "Compras",   "Lista colaborativa. Cualquiera agrega items, quien compra los marca."),
    CoachmarkPaso("tareas",    "Tareas",    "Pendientes a resolver. Tocá un item para editarlo, el checkbox para completarlo."),
    CoachmarkPaso("cuentas",   "Cuentas",   "Compensaciones entre adultos. Cuánto debe cada uno según los gastos."),
    CoachmarkPaso("finanzas",  "Finanzas",  "Total gastado del mes. Tocá para ver detalle y cargar nuevo gasto.")
)

@Composable
fun CoachmarkHome(
    rects: Map<String, Rect>,
    onTerminar: () -> Unit
) {
    var indice by remember { mutableStateOf(0) }
    val paso = PASOS_COACHMARK[indice]
    val rect = rects[paso.key]
    val density = LocalDensity.current
    val ultimo = indice == PASOS_COACHMARK.lastIndex
    var screenHeightPx by remember { mutableStateOf(0) }
    var tooltipHeightPx by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { screenHeightPx = it.size.height }
    ) {
        // Dim + cutout
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.75f))
            rect?.let { r ->
                val pad = with(density) { 6.dp.toPx() }
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(r.left - pad, r.top - pad),
                    size = Size(r.width + pad * 2, r.height + pad * 2),
                    cornerRadius = CornerRadius(with(density) { 20.dp.toPx() }),
                    blendMode = BlendMode.Clear
                )
            }
        }

        // Tooltip — posición dinámica: arriba si rect en mitad inferior, abajo si en superior
        val gapPx = with(density) { 16.dp.toPx() }
        val tooltipOffsetY: Int = if (rect != null && screenHeightPx > 0 && tooltipHeightPx > 0) {
            val rectMid = (rect.top + rect.bottom) / 2f
            if (rectMid < screenHeightPx / 2f) {
                // Card en mitad superior → tooltip abajo del card
                (rect.bottom + gapPx).toInt().coerceAtMost(screenHeightPx - tooltipHeightPx - gapPx.toInt())
            } else {
                // Card en mitad inferior → tooltip arriba del card
                (rect.top - tooltipHeightPx - gapPx).toInt().coerceAtLeast(gapPx.toInt())
            }
        } else {
            0
        }

        Surface(
            modifier = Modifier
                .offset { IntOffset(0, tooltipOffsetY) }
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { tooltipHeightPx = it.size.height },
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "${indice + 1} / ${PASOS_COACHMARK.size}",
                    fontSize = 12.sp,
                    color = NeutralVariant50,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    paso.titulo,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo20
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    paso.descripcion,
                    fontSize = 14.sp,
                    color = NeutralVariant30,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onTerminar) {
                        Text("Saltar", color = NeutralVariant50)
                    }
                    Spacer(Modifier.weight(1f))
                    if (indice > 0) {
                        OutlinedButton(
                            onClick = { indice-- },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Anterior", color = Indigo40)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            if (ultimo) onTerminar()
                            else indice++
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo40)
                    ) {
                        Text(
                            if (ultimo) "Listo" else "Siguiente",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
