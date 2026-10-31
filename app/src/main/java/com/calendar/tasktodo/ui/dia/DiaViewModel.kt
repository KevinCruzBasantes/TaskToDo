package com.calendar.tasktodo.ui.dia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate

data class DiaUiState(
    val fecha: LocalDate,
    val esHoy: Boolean,
    val clases: List<ClaseConMateria> = emptyList(),
    val cargando: Boolean = true,
)

/** Módulo del día (CU-10). En el incremento 1 muestra solo las clases. */
class DiaViewModel(
    private val repositorio: HorarioRepository,
    private val reloj: Clock,
    fechaInicial: LocalDate? = null,
) : ViewModel() {

    private val fecha = MutableStateFlow(fechaInicial ?: hoy())

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<DiaUiState> = fecha
        .flatMapLatest { dia ->
            // Las clases no se guardan por fecha: se calculan por su día de la semana (RF-08)
            repositorio.clasesDelDia(dia.dayOfWeek).map { clases ->
                DiaUiState(dia, dia == hoy(), clases.sortedBy { it.clase.horaInicio }, cargando = false)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DiaUiState(fecha.value, fecha.value == hoy()),
        )

    fun anterior() = fecha.update { it.minusDays(1) }

    fun siguiente() = fecha.update { it.plusDays(1) }

    fun irA(dia: LocalDate) {
        fecha.value = dia
    }

    fun irAHoy() {
        fecha.value = hoy()
    }

    private fun hoy(): LocalDate = LocalDate.now(reloj)

    companion object {
        fun factory(fechaInicial: LocalDate?) = viewModelFactory {
            initializer {
                val contenedor = contenedor()
                DiaViewModel(contenedor.horarioRepository, contenedor.reloj, fechaInicial)
            }
        }
    }
}
