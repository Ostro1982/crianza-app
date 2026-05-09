@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tudominio.crianza.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Setup del scheduler de custodia. Pasos:
 * 1. Elegir patrón (5 presets).
 * 2. Elegir fecha de arranque (default: próximo lunes).
 * 3. Elegir cuántos meses generar (default 6).
 * 4. Elegir padre A (el que arranca el ciclo).
 * 5. Generar — borra registros generados previamente con el mismo origen y crea los nuevos.
 *
 * No toca registros manuales (origenSchedule = "").
 */
@Composable
fun PantallaCustodyScheduler(
    padres: List<Padre>,
    hijos: List<Hijo>,
    onAtras: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var schedules by remember { mutableStateOf(listOf<CustodySchedule>()) }
    var patron by remember { mutableStateOf(CustodyScheduleGenerator.Patrones.P_223) }
    var fechaInicio by remember { mutableStateOf(sdf.format(Date())) }
    var meses by remember { mutableStateOf("6") }
    var idPadreA by remember { mutableStateOf(padres.firstOrNull()?.id ?: "") }
    var idPadreB by remember { mutableStateOf(padres.getOrNull(1)?.id ?: "") }
    var generando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var confirmarBorrar by remember { mutableStateOf<CustodySchedule?>(null) }

    // Patrón custom: secuencia editable de A/B. Default: 7 días alternos.
    var modoCustom by remember { mutableStateOf(false) }
    var customSeq by remember { mutableStateOf(charArrayOf('A','A','A','B','B','B','B').toList()) }
    var customLargoTexto by remember { mutableStateOf(customSeq.size.toString()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            schedules = db.custodyScheduleDao().obtenerTodos()
        }
    }

    Box(Modifier.fillMaxSize().background(BgGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Plan de custodia", color = Neutral10) },
                    navigationIcon = {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (padres.size < 2) {
                    Text(
                        "Necesitás cargar al menos dos adultos en la familia para generar un plan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    return@Column
                }
                if (hijos.isEmpty()) {
                    Text(
                        "Necesitás cargar al menos un hijo para generar un plan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    return@Column
                }

                Text(
                    "Elegí un patrón y la app crea los registros de día por día. Después podés editar cualquier día puntual a mano.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant30
                )

                // ── Patrón ─────────────────────────────────────────────────────
                Text("Patrón",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                CustodyScheduleGenerator.Patrones.listaTodos().forEach { p ->
                    val sel = !modoCustom && patron == p
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) GlassWhite else Color.Transparent)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            RadioButton(selected = sel, onClick = {
                                patron = p
                                modoCustom = false
                            })
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(
                                    CustodyScheduleGenerator.Patrones.nombreLargo(p),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Neutral10
                                )
                                Text(
                                    CustodyScheduleGenerator.Patrones.descripcionCorta(p),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeutralVariant30
                                )
                            }
                        }
                    }
                }
                // ── Opción Personalizado ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (modoCustom) GlassWhite else Color.Transparent)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            RadioButton(selected = modoCustom, onClick = {
                                modoCustom = true
                                patron = CustodyScheduleGenerator.Patrones.construirCustom(customSeq.joinToString(""))
                            })
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text("Personalizado",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Neutral10
                                )
                                Text(
                                    "Diseñás vos el ciclo: elegí cuántos días dura y tocá cada día para que sea A o B.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeutralVariant30
                                )
                            }
                        }
                        if (modoCustom) {
                            Spacer(Modifier.height(8.dp))
                            // Largo del ciclo
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Días del ciclo:", color = Neutral10, modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = customLargoTexto,
                                    onValueChange = { txt ->
                                        customLargoTexto = txt.filter { it.isDigit() }.take(2)
                                        val n = customLargoTexto.toIntOrNull()?.coerceIn(1, 28) ?: customSeq.size
                                        customSeq = if (n > customSeq.size) {
                                            customSeq + List(n - customSeq.size) { 'A' }
                                        } else customSeq.take(n)
                                        patron = CustodyScheduleGenerator.Patrones.construirCustom(customSeq.joinToString(""))
                                    },
                                    modifier = Modifier.width(72.dp),
                                    singleLine = true
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // Grid de toggles A/B (chunks de 7)
                            customSeq.chunked(7).forEachIndexed { rowIdx, fila ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    fila.forEachIndexed { colIdx, ch ->
                                        val absoluto = rowIdx * 7 + colIdx
                                        val esA = ch == 'A'
                                        val color = if (esA) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error
                                        OutlinedButton(
                                            onClick = {
                                                customSeq = customSeq.toMutableList().also {
                                                    it[absoluto] = if (esA) 'B' else 'A'
                                                }
                                                patron = CustodyScheduleGenerator.Patrones.construirCustom(customSeq.joinToString(""))
                                            },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            border = androidx.compose.foundation.BorderStroke(2.dp, color)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("D${absoluto + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeutralVariant30
                                                )
                                                Text(ch.toString(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = color
                                                )
                                            }
                                        }
                                    }
                                    // Padding si la fila no llena 7 cols
                                    repeat(7 - fila.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }

                // ── Padre A (arranca ciclo) ─────────────────────────────────────
                Text("Quién arranca el ciclo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Padre A: el adulto que tiene los chicos en el primer día del patrón. Padre B: el otro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant30
                )
                padres.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = idPadreA == p.id, onClick = {
                            idPadreA = p.id
                            idPadreB = padres.firstOrNull { it.id != p.id }?.id ?: ""
                        })
                        Text("${p.nombre} (A)", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Text(
                    "Padre B será: ${padres.firstOrNull { it.id == idPadreB }?.nombre ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant30
                )

                // ── Fecha de arranque ───────────────────────────────────────────
                Text("Fecha de arranque",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Si el patrón es semanal (todos lo son), se recomienda que sea un lunes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant30
                )
                CampoFecha(value = fechaInicio, label = "Fecha (YYYY-MM-DD)",
                    onValueChange = { fechaInicio = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Meses a generar ─────────────────────────────────────────────
                OutlinedTextField(
                    value = meses,
                    onValueChange = { meses = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Meses a generar") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "Si la fecha de arranque es futura, los registros aparecen desde ese día en adelante (no aparecen hoy).",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant30
                )
                Button(
                    enabled = !generando && idPadreA.isNotBlank() && idPadreB.isNotBlank()
                        && fechaInicio.length == 10,
                    onClick = {
                        scope.launch {
                            generando = true
                            mensaje = "Generando…"
                            val mesesInt = meses.toIntOrNull()?.coerceIn(1, 24) ?: 6
                            val schedule = CustodySchedule(
                                patron = patron,
                                fechaInicio = fechaInicio,
                                mesesGenerados = mesesInt,
                                idPadreA = idPadreA,
                                idPadreB = idPadreB
                            )
                            val padreA = padres.first { it.id == idPadreA }
                            val padreB = padres.first { it.id == idPadreB }
                            val regs = withContext(Dispatchers.IO) {
                                CustodyScheduleGenerator.generar(schedule, padreA, padreB, hijos)
                            }
                            withContext(Dispatchers.IO) {
                                // Reemplazar planes anteriores: 1 plan activo a la vez.
                                // Borra registros generados + entries de schedule. Manuales (origenSchedule="") se preservan.
                                val previos = db.custodyScheduleDao().obtenerTodos()
                                previos.forEach { p ->
                                    db.registroTiempoDao().eliminarPorOrigenSchedule(p.id)
                                    db.custodyScheduleDao().eliminarPorId(p.id)
                                }
                                db.custodyScheduleDao().insertar(schedule)
                                db.registroTiempoDao().insertarRegistros(regs)
                                schedules = db.custodyScheduleDao().obtenerTodos()

                                // Espejar al organizador semanal (SharedPrefs):
                                // 1 slot por día del ciclo con el padre asignado, día completo 00:00-23:59.
                                val largoCiclo = CustodyScheduleGenerator.Patrones.largoCiclo(schedule.patron)
                                val prefs = context.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE)
                                val edit = prefs.edit()
                                edit.putInt("dias_fijos_ciclo", largoCiclo)
                                for (i in 0 until largoCiclo) {
                                    val cuál = CustodyScheduleGenerator.Patrones.padreEnDia(schedule.patron, i)
                                    val padreId = if (cuál == CustodyScheduleGenerator.PadreEnCiclo.A) schedule.idPadreA else schedule.idPadreB
                                    edit.putString("dias_fijos_slots_${largoCiclo}_$i", "$padreId:00:00-23:59")
                                }
                                edit.apply()
                            }
                            mensaje = "Listo: ${regs.size} registros creados desde $fechaInicio"
                            generando = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (generando) "Generando…" else "Generar plan (reemplaza el anterior)")
                }

                mensaje?.let {
                    Text(it,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant30
                    )
                }

                // ── Planes generados ────────────────────────────────────────────
                if (schedules.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Planes activos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    schedules.forEach { s ->
                        val padreA = padres.firstOrNull { it.id == s.idPadreA }?.nombre ?: "—"
                        val padreB = padres.firstOrNull { it.id == s.idPadreB }?.nombre ?: "—"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlassWhite)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    CustodyScheduleGenerator.Patrones.nombreLargo(s.patron),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Neutral10
                                )
                                Text(
                                    "Desde ${s.fechaInicio} · ${s.mesesGenerados} meses · $padreA (A) · $padreB (B)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeutralVariant30
                                )
                                TextButton(onClick = { confirmarBorrar = s }) {
                                    Text("Borrar este plan", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmarBorrar != null) {
        val s = confirmarBorrar!!
        AlertDialog(
            onDismissRequest = { confirmarBorrar = null },
            title = { Text("¿Borrar plan?") },
            text = { Text("Se borran los registros generados por este plan. Los registros que editaste a mano no se tocan.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrar = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            db.registroTiempoDao().eliminarPorOrigenSchedule(s.id)
                            db.custodyScheduleDao().eliminarPorId(s.id)
                            schedules = db.custodyScheduleDao().obtenerTodos()
                        }
                        mensaje = "Plan borrado"
                    }
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrar = null }) { Text("Cancelar") }
            }
        )
    }
}

private fun proximoLunes(sdf: SimpleDateFormat): String {
    val cal = java.util.Calendar.getInstance()
    while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.MONDAY) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    return sdf.format(cal.time)
}
