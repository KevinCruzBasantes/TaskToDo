package com.calendar.tasktodo.ui.horario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.ui.componentes.CampoHora
import com.calendar.tasktodo.ui.componentes.EstadoVacio
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.componentes.PuntoColor
import com.calendar.tasktodo.ui.nombre
import com.calendar.tasktodo.ui.nombreCorto
import com.calendar.tasktodo.ui.rangoHoras
import com.calendar.tasktodo.ui.rememberFormatoHora
import java.time.DayOfWeek

/** Formulario de clase (CU-04). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormClaseScreen(
    id: Long?,
    materiaId: Long?,
    onTerminar: () -> Unit,
    onNuevaMateria: () -> Unit,
    viewModel: FormClaseViewModel = viewModel(factory = FormClaseViewModel.factory(id, materiaId)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()

    LaunchedEffect(estado.terminado) { if (estado.terminado) onTerminar() }

    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Editar clase" else "Nueva clase") },
                navigationIcon = {
                    IconButton(onClick = onTerminar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") }
                },
                actions = {
                    if (estado.materias.isNotEmpty()) {
                        TextButton(onClick = { viewModel.guardar() }, enabled = !estado.cargando && !estado.ocupado) {
                            Text("Guardar")
                        }
                    }
                },
            )
        },
    ) { modifier ->
        if (estado.cargando) return@Pantalla
        if (estado.materias.isEmpty()) {
            EstadoVacio(
                mensaje = "Primero registra una materia",
                accion = "Agregar materia",
                onAccion = onNuevaMateria,
                modifier = modifier,
            )
            return@Pantalla
        }
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SelectorMateria(
                estado = estado,
                onElegir = viewModel::cambiarMateria,
            )
            Spacer(Modifier.height(16.dp))
            Text("Día", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { dia ->
                    FilterChip(
                        selected = estado.dia == dia,
                        onClick = { viewModel.cambiarDia(dia) },
                        label = { Text(dia.nombreCorto()) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CampoHora("Inicio", estado.horaInicio, formatoHora, viewModel::cambiarInicio, Modifier.weight(1f))
                CampoHora("Fin", estado.horaFin, formatoHora, viewModel::cambiarFin, Modifier.weight(1f))
            }
            estado.errorHoras?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = estado.aula,
                onValueChange = viewModel::cambiarAula,
                label = { Text("Aula (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (estado.cruces.isNotEmpty()) {
        val detalle = estado.cruces.joinToString("\n") {
            "• ${it.materiaNombre}, ${rangoHoras(it.clase.horaInicio, it.clase.horaFin, formatoHora)}"
        }
        AlertDialog(
            onDismissRequest = viewModel::descartarCruces,
            title = { Text("Se cruza con otra clase") },
            text = { Text("El ${estado.dia.nombre().lowercase()} ya tienes:\n$detalle") },
            confirmButton = { TextButton(onClick = { viewModel.guardar(confirmarCruce = true) }) { Text("Guardar igual") } },
            dismissButton = { TextButton(onClick = viewModel::descartarCruces) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorMateria(estado: FormClaseUiState, onElegir: (Long) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val elegida = estado.materias.firstOrNull { it.id == estado.materiaId }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = elegida?.nombre.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Materia") },
            leadingIcon = elegida?.let { { PuntoColor(it.color) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            isError = estado.errorMateria != null,
            supportingText = estado.errorMateria?.let { error -> { Text(error) } },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            estado.materias.forEach { materia ->
                DropdownMenuItem(
                    text = { Text(materia.nombre) },
                    leadingIcon = { PuntoColor(materia.color) },
                    onClick = {
                        onElegir(materia.id)
                        abierto = false
                    },
                )
            }
        }
    }
}
