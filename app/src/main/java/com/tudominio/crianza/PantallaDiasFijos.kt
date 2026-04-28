@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tudominio.crianza.ui.theme.*
import java.util.UUID

data class DaySlot(val padreId: String, val inicio: String, val fin: String)

data class ActividadFija(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val diasSemana: Set<Int>,
    val inicio: String,
    val fin: String,
    val idHijos: Set<String> = emptySet(),
    val conResponsable: Boolean = false
)

private fun parseHM(s: String): Pair<Int, Int>? {
    val p = s.trim().split(":")
    if (p.size != 2) return null
    val h = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59) Pair(h, m) else null
}

private fun fmt(h: Int, m: Int) = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"

private fun slotMins(inicio: String, fin: String): Int {
    val (ih, im) = parseHM(inicio) ?: return 24 * 60
    val (fh, fm) = parseHM(fin) ?: return 24 * 60
    val iniMins = ih * 60 + im
    val finMins = if (fh == 0 && fm == 0 && iniMins > 0) 24 * 60 else fh * 60 + fm
    return when {
        iniMins == 0 && finMins == 0 -> 24 * 60  // 00:00–00:00 = día completo
        finMins > iniMins -> finMins - iniMins
        else -> 0
    }
}

private fun slotMinutos(slot: DaySlot) = slotMins(slot.inicio, slot.fin)

