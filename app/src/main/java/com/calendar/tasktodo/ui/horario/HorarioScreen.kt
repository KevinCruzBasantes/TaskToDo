package com.calendar.tasktodo.ui.horario

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.ui.componentes.EstadoVacio
import com.calendar.tasktodo.ui.componentes.FilaClase
import com.calendar.tasktodo.ui.nombre
import com.calendar.tasktodo.ui.rememberFormatoHora

/**
 * Pestaña Horario de Calendario (CU-05): la plantilla de clases de lunes a domingo, sin fechas.
 * La barra superior y las pestañas las pone el Calendario.
 */
@Composable
fun HorarioContenido(
    onAbrirClase: (Long) -> Unit,
    onNuevaClase: () -> Unit,
    onNuevaMateria: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HorarioViewModel = viewModel(factory = HorarioViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()

    Box(modifier.fillMaxSize()) {
        when {
            estado.cargando -> Unit
            !estado.hayMaterias -> EstadoVacio(
                mensaje = "Primero agrega una materia",
                detalle = "Cada clase pertenece a una materia.",
                accion = "Agregar materia",
                onAccion = onNuevaMateria,
            )
            estado.dias.isEmpty() -> EstadoVacio(
                mensaje = "Aún no tienes clases",
                accion = "Agregar clase",
                onAccion = onNuevaClase,
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
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
        if (estado.hayMaterias) {
            FloatingActionButton(
                onClick = onNuevaClase,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar clase")
            }
        }
    }
}
