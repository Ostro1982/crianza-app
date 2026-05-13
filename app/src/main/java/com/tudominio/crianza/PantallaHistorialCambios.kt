@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tudominio.crianza.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla "Historial de cambios": lista cronológica de todas las ediciones a registros
 * de tiempo. Convierte un dato interno (RegistroEdicion) en evidencia visible para el
 * otro adulto y, eventualmente, para mediación.
 *
 * Cada item muestra:
 * - Qué cambió (fecha/hora del registro original).
 * - Cuándo se editó.
 * - Quién (idPadre del registro actual).
 *
 * Solo lectura: no se puede borrar desde acá (preserva integridad del registro).
 */
@Composable
fun PantallaHistorialCambios(
    onAtras: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var ediciones by remember { mutableStateOf(listOf<RegistroEdicion>()) }
    var registrosPorId by remember { mutableStateOf(mapOf<String, RegistroTiempo>()) }
    val sdf = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val all = db.registroEdicionDao().obtenerTodos()
            val regs = db.registroTiempoDao().obtenerTodosLosRegistros().associateBy { it.id }
            ediciones = all
            registrosPorId = regs
        }
    }

    Box(Modifier.fillMaxSize().background(BgGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Historial de cambios", color = Neutral10) },
                    navigationIcon = {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (ediciones.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Todavía no hay cambios registrados.",
                        color = NeutralVariant30
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(ediciones, key = { it.id }) { ed ->
                        ItemEdicion(ed, registrosPorId[ed.idRegistro], sdf)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemEdicion(
    ed: RegistroEdicion,
    registroActual: RegistroTiempo?,
    sdf: SimpleDateFormat
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Editado el ${sdf.format(Date(ed.fechaEdicion))}",
                fontWeight = FontWeight.Bold,
                color = Neutral10,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "Antes: ${ed.nombreHijoAnterior} · ${ed.fechaAnterior} · ${ed.horaInicioAnterior}–${ed.horaFinAnterior}" +
                    if (ed.autocompensadoAnterior) " · autocompensado" else "",
                color = NeutralVariant30,
                style = MaterialTheme.typography.bodySmall
            )
            if (registroActual != null) {
                Text(
                    "Ahora: ${registroActual.nombreHijo} · ${registroActual.fecha} · " +
                        "${registroActual.horaInicio}–${registroActual.horaFin}" +
                        if (registroActual.autocompensado) " · autocompensado" else "",
                    color = NeutralVariant30,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Por: ${registroActual.nombrePadre}",
                    color = NeutralVariant50,
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                Text(
                    "Registro original eliminado.",
                    color = NeutralVariant50,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
