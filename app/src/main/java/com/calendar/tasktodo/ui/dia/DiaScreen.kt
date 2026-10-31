package com.calendar.tasktodo.ui.dia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.ui.componentes.DialogoConfirmar
import com.calendar.tasktodo.ui.componentes.DialogoFecha
import com.calendar.tasktodo.ui.componentes.EstadoVacio
import com.calendar.tasktodo.ui.componentes.FilaActividad
import com.calendar.tasktodo.ui.componentes.FilaClase
import com.calendar.tasktodo.ui.componentes.OpcionMenu
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.rememberFormatoHora
import com.calendar.tasktodo.ui.textoLargo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Módulo del día (CU-08, CU-09, CU-10). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaScreen(
    fechaInicial: LocalDate?,
    onAbrirClase: (Long) -> Unit,
    onAbrirActividad: (Long) -> Unit,
    onNuevaActividad: (LocalDate) -> Unit,
    /** Al abrirse desde la semana (RF-36): flecha para volver y sin barra inferior. */
    onAtras: (() -> Unit)? = null,
    viewModel: DiaViewModel = viewModel(factory = DiaViewModel.factory(fechaInicial)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    var eligiendoFecha by rememberSaveable { mutableStateOf(false) }
    var reprogramando by rememberSaveable { mutableStateOf<Long?>(null) }
    var borrando by rememberSaveable { mutableStateOf<Long?>(null) }

    Pantalla(
        enBarra = onAtras == null,
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
                    if (onAtras != null) {
                        IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                    } else {
                        IconButton(onClick = viewModel::anterior) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior")
                        }
                    }
                },
                actions = {
                    if (onAtras != null) {
                        IconButton(onClick = viewModel::anterior) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior")
                        }
                    }
                    if (!estado.esHoy && onAtras == null) TextButton(onClick = viewModel::irAHoy) { Text("Hoy") }
                    IconButton(onClick = { eligiendoFecha = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Elegir fecha")
                    }
                    IconButton(onClick = viewModel::siguiente) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Día siguiente")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNuevaActividad(estado.fecha) }) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar actividad")
            }
        },
    ) { modifier ->
        when {
            estado.cargando -> Unit
            estado.vacio -> EstadoVacio(
                mensaje = "No hay nada para este día",
                accion = "Agregar actividad",
                onAccion = { onNuevaActividad(estado.fecha) },
                modifier = modifier,
            )
            else -> LazyColumn(modifier.fillMaxSize()) {
                if (estado.atrasadas.isNotEmpty()) {
                    seccionAtrasadas(
                        atrasadas = estado.atrasadas,
                        hoy = estado.hoy,
                        filaActividad = { item, extra, menu ->
                            FilaActividad(
                                item, estado.hoy, formatoHora,
                                onCambiarCumplida = { viewModel.cambiarCumplida(item.actividad.id, it) },
                                onClick = { onAbrirActividad(item.actividad.id) },
                                detalleExtra = extra,
                                menu = menu,
                            )
                        },
                        onReprogramar = { reprogramando = it },
                        onBorrar = { borrando = it },
                    )
                }
                items(estado.conHora, key = { it.clave() }) { elemento ->
                    when (elemento) {
                        is ElementoDia.DeClase -> FilaClase(elemento.clase, formatoHora, onClick = { onAbrirClase(elemento.clase.clase.id) })
                        is ElementoDia.DeActividad -> FilaActividad(
                            elemento.actividad, estado.hoy, formatoHora,
                            onCambiarCumplida = { viewModel.cambiarCumplida(elemento.actividad.actividad.id, it) },
                            onClick = { onAbrirActividad(elemento.actividad.actividad.id) },
                        )
                    }
                }
                if (estado.sinHora.isNotEmpty()) {
                    item(key = "sin-hora") { Encabezado("SIN HORA") }
                    items(estado.sinHora, key = { "a${it.actividad.id}" }) { item ->
                        FilaActividad(
                            item, estado.hoy, formatoHora,
                            onCambiarCumplida = { viewModel.cambiarCumplida(item.actividad.id, it) },
                            onClick = { onAbrirActividad(item.actividad.id) },
                        )
                    }
                }
            }
        }
    }

    if (eligiendoFecha) {
        DialogoFecha(
            inicial = estado.fecha,
            onElegir = {
                viewModel.irA(it)
                eligiendoFecha = false
            },
            onCancelar = { eligiendoFecha = false },
        )
    }
    reprogramando?.let { id ->
        DialogoFecha(
            inicial = estado.hoy,
            desde = estado.hoy,
            onElegir = {
                viewModel.reprogramar(id, it)
                reprogramando = null
            },
            onCancelar = { reprogramando = null },
        )
    }
    borrando?.let { id ->
        val actividad = estado.atrasadas.firstOrNull { it.actividad.id == id }?.actividad
        if (actividad == null) {
            borrando = null
        } else {
            DialogoConfirmar(
                titulo = "¿Borrar ${actividad.titulo}?",
                texto = "Esta tarea se eliminará.",
                confirmar = "Borrar",
                onConfirmar = {
                    viewModel.eliminar(actividad)
                    borrando = null
                },
                onCancelar = { borrando = null },
            )
        }
    }
}

private fun ElementoDia.clave(): String = when (this) {
    is ElementoDia.DeClase -> "c${clase.clase.id}"
    is ElementoDia.DeActividad -> "a${actividad.actividad.id}"
}

/** Sección destacada de tareas atrasadas, con los días de atraso (RF-14). */
private fun LazyListScope.seccionAtrasadas(
    atrasadas: List<ActividadConMateria>,
    hoy: LocalDate,
    filaActividad: @Composable (ActividadConMateria, String, List<OpcionMenu>) -> Unit,
    onReprogramar: (Long) -> Unit,
    onBorrar: (Long) -> Unit,
) {
    item(key = "atrasadas") {
        Text(
            "⚠ ATRASADAS (${atrasadas.size})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    items(atrasadas, key = { "atr${it.actividad.id}" }) { item ->
        val dias = ChronoUnit.DAYS.between(item.actividad.fecha, hoy)
        filaActividad(
            item,
            if (dias == 1L) "hace 1 día" else "hace $dias días",
            listOf(
                OpcionMenu("Reprogramar") { onReprogramar(item.actividad.id) },
                OpcionMenu("Borrar") { onBorrar(item.actividad.id) },
            ),
        )
    }
    item(key = "fin-atrasadas") { HorizontalDivider() }
}

@Composable
private fun Encabezado(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
