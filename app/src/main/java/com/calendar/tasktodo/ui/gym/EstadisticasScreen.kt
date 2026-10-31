package com.calendar.tasktodo.ui.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.repositorio.Agrupacion
import com.calendar.tasktodo.data.repositorio.Estadisticas
import com.calendar.tasktodo.data.repositorio.EstadisticasRepository
import com.calendar.tasktodo.data.repositorio.Periodo
import com.calendar.tasktodo.ui.ESPANOL
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.contenedor
import com.calendar.tasktodo.ui.nombreCorto
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

data class EstadisticasUiState(
    val periodo: Periodo = Periodo.MES,
    val datos: Estadisticas? = null,
    val ejercicioId: Long? = null,
    val verImc: Boolean = false,
    val verVolumen: Boolean = false,
)

/** Estadísticas (CU-18): por defecto, el último mes. */
class EstadisticasViewModel(repositorio: EstadisticasRepository, reloj: Clock) : ViewModel() {

    private val hoy = LocalDate.now(reloj)
    private val periodo = MutableStateFlow(Periodo.MES)
    private val elegido = MutableStateFlow<Long?>(null)
    private val opciones = MutableStateFlow(false to false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<EstadisticasUiState> = combine(
        periodo.flatMapLatest { repositorio.estadisticas(it, hoy) },
        elegido,
        opciones,
    ) { datos, id, (verImc, verVolumen) ->
        EstadisticasUiState(
            periodo = datos.periodo,
            datos = datos,
            // Si el ejercicio elegido no tiene datos en este periodo, se muestra el primero que sí
            ejercicioId = id?.takeIf { e -> datos.ejercicios.any { it.id == e } } ?: datos.ejercicios.firstOrNull()?.id,
            verImc = verImc && datos.pesos.any { it.imc != null },
            verVolumen = verVolumen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadisticasUiState())

    fun cambiarPeriodo(p: Periodo) {
        periodo.value = p
    }

    fun elegirEjercicio(id: Long) {
        elegido.value = id
    }

    fun verImc(ver: Boolean) {
        opciones.value = ver to opciones.value.second
    }

    fun verVolumen(ver: Boolean) {
        opciones.value = opciones.value.first to ver
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                EstadisticasViewModel(c.estadisticasRepository, c.reloj)
            }
        }
    }
}

private val FORMATO_DIA_MES = DateTimeFormatter.ofPattern("d MMM", ESPANOL)
private val FORMATO_MES = DateTimeFormatter.ofPattern("MMM yy", ESPANOL)

private fun etiqueta(fecha: LocalDate, agrupacion: Agrupacion): String = when (agrupacion) {
    Agrupacion.DIA -> fecha.dayOfWeek.nombreCorto()
    Agrupacion.SEMANA -> fecha.format(FORMATO_DIA_MES).replace(".", "")
    Agrupacion.MES -> fecha.format(FORMATO_MES).replace(".", "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(onAtras: () -> Unit, viewModel: EstadisticasViewModel = viewModel(factory = EstadisticasViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
            )
        },
    ) { modifier ->
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Periodo.entries.forEach { p ->
                    FilterChip(estado.periodo == p, { viewModel.cambiarPeriodo(p) }, { Text(p.texto) })
                }
            }
            val datos = estado.datos ?: return@Column
            val agrupacion = datos.periodo.agrupacion

            TarjetaGrafica("Minutos entrenados", "${datos.minutosTotal} min", vacia = datos.minutosTotal == 0) {
                GraficaBarras(datos.barras.map { it.minutos }, datos.barras.map { etiqueta(it.inicio, agrupacion) })
            }

            val c = datos.cumplimiento
            TarjetaGrafica(
                "Sesiones",
                c.porcentaje?.let { "${c.hechas} de ${c.planificadas} · $it %" } ?: "",
                vacia = c.planificadas == 0,
            ) {
                LinearProgressIndicator(
                    progress = { c.hechas.toFloat() / c.planificadas },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                )
            }

            val ultimo = datos.pesos.lastOrNull()
            TarjetaGrafica(
                "Peso e IMC",
                when {
                    ultimo == null -> ""
                    estado.verImc -> "IMC ${textoImc(ultimo.imc ?: 0.0)}"
                    else -> "${textoPeso(ultimo.pesoKg)} kg"
                },
                vacia = datos.pesos.isEmpty(),
                opciones = {
                    if (datos.pesos.any { it.imc != null }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(!estado.verImc, { viewModel.verImc(false) }, { Text("Peso") })
                            FilterChip(estado.verImc, { viewModel.verImc(true) }, { Text("IMC") })
                        }
                    }
                },
            ) {
                val puntos = if (estado.verImc) datos.pesos.filter { it.imc != null } else datos.pesos
                GraficaLinea(
                    puntos.map { if (estado.verImc) it.imc!! else it.pesoKg },
                    puntos.map { it.fecha.format(FORMATO_DIA_MES).replace(".", "") },
                )
            }

            val serie = estado.ejercicioId?.let { datos.fuerza[it] }.orEmpty()
            TarjetaGrafica(
                "Fuerza",
                when {
                    serie.isEmpty() -> ""
                    estado.verVolumen -> "${textoPeso(serie.maxOf { it.volumen })} kg"
                    else -> "Máx. ${textoPeso(serie.maxOf { it.pesoMaximo })} kg"
                },
                vacia = serie.isEmpty(),
                opciones = {
                    if (datos.ejercicios.isNotEmpty()) {
                        SelectorEjercicio(datos.ejercicios.map { it.id to it.nombre }, estado.ejercicioId, viewModel::elegirEjercicio)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(!estado.verVolumen, { viewModel.verVolumen(false) }, { Text("Peso máximo") })
                            FilterChip(estado.verVolumen, { viewModel.verVolumen(true) }, { Text("Volumen") })
                        }
                    }
                },
            ) {
                GraficaLinea(
                    serie.map { if (estado.verVolumen) it.volumen else it.pesoMaximo },
                    serie.map { it.fecha.format(FORMATO_DIA_MES).replace(".", "") },
                )
            }
        }
    }
}

