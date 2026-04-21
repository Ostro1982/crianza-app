@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tudominio.crianza.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PantallaTiempo(
    hijos: List<Hijo>,
    padres: List<Padre>,
    registros: List<RegistroTiempo>,
    configuracion: ConfiguracionTiempo = ConfiguracionTiempo(),
    onAgregarRegistro: (RegistroTiempo) -> Unit,
    onAgregarMultiplesRegistros: (List<RegistroTiempo>) -> Unit,
    onEliminarRegistro: (String) -> Unit,
    onEditarRegistro: (RegistroTiempo) -> Unit,
    onVerHistorial: () -> Unit = {},
    onAtras: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var registroEditando by remember { mutableStateOf<RegistroTiempo?>(null) }
    var mostrarResumen by remember { mutableStateOf(false) }

    val registrosOrdenados = remember(registros) { registros.sortedByDescending { it.fechaCompleta } }
    val ultimos2 = registrosOrdenados.take(2)

    Box(Modifier.fillMaxSize().background(BgGrad0)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Registro de tiempo", color = Neutral10) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarResumen = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Resumen", tint = NeutralVariant30)
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Mini gráfico de distribución
            if (registros.isNotEmpty() && padres.size >= 2) {
                MiniBaraCuidado(
                    registros = registros,
                    padres = padres,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            // Cuerpo principal
            if (registros.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏱️", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Sin registros de tiempo",
                            style = MaterialTheme.typography.titleMedium,
                            color = Neutral10
                        )
                        Text(
                            "Tocá + para anotar tiempo con los niños",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralVariant30
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
                // Últimos 2 registros (más compactos)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Últimos registros",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeutralVariant50,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    ultimos2.forEach { registro ->
                        TarjetaRegistroTiempoCompacta(
                            registro = registro,
                            onEditar = { registroEditando = registro }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVerHistorial() }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = NeutralVariant30
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Ver historial completo (${registros.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralVariant30,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
                Spacer(Modifier.height(80.dp)) // FAB space
            }
        }
    }
    } // Box

    // ── BottomSheet de resumen ────────────────────────────────────────────────
    if (mostrarResumen) {
        ModalBottomSheet(
            onDismissRequest = { mostrarResumen = false },
            containerColor = Color(0xFFD4EDCA),
            scrimColor = Color(0x801A2E10)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Resumen de tiempo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Neutral10
                )
                if (padres.size >= 2 && registros.isNotEmpty()) {
                    val horasPorPadre = calcularHorasPorPadre(registros)
                    val totalH = horasPorPadre.values.sum()
                    padres.forEach { padre ->
                        val horas = horasPorPadre[padre.id] ?: 0.0
                        val pct = if (totalH > 0) (horas / totalH * 100).toInt() else 0
                        val obj = if (padre == padres[0]) configuracion.porcentajePadre1 else configuracion.porcentajePadre2
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(padre.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Neutral10)
                            Text(
                                "${String.format("%.1f", horas)} hs · $pct% (obj $obj%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralVariant30
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(NeutralVariant80.copy(0.3f))) {
                            Box(Modifier.fillMaxWidth((pct / 100f).coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Indigo40))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        "Total: ${String.format("%.1f", registros.size.toDouble())} registros · ${String.format("%.1f", horasPorPadre.values.sum())} hs",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeutralVariant50
                    )
                } else {
                    Text("Sin datos suficientes para mostrar resumen.", style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                }
            }
        }
    }

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

// ── Tarjeta compacta (para últimos 2 en pantalla principal) ──────────────────
@Composable
fun TarjetaRegistroTiempoCompacta(
    registro: RegistroTiempo,
    onEditar: () -> Unit
) {
    val horasFmt = run {
        val ini = registro.horaInicio.split(":").map { it.toIntOrNull() ?: 0 }
        val fin = registro.horaFin.split(":").map { it.toIntOrNull() ?: 0 }
        if (ini.size >= 2 && fin.size >= 2) {
            val mins = (fin[0] * 60 + fin[1]) - (ini[0] * 60 + ini[1])
            if (mins > 0) { val h = mins / 60; val m = mins % 60; if (m == 0) "${h}h" else "${h}h ${m}m" } else null
        } else null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite)
            .clickable { onEditar() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (registro.esTodosLosHijos) "Todos los niños" else registro.nombreHijo,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Neutral10
            )
            Text(
                "${registro.fecha} · ${registro.horaInicio}–${registro.horaFin}",
                style = MaterialTheme.typography.labelSmall,
                color = NeutralVariant50
            )
        }
        if (horasFmt != null) {
            Text(horasFmt, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeutralVariant30)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = NeutralVariant50)
    }
}

