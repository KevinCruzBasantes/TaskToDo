package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.db.dao.ClaseDao
import com.calendar.tasktodo.data.db.dao.MateriaDao
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.MateriaConConteo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalTime

/** Materias y clases (CU-01 a CU-05). */
class HorarioRepository(
    private val materiaDao: MateriaDao,
    private val claseDao: ClaseDao,
) {
    val materias: Flow<List<Materia>> = materiaDao.observarTodas()

    val materiasConConteo: Flow<List<MateriaConConteo>> = materiaDao.observarConConteo()

    val cantidadMaterias: Flow<Int> = materiaDao.observarCantidad()

    val clases: Flow<List<ClaseConMateria>> = claseDao.observarTodas()

    fun clasesDelDia(dia: DayOfWeek): Flow<List<ClaseConMateria>> = claseDao.observarPorDia(dia)

    fun clase(id: Long): Flow<ClaseConMateria?> = claseDao.observar(id)

    suspend fun obtenerMateria(id: Long): Materia? = materiaDao.obtener(id)

    suspend fun obtenerClase(id: Long): Clase? = claseDao.obtener(id)

    /**
     * Sin distinguir mayúsculas, también en letras con tilde ("ÁLGEBRA" = "álgebra"), que el
     * `COLLATE NOCASE` de SQLite no cubre (CU-02 5b).
     */
    suspend fun existeMateria(nombre: String, excluirId: Long = 0): Boolean =
        materiaDao.observarTodas().first().any { it.id != excluirId && it.nombre.equals(nombre, ignoreCase = true) }

    suspend fun guardarMateria(materia: Materia): Long =
        if (materia.id == 0L) {
            materiaDao.insertar(materia)
        } else {
            materiaDao.actualizar(materia)
            materia.id
        }

    /** Borra la materia y sus clases (RF-03). */
    suspend fun eliminarMateria(materia: Materia) = materiaDao.eliminar(materia)

    /** Borra todas las materias y, en cascada, todas las clases (RF-04). */
    suspend fun eliminarTodasLasMaterias() = materiaDao.eliminarTodas()

    suspend fun contarClasesDeMateria(materiaId: Long): Int = claseDao.contarDeMateria(materiaId)

    suspend fun cruces(
        dia: DayOfWeek,
        inicio: LocalTime,
        fin: LocalTime,
        excluirId: Long = 0,
    ): List<ClaseConMateria> = claseDao.cruces(dia, inicio, fin, excluirId)

    suspend fun guardarClase(clase: Clase): Long =
        if (clase.id == 0L) {
            claseDao.insertar(clase)
        } else {
            claseDao.actualizar(clase)
            clase.id
        }

    suspend fun eliminarClase(clase: Clase) = claseDao.eliminar(clase)
}
