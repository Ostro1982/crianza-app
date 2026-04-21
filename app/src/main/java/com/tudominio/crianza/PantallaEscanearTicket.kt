@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tudominio.crianza.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PantallaEscanearTicket(
    onGuardar: (descripcion: String, monto: Double) -> Unit,
    onAtras: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var procesando by remember { mutableStateOf(false) }
    var textoCrudo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            procesando = true
            scope.launch {
                try {
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val result = recognizer.process(image).await()
                    textoCrudo = result.text
                    val (desc, mon) = parsearTicket(result.text)
                    descripcion = desc
                    monto = mon
                } catch (e: Exception) {
                    textoCrudo = "Error: ${e.message}"
                } finally {
                    procesando = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear ticket") },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgGrad2)
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (textoCrudo.isBlank()) "Tomar foto del ticket" else "Tomar otra foto")
            }

            if (procesando) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Leyendo ticket…", color = NeutralVariant30)
                }
            }

            if (textoCrudo.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = GlassWhiteHeavy)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Detectado", fontWeight = FontWeight.Bold, color = Neutral10)
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it },
                            label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = { IconoVoz(onTexto = { descripcion = it }) }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = monto,
                            onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Monto") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val m = monto.toDoubleOrNull() ?: return@Button
                                if (descripcion.isBlank() || m <= 0) return@Button
                                onGuardar(descripcion.trim(), m)
                            },
                            enabled = descripcion.isNotBlank() && monto.toDoubleOrNull() != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Guardar como gasto") }

                        Spacer(Modifier.height(12.dp))
                        Text("Texto completo:", style = MaterialTheme.typography.labelSmall, color = NeutralVariant50)
                        Text(textoCrudo, style = MaterialTheme.typography.bodySmall, color = NeutralVariant30)
                    }
                }
            } else if (!procesando) {
                Text(
                    "Tomá una foto del ticket. Detectamos el total y la descripción automáticamente. Podés corregir antes de guardar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralVariant30
                )
            }
        }
    }
}

/**
 * Parser simple: el monto más grande que tenga "$" o "TOTAL" cerca.
 * Descripción: primera línea con letras, limitada a 40 chars.
 */
private fun parsearTicket(texto: String): Pair<String, String> {
    val lineas = texto.lines().filter { it.isNotBlank() }
    val regexMonto = Regex("""\$?\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?|\d+[.,]?\d*)""")

    val candidatos = mutableListOf<Double>()
    lineas.forEach { linea ->
        val baja = linea.lowercase()
        val esTotal = "total" in baja || "importe" in baja || "pagar" in baja
        regexMonto.findAll(linea).forEach { m ->
            val raw = m.groupValues[1].replace(".", "").replace(",", ".")
            raw.toDoubleOrNull()?.let { n ->
                if (n >= 100 || esTotal) candidatos.add(n)
            }
        }
    }
    val monto = candidatos.maxOrNull()?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    } ?: ""

    // Descripción: primer texto significativo (≥3 letras, no pura cifra)
    val desc = lineas
        .firstOrNull { l -> l.count { it.isLetter() } >= 3 }
        ?.take(40)
        ?.trim()
        ?: ""
    return desc to monto
}
