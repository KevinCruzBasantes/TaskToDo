package com.calendar.tasktodo.ui.dia

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.ui.componentes.EstadoVacio
import com.calendar.tasktodo.ui.componentes.FilaClase
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.rememberFormatoHora
import com.calendar.tasktodo.ui.textoLargo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaScreen(
    fechaInicial: LocalDate?,
    onAbrirClase: (Long) -> Unit,
    onVerHorario: () -> Unit,
    viewModel: DiaViewModel = viewModel(factory = DiaViewModel.factory(fechaInicial)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    var eligiendoFecha by rememberSaveable { mutableStateOf(false) }

    Pantalla(
        enBarra = true,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(estado.fecha.textoLargo(), style = MaterialTheme.typography.titleMedium)
                        if (estado.esHoy) {
                            Text("Hoy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::anterior) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior")
                    }
                },
                actions = {
                    if (!estado.esHoy) TextButton(onClick = viewModel::irAHoy) { Text("Hoy") }
                    IconButton(onClick = { eligiendoFecha = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Elegir fecha")
                    }
                    IconButton(onClick = viewModel::siguiente) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Día siguiente")
                    }
                },
            )
        },
    ) { modifier ->
        when {
            estado.cargando -> Unit
            estado.clases.isEmpty() -> EstadoVacio(
                mensaje = "No hay clases este día",
                accion = "Ver horario",
                onAccion = onVerHorario,
                modifier = modifier,
            )
            else -> LazyColumn(modifier.fillMaxSize()) {
                items(estado.clases, key = { it.clase.id }) { clase ->
                    FilaClase(clase, formatoHora, onClick = { onAbrirClase(clase.clase.id) })
                }
            }
        }
    }

    if (eligiendoFecha) {
        // El selector trabaja en milisegundos UTC a medianoche
        val estadoSelector = rememberDatePickerState(
            initialSelectedDateMillis = estado.fecha.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { eligiendoFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoSelector.selectedDateMillis?.let { millis ->
                        viewModel.irA(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    eligiendoFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { eligiendoFecha = false }) { Text("Cancelar") } },
        ) { DatePicker(estadoSelector) }
    }
}
