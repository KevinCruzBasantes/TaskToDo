package com.calendar.tasktodo.ui.navegacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class AppViewModel(repositorio: HorarioRepository) : ViewModel() {

    /** Sin materias abre la bienvenida (RF-01); con materias, el día actual (RF-18). Nulo mientras carga. */
    val destinoInicial: StateFlow<Any?> =
        flow { emit(if (repositorio.cantidadMaterias.first() == 0) Bienvenida else Dia()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        val Factory = viewModelFactory {
            initializer { AppViewModel(contenedor().horarioRepository) }
        }
    }
}
