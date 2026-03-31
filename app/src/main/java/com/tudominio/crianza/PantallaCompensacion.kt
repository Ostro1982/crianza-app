@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tudominio.crianza.ui.theme.*
import java.util.*
import kotlin.math.abs

// ── Paleta interna ────────────────────────────────────────────────────────────
private val Glass10      = Color(0x1AFFFFFF)   // 10 % blanco
private val Glass20      = Color(0x33FFFFFF)   // 20 %
private val Glass35      = Color(0x59FFFFFF)   // 35 %
private val Amber        = Color(0xFFFBBF24)
private val Mint         = Color(0xFF34D399)
private val Coral        = Color(0xFFF87171)
private val Lavender     = Color(0xFFA78BFA)
private val Pink         = Color(0xFFF472B6)

// Gradiente de fondo compartido
private val BgGradient   = Brush.verticalGradient(
    listOf(Color(0xFF064E3B), Color(0xFF10B981))
)
// Gradiente oscuro para diálogos (más profundo)
private val DialogBg     = Brush.verticalGradient(
    listOf(Color(0xFF0F0C29), Color(0xFF1E1A4E), Color(0xFF302B63))
)

// =============================================================================
@Composable
fun PantallaCompensacion(
    padres: List<Padre>,
    registros: List<RegistroTiempo>,
    configuracionTiempo: ConfiguracionTiempo,
    compensaciones: List<Compensacion>,
    gastos: List<Gasto> = emptyList(),
    itemsCompra: List<ItemCompra> = emptyList(),
    onRegistrarCompensacion: (Compensacion) -> Unit,
    onEliminarCompensacion: (String) -> Unit,
    onEditarCompensacion: (Compensacion) -> Unit,
    onGuardarConfiguracion: (ConfiguracionTiempo) -> Unit,
    onAtras: () -> Unit
) {
    var mostrarDialogoValorHora  by remember { mutableStateOf(false) }
    var mostrarDialogoTipoValor  by remember { mutableStateOf(false) }
    var valorHora                by remember { mutableStateOf(configuracionTiempo.valorHora) }
    var periodo                  by remember { mutableStateOf(Periodo.TODO) }
    var compAEditar              by remember { mutableStateOf<Compensacion?>(null) }

    val valorEfectivo = when (configuracionTiempo.tipoValor) {
        "hora"   -> valorHora
        "semana" -> valorHora / (7 * 24)
        else     -> valorHora / 24
    }

    // ── Registros filtrados ───────────────────────────────────────────────────
    val registrosFiltrados = remember(registros, periodo) {
        registros.filter { r ->
            val f = parseFecha(r.fecha)
            when (periodo) {
                Periodo.SEMANA -> esMismaSemana(f, Date())
                Periodo.MES    -> esMismoMes(f, Date())
                Periodo.ANIO   -> esMismoAnio(f, Date())
                Periodo.TODO   -> true
            }
        }
    }

    // ── Cálculos ──────────────────────────────────────────────────────────────
    val p1 = padres.getOrNull(0)
    val p2 = padres.getOrNull(1)
    val hasDos = p1 != null && p2 != null

    val horasPorPadre = if (hasDos) calcularHorasPorPadre(registrosFiltrados) else emptyMap()
    val totalHoras    = horasPorPadre.values.sum()
    val horasP1       = horasPorPadre[p1?.id] ?: 0.0
    val horasP2       = horasPorPadre[p2?.id] ?: 0.0
    val pctR1         = if (totalHoras > 0) (horasP1 / totalHoras * 100).toInt() else 0
    val pctR2         = if (totalHoras > 0) (horasP2 / totalHoras * 100).toInt() else 0
    val dif1          = pctR1 - configuracionTiempo.porcentajePadre1
    val horasDeuda    = if (totalHoras > 0) abs(dif1) * totalHoras / 100 else 0.0
    val montoTiempo   = horasDeuda * valorEfectivo
    val timeBal       = when { dif1 < 0 -> -montoTiempo; dif1 > 0 -> montoTiempo; else -> 0.0 }

    val gastoP1  = if (p1 != null) gastos.filter { it.idPagador == p1.id }.sumOf { it.monto } else 0.0
    val gastoP2  = if (p2 != null) gastos.filter { it.idPagador == p2.id }.sumOf { it.monto } else 0.0
    val balGasto = (gastoP1 - gastoP2) / 2

    val pagadas  = itemsCompra.filter { it.precio > 0 && it.comprado }
    val compP1   = if (p1 != null) pagadas.filter { it.idPagador == p1.id }.sumOf { it.precio } else 0.0
    val compP2   = if (p2 != null) pagadas.filter { it.idPagador == p2.id }.sumOf { it.precio } else 0.0
    val balComp  = (compP1 - compP2) / 2

    val balPend  = if (p1 != null && p2 != null) {
        compensaciones.filter { !it.confirmada }.sumOf { c ->
            when (c.idPagador) { p1.id -> c.montoTotal; p2.id -> -c.montoTotal; else -> 0.0 }
        }
    } else 0.0

    // positivo → p2 le debe a p1
    val balNeto = balGasto + balComp + timeBal - balPend

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(BgGradient)) {
        Column(Modifier.fillMaxSize()) {

            TopAppBar(
                title = {
                    Text(
                        "Compensación",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        letterSpacing = (-0.3).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    val tLabel = when (configuracionTiempo.tipoValor) {
                        "hora" -> "$/hora"; "semana" -> "$/sem"; else -> "$/día"
                    }
                    val ok = configuracionTiempo.aprobadoTipoValor1 && configuracionTiempo.aprobadoTipoValor2
                    TextButton(onClick = { mostrarDialogoTipoValor = true }) {
                        Text(
                            if (ok) "✓ $tLabel" else "⏳ $tLabel",
                            color = Color.White.copy(.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = { mostrarDialogoValorHora = true }) {
                        Icon(Icons.Default.Edit, null, tint = Color.White.copy(.85f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 36.dp)
            ) {

                item { PillSelector(periodo) { periodo = it } }

                if (!hasDos) {
                    item {
                        DarkCard {
                            Box(Modifier.fillMaxWidth().padding(36.dp), Alignment.Center) {
                                Text(
                                    "Necesitás registrar al menos dos integrantes para calcular la compensación.",
                                    color = Color.White.copy(.75f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {

                    item { HeroCard(p1!!, p2!!, balNeto) }

                    if (totalHoras > 0) {
                        item {
                            TiempoCard(
                                p1 = p1!!, p2 = p2!!,
                                horasP1 = horasP1, horasP2 = horasP2,
                                totalHoras = totalHoras,
                                pctR1 = pctR1, pctR2 = pctR2,
                                obj1 = configuracionTiempo.porcentajePadre1,
                                obj2 = configuracionTiempo.porcentajePadre2,
                                dif1 = dif1,
                                horasDeuda = horasDeuda,
                                montoTiempo = montoTiempo,
                                tipoValor = configuracionTiempo.tipoValor,
                                valorHora = valorHora
                            )
                        }
                    }

                    if (gastoP1 + gastoP2 + compP1 + compP2 > 0) {
                        item {
                            GastosCard(p1!!, p2!!, gastoP1, gastoP2, compP1, compP2)
                        }
                    }

                    if (abs(balNeto) > 0.01) {
                        val (pag, rec) = if (balNeto < 0) p1!! to p2!! else p2!! to p1!!
                        item {
                            PagoCard(pag, rec, abs(balNeto), horasDeuda, valorHora, onRegistrarCompensacion)
                        }
                    }

                    item {
                        Text(
                            "Historial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }

                    if (compensaciones.isEmpty()) {
                        item {
                            DarkCard {
                                Box(Modifier.fillMaxWidth().padding(28.dp), Alignment.Center) {
                                    Text(
                                        "Sin compensaciones registradas",
                                        color = Color.White.copy(.45f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(compensaciones.sortedByDescending { it.fechaCompleta }) { comp ->
                            HistorialCard(comp, padres, { onEliminarCompensacion(comp.id) }, onEditarCompensacion)
                        }
                    }
                }
            }
        }
    }

    // ── Diálogos oscuros ──────────────────────────────────────────────────────
    if (mostrarDialogoValorHora) {
        DialogoValorHora(
            valorActual = valorHora,
            tipoValor   = configuracionTiempo.tipoValor,
            onDismiss   = { mostrarDialogoValorHora = false },
            onGuardar   = { v ->
                valorHora = v
                onGuardarConfiguracion(configuracionTiempo.copy(valorHora = v))
                mostrarDialogoValorHora = false
            }
        )
    }

    if (mostrarDialogoTipoValor) {
        DialogoTipoValor(
            configuracion = configuracionTiempo,
            padres        = padres,
            onDismiss     = { mostrarDialogoTipoValor = false },
            onAprobar     = { idx ->
                onGuardarConfiguracion(
                    if (idx == 0) configuracionTiempo.copy(aprobadoTipoValor1 = true)
                    else          configuracionTiempo.copy(aprobadoTipoValor2 = true)
                )
            },
            onCambiarTipo = { tipo ->
                onGuardarConfiguracion(
                    configuracionTiempo.copy(tipoValor = tipo, aprobadoTipoValor1 = false, aprobadoTipoValor2 = false)
                )
            }
        )
    }

    compAEditar?.let { comp ->
        DialogoEditarCompensacion(
            compensacion = comp,
            onDismiss    = { compAEditar = null },
            onGuardar    = { onEditarCompensacion(it); compAEditar = null }
        )
    }
}

// =============================================================================
// Componentes internos
// =============================================================================

// ── Base card oscura con borde sutil ─────────────────────────────────────────
@Composable
private fun DarkCard(
    modifier: Modifier = Modifier,
    brushBg: Brush = Brush.linearGradient(listOf(Color(0x40312E81), Color(0x403730A3))),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 0.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(brushBg)
            // borde sutil
            .then(
                Modifier.padding(1.dp)
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(23.dp))
                .background(Glass10)
                .padding(20.dp)
        ) {
            Column(content = content)
        }
    }
}

// ── Selector de período ───────────────────────────────────────────────────────
@Composable
private fun PillSelector(seleccionado: Periodo, onSelect: (Periodo) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(Glass10)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(Periodo.SEMANA to "Semana", Periodo.MES to "Mes", Periodo.ANIO to "Año", Periodo.TODO to "Todo")
            .forEach { (p, label) ->
                val active = seleccionado == p
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (active)
                                Brush.linearGradient(listOf(Lavender.copy(.7f), Pink.copy(.7f)))
                            else
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { onSelect(p) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (active) Color.White else Color.White.copy(.5f),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────
@Composable
private fun HeroCard(p1: Padre, p2: Padre, balNeto: Double) {
    val equilibrado = abs(balNeto) < 0.01
    val deudor      = if (balNeto < 0) p1 else p2
    val acreedor    = if (balNeto < 0) p2 else p1

    val heroBg = if (equilibrado)
        Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF047857)))
    else
        Brush.linearGradient(listOf(Color(0xFF047857), Color(0xFF059669), Color(0xFF10B981)))

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(heroBg)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        // Círculo decorativo traslúcido de fondo
        Box(
            Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color.White.copy(.06f))
        )
        Box(
            Modifier
                .size(100.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(.05f))
        )

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "BALANCE TOTAL",
                color = Color.White.copy(.55f),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(20.dp))

            if (equilibrado) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Mint.copy(.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Mint, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "¡Todo equilibrado!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text("Sin deuda pendiente", color = Mint.copy(.85f), style = MaterialTheme.typography.bodySmall)
            } else {
                // Avatares con flecha
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AvatarHero(deudor.nombre, Coral)
                    Column(
                        Modifier.padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("debe", color = Color.White.copy(.5f), style = MaterialTheme.typography.labelSmall)
                        Text(
                            "────▶",
                            color = Color.White.copy(.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                    AvatarHero(acreedor.nombre, Mint)
                }
                Spacer(Modifier.height(24.dp))
                // Monto grande
                Text(
                    "$${String.format("%,.2f", abs(balNeto))}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.White.copy(.12f))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        "${deudor.nombre} le debe a ${acreedor.nombre}",
                        color = Color.White.copy(.85f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarHero(nombre: String, ringColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(ringColor.copy(.25f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(ringColor.copy(.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(nombre, color = Color.White.copy(.75f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

// ── Tiempo card ───────────────────────────────────────────────────────────────
@Composable
private fun TiempoCard(
    p1: Padre, p2: Padre,
    horasP1: Double, horasP2: Double,
    totalHoras: Double,
    pctR1: Int, pctR2: Int,
    obj1: Int, obj2: Int,
    dif1: Int, horasDeuda: Double, montoTiempo: Double,
    tipoValor: String, valorHora: Double
) {
    DarkCard(brushBg = Brush.linearGradient(listOf(Color(0x40312E81), Color(0x40312E81)))) {
        // Encabezado
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Lavender.copy(.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AccessTime, null, tint = Lavender, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Tiempo con los hijos", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${String.format("%.0f", totalHoras)} hs totales registradas",
                    color = Color.White.copy(.45f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Barra padre 1
        BarraTiempoVisual(nombre = p1.nombre, horas = horasP1, pctReal = pctR1, pctObj = obj1, color = Lavender)
        Spacer(Modifier.height(12.dp))
        // Barra padre 2
        BarraTiempoVisual(nombre = p2.nombre, horas = horasP2, pctReal = pctR2, pctObj = obj2, color = Pink)

        if (horasDeuda > 0.01) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.08f)))
            Spacer(Modifier.height(14.dp))

            val tipoTxt = when (tipoValor) { "hora" -> "hora"; "semana" -> "semana"; else -> "día" }
            val quien   = if (dif1 < 0) p1.nombre else p2.nombre

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Amber.copy(.12f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Debe compensar", color = Amber.copy(.65f), style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$quien  ·  ${String.format("%.1f", horasDeuda)} hs",
                        color = Amber,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("$${String.format("%.2f", valorHora)} / $tipoTxt", color = Color.White.copy(.35f), style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    "$${String.format("%.2f", montoTiempo)}",
                    color = Amber,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    }
}

@Composable
private fun BarraTiempoVisual(nombre: String, horas: Double, pctReal: Int, pctObj: Int, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(7.dp))
                Text(nombre, color = Color.White.copy(.9f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${String.format("%.0f", horas)} hs", color = Color.White.copy(.45f), style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier.clip(RoundedCornerShape(50.dp)).background(color.copy(.2f)).padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("$pctReal%", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Text("obj $pctObj%", color = Color.White.copy(.3f), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(6.dp))
        // Track
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50.dp)).background(Color.White.copy(.08f))) {
            // Fill real
            Box(
                Modifier
                    .fillMaxWidth((pctReal / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(.6f), color)))
            )
            // Marcador objetivo
            val objFrac = (pctObj / 100f).coerceIn(0.02f, 0.98f)
            Box(Modifier.fillMaxWidth(objFrac).fillMaxHeight()) {
                Box(Modifier.align(Alignment.CenterEnd).width(2.dp).fillMaxHeight().background(Color.White.copy(.55f)))
            }
        }
    }
}

// ── Gastos + Compras card ─────────────────────────────────────────────────────
@Composable
private fun GastosCard(
    p1: Padre, p2: Padre,
    gastoP1: Double, gastoP2: Double,
    compP1: Double, compP2: Double
) {
    DarkCard(brushBg = Brush.linearGradient(listOf(Color(0x40003730), Color(0x40005046)))) {
        if (gastoP1 + gastoP2 > 0) {
            SeccionGastos(
                icono = { Icon(Icons.Outlined.Receipt, null, tint = Mint, modifier = Modifier.size(20.dp)) },
                titulo = "Gastos compartidos",
                total = gastoP1 + gastoP2,
                p1 = p1, v1 = gastoP1,
                p2 = p2, v2 = gastoP2,
                accentColor = Mint
            )
        }
        if (compP1 + compP2 > 0) {
            if (gastoP1 + gastoP2 > 0) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.07f)))
                Spacer(Modifier.height(16.dp))
            }
            SeccionGastos(
                icono = { Icon(Icons.Outlined.ShoppingCart, null, tint = Color(0xFF67E8F9), modifier = Modifier.size(20.dp)) },
                titulo = "Compras de lista",
                total = compP1 + compP2,
                p1 = p1, v1 = compP1,
                p2 = p2, v2 = compP2,
                accentColor = Color(0xFF67E8F9)
            )
        }
        // Diferencia neta
        val dif = abs((gastoP1 - gastoP2) / 2 + (compP1 - compP2) / 2)
        if (dif > 0.01) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.07f)))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Diferencia a repartir", color = Color.White.copy(.5f), style = MaterialTheme.typography.bodySmall)
                Text(
                    "$${String.format("%.2f", dif)}",
                    color = Color(0xFFFCD34D),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SeccionGastos(
    icono: @Composable () -> Unit,
    titulo: String,
    total: Double,
    p1: Padre, v1: Double,
    p2: Padre, v2: Double,
    accentColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(.18f)),
            contentAlignment = Alignment.Center
        ) { icono() }
        Spacer(Modifier.width(10.dp))
        Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text("$${String.format("%,.2f", total)}", color = accentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(12.dp))
    FilaProporcional(p1.nombre, v1, total, accentColor)
    Spacer(Modifier.height(8.dp))
    FilaProporcional(p2.nombre, v2, total, accentColor)
}

@Composable
private fun FilaProporcional(nombre: String, valor: Double, total: Double, color: Color) {
    val frac = if (total > 0) (valor / total).toFloat().coerceIn(0f, 1f) else 0f
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(nombre, color = Color.White.copy(.6f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(76.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50.dp)).background(Color.White.copy(.1f))) {
            Box(
                Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(50.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(.5f), color)))
            )
        }
        Text(
            "$${String.format("%.2f", valor)}",
            color = Color.White.copy(.85f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.End
        )
    }
}

// ── Registrar pago card ───────────────────────────────────────────────────────
@Composable
private fun PagoCard(
    pagador: Padre, receptor: Padre,
    monto: Double, horasDeuda: Double, valorHora: Double,
    onRegistrar: (Compensacion) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF059669), Color(0xFF10B981))))
    ) {
        // Decoración
        Box(
            Modifier.size(120.dp).align(Alignment.TopEnd).offset(20.dp, (-20).dp)
                .clip(CircleShape).background(Color.White.copy(.07f))
        )
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Payment, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Registrar compensación", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("${pagador.nombre} paga a ${receptor.nombre}", color = Color.White.copy(.6f), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    "$${String.format("%,.2f", monto)}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    letterSpacing = (-1).sp
                )
                Button(
                    onClick = {
                        onRegistrar(
                            Compensacion(
                                id = UUID.randomUUID().toString(),
                                fecha = obtenerFechaActual(),
                                idPagador = pagador.id, nombrePagador = pagador.nombre,
                                idReceptor = receptor.id, nombreReceptor = receptor.nombre,
                                horasCompensadas = horasDeuda,
                                valorHora = valorHora,
                                montoTotal = monto,
                                fechaCompleta = System.currentTimeMillis()
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(.22f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Confirmar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Historial card ─────────────────────────────────────────────────────────────
@Composable
private fun HistorialCard(
    comp: Compensacion,
    padres: List<Padre>,
    onEliminar: () -> Unit,
    onEditar: (Compensacion) -> Unit
) {
    // Indicador visual si lleva mas de 3 dias pendiente
    val diasPendiente = if (!comp.confirmada) {
        val ahora = System.currentTimeMillis()
        val diffMs = ahora - comp.fechaCompleta
        (diffMs / (1000L * 60 * 60 * 24)).toInt()
    } else 0
    val pendienteLarga = !comp.confirmada && diasPendiente >= 3
    val WarningAmber = Color(0xFFFF9800)

    // Transicion de color suave segun estado
    val targetAccent = when {
        comp.confirmada -> Mint
        pendienteLarga -> WarningAmber
        else -> Amber
    }
    val accentColor by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(500),
        label = "accentTransition"
    )
    val targetBg = when {
        pendienteLarga -> Color(0x28FF9800)  // Amber tint para urgencia
        comp.confirmada -> Color(0x1534D399) // Green tint sutil
        else -> Glass10
    }
    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(500),
        label = "bgTransition"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
    ) {
        // Acento lateral con grosor segun urgencia
        Box(Modifier.width(if (pendienteLarga) 5.dp else 4.dp).fillMaxHeight().background(accentColor))

        Column(Modifier.weight(1f).padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(50.dp)).background(accentColor.copy(.18f)).padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (pendienteLarga) {
                                Icon(
                                    Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                when {
                                    comp.confirmada -> "Confirmada"
                                    pendienteLarga -> "Pendiente ($diasPendiente d)"
                                    else -> "Pendiente"
                                },
                                color = accentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(comp.fecha, color = Color.White.copy(.35f), style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Coral.copy(.7f), modifier = Modifier.size(15.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(
                        "${comp.nombrePagador}  →  ${comp.nombreReceptor}",
                        color = Color.White.copy(.9f),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (comp.horasCompensadas > 0.01) {
                        Text("${String.format("%.1f", comp.horasCompensadas)} hs", color = Color.White.copy(.35f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(
                    "$${String.format("%,.2f", comp.montoTotal)}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            if (!comp.confirmada) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.08f)))
                Spacer(Modifier.height(10.dp))
                Text("Requiere aprobación de ambos", color = Color.White.copy(.35f), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    padres.forEachIndexed { idx, padre ->
                        val aprobado = if (idx == 0) comp.aceptadoPadre1 else comp.aceptadoPadre2
                        OutlinedButton(
                            onClick = {
                                val upd = if (idx == 0) comp.copy(aceptadoPadre1 = true) else comp.copy(aceptadoPadre2 = true)
                                onEditar(upd)
                            },
                            enabled = !aprobado,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (aprobado) Mint else Color.White.copy(.75f),
                                disabledContentColor = Mint.copy(.7f)
                            ),
                            border = BorderStroke(1.dp, if (aprobado) Mint.copy(.4f) else Color.White.copy(.2f))
                        ) {
                            Text(
                                if (aprobado) "✓ ${padre.nombre}" else padre.nombre,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Diálogos oscuros
// =============================================================================

@Composable
private fun DarkDialogBase(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxWidth(.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(DialogBg)
                // borde sutil
                .padding(1.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(27.dp))
                    .background(Glass10)
                    .padding(24.dp)
            ) {
                Column(content = content)
            }
        }
    }
}

@Composable
fun DialogoValorHora(
    valorActual: Double,
    tipoValor: String = "dia",
    onDismiss: () -> Unit,
    onGuardar: (Double) -> Unit
) {
    val tipoTxt = when (tipoValor) { "hora" -> "hora"; "semana" -> "semana"; else -> "día" }
    var valor by remember { mutableStateOf(valorActual.toString()) }

    DarkDialogBase(onDismiss) {
        Text(
            "Valor por $tipoTxt",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text("Ingresá el monto en $", color = Color.White.copy(.5f), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = valor,
            onValueChange = { valor = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Valor por $tipoTxt ($)", color = Color.White.copy(.5f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Lavender,
                unfocusedBorderColor = Color.White.copy(.25f),
                cursorColor = Lavender,
                focusedLabelColor = Lavender,
                unfocusedLabelColor = Color.White.copy(.4f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(.6f)),
                border = BorderStroke(1.dp, Color.White.copy(.2f))
            ) { Text("Cancelar") }
            Button(
                onClick = { onGuardar(valor.toDoubleOrNull() ?: valorActual) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Lavender, contentColor = Color.White
                )
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun DialogoEditarCompensacion(
    compensacion: Compensacion,
    onDismiss: () -> Unit,
    onGuardar: (Compensacion) -> Unit
) {
    var monto by remember { mutableStateOf(compensacion.montoTotal.toString()) }

    DarkDialogBase(onDismiss) {
        Text("Editar compensación", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = monto,
            onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Monto total ($)", color = Color.White.copy(.5f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Lavender,
                unfocusedBorderColor = Color.White.copy(.25f),
                cursorColor = Lavender,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDismiss, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(.6f)),
                border = BorderStroke(1.dp, Color.White.copy(.2f))
            ) { Text("Cancelar") }
            Button(
                onClick = { onGuardar(compensacion.copy(montoTotal = monto.toDoubleOrNull() ?: compensacion.montoTotal)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lavender, contentColor = Color.White)
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun DialogoTipoValor(
    configuracion: ConfiguracionTiempo,
    padres: List<Padre>,
    onDismiss: () -> Unit,
    onAprobar: (Int) -> Unit,
    onCambiarTipo: (String) -> Unit
) {
    var tipoSeleccionado by remember { mutableStateOf(configuracion.tipoValor) }

    DarkDialogBase(onDismiss) {
        Text("Tipo de compensación", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("Cómo se calcula el valor de referencia", color = Color.White.copy(.45f), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))

        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("hora" to "Por hora", "dia" to "Por día", "semana" to "Por semana").forEach { (tipo, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (tipoSeleccionado == tipo) Lavender.copy(.2f) else Color.White.copy(.05f))
                        .clickable { tipoSeleccionado = tipo }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = tipoSeleccionado == tipo,
                        onClick = { tipoSeleccionado = tipo },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Lavender,
                            unselectedColor = Color.White.copy(.3f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = if (tipoSeleccionado == tipo) Color.White else Color.White.copy(.65f), fontWeight = if (tipoSeleccionado == tipo) FontWeight.SemiBold else FontWeight.Normal)
                }
            }

            if (tipoSeleccionado != configuracion.tipoValor) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onCambiarTipo(tipoSeleccionado) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lavender.copy(.6f), contentColor = Color.White)
                ) { Text("Proponer cambio · requiere aprobación de ambos", style = MaterialTheme.typography.labelSmall) }
            }

            if (tipoSeleccionado == configuracion.tipoValor) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(.08f)))
                Spacer(Modifier.height(12.dp))
                Text("Aprobaciones", color = Color.White.copy(.45f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                padres.forEachIndexed { idx, padre ->
                    val aprobado = if (idx == 0) configuracion.aprobadoTipoValor1 else configuracion.aprobadoTipoValor2
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text(padre.nombre, color = Color.White.copy(.8f), style = MaterialTheme.typography.bodyMedium)
                        if (aprobado) {
                            Box(
                                Modifier.clip(RoundedCornerShape(50.dp)).background(Mint.copy(.2f)).padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("✓ Aprobado", color = Mint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onAprobar(idx) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Lavender),
                                border = BorderStroke(1.dp, Lavender.copy(.4f))
                            ) { Text("Aprobar", style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(.5f))
        ) { Text("Cerrar") }
    }
}

// BalanceGeneralCard — mantenida para compatibilidad
@Composable
fun BalanceGeneralCard(
    padres: List<Padre>,
    gastos: List<Gasto>,
    itemsCompra: List<ItemCompra>,
    compensaciones: List<Compensacion>
) {
    if (padres.size < 2) return
    val p1 = padres[0]; val p2 = padres[1]
    val gP1 = gastos.filter { it.idPagador == p1.id }.sumOf { it.monto }
    val gP2 = gastos.filter { it.idPagador == p2.id }.sumOf { it.monto }
    val cP1 = itemsCompra.filter { it.precio > 0 && it.comprado && it.idPagador == p1.id }.sumOf { it.precio }
    val cP2 = itemsCompra.filter { it.precio > 0 && it.comprado && it.idPagador == p2.id }.sumOf { it.precio }
    GastosCard(p1, p2, gP1, gP2, cP1, cP2)
}
