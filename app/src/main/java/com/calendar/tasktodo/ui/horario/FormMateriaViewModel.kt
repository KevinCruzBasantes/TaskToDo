package com.calendar.tasktodo.ui.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.componentes.PaletaMaterias
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormMateriaUiState(
    val cargando: Boolean = true,
    val esEdicion: Boolean = false,
    val nombre: String = "",
    val color: Int = PaletaMaterias.colores.first().argb,
    val error: String? = null,
    /** Clases que se borrarán junto con la materia, para la confirmación. */
    val clasesDeLaMateria: Int = 0,
    val ocupado: Boolean = false,
    /** Se guardó o se eliminó: la pantalla vuelve atrás. */
    val terminado: Boolean = false,
)

/** Crear, editar y eliminar una materia (CU-02). */
class FormMateriaViewModel(
    private val repositorio: HorarioRepository,
    private val id: Long?,
) : ViewModel() {

    private val _estado = MutableStateFlow(FormMateriaUiState(esEdicion = id != null))
    val estado: StateFlow<FormMateriaUiState> = _estado.asStateFlow()

    private var original: Materia? = null

    init {
        viewModelScope.launch {
            if (id == null) {
                val usados = repositorio.materias.first().map { it.color }
                _estado.update { it.copy(cargando = false, color = PaletaMaterias.siguienteLibre(usados)) }
                return@launch
            }
            val materia = repositorio.obtenerMateria(id)
            if (materia == null) {
                _estado.update { it.copy(terminado = true) }
                return@launch
            }
            original = materia
            val clases = repositorio.contarClasesDeMateria(id)
            _estado.update {
                it.copy(cargando = false, nombre = materia.nombre, color = materia.color, clasesDeLaMateria = clases)
            }
        }
    }

    fun cambiarNombre(nombre: String) = _estado.update { it.copy(nombre = nombre, error = null) }

    fun cambiarColor(color: Int) = _estado.update { it.copy(color = color) }

    fun guardar() {
        val actual = _estado.value
        if (actual.ocupado) return
        val nombre = actual.nombre.trim()
        if (nombre.isEmpty()) {
            _estado.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        _estado.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            if (repositorio.existeMateria(nombre, excluirId = id ?: 0)) {
                _estado.update { it.copy(ocupado = false, error = "Ya existe una materia con ese nombre") }
                return@launch
            }
            repositorio.guardarMateria(Materia(id = id ?: 0, nombre = nombre, color = actual.color))
            _estado.update { it.copy(ocupado = false, terminado = true) }
        }
    }

    /** Borra la materia y sus clases; sus actividades quedarán sin materia (RF-03). */
    fun eliminar() {
        val materia = original ?: return
        viewModelScope.launch {
            repositorio.eliminarMateria(materia)
            _estado.update { it.copy(terminado = true) }
        }
    }

    companion object {
        fun factory(id: Long?) = viewModelFactory {
            initializer { FormMateriaViewModel(contenedor().horarioRepository, id) }
        }
    }
}
