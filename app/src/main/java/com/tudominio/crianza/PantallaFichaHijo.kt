@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tudominio.crianza.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PantallaFichaHijo(
    hijo: Hijo,
    onAtras: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    var ficha by remember { mutableStateOf(FichaHijo(idHijo = hijo.id)) }
    var cargada by remember { mutableStateOf(false) }
    var mensajeGuardado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(hijo.id) {
        ficha = db.fichaHijoDao().obtenerPorHijo(hijo.id) ?: FichaHijo(idHijo = hijo.id)
        cargada = true
    }

    fun update(modify: (FichaHijo) -> FichaHijo) {
        ficha = modify(ficha)
    }

    Box(Modifier.fillMaxSize().background(BgGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Ficha de ${hijo.nombre}", color = Neutral10) },
                    navigationIcon = {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            scope.launch {
                                db.fichaHijoDao().guardar(ficha)
                                mensajeGuardado = "Guardado"
                            }
                        }) {
                            Text("Guardar", fontWeight = FontWeight.Bold, color = Neutral10)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (!cargada) {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Datos rápidos que sirven cuando vos no estás (médico de urgencia, talles, vacunas próximas).",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant50
                )

                SeccionTituloLocal("Salud")
                CampoTexto("Obra social", ficha.obraSocial) { v -> update { it.copy(obraSocial = v) } }
                CampoTexto("Número de afiliado", ficha.numAfiliado) { v -> update { it.copy(numAfiliado = v) } }
                CampoTexto("Pediatra", ficha.pediatra) { v -> update { it.copy(pediatra = v) } }
                CampoTexto("Teléfono pediatra", ficha.telefonoPediatra) { v -> update { it.copy(telefonoPediatra = v) } }
                CampoTexto("Grupo sanguíneo", ficha.grupoSanguineo) { v -> update { it.copy(grupoSanguineo = v) } }

                SeccionTituloLocal("Alergias y medicación")
                CampoTexto("Alergias (separadas por coma)", ficha.alergias, multiLinea = true) { v -> update { it.copy(alergias = v) } }
                CampoTexto("Medicación habitual", ficha.medicacion, multiLinea = true) { v -> update { it.copy(medicacion = v) } }

                SeccionTituloLocal("Vacunación")
                CampoFecha(
                    value = ficha.proximaVacuna,
                    label = "Próxima vacuna",
                    onValueChange = { v -> update { it.copy(proximaVacuna = v) } },
                    modifier = Modifier.fillMaxWidth()
                )

                SeccionTituloLocal("Talles")
                CampoTexto("Ropa", ficha.talleRopa) { v -> update { it.copy(talleRopa = v) } }
                CampoTexto("Zapato", ficha.talleZapato) { v -> update { it.copy(talleZapato = v) } }

                SeccionTituloLocal("Notas")
                CampoTexto("Notas (escuela, terapeuta, etc)", ficha.notas, multiLinea = true) { v -> update { it.copy(notas = v) } }

                mensajeGuardado?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SeccionTituloLocal(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun CampoTexto(label: String, value: String, multiLinea: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = !multiLinea,
        minLines = if (multiLinea) 2 else 1,
        modifier = Modifier.fillMaxWidth()
    )
}
