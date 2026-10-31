package com.calendar.tasktodo.ui.dia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/** Lo que va en la línea de tiempo del día, ordenado por hora (RF-17). */
sealed interface ElementoDia {
    val hora: LocalTime

    data class DeClase(val clase: ClaseConMateria) : ElementoDia {
        override val hora: LocalTime get() = clase.clase.horaInicio
    }

    data class DeActividad(val actividad: ActividadConMateria, override val hora: LocalTime) : ElementoDia
}

data class DiaUiState(
    val fecha: LocalDate,
    val hoy: LocalDate,
    /** Solo en el día actual (RF-14). */
    val atrasadas: List<ActividadConMateria> = emptyList(),
    val conHora: List<ElementoDia> = emptyList(),
    val sinHora: List<ActividadConMateria> = emptyList(),
    val cargando: Boolean = true,
) {
    val esHoy: Boolean get() = fecha == hoy
    val vacio: Boolean get() = atrasadas.isEmpty() && conHora.isEmpty() && sinHora.isEmpty()
}

/** Módulo del día (CU-08, CU-09, CU-10). */
class DiaViewModel(
    private val horario: HorarioRepository,
    private val actividades: ActividadRepository,
    private val reloj: Clock,
    fechaInicial: LocalDate? = null,
) : ViewModel() {

    private val fecha = MutableStateFlow(fechaInicial ?: hoy())

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<DiaUiState> = fecha
        .flatMapLatest { dia ->
            val hoy = hoy()
            combine(
                // Las clases no se guardan por fecha: se calculan por su día de la semana (RF-08)
                horario.clasesDelDia(dia.dayOfWeek),
                actividades.delDia(dia),
                if (dia == hoy) actividades.atrasadas(hoy) else flowOf(emptyList()),
            ) { clases, delDia, atrasadas ->
                val (conHora, sinHora) = delDia.partition { it.actividad.hora != null }
                DiaUiState(
                    fecha = dia,
                    hoy = hoy,
                    atrasadas = atrasadas,
                    conHora = (clases.map(ElementoDia::DeClase) + conHora.map { ElementoDia.DeActividad(it, it.actividad.hora!!) })
                        .sortedBy { it.hora },
                    sinHora = sinHora,
                    cargando = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaUiState(fecha.value, hoy()))

    fun anterior() = fecha.update { it.minusDays(1) }

    fun siguiente() = fecha.update { it.plusDays(1) }

    fun irA(dia: LocalDate) {
        fecha.value = dia
    }

    fun irAHoy() {
        fecha.value = hoy()
    }

    fun cambiarCumplida(id: Long, cumplida: Boolean) {
        viewModelScope.launch { actividades.cambiarCumplida(id, cumplida) }
    }

    /** Solo hoy o después; el selector de la pantalla ya no ofrece días pasados (CU-09). */
    fun reprogramar(id: Long, nuevaFecha: LocalDate) {
        val hoy = hoy()
        if (nuevaFecha.isBefore(hoy)) return
        viewModelScope.launch { actividades.reprogramar(id, nuevaFecha, hoy) }
    }

    fun eliminar(actividad: Actividad) {
        viewModelScope.launch { actividades.eliminar(actividad) }
    }

    private fun hoy(): LocalDate = LocalDate.now(reloj)

    companion object {
        fun factory(fechaInicial: LocalDate?) = viewModelFactory {
            initializer {
                val contenedor = contenedor()
                DiaViewModel(contenedor.horarioRepository, contenedor.actividadRepository, contenedor.reloj, fechaInicial)
            }
        }
    }
}
