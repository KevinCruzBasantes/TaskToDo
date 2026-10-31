package com.calendar.tasktodo.ui.calendario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.Situacion
import com.calendar.tasktodo.data.db.entidad.situacion
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import com.calendar.tasktodo.ui.dia.ElementoDia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Lunes de la semana de [fecha]: la semana empieza el lunes (diseño de interfaz §1). */
fun lunesDe(fecha: LocalDate): LocalDate = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Un día pasado con actividades sin check lleva una señal discreta (RF-37). No cuenta el gym,
 * que no tiene check: su "no realizada" se ve en el módulo del día.
 */
fun List<ActividadConMateria>.tieneSinCheck(hoy: LocalDate): Boolean =
    any { it.actividad.situacion(hoy) in setOf(Situacion.ATRASADA, Situacion.NO_MARCADA) }

data class DiaDeSemana(
    val fecha: LocalDate,
    val esHoy: Boolean,
    val senal: Boolean,
    /** Clases y actividades con hora, ordenadas por hora. */
    val conHora: List<ElementoDia>,
    val sinHora: List<ActividadConMateria>,
) {
    val vacio: Boolean get() = conHora.isEmpty() && sinHora.isEmpty()
}

data class SemanaUiState(
    val lunes: LocalDate,
    val dias: List<DiaDeSemana> = emptyList(),
    val cargando: Boolean = true,
    val contieneHoy: Boolean = false,
)

/** Vista de semana (RF-34, RF-36, RF-37). */
class SemanaViewModel(
    horario: HorarioRepository,
    actividades: ActividadRepository,
    private val reloj: Clock,
) : ViewModel() {

    private val lunes = MutableStateFlow(lunesDe(hoy()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<SemanaUiState> = lunes
        .flatMapLatest { inicio ->
            combine(horario.clases, actividades.entre(inicio, inicio.plusDays(6))) { clases, delRango ->
                val hoy = hoy()
                val dias = (0L..6L).map { inicio.plusDays(it) }.map { fecha ->
                    val delDia = delRango.filter { it.actividad.fecha == fecha }
                    val (conHora, sinHora) = delDia.partition { it.actividad.hora != null }
                    DiaDeSemana(
                        fecha = fecha,
                        esHoy = fecha == hoy,
                        senal = fecha.isBefore(hoy) && delDia.tieneSinCheck(hoy),
                        // Las clases se repiten cada semana por su día (RF-08)
                        conHora = (
                            clases.filter { it.clase.diaSemana == fecha.dayOfWeek }.map(ElementoDia::DeClase) +
                                conHora.map { ElementoDia.DeActividad(it, it.actividad.hora!!) }
                            ).sortedBy { it.hora },
                        sinHora = sinHora,
                    )
                }
                SemanaUiState(inicio, dias, cargando = false, contieneHoy = dias.any { it.esHoy })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SemanaUiState(lunes.value))

    fun anterior() = lunes.update { it.minusWeeks(1) }

    fun siguiente() = lunes.update { it.plusWeeks(1) }

    fun irAHoy() {
        lunes.value = lunesDe(hoy())
    }

    /** La semana que contiene [fecha], al venir del mes (RF-36). */
    fun irA(fecha: LocalDate) {
        lunes.value = lunesDe(fecha)
    }

    private fun hoy() = LocalDate.now(reloj)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                SemanaViewModel(c.horarioRepository, c.actividadRepository, c.reloj)
            }
        }
    }
}

data class CeldaMes(
    val fecha: LocalDate,
    /** Cuántas actividades tiene; la pantalla dibuja hasta tres puntos. */
    val actividades: Int,
    val senal: Boolean,
    val esHoy: Boolean,
)

data class MesUiState(
    val mes: YearMonth,
    /** Filas de lunes a domingo; nulo en los huecos antes del 1 y después del último día. */
    val semanas: List<List<CeldaMes?>> = emptyList(),
    val seleccionado: LocalDate? = null,
    val delSeleccionado: List<ActividadConMateria> = emptyList(),
    val hoy: LocalDate = LocalDate.MIN,
    val cargando: Boolean = true,
)

/** Vista de mes (RF-35, RF-36, RF-37). Las clases no se dibujan: llenarían todo el mes. */
class MesViewModel(
    actividades: ActividadRepository,
    private val reloj: Clock,
) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now(reloj))
    private val seleccionado = MutableStateFlow<LocalDate?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<MesUiState> = mes
        .flatMapLatest { actual ->
            combine(actividades.entre(actual.atDay(1), actual.atEndOfMonth()), seleccionado) { delMes, elegido ->
                val hoy = LocalDate.now(reloj)
                val porDia = delMes.groupBy { it.actividad.fecha }
                val celdas = (1..actual.lengthOfMonth()).map { dia ->
                    val fecha = actual.atDay(dia)
                    val lista = porDia[fecha].orEmpty()
                    CeldaMes(fecha, lista.size, fecha.isBefore(hoy) && lista.tieneSinCheck(hoy), fecha == hoy)
                }
                val huecosAntes = actual.atDay(1).dayOfWeek.value - 1
                val todas: List<CeldaMes?> = List(huecosAntes) { null } + celdas
                MesUiState(
                    mes = actual,
                    semanas = (todas + List((7 - todas.size % 7) % 7) { null }).chunked(7),
                    seleccionado = elegido,
                    delSeleccionado = elegido?.let { porDia[it] }.orEmpty(),
                    hoy = hoy,
                    cargando = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MesUiState(mes.value))

    fun anterior() {
        mes.update { it.minusMonths(1) }
        seleccionado.value = null
    }

    fun siguiente() {
        mes.update { it.plusMonths(1) }
        seleccionado.value = null
    }

    fun seleccionar(fecha: LocalDate) {
        seleccionado.value = fecha
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                MesViewModel(c.actividadRepository, c.reloj)
            }
        }
    }
}
