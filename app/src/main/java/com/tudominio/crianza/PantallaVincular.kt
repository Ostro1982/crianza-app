@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.tudominio.crianza

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PantallaVincular(
    onVinculado: () -> Unit,
    onAtras: () -> Unit,
    onBuscarEmail: suspend (String) -> Pair<String, String>? = { null }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val familyId = remember { FamilyIdManager.obtenerFamilyId(context) }

    // UUID manual
    var codigoIngresado by remember { mutableStateOf("") }
    var errorUuid by remember { mutableStateOf<String?>(null) }

    // Búsqueda por email
    var emailIngresado by remember { mutableStateOf("") }
    var buscando by remember { mutableStateOf(false) }
    var resultadoBusqueda by remember { mutableStateOf<Pair<String, String>?>(null) } // (nombre, familyId)
    var errorEmail by remember { mutableStateOf<String?>(null) }
    var noEncontrado by remember { mutableStateOf(false) }

    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vincular dispositivos") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Tu código ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tu código de familia", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        familyId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Te invito a usar Crianza para gestionar nuestra familia juntos.\n\n" +
                                "📲 Descargá la app desde acá:\nhttps://github.com/Ostro1982/crianza-app/releases/latest\n\n" +
                                "Una vez instalada, ingresá este código para vincularnos:\n$familyId"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Invitar al otro padre"))
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Invitar al otro padre")
                    }
                }
            }

            Divider()
            Text(
                "— opción 1: ingresá el código del otro padre —",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // ── Ingresar UUID manualmente ─────────────────────────────────────
            OutlinedTextField(
                value = codigoIngresado,
                onValueChange = { codigoIngresado = it.trim(); errorUuid = null },
                label = { Text("Código del otro padre") },
                placeholder = { Text("Pegá el UUID completo aquí") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorUuid != null,
                supportingText = errorUuid?.let { msg -> { Text(msg) } }
            )

            Button(
                onClick = {
                    if (FamilyIdManager.vincularConCodigo(context, codigoIngresado)) {
                        mostrarConfirmacion = true
                    } else {
                        errorUuid = "Código inválido. Pegá el UUID completo (ej: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = codigoIngresado.length >= 32
            ) {
                Text("Vincular con este código")
            }

            Divider()
            Text(
                "— opción 2: buscá por email de Google —",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // ── Búsqueda por email ────────────────────────────────────────────
            OutlinedTextField(
                value = emailIngresado,
                onValueChange = {
                    emailIngresado = it.trim()
                    errorEmail = null
                    noEncontrado = false
                    resultadoBusqueda = null
                },
                label = { Text("Email de Google del otro padre") },
                placeholder = { Text("ejemplo@gmail.com") },
                modifier = Modifier.fillMaxWidth(),
                isError = errorEmail != null,
                supportingText = errorEmail?.let { msg -> { Text(msg) } }
            )

            Button(
                onClick = {
                    scope.launch {
                        buscando = true
                        noEncontrado = false
                        resultadoBusqueda = null
                        errorEmail = null
                        val resultado = onBuscarEmail(emailIngresado)
                        if (resultado == null) {
                            noEncontrado = true
                        } else {
                            resultadoBusqueda = resultado
                        }
                        buscando = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = emailIngresado.contains("@") && !buscando
            ) {
                if (buscando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Buscar")
            }

            if (noEncontrado) {
                Text(
                    "No se encontró ningún usuario con ese email. El otro padre debe abrir la app e iniciar sesión con Google primero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            resultadoBusqueda?.let { (nombre, fid) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Encontrado:", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(nombre, style = MaterialTheme.typography.titleMedium)
                        Text(emailIngresado, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                FamilyIdManager.vincularConCodigo(context, fid)
                                mostrarConfirmacion = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Vincular con la familia de $nombre")
                        }
                    }
                }
            }

            Text(
                "Una vez vinculados, eventos, gastos, compras y mensajes se sincronizan automáticamente entre ambos teléfonos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Vinculado") },
            text = {
                Text("Este teléfono ahora sincroniza con la familia del código ingresado. Reiniciá la app para que los cambios tomen efecto.")
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    onVinculado()
                }) { Text("Entendido") }
            }
        )
    }
}
