@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                title = { Text("Resumen de tiempo", color = Neutral10) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoConfig = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Ajustar porcentaje", tint = NeutralVariant30)
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

            // ── Hero: Total de horas ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Indigo30, Indigo40, Teal30)))
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                // Circulo decorativo grande
                Box(
                    Modifier.size(140.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-40).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.06f))
                )
                // Circulo decorativo chico
                Box(
                    Modifier.size(70.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-20).dp, y = 20.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.04f))
                )
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "HORAS TOTALES",
                        color = Color.White.copy(.55f),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(12.dp))
                    // Numero grande y prominente
                    val horasEnteras = totalHoras.toInt()
                    val minutosSobrantes = ((totalHoras - horasEnteras) * 60).toInt()
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "$horasEnteras",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-3).sp
                        )
                        if (minutosSobrantes > 0) {
                            Text(
                                "h ${minutosSobrantes}m",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(.65f),
                                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                            )
                        } else {
                            Text(
                                "h",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(.65f),
                                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color.White.copy(.1f))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "horas registradas",
                            color = Color.White.copy(.6f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Distribucion visual ────────────────────────────────────────
            val Lavender = Indigo40
            val PinkBar  = Rose40
            val Mint     = Teal40
            val AmberW   = Rose40

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0x40312E81), Color(0x403730A3)))
                    )
            ) {
                // Glass overlay interno
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color(0x1AFFFFFF))
                ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Distribución de tiempo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Neutral10
                    )
                    Text(
                        "Período: ${textoPeriodo(periodoSeleccionado)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant50
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (padres.size >= 2) {
                        val horasPadre1 = horasPorPadre[padres[0].id] ?: 0.0
                        val horasPadre2 = horasPorPadre[padres[1].id] ?: 0.0

                        val porcentajeReal1 = if (totalHoras > 0) (horasPadre1 / totalHoras * 100).toInt() else 0
                        val porcentajeReal2 = if (totalHoras > 0) (horasPadre2 / totalHoras * 100).toInt() else 0

                        val diferencia1 = porcentajeReal1 - configuracion.porcentajePadre1
                        val diferencia2 = porcentajeReal2 - configuracion.porcentajePadre2

                        val horasDeuda1 = if (totalHoras > 0) (abs(diferencia1) * totalHoras / 100) else 0.0
                        val horasDeuda2 = if (totalHoras > 0) (abs(diferencia2) * totalHoras / 100) else 0.0

                        // ── Barra de split horizontal ──────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GlassWhite)
                        ) {
                            Row(Modifier.fillMaxSize()) {
                                // Lado padre 1
                                val frac1 = (porcentajeReal1 / 100f).coerceIn(0.05f, 0.95f)
                                Box(
                                    Modifier
                                        .fillMaxWidth(frac1)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                                        .background(Brush.horizontalGradient(listOf(Lavender.copy(.6f), Lavender)))
                                ) {
                                    Row(
                                        modifier = Modifier.align(Alignment.Center),
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "$porcentajeReal1",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            "%",
                                            color = Color.White.copy(.7f),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(bottom = 1.dp)
                                        )
                                    }
                                }
                                // Separador
                                Box(Modifier.width(2.dp).fillMaxHeight().background(GlassWhite))
                                // Lado padre 2
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                                        .background(Brush.horizontalGradient(listOf(PinkBar, PinkBar.copy(.6f))))
                                ) {
                                    Row(
                                        modifier = Modifier.align(Alignment.Center),
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "$porcentajeReal2",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            "%",
                                            color = Color.White.copy(.7f),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(bottom = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Nombres bajo la barra con horas
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(Lavender))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(padres[0].nombre, color = Neutral10, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                    Text("${String.format("%.1f", horasPadre1)} hs", color = NeutralVariant50, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(padres[1].nombre, color = Neutral10, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                    Text("${String.format("%.1f", horasPadre2)} hs", color = NeutralVariant50, style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(10.dp).clip(CircleShape).background(PinkBar))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Objetivo marcador
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlassWhite)
                                .padding(12.dp),
                            Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Objetivo", color = NeutralVariant50, style = MaterialTheme.typography.labelSmall)
                                Text("${configuracion.porcentajePadre1}% / ${configuracion.porcentajePadre2}%", color = Neutral10, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Horas", color = NeutralVariant50, style = MaterialTheme.typography.labelSmall)
                                Text("${String.format("%.1f", horasPadre1)} / ${String.format("%.1f", horasPadre2)}", color = Neutral10, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Detalle de diferencias
                        @Composable
                        fun DetallePadre(nombre: String, pctReal: Int, pctObj: Int, dif: Int, color: Color) {
                            val difColor = when {
                                dif > 0 -> Mint
                                dif < 0 -> Red40
                                else -> NeutralVariant50
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                Arrangement.SpaceBetween,
                                Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                                    Spacer(Modifier.width(8.dp))
                                    Text(nombre, color = Neutral10, style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(50.dp))
                                            .background(color.copy(.2f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("$pctReal%", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        "${if (dif > 0) "+" else ""}$dif%",
                                        color = difColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        DetallePadre(padres[0].nombre, porcentajeReal1, configuracion.porcentajePadre1, diferencia1, Lavender)
                        DetallePadre(padres[1].nombre, porcentajeReal2, configuracion.porcentajePadre2, diferencia2, PinkBar)

                        // Compensacion sugerida
                        if (diferencia1 < 0 || diferencia2 < 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val (deudor, acreedor, hDeuda) = if (diferencia1 < 0)
                                Triple(padres[0].nombre, padres[1].nombre, horasDeuda1)
                            else
                                Triple(padres[1].nombre, padres[0].nombre, horasDeuda2)

                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AmberW.copy(.12f))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    Arrangement.SpaceBetween,
                                    Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Compensación sugerida", color = AmberW.copy(.7f), style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            "$deudor debe ${String.format("%.1f", hDeuda)} hs",
                                            color = AmberW,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text("a $acreedor", color = NeutralVariant50, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        "${String.format("%.0f", hDeuda)}h",
                                        color = AmberW,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 24.sp,
                                        letterSpacing = (-0.5).sp
                                    )
                                }
                            }
                        }
                    }
                }
            } // inner glass Box
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Detalle por hijo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Neutral10
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
    val horasPorPadre = calcularHorasPorPadre(registros)
    val total = horasPorPadre.values.sum()
    val Lavender = Indigo40
    val PinkBar  = Rose40

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = hijo.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Neutral10
            )
            Spacer(modifier = Modifier.height(10.dp))
            padres.forEachIndexed { index, padre ->
                val horas = horasPorPadre[padre.id] ?: 0.0
                val pct = if (total > 0) (horas / total * 100).toInt() else 0
                val color = if (index == 0) Lavender else PinkBar
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(8.dp))
                        Text(padre.nombre, color = Neutral10, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "${String.format("%.1f", horas)}h ($pct%)",
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            // Mini barra de split
            if (padres.size >= 2 && total > 0) {
                Spacer(Modifier.height(8.dp))
                val h1 = horasPorPadre[padres[0].id] ?: 0.0
                val frac = (h1 / total).toFloat().coerceIn(0.02f, 0.98f)
                Box(
                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50.dp)).background(GlassWhite)
                ) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)).background(Lavender))
                        Box(Modifier.fillMaxWidth().fillMaxHeight().clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)).background(PinkBar))
                    }
                }
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
