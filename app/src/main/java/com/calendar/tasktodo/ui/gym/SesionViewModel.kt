package com.calendar.tasktodo.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.SerieRealizada
import com.calendar.tasktodo.data.db.entidad.SesionGym
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.Avance
import com.calendar.tasktodo.data.repositorio.GymRepository
import com.calendar.tasktodo.data.repositorio.Inicio
import com.calendar.tasktodo.data.repositorio.MotorSesion
import com.calendar.tasktodo.data.repositorio.SesionRepository
import com.calendar.tasktodo.sistema.sesion.ControlSesion
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SesionUiState(
    val cargando: Boolean = true,
    val actividad: Actividad? = null,
    val rutina: Rutina? = null,
    /** Para ver la rutina antes de iniciar. */
    val ejerciciosRutina: List<Ejercicio> = emptyList(),
    val sesion: SesionGym? = null,
    val avance: Avance? = null,
    val series: List<SerieRealizada> = emptyList(),
    /** Valores editables de la serie actual, prellenados (RF-28). */
    val repeticiones: Int = 0,
    val duracionSeg: Int = 0,
    val pesoKg: Double? = null,
    val ultimaVez: String? = null,
    val error: String? = null,
    /** El orden fue distinto al de la rutina: se pregunta si se guarda (CU-16 11a). */
    val preguntarOrden: Boolean = false,
    val salir: Boolean = false,
)

