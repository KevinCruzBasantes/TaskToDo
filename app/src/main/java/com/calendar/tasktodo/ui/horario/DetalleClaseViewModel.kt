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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleClaseUiState(
    val cargando: Boolean = true,
    /** Nula después de cargar significa que la clase ya no existe. */
    val clase: ClaseConMateria? = null,
)

/** Detalle de una clase (CU-05). Las próximas actividades de su materia llegan en el incremento 2. */
class DetalleClaseViewModel(
    private val repositorio: HorarioRepository,
    private val id: Long,
) : ViewModel() {

    val estado: StateFlow<DetalleClaseUiState> = repositorio.clase(id)
        .map { DetalleClaseUiState(cargando = false, clase = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleClaseUiState())

    fun eliminar() {
        viewModelScope.launch {
            repositorio.obtenerClase(id)?.let { repositorio.eliminarClase(it) }
        }
    }

    companion object {
        fun factory(id: Long) = viewModelFactory {
            initializer { DetalleClaseViewModel(contenedor().horarioRepository, id) }
        }
    }
}
