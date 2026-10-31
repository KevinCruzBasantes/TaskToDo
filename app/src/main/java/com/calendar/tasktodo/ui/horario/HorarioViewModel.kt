package com.calendar.tasktodo.ui.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek

data class HorarioUiState(
    val cargando: Boolean = true,
    val hayMaterias: Boolean = false,
    /** Solo los días con clases, de lunes a domingo, cada uno ordenado por hora. */
    val dias: List<Pair<DayOfWeek, List<ClaseConMateria>>> = emptyList(),
)

/** Horario semanal (CU-05). */
class HorarioViewModel(repositorio: HorarioRepository) : ViewModel() {

    val estado: StateFlow<HorarioUiState> =
        combine(repositorio.cantidadMaterias, repositorio.clases) { materias, clases ->
            HorarioUiState(
                cargando = false,
                hayMaterias = materias > 0,
                dias = DayOfWeek.entries.mapNotNull { dia ->
                    clases.filter { it.clase.diaSemana == dia }
                        .sortedBy { it.clase.horaInicio }
                        .takeIf { it.isNotEmpty() }
                        ?.let { dia to it }
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HorarioUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer { HorarioViewModel(contenedor().horarioRepository) }
        }
    }
}
