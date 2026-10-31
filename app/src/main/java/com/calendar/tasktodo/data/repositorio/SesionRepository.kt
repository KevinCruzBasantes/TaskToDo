package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.db.dao.ActividadDao
import com.calendar.tasktodo.data.db.dao.GymDao
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.EstadoSesion
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.SerieRealizada
import com.calendar.tasktodo.data.db.entidad.SesionGym
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Dónde va la sesión, calculado solo con su orden y las series hechas (diagramas §1.3). */
data class Avance(
    /** Los ejercicios en el orden de la sesión. */
    val ordenados: List<Ejercicio>,
    val hechasPorEjercicio: Map<Long, Int>,
    /** Nulo cuando ya se hicieron todas las series. */
    val actual: Ejercicio?,
    /** Desde 1. */
    val serieActual: Int,
) {
    val completos: List<Ejercicio> get() = ordenados.filter { (hechasPorEjercicio[it.id] ?: 0) >= it.series }
    val pendientes: List<Ejercicio> get() = ordenados.filter { it.id != actual?.id && (hechasPorEjercicio[it.id] ?: 0) < it.series }
    val terminada: Boolean get() = actual == null
    val posicionActual: Int get() = actual?.let { a -> ordenados.indexOfFirst { it.id == a.id } + 1 } ?: ordenados.size
}

/** Valores con los que viene rellenada una serie (RF-28). */
data class Prellenado(val repeticiones: Int?, val duracionSeg: Int?, val pesoKg: Double?, val deLaUltimaVez: Boolean)

/** Las reglas de la sesión, sin Android ni base de datos, para probarlas directo. */
object MotorSesion {

    fun avance(orden: List<Long>, ejercicios: List<Ejercicio>, series: List<SerieRealizada>): Avance {
        val porId = ejercicios.associateBy { it.id }
        // Los ejercicios agregados a la rutina después de empezar van al final
        val ordenados = orden.mapNotNull { porId[it] } + ejercicios.filter { it.id !in orden }
        val hechas = series.mapNotNull { it.ejercicioId }.groupingBy { it }.eachCount()
        val actual = ordenados.firstOrNull { (hechas[it.id] ?: 0) < it.series }
        return Avance(ordenados, hechas, actual, actual?.let { (hechas[it.id] ?: 0) + 1 } ?: 0)
    }

    /**
     * Descanso después de confirmar la serie [numero] de [ejercicio]: entre series si quedan, o
     * entre ejercicios si era la última; el del ejercicio reemplaza al de la rutina (RF-29).
     */
    fun descansoTras(ejercicio: Ejercicio, numero: Int, rutina: Rutina?, quedanEjercicios: Boolean): Int? = when {
        numero < ejercicio.series -> ejercicio.descansoSeriesSeg
        quedanEjercicios -> ejercicio.descansoTrasEjercicioSeg ?: rutina?.descansoEntreEjerciciosSeg
        else -> null
    }

    /** "Hacer ahora": [id] pasa a ser el actual; el que estaba a medias vuelve a pendientes (CU-16 9b). */
    fun hacerAhora(avance: Avance, id: Long): List<Long> {
        val pendientes = (listOfNotNull(avance.actual) + avance.pendientes).map { it.id }.filter { it != id }
        return avance.completos.map { it.id } + id + pendientes
    }

    /** Mueve un pendiente de la posición [desde] a [hasta] entre los pendientes (CU-16 9b). */
    fun moverPendiente(avance: Avance, desde: Int, hasta: Int): List<Long> {
        val pendientes = avance.pendientes.map { it.id }.toMutableList()
        if (desde in pendientes.indices && hasta in pendientes.indices) pendientes.add(hasta, pendientes.removeAt(desde))
        return avance.completos.map { it.id } + listOfNotNull(avance.actual?.id) + pendientes
    }
}

sealed interface Inicio {
    data class Sesion(val id: Long) : Inicio

    /** Ya hay otra sesión en curso: solo puede haber una (modelo de datos §2.2). */
    data class OtraEnCurso(val id: Long) : Inicio

    data object SinEjercicios : Inicio
}