// ── Historial completo de registros de tiempo ────────────────────────────────
@Composable
fun PantallaHistorialTiempo(
    hijos: List<Hijo>,
    padres: List<Padre>,
    registros: List<RegistroTiempo>,
    ediciones: Map<String, List<RegistroEdicion>> = emptyMap(),
    onEliminarRegistro: (String) -> Unit,
    onEditarRegistro: (RegistroTiempo) -> Unit,
    onAtras: () -> Unit
) {
    var registroEditando by remember { mutableStateOf<RegistroTiempo?>(null) }

    Box(Modifier.fillMaxSize().background(BgGrad0)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Historial de tiempo", color = Neutral10) },
                    navigationIcon = {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.Default.ArrowBack, null, tint = NeutralVariant30)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (registros.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
                    Text("Sin registros", color = NeutralVariant50)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(registros.sortedByDescending { it.fechaCompleta }, key = { it.id }) { registro ->
                        val historial = ediciones[registro.id] ?: emptyList()
                        SwipeParaBorrar(onEliminar = { onEliminarRegistro(registro.id) }) {
                            Column {
                                TarjetaRegistroTiempo(
                                    registro = registro,
                                    onEliminar = { onEliminarRegistro(registro.id) },
                                    onEditar = { registroEditando = registro }
                                )
                                if (historial.isNotEmpty()) {
                                    HistorialEdicionesRegistro(historial)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (registroEditando != null) {
        DialogoRegistroTiempo(
            hijos = hijos,
            padres = padres,
            registroExistente = registroEditando,
            onDismiss = { registroEditando = null },
            onGuardar = { nuevoRegistro, _ ->
                onEditarRegistro(nuevoRegistro)
                registroEditando = null
            }
        )
    }
}

@Composable
private fun HistorialEdicionesRegistro(ediciones: List<RegistroEdicion>) {
    var expandido by remember { mutableStateOf(false) }
    val sdf = remember { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
    ) {
        androidx.compose.foundation.text.ClickableText(
            text = androidx.compose.ui.text.AnnotatedString(
                if (expandido) "▲ Ocultar ediciones (${ediciones.size})"
                else "▼ Ver ${ediciones.size} edición${if (ediciones.size > 1) "es" else ""} anterior${if (ediciones.size > 1) "es" else ""}"
            ),
            style = MaterialTheme.typography.labelSmall.copy(color = NeutralVariant50),
            onClick = { expandido = !expandido }
        )
        if (expandido) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ediciones.forEach { edicion ->
                    val fechaStr = sdf.format(java.util.Date(edicion.fechaEdicion))
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = NeutralVariant80.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Editado el $fechaStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeutralVariant50
                            )
                            Text(
                                "Antes: ${edicion.fechaAnterior} · ${edicion.horaInicioAnterior}–${edicion.horaFinAnterior}" +
                                    (if (edicion.nombreHijoAnterior.isNotBlank()) " · ${edicion.nombreHijoAnterior}" else ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = NeutralVariant30
                            )
                        }
                    }
                }
            }
        }
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
                        tint = NeutralVariant30
                    )
                    if (horasFmt != null) {
                        Text(
                            horasFmt,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            color = Neutral10
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
                    color = Neutral10
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = NeutralVariant30
                    )
                    Text(
                        registro.nombrePadre,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant30
                    )
                }
                Text(
                    "${registro.fecha}  ·  ${registro.horaInicio}–${registro.horaFin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant50
                )
            }

            IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    modifier = Modifier.size(18.dp),
                    tint = NeutralVariant30
                )
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    modifier = Modifier.size(18.dp),
                    tint = Red40
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
    var autocompensado by remember { mutableStateOf(registroExistente?.autocompensado ?: false) }

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = autocompensado, onCheckedChange = { autocompensado = it })
                    Column {
                        Text("Autocompensado", fontWeight = FontWeight.Medium)
                        Text("No entra a la deuda de compensación",
                            style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                    }
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
                                    fechaCompleta = System.currentTimeMillis(), esTodosLosHijos = false,
                                    autocompensado = autocompensado
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
                                        esTodosLosHijos = registroExistente?.esTodosLosHijos ?: false,
                                        autocompensado = autocompensado
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

@Composable
fun MiniBaraCuidado(
    registros: List<RegistroTiempo>,
    padres: List<Padre>,
    modifier: Modifier = Modifier
) {
    val horasPorPadre = calcularHorasPorPadre(registros)
    val totalHoras = horasPorPadre.values.sum()
    if (totalHoras == 0.0 || padres.size < 2) return

    val coloresBarra = listOf(Indigo40, Teal40)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            "Distribución de tiempo",
            style = MaterialTheme.typography.labelSmall,
            color = NeutralVariant50
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
        ) {
            padres.forEachIndexed { idx, padre ->
                val horas = horasPorPadre[padre.id] ?: 0.0
                val pct = (horas / totalHoras).toFloat().coerceIn(0f, 1f)
                if (pct > 0f) {
                    Box(
                        Modifier
                            .weight(pct)
                            .fillMaxHeight()
                            .background(coloresBarra.getOrElse(idx) { NeutralVariant50 })
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            padres.forEachIndexed { idx, padre ->
                val horas = horasPorPadre[padre.id] ?: 0.0
                val pct = if (totalHoras > 0) (horas / totalHoras * 100).toInt() else 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(coloresBarra.getOrElse(idx) { NeutralVariant50 })
                    )
                    Text(
                        "${padre.nombre}: $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeutralVariant30
                    )
                }
            }
        }
    }
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
            onVerHistorial = {},
            onAtras = {}
        )
    }
}
