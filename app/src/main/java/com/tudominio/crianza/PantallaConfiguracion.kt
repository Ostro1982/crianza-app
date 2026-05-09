@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
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
    onExportarPDFGastos: () -> Unit = {}
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
            TopAppBar(
                title = { Text("Configuración", color = Neutral10) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Notificaciones push ──────────────────────────────────────────
            Text("Notificaciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Recibí un aviso cuando el otro integrante registre algo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SwitchRow("Eventos y calendario", notifEventos) { notifEventos = it }
            SwitchRow("Gastos", notifGastos) { notifGastos = it }
            SwitchRow("Compensaciones", notifCompensaciones) { notifCompensaciones = it }
            SwitchRow("Lista de compras", notifCompras) { notifCompras = it }
            SwitchRow("Recordatorios de custodia", notifCustodia) { notifCustodia = it }

            // ── Co-parenting (moneda + modo frozen) ──────────────────────────
            Spacer(Modifier.height(8.dp))
            Text("Co-parenting",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            // Moneda
            Column {
                Text("Moneda", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "Aplica a todos los gastos y compensaciones de la familia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val opciones = listOf("ARS", "MXN", "CLP", "COP", "PEN", "UYU", "USD", "EUR")
                FlowRowCompat(opciones) { op ->
                    val sel = moneda == op
                    FilterChip(
                        selected = sel,
                        onClick = { moneda = op },
                        label = { Text(op) }
                    )
                }
            }
            // Frozen
            Column {
                Text("Bloquear edición de registros pasados",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Después de N días, los gastos/eventos/registros no se pueden editar (evidencia inalterable). 0 = desactivado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = frozenDiasTexto,
                    onValueChange = { frozenDiasTexto = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Días") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Evidencia / PDF legal ────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text("Evidencia legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Documentos firmados con hash, presentables en mediación o juzgado de familia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onExportarPDFCustodia, modifier = Modifier.fillMaxWidth()) {
                Text("Exportar PDF custodia")
            }
            OutlinedButton(onClick = onExportarPDFGastos, modifier = Modifier.fillMaxWidth()) {
                Text("Exportar PDF gastos")
            }
            OutlinedButton(onClick = onVerHistorialCambios, modifier = Modifier.fillMaxWidth()) {
                Text("Ver historial de cambios")
            }

            // ── Dispositivo ──────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text("Dispositivo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(
                onClick = { mostrarDialogoCambiarIdentidad = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cambiar quién soy") }
            OutlinedButton(
                onClick = { mostrarDialogoReiniciar = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Reiniciar familia (borrar integrantes y empezar de cero)") }
            OutlinedButton(
                onClick = { mostrarDialogoDesvincular = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Desvincular dispositivo") }

            // ── Herramientas ─────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onVerEstadisticas, modifier = Modifier.fillMaxWidth()) {
                Text("Ver estadísticas")
            }
            OutlinedButton(onClick = onVerTutorial, modifier = Modifier.fillMaxWidth()) {
                Text("Ver tutorial (slides)")
            }
            OutlinedButton(onClick = onVerTourGuiado, modifier = Modifier.fillMaxWidth()) {
                Text("Tour guiado en pantalla")
            }

            // ── Sync calendario sistema ──────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text("Calendario del sistema",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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

            // ── Seguridad ────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text("Seguridad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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

            // ── Backup local encriptado ──────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text("Respaldo de datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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

            // ── Diálogo password export ──
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

            // ── Diálogo password import ──
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
                                    // Intento encriptado primero. Si falla y no es magic-mismatch, probar legacy.
                                    val r = BackupManager.importarConPassword(context, uri, pw)
                                    if (r.isSuccess) {
                                        android.os.Process.killProcess(android.os.Process.myPid())
                                    } else {
                                        val msg = r.exceptionOrNull()?.message ?: ""
                                        if (msg.contains("no es un backup encriptado")) {
                                            // Probar formato legacy (compat con backups previos)
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

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// FlowRow simple sin depender de accompanist; arma chips en filas que envuelven.
@Composable
private fun FlowRowCompat(items: List<String>, content: @Composable (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(4).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fila.forEach { content(it) }
            }
        }
    }
}
