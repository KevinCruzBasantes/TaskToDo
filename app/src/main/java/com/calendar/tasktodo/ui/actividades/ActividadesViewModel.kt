package com.calendar.tasktodo.ui.actividades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class FiltrosActividades(
    val tipo: TipoActividad? = null,
    val materiaId: Long? = null,
    /** Las cumplidas se ocultan por defecto para que la lista no crezca sin fin (diseño §3.4). */
    val verCumplidas: Boolean = false,
) {
    val activos: Boolean get() = tipo != null || materiaId != null
}

data class ActividadesUiState(
    val cargando: Boolean = true,
    val hoy: LocalDate = LocalDate.MIN,
    val filtros: FiltrosActividades = FiltrosActividades(),
    val materias: List<Materia> = emptyList(),
    val proximas: List<ActividadConMateria> = emptyList(),
    /** De la más reciente a la más antigua. */
    val pasadas: List<ActividadConMateria> = emptyList(),
    val hayActividades: Boolean = false,
)

/** Consultar y filtrar actividades (CU-07). */
class ActividadesViewModel(
    horario: HorarioRepository,
    private val actividades: ActividadRepository,
    private val reloj: Clock,
) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosActividades())

    val estado: StateFlow<ActividadesUiState> =
        combine(actividades.todas, horario.materias, filtros) { todas, materias, filtro ->
            val hoy = LocalDate.now(reloj)
            val visibles = todas.filter { item ->
                val a = item.actividad
                (filtro.tipo == null || a.tipo == filtro.tipo) &&
                    (filtro.materiaId == null || a.materiaId == filtro.materiaId) &&
                    (filtro.verCumplidas || a.estado != EstadoActividad.CUMPLIDA)
            }
            ActividadesUiState(
                cargando = false,
                hoy = hoy,
                filtros = filtro,
                materias = materias,
                proximas = visibles.filter { !it.actividad.fecha.isBefore(hoy) },
                pasadas = visibles.filter { it.actividad.fecha.isBefore(hoy) }.sortedByDescending { it.actividad.fecha },
                hayActividades = todas.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActividadesUiState())

    fun filtrarTipo(tipo: TipoActividad?) = filtros.update { it.copy(tipo = tipo) }

    fun filtrarMateria(materiaId: Long?) = filtros.update { it.copy(materiaId = materiaId) }

    fun cambiarVerCumplidas(ver: Boolean) = filtros.update { it.copy(verCumplidas = ver) }

    fun quitarFiltros() = filtros.update { it.copy(tipo = null, materiaId = null) }

    fun cambiarCumplida(id: Long, cumplida: Boolean) {
        viewModelScope.launch { actividades.cambiarCumplida(id, cumplida) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val contenedor = contenedor()
                ActividadesViewModel(contenedor.horarioRepository, contenedor.actividadRepository, contenedor.reloj)
            }
        }
    }
}
