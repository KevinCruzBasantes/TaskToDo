package com.calendar.tasktodo.ui.horario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.MateriaConConteo
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.data.respaldo.RespaldoRepository
import com.calendar.tasktodo.data.respaldo.ResultadoRespaldo
import com.calendar.tasktodo.data.respaldo.TipoRespaldo
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MateriasUiState(
    val cargando: Boolean = true,
    val materias: List<MateriaConConteo> = emptyList(),
    /** La pantalla debe abrir el selector de carpetas para respaldar antes de borrar. */
    val pedirCarpeta: Boolean = false,
    /** El respaldo previo falló: se pregunta si se borra igual (CU-03 3b). */
    val respaldoFallido: Boolean = false,
    val mensaje: String? = null,
) {
    val totalClases: Int get() = materias.sumOf { it.clases }
}

/** Lista de materias y "Eliminar todas las materias" (CU-02, CU-03). */
class MateriasViewModel(
    private val repositorio: HorarioRepository,
    private val respaldo: RespaldoRepository? = null,
) : ViewModel() {

    private val extra = MutableStateFlow(MateriasUiState())

    val estado: StateFlow<MateriasUiState> = combine(repositorio.materiasConConteo, extra) { materias, e ->
        e.copy(cargando = false, materias = materias)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MateriasUiState())

    fun eliminarTodas() {
        viewModelScope.launch { repositorio.eliminarTodasLasMaterias() }
    }

    /** Respalda y, si sale bien, borra todo (CU-03 2 a 6). */
    fun respaldarYEliminar() {
        val respaldo = respaldo ?: return eliminarTodas()
        viewModelScope.launch {
            when (respaldo.respaldar(TipoRespaldo.MANUAL)) {
                is ResultadoRespaldo.Exito -> {
                    repositorio.eliminarTodasLasMaterias()
                    extra.update { it.copy(mensaje = "Respaldo creado y materias eliminadas") }
                }
                ResultadoRespaldo.SinCarpeta -> extra.update { it.copy(pedirCarpeta = true) }
                is ResultadoRespaldo.Error -> extra.update { it.copy(respaldoFallido = true) }
            }
        }
    }

    fun carpetaPedida() = extra.update { it.copy(pedirCarpeta = false) }

    fun registrarCarpetaYSeguir(uri: String) {
        viewModelScope.launch {
            respaldo?.registrarCarpeta(uri)
            respaldarYEliminar()
        }
    }

    fun cerrarFallo() = extra.update { it.copy(respaldoFallido = false) }

    fun mensajeMostrado() = extra.update { it.copy(mensaje = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                MateriasViewModel(c.horarioRepository, c.respaldoRepository)
            }
        }
    }
}
