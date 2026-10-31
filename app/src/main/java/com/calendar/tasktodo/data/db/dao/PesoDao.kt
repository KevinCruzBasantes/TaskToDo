package com.calendar.tasktodo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.calendar.tasktodo.data.db.entidad.RegistroPeso
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PesoDao {

    /** Del más reciente al más antiguo (CU-17 2). */
    @Query("SELECT * FROM registro_peso ORDER BY fecha DESC")
    fun observarTodos(): Flow<List<RegistroPeso>>

    @Query("SELECT * FROM registro_peso WHERE id = :id")
    suspend fun obtener(id: Long): RegistroPeso?

    @Query("SELECT * FROM registro_peso WHERE fecha = :fecha")
    suspend fun porFecha(fecha: LocalDate): RegistroPeso?

    @Insert
    suspend fun insertar(registro: RegistroPeso): Long

    @Update
    suspend fun actualizar(registro: RegistroPeso)

    @Delete
    suspend fun eliminar(registro: RegistroPeso)

    // Respaldo

    @Query("SELECT * FROM registro_peso ORDER BY id")
    suspend fun todos(): List<RegistroPeso>

    @Insert
    suspend fun insertarTodos(registros: List<RegistroPeso>)

    @Query("DELETE FROM registro_peso")
    suspend fun vaciar()
}
