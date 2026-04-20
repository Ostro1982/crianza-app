@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.tudominio.crianza.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun PantallaCalendario(
    eventos: List<Evento>,
    padres: List<Padre>,
    onAgregarEvento: (Evento) -> Unit,
    onEliminarEvento: (String) -> Unit,
    onEliminarEventosBulk: (List<String>) -> Unit = {},
    onEditarEvento: (Evento) -> Unit,
    onAtras: () -> Unit
) {
    val context = LocalContext.current
    var mostrarDialogo by remember { mutableStateOf(false) }
    var eventoEditando by remember { mutableStateOf<Evento?>(null) }
    var fechaPreseleccionada by remember { mutableStateOf("") }
    var diaSeleccionado by remember { mutableStateOf("") }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackState = remember { SnackbarHostState() }
    var mostrarPickerCalendario by remember { mutableStateOf(false) }
    var calendariosDisponibles by remember { mutableStateOf<List<CalendarioDispositivo>>(emptyList()) }
    var mostrarGestionRepetidas by remember { mutableStateOf(false) }

    val calendario = Calendar.getInstance()
    var mesActual by remember { mutableIntStateOf(calendario.get(Calendar.MONTH)) }
    var anioActual by remember { mutableIntStateOf(calendario.get(Calendar.YEAR)) }
    val hoy = "%04d-%02d-%02d".format(
        calendario.get(Calendar.YEAR),
        calendario.get(Calendar.MONTH) + 1,
        calendario.get(Calendar.DAY_OF_MONTH)
    )

    val nombresMeses = arrayOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    fun iniciarImportConCalendarios() {
        val cals = GoogleCalendarService.obtenerCalendarios(context)
        when {
            cals.isEmpty() -> snackMsg = "No se encontraron calendarios en el dispositivo"
            cals.size == 1 -> {
                val importados = GoogleCalendarService.importarEventos(context, cals.first().id)
                importados.forEach { onAgregarEvento(it) }
                snackMsg = if (importados.isEmpty()) "Sin eventos nuevos" else "Importados ${importados.size} evento(s)"
            }
            else -> {
                calendariosDisponibles = cals
                mostrarPickerCalendario = true
            }
        }
    }

    // Lanzador de permisos de calendario
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.READ_CALENDAR] == true) {
            iniciarImportConCalendarios()
        } else {
            snackMsg = "Permiso de calendario denegado"
        }
    }

    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackState.showSnackbar(it)
            snackMsg = null
        }
    }

    Box(Modifier.fillMaxSize().background(BgGrad1)) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Calendario", color = Neutral10) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                    }
                },
                actions = {
                    val duplicados = eventos.groupBy { it.titulo }.filter { it.value.size > 1 }
                    if (duplicados.isNotEmpty()) {
                        IconButton(onClick = { mostrarGestionRepetidas = true }) {
                            BadgedBox(badge = { Badge { Text(duplicados.size.toString()) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Gestionar repetidas", tint = NeutralVariant30)
                            }
                        }
                    }
                    IconButton(onClick = {
                        val tienePermiso = android.content.pm.PackageManager.PERMISSION_GRANTED
                        val ok = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == tienePermiso
                        if (ok) {
                            iniciarImportConCalendarios()
                        } else {
                            permLauncher.launch(arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            ))
                        }
                    }) {
                        Icon(Icons.Outlined.Sync, contentDescription = "Importar de Google", tint = NeutralVariant30)
                    }
                    IconButton(onClick = {
                        fechaPreseleccionada = hoy
                        mostrarDialogo = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar evento", tint = NeutralVariant30)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            // Navegación mes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (mesActual == 0) { mesActual = 11; anioActual-- } else mesActual--
                }) { Text("‹", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

                Text(
                    "${nombresMeses[mesActual]} $anioActual",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Neutral10
                )

                IconButton(onClick = {
                    if (mesActual == 11) { mesActual = 0; anioActual++ } else mesActual++
                }) { Text("›", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            }

            // Cabeceras de días
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                    Text(
                        d, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val diasEnMes = Calendar.getInstance()
                .apply { set(anioActual, mesActual, 1) }
                .getActualMaximum(Calendar.DAY_OF_MONTH)
            // Offset: qué día de semana es el día 1 (Lun=0 ... Dom=6)
            val primerDia = Calendar.getInstance().apply { set(anioActual, mesActual, 1) }
            val offset = (primerDia.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(240.dp)) {
                // Celdas vacías de offset
                items(offset) { Box(modifier = Modifier.aspectRatio(1f)) }

                items((1..diasEnMes).toList()) { dia ->
                    val fechaStr = "%04d-%02d-%02d".format(anioActual, mesActual + 1, dia)
                    val tieneEvento = eventos.any { it.fecha == fechaStr }
                    val esHoy = fechaStr == hoy

                    Column(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .background(
                                color = when {
                                    esHoy -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                },
                                shape = CircleShape
                            )
                            .clickable {
                                diaSeleccionado = fechaStr
                                fechaPreseleccionada = fechaStr
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dia.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                esHoy -> MaterialTheme.colorScheme.onPrimaryContainer
                                tieneEvento -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (tieneEvento) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            val eventosMostrar = if (diaSeleccionado.isNotEmpty())
                eventos.filter { it.fecha == diaSeleccionado }
            else eventos.filter { it.fecha >= hoy }.sortedBy { it.fecha }.take(10)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (diaSeleccionado.isNotEmpty()) {
                        val parts = diaSeleccionado.split("-")
                        "Eventos del ${parts.getOrElse(2){""}}-${parts.getOrElse(1){""}}-${parts.getOrElse(0){""}}"
                    } else "Próximos eventos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { mostrarDialogo = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (eventosMostrar.isEmpty()) {
                    item {
                        Text(
                            if (diaSeleccionado.isNotEmpty()) "Sin eventos este día" else "Sin próximos eventos",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                items(eventosMostrar.sortedBy { it.fecha }) { evento ->
                    TarjetaEvento(
                        evento = evento,
                        padres = padres,
                        onEditar = { eventoEditando = evento },
                        onEliminar = { onEliminarEvento(evento.id) },
                        onCambiarAsistencia = { onEditarEvento(it) },
                        onExportarCalendario = {
                            val ok = context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) ==
                                     android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (ok) {
                                val id = GoogleCalendarService.exportarEvento(context, evento)
                                snackMsg = if (id != null) "Exportado al calendario del dispositivo"
                                           else "Error al exportar"
                            } else {
                                permLauncher.launch(arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                ))
                            }
                        }
                    )
                }
            }
        }
    }
    }  // Box

    // Dialog gestión de repetidas
    if (mostrarGestionRepetidas) {
        val grupos = eventos.groupBy { it.titulo }.filter { it.value.size > 1 }.toList()
        val mesActualStr = "%04d-%02d".format(anioActual, mesActual + 1)
        AlertDialog(
            onDismissRequest = { mostrarGestionRepetidas = false },
            title = { Text("Eventos repetidos") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    grupos.forEach { (titulo, grupo) ->
                        ElevatedCard {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(titulo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("${grupo.size} eventos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val delMes = grupo.filter { it.fecha.startsWith(mesActualStr) }
                                    if (delMes.isNotEmpty()) {
                                        OutlinedButton(
                                            onClick = {
                                                onEliminarEventosBulk(delMes.map { it.id })
                                                mostrarGestionRepetidas = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) { Text("Borrar este mes (${delMes.size})", style = MaterialTheme.typography.labelSmall) }
                                    }
                                    Button(
                                        onClick = {
                                            onEliminarEventosBulk(grupo.map { it.id })
                                            mostrarGestionRepetidas = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) { Text("Borrar todas (${grupo.size})", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { mostrarGestionRepetidas = false }) { Text("Cerrar") } }
        )
    }

    // Picker de calendario Google
    if (mostrarPickerCalendario && calendariosDisponibles.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { mostrarPickerCalendario = false },
            title = { Text("¿De cuál calendario importar?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    calendariosDisponibles.forEach { cal ->
                        Card(
                            onClick = {
                                mostrarPickerCalendario = false
                                val importados = GoogleCalendarService.importarEventos(context, cal.id)
                                importados.forEach { onAgregarEvento(it) }
                                snackMsg = if (importados.isEmpty()) "Sin eventos nuevos en ${cal.nombre}"
                                           else "Importados ${importados.size} evento(s) de ${cal.nombre}"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = com.tudominio.crianza.ui.theme.GlassWhite)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(cal.nombre, fontWeight = FontWeight.Medium)
                                if (cal.cuenta.isNotEmpty()) {
                                    Text(cal.cuenta, style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarPickerCalendario = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDialogo || eventoEditando != null) {
        DialogoEvento(
            evento = eventoEditando?.let { it } ?: run {
                // Nuevo evento con fecha preseleccionada
                if (fechaPreseleccionada.isNotEmpty()) Evento(
                    id = "", titulo = "", fecha = fechaPreseleccionada
                ) else null
            },
            padres = padres,
            onDismiss = {
                mostrarDialogo = false
                eventoEditando = null
                fechaPreseleccionada = ""
            },
            onGuardar = {
                val esNuevo = eventoEditando == null
                if (esNuevo) onAgregarEvento(it.copy(id = java.util.UUID.randomUUID().toString()))
                else onEditarEvento(it)
                mostrarDialogo = false
                eventoEditando = null
                fechaPreseleccionada = ""
            }
        )
    }
}

@Composable
fun TarjetaEvento(
    evento: Evento,
    padres: List<Padre>,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onCambiarAsistencia: (Evento) -> Unit,
    onExportarCalendario: () -> Unit = {}
) {
    val ambosAsignados = evento.asistenciaPadre1.isNotEmpty() && evento.asistenciaPadre2.isNotEmpty()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (evento.origenEmail)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fecha badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val partesFecha = evento.fecha.split("-")
                        Text(
                            text = partesFecha.getOrNull(2) ?: "--",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = when (partesFecha.getOrNull(1)) {
                                "01" -> "Ene"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Abr"
                                "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Ago"
                                "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dic"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (evento.origenEmail) Text("📧 ", style = MaterialTheme.typography.bodySmall)
                        Text(
                            evento.titulo,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val horario = listOfNotNull(evento.horaInicio, evento.horaFin).joinToString("–")
                    if (horario.isNotEmpty()) {
                        Text("🕐 $horario", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (evento.ubicacion.isNotEmpty()) {
                        Text("📍 ${evento.ubicacion}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (evento.descripcion.isNotEmpty()) {
                        Text(evento.descripcion, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onExportarCalendario, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Exportar al calendario",
                        modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar",
                        modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                        modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }

            // Asistencia: si ambos ya están asignados, solo mostrar resumen (cambiar desde editar)
            if (padres.size >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(6.dp))

                if (ambosAsignados) {
                    fun etiqueta(estado: String, nombre: String) = when (estado) {
                        "va" -> "✓ $nombre"
                        "no_va" -> "✗ $nombre"
                        else -> "$nombre: ?"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            etiqueta(evento.asistenciaPadre1, padres[0].nombre),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (evento.asistenciaPadre1 == "va")
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        Text(
                            etiqueta(evento.asistenciaPadre2, padres[1].nombre),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (evento.asistenciaPadre2 == "va")
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Editar para cambiar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Text(
                        "¿Quién va?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonAsistencia(
                            nombre = padres[0].nombre,
                            estado = evento.asistenciaPadre1,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val nuevo = when (evento.asistenciaPadre1) {
                                    "va" -> "no_va"; "no_va" -> ""; else -> "va"
                                }
                                onCambiarAsistencia(evento.copy(asistenciaPadre1 = nuevo))
                            }
                        )
                        BotonAsistencia(
                            nombre = padres[1].nombre,
                            estado = evento.asistenciaPadre2,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val nuevo = when (evento.asistenciaPadre2) {
                                    "va" -> "no_va"; "no_va" -> ""; else -> "va"
                                }
                                onCambiarAsistencia(evento.copy(asistenciaPadre2 = nuevo))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BotonAsistencia(
    nombre: String,
    estado: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (texto, color) = when (estado) {
        "va" -> "✓ $nombre va" to MaterialTheme.colorScheme.primary
        "no_va" -> "✗ $nombre no va" to MaterialTheme.colorScheme.error
        else -> "$nombre: ?" to MaterialTheme.colorScheme.outline
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Text(texto, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun DialogoEvento(
    evento: Evento?,
    padres: List<Padre> = emptyList(),
    onDismiss: () -> Unit,
    onGuardar: (Evento) -> Unit
) {
    var titulo by remember { mutableStateOf(evento?.titulo ?: "") }
    var fecha by remember { mutableStateOf(evento?.fecha ?: obtenerFechaActual()) }
    var desc by remember { mutableStateOf(evento?.descripcion ?: "") }
    var ubicacion by remember { mutableStateOf(evento?.ubicacion ?: "") }
    var horaInicio by remember { mutableStateOf(evento?.horaInicio ?: "") }
    var horaFin by remember { mutableStateOf(evento?.horaFin ?: "") }
    var asistencia1 by remember { mutableStateOf(evento?.asistenciaPadre1 ?: "") }
    var asistencia2 by remember { mutableStateOf(evento?.asistenciaPadre2 ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (evento == null) "Nuevo Evento" else "Editar Evento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titulo, onValueChange = { titulo = it },
                    label = { Text("Título") }, modifier = Modifier.fillMaxWidth()
                )
                CampoFecha(
                    value = fecha, label = "Fecha",
                    onValueChange = { fecha = it }, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoHora(
                        value = horaInicio, label = "Desde",
                        onValueChange = { horaInicio = it },
                        modifier = Modifier.weight(1f)
                    )
                    CampoHora(
                        value = horaFin, label = "Hasta",
                        onValueChange = { horaFin = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = ubicacion, onValueChange = { ubicacion = it },
                    label = { Text("Ubicación (opcional)") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth()
                )

                // Asistencia (solo al editar o si hay padres)
                if (padres.size >= 2) {
                    Divider()
                    Text(
                        "¿Quién va?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonAsistencia(
                            nombre = padres[0].nombre,
                            estado = asistencia1,
                            modifier = Modifier.weight(1f),
                            onClick = { asistencia1 = when (asistencia1) { "va" -> "no_va"; "no_va" -> ""; else -> "va" } }
                        )
                        BotonAsistencia(
                            nombre = padres[1].nombre,
                            estado = asistencia2,
                            modifier = Modifier.weight(1f),
                            onClick = { asistencia2 = when (asistencia2) { "va" -> "no_va"; "no_va" -> ""; else -> "va" } }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hIni = if (horaInicio.isNotBlank()) normalizarHora(horaInicio) else null
                    val hFin = if (horaFin.isNotBlank()) normalizarHora(horaFin) else null
                    onGuardar(Evento(
                        id = evento?.id ?: UUID.randomUUID().toString(),
                        titulo = titulo, fecha = fecha, descripcion = desc,
                        ubicacion = ubicacion, horaInicio = hIni, horaFin = hFin,
                        origenEmail = evento?.origenEmail ?: false,
                        asistenciaPadre1 = asistencia1, asistenciaPadre2 = asistencia2
                    ))
                },
                enabled = titulo.isNotBlank() && fecha.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun PantallaGastos(
    gastos: List<Gasto>,
    hijos: List<Hijo>,
    padres: List<Padre>,
    idPadreActual: String = "",
    onAgregarGasto: (Gasto) -> Unit,
    onEliminarGasto: (String) -> Unit,
    onEditarGasto: (Gasto) -> Unit,
    onAtras: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var gastoEditando by remember { mutableStateOf<Gasto?>(null) }
    var busqueda by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(BgGrad2)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Gastos", color = Neutral10) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogo = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar gasto", tint = NeutralVariant30)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            // Resumen mensual
            val cal = Calendar.getInstance()
            val mesActual = cal.get(Calendar.MONTH)
            val anioActual = cal.get(Calendar.YEAR)
            val nombresMeses = arrayOf(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            )
            val gastosMes = gastos.filter { g ->
                val f = parseFecha(g.fecha)
                esMismoMes(f, cal.time)
            }
            val totalMes = gastosMes.sumOf { it.monto }

            // Colores semanticos para balance
            val BalanceGreen = Teal40
            val BalanceAmber = Rose40

            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Indigo30.copy(alpha = 0.25f), Indigo40.copy(alpha = 0.25f))
                        )
                    )
            ) {
                // Efecto glassmorphic interior
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(23.dp))
                        .background(GlassWhite)
                        .padding(20.dp)
                ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${nombresMeses[mesActual]} $anioActual",
                            style = MaterialTheme.typography.titleMedium,
                            color = Neutral10,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            Modifier.clip(RoundedCornerShape(50.dp))
                                .background(GlassWhite)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "ESTE MES",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeutralVariant50,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "$${"%.2f".format(totalMes)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                        color = Neutral10,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )

                    if (padres.size >= 2) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(NeutralVariant50.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(16.dp))

                        val totalP1Mes = gastosMes.filter { it.idPagador == padres[0].id }.sumOf { it.monto }
                        val totalP2Mes = gastosMes.filter { it.idPagador == padres[1].id }.sumOf { it.monto }

                        // Barra proporcional mejorada
                        if (totalMes > 0) {
                            val frac1 = (totalP1Mes / totalMes).toFloat().coerceIn(0.05f, 0.95f)
                            Box(
                                Modifier.fillMaxWidth().height(10.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(NeutralVariant50.copy(.15f))
                            ) {
                                Row(Modifier.fillMaxSize()) {
                                    Box(
                                        Modifier.fillMaxWidth(frac1).fillMaxHeight()
                                            .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                                            .background(
                                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    listOf(Indigo40.copy(.7f), Indigo40)
                                                )
                                            )
                                    )
                                    Box(
                                        Modifier.fillMaxWidth().fillMaxHeight()
                                            .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                                            .background(
                                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    listOf(Rose40, Rose40.copy(.7f))
                                                )
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Indigo40))
                                    Spacer(Modifier.width(8.dp))
                                    Text(padres[0].nombre, color = NeutralVariant30, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("$${"%.2f".format(totalP1Mes)}", color = Neutral10, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(padres[1].nombre, color = NeutralVariant30, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Rose40))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("$${"%.2f".format(totalP2Mes)}", color = Neutral10, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Balance con indicador color: verde si equilibrado, ambar si imbalanced
                        val deudaMes = (totalP1Mes - totalP2Mes) / 2.0
                        val equilibrado = kotlin.math.abs(deudaMes) < 0.01
                        val balColor = if (equilibrado) BalanceGreen else BalanceAmber
                        val deudaTexto = when {
                            deudaMes > 0.01 -> "${padres[1].nombre} debe a ${padres[0].nombre}: $${"%.2f".format(deudaMes)}"
                            deudaMes < -0.01 -> "${padres[0].nombre} debe a ${padres[1].nombre}: $${"%.2f".format(-deudaMes)}"
                            else -> "Cuentas equilibradas"
                        }
                        Box(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(balColor.copy(.15f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                Arrangement.SpaceBetween,
                                Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(balColor.copy(.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (equilibrado) "✓" else "!",
                                            color = balColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(deudaTexto, fontWeight = FontWeight.Bold, color = balColor, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                } // inner Box
            }

            // Total general
            val total = gastos.sumOf { it.monto }
            if (gastos.size != gastosMes.size) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp)).background(GlassWhite)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text(
                            "Total general",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeutralVariant50
                        )
                        Text(
                            "$${"%.2f".format(total)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Neutral10,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Búsqueda
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar gastos...", color = NeutralVariant50) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeutralVariant50) },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = NeutralVariant50)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Neutral10,
                    unfocusedTextColor = Neutral10,
                    focusedBorderColor = NeutralVariant30.copy(0.5f),
                    unfocusedBorderColor = NeutralVariant50.copy(0.3f),
                    cursorColor = Neutral10,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp)
            )

            val gastosFiltrados = if (busqueda.isBlank()) gastos else {
                val q = busqueda.lowercase()
                gastos.filter {
                    it.descripcion.lowercase().contains(q) ||
                    it.nombrePagador.lowercase().contains(q) ||
                    it.categoria.lowercase().contains(q)
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(gastosFiltrados.sortedByDescending { it.fecha }, key = { it.id }) { gasto ->
                    SwipeParaBorrar(onEliminar = { onEliminarGasto(gasto.id) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)).background(GlassWhite)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(gasto.descripcion, fontWeight = FontWeight.Bold, color = Neutral10)
                                    Text("$${"%.2f".format(gasto.monto)} · ${gasto.fecha}", color = NeutralVariant30)
                                    val detalle = listOfNotNull(
                                        "Pagó: ${gasto.nombrePagador}",
                                        gasto.categoria.takeIf { it.isNotEmpty() }
                                    ).joinToString(" · ")
                                    Text(detalle, style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                                }
                                IconButton(onClick = { gastoEditando = gasto }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = NeutralVariant30)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }  // Box

    if (mostrarDialogo || gastoEditando != null) {
        DialogoGasto(
            gasto = gastoEditando,
            padres = padres,
            idPadreActual = idPadreActual,
            onDismiss = { mostrarDialogo = false; gastoEditando = null },
            onGuardar = {
                if (gastoEditando != null) onEditarGasto(it) else onAgregarGasto(it)
                mostrarDialogo = false
                gastoEditando = null
            }
        )
    }
}

@Composable
fun DialogoGasto(
    gasto: Gasto?,
    padres: List<Padre>,
    idPadreActual: String = "",
    onDismiss: () -> Unit,
    onGuardar: (Gasto) -> Unit
) {
    var desc by remember { mutableStateOf(gasto?.descripcion ?: "") }
    var monto by remember { mutableStateOf(gasto?.monto?.toString() ?: "") }
    var fecha by remember { mutableStateOf(gasto?.fecha ?: obtenerFechaActual()) }
    // Para nuevo gasto: defaultea al usuario actual si está configurado, si no al primero
    val defaultPagador = gasto?.idPagador
        ?: idPadreActual.ifEmpty { padres.firstOrNull()?.id ?: "" }
    var idPagador by remember { mutableStateOf(defaultPagador) }
    var categoria by remember { mutableStateOf(gasto?.categoria ?: "") }
    var autocompensado by remember { mutableStateOf(gasto?.autocompensado ?: false) }
    var expandirCategorias by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (gasto == null) "Nuevo Gasto" else "Editar Gasto") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto ($)") },
                    modifier = Modifier.fillMaxWidth()
                )
                CampoFecha(value = fecha, label = "Fecha", onValueChange = { fecha = it }, modifier = Modifier.fillMaxWidth())

                // Selector de categoría
                ExposedDropdownMenuBox(
                    expanded = expandirCategorias,
                    onExpandedChange = { expandirCategorias = it }
                ) {
                    OutlinedTextField(
                        value = categoria.ifEmpty { "Sin categoría" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirCategorias) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandirCategorias,
                        onDismissRequest = { expandirCategorias = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin categoría") },
                            onClick = { categoria = ""; expandirCategorias = false }
                        )
                        CATEGORIAS_GASTO.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { categoria = cat; expandirCategorias = false }
                            )
                        }
                    }
                }

                Text("¿Quién pagó?")
                padres.forEach { padre ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = idPagador == padre.id, onClick = { idPagador = padre.id })
                        Text(padre.nombre)
                    }
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
            }
        },
        confirmButton = {
            Button(onClick = {
                val pagador = padres.find { it.id == idPagador }
                onGuardar(Gasto(
                    id = gasto?.id ?: UUID.randomUUID().toString(),
                    descripcion = desc,
                    monto = monto.toDoubleOrNull() ?: 0.0,
                    fecha = fecha,
                    idPagador = idPagador,
                    nombrePagador = pagador?.nombre ?: "",
                    idsHijos = emptyList(),
                    nombresHijos = "",
                    categoria = categoria,
                    autocompensado = autocompensado
                ))
            }, enabled = desc.isNotBlank() && monto.toDoubleOrNull() != null) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun PantallaRecuerdos(
    recuerdos: List<Recuerdo>,
    onAgregarRecuerdo: (Recuerdo) -> Unit,
    onEliminarRecuerdo: (String) -> Unit,
    onEditarRecuerdo: (Recuerdo) -> Unit,
    onAtras: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var recuerdoEditando by remember { mutableStateOf<Recuerdo?>(null) }

    Box(Modifier.fillMaxSize().background(BgGrad7)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Recuerdos y Anécdotas", color = Neutral10) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogo = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar recuerdo", tint = NeutralVariant30)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (recuerdos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No hay recuerdos aún. ¡Agrega el primero!", color = NeutralVariant30)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                items(recuerdos.sortedByDescending { it.fecha }) { recuerdo ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)).background(GlassWhite)
                    ) {
                        Column {
                            if (!recuerdo.imagenUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = recuerdo.imagenUri!!.let { if (it.startsWith("/")) java.io.File(it) else it },
                                    contentDescription = "Foto del recuerdo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                                )
                            }
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = NeutralVariant30)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(recuerdo.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neutral10)
                                }
                                Text(recuerdo.fecha, style = MaterialTheme.typography.bodySmall, color = NeutralVariant50)
                                if (recuerdo.descripcion.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(recuerdo.descripcion, color = NeutralVariant30)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { recuerdoEditando = recuerdo }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = NeutralVariant30)
                                    }
                                    IconButton(onClick = { onEliminarRecuerdo(recuerdo.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Red40)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }  // Box

    if (mostrarDialogo || recuerdoEditando != null) {
        DialogoRecuerdo(
            recuerdo = recuerdoEditando,
            onDismiss = { mostrarDialogo = false; recuerdoEditando = null },
            onGuardar = { 
                if (recuerdoEditando != null) onEditarRecuerdo(it) else onAgregarRecuerdo(it)
                mostrarDialogo = false
                recuerdoEditando = null
            }
        )
    }
}

@Composable
fun DialogoRecuerdo(recuerdo: Recuerdo?, onDismiss: () -> Unit, onGuardar: (Recuerdo) -> Unit) {
    var titulo by remember { mutableStateOf(recuerdo?.titulo ?: "") }
    var fecha by remember { mutableStateOf(recuerdo?.fecha ?: obtenerFechaActual()) }
    var desc by remember { mutableStateOf(recuerdo?.descripcion ?: "") }
    var imagenUri by remember { mutableStateOf(recuerdo?.imagenUri) }
    val context = LocalContext.current

    val fotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val dir = java.io.File(context.filesDir, "recuerdos").also { it.mkdirs() }
            val file = java.io.File(dir, "${UUID.randomUUID()}.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                imagenUri = file.absolutePath
            } catch (_: Exception) {
                imagenUri = uri.toString()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (recuerdo == null) "Nuevo Recuerdo" else "Editar Recuerdo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título / Anécdota") })
                CampoFecha(value = fecha, label = "Fecha", onValueChange = { fecha = it })
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción detallada") },
                    modifier = Modifier.height(100.dp)
                )
                if (!imagenUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = imagenUri!!.let { if (it.startsWith("/")) java.io.File(it) else it },
                        contentDescription = "Foto seleccionada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                OutlinedButton(
                    onClick = {
                        fotoPicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (imagenUri.isNullOrEmpty()) "Agregar foto" else "Cambiar foto")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onGuardar(Recuerdo(id = recuerdo?.id ?: UUID.randomUUID().toString(), titulo = titulo, fecha = fecha, descripcion = desc, imagenUri = imagenUri))
            }, enabled = titulo.isNotBlank()) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
