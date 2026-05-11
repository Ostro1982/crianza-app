@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.tudominio.crianza.ui.theme.*
import androidx.compose.ui.unit.dp

private data class ResultadoBusqueda(
    val tipo: String,        // "Gasto", "Evento", "Pendiente", "Compra", "Recuerdo", "Documento"
    val emoji: String,
    val titulo: String,
    val subtitulo: String,
    val ruta: String         // pantalla destino
)

@Composable
fun PantallaBuscador(
    gastos: List<Gasto>,
    eventos: List<Evento>,
    pendientes: List<Pendiente>,
    compras: List<ItemCompra>,
    recuerdos: List<Recuerdo>,
    documentos: List<Documento>,
    onAtras: () -> Unit,
    onIrA: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val resultados = remember(query, gastos, eventos, pendientes, compras, recuerdos, documentos) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim()
            val res = mutableListOf<ResultadoBusqueda>()
            gastos.filter {
                it.descripcion.contains(q, true) ||
                    it.nombrePagador.contains(q, true) ||
                    it.categoria.contains(q, true) ||
                    it.nombresHijos.contains(q, true)
            }.take(20).forEach {
                res += ResultadoBusqueda(
                    "Gasto", "💸",
                    it.descripcion,
                    "${it.nombrePagador} · ${it.fecha} · ${Moneda.formatear(it.monto, MonedaConfig.actual)}",
                    "gastos"
                )
            }
            eventos.filter {
                it.titulo.contains(q, true) ||
                    it.descripcion.contains(q, true) ||
                    it.ubicacion.contains(q, true)
            }.take(20).forEach {
                res += ResultadoBusqueda(
                    "Evento", "📅",
                    it.titulo,
                    "${it.fecha}${it.horaInicio?.let { h -> " · $h" } ?: ""}${if (it.ubicacion.isNotBlank()) " · ${it.ubicacion}" else ""}",
                    "calendario"
                )
            }
            pendientes.filter {
                it.titulo.contains(q, true) || it.asignadoA.contains(q, true)
            }.take(20).forEach {
                res += ResultadoBusqueda(
                    "Pendiente", if (it.titulo.startsWith("🎁")) "🎁" else "📋",
                    it.titulo.removePrefix("🎁").trim(),
                    if (it.completado) "Completado"
                    else if (it.fechaLimite.isNotBlank()) "Límite: ${it.fechaLimite}"
                    else if (it.asignadoA.isNotBlank()) it.asignadoA
                    else "Sin asignar",
                    "pendientes"
                )
            }
            compras.filter {
                it.descripcion.contains(q, true) || it.categoria.contains(q, true)
            }.take(20).forEach {
                res += ResultadoBusqueda(
                    "Compra", "🛒",
                    it.descripcion,
                    if (it.comprado) "Comprado" else "Pendiente · ${it.categoria.ifBlank { "Sin categoría" }}",
                    "compras"
                )
            }
            recuerdos.filter {
                it.titulo.contains(q, true) || it.descripcion.contains(q, true)
            }.take(20).forEach {
                res += ResultadoBusqueda(
                    "Recuerdo", "📸",
                    it.titulo,
                    it.fecha,
                    "recuerdos"
                )
            }
            documentos.filter {
                it.titulo.contains(q, true) || it.descripcion.contains(q, true) || it.categoria.contains(q, true)
            }.take(20).forEach {
                res += ResultadoBusqueda(
                    "Documento", "📄",
                    it.titulo,
                    it.categoria.ifBlank { "Sin categoría" },
                    "documentos"
                )
            }
            res
        }
    }

    Box(Modifier.fillMaxSize().background(BgGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Buscar", color = Neutral10, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar gastos, eventos, pendientes, compras…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                when {
                    query.isBlank() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔍", style = MaterialTheme.typography.displayMedium)
                                Spacer(Modifier.height(12.dp))
                                Text("Buscá lo que necesites", color = NeutralVariant30, fontWeight = FontWeight.Medium)
                                Text(
                                    "Gastos, eventos, pendientes, compras, recuerdos y documentos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeutralVariant50
                                )
                            }
                        }
                    }
                    resultados.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("¯\\_(ツ)_/¯", style = MaterialTheme.typography.headlineMedium, color = NeutralVariant50)
                                Spacer(Modifier.height(8.dp))
                                Text("Nada que coincida", color = NeutralVariant30)
                            }
                        }
                    }
                    else -> {
                        Text(
                            "${resultados.size} resultado${if (resultados.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralVariant50,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(resultados) { r ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GlassWhite)
                                        .clickable { onIrA(r.ruta) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(r.emoji, style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                r.tipo,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            r.titulo,
                                            fontWeight = FontWeight.Medium,
                                            color = Neutral10,
                                            maxLines = 1
                                        )
                                        Text(
                                            r.subtitulo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeutralVariant50,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}
