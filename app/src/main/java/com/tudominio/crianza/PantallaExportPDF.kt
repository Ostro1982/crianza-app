@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TipoExportPDF { CUSTODIA, GASTOS }

@Composable
fun PantallaExportPDF(
    tipo: TipoExportPDF,
    onAtras: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var rangoSel by remember { mutableStateOf("mes") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var generando by remember { mutableStateOf(false) }

    val titulo = if (tipo == TipoExportPDF.CUSTODIA) "Resumen días con los chicos" else "Resumen de gastos"
    val nombreArchivo: () -> String = {
        val pref = if (tipo == TipoExportPDF.CUSTODIA) "custodia" else "gastos"
        val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        "nesty_${pref}_$ts.pdf"
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            generando = true
            mensaje = "Generando PDF…"
            val rango = when (rangoSel) {
                "mes" -> PDFExporter.Rango.mesActual()
                "3m" -> PDFExporter.Rango.ultimosMeses(3)
                "6m" -> PDFExporter.Rango.ultimosMeses(6)
                else -> PDFExporter.Rango.todo()
            }
            val familyId = FamilyIdManager.obtenerFamilyId(context)
            val r = withContext(Dispatchers.IO) {
                if (tipo == TipoExportPDF.CUSTODIA) {
                    val regs = db.registroTiempoDao().obtenerTodosLosRegistros()
                    val padres = db.familiaDao().obtenerTodosLosPadres()
                    val hijos = db.familiaDao().obtenerTodosLosHijos()
                    PDFExporter.exportarCustodia(context, uri, regs, padres, hijos, familyId, rango)
                } else {
                    val gastos = db.gastoDao().obtenerTodosLosGastos()
                    val comps = db.compensacionDao().obtenerTodasLasCompensaciones()
                    PDFExporter.exportarGastos(context, uri, gastos, comps, familyId, rango, MonedaConfig.actual)
                }
            }
            mensaje = if (r.isSuccess) "PDF guardado correctamente"
            else "Error: ${r.exceptionOrNull()?.message}"
            generando = false
        }
    }

    Box(Modifier.fillMaxSize().background(BgGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(titulo, color = Neutral10) },
                    navigationIcon = {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = NeutralVariant30)
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
                Text(
                    if (tipo == TipoExportPDF.CUSTODIA)
                        "Resumen ordenado de los días que tuvo a los chicos cada uno, porcentajes del período y detalle día por día. Lleva una firma técnica al pie por si necesitás validar el archivo más adelante."
                    else
                        "Resumen ordenado de gastos del período, recibos adjuntos (cuando hay), compensaciones registradas y total acumulado. Lleva una firma técnica al pie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralVariant30
                )

                Text("Período",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                listOf(
                    "mes" to "Mes actual",
                    "3m" to "Últimos 3 meses",
                    "6m" to "Últimos 6 meses",
                    "todo" to "Todo el historial"
                ).forEach { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = rangoSel == key,
                            onClick = { rangoSel = key }
                        )
                        Text(label, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !generando,
                    onClick = { launcher.launch(nombreArchivo()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (generando) "Generando…" else "Elegir destino y exportar")
                }

                mensaje?.let {
                    Text(it,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralVariant30
                    )
                }
            }
        }
    }
}