/** Realizar una sesión de gym (CU-16). Todo se guarda al momento, así se retoma si la app se cierra (RNF-07). */
class SesionViewModel(
    private val sesiones: SesionRepository,
    private val gym: GymRepository,
    private val actividades: ActividadRepository,
    private val control: ControlSesion,
    private val actividadId: Long,
) : ViewModel() {

    private val _estado = MutableStateFlow(SesionUiState())
    val estado: StateFlow<SesionUiState> = _estado.asStateFlow()

    private val sesionId = MutableStateFlow<Long?>(null)
    private var claveSerie: Pair<Long, Int>? = null

    init {
        viewModelScope.launch {
            val actividad = actividades.obtener(actividadId)
            val rutina = actividad?.rutinaId?.let { gym.obtenerRutina(it) }
            _estado.update { it.copy(cargando = false, actividad = actividad, rutina = rutina) }
            sesiones.sesionDeActividad(actividadId)?.let { sesionId.value = it.id }
            rutina?.let { r -> gym.ejercicios(r.id).collect { lista -> _estado.update { it.copy(ejerciciosRutina = lista) } } }
        }
        viewModelScope.launch { observarSesion() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observarSesion() {
        sesionId.filterNotNull()
            .flatMapLatest { id -> sesiones.sesion(id) }
            .flatMapLatest { sesion ->
                if (sesion == null) {
                    flowOf(Triple<SesionGym?, List<SerieRealizada>, List<Ejercicio>>(null, emptyList(), emptyList()))
                } else {
                    val ejercicios = sesion.rutinaId?.let { sesiones.ejercicios(it) } ?: flowOf(emptyList())
                    combine(sesiones.series(sesion.id), ejercicios) { series, lista -> Triple(sesion, series, lista) }
                }
            }
            .collect { (sesion, series, ejercicios) ->
                val avance = sesion?.let { MotorSesion.avance(it.ordenEjercicios, ejercicios, series) }
                _estado.update { it.copy(sesion = sesion, series = series, avance = avance) }
                avance?.actual?.let { prellenar(it, avance.serieActual) }
            }
    }

    /** Al pasar a otra serie, sus valores vienen de la última vez o de la rutina (RF-28). */
    private suspend fun prellenar(ejercicio: Ejercicio, numero: Int) {
        val clave = ejercicio.id to numero
        if (clave == claveSerie) return
        claveSerie = clave
        val p = sesiones.prellenado(ejercicio, numero)
        _estado.update {
            it.copy(
                repeticiones = p.repeticiones ?: 0,
                duracionSeg = p.duracionSeg ?: 0,
                pesoKg = p.pesoKg,
                ultimaVez = if (p.deLaUltimaVez) {
                    listOfNotNull(
                        p.repeticiones?.let { r -> "$r" } ?: p.duracionSeg?.let { d -> "%d:%02d".format(d / 60, d % 60) },
                        p.pesoKg?.let { kg -> "${textoPeso(kg)} kg" },
                    ).joinToString(" × ")
                } else {
                    null
                },
            )
        }
    }

    fun iniciar() {
        viewModelScope.launch {
            when (val r = sesiones.iniciar(actividadId)) {
                is Inicio.Sesion -> {
                    sesionId.value = r.id
                    control.iniciar()
                }
                is Inicio.OtraEnCurso -> _estado.update { it.copy(error = "Ya hay otra sesión en curso. Termínala antes de empezar esta.") }
                Inicio.SinEjercicios -> _estado.update { it.copy(error = "La rutina no tiene ejercicios.") }
            }
        }
    }

    fun cambiarRepeticiones(valor: Int) = _estado.update { it.copy(repeticiones = valor) }

    fun cambiarDuracion(valor: Int) = _estado.update { it.copy(duracionSeg = valor) }

    fun cambiarPeso(valor: Double?) = _estado.update { it.copy(pesoKg = valor?.coerceAtLeast(0.0)) }

    /** Un toque (RNF-08). */
    fun confirmarSerie() {
        val e = _estado.value
        val sesion = e.sesion ?: return
        val avance = e.avance ?: return
        val ejercicio = avance.actual ?: return
        viewModelScope.launch {
            sesiones.confirmarSerie(
                sesion,
                avance,
                repeticiones = e.repeticiones.takeIf { ejercicio.medida == Medida.REPETICIONES },
                duracionSeg = e.duracionSeg.takeIf { ejercicio.medida == Medida.TIEMPO },
                pesoKg = e.pesoKg,
            )
        }
    }

    fun ajustarDescanso(segundos: Long) {
        val sesion = _estado.value.sesion ?: return
        viewModelScope.launch { sesiones.ajustarDescanso(sesion, segundos) }
    }

    fun saltarDescanso() {
        val sesion = _estado.value.sesion ?: return
        viewModelScope.launch { sesiones.saltarDescanso(sesion) }
    }

    fun anterior() {
        val sesion = _estado.value.sesion ?: return
        claveSerie = null
        viewModelScope.launch { sesiones.deshacerUltimaSerie(sesion) }
    }

    fun hacerAhora(ejercicioId: Long) {
        val e = _estado.value
        val sesion = e.sesion ?: return
        val avance = e.avance ?: return
        viewModelScope.launch { sesiones.cambiarOrden(sesion, MotorSesion.hacerAhora(avance, ejercicioId)) }
    }

    fun moverPendiente(desde: Int, hasta: Int) {
        val e = _estado.value
        val sesion = e.sesion ?: return
        val avance = e.avance ?: return
        viewModelScope.launch { sesiones.cambiarOrden(sesion, MotorSesion.moverPendiente(avance, desde, hasta)) }
    }

    fun terminar() {
        val sesion = _estado.value.sesion ?: return
        viewModelScope.launch {
            val ordenCambio = sesiones.terminar(sesion)
            _estado.update { if (ordenCambio) it.copy(preguntarOrden = true) else it.copy(salir = true) }
        }
    }

    fun guardarOrden(guardar: Boolean) {
        val sesion = _estado.value.sesion
        viewModelScope.launch {
            if (guardar && sesion?.rutinaId != null) gym.ordenarComo(sesion.rutinaId, sesion.ordenEjercicios)
            _estado.update { it.copy(preguntarOrden = false, salir = true) }
        }
    }

    fun descartar() {
        val sesion = _estado.value.sesion ?: return
        viewModelScope.launch {
            sesiones.descartar(sesion)
            sesionId.value = null
            claveSerie = null
            _estado.update { it.copy(sesion = null, avance = null, series = emptyList(), salir = true) }
        }
    }

    fun cerrarError() = _estado.update { it.copy(error = null) }

    companion object {
        fun factory(actividadId: Long) = viewModelFactory {
            initializer {
                val c = contenedor()
                SesionViewModel(c.sesionRepository, c.gymRepository, c.actividadRepository, c.controlSesion, actividadId)
            }
        }
    }
}
