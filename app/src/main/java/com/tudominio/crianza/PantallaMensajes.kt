@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.draw.clip
import com.tudominio.crianza.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PantallaMensajes(
    mensajes: List<Mensaje>,
    padres: List<Padre>,
    configuracion: ConfiguracionIntegracion = ConfiguracionIntegracion(),
    onEnviar: (Mensaje) -> Unit,
    onAtras: () -> Unit
) {
    var texto by remember { mutableStateOf("") }
    var remitenteSeleccionado by remember { mutableStateOf(padres.firstOrNull()?.id ?: "") }
    val listState = rememberLazyListState()
    val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    Box(Modifier.fillMaxSize().background(BgGrad6)) {
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Mensajes internos", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column {
                // Selector de remitente
                if (padres.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassWhiteHeavy)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enviando como:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        padres.forEach { padre ->
                            FilterChip(
                                selected = remitenteSeleccionado == padre.id,
                                onClick = { remitenteSeleccionado = padre.id },
                                label = { Text(padre.nombre, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Campo de texto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassWhiteHeavy)
                        .navigationBarsPadding()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        placeholder = { Text("Escribir mensaje…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            val padre = padres.find { it.id == remitenteSeleccionado }
                            if (texto.isNotBlank() && padre != null) {
                                onEnviar(
                                    Mensaje(
                                        id = UUID.randomUUID().toString(),
                                        idEmisor = padre.id,
                                        nombreEmisor = padre.nombre,
                                        texto = texto.trim(),
                                        fechaCompleta = System.currentTimeMillis()
                                    )
                                )
                                texto = ""
                            }
                        },
                        enabled = texto.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = if (texto.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val waActivo = configuracion.habilitarWhatsApp &&
            (configuracion.whatsappTelefonoPadre1.isNotEmpty() || configuracion.whatsappTelefonoPadre2.isNotEmpty())

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Card de ayuda WhatsApp
            if (waActivo) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(GlassWhite)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📱 Comandos por WhatsApp",
                                fontWeight = FontWeight.Bold, color = Color.White)
                            if (configuracion.whatsappTelefonoPadre1.isNotEmpty())
                                Text("Padre 1: +${configuracion.whatsappTelefonoPadre1}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall)
                            if (configuracion.whatsappTelefonoPadre2.isNotEmpty())
                                Text("Padre 2: +${configuracion.whatsappTelefonoPadre2}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(2.dp))
                            val comandos = listOf(
                                "/gasto 1500 farmacia",
                                "/evento Cita médica 2025-06-15",
                                "/tiempo Sofia 8 16",
                                "/compra leche #alimentos",
                                "/recuerdo Primer paso: descripción",
                                "/lista  •  /estado"
                            )
                            comandos.forEach {
                                Text("  $it",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            if (mensajes.isEmpty() && !waActivo) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay mensajes. ¡Empezá la conversación!", color = Color.White)
                    }
                }
            }

            items(mensajes) { mensaje ->
                val esPropio = mensaje.idEmisor == remitenteSeleccionado
                BurbujaMensaje(
                    mensaje = mensaje,
                    esPropio = esPropio,
                    hora = sdf.format(Date(mensaje.fechaCompleta))
                )
            }
        }
    }
    }  // Box
}

@Composable
fun BurbujaMensaje(
    mensaje: Mensaje,
    esPropio: Boolean,
    hora: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (esPropio) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (esPropio) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!esPropio) {
                Text(
                    mensaje.nombreEmisor,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC4B5FD),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (esPropio) 16.dp else 4.dp,
                    topEnd = if (esPropio) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (esPropio) Color(0xFF7C3AED)
                else Color(0x4DFFFFFF)
            ) {
                Text(
                    mensaje.texto,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Text(
                hora,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}
