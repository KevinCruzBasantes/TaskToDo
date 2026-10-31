package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.db.dao.ActividadDao
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Actividades (CU-06 a CU-10). Cada cambio recalcula los avisos. */
class ActividadRepository(
    private val dao: ActividadDao,
    private val observador: ObservadorCambios = ObservadorCambios.NINGUNO,
) {
    val todas: Flow<List<ActividadConMateria>> = dao.observarTodas()

    fun delDia(fecha: LocalDate): Flow<List<ActividadConMateria>> = dao.observarDelDia(fecha)

    fun atrasadas(hoy: LocalDate): Flow<List<ActividadConMateria>> = dao.observarAtrasadas(hoy)

    fun actividad(id: Long): Flow<ActividadConMateria?> = dao.observar(id)

    /** Las próximas pendientes de una materia, para el detalle de clase (RF-16). */
    fun proximasDeMateria(materiaId: Long, hoy: LocalDate, limite: Int = 5): Flow<List<ActividadConMateria>> =
        dao.observarProximasDeMateria(materiaId, hoy, limite)

    suspend fun obtener(id: Long): Actividad? = dao.obtener(id)

    suspend fun guardar(actividad: Actividad): Long {
        val id = if (actividad.id == 0L) {
            dao.insertar(actividad)
        } else {
            dao.actualizar(actividad)
            actividad.id
        }
        observador.huboCambios()
        return id
    }

    suspend fun eliminar(actividad: Actividad) {
        dao.eliminar(actividad)
        observador.huboCambios()
    }

    /** Marca o desmarca con un solo toque (RF-13, RNF-08). */
    suspend fun cambiarCumplida(id: Long, cumplida: Boolean) {
        dao.cambiarEstado(id, if (cumplida) EstadoActividad.CUMPLIDA else EstadoActividad.PENDIENTE)
        observador.huboCambios()
    }

    /** Reprogramar una tarea atrasada; solo hoy o después (CU-09). */
    suspend fun reprogramar(id: Long, fecha: LocalDate, hoy: LocalDate) {
        require(!fecha.isBefore(hoy)) { "Solo se puede reprogramar para hoy o una fecha futura" }
        dao.cambiarFecha(id, fecha)
        observador.huboCambios()
    }
}
