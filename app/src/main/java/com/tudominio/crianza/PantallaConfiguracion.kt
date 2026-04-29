@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.tudominio.crianza.ui.theme.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@Composable
fun PantallaConfiguracion(
    config: ConfiguracionIntegracion,
    filtros: List<FiltroEmail>,
    padres: List<Padre>,
    onGuardarConfig: (ConfiguracionIntegracion) -> Unit,
    onAgregarFiltro: (FiltroEmail) -> Unit,
    onEliminarFiltro: (FiltroEmail) -> Unit,
    onAtras: () -> Unit,
    onVerEstadisticas: () -> Unit = {},
    onReiniciarFamilia: () -> Unit = {}
) {
    var mostrarDialogoReiniciar by remember { mutableStateOf(false) }
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
    val context = LocalContext.current
    val configPrefs = remember { context.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE) }

    var telegramToken by remember { mutableStateOf(config.telegramBotToken) }
    var chatIdPadre1 by remember { mutableStateOf(config.telegramChatIdPadre1) }
    var chatIdPadre2 by remember { mutableStateOf(config.telegramChatIdPadre2) }
    var habilitarTelegram by remember { mutableStateOf(config.habilitarTelegram) }

    var emailHost by remember { mutableStateOf(config.emailHost) }
    var emailPort by remember { mutableStateOf(config.emailPort.toString()) }
    var emailUser by remember { mutableStateOf(config.emailUser) }
    var emailPassword by remember { mutableStateOf(config.emailPassword) }
    var habilitarEmail by remember { mutableStateOf(config.habilitarEmail) }

    var whatsappTelPadre1 by remember { mutableStateOf(config.whatsappTelefonoPadre1) }
    var whatsappTelPadre2 by remember { mutableStateOf(config.whatsappTelefonoPadre2) }
    var habilitarWhatsApp by remember { mutableStateOf(config.habilitarWhatsApp) }
    var whatsappGruposEscuela by remember { mutableStateOf(config.whatsappGruposEscuela) }

    var notifEventos by remember { mutableStateOf(config.notifEventos) }
    var notifGastos by remember { mutableStateOf(config.notifGastos) }
    var notifCompensaciones by remember { mutableStateOf(config.notifCompensaciones) }
    var notifCompras by remember { mutableStateOf(config.notifCompras) }

    var mostrarDialogoFiltro by remember { mutableStateOf(false) }
    var mostrarAyuda by remember { mutableStateOf<String?>(null) } // "whatsapp" | "telegram" | "email"
    var mostrarDialogoCambiarIdentidad by remember { mutableStateOf(false) }
    var mostrarDialogoDesvincular by remember { mutableStateOf(false) }

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
                                telegramBotToken = telegramToken.trim(),
                                telegramChatIdPadre1 = chatIdPadre1.trim(),
                                telegramChatIdPadre2 = chatIdPadre2.trim(),
                                habilitarTelegram = habilitarTelegram,
                                emailHost = emailHost.trim(),
                                emailPort = emailPort.toIntOrNull() ?: 993,
                                emailUser = emailUser.trim(),
                                emailPassword = emailPassword,
                                habilitarEmail = habilitarEmail,
                                whatsappTelefonoPadre1 = whatsappTelPadre1.trim(),
                                whatsappTelefonoPadre2 = whatsappTelPadre2.trim(),
                                habilitarWhatsApp = habilitarWhatsApp,
                                whatsappGruposEscuela = whatsappGruposEscuela.trim(),
                                notifEventos = notifEventos,
                                notifGastos = notifGastos,
                                notifCompensaciones = notifCompensaciones,
                                notifCompras = notifCompras
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
            Text(
                "Notificaciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Recibí una notificación en este celular cuando el otro integrante registre algo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Eventos y calendario", modifier = Modifier.weight(1f))
                Switch(checked = notifEventos, onCheckedChange = { notifEventos = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gastos", modifier = Modifier.weight(1f))
                Switch(checked = notifGastos, onCheckedChange = { notifGastos = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Compensaciones", modifier = Modifier.weight(1f))
                Switch(checked = notifCompensaciones, onCheckedChange = { notifCompensaciones = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lista de compras", modifier = Modifier.weight(1f))
                Switch(checked = notifCompras, onCheckedChange = { notifCompras = it })
            }

            // ── Dispositivo ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Dispositivo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(
                onClick = { mostrarDialogoCambiarIdentidad = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cambiar quién soy")
            }
            OutlinedButton(
                onClick = { mostrarDialogoReiniciar = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Reiniciar familia (borrar integrantes y empezar de cero)")
            }
            OutlinedButton(
                onClick = { mostrarDialogoDesvincular = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Desvincular dispositivo")
            }

            // ── Herramientas ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onVerEstadisticas,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver estadísticas")
            }

            // ── Sync calendario ───────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Calendario del sistema",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            var syncCalendar by remember {
                mutableStateOf(configPrefs.getBoolean("sync_calendar_bidi", false))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Exportar eventos al calendario", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Al crear un evento en Crianza, también se agrega al calendario del teléfono (Google/Samsung/etc)",
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

            // ── Seguridad ─────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Seguridad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            var lockHabilitado by remember { mutableStateOf(AppLock.estaHabilitado(context)) }
            val soporta = remember { AppLock.dispositivoSoporta(context) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            // ── Backup ────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Respaldo de datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Guardá una copia de todos tus datos. Podés restaurarla en otro teléfono o recuperarla si algo falla.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val scope = rememberCoroutineScope()
            var mensajeBackup by remember { mutableStateOf<String?>(null) }
            var mostrarConfirmarRestaurar by remember { mutableStateOf<android.net.Uri?>(null) }

            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        val r = BackupManager.exportar(context, uri)
                        mensajeBackup = if (r.isSuccess) "Copia guardada"
                        else "Error: ${r.exceptionOrNull()?.message}"
                    }
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) mostrarConfirmarRestaurar = uri
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (mostrarConfirmarRestaurar != null) {
                AlertDialog(
                    onDismissRequest = { mostrarConfirmarRestaurar = null },
                    title = { Text("¿Restaurar copia?") },
                    text = { Text("Esto va a REEMPLAZAR todos tus datos actuales con los de la copia. La app se cerrará y tenés que volver a abrirla.") },
                    confirmButton = {
                        TextButton(onClick = {
                            val uri = mostrarConfirmarRestaurar!!
                            mostrarConfirmarRestaurar = null
                            scope.launch {
                                val r = BackupManager.importar(context, uri)
                                if (r.isSuccess) {
                                    android.os.Process.killProcess(android.os.Process.myPid())
                                } else {
                                    mensajeBackup = "Error: ${r.exceptionOrNull()?.message}"
                                }
                            }
                        }) { Text("Restaurar", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarConfirmarRestaurar = null }) { Text("Cancelar") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    }  // Box

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

    // Diálogos de ayuda removidos (integraciones WhatsApp/Telegram/Email eliminadas)
}

@Composable
private fun SeccionIntegracion(titulo: String, icono: ImageVector, onAyuda: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    icono, contentDescription = null,
                    modifier = Modifier.padding(6.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onAyuda, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Outlined.HelpOutline,
                contentDescription = "Ayuda $titulo",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ComandosCard(titulo: String, comandos: List<Pair<String, String>>) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            comandos.forEach { (cmd, desc) ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(cmd, style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f))
                    Text("→ $desc", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.75f),
                        modifier = Modifier.weight(1.2f))
                }
            }
        }
    }
}

@Composable
private fun DialogoAyuda(
    titulo: String,
    pasos: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Text(titulo, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pasos.forEach { (paso, descripcion) ->
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                paso,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(descripcion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido") }
        }
    )
}

@Composable
fun DialogoAgregarFiltro(
    onDismiss: () -> Unit,
    onGuardar: (tipo: String, valor: String) -> Unit
) {
    var tipo by remember { mutableStateOf("remitente") }
    var valor by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar filtro de email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tipo de filtro:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tipo == "remitente", onClick = { tipo = "remitente" })
                    Text("Remitente (dirección de email)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tipo == "asunto", onClick = { tipo = "asunto" })
                    Text("Asunto (texto a buscar)")
                }
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    label = {
                        Text(if (tipo == "remitente") "Email del remitente" else "Texto en el asunto")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGuardar(tipo, valor) }, enabled = valor.isNotBlank()) {
                Text("Agregar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
