@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.tudominio.crianza.ui.theme.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

// Categorías predeterminadas
val PLANTILLAS_CATEGORIAS: Map<String, List<String>> = mapOf(
    "🍎 Alimentos" to listOf(
        "Leche", "Pan", "Huevos", "Fruta", "Verdura", "Yogur", "Queso", "Jamón",
        "Pasta", "Arroz", "Aceite", "Sal", "Azúcar", "Galletitas", "Jugo", "Manteca"
    ),
    "🧹 Limpieza" to listOf(
        "Detergente", "Jabón", "Lavandina", "Papel higiénico", "Shampoo",
        "Acondicionador", "Pasta dental", "Cepillo de dientes", "Esponja"
    ),
    "✏️ Útiles escolares" to listOf(
        "Cuaderno rayado", "Cuaderno cuadriculado", "Cartuchera", "Lápices de color",
        "Marcadores", "Tijera", "Regla", "Plasticola", "Carpeta", "Hojas A4"
    ),
    "👕 Ropa" to listOf(
        "Medias", "Ropa interior", "Remera", "Pantalón", "Abrigo", "Zapatillas"
    ),
    "💊 Medicamentos" to listOf(
        "Ibuprofeno", "Paracetamol", "Jarabe para la tos", "Antihistamínico",
        "Crema solar", "Repelente", "Alcohol en gel"
    ),
    "🎁 Regalos" to listOf(
        "Papel de regalo", "Moño", "Tarjeta", "Cinta adhesiva"
    ),
    "👶 Bebés / Niños pequeños" to listOf(
        "Pañales", "Toallitas húmedas", "Leche de fórmula", "Papilla", "Crema para pañal"
    )
)

