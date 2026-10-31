package com.calendar.tasktodo.ui.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

data class FormClaseUiState(
    val cargando: Boolean = true,
    val esEdicion: Boolean = false,
    val materias: List<Materia> = emptyList(),
    val materiaId: Long? = null,
    val dia: DayOfWeek = DayOfWeek.MONDAY,
    val horaInicio: LocalTime = LocalTime.of(7, 0),
    val horaFin: LocalTime = LocalTime.of(9, 0),
    val aula: String = "",
    val errorMateria: String? = null,
    val errorHoras: String? = null,
    /** Clases del mismo día que se solapan; si no está vacía, la pantalla pide confirmar (CU-04 3b). */
    val cruces: List<ClaseConMateria> = emptyList(),
    val ocupado: Boolean = false,
    val terminado: Boolean = false,
)

/** Crear y editar una clase (CU-04). */
class FormClaseViewModel(
    private val repositorio: HorarioRepository,
    private val id: Long?,
    materiaInicial: Long?,
) : ViewModel() {

    private val _estado = MutableStateFlow(FormClaseUiState(esEdicion = id != null, materiaId = materiaInicial))
    val estado: StateFlow<FormClaseUiState> = _estado.asStateFlow()

    init {
        // La lista se mantiene viva por si el usuario crea una materia desde aquí (CU-04 1a)
        viewModelScope.launch {
            repositorio.materias.collect { materias ->
                _estado.update { it.copy(materias = materias, materiaId = it.materiaId ?: materias.singleOrNull()?.id) }
            }
        }
        viewModelScope.launch {
            val clase = id?.let { repositorio.obtenerClase(it) }
            if (id != null && clase == null) {
                _estado.update { it.copy(terminado = true) }
                return@launch
            }
            _estado.update { actual ->
                if (clase == null) {
                    actual.copy(cargando = false)
                } else {
                    actual.copy(
                        cargando = false,
                        materiaId = clase.materiaId,
                        dia = clase.diaSemana,
                        horaInicio = clase.horaInicio,
                        horaFin = clase.horaFin,
                        aula = clase.aula.orEmpty(),
                    )
                }
            }
        }
    }

    fun cambiarMateria(materiaId: Long) = _estado.update { it.copy(materiaId = materiaId, errorMateria = null) }

    fun cambiarDia(dia: DayOfWeek) = _estado.update { it.copy(dia = dia) }

    fun cambiarInicio(hora: LocalTime) = _estado.update { it.copy(horaInicio = hora, errorHoras = null) }

    fun cambiarFin(hora: LocalTime) = _estado.update { it.copy(horaFin = hora, errorHoras = null) }

    fun cambiarAula(aula: String) = _estado.update { it.copy(aula = aula) }

    fun descartarCruces() = _estado.update { it.copy(cruces = emptyList()) }

    fun guardar(confirmarCruce: Boolean = false) {
        val actual = _estado.value
        if (actual.ocupado) return
        val errorMateria = if (actual.materiaId == null) "Elige una materia" else null
        val errorHoras = if (actual.horaFin.isAfter(actual.horaInicio)) null else "La hora de fin debe ser posterior a la de inicio"
        if (errorMateria != null || errorHoras != null) {
            _estado.update { it.copy(errorMateria = errorMateria, errorHoras = errorHoras) }
            return
        }
        _estado.update { it.copy(ocupado = true, cruces = emptyList()) }
        viewModelScope.launch {
            if (!confirmarCruce) {
                val cruces = repositorio.cruces(actual.dia, actual.horaInicio, actual.horaFin, excluirId = id ?: 0)
                if (cruces.isNotEmpty()) {
                    _estado.update { it.copy(ocupado = false, cruces = cruces) }
                    return@launch
                }
            }
            repositorio.guardarClase(
                Clase(
                    id = id ?: 0,
                    materiaId = requireNotNull(actual.materiaId),
                    diaSemana = actual.dia,
                    horaInicio = actual.horaInicio,
                    horaFin = actual.horaFin,
                    aula = actual.aula.trim().ifEmpty { null },
                ),
            )
            _estado.update { it.copy(ocupado = false, terminado = true) }
        }
    }

    companion object {
        fun factory(id: Long?, materiaId: Long?) = viewModelFactory {
            initializer { FormClaseViewModel(contenedor().horarioRepository, id, materiaId) }
        }
    }
}