/** La sesión de gym (CU-16). El cronómetro y el descanso se calculan desde instantes guardados (RNF-07). */
class SesionRepository(
    private val gym: GymDao,
    private val actividades: ActividadDao,
    private val reloj: Clock,
    private val observador: ObservadorCambios = ObservadorCambios.NINGUNO,
) {
    val enCurso: Flow<SesionGym?> = gym.observarEnCurso()

    fun sesion(id: Long): Flow<SesionGym?> = gym.observarSesion(id)

    fun series(sesionId: Long): Flow<List<SerieRealizada>> = gym.observarSeries(sesionId)

    fun ejercicios(rutinaId: Long): Flow<List<Ejercicio>> = gym.observarEjercicios(rutinaId)

    suspend fun sesionDeActividad(actividadId: Long): SesionGym? = gym.sesionDeActividad(actividadId)

    suspend fun rutina(id: Long): Rutina? = gym.obtenerRutina(id)

    suspend fun iniciar(actividadId: Long): Inicio {
        gym.enCurso()?.let { return if (it.actividadId == actividadId) Inicio.Sesion(it.id) else Inicio.OtraEnCurso(it.id) }
        val actividad = actividades.obtener(actividadId) ?: return Inicio.SinEjercicios
        val rutina = actividad.rutinaId?.let { gym.obtenerRutina(it) } ?: return Inicio.SinEjercicios
        val ejercicios = gym.ejercicios(rutina.id)
        if (ejercicios.isEmpty()) return Inicio.SinEjercicios
        val id = gym.insertarSesion(
            SesionGym(
                actividadId = actividadId,
                rutinaId = rutina.id,
                nombreRutina = rutina.nombre,
                fecha = LocalDate.now(reloj),
                inicio = Instant.now(reloj),
                ordenEjercicios = ejercicios.map { it.id },
            ),
        )
        return Inicio.Sesion(id)
    }

    /** Lo que se hizo la última vez o, si no hay registro, lo de la rutina (RF-28). */
    suspend fun prellenado(ejercicio: Ejercicio, numero: Int): Prellenado {
        val ultima = gym.ultimaSerie(ejercicio.id, numero)
        return if (ultima != null) {
            Prellenado(ultima.repeticiones, ultima.duracionSeg, ultima.pesoKg, deLaUltimaVez = true)
        } else {
            Prellenado(ejercicio.repeticiones, ejercicio.duracionSeg, ejercicio.pesoKg, deLaUltimaVez = false)
        }
    }

    /** Guarda la serie y, si hay descanso configurado, lo inicia. Devuelve los segundos de descanso. */
    suspend fun confirmarSerie(
        sesion: SesionGym,
        avance: Avance,
        repeticiones: Int?,
        duracionSeg: Int?,
        pesoKg: Double?,
    ): Int? {
        val ejercicio = avance.actual ?: return null
        val numero = avance.serieActual
        val ahora = Instant.now(reloj)
        gym.insertarSerie(
            SerieRealizada(
                sesionId = sesion.id,
                ejercicioId = ejercicio.id,
                nombreEjercicio = ejercicio.nombre,
                ordenEjercicio = avance.posicionActual,
                numeroSerie = numero,
                medida = ejercicio.medida,
                repeticiones = repeticiones,
                duracionSeg = duracionSeg,
                pesoKg = pesoKg,
                completadaEn = ahora,
            ),
        )
        val rutina = sesion.rutinaId?.let { gym.obtenerRutina(it) }
        val descanso = MotorSesion.descansoTras(ejercicio, numero, rutina, quedanEjercicios = avance.pendientes.isNotEmpty())
        gym.actualizarSesion(sesion.copy(descansoTerminaEn = descanso?.let { ahora.plusSeconds(it.toLong()) }))
        observador.huboCambios()
        return descanso
    }

    /** ±15 segundos al descanso en curso (RF-29). */
    suspend fun ajustarDescanso(sesion: SesionGym, segundos: Long) {
        val fin = sesion.descansoTerminaEn ?: return
        val nuevo = fin.plusSeconds(segundos)
        gym.actualizarSesion(sesion.copy(descansoTerminaEn = if (nuevo.isAfter(Instant.now(reloj))) nuevo else null))
        observador.huboCambios()
    }

    suspend fun saltarDescanso(sesion: SesionGym) {
        gym.actualizarSesion(sesion.copy(descansoTerminaEn = null))
        observador.huboCambios()
    }

    /** "‹ Anterior": quita la última serie para volver a hacerla o corregirla. */
    suspend fun deshacerUltimaSerie(sesion: SesionGym) {
        gym.series(sesion.id).lastOrNull()?.let { gym.eliminarSerie(it) }
        gym.actualizarSesion(sesion.copy(descansoTerminaEn = null))
        observador.huboCambios()
    }

    suspend fun cambiarOrden(sesion: SesionGym, orden: List<Long>) {
        gym.actualizarSesion(sesion.copy(ordenEjercicios = orden))
    }

    /**
     * Cierra la sesión con su duración y deja la actividad realizada (RF-31). Devuelve si el orden
     * usado fue distinto al de la rutina, para preguntar si se guarda (CU-16 11a).
     */
    suspend fun terminar(sesion: SesionGym): Boolean {
        val ahora = Instant.now(reloj)
        gym.actualizarSesion(
            sesion.copy(
                fin = ahora,
                duracionSeg = Duration.between(sesion.inicio, ahora).seconds.toInt(),
                estado = EstadoSesion.TERMINADA,
                descansoTerminaEn = null,
            ),
        )
        sesion.actividadId?.let { actividades.cambiarEstado(it, EstadoActividad.CUMPLIDA) }
        observador.huboCambios()
        val deLaRutina = sesion.rutinaId?.let { gym.ejercicios(it).map { e -> e.id } }.orEmpty()
        return sesion.ordenEjercicios.filter { it in deLaRutina } != deLaRutina.filter { it in sesion.ordenEjercicios }
    }

    /** Descartar: se borra con sus series y la actividad sigue pendiente (CU-16 10b). */
    suspend fun descartar(sesion: SesionGym) {
        gym.eliminarSesion(sesion)
        observador.huboCambios()
    }
}
