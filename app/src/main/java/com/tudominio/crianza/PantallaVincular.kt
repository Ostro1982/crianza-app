@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.tudominio.crianza

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PantallaVincular(
    onVinculado: () -> Unit,
    onAtras: () -> Unit
) {
    val context = LocalContext.current
    val familyId = remember { FamilyIdManager.obtenerFamilyId(context) }
    var codigoIngresado by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
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
                .padding(24.dp),
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
                                "Instala Crianza y usá este código para sincronizar nuestra familia:\n$familyId"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir código"))
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Compartir código")
                    }
                }
            }

            Divider()
            Text(
                "— o ingresá el código del otro padre —",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // ── Ingresar código del otro ──────────────────────────────────────
            OutlinedTextField(
                value = codigoIngresado,
                onValueChange = { codigoIngresado = it.trim(); error = null },
                label = { Text("Código del otro padre") },
                placeholder = { Text("Pegá el UUID completo aquí") },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
                supportingText = error?.let { msg -> { Text(msg) } }
            )

            Button(
                onClick = {
                    if (FamilyIdManager.vincularConCodigo(context, codigoIngresado)) {
                        mostrarConfirmacion = true
                    } else {
                        error = "Código inválido. Pegá el UUID completo (ej: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = codigoIngresado.length >= 32
            ) {
                Text("Vincular con este código")
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
