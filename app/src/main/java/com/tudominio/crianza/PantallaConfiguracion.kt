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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onAtras: () -> Unit
) {
    val context = LocalContext.current
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

    Box(Modifier.fillMaxSize().background(BgGradient)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Integraciones", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarAyuda = "general" }) {
                        Icon(
                            Icons.Outlined.HelpOutline,
                            contentDescription = "Ayuda",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
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
                        Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
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

            // ── Sección WhatsApp ─────────────────────────────────────────────
            SeccionIntegracion(
                titulo = "WhatsApp",
                icono = Icons.Outlined.PhoneAndroid,
                onAyuda = { mostrarAyuda = "whatsapp" }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Habilitar WhatsApp", modifier = Modifier.weight(1f))
                Switch(checked = habilitarWhatsApp, onCheckedChange = { habilitarWhatsApp = it })
            }

            val accesoWA = WhatsAppListenerService.accesoHabilitado(context)
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (accesoWA)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (accesoWA) "✅ Acceso a notificaciones habilitado"
                        else "⚠️ Requiere acceso a notificaciones para leer comandos de WhatsApp",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!accesoWA) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { WhatsAppListenerService.abrirConfiguracion(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Habilitar acceso") }
                    }
                }
            }

            OutlinedTextField(
                value = whatsappTelPadre1,
                onValueChange = { whatsappTelPadre1 = it },
                label = { Text("Teléfono ${padres.getOrNull(0)?.nombre ?: "Padre 1"} (con código país)") },
                placeholder = { Text("5491112345678") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = habilitarWhatsApp
            )
            OutlinedTextField(
                value = whatsappTelPadre2,
                onValueChange = { whatsappTelPadre2 = it },
                label = { Text("Teléfono ${padres.getOrNull(1)?.nombre ?: "Padre 2"} (con código país)") },
                placeholder = { Text("5491198765432") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = habilitarWhatsApp
            )

            OutlinedTextField(
                value = whatsappGruposEscuela,
                onValueChange = { whatsappGruposEscuela = it },
                label = { Text("Grupos escolares (separados por coma)") },
                placeholder = { Text("Grupo Padres Sala 3, Jardín Arcoiris") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        "Los mensajes de texto de estos grupos se analizan para detectar eventos escolares automáticamente.",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                enabled = habilitarWhatsApp
            )

            ComandosCard(
                titulo = "Comandos WhatsApp / Telegram",
                comandos = listOf(
                    "/compra [producto] [#categoria]" to "Agrega un ítem a la lista de compras",
                    "/lista" to "Muestra la lista de compras actual",
                    "/gasto [monto] [descripcion]" to "Registra un gasto",
                    "/tiempo [hijo] [desde] [hasta]" to "Registra tiempo con un hijo",
                    "/evento [titulo] [YYYY-MM-DD]" to "Crea un evento en el calendario",
                    "/aceptar / /rechazar [id]" to "Acepta o rechaza una compensación",
                    "/estado" to "Ver compensaciones pendientes"
                )
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Sección Telegram ─────────────────────────────────────────────
            SeccionIntegracion(
                titulo = "Telegram Bot",
                icono = Icons.Outlined.Send,
                onAyuda = { mostrarAyuda = "telegram" }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Habilitar Telegram", modifier = Modifier.weight(1f))
                Switch(checked = habilitarTelegram, onCheckedChange = { habilitarTelegram = it })
            }

            OutlinedTextField(
                value = telegramToken,
                onValueChange = { telegramToken = it },
                label = { Text("Bot Token") },
                placeholder = { Text("1234567890:ABC-DEF...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = habilitarTelegram
            )

            val nombrePadre1 = padres.getOrNull(0)?.nombre ?: "Padre 1"
            val nombrePadre2 = padres.getOrNull(1)?.nombre ?: "Padre 2"

            OutlinedTextField(
                value = chatIdPadre1,
                onValueChange = { chatIdPadre1 = it },
                label = { Text("Chat ID de $nombrePadre1") },
                placeholder = { Text("Ej: 123456789") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = habilitarTelegram
            )
            OutlinedTextField(
                value = chatIdPadre2,
                onValueChange = { chatIdPadre2 = it },
                label = { Text("Chat ID de $nombrePadre2") },
                placeholder = { Text("Ej: 987654321") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = habilitarTelegram
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Sección Email ────────────────────────────────────────────────
            SeccionIntegracion(
                titulo = "Email (IMAP)",
                icono = Icons.Outlined.Email,
                onAyuda = { mostrarAyuda = "email" }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Habilitar Email", modifier = Modifier.weight(1f))
                Switch(checked = habilitarEmail, onCheckedChange = { habilitarEmail = it })
            }

            OutlinedTextField(value = emailHost, onValueChange = { emailHost = it },
                label = { Text("Servidor IMAP") }, placeholder = { Text("imap.gmail.com") },
                modifier = Modifier.fillMaxWidth(), enabled = habilitarEmail)
            OutlinedTextField(
                value = emailPort,
                onValueChange = { emailPort = it.filter { c -> c.isDigit() } },
                label = { Text("Puerto") }, placeholder = { Text("993") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = habilitarEmail)
            OutlinedTextField(value = emailUser, onValueChange = { emailUser = it },
                label = { Text("Email / Usuario") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = habilitarEmail)
            OutlinedTextField(value = emailPassword, onValueChange = { emailPassword = it },
                label = { Text("Contraseña de aplicación") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(), enabled = habilitarEmail)

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    text = "📌 El asunto del email debe empezar con \"CRIANZA:\" seguido del comando.\nEjemplo: CRIANZA: /gasto 500 supermercado",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Filtros de Email ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Filtros de Email",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { mostrarDialogoFiltro = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar")
                }
            }

            Text(
                text = "Solo se procesan emails que coincidan con al menos uno de estos filtros.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            filtros.forEach { filtro ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (filtro.tipo == "remitente") "Remitente" else "Asunto",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(filtro.valor)
                        }
                        IconButton(onClick = { onEliminarFiltro(filtro) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar filtro",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    }  // Box

    // ── Diálogos de ayuda ────────────────────────────────────────────────────
    when (mostrarAyuda) {
        "general" -> DialogoAyuda(
            titulo = "¿Cómo configurar las integraciones?",
            onDismiss = { mostrarAyuda = null },
            pasos = listOf(
                "🟢 WhatsApp" to "Habilitá el acceso a notificaciones y registrá los números de cada padre con código de país (ej: 5491112345678). Luego mandá comandos por WhatsApp al número registrado.",
                "🔵 Telegram" to "Creá un bot con @BotFather, obtené el token y el Chat ID de cada padre con @userinfobot. Pegá los datos en los campos correspondientes.",
                "📧 Email" to "Configurá una contraseña de aplicación de Gmail y usá imap.gmail.com:993. Los emails deben tener el asunto empezando con CRIANZA:"
            )
        )
        "whatsapp" -> DialogoAyuda(
            titulo = "Cómo configurar WhatsApp",
            onDismiss = { mostrarAyuda = null },
            pasos = listOf(
                "Paso 1" to "En tu Android, andá a Ajustes → Apps → Acceso especial → Acceso a notificaciones.",
                "Paso 2" to "Encontrá Crianza en la lista y habilitá el acceso a notificaciones.",
                "Paso 3" to "Volvé aquí, activá el toggle de WhatsApp y escribí el número de cada padre con código de país. Ejemplo Argentina: 5491112345678 (54 = país, 9 = celular, luego el número sin 0 ni 15).",
                "Paso 4" to "Mandá mensajes de WhatsApp con los comandos listados. La app los detectará automáticamente en segundo plano.",
                "⚠️ Importante" to "WhatsApp debe estar instalado en el mismo teléfono. Esta función lee notificaciones entrantes, no accede a tu cuenta de WhatsApp."
            )
        )
        "telegram" -> DialogoAyuda(
            titulo = "Cómo configurar Telegram",
            onDismiss = { mostrarAyuda = null },
            pasos = listOf(
                "Paso 1 — Crear el bot" to "Abrí Telegram y buscá @BotFather. Mandá el comando /newbot y seguí las instrucciones. Elegí un nombre y un nombre de usuario (debe terminar en 'bot'). Al finalizar te dará un Token como: 1234567890:ABCdef-GHIjkl.",
                "Paso 2 — Obtener tu Chat ID" to "Buscá el bot @userinfobot en Telegram y mandá /start. Te responderá con tu ID numérico. Cada padre debe hacer esto con su propia cuenta de Telegram.",
                "Paso 3 — Configurar en la app" to "Pegá el Token del bot en el campo 'Bot Token'. Luego ingresá el Chat ID de cada padre en los campos correspondientes.",
                "Paso 4 — Activar" to "Habilitá el toggle de Telegram, guardá la configuración y empezá a mandar comandos al bot.",
                "Paso 5 — Comandos" to "Buscá tu bot por su nombre de usuario en Telegram y mandá /start. A partir de ahí podés usar todos los comandos listados. El bot te responderá confirmando cada acción."
            )
        )
        "email" -> DialogoAyuda(
            titulo = "Cómo configurar Email (Gmail)",
            onDismiss = { mostrarAyuda = null },
            pasos = listOf(
                "Paso 1 — Activar verificación en 2 pasos" to "Para usar contraseñas de aplicación, primero necesitás tener la verificación en 2 pasos activada en tu cuenta de Google (myaccount.google.com → Seguridad).",
                "Paso 2 — Crear contraseña de aplicación" to "Andá a myaccount.google.com → Seguridad → Contraseñas de aplicación. Seleccioná 'Correo' y 'Dispositivo Android'. Google te dará una contraseña de 16 caracteres.",
                "Paso 3 — Configurar en la app" to "Servidor: imap.gmail.com · Puerto: 993 · Usuario: tu dirección de Gmail completa · Contraseña: la contraseña de 16 caracteres (NO tu contraseña normal).",
                "Paso 4 — Formato de comandos por email" to "El asunto del email DEBE empezar con 'CRIANZA:' seguido del comando. Ejemplos:\n• CRIANZA: /gasto 1500 farmacia\n• CRIANZA: /evento Cita médica 2025-06-15\n• CRIANZA: /compra leche #alimentos",
                "Paso 5 — Filtros (opcional)" to "Agregá filtros para que solo se procesen emails de remitentes o asuntos específicos. Esto evita procesar emails no relacionados con la crianza.",
                "⚠️ Seguridad" to "La contraseña de aplicación es diferente a tu contraseña de Gmail. Solo da acceso de lectura a tu bandeja. Podés revocarla en cualquier momento desde myaccount.google.com."
            )
        )
    }

    if (mostrarDialogoFiltro) {
        DialogoAgregarFiltro(
            onDismiss = { mostrarDialogoFiltro = false },
            onGuardar = { tipo, valor ->
                onAgregarFiltro(FiltroEmail(id = UUID.randomUUID().toString(), tipo = tipo, valor = valor))
                mostrarDialogoFiltro = false
            }
        )
    }
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