@Composable
fun PantallaListaCompras(
    items: List<ItemCompra>,
    padres: List<Padre>,
    idPadreActual: String,
    categoriasPersonalizadas: List<CategoriaCompra>,
    onAgregar: (ItemCompra) -> Unit,
    onActualizar: (ItemCompra) -> Unit,
    onEliminar: (ItemCompra) -> Unit,
    onEliminarComprados: () -> Unit,
    onAgregarCategoria: (CategoriaCompra) -> Unit,
    onAtras: () -> Unit
) {
    var mostrarDialogoAgregar by remember { mutableStateOf(false) }
    var tabSeleccionado by remember { mutableIntStateOf(0) }

    val efectivoPadreId = idPadreActual.ifEmpty { padres.firstOrNull()?.id ?: "" }
    val compartidos = items.filter { !it.esPrivado }
    val privados = items.filter { it.esPrivado && it.idPropietario == efectivoPadreId }
    val itemsMostrados = when (tabSeleccionado) {
        0 -> compartidos
        1 -> privados
        else -> compartidos + privados
    }

    val pendientes = itemsMostrados.filter { !it.comprado }
    val comprados = itemsMostrados.filter { it.comprado }

    Box(Modifier.fillMaxSize().background(BgGrad4)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lista de compras", fontWeight = FontWeight.Bold, color = Color.White)
                        if (padres.size >= 2) {
                            val nombre = padres.find { it.id == idPadreActual }?.nombre ?: ""
                            if (nombre.isNotEmpty())
                                Text(
                                    "Hola, $nombre",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    if (comprados.isNotEmpty()) {
                        TextButton(onClick = onEliminarComprados) {
                            Text("Limpiar ✓", color = Color(0xFFF87171))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogoAgregar = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agregar") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Chips scrollables con estado activo claro
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val tabs = listOf(
                    Triple(0, "Compartida", compartidos.size),
                    Triple(1, "Privada", privados.size),
                    Triple(2, "Todas", compartidos.size + privados.size)
                )
                items(tabs) { (index, label, count) ->
                    val isSelected = tabSeleccionado == index
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f),
                        animationSpec = tween(300),
                        label = "chipBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f),
                        animationSpec = tween(300),
                        label = "chipBorder"
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(bgColor)
                            .then(
                                Modifier.padding(1.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(if (isSelected) Color.Transparent else Color.Transparent)
                            )
                            .clickable { tabSeleccionado = index }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    Modifier.size(7.dp).clip(CircleShape).background(Color.White)
                                )
                            }
                            Text(
                                label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (count > 0) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "$count",
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (itemsMostrados.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛒", style = MaterialTheme.typography.displayLarge)
                        Text(
                            if (tabSeleccionado == 1) "Tu lista privada está vacía"
                            else "La lista está vacía",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "Tocá el botón para agregar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (pendientes.isNotEmpty()) {
                        item {
                            Text(
                                "PENDIENTES  •  ${pendientes.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(pendientes, key = { it.id }) { item ->
                            TarjetaItemCompra(
                                item = item,
                                onToggle = { onActualizar(item.copy(comprado = !item.comprado)) },
                                onEliminar = { onEliminar(item) }
                            )
                        }
                    }
                    if (comprados.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "COMPRADOS  •  ${comprados.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(comprados, key = { it.id }) { item ->
                            TarjetaItemCompra(
                                item = item,
                                onToggle = { onActualizar(item.copy(comprado = !item.comprado)) },
                                onEliminar = { onEliminar(item) }
                            )
                        }
                    }
                }
            }
        }
    }
    }  // Box

    if (mostrarDialogoAgregar) {
        DialogoAgregarItem(
            padres = padres,
            idPadreActual = idPadreActual,
            esPrivadoPorDefecto = tabSeleccionado == 1,
            categoriasPersonalizadas = categoriasPersonalizadas,
            onAgregarCategoria = onAgregarCategoria,
            onDismiss = { mostrarDialogoAgregar = false },
            onGuardar = { item ->
                onAgregar(item)
                mostrarDialogoAgregar = false
            }
        )
    }
}

@Composable
fun TarjetaItemCompra(
    item: ItemCompra,
    onToggle: () -> Unit,
    onEliminar: () -> Unit
) {
    val targetAlpha = if (item.comprado) 0.4f else 1f
    val animBgColor by animateColorAsState(
        targetValue = if (item.comprado) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(350),
        label = "cardBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(targetAlpha)
            .animateContentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(animBgColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox circular custom look
            Checkbox(
                checked = item.comprado,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF34D399),
                    checkmarkColor = Color.White,
                    uncheckedColor = Color.White.copy(alpha = 0.45f)
                )
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.esPrivado) Text("🔒", style = MaterialTheme.typography.bodySmall)
                    val cantidadStr = if (item.cantidad.isNotEmpty() && item.cantidad != "1")
                        "${item.cantidad}${if (item.unidad.isNotEmpty()) " ${item.unidad}" else ""}  " else ""
                    Text(
                        text = "$cantidadStr${item.descripcion}",
                        fontWeight = if (!item.comprado) FontWeight.SemiBold else FontWeight.Normal,
                        textDecoration = if (item.comprado) TextDecoration.LineThrough else TextDecoration.None,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item.comprado) Color.White.copy(alpha = 0.35f) else Color.White
                    )
                }
                val meta = listOfNotNull(
                    item.categoria.takeIf { it.isNotEmpty() }?.let { cat ->
                        if (item.subcategoria.isNotEmpty() && item.subcategoria != item.descripcion)
                            "$cat > ${item.subcategoria}" else cat
                    },
                    if (item.precio > 0) "$${String.format("%.2f", item.precio)}" else null,
                    item.agregadoPor.takeIf { it.isNotEmpty() }?.let { "por $it" }
                ).joinToString("  ·  ")
                if (meta.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = if (item.comprado) 0.2f else 0.55f),
                        textDecoration = if (item.comprado) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }
            if (!item.comprado) {
                IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFF87171).copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Comprado: icono de check con fondo circular
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF34D399).copy(.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Comprado",
                        tint = Color(0xFF34D399).copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DialogoAgregarItem(
    padres: List<Padre>,
    idPadreActual: String,
    esPrivadoPorDefecto: Boolean,
    categoriasPersonalizadas: List<CategoriaCompra>,
    onAgregarCategoria: (CategoriaCompra) -> Unit,
    onDismiss: () -> Unit,
    onGuardar: (ItemCompra) -> Unit
) {
    var categoriaSeleccionada by remember { mutableStateOf("") }
    var subcategoriaSeleccionada by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var unidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var idAgregador by remember { mutableStateOf(idPadreActual.ifEmpty { padres.firstOrNull()?.id ?: "" }) }
    var esPrivado by remember { mutableStateOf(esPrivadoPorDefecto) }
    var mostrarSelectorCategoria by remember { mutableStateOf(false) }

    // Productos disponibles según la categoría elegida
    val productosCategoria: List<String> = remember(categoriaSeleccionada, categoriasPersonalizadas) {
        if (categoriaSeleccionada.isEmpty()) emptyList()
        else {
            val predeterminados = PLANTILLAS_CATEGORIAS[categoriaSeleccionada] ?: emptyList()
            val personalizados = categoriasPersonalizadas
                .find { it.nombre == categoriaSeleccionada }
                ?.subcategorias?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
            (predeterminados + personalizados).distinct()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Agregar producto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── 1. CATEGORÍA (campo principal) ────────────────────────────
                Text("Categoría", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = when {
                            categoriaSeleccionada.isEmpty() -> ""
                            subcategoriaSeleccionada.isNotEmpty() && subcategoriaSeleccionada != desc ->
                                "$categoriaSeleccionada  ›  $subcategoriaSeleccionada"
                            else -> categoriaSeleccionada
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seleccioná una categoría") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { mostrarSelectorCategoria = true })
                }

                // ── 2. CHIPS de productos sugeridos ───────────────────────────
                if (productosCategoria.isNotEmpty()) {
                    Text("Productos de esta categoría:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(productosCategoria) { prod ->
                            val selected = desc.equals(prod, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) {
                                        desc = ""
                                        subcategoriaSeleccionada = ""
                                    } else {
                                        desc = prod
                                        subcategoriaSeleccionada = prod
                                    }
                                },
                                label = { Text(prod, style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                                } else null
                            )
                        }
                    }
                }

                // ── 3. PRODUCTO (nombre) ───────────────────────────────────────
                Text("Producto", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = desc,
                    onValueChange = {
                        desc = it
                        subcategoriaSeleccionada = ""
                    },
                    label = { Text("Nombre del producto") },
                    placeholder = { Text("Escribí o elegí de arriba") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // ── 4. Cantidad + Unidad ──────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = unidad,
                        onValueChange = { unidad = it },
                        label = { Text("Unidad") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("kg, L, u.") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── 5. Precio ─────────────────────────────────────────────────
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Precio estimado (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$  ") },
                    shape = RoundedCornerShape(12.dp)
                )

                // ── 6. Quién lo agrega ────────────────────────────────────────
                if (padres.isNotEmpty()) {
                    Text("¿Quién lo agrega?", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        padres.forEach { padre ->
                            FilterChip(
                                selected = idAgregador == padre.id,
                                onClick = { idAgregador = padre.id },
                                label = { Text(padre.nombre) },
                                leadingIcon = if (idAgregador == padre.id) {
                                    { Icon(Icons.Default.Check, contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                                } else null
                            )
                        }
                    }
                }

                // ── 7. Lista privada ──────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { esPrivado = !esPrivado },
                    colors = CardDefaults.cardColors(
                        containerColor = if (esPrivado)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = esPrivado, onCheckedChange = { esPrivado = it })
                        Column {
                            Text("🔒 Lista privada", fontWeight = FontWeight.Medium)
                            Text("Solo vos lo verás", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Auto-guardar producto nuevo en su categoría
                    if (categoriaSeleccionada.isNotEmpty() && desc.isNotBlank()) {
                        val existentesPred = PLANTILLAS_CATEGORIAS[categoriaSeleccionada] ?: emptyList()
                        val existentesPerson = categoriasPersonalizadas
                            .find { it.nombre == categoriaSeleccionada }
                            ?.subcategorias?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
                        if (!existentesPred.any { it.equals(desc.trim(), ignoreCase = true) } &&
                            !existentesPerson.any { it.equals(desc.trim(), ignoreCase = true) }
                        ) {
                            val nuevasSubcats = (existentesPerson + desc.trim()).joinToString("|")
                            onAgregarCategoria(CategoriaCompra(nombre = categoriaSeleccionada, subcategorias = nuevasSubcats))
                        }
                    }
                    onGuardar(
                        ItemCompra(
                            id = UUID.randomUUID().toString(),
                            descripcion = desc.trim(),
                            cantidad = cantidad.ifBlank { "1" },
                            unidad = unidad.trim(),
                            categoria = categoriaSeleccionada,
                            subcategoria = subcategoriaSeleccionada,
                            precio = precio.toDoubleOrNull() ?: 0.0,
                            agregadoPor = padres.find { it.id == idAgregador }?.nombre ?: "",
                            idPagador = idAgregador,
                            fechaCompleta = System.currentTimeMillis(),
                            esPrivado = esPrivado,
                            idPropietario = if (esPrivado) idAgregador else ""
                        )
                    )
                },
                enabled = desc.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(20.dp)
    )

    if (mostrarSelectorCategoria) {
        DialogoSelectorCategoria(
            categoriasPersonalizadas = categoriasPersonalizadas,
            onSeleccionar = { cat, sub ->
                categoriaSeleccionada = cat
                subcategoriaSeleccionada = sub ?: ""
                if (sub != null) desc = sub
                mostrarSelectorCategoria = false
            },
            onAgregarCategoria = onAgregarCategoria,
            onDismiss = { mostrarSelectorCategoria = false }
        )
    }
}

@Composable
fun DialogoSelectorCategoria(
    categoriasPersonalizadas: List<CategoriaCompra>,
    onSeleccionar: (categoria: String, subcategoria: String?) -> Unit,
    onAgregarCategoria: (CategoriaCompra) -> Unit,
    onDismiss: () -> Unit
) {
    var nivelActual by remember { mutableStateOf<String?>(null) }
    var mostrarAgregarCategoria by remember { mutableStateOf(false) }
    var mostrarAgregarSubcat by remember { mutableStateOf(false) }
    var nuevaCatNombre by remember { mutableStateOf("") }
    var nuevaSubcat by remember { mutableStateOf("") }

    val catPersonalizadasMap = categoriasPersonalizadas.associate { cat ->
        cat.nombre to cat.subcategorias.split("|").filter { it.isNotEmpty() }
    }
    val todasLasCategorias: Map<String, List<String>> = buildMap {
        putAll(PLANTILLAS_CATEGORIAS)
        catPersonalizadasMap.forEach { (nombre, subcats) ->
            val existentes = this[nombre] ?: emptyList()
            put(nombre, (existentes + subcats).distinct())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nivelActual != null) {
                    TextButton(onClick = { nivelActual = null }, contentPadding = PaddingValues(0.dp)) {
                        Text("← ", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Text(
                    nivelActual ?: "Categorías",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (nivelActual == null) {
                    // ── Nivel 1: lista de categorías ──────────────────────────
                    todasLasCategorias.keys.forEach { cat ->
                        val esPredeterminada = PLANTILLAS_CATEGORIAS.containsKey(cat)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { nivelActual = cat },
                            colors = CardDefaults.cardColors(
                                containerColor = if (esPredeterminada)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                val count = todasLasCategorias[cat]?.size ?: 0
                                if (count > 0)
                                    Text("$count", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("›", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(4.dp))

                    if (mostrarAgregarCategoria) {
                        OutlinedTextField(
                            value = nuevaCatNombre,
                            onValueChange = { nuevaCatNombre = it },
                            label = { Text("Nombre de la nueva categoría") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { mostrarAgregarCategoria = false; nuevaCatNombre = "" },
                                modifier = Modifier.weight(1f)) { Text("Cancelar") }
                            Button(
                                onClick = {
                                    if (nuevaCatNombre.isNotBlank()) {
                                        onAgregarCategoria(CategoriaCompra(nombre = nuevaCatNombre.trim()))
                                        onSeleccionar(nuevaCatNombre.trim(), null)
                                    }
                                },
                                enabled = nuevaCatNombre.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) { Text("Crear") }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { mostrarAgregarCategoria = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("+ Nueva categoría") }
                    }

                } else {
                    // ── Nivel 2: productos de la categoría ────────────────────
                    val subcats = todasLasCategorias[nivelActual!!] ?: emptyList()

                    FilledTonalButton(
                        onClick = { onSeleccionar(nivelActual!!, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Usar solo categoría \"${nivelActual!!.take(20)}\"") }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (subcats.isEmpty()) {
                        Text("Sin productos aún. Agregá uno:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    } else {
                        subcats.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSeleccionar(nivelActual!!, sub) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sub, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text("›", color = MaterialTheme.colorScheme.outline)
                            }
                            Divider(modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (mostrarAgregarSubcat) {
                        OutlinedTextField(
                            value = nuevaSubcat,
                            onValueChange = { nuevaSubcat = it },
                            label = { Text("Nuevo producto") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { mostrarAgregarSubcat = false; nuevaSubcat = "" },
                                modifier = Modifier.weight(1f)) { Text("Cancelar") }
                            Button(
                                onClick = {
                                    if (nuevaSubcat.isNotBlank()) {
                                        val catExistente = categoriasPersonalizadas.find { it.nombre == nivelActual }
                                        val subcatsExistentes = catExistente?.subcategorias
                                            ?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
                                        onAgregarCategoria(
                                            CategoriaCompra(
                                                nombre = nivelActual!!,
                                                subcategorias = (subcatsExistentes + nuevaSubcat.trim()).joinToString("|")
                                            )
                                        )
                                        onSeleccionar(nivelActual!!, nuevaSubcat.trim())
                                    }
                                },
                                enabled = nuevaSubcat.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) { Text("Agregar") }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { mostrarAgregarSubcat = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("+ Nuevo producto") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        dismissButton = null
    )
}
