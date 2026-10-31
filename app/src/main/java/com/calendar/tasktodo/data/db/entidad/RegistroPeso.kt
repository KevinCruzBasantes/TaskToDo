package com.calendar.tasktodo.data.db.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Un registro de peso corporal por día (CU-17 6b). El IMC no se guarda: se calcula con la estatura
 * de los ajustes, así cambiarla actualiza todos los registros (modelo de datos §2.2).
 */
@Entity(tableName = "registro_peso", indices = [Index(value = ["fecha"], unique = true)])
data class RegistroPeso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    @ColumnInfo(name = "peso_kg") val pesoKg: Double,
)