/** Título, número principal y la gráfica, o "Sin datos en este periodo" (CU-18 2a). */
@Composable
fun TarjetaGrafica(
    titulo: String,
    principal: String,
    vacia: Boolean,
    opciones: @Composable () -> Unit = {},
    grafica: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (!vacia) Text(principal, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            opciones()
            if (vacia) {
                Text("Sin datos en este periodo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                grafica()
            }
        }
    }
}

private fun formateador(etiquetas: List<String>) = CartesianValueFormatter { _, x, _ -> etiquetas.getOrNull(x.toInt()) ?: " " }

@Composable
private fun GraficaBarras(valores: List<Number>, etiquetas: List<String>) {
    val modelo = remember { CartesianChartModelProducer() }
    LaunchedEffect(valores) { modelo.runTransaction { columnSeries { series(valores) } } }
    CartesianChartHost(
        rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = remember(etiquetas) { formateador(etiquetas) }),
        ),
        modelo,
        Modifier
            .fillMaxWidth()
            .height(180.dp),
    )
}

/** Margen vertical para que una variación de 1 kg no se vea plana y un solo valor quede centrado. */
fun rangoVertical(valores: List<Double>): Pair<Double, Double> {
    val min = valores.min()
    val max = valores.max()
    val margen = maxOf((max - min) * 0.2, 1.0)
    return floor(min - margen).coerceAtLeast(0.0) to ceil(max + margen)
}

@Composable
private fun GraficaLinea(valores: List<Number>, etiquetas: List<String>) {
    val modelo = remember { CartesianChartModelProducer() }
    LaunchedEffect(valores) { if (valores.isNotEmpty()) modelo.runTransaction { lineSeries { series(valores) } } }
    val color = MaterialTheme.colorScheme.primary
    // Un punto en cada registro: con uno solo, la línea no se dibuja y la gráfica parecía vacía
    val linea = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(fill(color)),
        pointProvider = LineCartesianLayer.PointProvider.single(
            LineCartesianLayer.point(rememberShapeComponent(fill(color), CorneredShape.Pill), 8.dp),
        ),
    )
    val rango = remember(valores) {
        valores.takeIf { it.isNotEmpty() }?.let { v ->
            val (min, max) = rangoVertical(v.map { it.toDouble() })
            CartesianLayerRangeProvider.fixed(minY = min, maxY = max)
        } ?: CartesianLayerRangeProvider.auto()
    }
    CartesianChartHost(
        rememberCartesianChart(
            rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(linea), rangeProvider = rango),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = remember(etiquetas) { formateador(etiquetas) }),
        ),
        modelo,
        Modifier
            .fillMaxWidth()
            .height(180.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorEjercicio(ejercicios: List<Pair<Long, String>>, elegido: Long?, onElegir: (Long) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
            OutlinedTextField(
                value = ejercicios.firstOrNull { it.first == elegido }?.second.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Ejercicio") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                ejercicios.forEach { (id, nombre) ->
                    DropdownMenuItem(text = { Text(nombre) }, onClick = {
                        onElegir(id)
                        abierto = false
                    })
                }
            }
        }
    }
}
