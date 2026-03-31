@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tudominio.crianza

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.*

/**
 * Campo de texto que abre un DatePickerDialog de Material3 al tocarlo.
 * Valor y resultado en formato "YYYY-MM-DD".
 */
@Composable
fun CampoFecha(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            }
        )
        // Overlay clickable transparente sobre el TextField
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { mostrarPicker = true }
        )
    }

    if (mostrarPicker) {
        val sdf = remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        val initialMillis = try {
            sdf.parse(value)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { mostrarPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onValueChange(sdf.format(Date(it)))
                    }
                    mostrarPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/**
 * Campo de texto que abre un TimePicker de Material3 al tocarlo.
 * Valor y resultado en formato "HH:MM" (24h).
 */
@Composable
fun CampoHora(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "HH:MM"
) {
    var mostrarPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            trailingIcon = {
                Icon(Icons.Default.AccessTime, contentDescription = null)
            }
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { mostrarPicker = true }
        )
    }

    if (mostrarPicker) {
        val parts = value.split(":").map { it.toIntOrNull() ?: 0 }
        val state = rememberTimePickerState(
            initialHour = parts.getOrElse(0) { 8 },
            initialMinute = parts.getOrElse(1) { 0 },
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { mostrarPicker = false },
            title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange("%02d:%02d".format(state.hour, state.minute))
                    mostrarPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPicker = false }) { Text("Cancelar") }
            }
        )
    }
}
