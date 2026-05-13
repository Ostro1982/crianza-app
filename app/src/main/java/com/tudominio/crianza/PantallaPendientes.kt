@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.tudominio.crianza

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    var busqueda by remember { mutableStateOf("") }
    // Filtro: "todos" | "tareas" | "regalos"
    var filtroTipo by remember { mutableStateOf("todos") }

    fun esRegalo(p: Pendiente) = p.titulo.startsWith("🎁")
    fun precioRegalo(p: Pendiente): Double? {
        val m = Regex("·\\s*\\$([0-9.,]+)\\s*$").find(p.titulo) ?: return null
        return m.groupValues[1].replace(".", "").replace(",", ".").toDoubleOrNull()
    }
    fun tituloSinPrecio(p: Pendiente): String =
        p.titulo.replace(Regex("\\s*·\\s*\\$[0-9.,]+\\s*$"), "").removePrefix("🎁").trim()

    val basePorTipo = when (filtroTipo) {
        "tareas" -> pendientes.filterNot { esRegalo(it) }
        "regalos" -> pendientes.filter { esRegalo(it) }
        else -> pendientes
    }
    val filtrados = if (busqueda.isBlank()) basePorTipo
    else basePorTipo.filter {
        it.titulo.contains(busqueda, ignoreCase = true) ||
            it.asignadoA.contains(busqueda, ignoreCase = true)
    }
    val noCompletados = filtrados.filter { !it.completado }
    val completados = filtrados.filter { it.completado }

    Box(Modifier.fillMaxSize().background(BgGrad3)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Pendientes", color = Neutral10, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
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
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar…") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    FilterChip(
                        selected = filtroTipo == "todos",
                        onClick = { filtroTipo = "todos" },
                        label = { Text("Todos") }
                    )
                    FilterChip(
                        selected = filtroTipo == "tareas",
                        onClick = { filtroTipo = "tareas" },
                        label = { Text("Tareas") }
                    )
                    FilterChip(
                        selected = filtroTipo == "regalos",
                        onClick = { filtroTipo = "regalos" },
                        label = { Text("🎁 Regalos") }
                    )
                }
                if (filtroTipo == "regalos" && filtrados.isNotEmpty()) {
                    val totalGastado = filtrados.filter { it.completado }.sumOf { precioRegalo(it) ?: 0.0 }
                    val totalPendiente = filtrados.filterNot { it.completado }.sumOf { precioRegalo(it) ?: 0.0 }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (totalGastado > 0) {
                            Text(
                                "Gastado: ${Moneda.formatear(totalGastado, MonedaConfig.actual)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Teal40,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (totalPendiente > 0) {
                            Text(
                                "Por comprar: ${Moneda.formatear(totalPendiente, MonedaConfig.actual)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Rose40,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
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
                            Box(Modifier.animateItemPlacement()) {
                            SwipeParaBorrar(onEliminar = { onEliminar(pendiente) }) {
                                TarjetaPendiente(
                                    pendiente = pendiente,
                                    onToggle = {
                                        val nuevo = !pendiente.completado
                                        onActualizar(pendiente.copy(
                                            completado = nuevo,
                                            fechaCompletado = if (nuevo) System.currentTimeMillis() else 0L
                                        ))
                                    },
                                    onEditar = { pendienteEditando = pendiente },
                                    onEliminar = { onEliminar(pendiente) }
                                )
                            }
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
                                    Box(Modifier.animateItemPlacement()) {
                                    SwipeParaBorrar(onEliminar = { onEliminar(pendiente) }) {
                                        TarjetaPendiente(
                                            pendiente = pendiente,
                                            onToggle = {
                                        val nuevo = !pendiente.completado
                                        onActualizar(pendiente.copy(
                                            completado = nuevo,
                                            fechaCompletado = if (nuevo) System.currentTimeMillis() else 0L
                                        ))
                                    },
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
    }

    if (mostrarDialogo || pendienteEditando != null) {
        DialogoPendiente(
            pendiente = pendienteEditando,
            padres = padres,
            tipoInicial = if (filtroTipo == "regalos") "regalo" else "tarea",
            onDismiss = { mostrarDialogo = false; pendienteEditando = null },
            onGuardar = { titulo, fechaLimite, asignadoA, frecuenciaDias, esRegalo ->
                val tituloLimpio = titulo.removePrefix("🎁").trim()
                val tituloFinal = if (esRegalo) "🎁 $tituloLimpio" else tituloLimpio
                val p = if (pendienteEditando != null) {
                    pendienteEditando!!.copy(
                        titulo = tituloFinal,
                        fechaLimite = fechaLimite,
                        asignadoA = asignadoA,
                        frecuenciaDias = frecuenciaDias
                    )
                } else {
                    Pendiente(
                        titulo = tituloFinal,
                        fechaLimite = fechaLimite,
                        asignadoA = asignadoA,
                        frecuenciaDias = frecuenciaDias
                    )
                }
                if (pendienteEditando != null) onActualizar(p) else onAgregar(p)
                mostrarDialogo = false
                pendienteEditando = null
            },
            onEliminar = pendienteEditando?.let { p -> { onEliminar(p); pendienteEditando = null } }
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
                val esR = pendiente.titulo.startsWith("🎁")
                val precioMatch = Regex("·\\s*\\$([0-9.,]+)\\s*$").find(pendiente.titulo)
                val precio = precioMatch?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull()
                val tituloLimpio = pendiente.titulo
                    .replace(Regex("\\s*·\\s*\\$[0-9.,]+\\s*$"), "")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tituloLimpio,
                        fontWeight = FontWeight.Medium,
                        color = Neutral10,
                        textDecoration = if (pendiente.completado) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (precio != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            Moneda.formatear(precio, MonedaConfig.actual),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (pendiente.completado) Teal40 else Rose40,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                val detalles = listOfNotNull(
                    if (esR) (if (pendiente.completado) "Comprado ✓" else "Por comprar") else null,
                    pendiente.asignadoA.takeIf { it.isNotEmpty() && !esR },
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
    tipoInicial: String = "tarea",
    onDismiss: () -> Unit,
    onGuardar: (titulo: String, fechaLimite: String, asignadoA: String, frecuenciaDias: Int, esRegalo: Boolean) -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    val tituloOriginal = pendiente?.titulo ?: ""
    val regaloInicial = tituloOriginal.startsWith("🎁") || (pendiente == null && tipoInicial == "regalo")
    val precioInicial = run {
        val m = Regex("·\\s*\\$([0-9.,]+)\\s*$").find(tituloOriginal)
        m?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull()
    }
    val tituloSinPrecioInicial = tituloOriginal
        .replace(Regex("\\s*·\\s*\\$[0-9.,]+\\s*$"), "")
        .removePrefix("🎁").trim()
    var titulo by remember { mutableStateOf(tituloSinPrecioInicial) }
    var fechaLimite by remember { mutableStateOf(pendiente?.fechaLimite ?: "") }
    var asignadoA by remember { mutableStateOf(pendiente?.asignadoA ?: "") }
    var frecuenciaDias by remember { mutableIntStateOf(pendiente?.frecuenciaDias ?: 0) }
    var esRegalo by remember { mutableStateOf(regaloInicial) }
    var precioTexto by remember { mutableStateOf(precioInicial?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pendiente == null) (if (esRegalo) "Nuevo regalo" else "Nuevo pendiente") else "Editar pendiente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !esRegalo,
                        onClick = { esRegalo = false },
                        label = { Text("Tarea") }
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = esRegalo,
                        onClick = { esRegalo = true },
                        label = { Text("🎁 Regalo") }
                    )
                }
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text(if (esRegalo) "Regalo" else "Tarea") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(if (esRegalo) "Ej: Lego para Juan, libro para Sofía…"
                            else "Ej: Llevar al pediatra, trámite DNI…")
                    },
                    trailingIcon = {
                        IconoVoz(onTexto = { titulo = it })
                    }
                )
                if (esRegalo) {
                    OutlinedTextField(
                        value = precioTexto,
                        onValueChange = { precioTexto = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10) },
                        label = { Text("Precio (opcional)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                Text("Se repite:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        0 to "No",
                        1 to "Diario",
                        7 to "Semanal",
                        30 to "Mensual"
                    ).forEach { (dias, label) ->
                        FilterChip(
                            selected = frecuenciaDias == dias,
                            onClick = { frecuenciaDias = dias },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val precioVal = precioTexto.replace(",", ".").toDoubleOrNull()
                    val tituloConPrecio = if (esRegalo && precioVal != null && precioVal > 0) {
                        val fmt = if (precioVal == precioVal.toLong().toDouble())
                            precioVal.toLong().toString()
                        else precioVal.toString()
                        "${titulo.trim()} · \$$fmt"
                    } else titulo.trim()
                    onGuardar(tituloConPrecio, fechaLimite.trim(), asignadoA, frecuenciaDias, esRegalo)
                },
                enabled = titulo.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (pendiente != null && onEliminar != null) {
                    TextButton(
                        onClick = onEliminar,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}
