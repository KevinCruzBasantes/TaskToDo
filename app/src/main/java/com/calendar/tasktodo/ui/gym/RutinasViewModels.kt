package com.calendar.tasktodo.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.RutinaConConteo
import com.calendar.tasktodo.data.repositorio.GymRepository
import com.calendar.tasktodo.data.repositorio.ResultadoRutina
import com.calendar.tasktodo.ui.contenedor
import com.calendar.tasktodo.ui.nombre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lista de rutinas (CU-12). */
class RutinasViewModel(gym: GymRepository) : ViewModel() {

    /** Nula mientras carga. */
    val rutinas: StateFlow<List<RutinaConConteo>?> = gym.rutinas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory { initializer { RutinasViewModel(contenedor().gymRepository) } }
    }
}

data class FormRutinaUiState(
    val cargando: Boolean = true,
    val esEdicion: Boolean = false,
    val nombre: String = "",
    val descansoEntreEjercicios: Int? = null,
    val error: String? = null,
    /** Id guardado: la pantalla sigue al detalle de la rutina. */
    val guardada: Long? = null,
)

/** Crear o editar el nombre y el descanso de una rutina (CU-12). */
class FormRutinaViewModel(private val gym: GymRepository, private val id: Long?) : ViewModel() {

    private val _estado = MutableStateFlow(FormRutinaUiState(esEdicion = id != null))
    val estado: StateFlow<FormRutinaUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val rutina = id?.let { gym.obtenerRutina(it) }
            _estado.update {
                it.copy(cargando = false, nombre = rutina?.nombre.orEmpty(), descansoEntreEjercicios = rutina?.descansoEntreEjerciciosSeg)
            }
        }
    }

    fun cambiarNombre(nombre: String) = _estado.update { it.copy(nombre = nombre, error = null) }

    fun cambiarDescanso(segundos: Int?) = _estado.update { it.copy(descansoEntreEjercicios = segundos) }

    fun guardar() {
        val actual = _estado.value
        val nombre = actual.nombre.trim()
        if (nombre.isEmpty()) {
            _estado.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        viewModelScope.launch {
            when (val r = gym.guardarRutina(Rutina(id ?: 0, nombre, actual.descansoEntreEjercicios))) {
                is ResultadoRutina.Guardada -> _estado.update { it.copy(guardada = r.id) }
                ResultadoRutina.NombreRepetido -> _estado.update { it.copy(error = "Ya existe una rutina con ese nombre") }
                else -> Unit
            }
        }
    }

    companion object {
        fun factory(id: Long?) = viewModelFactory { initializer { FormRutinaViewModel(contenedor().gymRepository, id) } }
    }
}

data class DetalleRutinaUiState(
    val cargando: Boolean = true,
    /** Nula después de cargar significa que se borró. */
    val rutina: Rutina? = null,
    val ejercicios: List<Ejercicio> = emptyList(),
    /** Días de gym que usan la rutina, si se intentó borrarla (CU-12 3b). */
    val enUso: List<String>? = null,
)

/** Detalle de una rutina con sus ejercicios como bloques reordenables (CU-12, CU-13). */
class DetalleRutinaViewModel(private val gym: GymRepository, private val id: Long) : ViewModel() {

    private val enUso = MutableStateFlow<List<String>?>(null)

    val estado: StateFlow<DetalleRutinaUiState> = combine(gym.rutina(id), gym.ejercicios(id), enUso) { rutina, ejercicios, usado ->
        DetalleRutinaUiState(cargando = false, rutina = rutina, ejercicios = ejercicios, enUso = usado)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleRutinaUiState())

    /** Al soltar un bloque: se guarda al momento, sin botón (CU-13 2d). */
    fun mover(desde: Int, hasta: Int) {
        viewModelScope.launch { gym.moverEjercicio(id, desde, hasta) }
    }

    fun duplicar(ejercicio: Ejercicio) {
        viewModelScope.launch { gym.duplicarEjercicio(ejercicio) }
    }

    fun eliminarEjercicio(ejercicio: Ejercicio) {
        viewModelScope.launch { gym.eliminarEjercicio(ejercicio) }
    }

    fun eliminarRutina() {
        viewModelScope.launch {
            val r = gym.eliminarRutina(id)
            if (r is ResultadoRutina.EnUso) enUso.value = r.dias.map { it.nombre() }
        }
    }

    fun cerrarEnUso() {
        enUso.value = null
    }

    companion object {
        fun factory(id: Long) = viewModelFactory { initializer { DetalleRutinaViewModel(contenedor().gymRepository, id) } }
    }
}

