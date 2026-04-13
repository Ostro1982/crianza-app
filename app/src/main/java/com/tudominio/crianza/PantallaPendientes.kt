@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tudominio.crianza.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PantallaPendientes(
    pendientes: List<Pendiente>,
    padres: List<Padre>,
    onAgregar: (Pendiente) -> Unit,
    onActualizar: (Pendiente) -> Unit,
    onEliminar: (Pendiente) -> Unit,
    onAtras: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var pendienteEditando by remember { mutableStateOf<Pendiente?>(null) }
    var mostrarCompletados by remember { mutableStateOf(false) }

    val noCompletados = pendientes.filter { !it.completado }
    val completados = pendientes.filter { it.completado }

    Box(Modifier.fillMaxSize().background(BgGrad3)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Pendientes", color = Neutral10, fontWeight = FontWeight.Bold) },
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
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                if (pendientes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✓", style = MaterialTheme.typography.displayLarge, color = NeutralVariant50)
                            Spacer(Modifier.height(8.dp))
                            Text("Sin pendientes", color = NeutralVariant30)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tocá + para agregar tareas",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralVariant50
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.animateContentSize()
                    ) {
                        items(noCompletados, key = { it.id }) { pendiente ->
                            SwipeParaBorrar(onEliminar = { onEliminar(pendiente) }) {
                                TarjetaPendiente(
                                    pendiente = pendiente,
                                    onToggle = { onActualizar(pendiente.copy(completado = !pendiente.completado)) },
                                    onEditar = { pendienteEditando = pendiente },
                                    onEliminar = { onEliminar(pendiente) }
                                )
                            }
                        }

                        if (completados.isNotEmpty()) {
                            item {
                                TextButton(
                                    onClick = { mostrarCompletados = !mostrarCompletados },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(
                                        if (mostrarCompletados) "Ocultar completados (${completados.size})"
                                        else "Mostrar completados (${completados.size})",
                                        color = NeutralVariant30
                                    )
                                }
                            }

                            if (mostrarCompletados) {
                                items(completados, key = { it.id }) { pendiente ->
                                    SwipeParaBorrar(onEliminar = { onEliminar(pendiente) }) {
                                        TarjetaPendiente(
                                            pendiente = pendiente,
                                            onToggle = { onActualizar(pendiente.copy(completado = !pendiente.completado)) },
                                            onEditar = { pendienteEditando = pendiente },
                                            onEliminar = { onEliminar(pendiente) }
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

    if (mostrarDialogo || pendienteEditando != null) {
        DialogoPendiente(
            pendiente = pendienteEditando,
            padres = padres,
            onDismiss = { mostrarDialogo = false; pendienteEditando = null },
            onGuardar = { titulo, fechaLimite, asignadoA ->
                val p = if (pendienteEditando != null) {
                    pendienteEditando!!.copy(
                        titulo = titulo,
                        fechaLimite = fechaLimite,
                        asignadoA = asignadoA
                    )
                } else {
                    Pendiente(titulo = titulo, fechaLimite = fechaLimite, asignadoA = asignadoA)
                }
                if (pendienteEditando != null) onActualizar(p) else onAgregar(p)
                mostrarDialogo = false
                pendienteEditando = null
            }
        )
    }
}

@Composable
fun TarjetaPendiente(
    pendiente: Pendiente,
    onToggle: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite)
            .clickable(onClick = onEditar)
            .alpha(if (pendiente.completado) 0.6f else 1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (pendiente.completado) Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (pendiente.completado) "Desmarcar" else "Completar",
                    tint = if (pendiente.completado) Teal80 else NeutralVariant50
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pendiente.titulo,
                    fontWeight = FontWeight.Medium,
                    color = Neutral10,
                    textDecoration = if (pendiente.completado) TextDecoration.LineThrough else TextDecoration.None
                )
                val detalles = listOfNotNull(
                    pendiente.asignadoA.takeIf { it.isNotEmpty() },
                    pendiente.fechaLimite.takeIf { it.isNotEmpty() }?.let {
                        try {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                            "Límite: ${sdf.format(date!!)}"
                        } catch (_: Exception) { null }
                    }
                ).joinToString(" · ")
                if (detalles.isNotEmpty()) {
                    Text(
                        detalles,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant50
                    )
                }
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                    tint = Red40, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun DialogoPendiente(
    pendiente: Pendiente?,
    padres: List<Padre>,
    onDismiss: () -> Unit,
    onGuardar: (titulo: String, fechaLimite: String, asignadoA: String) -> Unit
) {
    var titulo by remember { mutableStateOf(pendiente?.titulo ?: "") }
    var fechaLimite by remember { mutableStateOf(pendiente?.fechaLimite ?: "") }
    var asignadoA by remember { mutableStateOf(pendiente?.asignadoA ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pendiente == null) "Nuevo pendiente" else "Editar pendiente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Tarea") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: Llevar al pediatra, trámite DNI…") }
                )
                CampoFecha(
                    value = fechaLimite,
                    label = "Fecha límite (opcional)",
                    onValueChange = { fechaLimite = it },
                    modifier = Modifier.fillMaxWidth()
                )
                if (padres.isNotEmpty()) {
                    Text("Asignar a:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = asignadoA.isEmpty(),
                            onClick = { asignadoA = "" },
                            label = { Text("Ambos") }
                        )
                        padres.forEach { padre ->
                            FilterChip(
                                selected = asignadoA == padre.nombre,
                                onClick = { asignadoA = padre.nombre },
                                label = { Text(padre.nombre) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(titulo.trim(), fechaLimite.trim(), asignadoA) },
                enabled = titulo.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
