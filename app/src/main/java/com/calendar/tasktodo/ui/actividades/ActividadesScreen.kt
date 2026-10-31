package com.calendar.tasktodo.ui.actividades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.ui.componentes.EstadoVacio
import com.calendar.tasktodo.ui.componentes.FilaActividad
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.nombre
import com.calendar.tasktodo.ui.rememberFormatoHora
import com.calendar.tasktodo.ui.textoCorto

/** Lista de actividades con filtros (CU-07). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActividadesScreen(
    onAbrirActividad: (Long) -> Unit,
    onNuevaActividad: () -> Unit,
    viewModel: ActividadesViewModel = viewModel(factory = ActividadesViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()

    Pantalla(
        enBarra = true,
        topBar = { TopAppBar(title = { Text("Actividades") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaActividad) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar actividad")
            }
        },
    ) { modifier ->
        if (estado.cargando) return@Pantalla
        if (!estado.hayActividades) {
            EstadoVacio("No tienes actividades", accion = "Agregar actividad", onAccion = onNuevaActividad, modifier = modifier)
            return@Pantalla
        }
        Column(modifier.fillMaxSize()) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroDesplegable(
                    texto = estado.filtros.tipo?.nombre() ?: "Todos los tipos",
                    activo = estado.filtros.tipo != null,
                    opciones = listOf<Pair<String, () -> Unit>>("Todos los tipos" to { viewModel.filtrarTipo(null) }) +
                        TipoActividad.entries.filter { it != TipoActividad.GYM }.map { tipo -> tipo.nombre() to { viewModel.filtrarTipo(tipo) } },
                )
                FiltroDesplegable(
                    texto = estado.materias.firstOrNull { it.id == estado.filtros.materiaId }?.nombre ?: "Todas las materias",
                    activo = estado.filtros.materiaId != null,
                    opciones = listOf<Pair<String, () -> Unit>>("Todas las materias" to { viewModel.filtrarMateria(null) }) +
                        estado.materias.map { materia -> materia.nombre to { viewModel.filtrarMateria(materia.id) } },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = estado.filtros.verCumplidas, onCheckedChange = viewModel::cambiarVerCumplidas)
                Text("Ver cumplidas")
            }
            if (estado.proximas.isEmpty() && estado.pasadas.isEmpty()) {
                EstadoVacio(
                    "Ninguna actividad coincide",
                    accion = if (estado.filtros.activos) "Quitar filtros" else null,
                    onAccion = viewModel::quitarFiltros,
                )
                return@Column
            }
            val fila: @Composable (ActividadConMateria) -> Unit = { item ->
                FilaActividad(
                    item, estado.hoy, formatoHora,
                    onCambiarCumplida = { viewModel.cambiarCumplida(item.actividad.id, it) },
                    onClick = { onAbrirActividad(item.actividad.id) },
                    detalleExtra = item.actividad.fecha.textoCorto(),
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                seccion("PRÓXIMAS", estado.proximas, fila)
                seccion("PASADAS", estado.pasadas, fila)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.seccion(
    titulo: String,
    lista: List<ActividadConMateria>,
    fila: @Composable (ActividadConMateria) -> Unit,
) {
    if (lista.isEmpty()) return
    item(key = titulo) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
    items(lista, key = { "$titulo${it.actividad.id}" }) { fila(it) }
}

@Composable
private fun FiltroDesplegable(texto: String, activo: Boolean, opciones: List<Pair<String, () -> Unit>>) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = activo,
            onClick = { abierto = true },
            label = { Text(texto) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (nombre, accion) ->
                DropdownMenuItem(text = { Text(nombre) }, onClick = {
                    abierto = false
                    accion()
                })
            }
        }
    }
}
