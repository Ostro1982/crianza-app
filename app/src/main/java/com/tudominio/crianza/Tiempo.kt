@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tudominio.crianza.ui.theme.BgGrad0
import com.tudominio.crianza.ui.theme.CrianzaTheme
import com.tudominio.crianza.ui.theme.GlassWhite
import com.tudominio.crianza.ui.theme.GlassWhiteHeavy
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PantallaTiempo(
    hijos: List<Hijo>,
    padres: List<Padre>,
    registros: List<RegistroTiempo>,
    onAgregarRegistro: (RegistroTiempo) -> Unit,
    onAgregarMultiplesRegistros: (List<RegistroTiempo>) -> Unit,
    onEliminarRegistro: (String) -> Unit,
    onEditarRegistro: (RegistroTiempo) -> Unit,
    onVerResumen: () -> Unit,
    onAtras: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var registroEditando by remember { mutableStateOf<RegistroTiempo?>(null) }

    Box(Modifier.fillMaxSize().background(BgGrad0)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Registro de tiempo", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onVerResumen) {
                        Icon(Icons.Default.Info, contentDescription = "Resumen", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogo = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agregar registro") }
            )
        }
    ) { paddingValues ->
        if (registros.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏱️", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Sin registros de tiempo",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        "Tocá + para anotar tiempo con los niños",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(registros.sortedByDescending { it.fechaCompleta }) { registro ->
                    TarjetaRegistroTiempo(
                        registro = registro,
                        onEliminar = { onEliminarRegistro(registro.id) },
                        onEditar = { registroEditando = registro }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
    } // Box

    if (mostrarDialogo || registroEditando != null) {
        DialogoRegistroTiempo(
            hijos = hijos,
            padres = padres,
            registroExistente = registroEditando,
            onDismiss = {
                mostrarDialogo = false
                registroEditando = null
            },
            onGuardar = { nuevoRegistro, esTodosLosHijos ->
                if (esTodosLosHijos && registroEditando == null) {
                    val registrosMultiples = hijos.map { hijo ->
                        nuevoRegistro.copy(
                            id = UUID.randomUUID().toString(),
                            idHijo = hijo.id,
                            nombreHijo = hijo.nombre,
                            esTodosLosHijos = true
                        )
                    }
                    onAgregarMultiplesRegistros(registrosMultiples)
                } else if (registroEditando != null) {
                    onEditarRegistro(nuevoRegistro)
                } else {
                    onAgregarRegistro(nuevoRegistro)
                }
                mostrarDialogo = false
                registroEditando = null
            }
        )
    }
}

@Composable
fun TarjetaRegistroTiempo(
    registro: RegistroTiempo,
    onEliminar: () -> Unit,
    onEditar: () -> Unit
) {
    // Calcular horas
    val horasFmt = run {
        val ini = registro.horaInicio.split(":").map { it.toIntOrNull() ?: 0 }
        val fin = registro.horaFin.split(":").map { it.toIntOrNull() ?: 0 }
        if (ini.size >= 2 && fin.size >= 2) {
            val mins = (fin[0] * 60 + fin[1]) - (ini[0] * 60 + ini[1])
            if (mins > 0) {
                val h = mins / 60; val m = mins % 60
                if (m == 0) "${h}h" else "${h}h ${m}m"
            } else null
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge de horas con estilo mejorado
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassWhiteHeavy),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                    if (horasFmt != null) {
                        Text(
                            horasFmt,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (registro.esTodosLosHijos) "Todos los niños" else registro.nombreHijo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        registro.nombrePadre,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    "${registro.fecha}  ·  ${registro.horaInicio}–${registro.horaFin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFF87171)
                )
            }
        }
    }
}

@Composable
fun DialogoRegistroTiempo(
    hijos: List<Hijo>,
    padres: List<Padre>,
    registroExistente: RegistroTiempo?,
    onDismiss: () -> Unit,
    onGuardar: (RegistroTiempo, Boolean) -> Unit
) {
    var hijoSeleccionado by remember { mutableStateOf(registroExistente?.idHijo ?: "") }
    var padreSeleccionado by remember { mutableStateOf(registroExistente?.idPadre ?: "") }
    var fecha by remember { mutableStateOf(registroExistente?.fecha ?: obtenerFechaActual()) }
    var horaInicio by remember { mutableStateOf(registroExistente?.horaInicio ?: "") }
    var horaFin by remember { mutableStateOf(registroExistente?.horaFin ?: "") }
    var esTodosLosHijos by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (registroExistente == null) "Nuevo registro" else "Editar registro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (registroExistente == null && hijos.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = esTodosLosHijos, onCheckedChange = { esTodosLosHijos = it })
                        Text("Todos los niños (un registro por hijo)")
                    }
                }

                if (!esTodosLosHijos || registroExistente != null) {
                    if (hijos.isNotEmpty()) {
                        var expandedHijo by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedHijo, onExpandedChange = { expandedHijo = it }) {
                            OutlinedTextField(
                                value = hijos.find { it.id == hijoSeleccionado }?.nombre ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Niño/a") },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expandedHijo, onDismissRequest = { expandedHijo = false }) {
                                hijos.forEach { hijo ->
                                    DropdownMenuItem(
                                        text = { Text(hijo.nombre) },
                                        onClick = { hijoSeleccionado = hijo.id; expandedHijo = false }
                                    )
                                }
                            }
                        }
                    }
                }

                if (padres.isNotEmpty()) {
                    var expandedPadre by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedPadre, onExpandedChange = { expandedPadre = it }) {
                        OutlinedTextField(
                            value = padres.find { it.id == padreSeleccionado }?.nombre ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Responsable") },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expandedPadre, onDismissRequest = { expandedPadre = false }) {
                            padres.forEach { padre ->
                                DropdownMenuItem(
                                    text = { Text(padre.nombre) },
                                    onClick = { padreSeleccionado = padre.id; expandedPadre = false }
                                )
                            }
                        }
                    }
                }

                CampoFecha(
                    value = fecha,
                    label = "Fecha",
                    onValueChange = { fecha = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoHora(
                        value = horaInicio,
                        label = "Desde",
                        onValueChange = { horaInicio = it },
                        modifier = Modifier.weight(1f)
                    )
                    CampoHora(
                        value = horaFin,
                        label = "Hasta",
                        onValueChange = { horaFin = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Preview de horas calculadas
                val hIni = normalizarHora(horaInicio)
                val hFin = normalizarHora(horaFin)
                val iniParts = hIni.split(":").map { it.toIntOrNull() ?: 0 }
                val finParts = hFin.split(":").map { it.toIntOrNull() ?: 0 }
                if (iniParts.size >= 2 && finParts.size >= 2 && horaInicio.isNotBlank() && horaFin.isNotBlank()) {
                    val mins = (finParts[0] * 60 + finParts[1]) - (iniParts[0] * 60 + iniParts[1])
                    if (mins > 0) {
                        val h = mins / 60; val m = mins % 60
                        val texto = if (m == 0) "$h horas" else "$h h $m min"
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "⏱ $texto  ($hIni – $hFin)",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val padre = padres.find { it.id == padreSeleccionado }
                    val hInorm = normalizarHora(horaInicio)
                    val hFnorm = normalizarHora(horaFin)
                    if (padre != null) {
                        if (esTodosLosHijos && registroExistente == null) {
                            onGuardar(
                                RegistroTiempo(
                                    id = UUID.randomUUID().toString(),
                                    idHijo = "", nombreHijo = "",
                                    idPadre = padre.id, nombrePadre = padre.nombre,
                                    fecha = fecha, horaInicio = hInorm, horaFin = hFnorm,
                                    fechaCompleta = System.currentTimeMillis(), esTodosLosHijos = false
                                ), true
                            )
                        } else {
                            val hijo = hijos.find { it.id == hijoSeleccionado }
                            if (hijo != null || registroExistente != null) {
                                onGuardar(
                                    RegistroTiempo(
                                        id = registroExistente?.id ?: UUID.randomUUID().toString(),
                                        idHijo = registroExistente?.idHijo ?: hijo!!.id,
                                        nombreHijo = registroExistente?.nombreHijo ?: hijo!!.nombre,
                                        idPadre = padre.id, nombrePadre = padre.nombre,
                                        fecha = fecha, horaInicio = hInorm, horaFin = hFnorm,
                                        fechaCompleta = registroExistente?.fechaCompleta ?: System.currentTimeMillis(),
                                        esTodosLosHijos = registroExistente?.esTodosLosHijos ?: false
                                    ), false
                                )
                            }
                        }
                    }
                },
                enabled = when {
                    esTodosLosHijos && registroExistente == null ->
                        padreSeleccionado.isNotEmpty() && fecha.isNotEmpty() && horaInicio.isNotEmpty() && horaFin.isNotEmpty()
                    registroExistente != null ->
                        padreSeleccionado.isNotEmpty() && fecha.isNotEmpty() && horaInicio.isNotEmpty() && horaFin.isNotEmpty()
                    else ->
                        hijoSeleccionado.isNotEmpty() && padreSeleccionado.isNotEmpty() && fecha.isNotEmpty() && horaInicio.isNotEmpty() && horaFin.isNotEmpty()
                }
            ) {
                Text(if (registroExistente == null) "Guardar" else "Actualizar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Preview(showBackground = true)
@Composable
fun PantallaTiempoPreview() {
    CrianzaTheme {
        PantallaTiempo(
            hijos = listOf(Hijo(id = "1", nombre = "Hijo 1"), Hijo(id = "2", nombre = "Hijo 2")),
            padres = listOf(Padre(id = "1", nombre = "Padre 1"), Padre(id = "2", nombre = "Padre 2")),
            registros = listOf(),
            onAgregarRegistro = {},
            onAgregarMultiplesRegistros = {},
            onEliminarRegistro = {},
            onEditarRegistro = {},
            onVerResumen = {},
            onAtras = {}
        )
    }
}
