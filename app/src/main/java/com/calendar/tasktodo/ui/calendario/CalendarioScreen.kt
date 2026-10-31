package com.calendar.tasktodo.ui.calendario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.ui.ESPANOL
import com.calendar.tasktodo.ui.componentes.FranjaMateria
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.dia.ElementoDia
import com.calendar.tasktodo.ui.horario.HorarioContenido
import com.calendar.tasktodo.ui.icono
import com.calendar.tasktodo.ui.nombreCorto
import com.calendar.tasktodo.ui.rememberFormatoHora
import com.calendar.tasktodo.ui.textoLargo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private enum class Pestana(val titulo: String) { SEMANA("Semana"), MES("Mes"), HORARIO("Horario") }

/** Calendario con sus pestañas Semana, Mes y Horario; recuerda la última usada (diseño §2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen(
    onAbrirDia: (LocalDate) -> Unit,
    onAbrirClase: (Long) -> Unit,
    onNuevaClase: () -> Unit,
    onNuevaMateria: () -> Unit,
    onMaterias: () -> Unit,
    semana: SemanaViewModel = viewModel(factory = SemanaViewModel.Factory),
    mes: MesViewModel = viewModel(factory = MesViewModel.Factory),
) {
    var pestana by rememberSaveable { mutableIntStateOf(Pestana.SEMANA.ordinal) }

    Pantalla(
        enBarra = true,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Calendario") },
                    actions = { TextButton(onClick = onMaterias) { Text("Materias") } },
                )
                PrimaryTabRow(selectedTabIndex = pestana) {
                    Pestana.entries.forEach { p ->
                        Tab(selected = pestana == p.ordinal, onClick = { pestana = p.ordinal }, text = { Text(p.titulo) })
                    }
                }
            }
        },
    ) { modifier ->
        when (Pestana.entries[pestana]) {
            Pestana.SEMANA -> SemanaContenido(semana, onAbrirDia, modifier)
            Pestana.MES -> MesContenido(
                mes,
                onVerSemana = { fecha ->
                    semana.irA(fecha)
                    pestana = Pestana.SEMANA.ordinal
                },
                modifier = modifier,
            )
            Pestana.HORARIO -> HorarioContenido(onAbrirClase, onNuevaClase, onNuevaMateria, modifier)
        }
    }
}

private val FORMATO_DIA_MES = DateTimeFormatter.ofPattern("d MMM", ESPANOL)

@Composable
private fun Cabecera(texto: String, onAnterior: () -> Unit, onSiguiente: () -> Unit, extra: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onAnterior) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior") }
        Text(texto, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        extra()
        IconButton(onClick = onSiguiente) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente") }
    }
}

/** Vista de semana (RF-34): cada día con sus clases y actividades; al tocarlo se abre su módulo (RF-36). */
@Composable
fun SemanaContenido(viewModel: SemanaViewModel, onAbrirDia: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    val domingo = estado.lunes.plusDays(6)
    Column(modifier.fillMaxSize()) {
        Cabecera(
            "${estado.lunes.format(FORMATO_DIA_MES)} – ${domingo.format(FORMATO_DIA_MES)} ${domingo.year}".replace(".", ""),
            viewModel::anterior,
            viewModel::siguiente,
        ) {
            if (!estado.contieneHoy) TextButton(onClick = viewModel::irAHoy) { Text("Hoy") }
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(estado.dias, key = { it.fecha.toString() }) { dia ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onAbrirDia(dia.fecha) }
                        .semantics { contentDescription = "Abrir ${dia.fecha.textoLargo()}" }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${dia.fecha.dayOfWeek.nombreCorto().uppercase(ESPANOL)} ${dia.fecha.dayOfMonth}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (dia.esHoy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (dia.senal) SenalSinCheck(Modifier.padding(start = 6.dp))
                        if (dia.esHoy) {
                            Text("HOY", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (dia.vacio) {
                        Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    dia.conHora.forEach { elemento ->
                        when (elemento) {
                            is ElementoDia.DeClase -> LineaSemana(
                                elemento.hora.format(formatoHora), elemento.clase.materiaNombre, elemento.clase.materiaColor,
                            )
                            is ElementoDia.DeActividad -> LineaActividad(elemento.actividad, elemento.hora.format(formatoHora))
                        }
                    }
                    dia.sinHora.forEach { LineaActividad(it, "Sin hora") }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun LineaActividad(item: ActividadConMateria, hora: String) =
    LineaSemana(hora, "${item.actividad.tipo.icono()} ${item.actividad.titulo}", item.materiaColor)

@Composable
private fun LineaSemana(hora: String, texto: String, color: Int?) {
    Row(Modifier.padding(top = 4.dp).height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Text(hora, Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        color?.let { FranjaMateria(it) }
        Text(texto, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Señal discreta de día pasado con actividades sin check (RF-37). */
@Composable
private fun SenalSinCheck(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
            .semantics { contentDescription = "Con actividades sin marcar" },
    )
}

/** Vista de mes (RF-35): puntos por actividad; tocar un día muestra su lista y "Ver semana" (RF-36). */
@Composable
fun MesContenido(viewModel: MesViewModel, onVerSemana: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Cabecera(
            estado.mes.month.getDisplayName(TextStyle.FULL_STANDALONE, ESPANOL).replaceFirstChar { it.titlecase(ESPANOL) } +
                " ${estado.mes.year}",
            viewModel::anterior,
            viewModel::siguiente,
        )
        Row(Modifier.padding(horizontal = 8.dp)) {
            DayOfWeek.entries.forEach { dia ->
                Text(
                    dia.getDisplayName(TextStyle.NARROW, ESPANOL).uppercase(ESPANOL),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        estado.semanas.forEach { semana ->
            Row(Modifier.padding(horizontal = 8.dp)) {
                semana.forEach { celda ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (celda != null) {
                            CeldaDia(
                                celda = celda,
                                elegida = celda.fecha == estado.seleccionado,
                                onClick = {
                                    // Segundo toque sobre el mismo día: abre su semana
                                    if (celda.fecha == estado.seleccionado) onVerSemana(celda.fecha) else viewModel.seleccionar(celda.fecha)
                                },
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
        estado.seleccionado?.let { fecha ->
            Column(Modifier.padding(16.dp)) {
                Text(fecha.textoLargo(), style = MaterialTheme.typography.titleMedium)
                if (estado.delSeleccionado.isEmpty()) {
                    Text("Sin actividades", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                estado.delSeleccionado.forEach { item ->
                    LineaActividad(item, item.actividad.hora?.format(formatoHora) ?: "Sin hora")
                }
                OutlinedButton(onClick = { onVerSemana(fecha) }, modifier = Modifier.padding(top = 12.dp)) { Text("Ver semana") }
            }
        }
    }
}

@Composable
private fun CeldaDia(celda: CeldaMes, elegida: Boolean, onClick: () -> Unit) {
    val fondo = when {
        elegida -> MaterialTheme.colorScheme.primaryContainer
        celda.esHoy -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(fondo)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${celda.fecha.textoLargo()}, ${celda.actividades} actividades" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${celda.fecha.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (celda.esHoy) FontWeight.Bold else FontWeight.Normal,
            )
            if (celda.senal) SenalSinCheck(Modifier.padding(start = 2.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(8.dp)) {
            repeat(minOf(celda.actividades, 3)) {
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
