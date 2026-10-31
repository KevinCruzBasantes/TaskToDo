package com.calendar.tasktodo.data.db.entidad

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Bloque del horario. Se guarda una vez con su día de la semana y se repite cada semana
 * hasta eliminarlo (RF-08): no hay una fila por fecha.
 */
@Entity(
    tableName = "clase",
    foreignKeys = [
        ForeignKey(
            entity = Materia::class,
            parentColumns = ["id"],
            childColumns = ["materia_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("materia_id"), Index("dia_semana")],
)
data class Clase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "materia_id") val materiaId: Long,
    @ColumnInfo(name = "dia_semana") val diaSemana: DayOfWeek,
    @ColumnInfo(name = "hora_inicio") val horaInicio: LocalTime,
    @ColumnInfo(name = "hora_fin") val horaFin: LocalTime,
    val aula: String? = null,
)

data class ClaseConMateria(
    @Embedded val clase: Clase,
    @ColumnInfo(name = "materia_nombre") val materiaNombre: String,
    @ColumnInfo(name = "materia_color") val materiaColor: Int,
)
