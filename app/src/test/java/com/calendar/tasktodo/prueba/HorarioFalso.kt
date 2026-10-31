package com.calendar.tasktodo.prueba

import com.calendar.tasktodo.data.db.dao.ActividadDao
import com.calendar.tasktodo.data.db.dao.ClaseDao
import com.calendar.tasktodo.data.db.dao.MateriaDao
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.MateriaConConteo
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.data.repositorio.ObservadorCambios
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * DAOs en memoria con las mismas reglas que Room: clases borradas en cascada con su materia,
 * actividades que quedan sin materia (SET_NULL) y el mismo orden de las consultas. Las reglas
 * reales se prueban en androidTest.
 */
class HorarioFalso {

    val materias = MutableStateFlow<List<Materia>>(emptyList())
    val clases = MutableStateFlow<List<Clase>>(emptyList())
    val actividades = MutableStateFlow<List<Actividad>>(emptyList())

    /** Cuántas veces los repositorios avisaron de un cambio (para los avisos). */
    var cambiosAvisados = 0
        private set

    /** Lejos de los ids de [DatosDePrueba], para que una inserción no choque con ellos. */
    private var siguienteId = 100L

    private val observador = ObservadorCambios { cambiosAvisados++ }

    private fun conMateria(clase: Clase): ClaseConMateria? =
        materias.value.firstOrNull { it.id == clase.materiaId }?.let { ClaseConMateria(clase, it.nombre, it.color) }

    private fun conMateria(actividad: Actividad): ActividadConMateria {
        val materia = materias.value.firstOrNull { it.id == actividad.materiaId }
        return ActividadConMateria(actividad, materia?.nombre, materia?.color)
    }

    private fun unidas(): Flow<List<ClaseConMateria>> = combine(materias, clases) { _, lista -> lista.mapNotNull(::conMateria) }

    /** Mismo orden que la consulta: por fecha, las sin hora al final, y por título. */
    private val orden = compareBy<Actividad>({ it.fecha }, { it.hora == null }, { it.hora }, { it.titulo })

    private fun actividadesUnidas(filtro: (Actividad) -> Boolean): Flow<List<ActividadConMateria>> =
        combine(materias, actividades) { _, lista -> lista.filter(filtro).sortedWith(orden).map(::conMateria) }

    private fun esAtrasada(a: Actividad, hoy: LocalDate) =
        a.tipo == TipoActividad.TAREA && a.estado == EstadoActividad.PENDIENTE && a.fecha.isBefore(hoy)

    val materiaDao = object : MateriaDao {
        override fun observarConConteo() = combine(materias, clases) { lista, todas ->
            lista.sortedBy { it.nombre.lowercase() }
                .map { m -> MateriaConConteo(m, todas.count { it.materiaId == m.id }) }
        }

        override fun observarTodas() = materias.map { lista -> lista.sortedBy { it.nombre.lowercase() } }

        override fun observarCantidad() = materias.map { it.size }

        override suspend fun obtener(id: Long) = materias.value.firstOrNull { it.id == id }

        override suspend fun todas() = materias.value.sortedBy { it.id }

        override suspend fun insertarTodas(materias: List<Materia>) {
            require(materias.none { m -> this@HorarioFalso.materias.value.any { it.id == m.id } }) { "UNIQUE constraint failed: materia.id" }
            this@HorarioFalso.materias.update { it + materias }
        }

        override suspend fun insertar(materia: Materia): Long {
            val id = siguienteId++
            materias.update { it + materia.copy(id = id) }
            return id
        }

        override suspend fun actualizar(materia: Materia) =
            materias.update { lista -> lista.map { if (it.id == materia.id) materia else it } }

        override suspend fun eliminar(materia: Materia) {
            clases.update { lista -> lista.filterNot { it.materiaId == materia.id } }
            actividades.update { lista -> lista.map { if (it.materiaId == materia.id) it.copy(materiaId = null) else it } }
            materias.update { lista -> lista.filterNot { it.id == materia.id } }
        }

        override suspend fun eliminarTodas() {
            clases.value = emptyList()
            actividades.update { lista -> lista.map { it.copy(materiaId = null) } }
            materias.value = emptyList()
        }
    }

