package com.calendar.tasktodo.prueba

import com.calendar.tasktodo.data.db.dao.ClaseDao
import com.calendar.tasktodo.data.db.dao.MateriaDao
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.MateriaConConteo
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * DAOs en memoria con las mismas reglas que Room: nombre único sin distinguir mayúsculas y
 * clases borradas en cascada con su materia. Las reglas reales se prueban en androidTest.
 */
class HorarioFalso {

    val materias = MutableStateFlow<List<Materia>>(emptyList())
    val clases = MutableStateFlow<List<Clase>>(emptyList())
    /** Lejos de los ids de [DatosDePrueba], para que una inserción no choque con ellos. */
    private var siguienteId = 100L

    private fun conMateria(clase: Clase): ClaseConMateria? =
        materias.value.firstOrNull { it.id == clase.materiaId }?.let { ClaseConMateria(clase, it.nombre, it.color) }

    private fun unidas(): Flow<List<ClaseConMateria>> =
        combine(materias, clases) { _, lista -> lista.mapNotNull(::conMateria) }

    val materiaDao = object : MateriaDao {
        override fun observarConConteo() = combine(materias, clases) { lista, todas ->
            lista.sortedBy { it.nombre.lowercase() }
                .map { m -> MateriaConConteo(m, todas.count { it.materiaId == m.id }) }
        }

        override fun observarTodas() = materias.map { lista -> lista.sortedBy { it.nombre.lowercase() } }

        override fun observarCantidad() = materias.map { it.size }

        override suspend fun obtener(id: Long) = materias.value.firstOrNull { it.id == id }

        override suspend fun insertar(materia: Materia): Long {
            val id = siguienteId++
            materias.update { it + materia.copy(id = id) }
            return id
        }

        override suspend fun actualizar(materia: Materia) =
            materias.update { lista -> lista.map { if (it.id == materia.id) materia else it } }

        override suspend fun eliminar(materia: Materia) {
            clases.update { lista -> lista.filterNot { it.materiaId == materia.id } }
            materias.update { lista -> lista.filterNot { it.id == materia.id } }
        }

        override suspend fun eliminarTodas() {
            clases.value = emptyList()
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

        override suspend fun insertar(clase: Clase): Long {
            val id = siguienteId++
            clases.update { it + clase.copy(id = id) }
            return id
        }

        override suspend fun actualizar(clase: Clase) =
            clases.update { lista -> lista.map { if (it.id == clase.id) clase else it } }

        override suspend fun eliminar(clase: Clase) = clases.update { lista -> lista.filterNot { it.id == clase.id } }
    }

    val repositorio = HorarioRepository(materiaDao, claseDao)
}
