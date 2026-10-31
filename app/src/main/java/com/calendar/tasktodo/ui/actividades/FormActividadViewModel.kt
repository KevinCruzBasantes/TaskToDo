package com.calendar.tasktodo.ui.actividades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.ajustes.AjustesAvisos
import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

data class FormActividadUiState(
    val cargando: Boolean = true,
    val esEdicion: Boolean = false,
    val titulo: String = "",
    val tipo: TipoActividad = TipoActividad.TAREA,
    val fecha: LocalDate = LocalDate.MIN,
    val hora: LocalTime? = null,
    val materiaId: Long? = null,
    val notas: String = "",
    val avisoMinutos: Int? = null,
    val materias: List<Materia> = emptyList(),
    /** Para explicar a qué hora llega el aviso sin hora y el del día anterior. */
    val ajustesAvisos: AjustesAvisos = AjustesAvisos(),
    val errorTitulo: String? = null,
    val ocupado: Boolean = false,
    val terminado: Boolean = false,
)

/** Crear, editar y eliminar una actividad (CU-06). Las de gym se generan desde su programación. */
class FormActividadViewModel(
    horario: HorarioRepository,
    private val actividades: ActividadRepository,
    ajustes: AjustesRepository,
    reloj: Clock,
    private val id: Long?,
    fechaInicial: LocalDate?,
) : ViewModel() {

    private val _estado = MutableStateFlow(
        // Desde el módulo del día, la fecha de ese día ya viene puesta (RF-21)
        FormActividadUiState(esEdicion = id != null, fecha = fechaInicial ?: LocalDate.now(reloj)),
    )
    val estado: StateFlow<FormActividadUiState> = _estado.asStateFlow()

    private var original: Actividad? = null

    init {
        viewModelScope.launch { horario.materias.collect { lista -> _estado.update { it.copy(materias = lista) } } }
        viewModelScope.launch { ajustes.avisos.collect { a -> _estado.update { it.copy(ajustesAvisos = a) } } }
        viewModelScope.launch {
            if (id == null) {
                _estado.update { it.copy(cargando = false) }
                return@launch
            }
            val actividad = actividades.obtener(id)
            if (actividad == null) {
                _estado.update { it.copy(terminado = true) }
                return@launch
            }
            original = actividad
            _estado.update {
                it.copy(
                    cargando = false,
                    titulo = actividad.titulo,
                    tipo = actividad.tipo,
                    fecha = actividad.fecha,
                    hora = actividad.hora,
                    materiaId = actividad.materiaId,
                    notas = actividad.notas.orEmpty(),
                    avisoMinutos = actividad.avisoMinutosAntes,
                )
            }
        }
    }

    fun cambiarTitulo(titulo: String) = _estado.update { it.copy(titulo = titulo, errorTitulo = null) }

    fun cambiarTipo(tipo: TipoActividad) = _estado.update { it.copy(tipo = tipo) }

    fun cambiarFecha(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha) }

    fun cambiarHora(hora: LocalTime?) = _estado.update { it.copy(hora = hora) }

    fun cambiarMateria(materiaId: Long?) = _estado.update { it.copy(materiaId = materiaId) }

    fun cambiarNotas(notas: String) = _estado.update { it.copy(notas = notas) }

    fun cambiarAviso(minutos: Int?) = _estado.update { it.copy(avisoMinutos = minutos) }

    fun guardar() {
        val actual = _estado.value
        if (actual.ocupado) return
        val titulo = actual.titulo.trim()
        if (titulo.isEmpty()) {
            _estado.update { it.copy(errorTitulo = "El título es obligatorio") }
            return
        }
        _estado.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            actividades.guardar(
                Actividad(
                    id = id ?: 0,
                    titulo = titulo,
                    tipo = actual.tipo,
                    fecha = actual.fecha,
                    hora = actual.hora,
                    materiaId = actual.materiaId,
                    notas = actual.notas.trim().ifEmpty { null },
                    avisoMinutosAntes = actual.avisoMinutos,
                    // Editar no cambia si estaba cumplida
                    estado = original?.estado ?: EstadoActividad.PENDIENTE,
                ),
            )
            _estado.update { it.copy(ocupado = false, terminado = true) }
        }
    }

    fun eliminar() {
        val actividad = original ?: return
        viewModelScope.launch {
            actividades.eliminar(actividad)
            _estado.update { it.copy(terminado = true) }
        }
    }

    companion object {
        fun factory(id: Long?, fecha: LocalDate?) = viewModelFactory {
            initializer {
                val c = contenedor()
                FormActividadViewModel(c.horarioRepository, c.actividadRepository, c.ajustesRepository, c.reloj, id, fecha)
            }
        }
    }
}
