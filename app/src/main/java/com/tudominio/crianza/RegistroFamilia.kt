@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.tudominio.crianza

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tudominio.crianza.ui.theme.CrianzaTheme
import com.tudominio.crianza.ui.theme.GlassWhite
import com.tudominio.crianza.ui.theme.GlassWhiteHeavy
import com.tudominio.crianza.ui.theme.GradientEnd
import com.tudominio.crianza.ui.theme.GradientMid
import com.tudominio.crianza.ui.theme.GradientStart
import androidx.compose.ui.tooling.preview.Preview

data class PersonaFormulario(
    val nombre: String = "",
    val rol: String = "padre",
    val telefono: String = "",
    val email: String = "",
    val fechaNacimiento: String = "" // YYYY-MM-DD
)

data class HijoFormulario(
    val nombre: String = "",
    val fechaNacimiento: String = ""
)

val ROLES_DISPONIBLES = listOf("padre", "madre", "tutor/a", "abuelo/a", "familiar", "niñera/o", "otro")

private val ROLES_PRINCIPALES = listOf("padre", "madre", "tutor/a", "abuelo/a", "otro")

private val AVATAR_POR_ROL = mapOf(
    "padre" to "👨",
    "madre" to "👩",
    "tutor/a" to "🧑",
    "abuelo/a" to "👴",
    "familiar" to "🧑‍🤝‍🧑",
    "niñera/o" to "🧑‍🍼",
    "otro" to "🙂"
)

