package com.tudominio.crianza

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class TipoItemCompartido { GASTO, COMPRA, PENDIENTE }

@Composable
fun DialogoTextoCompartido(
    texto: String,
    onElegir: (TipoItemCompartido) -> Unit,
    onDescartar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDescartar,
        title = { Text("Texto compartido", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "\"${texto.take(200)}${if (texto.length > 200) "…" else ""}\"",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Text("¿Guardar como?", style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onElegir(TipoItemCompartido.GASTO) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Gasto") }
                    OutlinedButton(
                        onClick = { onElegir(TipoItemCompartido.COMPRA) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Compra") }
                    OutlinedButton(
                        onClick = { onElegir(TipoItemCompartido.PENDIENTE) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Pendiente") }
                }
                TextButton(
                    onClick = onDescartar,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Descartar") }
            }
        }
    )
}

/** Parser: extrae monto de texto tipo "compré pilas 3500" o "$3500" */
fun extraerMonto(texto: String): Double? {
    val regex = Regex("""\$?\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?|\d+[.,]?\d*)""")
    return regex.findAll(texto)
        .mapNotNull { m ->
            m.groupValues[1].replace(".", "").replace(",", ".").toDoubleOrNull()
        }
        .maxOrNull()
}

fun limpiarTextoCompartido(context: Context) {
    context.getSharedPreferences("crianza_prefs", Context.MODE_PRIVATE).edit()
        .remove("pending_share_text")
        .remove("pending_share_ts")
        .apply()
}
