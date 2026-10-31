package com.calendar.tasktodo.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.RegistroPeso
import com.calendar.tasktodo.data.repositorio.Imc
import com.calendar.tasktodo.data.repositorio.PesoRepository
import com.calendar.tasktodo.data.repositorio.RegistroConImc
import com.calendar.tasktodo.data.repositorio.ResultadoPeso
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

/** La hoja inferior para agregar o editar un registro. */
data class EditorPeso(
    val id: Long = 0,
    val peso: String = "",
    val fecha: LocalDate,
    val error: String? = null,
    /** El registro que ya está en esa fecha, cuando se pregunta si se reemplaza (CU-17 6b). */
    val ocupada: RegistroPeso? = null,
) {
    val esEdicion: Boolean get() = id != 0L
}

data class PesoUiState(
    val cargando: Boolean = true,
    val estaturaCm: Int? = null,
    val registros: List<RegistroConImc> = emptyList(),
    val editor: EditorPeso? = null,
    val errorEstatura: String? = null,
) {
    val actual: RegistroConImc? get() = registros.firstOrNull()
}

/** Peso e IMC (CU-17). */
class PesoViewModel(private val peso: PesoRepository, private val reloj: Clock) : ViewModel() {

    private val editor = MutableStateFlow<EditorPeso?>(null)
    private val errorEstatura = MutableStateFlow<String?>(null)

    val estado: StateFlow<PesoUiState> = combine(peso.registros, peso.estaturaCm, editor, errorEstatura) { registros, estatura, e, error ->
        PesoUiState(cargando = false, estaturaCm = estatura, registros = registros, editor = e, errorEstatura = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PesoUiState())

    fun nuevo() {
        editor.value = EditorPeso(fecha = LocalDate.now(reloj))
    }

    fun editar(registro: RegistroPeso) {
        editor.value = EditorPeso(registro.id, textoPeso(registro.pesoKg), registro.fecha)
    }

    fun cambiarPeso(texto: String) = editor.update { it?.copy(peso = texto.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6), error = null) }

    fun cambiarFecha(fecha: LocalDate) = editor.update { it?.copy(fecha = fecha, ocupada = null) }

    fun cerrarEditor() {
        editor.value = null
    }

    fun guardar(reemplazar: Boolean = false) {
        val e = editor.value ?: return
        val kg = e.peso.replace(',', '.').toDoubleOrNull()
        if (kg == null || kg <= 0) {
            editor.value = e.copy(error = "Ingresa un peso mayor que 0")
            return
        }
        viewModelScope.launch {
            when (val r = peso.guardar(RegistroPeso(e.id, e.fecha, kg), reemplazar)) {
                ResultadoPeso.Guardado -> editor.value = null
                ResultadoPeso.PesoInvalido -> editor.value = e.copy(error = "Ingresa un peso mayor que 0")
                is ResultadoPeso.FechaOcupada -> editor.value = e.copy(ocupada = r.existente)
            }
        }
    }

    fun noReemplazar() = editor.update { it?.copy(ocupada = null) }

    fun eliminar(registro: RegistroPeso) {
        viewModelScope.launch { peso.eliminar(registro) }
    }

    fun guardarEstatura(texto: String) {
        val cm = texto.toIntOrNull()
        viewModelScope.launch {
            errorEstatura.value = if (cm != null && peso.guardarEstatura(cm)) {
                null
            } else {
                "Ingresa tu estatura en centímetros, entre ${Imc.ESTATURA_MINIMA} y ${Imc.ESTATURA_MAXIMA}"
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                PesoViewModel(c.pesoRepository, c.reloj)
            }
        }
    }
}

/** "23.7". */
fun textoImc(imc: Double): String = "%.1f".format(java.util.Locale.ROOT, imc)