@Composable
fun PantallaRegistroFamilia(
    modo: String,
    padresExistentes: List<Padre> = emptyList(),
    hijosExistentes: List<Hijo> = emptyList(),
    onRegistroCompleto: (List<PersonaFormulario>, List<HijoFormulario>) -> Unit,
    onAtras: () -> Unit
) {
    var adultos by remember {
        mutableStateOf(
            if (padresExistentes.isNotEmpty())
                padresExistentes.map { p -> PersonaFormulario(nombre = p.nombre, rol = p.rol, telefono = p.telefono, email = p.email, fechaNacimiento = p.fechaNacimiento) }
            else
                listOf(PersonaFormulario(rol = "padre"), PersonaFormulario(rol = "madre"))
        )
    }
    var hijos by remember {
        mutableStateOf(
            if (hijosExistentes.isNotEmpty())
                hijosExistentes.map { h -> HijoFormulario(nombre = h.nombre, fechaNacimiento = h.fechaNacimiento) }
            else
                listOf<HijoFormulario>()
        )
    }
    var nuevoHijo by remember { mutableStateOf("") }
    var nuevoHijoFecha by remember { mutableStateOf("") }

    val gradientBackground = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to GradientStart,
            0.45f to GradientMid,
            0.75f to Color(0xFF4F46E5),
            1.0f to GradientEnd
        )
    )

    val puedeCrear = adultos.any { it.nombre.isNotBlank() } && hijos.isNotEmpty()
    val edadHijo: (HijoFormulario) -> String? = { h ->
        calcularEdad(h.fechaNacimiento)?.let { "${it} años" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onAtras,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tu familia",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (modo == "juntos") "Padres juntos · mismo hogar"
                               else "Co-parentalidad · hogares separados",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Sección Adultos ───────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "👥",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Adultos responsables",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        onClick = { adultos = adultos + PersonaFormulario(rol = "familiar") },
                        color = GlassWhiteHeavy,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Agregar adulto",
                            tint = Color.White,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                }

                adultos.forEachIndexed { index, persona ->
                    AdultoCard(
                        persona = persona,
                        index = index,
                        mostrarEliminar = adultos.size > 1,
                        onCambiar = { nuevo ->
                            adultos = adultos.toMutableList().also { it[index] = nuevo }
                        },
                        onEliminar = {
                            adultos = adultos.toMutableList().also { it.removeAt(index) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Sección Hijos ─────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🧒",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Niños y niñas",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Chips de hijos existentes
                if (hijos.isNotEmpty()) {
                    Surface(color = GlassWhite, shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp).animateContentSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hijos.forEachIndexed { index, hijo ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            color = GlassWhiteHeavy, shape = CircleShape,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🧒", fontSize = 18.sp)
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = hijo.nombre,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                            val edad = edadHijo(hijo)
                                            if (edad != null) {
                                                Text(
                                                    text = edad,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(0.65f)
                                                )
                                            } else if (hijo.fechaNacimiento.isNotBlank()) {
                                                Text(
                                                    text = hijo.fechaNacimiento,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(0.65f)
                                                )
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = { hijos = hijos.toMutableList().also { it.removeAt(index) } },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Eliminar",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (index < hijos.lastIndex) Divider(color = Color.White.copy(alpha = 0.15f))
                            }
                        }
                    }
                }

                // Campo agregar hijo (nombre + fecha)
                Surface(color = GlassWhite, shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nuevoHijo,
                                onValueChange = { nuevoHijo = it },
                                placeholder = { Text("Nombre del niño/a", color = Color.White.copy(0.5f)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White.copy(0.5f),
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilledIconButton(
                                onClick = {
                                    if (nuevoHijo.isNotBlank()) {
                                        hijos = hijos + HijoFormulario(
                                            nombre = nuevoHijo.trim(),
                                            fechaNacimiento = nuevoHijoFecha.trim()
                                        )
                                        nuevoHijo = ""
                                        nuevoHijoFecha = ""
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = GlassWhiteHeavy)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
                            }
                        }
                        CampoFecha(
                            value = nuevoHijoFecha,
                            label = "Fecha de nacimiento (opcional)",
                            onValueChange = { nuevoHijoFecha = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(0.5f),
                                unfocusedBorderColor = Color.White.copy(0.2f),
                                cursorColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = Color.White.copy(0.7f),
                                unfocusedLabelColor = Color.White.copy(0.4f),
                                focusedTrailingIconColor = Color.White.copy(0.7f),
                                unfocusedTrailingIconColor = Color.White.copy(0.5f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Botón Crear Familia (floating) ────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, GradientEnd.copy(alpha = 0.95f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = { onRegistroCompleto(adultos, hijos) },
                enabled = puedeCrear,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = GradientMid,
                    disabledContainerColor = Color.White.copy(alpha = 0.3f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Crear familia  →",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AdultoCard(
    persona: PersonaFormulario,
    index: Int,
    mostrarEliminar: Boolean,
    onCambiar: (PersonaFormulario) -> Unit,
    onEliminar: () -> Unit
) {
    val avatar = AVATAR_POR_ROL[persona.rol] ?: "🙂"
    val edadAdulto = calcularEdad(persona.fechaNacimiento)

    Surface(
        color = GlassWhite,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header de la tarjeta
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = GlassWhiteHeavy,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(avatar, fontSize = 22.sp)
                        }
                    }
                    Column {
                        Text(
                            text = if (persona.nombre.isBlank()) "Persona ${index + 1}"
                                   else persona.nombre,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (edadAdulto != null) "${persona.rol} · $edadAdulto" else persona.rol,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                if (mostrarEliminar) {
                    IconButton(
                        onClick = onEliminar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Campo nombre
            OutlinedTextField(
                value = persona.nombre,
                onValueChange = { onCambiar(persona.copy(nombre = it)) },
                placeholder = { Text("Nombre completo", color = Color.White.copy(0.45f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    cursorColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Selector de rol como chips
            Text(
                "Rol",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ROLES_PRINCIPALES.forEach { rol ->
                    val seleccionado = persona.rol == rol
                    FilterChip(
                        selected = seleccionado,
                        onClick = { onCambiar(persona.copy(rol = rol)) },
                        label = {
                            Text(
                                "${AVATAR_POR_ROL[rol] ?: "🙂"} $rol",
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White.copy(alpha = 0.9f),
                            selectedLabelColor = GradientMid,
                            containerColor = GlassWhiteHeavy,
                            labelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selectedBorderColor = Color.Transparent,
                            borderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            // Campo fecha de nacimiento
            CampoFecha(
                value = persona.fechaNacimiento,
                label = "Fecha de nacimiento (opcional)",
                onValueChange = { onCambiar(persona.copy(fechaNacimiento = it)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    cursorColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedLabelColor = Color.White.copy(0.7f),
                    unfocusedLabelColor = Color.White.copy(0.4f),
                    focusedTrailingIconColor = Color.White.copy(0.7f),
                    unfocusedTrailingIconColor = Color.White.copy(0.5f)
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaRegistroFamiliaPreview() {
    CrianzaTheme {
        PantallaRegistroFamilia(
            modo = "separados",
            onRegistroCompleto = { _, _ -> },
            onAtras = {}
        )
    }
}
