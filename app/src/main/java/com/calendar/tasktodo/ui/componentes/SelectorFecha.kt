package com.calendar.tasktodo.ui.componentes

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** El selector de Material 3 trabaja en milisegundos UTC a medianoche. */
private fun LocalDate.aMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.aFecha(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Diálogo para elegir una fecha; con [desde], los días anteriores no se pueden elegir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFecha(
    inicial: LocalDate,
    onElegir: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
    desde: LocalDate? = null,
) {
    val permitidas = remember(desde) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = desde == null || !utcTimeMillis.aFecha().isBefore(desde)
        }
    }
    val estado = rememberDatePickerState(initialSelectedDateMillis = inicial.aMillis(), selectableDates = permitidas)
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(onClick = { estado.selectedDateMillis?.aFecha()?.let(onElegir) ?: onCancelar() }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    ) { DatePicker(estado) }
}
