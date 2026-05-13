@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import com.tudominio.crianza.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PantallaConfiguracion(
    config: ConfiguracionIntegracion,
    onGuardarConfig: (ConfiguracionIntegracion) -> Unit,
    onAtras: () -> Unit,
    onVerEstadisticas: () -> Unit = {},
    onReiniciarFamilia: () -> Unit = {},
    onVerTutorial: () -> Unit = {},
    onVerTourGuiado: () -> Unit = {},
    onVerHistorialCambios: () -> Unit = {},
    onExportarPDFCustodia: () -> Unit = {},
    onExportarPDFGastos: () -> Unit = {},
    onCustodyScheduler: () -> Unit = {},
    onCategorias: () -> Unit = {},
    hijos: List<Hijo> = emptyList(),
    onFichaHijo: (Hijo) -> Unit = {}
) {
    var mostrarDialogoReiniciar by remember { mutableStateOf(false) }
    var mostrarDialogoCambiarIdentidad by remember { mutableStateOf(false) }
    var mostrarDialogoDesvincular by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val configPrefs = remember { context.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE) }

    var notifEventos by remember { mutableStateOf(config.notifEventos) }
    var notifGastos by remember { mutableStateOf(config.notifGastos) }
    var notifCompensaciones by remember { mutableStateOf(config.notifCompensaciones) }
    var notifCompras by remember { mutableStateOf(config.notifCompras) }
    var notifCustodia by remember { mutableStateOf(config.notifCustodia) }
    var moneda by remember { mutableStateOf(config.moneda) }
    var frozenDiasTexto by remember { mutableStateOf(config.frozenDias.toString()) }

    var modoActual by remember { mutableStateOf(ModoFamilia.actual(context)) }
    val esConvivencia = modoActual == ModoFamilia.CONVIVENCIA

    var tabActiva by remember { mutableStateOf(0) }
    val tabs: List<Pair<String, ImageVector>> = listOf(
        "Familia" to Icons.Filled.Group,
        "Prefs" to Icons.Filled.Tune,
        "Notif" to Icons.Filled.Notifications,
        "Reportes" to Icons.Filled.Description,
        "Seguridad" to Icons.Filled.Security,
        "Ayuda" to Icons.AutoMirrored.Filled.HelpOutline
    )

    if (mostrarDialogoReiniciar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoReiniciar = false },
            title = { Text("Reiniciar familia") },
            text = {
                Text("Se van a borrar TODOS los integrantes (aca y en la nube) y vas a registrar la familia de cero. Los gastos/eventos/mensajes se mantienen pero quedan sin persona asociada. ¿Continuar?")
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoReiniciar = false
                    onReiniciarFamilia()
                }) { Text("Reiniciar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoReiniciar = false }) { Text("Cancelar") }
            }
        )
    }

    Box(Modifier.fillMaxSize().background(BgGradient)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Configuración", color = Neutral10) },
                    navigationIcon = {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            onGuardarConfig(
                                config.copy(
                                    notifEventos = notifEventos,
                                    notifGastos = notifGastos,
                                    notifCompensaciones = notifCompensaciones,
                                    notifCompras = notifCompras,
                                    notifCustodia = notifCustodia,
                                    moneda = moneda,
                                    frozenDias = frozenDiasTexto.toIntOrNull()?.coerceAtLeast(0) ?: 0
                                )
                            )
                            onAtras()
                        }) {
                            Text("Guardar", fontWeight = FontWeight.Bold, color = Neutral10)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                ScrollableTabRow(
                    selectedTabIndex = tabActiva,
                    containerColor = Color.Transparent,
                    edgePadding = 12.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { i, (label, icon) ->
                        Tab(
                            selected = tabActiva == i,
                            onClick = { tabActiva = i },
                            icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
                            text = {
                                Text(
                                    label,
                                    fontWeight = if (tabActiva == i) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tabActiva) {
                0 -> TabFamilia(
                    esConvivencia = esConvivencia,
                    hijos = hijos,
                    onFichaHijo = onFichaHijo,
                    onCustodyScheduler = onCustodyScheduler,
                    onCambiarIdentidad = { mostrarDialogoCambiarIdentidad = true },
                    onReiniciar = { mostrarDialogoReiniciar = true },
                    onDesvincular = { mostrarDialogoDesvincular = true },
                    onCambiarModo = { nuevoModo ->
                        ModoFamilia.setModo(context, nuevoModo)
                        modoActual = nuevoModo
                    }
                )
                1 -> TabPreferencias(
                    moneda = moneda,
                    onMonedaChange = { moneda = it },
                    frozenDiasTexto = frozenDiasTexto,
                    onFrozenChange = { frozenDiasTexto = it },
                    configPrefs = configPrefs,
                    onCategorias = onCategorias
                )
                2 -> TabNotificaciones(
                    esConvivencia = esConvivencia,
                    notifEventos = notifEventos, onEventos = { notifEventos = it },
                    notifGastos = notifGastos, onGastos = { notifGastos = it },
                    notifCompensaciones = notifCompensaciones, onComp = { notifCompensaciones = it },
                    notifCompras = notifCompras, onCompras = { notifCompras = it },
                    notifCustodia = notifCustodia, onCustodia = { notifCustodia = it }
                )
                3 -> TabReportes(
                    esConvivencia = esConvivencia,
                    onPDFCustodia = onExportarPDFCustodia,
                    onPDFGastos = onExportarPDFGastos,
                    onHistorial = onVerHistorialCambios,
                    onEstadisticas = onVerEstadisticas
                )
                4 -> TabSeguridad(context = context)
                5 -> TabAyuda(
                    onTutorial = onVerTutorial,
                    onTour = onVerTourGuiado
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    }

    if (mostrarDialogoCambiarIdentidad) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCambiarIdentidad = false },
            title = { Text("¿Cambiar identidad?") },
            text = { Text("Vas a poder elegir de nuevo quién sos vos en esta familia. Los datos y la sincronización no se pierden.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoCambiarIdentidad = false
                    context.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .remove("padre_actual_id")
                        .putBoolean("padre_actual_fijado", false)
                        .apply()
                    onAtras()
                }) { Text("Confirmar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCambiarIdentidad = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDialogoDesvincular) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDesvincular = false },
            title = { Text("¿Desvincular este dispositivo?") },
            text = { Text("El dispositivo dejará de sincronizarse con la familia. Podés volver a vincular con el mismo código.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoDesvincular = false
                    FamilyIdManager.desvincular(context)
                    onAtras()
                }) { Text("Desvincular", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoDesvincular = false }) { Text("Cancelar") }
            }
        )
    }
}

// ═══════════════ TAB 0: FAMILIA ═══════════════

@Composable
private fun TabFamilia(
    esConvivencia: Boolean,
    hijos: List<Hijo>,
    onFichaHijo: (Hijo) -> Unit,
    onCustodyScheduler: () -> Unit,
    onCambiarIdentidad: () -> Unit,
    onReiniciar: () -> Unit,
    onDesvincular: () -> Unit,
    onCambiarModo: (String) -> Unit
) {
    var dialogoModo by remember { mutableStateOf(false) }
    val ctxLocal = LocalContext.current
    val prefsLocal = remember { ctxLocal.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE) }
    var aniversario by remember { mutableStateOf(prefsLocal.getString("fecha_aniversario", "") ?: "") }

    SeccionTitulo("Modo de la familia")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (esConvivencia) "🏡 Vivimos juntos" else "🔄 Vivimos separados",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                if (esConvivencia) "Coordinamos la crianza bajo el mismo techo"
                else "Compartimos la crianza entre dos hogares",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { dialogoModo = true }) { Text("Cambiar") }
    }

    if (!esConvivencia) {
        Spacer(Modifier.height(8.dp))
        SeccionTitulo("Plan de custodia")
        Text(
            "Elegí un patrón (2-2-3, semana on/off, etc) y la app crea los registros día por día.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onCustodyScheduler, modifier = Modifier.fillMaxWidth()) {
            Text("Configurar plan de custodia")
        }
    }

    if (hijos.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        SeccionTitulo("Ficha de los hijos")
        Text(
            "Obra social, pediatra, alergias, vacunas, talles. Lo que sirve cuando vos no estás.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        hijos.forEach { hijo ->
            OutlinedButton(
                onClick = { onFichaHijo(hijo) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ficha de ${hijo.nombre}")
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    SeccionTitulo("Aniversario")
    Text(
        if (esConvivencia)
            "Te avisamos el día de su aniversario."
        else
            "Día especial entre vos y la otra persona (cumpleaños de pareja anterior, fecha que les importa).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    CampoFecha(
        value = aniversario,
        label = "Fecha (opcional)",
        onValueChange = {
            aniversario = it
            prefsLocal.edit().putString("fecha_aniversario", it).apply()
        },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))
    SeccionTitulo("Identidad y vínculo")
    OutlinedButton(onClick = onCambiarIdentidad, modifier = Modifier.fillMaxWidth()) {
        Text("Cambiar quién soy")
    }
    OutlinedButton(
        onClick = onReiniciar,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) { Text("Reiniciar familia (borrar integrantes)") }
    OutlinedButton(
        onClick = onDesvincular,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) { Text("Desvincular dispositivo") }

    if (dialogoModo) {
        AlertDialog(
            onDismissRequest = { dialogoModo = false },
            title = { Text("¿Cómo viven?") },
            text = {
                Column {
                    Text(
                        "Esto cambia qué secciones ves. Tus datos no se borran.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OpcionModo(
                        emoji = "🏡",
                        titulo = "Vivimos juntos",
                        descripcion = "Sin plan de custodia, sin PDF de días con los chicos.",
                        seleccionado = esConvivencia,
                        onClick = {
                            onCambiarModo(ModoFamilia.CONVIVENCIA)
                            dialogoModo = false
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    OpcionModo(
                        emoji = "🔄",
                        titulo = "Vivimos separados",
                        descripcion = "Coparenting con plan de custodia, días con cada padre, PDFs.",
                        seleccionado = !esConvivencia,
                        onClick = {
                            onCambiarModo(ModoFamilia.COPARENTING)
                            dialogoModo = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogoModo = false }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun OpcionModo(
    emoji: String,
    titulo: String,
    descripcion: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (seleccionado)
            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else ButtonDefaults.outlinedButtonColors()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold)
                Text(descripcion, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ═══════════════ TAB 1: PREFERENCIAS ═══════════════

@Composable
private fun TabPreferencias(
    moneda: String,
    onMonedaChange: (String) -> Unit,
    frozenDiasTexto: String,
    onFrozenChange: (String) -> Unit,
    configPrefs: android.content.SharedPreferences,
    onCategorias: () -> Unit
) {
    SeccionTitulo("Moneda")
    Text(
        "Aplica a todos los gastos y compensaciones de la familia.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    var monedaExpandida by remember { mutableStateOf(false) }
    val opciones = listOf(
        "ARS" to "Pesos argentinos (ARS)",
        "MXN" to "Pesos mexicanos (MXN)",
        "CLP" to "Pesos chilenos (CLP)",
        "COP" to "Pesos colombianos (COP)",
        "PEN" to "Soles peruanos (PEN)",
        "UYU" to "Pesos uruguayos (UYU)",
        "USD" to "Dólares (USD)",
        "EUR" to "Euros (EUR)"
    )
    val labelActual = opciones.firstOrNull { it.first == moneda }?.second ?: moneda
    ExposedDropdownMenuBox(
        expanded = monedaExpandida,
        onExpandedChange = { monedaExpandida = it }
    ) {
        OutlinedTextField(
            value = labelActual,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monedaExpandida) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = monedaExpandida,
            onDismissRequest = { monedaExpandida = false }
        ) {
            opciones.forEach { (codigo, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onMonedaChange(codigo)
                        monedaExpandida = false
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    SeccionTitulo("Bloquear cambios viejos")
    Text(
        "Después de N días los gastos y registros quedan fijos. Útil si necesitás dejar el historial estable. 0 = siempre se pueden editar.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = frozenDiasTexto,
        onValueChange = { onFrozenChange(it.filter { c -> c.isDigit() }.take(3)) },
        label = { Text("Días") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))
    SeccionTitulo("Tarjetas del inicio")
    Text(
        "Elegí qué módulos ver en la pantalla principal. Lo que no usás, ocultalo.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val ctxDash = LocalContext.current
    val refreshDash = remember { mutableStateOf(0) }
    DashWidgets.TODOS.forEach { (id, label) ->
        var act by remember(refreshDash.value, id) { mutableStateOf(DashWidgets.activo(id, ctxDash)) }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Switch(
                checked = act,
                onCheckedChange = {
                    act = it
                    DashWidgets.set(id, ctxDash, it)
                }
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    SeccionTitulo("Tema")
    val ctxTema = LocalContext.current
    var temaActual by remember { mutableStateOf(TemaPref.actual(ctxTema)) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf(
            TemaPref.AUTO to "Auto",
            TemaPref.CLARO to "☀️ Claro",
            TemaPref.OSCURO to "🌙 Oscuro"
        ).forEach { (codigo, label) ->
            FilterChip(
                selected = temaActual == codigo,
                onClick = {
                    temaActual = codigo
                    TemaPref.setTema(ctxTema, codigo)
                    (ctxTema as? android.app.Activity)?.recreate()
                },
                label = { Text(label) }
            )
        }
    }
    Text(
        "Auto sigue el modo del teléfono. Cambiá manual si lo querés fijo.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(8.dp))
    SeccionTitulo("Categorías de gasto")
    Text(
        "Agregá o sacá categorías propias (cuotas, vacaciones, regalos, lo que necesites).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedButton(onClick = onCategorias, modifier = Modifier.fillMaxWidth()) {
        Text("Gestionar categorías")
    }

    Spacer(Modifier.height(8.dp))
    SeccionTitulo("Calendario del sistema")
    var syncCalendar by remember {
        mutableStateOf(configPrefs.getBoolean("sync_calendar_bidi", false))
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Exportar eventos al calendario", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Al crear un evento en Nesty también se agrega al calendario del teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = syncCalendar,
            onCheckedChange = {
                syncCalendar = it
                configPrefs.edit().putBoolean("sync_calendar_bidi", it).apply()
            }
        )
    }
}

// ═══════════════ TAB 2: NOTIFICACIONES ═══════════════

@Composable
private fun TabNotificaciones(
    esConvivencia: Boolean,
    notifEventos: Boolean, onEventos: (Boolean) -> Unit,
    notifGastos: Boolean, onGastos: (Boolean) -> Unit,
    notifCompensaciones: Boolean, onComp: (Boolean) -> Unit,
    notifCompras: Boolean, onCompras: (Boolean) -> Unit,
    notifCustodia: Boolean, onCustodia: (Boolean) -> Unit
) {
    SeccionTitulo("Avisos push")
    Text(
        if (esConvivencia) "Recibí avisos cuando tu pareja registre algo."
        else "Recibí un aviso cuando el otro integrante registre algo.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    SwitchRow("Eventos y calendario", notifEventos, onEventos)
    SwitchRow("Gastos", notifGastos, onGastos)
    if (!esConvivencia) {
        SwitchRow("Compensaciones", notifCompensaciones, onComp)
    }
    SwitchRow("Lista de compras", notifCompras, onCompras)
    if (!esConvivencia) {
        SwitchRow("Recordatorios de custodia", notifCustodia, onCustodia)
    }
    SeccionTitulo("Cumpleaños")
    Text(
        "Te avisamos el día del cumpleaños de los chicos y los adultos de la familia.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ═══════════════ TAB 3: REPORTES ═══════════════

@Composable
private fun TabReportes(
    esConvivencia: Boolean,
    onPDFCustodia: () -> Unit,
    onPDFGastos: () -> Unit,
    onHistorial: () -> Unit,
    onEstadisticas: () -> Unit
) {
    SeccionTitulo("Resúmenes en PDF")
    Text(
        if (esConvivencia)
            "Generá un PDF ordenado de gastos del mes."
        else
            "Generá un PDF ordenado de gastos o días con los chicos. Sirve para repasar el mes, mandar a la otra persona o, si hace falta, presentar en una mediación.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (!esConvivencia) {
        OutlinedButton(onClick = onPDFCustodia, modifier = Modifier.fillMaxWidth()) {
            Text("Resumen de días con los chicos (PDF)")
        }
    }
    OutlinedButton(onClick = onPDFGastos, modifier = Modifier.fillMaxWidth()) {
        Text("Resumen de gastos (PDF)")
    }

    Spacer(Modifier.height(8.dp))
    SeccionTitulo("Otros")
    OutlinedButton(onClick = onHistorial, modifier = Modifier.fillMaxWidth()) {
        Text("Ver historial de cambios")
    }
    OutlinedButton(onClick = onEstadisticas, modifier = Modifier.fillMaxWidth()) {
        Text("Ver estadísticas")
    }
}

// ═══════════════ TAB 4: SEGURIDAD ═══════════════

@Composable
private fun TabSeguridad(context: android.content.Context) {
    SeccionTitulo("Bloqueo al abrir")
    var lockHabilitado by remember { mutableStateOf(AppLock.estaHabilitado(context)) }
    val soporta = remember { AppLock.dispositivoSoporta(context) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Pedir huella/PIN al abrir", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (soporta) "Los datos de tu familia quedan protegidos"
                else "Este dispositivo no tiene huella/PIN configurado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = lockHabilitado,
            onCheckedChange = {
                lockHabilitado = it
                AppLock.setHabilitado(context, it)
            },
            enabled = soporta
        )
    }

    Spacer(Modifier.height(8.dp))
    SeccionTitulo("Respaldo de datos")
    Text(
        "Guardá una copia encriptada de todos tus datos. Podés restaurarla en otro teléfono.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val scope = rememberCoroutineScope()
    var mensajeBackup by remember { mutableStateOf<String?>(null) }
    var passwordExportDialog by remember { mutableStateOf<android.net.Uri?>(null) }
    var passwordImportDialog by remember { mutableStateOf<android.net.Uri?>(null) }
    var passwordTexto by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            passwordExportDialog = uri
            passwordTexto = ""
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            passwordImportDialog = uri
            passwordTexto = ""
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { exportLauncher.launch(BackupManager.nombreSugerido()) },
            modifier = Modifier.weight(1f)
        ) { Text("Exportar") }
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.weight(1f)
        ) { Text("Restaurar") }
    }
    mensajeBackup?.let {
        Text(it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (passwordExportDialog != null) {
        AlertDialog(
            onDismissRequest = { passwordExportDialog = null },
            title = { Text("Encriptar backup") },
            text = {
                Column {
                    Text("Elegí una contraseña (mínimo 8 caracteres). Sin ella nadie va a poder restaurar este archivo.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordTexto,
                        onValueChange = { passwordTexto = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Ocultar" else "Mostrar")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = passwordTexto.length >= 8,
                    onClick = {
                        val uri = passwordExportDialog!!
                        val pw = passwordTexto
                        passwordExportDialog = null
                        passwordTexto = ""
                        scope.launch {
                            val r = BackupManager.exportarConPassword(context, uri, pw)
                            mensajeBackup = if (r.isSuccess) "Backup encriptado guardado"
                            else "Error: ${r.exceptionOrNull()?.message}"
                        }
                    }
                ) { Text("Exportar") }
            },
            dismissButton = {
                TextButton(onClick = { passwordExportDialog = null }) { Text("Cancelar") }
            }
        )
    }

    if (passwordImportDialog != null) {
        AlertDialog(
            onDismissRequest = { passwordImportDialog = null },
            title = { Text("Restaurar backup") },
            text = {
                Column {
                    Text("Esto va a REEMPLAZAR todos tus datos. La app se cerrará al terminar.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordTexto,
                        onValueChange = { passwordTexto = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Ocultar" else "Mostrar")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = passwordTexto.length >= 8,
                    onClick = {
                        val uri = passwordImportDialog!!
                        val pw = passwordTexto
                        passwordImportDialog = null
                        passwordTexto = ""
                        scope.launch {
                            val r = BackupManager.importarConPassword(context, uri, pw)
                            if (r.isSuccess) {
                                android.os.Process.killProcess(android.os.Process.myPid())
                            } else {
                                val msg = r.exceptionOrNull()?.message ?: ""
                                if (msg.contains("no es un backup encriptado")) {
                                    val r2 = BackupManager.importar(context, uri)
                                    if (r2.isSuccess) android.os.Process.killProcess(android.os.Process.myPid())
                                    else mensajeBackup = "Error: ${r2.exceptionOrNull()?.message}"
                                } else {
                                    mensajeBackup = "Error: $msg"
                                }
                            }
                        }
                    }
                ) { Text("Restaurar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { passwordImportDialog = null }) { Text("Cancelar") }
            }
        )
    }
}

// ═══════════════ TAB 5: AYUDA ═══════════════

@Composable
private fun TabAyuda(
    onTutorial: () -> Unit,
    onTour: () -> Unit
) {
    SeccionTitulo("Cómo usar Nesty")
    OutlinedButton(onClick = onTutorial, modifier = Modifier.fillMaxWidth()) {
        Text("Ver tutorial (slides)")
    }
    OutlinedButton(onClick = onTour, modifier = Modifier.fillMaxWidth()) {
        Text("Tour guiado en pantalla")
    }
}

// ═══════════════ HELPERS ═══════════════

@Composable
private fun SeccionTitulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
