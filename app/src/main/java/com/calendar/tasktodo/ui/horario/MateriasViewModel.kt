package com.calendar.tasktodo.ui.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.MateriaConConteo
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MateriasUiState(
    val cargando: Boolean = true,
    val materias: List<MateriaConConteo> = emptyList(),
) {
    val totalClases: Int get() = materias.sumOf { it.clases }
}

/** Lista de materias y "Eliminar todas las materias" (CU-02, CU-03). */
class MateriasViewModel(private val repositorio: HorarioRepository) : ViewModel() {

    val estado: StateFlow<MateriasUiState> = repositorio.materiasConConteo
        .map { MateriasUiState(cargando = false, materias = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MateriasUiState())

    /** El respaldo previo de CU-03 llega con el incremento 3. */
    fun eliminarTodas() {
        viewModelScope.launch { repositorio.eliminarTodasLasMaterias() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MateriasViewModel(contenedor().horarioRepository) }
        }
    }
}
