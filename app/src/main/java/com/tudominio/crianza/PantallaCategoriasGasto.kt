@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import kotlinx.coroutines.launch

@Composable
fun PantallaCategoriasGasto(onAtras: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    var categorias by remember { mutableStateOf(listOf<CategoriaGasto>()) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { categorias = db.categoriaGastoDao().obtenerTodas() }

    Box(Modifier.fillMaxSize().background(BgGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Categorías de gasto", color = Neutral10) },
                    navigationIcon = {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                        }
                    },
                    actions = {
                        IconButton(onClick = { mostrarDialogo = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar", tint = NeutralVariant30)
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
                Text(
                    "Tocá + para agregar. Los gastos pasados conservan su categoría aunque la elimines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant50,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categorias, key = { it.nombre }) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassWhite)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                cat.emoji.ifBlank { "📦" },
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                cat.nombre,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                                color = Neutral10
                            )
                            IconButton(onClick = {
                                scope.launch {
                                    db.categoriaGastoDao().eliminar(cat)
                                    categorias = db.categoriaGastoDao().obtenerTodas()
                                }
                            }) {
                                Icon(Icons.Default.Delete, "Eliminar", tint = Red40)
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        var nombreNueva by remember { mutableStateOf("") }
        var emojiNueva by remember { mutableStateOf("📦") }
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Nueva categoría") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreNueva,
                        onValueChange = { nombreNueva = it.take(30) },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ej: Cuotas, Cumple, Vacaciones…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emojiNueva,
                        onValueChange = { emojiNueva = it.take(4) },
                        label = { Text("Emoji (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = nombreNueva.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val ordenMax = (categorias.maxOfOrNull { it.orden } ?: -1) + 1
                            db.categoriaGastoDao().insertar(
                                CategoriaGasto(nombre = nombreNueva.trim(), emoji = emojiNueva.trim(), orden = ordenMax)
                            )
                            categorias = db.categoriaGastoDao().obtenerTodas()
                            mostrarDialogo = false
                        }
                    }
                ) { Text("Agregar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }
}