private fun formatMins(mins: Int): String {
    val h = mins / 60; val m = mins % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

private fun parseSlots(raw: String): List<DaySlot> {
    if (raw.isBlank()) return emptyList()
    return raw.split("|").mapNotNull { s ->
        val ci = s.indexOf(':'); if (ci < 0) null else {
            val range = s.substring(ci + 1).split("-")
            DaySlot(s.substring(0, ci), range.getOrElse(0) { "" }, range.getOrElse(1) { "" })
        }
    }
}

private fun encodeSlots(slots: List<DaySlot>) =
    slots.joinToString("|") { "${it.padreId}:${it.inicio}-${it.fin}" }

private fun encodeActividades(list: List<ActividadFija>) =
    list.joinToString("§") { "${it.id}~${it.nombre}~${it.diasSemana.joinToString(",")}~${it.inicio}~${it.fin}~${it.idHijos.joinToString(",")}~${it.conResponsable}" }

private fun decodeActividades(raw: String): List<ActividadFija> {
    if (raw.isBlank()) return emptyList()
    return raw.split("§").mapNotNull { s ->
        val p = s.split("~")
        if (p.size < 5) null else ActividadFija(
            id = p[0], nombre = p[1],
            diasSemana = p[2].split(",").mapNotNull { it.toIntOrNull() }.toSet(),
            inicio = p[3], fin = p[4],
            idHijos = if (p.size > 5) p[5].split(",").filter { it.isNotBlank() }.toSet() else emptySet(),
            conResponsable = p.getOrNull(6)?.toBooleanStrictOrNull() ?: false
        )
    }
}

private fun actMinsEnDia(dow: Int, actividades: List<ActividadFija>): Int =
    actividades.filter { dow in it.diasSemana }.sumOf { slotMins(it.inicio, it.fin) }

private fun toMinsRange(inicio: String, fin: String): Pair<Int, Int> {
    val (ih, im) = parseHM(inicio) ?: return Pair(0, 1440)
    val (fh, fm) = parseHM(fin) ?: return Pair(0, 1440)
    val iniM = ih * 60 + im
    val finM = if (fh == 0 && fm == 0 && iniM > 0) 1440 else fh * 60 + fm
    return if (iniM == 0 && finM == 0) Pair(0, 1440) else Pair(iniM, finM)
}

private fun overlapMins(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Int =
    maxOf(0, minOf(aEnd, bEnd) - maxOf(aStart, bStart))

@Composable
fun PantallaDiasFijos(
    padres: List<Padre>,
    hijos: List<Hijo> = emptyList(),
    onAgregarEventos: (List<Evento>, List<RegistroTiempo>) -> Unit = { _, _ -> },
    onEliminarEventosActividad: (List<String>) -> Unit = {},
    onAtras: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("crianza_prefs", android.content.Context.MODE_PRIVATE) }

    var cicloDias by remember { mutableIntStateOf(prefs.getInt("dias_fijos_ciclo", 7)) }
    var aplicarSiempre by remember { mutableStateOf(prefs.getBoolean("dias_fijos_repetir", true)) }

    val actividadesFijas = remember {
        decodeActividades(prefs.getString("actividades_fijas", "") ?: "").toMutableStateList()
    }
    fun saveActividades() { prefs.edit().putString("actividades_fijas", encodeActividades(actividadesFijas)).apply() }

    val slotsPerDay = remember(cicloDias) {
        (0 until cicloDias).map { idx ->
            val raw = prefs.getString("dias_fijos_slots_${cicloDias}_${idx}", null)
            if (raw != null) parseSlots(raw).toMutableStateList()
            else {
                val oldId = (prefs.getString("dias_fijos_schedule_$cicloDias", "") ?: "").split("|").getOrElse(idx) { "" }
                val oldHora = (prefs.getString("dias_fijos_horas_$cicloDias", "") ?: "").split("|").getOrElse(idx) { "" }
                if (oldId.isNotBlank()) {
                    val rng = oldHora.split("-")
                    mutableStateListOf(DaySlot(oldId, rng.getOrElse(0) { "" }, rng.getOrElse(1) { "" }))
                } else mutableStateListOf()
            }
        }
    }
    fun saveDay(idx: Int) { prefs.edit().putString("dias_fijos_slots_${cicloDias}_${idx}", encodeSlots(slotsPerDay[idx])).apply() }

    val nombresDias7 = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val nombresDiasCorto = listOf("L", "M", "X", "J", "V", "S", "D")
    fun nombreDia(idx: Int) = when (cicloDias) {
        7 -> nombresDias7[idx]; 14 -> "Sem ${idx / 7 + 1} · ${nombresDias7[idx % 7]}"; else -> "Día ${idx + 1}"
    }

    val colorP1 = Teal40; val colorP2 = Color(0xFF8B4A20)

    var timePicker by remember { mutableStateOf<Triple<Int, Int, Boolean>?>(null) }
    var dialogActividad by remember { mutableStateOf<ActividadFija?>(null) }
    var dialogActIdx by remember { mutableIntStateOf(-1) }
    var timePickerActividad by remember { mutableStateOf<Boolean?>(null) }

    // Análisis — derivedStateOf para reaccionar a cambios dentro de las SnapshotStateLists
    val analisis by remember(cicloDias) { derivedStateOf {
        val porPadre = mutableMapOf<String, Int>()
        val porActividad = mutableMapOf<String, Int>()
        var libre = 0
        val libreDetalle = mutableListOf<Pair<String, Int>>()
        val nombresDias7Local = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        for (idx in 0 until cicloDias) {
            val dow = idx % 7
            val slots = slotsPerDay[idx]
            val actDiaList = actividadesFijas.filter { dow in it.diasSemana }
            // conResponsable=true → tiempo del padre (no descuenta), conResponsable=false → actividad autónoma
            val actAutonomas = actDiaList.filter { !it.conResponsable }
            val actMins = actAutonomas.sumOf { slotMins(it.inicio, it.fin) }
            val anyTimed = slots.any { parseHM(it.inicio) != null && parseHM(it.fin) != null }
            var effectivePadresMins = 0
            for (slot in slots) {
                if (slot.padreId.isBlank()) continue
                val (sStart, sEnd) = if (!anyTimed) Pair(0, 1440) else toMinsRange(slot.inicio, slot.fin)
                val rawMins = sEnd - sStart
                val actOverlap = actAutonomas.sumOf { act ->
                    val (aStart, aEnd) = toMinsRange(act.inicio, act.fin)
                    overlapMins(sStart, sEnd, aStart, aEnd)
                }
                val effectiveMins = maxOf(0, rawMins - actOverlap)
                porPadre[slot.padreId] = (porPadre[slot.padreId] ?: 0) + effectiveMins
                effectivePadresMins += effectiveMins
            }
            for (act in actAutonomas) {
                val mins = slotMins(act.inicio, act.fin)
                if (mins > 0) porActividad[act.id] = (porActividad[act.id] ?: 0) + mins
            }
            val dayLibre = maxOf(0, 24 * 60 - actMins - effectivePadresMins)
            libre += dayLibre
            if (dayLibre > 0) {
                val label = when (cicloDias) {
                    7 -> nombresDias7Local[idx]
                    14 -> "Sem ${idx / 7 + 1} · ${nombresDias7Local[idx % 7]}"
                    else -> "Día ${idx + 1}"
                }
                libreDetalle.add(label to dayLibre)
            }
        }
        Pair(Triple(porPadre, porActividad, libre), libreDetalle)
    } }
    val totalMinsPorPadre = analisis.first.first
    val totalMinsPorActividad = analisis.first.second
    val totalMinsLibre = analisis.first.third
    val libreDetalle = analisis.second

    Box(Modifier.fillMaxSize().background(BgGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Planificación semanal", color = Neutral10) },
                    navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.Default.ArrowBack, null, tint = NeutralVariant30) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { pv ->
            Column(
                modifier = Modifier.fillMaxSize().padding(pv).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Grilla de días ────────────────────────────────────────────
                Text("Asignar días", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(cicloDias) { dayIdx ->
                        val slots = slotsPerDay[dayIdx]
                        val dow = dayIdx % 7
                        val actDia = actividadesFijas.filter { dow in it.diasSemana }
                        val minsP0 = slots.filter { it.padreId == padres.getOrNull(0)?.id }.sumOf { slotMinutos(it) }
                        val minsP1 = slots.filter { it.padreId == padres.getOrNull(1)?.id }.sumOf { slotMinutos(it) }
                        val totalAssigned = minsP0 + minsP1
                        val cardBrush = when {
                            totalAssigned == 0 -> Brush.horizontalGradient(listOf(NeutralVariant80.copy(0.08f), NeutralVariant80.copy(0.08f)))
                            minsP0 == 0 -> Brush.horizontalGradient(listOf(colorP2.copy(0.12f), colorP2.copy(0.12f)))
                            minsP1 == 0 -> Brush.horizontalGradient(listOf(colorP1.copy(0.12f), colorP1.copy(0.12f)))
                            else -> { val f = minsP0.toFloat() / totalAssigned
                                Brush.horizontalGradient(0f to colorP1.copy(0.18f), f to colorP1.copy(0.18f), f to colorP2.copy(0.18f), 1f to colorP2.copy(0.18f)) }
                        }

                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBrush)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(nombreDia(dayIdx), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(if (cicloDias == 7) 44.dp else 96.dp))
                                    if (actDia.isNotEmpty()) {
                                        Text(actDia.joinToString(" · ") { it.nombre },
                                            style = MaterialTheme.typography.labelSmall, color = NeutralVariant50, modifier = Modifier.weight(1f))
                                    } else Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { slotsPerDay[dayIdx].add(DaySlot("", "", "")) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Tramo", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                if (slots.isEmpty()) Text("Sin asignar", style = MaterialTheme.typography.bodySmall, color = NeutralVariant50, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))

                                slots.forEachIndexed { slotIdx, slot ->
                                    var ddExpanded by remember { mutableStateOf(false) }
                                    val padreSlot = padres.find { it.id == slot.padreId }
                                    val colorSlot = when (slot.padreId) { padres.getOrNull(0)?.id -> colorP1; padres.getOrNull(1)?.id -> colorP2; else -> NeutralVariant80 }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .background(colorSlot.copy(if (padreSlot != null) 0.10f else 0.04f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (padreSlot != null) colorSlot else NeutralVariant50))
                                        Box(Modifier.weight(1f)) {
                                            OutlinedButton(onClick = { ddExpanded = true }, modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                                Text(padreSlot?.nombre ?: "Elegir padre", style = MaterialTheme.typography.bodySmall)
                                            }
                                            DropdownMenu(expanded = ddExpanded, onDismissRequest = { ddExpanded = false }) {
                                                padres.forEach { padre ->
                                                    DropdownMenuItem(text = { Text(padre.nombre) }, onClick = {
                                                        slotsPerDay[dayIdx][slotIdx] = slot.copy(padreId = padre.id); saveDay(dayIdx); ddExpanded = false })
                                                }
                                            }
                                        }
                                        OutlinedButton(onClick = { timePicker = Triple(dayIdx, slotIdx, true) }, shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp), modifier = Modifier.width(66.dp)) {
                                            Text(slot.inicio.ifBlank { "--:--" }, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("–", color = NeutralVariant50, style = MaterialTheme.typography.bodySmall)
                                        OutlinedButton(onClick = { timePicker = Triple(dayIdx, slotIdx, false) }, shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp), modifier = Modifier.width(66.dp)) {
                                            Text(slot.fin.ifBlank { "--:--" }, style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(onClick = { slotsPerDay[dayIdx].removeAt(slotIdx); saveDay(dayIdx) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp), tint = NeutralVariant50)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Análisis del ciclo ────────────────────────────────────────
                HorizontalDivider(color = NeutralVariant80.copy(0.3f))
                Text("Resumen del ciclo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Alerta si hay momentos sin asignar
                if (totalMinsLibre > 0) {
                    var detalleExpandido by remember { mutableStateOf(false) }
                    val warningColor = Color(0xFF7A5C00)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = warningColor.copy(0.10f),
                        modifier = Modifier.fillMaxWidth().clickable { detalleExpandido = !detalleExpandido }
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Warning, null, modifier = Modifier.size(16.dp), tint = warningColor)
                                Text("${formatMins(totalMinsLibre)} sin nadie asignado en este ciclo",
                                    style = MaterialTheme.typography.bodySmall, color = warningColor, modifier = Modifier.weight(1f))
                                Text(if (detalleExpandido) "▲" else "▼", style = MaterialTheme.typography.labelSmall, color = warningColor)
                            }
                            if (detalleExpandido) {
                                HorizontalDivider(color = warningColor.copy(0.2f))
                                libreDetalle.forEach { (label, mins) ->
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = warningColor.copy(0.8f))
                                        Text(formatMins(mins), style = MaterialTheme.typography.labelSmall, color = warningColor.copy(0.8f))
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(shape = RoundedCornerShape(12.dp), color = NeutralVariant80.copy(0.15f), modifier = Modifier.fillMaxWidth()) {
                    val totalCicloMins = cicloDias * 24 * 60
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        padres.forEach { padre ->
                            val mins = totalMinsPorPadre[padre.id] ?: 0
                            val color = if (padre.id == padres.getOrNull(0)?.id) colorP1 else colorP2
                            val frac = mins.toFloat() / totalCicloMins
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                                Text(padre.nombre, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp), fontWeight = FontWeight.SemiBold)
                                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(NeutralVariant80.copy(0.3f))) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f)).background(color.copy(0.7f)))
                                }
                                Text(if (mins == 0) "–" else formatMins(mins), style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(52.dp))
                            }
                        }
                        actividadesFijas.forEach { act ->
                            val mins = totalMinsPorActividad[act.id] ?: 0
                            if (mins > 0) {
                                val frac = mins.toFloat() / totalCicloMins
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(NeutralVariant50.copy(0.5f)))
                                    Text(act.nombre.ifBlank { "Actividad" }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp), color = NeutralVariant50)
                                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(NeutralVariant80.copy(0.3f))) {
                                        Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f)).background(NeutralVariant50.copy(0.35f)))
                                    }
                                    Text(formatMins(mins), style = MaterialTheme.typography.bodySmall, color = NeutralVariant50, modifier = Modifier.width(52.dp))
                                }
                            }
                        }
                    }
                }

                // ── Opciones ──────────────────────────────────────────────────
                HorizontalDivider(color = NeutralVariant80.copy(0.3f))
                Text("Opciones", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7 to "7 días", 14 to "14 días").forEach { (dias, label) ->
                        FilterChip(selected = cicloDias == dias,
                            onClick = {
                                if (dias != cicloDias) {
                                    val cicloAnterior = cicloDias
                                    // Copio slots del ciclo anterior al nuevo si los del destino estan vacios.
                                    // Asi cambiar 7->14 conserva los 7 dias y rellena los 7 nuevos con copia.
                                    // Cambiar 14->7 conserva los slots del ciclo 7 si fueron seteados antes (no machaca).
                                    for (idx in 0 until dias) {
                                        val keyDestino = "dias_fijos_slots_${dias}_${idx}"
                                        val existente = prefs.getString(keyDestino, null)
                                        if (existente == null) {
                                            // tomo del ciclo anterior, modulando por si crece (idx % anterior)
                                            val origen = prefs.getString("dias_fijos_slots_${cicloAnterior}_${idx % cicloAnterior}", null)
                                            if (origen != null) prefs.edit().putString(keyDestino, origen).apply()
                                        }
                                    }
                                }
                                cicloDias = dias
                                prefs.edit().putInt("dias_fijos_ciclo", dias).apply()
                            },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aplicar siempre", style = MaterialTheme.typography.bodyMedium)
                        Text("El ciclo se repite automáticamente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = aplicarSiempre, onCheckedChange = { aplicarSiempre = it; prefs.edit().putBoolean("dias_fijos_repetir", it).apply() })
                }
                if (cicloDias > 7) {
                    TextButton(onClick = {
                        for (i in 7 until cicloDias) { slotsPerDay[i].clear(); slotsPerDay[i].addAll(slotsPerDay[i % 7].map { it.copy() }); saveDay(i) }
                    }) { Text("Repetir semana 1 como base", style = MaterialTheme.typography.bodySmall) }
                }

                // Actividades fijas
                HorizontalDivider(color = NeutralVariant80.copy(0.2f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Actividades fijas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Horarios sin cuidado parental (escuela, deporte, etc.)", style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
                    }
                    IconButton(onClick = {
                        dialogActividad = ActividadFija(nombre = "", diasSemana = emptySet(), inicio = "", fin = "")
                        dialogActIdx = -1
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = NeutralVariant50)
                    }
                }

                if (actividadesFijas.isEmpty()) {
                    Text("Sin actividades configuradas", style = MaterialTheme.typography.bodySmall, color = NeutralVariant50, modifier = Modifier.padding(start = 4.dp))
                }
                actividadesFijas.forEachIndexed { aIdx, act ->
                    val actMinsTotal = (0 until cicloDias).sumOf { actMinsEnDia(it % 7, listOf(act)) }
                    Surface(shape = RoundedCornerShape(10.dp), color = NeutralVariant80.copy(0.15f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(act.nombre.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                val diasStr = act.diasSemana.sorted().joinToString(" ") { nombresDiasCorto.getOrElse(it) { "?" } }
                                val hijosStr = if (act.idHijos.isEmpty()) "Todos"
                                    else act.idHijos.mapNotNull { id -> hijos.find { it.id == id }?.nombre }.joinToString(", ").ifBlank { "Todos" }
                                Text("$diasStr  ·  ${act.inicio.ifBlank{"--:--"}} – ${act.fin.ifBlank{"--:--"}}  ·  $hijosStr  ·  ${if (actMinsTotal > 0) formatMins(actMinsTotal) else "–"}/ciclo",
                                    style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
                            }
                            IconButton(onClick = { dialogActividad = act; dialogActIdx = aIdx }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(14.dp), tint = NeutralVariant50)
                            }
                            IconButton(onClick = {
                                val eventIds = (prefs.getString("actividad_eventos_${act.id}", null) ?: "")
                                    .split(",").filter { it.isNotBlank() }
                                if (eventIds.isNotEmpty()) {
                                    prefs.edit().remove("actividad_eventos_${act.id}").apply()
                                    onEliminarEventosActividad(eventIds)
                                }
                                actividadesFijas.removeAt(aIdx); saveActividades()
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp), tint = NeutralVariant50)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ── Time picker (slots) ───────────────────────────────────────────────────
    val tp = timePicker
    if (tp != null) {
        val (dayIdx, slotIdx, isInicio) = tp
        val slot = slotsPerDay.getOrNull(dayIdx)?.getOrNull(slotIdx)
        if (slot != null) {
            val (initH, initM) = parseHM(if (isInicio) slot.inicio else slot.fin) ?: Pair(if (isInicio) 8 else 20, 0)
            val tpState = rememberTimePickerState(initialHour = initH, initialMinute = initM, is24Hour = true)
            AlertDialog(onDismissRequest = { timePicker = null },
                confirmButton = { TextButton(onClick = {
                    val hs = fmt(tpState.hour, tpState.minute)
                    slotsPerDay[dayIdx][slotIdx] = if (isInicio) slot.copy(inicio = hs) else slot.copy(fin = hs)
                    saveDay(dayIdx); timePicker = null
                }) { Text("OK") } },
                dismissButton = { TextButton(onClick = { timePicker = null }) { Text("Cancelar") } },
                title = { Text(if (isInicio) "Hora de inicio" else "Hora de fin") },
                text = { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) { TimePicker(state = tpState) } }
            )
        }
    }

    // ── Diálogo actividad fija ────────────────────────────────────────────────
    val da = dialogActividad
    if (da != null) {
        var nombre by remember(da.id) { mutableStateOf(da.nombre) }
        var diasSel by remember(da.id) { mutableStateOf(da.diasSemana) }
        var inicio by remember(da.id) { mutableStateOf(da.inicio) }
        var fin by remember(da.id) { mutableStateOf(da.fin) }
        var hijosSel by remember(da.id) { mutableStateOf(da.idHijos) }
        var conResponsable by remember(da.id) { mutableStateOf(da.conResponsable) }
        val eventoIdsGuardados = remember(da.id) { prefs.getString("actividad_eventos_${da.id}", null) }
        val yaEnCalendario = eventoIdsGuardados != null
        var agregarCalendario by remember(da.id) { mutableStateOf(yaEnCalendario) }

        val tpa = timePickerActividad
        if (tpa != null) {
            val (initH, initM) = parseHM(if (tpa) inicio else fin) ?: Pair(if (tpa) 8 else 16, 0)
            val tpState = rememberTimePickerState(initialHour = initH, initialMinute = initM, is24Hour = true)
            AlertDialog(onDismissRequest = { timePickerActividad = null },
                confirmButton = { TextButton(onClick = { val hs = fmt(tpState.hour, tpState.minute); if (tpa) inicio = hs else fin = hs; timePickerActividad = null }) { Text("OK") } },
                dismissButton = { TextButton(onClick = { timePickerActividad = null }) { Text("Cancelar") } },
                title = { Text(if (tpa) "Hora de inicio" else "Hora de fin") },
                text = { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) { TimePicker(state = tpState) } }
            )
        } else {
            AlertDialog(
                onDismissRequest = { dialogActividad = null },
                title = { Text(if (dialogActIdx < 0) "Nueva actividad" else "Editar actividad") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Días", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { diasSel = setOf(0,1,2,3,4) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                Text("L–V", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("L","M","X","J","V").forEachIndexed { i, d ->
                                val sel = i in diasSel
                                FilterChip(selected = sel, onClick = { diasSel = if (sel) diasSel - i else diasSel + i },
                                    label = { Text(d, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(5 to "S", 6 to "D").forEach { (i, d) ->
                                val sel = i in diasSel
                                FilterChip(selected = sel, onClick = { diasSel = if (sel) diasSel - i else diasSel + i },
                                    label = { Text(d, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { timePickerActividad = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                Text("Desde: ${inicio.ifBlank { "--:--" }}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("–", color = NeutralVariant50)
                            OutlinedButton(onClick = { timePickerActividad = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                Text("Hasta: ${fin.ifBlank { "--:--" }}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (hijos.isNotEmpty()) {
                            Text("Para quién", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = hijosSel.isEmpty(), onClick = { hijosSel = emptySet() },
                                    label = { Text("Todos", style = MaterialTheme.typography.labelSmall) })
                                hijos.forEach { hijo ->
                                    val sel = hijo.id in hijosSel
                                    FilterChip(selected = sel, onClick = { hijosSel = if (sel) hijosSel - hijo.id else hijosSel + hijo.id },
                                        label = { Text(hijo.nombre, style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = conResponsable, onCheckedChange = { conResponsable = it })
                            Column {
                                Text("Con el responsable", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("El tiempo cuenta como tiempo del padre", style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = agregarCalendario, onCheckedChange = { agregarCalendario = it })
                            Text("Agregar al calendario de la app", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val nueva = ActividadFija(id = da.id, nombre = nombre.trim(), diasSemana = diasSel, inicio = inicio, fin = fin, idHijos = hijosSel, conResponsable = conResponsable)
                        if (dialogActIdx < 0) actividadesFijas.add(nueva) else actividadesFijas[dialogActIdx] = nueva
                        saveActividades()
                        when {
                            agregarCalendario && !yaEnCalendario -> {
                                // Agregar al calendario por primera vez
                                val (evts, regs) = generarEventosActividad(nueva, slotsPerDay, cicloDias, padres, hijos)
                                prefs.edit().putString("actividad_eventos_${nueva.id}", evts.joinToString(",") { it.id }).apply()
                                onAgregarEventos(evts, regs)
                            }
                            !agregarCalendario && yaEnCalendario -> {
                                // Quitar del calendario
                                val ids = eventoIdsGuardados!!.split(",").filter { it.isNotBlank() }
                                prefs.edit().remove("actividad_eventos_${nueva.id}").apply()
                                onEliminarEventosActividad(ids)
                            }
                        }
                        dialogActividad = null
                    }, enabled = nombre.isNotBlank() && diasSel.isNotEmpty()) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { dialogActividad = null }) { Text("Cancelar") } }
            )
        }
    }
}

private val calDowMap = mapOf(0 to java.util.Calendar.MONDAY, 1 to java.util.Calendar.TUESDAY,
    2 to java.util.Calendar.WEDNESDAY, 3 to java.util.Calendar.THURSDAY, 4 to java.util.Calendar.FRIDAY,
    5 to java.util.Calendar.SATURDAY, 6 to java.util.Calendar.SUNDAY)

private fun padreEnFecha(
    cal: java.util.Calendar,
    actInicio: String, actFin: String,
    slotsPerDay: List<List<DaySlot>>, cicloDias: Int
): DaySlot? {
    val calDow = cal.get(java.util.Calendar.DAY_OF_WEEK)
    val appDow = (calDow - java.util.Calendar.MONDAY + 7) % 7
    val idx = appDow % cicloDias
    val slots = slotsPerDay.getOrNull(idx) ?: return null
    val (aStart, aEnd) = toMinsRange(actInicio, actFin)
    return slots.filter { it.padreId.isNotBlank() }.maxByOrNull { s ->
        val (sStart, sEnd) = toMinsRange(s.inicio, s.fin)
        overlapMins(sStart, sEnd, aStart, aEnd)
    }?.takeIf { s ->
        val (sStart, sEnd) = toMinsRange(s.inicio, s.fin)
        overlapMins(sStart, sEnd, aStart, aEnd) > 0
    }
}

private fun generarEventosActividad(
    act: ActividadFija,
    slotsPerDay: List<List<DaySlot>>,
    cicloDias: Int,
    padres: List<Padre>,
    hijos: List<Hijo>,
    semanas: Int = 8
): Pair<List<Evento>, List<RegistroTiempo>> {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val eventos = mutableListOf<Evento>()
    val registros = mutableListOf<RegistroTiempo>()
    val hijosActivos = if (act.idHijos.isEmpty()) hijos else hijos.filter { it.id in act.idHijos }
    repeat(semanas * 7) { offsetDias ->
        val cal = java.util.Calendar.getInstance().also { it.add(java.util.Calendar.DAY_OF_YEAR, offsetDias) }
        val calDow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        if (act.diasSemana.none { calDowMap[it] == calDow }) return@repeat
        val fecha = sdf.format(cal.time)
        eventos.add(Evento(titulo = act.nombre, fecha = fecha,
            horaInicio = act.inicio.ifBlank { null }, horaFin = act.fin.ifBlank { null }))
        if (act.conResponsable) {
            val slot = padreEnFecha(cal, act.inicio, act.fin, slotsPerDay, cicloDias) ?: return@repeat
            val padre = padres.find { it.id == slot.padreId } ?: return@repeat
            hijosActivos.forEach { hijo ->
                registros.add(RegistroTiempo(
                    idHijo = hijo.id, nombreHijo = hijo.nombre,
                    idPadre = padre.id, nombrePadre = padre.nombre,
                    fecha = fecha, horaInicio = act.inicio, horaFin = act.fin
                ))
            }
        }
    }
    return Pair(eventos, registros)
}
