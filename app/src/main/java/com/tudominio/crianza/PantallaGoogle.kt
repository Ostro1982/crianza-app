@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PantallaGoogle(
    usuarioActual: UsuarioGoogle?,
    codigoFamiliar: String,
    onIniciarSesion: (UsuarioGoogle) -> Unit,
    onCerrarSesion: () -> Unit,
    onAtras: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleAuth = remember { GoogleAuthHelper(context) }
    var cargando by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuenta Google") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (usuarioActual == null) {
                // ── Sin sesión ────────────────────────────────────────────────
                Spacer(modifier = Modifier.height(24.dp))
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Conectá tu cuenta Google",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Iniciá sesión para identificarte y poder invitar al otro padre/madre a unirse.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )

                if (!googleAuth.configurado) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            "⚠️ Google Sign-In no está configurado.\nEl desarrollador debe completar el Client ID en GoogleAuthHelper.kt.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Button(
                    onClick = {
                        if (!googleAuth.configurado) {
                            errorMsg = "Configurá el GOOGLE_WEB_CLIENT_ID primero."
                            return@Button
                        }
                        scope.launch {
                            cargando = true
                            errorMsg = null
                            val resultado = googleAuth.iniciarSesion(context as Activity)
                            resultado.onSuccess { usuario ->
                                onIniciarSesion(usuario)
                            }.onFailure { e ->
                                errorMsg = e.message
                            }
                            cargando = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !cargando
                ) {
                    if (cargando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Iniciar sesión con Google")
                }

                errorMsg?.let { msg ->
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Divider()

                // Invitar sin cuenta
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Invitar sin cuenta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Podés compartir la app directamente:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { GoogleAuthHelper.compartirApp(context, codigoFamiliar) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Invitar co-padre/co-madre")
                        }
                    }
                }

            } else {
                // ── Con sesión activa ─────────────────────────────────────────
                Spacer(modifier = Modifier.height(24.dp))
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    usuarioActual.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    usuarioActual.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Código de familia", style = MaterialTheme.typography.labelMedium)
                        Text(
                            codigoFamiliar,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Compartí este código con el otro padre/madre para que pueda unirse.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Button(
                    onClick = { GoogleAuthHelper.compartirApp(context, codigoFamiliar) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invitar co-padre/co-madre")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            googleAuth.cerrarSesion()
                            onCerrarSesion()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}
