package com.calendar.tasktodo.ui.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class DetalleClaseUiState(
    val cargando: Boolean = true,
    /** Nula después de cargar significa que la clase ya no existe. */
    val clase: ClaseConMateria? = null,
    /** Las próximas pendientes de su materia (RF-16). */
    val proximas: List<ActividadConMateria> = emptyList(),
    val hoy: LocalDate = LocalDate.MIN,
)

/** Detalle de una clase (CU-05). */
class DetalleClaseViewModel(
    private val horario: HorarioRepository,
    private val actividades: ActividadRepository,
    reloj: Clock,
    private val id: Long,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<DetalleClaseUiState> = horario.clase(id)
        .flatMapLatest { clase ->
            val hoy = LocalDate.now(reloj)
            if (clase == null) {
                flowOf(DetalleClaseUiState(cargando = false, hoy = hoy))
            } else {
                actividades.proximasDeMateria(clase.clase.materiaId, hoy)
                    .map { DetalleClaseUiState(cargando = false, clase = clase, proximas = it, hoy = hoy) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleClaseUiState())

    fun cambiarCumplida(actividadId: Long, cumplida: Boolean) {
        viewModelScope.launch { actividades.cambiarCumplida(actividadId, cumplida) }
    }

    fun eliminar() {
        viewModelScope.launch {
            horario.obtenerClase(id)?.let { horario.eliminarClase(it) }
        }
    }

    companion object {
        fun factory(id: Long) = viewModelFactory {
            initializer {
                val c = contenedor()
                DetalleClaseViewModel(c.horarioRepository, c.actividadRepository, c.reloj, id)
            }
        }
    }
}
