package com.calendar.tasktodo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.MateriaConConteo
import kotlinx.coroutines.flow.Flow

@Dao
interface MateriaDao {

    @Query(
        """
        SELECT materia.*, (SELECT COUNT(*) FROM clase WHERE clase.materia_id = materia.id) AS clases
        FROM materia ORDER BY nombre
        """,
    )
    fun observarConConteo(): Flow<List<MateriaConConteo>>

    @Query("SELECT * FROM materia ORDER BY nombre")
    fun observarTodas(): Flow<List<Materia>>

    @Query("SELECT COUNT(*) FROM materia")
    fun observarCantidad(): Flow<Int>

    @Query("SELECT * FROM materia WHERE id = :id")
    suspend fun obtener(id: Long): Materia?

    @Query("SELECT * FROM materia ORDER BY id")
    suspend fun todas(): List<Materia>

    /** Para restaurar un respaldo: conserva los ids del archivo. */
    @Insert
    suspend fun insertarTodas(materias: List<Materia>)

    @Insert
    suspend fun insertar(materia: Materia): Long

    @Update
    suspend fun actualizar(materia: Materia)

    /** Las clases se borran en cascada (RF-03). */
    @Delete
    suspend fun eliminar(materia: Materia)

    @Query("DELETE FROM materia")
    suspend fun eliminarTodas()
}
