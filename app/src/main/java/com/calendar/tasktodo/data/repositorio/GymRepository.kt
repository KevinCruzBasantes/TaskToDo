package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.data.db.dao.GymDao
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.DiaGym
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.RutinaConConteo
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

sealed interface ResultadoRutina {
    data class Guardada(val id: Long) : ResultadoRutina

    data object NombreRepetido : ResultadoRutina

    /** No se borra una rutina asignada a un día de gym (CU-12 3b). */
    data class EnUso(val dias: List<DayOfWeek>) : ResultadoRutina

    data object Eliminada : ResultadoRutina
}

/** Lo que el usuario elige para un día en la programación (CU-14). */
data class EleccionDia(val hora: LocalTime, val rutinaId: Long)

/** Rutinas, ejercicios, programación y la generación de actividades de gym (CU-12 a CU-15). */
class GymRepository(
    private val dao: GymDao,
    private val ajustes: AjustesRepository,
    private val reloj: Clock,
    private val observador: ObservadorCambios = ObservadorCambios.NINGUNO,
    /** `withTransaction` de Room; en las pruebas, solo ejecuta el bloque. */
    private val transaccion: suspend (suspend () -> Unit) -> Unit = { it() },
) {
    val rutinas: Flow<List<RutinaConConteo>> = dao.observarRutinas()

    val dias: Flow<List<DiaGym>> = dao.observarDias()

    val finProgramacion: Flow<LocalDate?> = ajustes.finProgramacionGym

    fun rutina(id: Long): Flow<Rutina?> = dao.observarRutina(id)

    fun ejercicios(rutinaId: Long): Flow<List<Ejercicio>> = dao.observarEjercicios(rutinaId)

    fun proximaSesion(): Flow<Actividad?> = dao.observarProximaGym(hoy())

    fun gymEntre(desde: LocalDate, hasta: LocalDate): Flow<List<Actividad>> = dao.observarGymEntre(desde, hasta)

    suspend fun obtenerRutina(id: Long): Rutina? = dao.obtenerRutina(id)

    suspend fun obtenerEjercicio(id: Long): Ejercicio? = dao.obtenerEjercicio(id)

    // Rutinas (CU-12)

    suspend fun guardarRutina(rutina: Rutina): ResultadoRutina {
        val repetida = dao.rutinas().any { it.id != rutina.id && it.nombre.equals(rutina.nombre, ignoreCase = true) }
        if (repetida) return ResultadoRutina.NombreRepetido
        val id = if (rutina.id == 0L) {
            dao.insertarRutina(rutina)
        } else {
            dao.actualizarRutina(rutina)
            dao.renombrarActividadesDeRutina(rutina.id, rutina.nombre)
            rutina.id
        }
        observador.huboCambios()
        return ResultadoRutina.Guardada(id)
    }

    /** Antes de borrarla, se borran sus actividades futuras pendientes, también las movidas (modelo §3). */
    suspend fun eliminarRutina(id: Long): ResultadoRutina {
        val enUso = dao.diasDeRutina(id)
        if (enUso.isNotEmpty()) return ResultadoRutina.EnUso(enUso.map { it.diaSemana })
        val rutina = dao.obtenerRutina(id) ?: return ResultadoRutina.Eliminada
        transaccion {
            dao.borrarFuturasDeRutina(id, hoy())
            dao.eliminarRutina(rutina)
        }
        observador.huboCambios()
        return ResultadoRutina.Eliminada
    }

    // Ejercicios como bloques (CU-13)

    /** Nuevo al final de la lista; si ya existe, se actualiza sin mover. */
    suspend fun guardarEjercicio(ejercicio: Ejercicio): Long =
        if (ejercicio.id == 0L) {
            dao.insertarEjercicio(ejercicio.copy(orden = dao.ejercicios(ejercicio.rutinaId).size))
        } else {
            dao.actualizarEjercicios(listOf(ejercicio))
            ejercicio.id
        }

    suspend fun eliminarEjercicio(ejercicio: Ejercicio) {
        transaccion {
            dao.eliminarEjercicio(ejercicio)
            renumerar(dao.ejercicios(ejercicio.rutinaId))
        }
    }

    /** La copia queda justo debajo del original (CU-13 2e). Devuelve su id. */
    suspend fun duplicarEjercicio(ejercicio: Ejercicio): Long {
        var nuevo = 0L
        transaccion {
            val lista = dao.ejercicios(ejercicio.rutinaId)
            nuevo = dao.insertarEjercicio(ejercicio.copy(id = 0, orden = lista.size))
            val copia = dao.obtenerEjercicio(nuevo)!!
            val posicion = lista.indexOfFirst { it.id == ejercicio.id } + 1
            renumerar(lista.toMutableList().apply { add(posicion, copia) })
        }
        return nuevo
    }

    /** Mueve el bloque de la posición [desde] a [hasta] y corre los demás (CU-13 2d). */
    suspend fun moverEjercicio(rutinaId: Long, desde: Int, hasta: Int) {
        transaccion {
            val lista = dao.ejercicios(rutinaId).toMutableList()
            if (desde !in lista.indices || hasta !in lista.indices || desde == hasta) return@transaccion
            lista.add(hasta, lista.removeAt(desde))
            renumerar(lista)
        }
    }

    /** Guarda en la rutina el orden que se usó en una sesión (CU-16 11a). */
    suspend fun ordenarComo(rutinaId: Long, ids: List<Long>) {
        transaccion {
            val lista = dao.ejercicios(rutinaId)
            val ordenados = ids.mapNotNull { id -> lista.firstOrNull { it.id == id } } + lista.filter { it.id !in ids }
            renumerar(ordenados)
        }
    }

    private suspend fun renumerar(lista: List<Ejercicio>) {
        val cambiados = lista.mapIndexedNotNull { i, e -> if (e.orden != i) e.copy(orden = i) else null }
        if (cambiados.isNotEmpty()) dao.actualizarEjercicios(cambiados)
    }

    // Programación (CU-14)

    /**
     * Actualiza los días en su lugar (así las actividades ya generadas siguen enlazadas a su día),
     * guarda la fecha de fin y regenera las actividades futuras (diagramas §5).
     */
    suspend fun guardarProgramacion(elegidos: Map<DayOfWeek, EleccionDia>, fin: LocalDate?) {
        transaccion {
            val actuales = dao.dias().associateBy { it.diaSemana }
            DayOfWeek.entries.forEach { dia ->
                val actual = actuales[dia]
                val eleccion = elegidos[dia]
                when {
                    eleccion == null && actual != null -> dao.eliminarDia(actual)
                    eleccion != null && actual == null -> dao.insertarDia(DiaGym(diaSemana = dia, hora = eleccion.hora, rutinaId = eleccion.rutinaId))
                    eleccion != null && actual != null -> dao.actualizarDia(actual.copy(hora = eleccion.hora, rutinaId = eleccion.rutinaId))
                }
            }
        }
        ajustes.guardarFinProgramacionGym(fin)
        generar(cambioLaProgramacion = true)
    }

    /**
     * Genera las actividades de gym hasta [SEMANAS_ADELANTE] semanas o hasta la fecha de fin. Si la
     * programación cambió, primero borra las futuras sin mover; las movidas, saltadas y hechas no
     * se tocan, y el índice único evita duplicarlas (diagramas de comportamiento §5).
     */
    suspend fun generar(cambioLaProgramacion: Boolean = false) {
        val hoy = hoy()
        val fin = ajustes.finProgramacionGym.first()
        val limite = listOfNotNull(hoy.plusWeeks(SEMANAS_ADELANTE), fin).min()
        transaccion {
            if (cambioLaProgramacion) dao.borrarGeneradasFuturas(hoy)
            val rutinas = dao.rutinas().associateBy { it.id }
            dao.dias().forEach { dia ->
                val rutina = rutinas[dia.rutinaId] ?: return@forEach
                generateSequence(hoy) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(limite) }
                    .filter { it.dayOfWeek == dia.diaSemana }
                    .forEach { fecha ->
                        dao.insertarGenerada(
                            Actividad(
                                titulo = rutina.nombre,
                                tipo = TipoActividad.GYM,
                                fecha = fecha,
                                hora = dia.hora,
                                rutinaId = rutina.id,
                                diaGymId = dia.id,
                                fechaProgramada = fecha,
                            ),
                        )
                    }
            }
        }
        observador.huboCambios()
    }

    // Una sesión puntual (CU-15)

    suspend fun moverSesion(actividadId: Long, fecha: LocalDate, hora: LocalTime) {
        dao.moverActividad(actividadId, fecha, hora)
        observador.huboCambios()
    }

    private fun hoy() = LocalDate.now(reloj)

    companion object {
        const val SEMANAS_ADELANTE = 4L
    }
}
