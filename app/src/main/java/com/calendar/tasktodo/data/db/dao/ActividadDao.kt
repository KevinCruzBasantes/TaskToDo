package com.calendar.tasktodo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

private const val ACTIVIDAD_CON_MATERIA = """
    SELECT actividad.*, materia.nombre AS materia_nombre, materia.color AS materia_color
    FROM actividad LEFT JOIN materia ON materia.id = actividad.materia_id
"""

/** Las sin hora van al final de cada fecha. */
private const val ORDEN = "ORDER BY actividad.fecha, actividad.hora IS NULL, actividad.hora, actividad.titulo"

@Dao
interface ActividadDao {

    @Query("$ACTIVIDAD_CON_MATERIA WHERE actividad.fecha = :fecha $ORDEN")
    fun observarDelDia(fecha: LocalDate): Flow<List<ActividadConMateria>>

    /** Tareas sin check de días anteriores (RF-14). */
    @Query("$ACTIVIDAD_CON_MATERIA WHERE actividad.tipo = 'TAREA' AND actividad.estado = 'PENDIENTE' AND actividad.fecha < :hoy $ORDEN")
    fun observarAtrasadas(hoy: LocalDate): Flow<List<ActividadConMateria>>

    @Query("$ACTIVIDAD_CON_MATERIA $ORDEN")
    fun observarTodas(): Flow<List<ActividadConMateria>>

    @Query("$ACTIVIDAD_CON_MATERIA WHERE actividad.id = :id")
    fun observar(id: Long): Flow<ActividadConMateria?>

    @Query(
        """
        $ACTIVIDAD_CON_MATERIA
        WHERE actividad.materia_id = :materiaId AND actividad.estado = 'PENDIENTE' AND actividad.fecha >= :hoy
        $ORDEN LIMIT :limite
        """,
    )
    fun observarProximasDeMateria(materiaId: Long, hoy: LocalDate, limite: Int): Flow<List<ActividadConMateria>>

    @Query("$ACTIVIDAD_CON_MATERIA WHERE actividad.id = :id")
    suspend fun obtenerConMateria(id: Long): ActividadConMateria?

    @Query("SELECT * FROM actividad WHERE id = :id")
    suspend fun obtener(id: Long): Actividad?

    /** Para calcular los avisos de la ventana (diagramas de comportamiento §3). */
    @Query("SELECT * FROM actividad WHERE estado = 'PENDIENTE' AND fecha BETWEEN :desde AND :hasta")
    suspend fun pendientesEntre(desde: LocalDate, hasta: LocalDate): List<Actividad>

    @Query("$ACTIVIDAD_CON_MATERIA WHERE actividad.fecha = :fecha $ORDEN")
    suspend fun delDia(fecha: LocalDate): List<ActividadConMateria>

    @Query("$ACTIVIDAD_CON_MATERIA WHERE actividad.tipo = 'TAREA' AND actividad.estado = 'PENDIENTE' AND actividad.fecha < :hoy $ORDEN")
    suspend fun atrasadas(hoy: LocalDate): List<ActividadConMateria>

    @Query("UPDATE actividad SET estado = :estado WHERE id = :id")
    suspend fun cambiarEstado(id: Long, estado: EstadoActividad)

    @Query("UPDATE actividad SET fecha = :fecha WHERE id = :id")
    suspend fun cambiarFecha(id: Long, fecha: LocalDate)

    @Query("SELECT * FROM actividad ORDER BY id")
    suspend fun todas(): List<Actividad>

    @Insert
    suspend fun insertarTodas(actividades: List<Actividad>)

    @Query("DELETE FROM actividad")
    suspend fun eliminarTodas()

    @Insert
    suspend fun insertar(actividad: Actividad): Long

    @Update
    suspend fun actualizar(actividad: Actividad)

    @Delete
    suspend fun eliminar(actividad: Actividad)
}
