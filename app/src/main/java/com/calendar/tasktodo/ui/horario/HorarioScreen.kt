package com.calendar.tasktodo.ui.horario

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.ui.componentes.EstadoVacio
import com.calendar.tasktodo.ui.componentes.FilaClase
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.nombre
import com.calendar.tasktodo.ui.rememberFormatoHora

/** Pestaña Calendario. En el incremento 1 solo muestra el horario semanal (CU-05). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioScreen(
    onAbrirClase: (Long) -> Unit,
    onNuevaClase: () -> Unit,
    onNuevaMateria: () -> Unit,
    onMaterias: () -> Unit,
    viewModel: HorarioViewModel = viewModel(factory = HorarioViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()

    Pantalla(
        enBarra = true,
        topBar = {
            TopAppBar(
                title = { Text("Horario") },
                actions = { TextButton(onClick = onMaterias) { Text("Materias") } },
            )
        },
        floatingActionButton = {
            if (estado.hayMaterias) {
                FloatingActionButton(onClick = onNuevaClase) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar clase")
                }
            }
        },
    ) { modifier ->
        when {
            estado.cargando -> Unit
            !estado.hayMaterias -> EstadoVacio(
                mensaje = "Primero agrega una materia",
                detalle = "Cada clase pertenece a una materia.",
                accion = "Agregar materia",
                onAccion = onNuevaMateria,
                modifier = modifier,
            )
            estado.dias.isEmpty() -> EstadoVacio(
                mensaje = "Aún no tienes clases",
                accion = "Agregar clase",
                onAccion = onNuevaClase,
                modifier = modifier,
            )
            else -> LazyColumn(modifier.fillMaxSize()) {
                estado.dias.forEach { (dia, clases) ->
                    item(key = dia) {
                        Text(
                            dia.nombre().uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(clases, key = { it.clase.id }) { clase ->
                        FilaClase(clase, formatoHora, onClick = { onAbrirClase(clase.clase.id) })
                    }
                }
            }
        }
    }
}
