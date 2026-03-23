@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tudominio.crianza.ui.theme.*
import kotlin.math.abs
import java.util.*

@Composable
fun PantallaResumenTiempo(
    hijos: List<Hijo>,
    padres: List<Padre>,
    registros: List<RegistroTiempo>,
    configuracion: ConfiguracionTiempo,
    onGuardarConfiguracion: (ConfiguracionTiempo) -> Unit,
    onAtras: () -> Unit
) {
    var mostrarDialogoConfig by remember { mutableStateOf(false) }
    var periodoSeleccionado by remember { mutableStateOf(Periodo.TODO) }
    var hijoSeleccionado by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(BgGrad0)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Resumen de tiempo", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoConfig = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Ajustar porcentaje", tint = Color.White)
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PeriodoButton(
                    texto = "Semana",
                    seleccionado = periodoSeleccionado == Periodo.SEMANA,
                    onClick = { periodoSeleccionado = Periodo.SEMANA }
                )
                PeriodoButton(
                    texto = "Mes",
                    seleccionado = periodoSeleccionado == Periodo.MES,
                    onClick = { periodoSeleccionado = Periodo.MES }
                )
                PeriodoButton(
                    texto = "Año",
                    seleccionado = periodoSeleccionado == Periodo.ANIO,
                    onClick = { periodoSeleccionado = Periodo.ANIO }
                )
                PeriodoButton(
                    texto = "Todo",
                    seleccionado = periodoSeleccionado == Periodo.TODO,
                    onClick = { periodoSeleccionado = Periodo.TODO }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            var expandedHijo by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedHijo,
                onExpandedChange = { expandedHijo = it }
            ) {
                OutlinedTextField(
                    value = if (hijoSeleccionado == null) "Todos los hijos"
                    else hijos.find { it.id == hijoSeleccionado }?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrar por hijo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedHijo,
                    onDismissRequest = { expandedHijo = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todos los hijos") },
                        onClick = {
                            hijoSeleccionado = null
                            expandedHijo = false
                        }
                    )
                    hijos.forEach { hijo ->
                        DropdownMenuItem(
                            text = { Text(hijo.nombre) },
                            onClick = {
                                hijoSeleccionado = hijo.id
                                expandedHijo = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val registrosFiltrados = registros.filter { registro ->
                val fechaRegistro = parseFecha(registro.fecha)
                val dentroDePeriodo = when (periodoSeleccionado) {
                    Periodo.SEMANA -> esMismaSemana(fechaRegistro, Date())
                    Periodo.MES -> esMismoMes(fechaRegistro, Date())
                    Periodo.ANIO -> esMismoAnio(fechaRegistro, Date())
                    Periodo.TODO -> true
                }
                val coincideHijo = hijoSeleccionado == null || registro.idHijo == hijoSeleccionado
                dentroDePeriodo && coincideHijo
            }

            val horasPorPadre = calcularHorasPorPadre(registrosFiltrados)
            val totalHoras = horasPorPadre.values.sum()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassWhite)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Distribución de tiempo",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )

                    Text(
                        text = "Período: ${textoPeriodo(periodoSeleccionado)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    if (padres.size >= 2) {
                        val horasPadre1 = horasPorPadre[padres[0].id] ?: 0.0
                        val horasPadre2 = horasPorPadre[padres[1].id] ?: 0.0

                        val porcentajeReal1 = if (totalHoras > 0) (horasPadre1 / totalHoras * 100).toInt() else 0
                        val porcentajeReal2 = if (totalHoras > 0) (horasPadre2 / totalHoras * 100).toInt() else 0

                        val diferencia1 = porcentajeReal1 - configuracion.porcentajePadre1
                        val diferencia2 = porcentajeReal2 - configuracion.porcentajePadre2

                        val horasDeuda1 = if (totalHoras > 0) (abs(diferencia1) * totalHoras / 100) else 0.0
                        val horasDeuda2 = if (totalHoras > 0) (abs(diferencia2) * totalHoras / 100) else 0.0

                        Text(
                            text = padres[0].nombre,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Horas: %.1f".format(horasPadre1),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Porcentaje real: $porcentajeReal1%",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Porcentaje ideal: ${configuracion.porcentajePadre1}%",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Diferencia: ${if (diferencia1 > 0) "+$diferencia1" else diferencia1}%",
                            color = if (diferencia1 > 0) Color.Green else if (diferencia1 < 0) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = padres[1].nombre,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Horas: %.1f".format(horasPadre2),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Porcentaje real: $porcentajeReal2%",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Porcentaje ideal: ${configuracion.porcentajePadre2}%",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Diferencia: ${if (diferencia2 > 0) "+$diferencia2" else diferencia2}%",
                            color = if (diferencia2 > 0) Color.Green else if (diferencia2 < 0) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (diferencia1 < 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Compensación sugerida:",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${padres[0].nombre} debe %.1f horas a ${padres[1].nombre}".format(horasDeuda1)
                                    )
                                }
                            }
                        } else if (diferencia2 < 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Compensación sugerida:",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${padres[1].nombre} debe %.1f horas a ${padres[0].nombre}".format(horasDeuda2)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Detalle por hijo",
                style = MaterialTheme.typography.titleLarge
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(hijos) { hijo ->
                    val registrosHijo = registrosFiltrados.filter { it.idHijo == hijo.id }
                    if (registrosHijo.isNotEmpty() || hijoSeleccionado == null) {
                        ResumenHijoCard(
                            hijo = hijo,
                            padres = padres,
                            registros = registrosHijo
                        )
                    }
                }
            }
        }
    }
    }  // Box

    if (mostrarDialogoConfig) {
        DialogoConfiguracionTiempo(
            configuracion = configuracion,
            onDismiss = { mostrarDialogoConfig = false },
            onGuardar = {
                onGuardarConfiguracion(it)
                mostrarDialogoConfig = false
            }
        )
    }
}

