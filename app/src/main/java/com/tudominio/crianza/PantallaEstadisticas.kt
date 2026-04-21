@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tudominio.crianza.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PantallaEstadisticas(onAtras: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }

    var gastosPorMes by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var horasPorPadre by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var categoriasTop by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var totalMes by remember { mutableStateOf(0.0) }
    var totalAno by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        scope.launch {
            val gastos = db.gastoDao().obtenerTodosLosGastos()
            val registros = db.registroTiempoDao().obtenerTodosLosRegistros()

            val fmtMes = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val fmtLabel = SimpleDateFormat("MMM", Locale("es", "AR"))
            val cal = Calendar.getInstance()
            val mesesKeys = (5 downTo 0).map {
                val c = Calendar.getInstance().apply { add(Calendar.MONTH, -it) }
                fmtMes.format(c.time) to fmtLabel.format(c.time).replaceFirstChar { ch -> ch.uppercase() }
            }
            gastosPorMes = mesesKeys.map { (key, label) ->
                label to gastos.filter { it.fecha.startsWith(key) }.sumOf { it.monto }
            }

            val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val mesActual = hoy.substring(0, 7)
            val anoActual = hoy.substring(0, 4)
            totalMes = gastos.filter { it.fecha.startsWith(mesActual) }.sumOf { it.monto }
            totalAno = gastos.filter { it.fecha.startsWith(anoActual) }.sumOf { it.monto }

            val cuatroSemAtras = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -28) }
            val fechaDesde = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cuatroSemAtras.time)
            horasPorPadre = registros
                .filter { it.fecha >= fechaDesde }
                .groupBy { it.nombrePadre }
                .map { (padre, regs) ->
                    padre to regs.sumOf { r ->
                        try {
                            val (hiH, hiM) = r.horaInicio.split(":").map { it.toInt() }
                            val (hfH, hfM) = r.horaFin.split(":").map { it.toInt() }
                            ((hfH * 60 + hfM) - (hiH * 60 + hiM)).coerceAtLeast(0)
                        } catch (_: Exception) { 0 }
                    } / 60.0
                }
                .sortedByDescending { it.second }

            categoriasTop = gastos
                .filter { it.fecha.startsWith(mesActual) }
                .groupBy { it.categoria.ifBlank { "Sin categoría" } }
                .map { (cat, gs) -> cat to gs.sumOf { it.monto } }
                .sortedByDescending { it.second }
                .take(5)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Neutral10,
                    navigationIconContentColor = Neutral10
                )
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgGradientMain)
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Totales
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaTotal("Gastos del mes", "$${totalMes.toLong()}", Indigo40, Modifier.weight(1f))
                TarjetaTotal("Gastos del año", "$${totalAno.toLong()}", Rose40, Modifier.weight(1f))
            }

            // Gastos por mes
            TarjetaSeccion("Gastos últimos 6 meses") {
                GraficoBarras(
                    datos = gastosPorMes.map { it.first to it.second.toFloat() },
                    colorBarra = Indigo80
                )
            }

            // Horas por padre
            if (horasPorPadre.isNotEmpty()) {
                TarjetaSeccion("Horas por padre (últimas 4 semanas)") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val max = horasPorPadre.maxOf { it.second }.coerceAtLeast(1.0)
                        horasPorPadre.forEach { (padre, horas) ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(padre, fontWeight = FontWeight.Medium, color = Neutral10)
                                    Text("${"%.1f".format(horas)}h", color = NeutralVariant30)
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeutralVariant80)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = (horas / max).toFloat())
                                            .fillMaxHeight()
                                            .background(Teal40)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top categorías
            if (categoriasTop.isNotEmpty()) {
                TarjetaSeccion("Top categorías este mes") {
                    val totalCat = categoriasTop.sumOf { it.second }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        categoriasTop.forEach { (cat, monto) ->
                            val pct = if (totalCat > 0) (monto / totalCat * 100).toInt() else 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$cat ($pct%)", color = Neutral10)
                                Text("$${monto.toLong()}", color = NeutralVariant30)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TarjetaTotal(titulo: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GlassWhiteHeavy)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = NeutralVariant30)
            Spacer(Modifier.height(4.dp))
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun TarjetaSeccion(titulo: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassWhiteHeavy)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, color = Neutral10)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun GraficoBarras(datos: List<Pair<String, Float>>, colorBarra: Color) {
    val max = (datos.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val anchoBarra = size.width / (datos.size * 1.5f)
        val espacio = anchoBarra / 2f
        val altoGrafico = size.height - 20f
        datos.forEachIndexed { i, (_, v) ->
            val h = (v / max) * altoGrafico
            val x = i * (anchoBarra + espacio) + espacio
            drawRect(
                color = colorBarra,
                topLeft = Offset(x, altoGrafico - h),
                size = Size(anchoBarra, h)
            )
        }
        drawLine(
            color = Color(0xFFB8B8B8),
            start = Offset(0f, altoGrafico),
            end = Offset(size.width, altoGrafico),
            strokeWidth = 1.5f
        )
    }
    // Labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        datos.forEach { (label, v) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontSize = 10.sp, color = NeutralVariant30)
                Text("$${v.toLong()}", fontSize = 9.sp, color = NeutralVariant50)
            }
        }
    }
}