    val claseDao = object : ClaseDao {
        override fun observarPorDia(dia: DayOfWeek) =
            unidas().map { lista -> lista.filter { it.clase.diaSemana == dia }.sortedBy { it.clase.horaInicio } }

        override fun observarTodas() =
            unidas().map { lista -> lista.sortedWith(compareBy({ it.clase.diaSemana }, { it.clase.horaInicio })) }

        override fun observar(id: Long) = unidas().map { lista -> lista.firstOrNull { it.clase.id == id } }

        override suspend fun obtener(id: Long) = clases.value.firstOrNull { it.id == id }

        override suspend fun cruces(dia: DayOfWeek, inicio: LocalTime, fin: LocalTime, excluirId: Long) =
            clases.value
                .filter { it.diaSemana == dia && it.id != excluirId && it.horaInicio < fin && it.horaFin > inicio }
                .mapNotNull(::conMateria)

        override suspend fun contarDeMateria(materiaId: Long) = clases.value.count { it.materiaId == materiaId }

        override suspend fun todas() = clases.value.sortedBy { it.id }

        override suspend fun insertarTodas(clases: List<Clase>) {
            // Como la clave foránea de Room: la materia debe existir
            require(clases.all { c -> materias.value.any { it.id == c.materiaId } }) { "FOREIGN KEY constraint failed" }
            this@HorarioFalso.clases.update { it + clases }
        }

        override suspend fun eliminarTodas() {
            clases.value = emptyList()
        }

        override suspend fun insertar(clase: Clase): Long {
            val id = siguienteId++
            clases.update { it + clase.copy(id = id) }
            return id
        }

        override suspend fun actualizar(clase: Clase) =
            clases.update { lista -> lista.map { if (it.id == clase.id) clase else it } }

        override suspend fun eliminar(clase: Clase) = clases.update { lista -> lista.filterNot { it.id == clase.id } }
    }

    val actividadDao = object : ActividadDao {
        override fun observarDelDia(fecha: LocalDate) = actividadesUnidas { it.fecha == fecha }

        override fun observarAtrasadas(hoy: LocalDate) = actividadesUnidas { esAtrasada(it, hoy) }

        override fun observarTodas() = actividadesUnidas { true }

        override fun observar(id: Long) = actividadesUnidas { it.id == id }.map { it.firstOrNull() }

        override fun observarProximasDeMateria(materiaId: Long, hoy: LocalDate, limite: Int) =
            actividadesUnidas { it.materiaId == materiaId && it.estado == EstadoActividad.PENDIENTE && !it.fecha.isBefore(hoy) }
                .map { it.take(limite) }

        override suspend fun obtenerConMateria(id: Long) = actividades.value.firstOrNull { it.id == id }?.let(::conMateria)

        override suspend fun obtener(id: Long) = actividades.value.firstOrNull { it.id == id }

        override suspend fun pendientesEntre(desde: LocalDate, hasta: LocalDate) =
            actividades.value.filter { it.estado == EstadoActividad.PENDIENTE && !it.fecha.isBefore(desde) && !it.fecha.isAfter(hasta) }

        override suspend fun delDia(fecha: LocalDate) =
            actividades.value.filter { it.fecha == fecha }.sortedWith(orden).map(::conMateria)

        override suspend fun atrasadas(hoy: LocalDate) =
            actividades.value.filter { esAtrasada(it, hoy) }.sortedWith(orden).map(::conMateria)

        override suspend fun cambiarEstado(id: Long, estado: EstadoActividad) =
            actividades.update { lista -> lista.map { if (it.id == id) it.copy(estado = estado) else it } }

        override suspend fun cambiarFecha(id: Long, fecha: LocalDate) =
            actividades.update { lista -> lista.map { if (it.id == id) it.copy(fecha = fecha) else it } }

        override suspend fun todas() = actividades.value.sortedBy { it.id }

        override suspend fun insertarTodas(actividades: List<Actividad>) {
            this@HorarioFalso.actividades.update { it + actividades }
        }

        override suspend fun eliminarTodas() {
            actividades.value = emptyList()
        }

        override suspend fun insertar(actividad: Actividad): Long {
            val id = siguienteId++
            actividades.update { it + actividad.copy(id = id) }
            return id
        }

        override suspend fun actualizar(actividad: Actividad) =
            actividades.update { lista -> lista.map { if (it.id == actividad.id) actividad else it } }

        override suspend fun eliminar(actividad: Actividad) =
            actividades.update { lista -> lista.filterNot { it.id == actividad.id } }
    }

    val repositorio = HorarioRepository(materiaDao, claseDao, observador)

    val actividadRepositorio = ActividadRepository(actividadDao, observador)
}
