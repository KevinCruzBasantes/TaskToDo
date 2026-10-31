package com.calendar.tasktodo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalTime

private const val CLASE_CON_MATERIA = """
    SELECT clase.*, materia.nombre AS materia_nombre, materia.color AS materia_color
    FROM clase INNER JOIN materia ON materia.id = clase.materia_id
"""

@Dao
interface ClaseDao {

    @Query("$CLASE_CON_MATERIA WHERE clase.dia_semana = :dia ORDER BY clase.hora_inicio")
    fun observarPorDia(dia: DayOfWeek): Flow<List<ClaseConMateria>>

    @Query("$CLASE_CON_MATERIA ORDER BY clase.dia_semana, clase.hora_inicio")
    fun observarTodas(): Flow<List<ClaseConMateria>>

    @Query("$CLASE_CON_MATERIA WHERE clase.id = :id")
    fun observar(id: Long): Flow<ClaseConMateria?>

    @Query("SELECT * FROM clase WHERE id = :id")
    suspend fun obtener(id: Long): Clase?

    /** Clases del mismo día que se solapan con el rango; tocarse en un extremo no cuenta. */
    @Query(
        """
        $CLASE_CON_MATERIA
        WHERE clase.dia_semana = :dia AND clase.id <> :excluirId
          AND clase.hora_inicio < :fin AND clase.hora_fin > :inicio
        ORDER BY clase.hora_inicio
        """,
    )
    suspend fun cruces(dia: DayOfWeek, inicio: LocalTime, fin: LocalTime, excluirId: Long): List<ClaseConMateria>

    @Query("SELECT COUNT(*) FROM clase WHERE materia_id = :materiaId")
    suspend fun contarDeMateria(materiaId: Long): Int

    @Query("SELECT * FROM clase ORDER BY id")
    suspend fun todas(): List<Clase>

    @Insert
    suspend fun insertarTodas(clases: List<Clase>)

    @Query("DELETE FROM clase")
    suspend fun eliminarTodas()

    @Insert
    suspend fun insertar(clase: Clase): Long

    @Update
    suspend fun actualizar(clase: Clase)

    @Delete
    suspend fun eliminar(clase: Clase)
}