data class FormEjercicioUiState(
    val cargando: Boolean = true,
    val esEdicion: Boolean = false,
    val nombre: String = "",
    val medida: Medida = Medida.REPETICIONES,
    val series: Int = 3,
    val repeticiones: Int = 10,
    val duracionSeg: Int = 30,
    /** Texto para aceptar decimales y vacío. */
    val peso: String = "",
    val descansoSeries: Int? = null,
    val descansoTras: Int? = null,
    val descansoRutina: Int? = null,
    val nota: String = "",
    val error: String? = null,
    val guardados: Int = 0,
    val terminado: Boolean = false,
)

/** Formulario de un ejercicio (CU-13). */
class FormEjercicioViewModel(
    private val gym: GymRepository,
    private val rutinaId: Long,
    private val id: Long?,
) : ViewModel() {

    private val _estado = MutableStateFlow(FormEjercicioUiState(esEdicion = id != null))
    val estado: StateFlow<FormEjercicioUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val rutina = gym.obtenerRutina(rutinaId)
            val e = id?.let { gym.obtenerEjercicio(it) }
            _estado.update {
                if (e == null) {
                    it.copy(cargando = false, descansoRutina = rutina?.descansoEntreEjerciciosSeg)
                } else {
                    it.copy(
                        cargando = false,
                        descansoRutina = rutina?.descansoEntreEjerciciosSeg,
                        nombre = e.nombre,
                        medida = e.medida,
                        series = e.series,
                        repeticiones = e.repeticiones ?: it.repeticiones,
                        duracionSeg = e.duracionSeg ?: it.duracionSeg,
                        peso = e.pesoKg?.let(::textoPeso).orEmpty(),
                        descansoSeries = e.descansoSeriesSeg,
                        descansoTras = e.descansoTrasEjercicioSeg,
                        nota = e.nota.orEmpty(),
                    )
                }
            }
        }
    }

    fun cambiar(transformar: (FormEjercicioUiState) -> FormEjercicioUiState) = _estado.update { transformar(it).copy(error = null) }

    /** [yOtro]: "Guardar y agregar otro" deja el formulario vacío para el siguiente (CU-13 4a). */
    fun guardar(yOtro: Boolean = false) {
        val e = _estado.value
        val nombre = e.nombre.trim()
        val peso = e.peso.replace(',', '.').trim()
        val error = when {
            nombre.isEmpty() -> "El nombre es obligatorio"
            e.series <= 0 -> "Las series deben ser más de 0"
            e.medida == Medida.REPETICIONES && e.repeticiones <= 0 -> "Las repeticiones deben ser más de 0"
            e.medida == Medida.TIEMPO && e.duracionSeg <= 0 -> "La duración debe ser más de 0"
            peso.isNotEmpty() && (peso.toDoubleOrNull() ?: -1.0) < 0 -> "El peso no es válido"
            else -> null
        }
        if (error != null) {
            _estado.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            val existente = id?.let { gym.obtenerEjercicio(it) }
            gym.guardarEjercicio(
                Ejercicio(
                    id = id ?: 0,
                    rutinaId = rutinaId,
                    orden = existente?.orden ?: 0,
                    nombre = nombre,
                    medida = e.medida,
                    series = e.series,
                    repeticiones = e.repeticiones.takeIf { e.medida == Medida.REPETICIONES },
                    duracionSeg = e.duracionSeg.takeIf { e.medida == Medida.TIEMPO },
                    pesoKg = peso.toDoubleOrNull(),
                    descansoSeriesSeg = e.descansoSeries,
                    descansoTrasEjercicioSeg = e.descansoTras,
                    nota = e.nota.trim().ifEmpty { null },
                ),
            )
            _estado.update {
                if (yOtro && id == null) {
                    FormEjercicioUiState(cargando = false, descansoRutina = it.descansoRutina, guardados = it.guardados + 1)
                } else {
                    it.copy(terminado = true)
                }
            }
        }
    }

    companion object {
        fun factory(rutinaId: Long, id: Long?) = viewModelFactory {
            initializer { FormEjercicioViewModel(contenedor().gymRepository, rutinaId, id) }
        }
    }
}

/** "72.5" o "60", sin ".0". */
fun textoPeso(kg: Double): String = if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()

/** "4 × 10 · 60 kg" o "3 × 0:45". */
fun Ejercicio.resumen(): String {
    val cantidad = if (medida == Medida.REPETICIONES) "$series × ${repeticiones ?: 0}" else "$series × ${"%d:%02d".format((duracionSeg ?: 0) / 60, (duracionSeg ?: 0) % 60)}"
    return listOfNotNull(cantidad, pesoKg?.let { "${textoPeso(it)} kg" }).joinToString(" · ")
}