@Composable
fun ResumenHijoCard(
    hijo: Hijo,
    padres: List<Padre>,
    registros: List<RegistroTiempo>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = hijo.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            val horasPorPadre = calcularHorasPorPadre(registros)
            padres.forEach { padre ->
                val horas = horasPorPadre[padre.id] ?: 0.0
                Text(text = "${padre.nombre}: %.1f horas".format(horas))
            }
        }
    }
}

@Composable
fun DialogoConfiguracionTiempo(
    configuracion: ConfiguracionTiempo,
    onDismiss: () -> Unit,
    onGuardar: (ConfiguracionTiempo) -> Unit
) {
    var porcentaje1 by remember { mutableStateOf(configuracion.porcentajePadre1.toString()) }
    var porcentaje2 by remember { mutableStateOf(configuracion.porcentajePadre2.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar porcentaje de tiempo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Porcentaje para padre 1:")
                OutlinedTextField(
                    value = porcentaje1,
                    onValueChange = { porcentaje1 = it.filter { char -> char.isDigit() } },
                    label = { Text("Porcentaje (0-100)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Porcentaje para padre 2:")
                OutlinedTextField(
                    value = porcentaje2,
                    onValueChange = { porcentaje2 = it.filter { char -> char.isDigit() } },
                    label = { Text("Porcentaje (0-100)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Total: ${(porcentaje1.toIntOrNull() ?: 0) + (porcentaje2.toIntOrNull() ?: 0)}%",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p1 = porcentaje1.toIntOrNull() ?: 50
                    val p2 = porcentaje2.toIntOrNull() ?: 50
                    onGuardar(configuracion.copy(porcentajePadre1 = p1, porcentajePadre2 = p2))
                },
                enabled = (porcentaje1.toIntOrNull() ?: 0) + (porcentaje2.toIntOrNull() ?: 0) == 100
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PantallaResumenTiempoPreview() {
    CrianzaTheme {
        PantallaResumenTiempo(
            hijos = listOf(Hijo(id = "1", nombre = "Hijo 1")),
            padres = listOf(Padre(id = "1", nombre = "Padre 1"), Padre(id = "2", nombre = "Padre 2")),
            registros = listOf(),
            configuracion = ConfiguracionTiempo(),
            onGuardarConfiguracion = {},
            onAtras = {}
        )
    }
}
