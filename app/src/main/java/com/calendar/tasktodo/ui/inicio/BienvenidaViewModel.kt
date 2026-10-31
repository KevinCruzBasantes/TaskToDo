package com.calendar.tasktodo.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.MateriaConConteo
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Configuración inicial (CU-01). */
class BienvenidaViewModel(repositorio: HorarioRepository) : ViewModel() {

    val materias: StateFlow<List<MateriaConConteo>> = repositorio.materiasConConteo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { BienvenidaViewModel(contenedor().horarioRepository) }
        }
    }
}
