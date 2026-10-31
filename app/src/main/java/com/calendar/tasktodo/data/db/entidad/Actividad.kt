package com.calendar.tasktodo.data.db.entidad

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

enum class TipoActividad { TAREA, CHARLA, SIMULACRO, EXAMEN, GYM }

/** Lo único que se guarda; atrasada, no marcada y no realizada se calculan con [situacion]. */
enum class EstadoActividad { PENDIENTE, CUMPLIDA, SALTADA }

/**
 * Algo por hacer en una fecha (modelo de datos §2.2). Las de gym las genera la programación y son
 * únicas por (`dia_gym_id`, `fecha_programada`), así generar dos veces no duplica nada.
 */
@Entity(
    tableName = "actividad",
    foreignKeys = [
        ForeignKey(
            entity = Materia::class,
            parentColumns = ["id"],
            childColumns = ["materia_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(entity = Rutina::class, parentColumns = ["id"], childColumns = ["rutina_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = DiaGym::class, parentColumns = ["id"], childColumns = ["dia_gym_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("fecha"),
        Index("materia_id"),
        Index("rutina_id"),
        Index(value = ["dia_gym_id", "fecha_programada"], unique = true),
    ],
)
data class Actividad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val tipo: TipoActividad,
    val fecha: LocalDate,
    val hora: LocalTime? = null,
    /** Nula si no tiene materia o si su materia se borró (RF-03). */
    @ColumnInfo(name = "materia_id") val materiaId: Long? = null,
    val notas: String? = null,
    /** Nulo = sin aviso. Sin hora, el aviso va a la hora fija de los ajustes (RF-38). */
    @ColumnInfo(name = "aviso_minutos_antes") val avisoMinutosAntes: Int? = null,
    val estado: EstadoActividad = EstadoActividad.PENDIENTE,
    /** Solo gym: la rutina que toca ese día. */
    @ColumnInfo(name = "rutina_id") val rutinaId: Long? = null,
    /** Solo gym: el día de la programación que la generó. */
    @ColumnInfo(name = "dia_gym_id") val diaGymId: Long? = null,
    /** Solo gym: la fecha en que la generó la programación; no cambia al moverla (CU-15). */
    @ColumnInfo(name = "fecha_programada") val fechaProgramada: LocalDate? = null,
)

data class ActividadConMateria(
    @Embedded val actividad: Actividad,
    @ColumnInfo(name = "materia_nombre") val materiaNombre: String?,
    @ColumnInfo(name = "materia_color") val materiaColor: Int?,
)

/** Cómo se ve una actividad un día dado (diagramas de comportamiento §4). */
enum class Situacion { PENDIENTE, CUMPLIDA, SALTADA, ATRASADA, NO_MARCADA, NO_REALIZADA }

fun Actividad.situacion(hoy: LocalDate): Situacion = when {
    estado == EstadoActividad.CUMPLIDA -> Situacion.CUMPLIDA
    estado == EstadoActividad.SALTADA -> Situacion.SALTADA
    !fecha.isBefore(hoy) -> Situacion.PENDIENTE
    tipo == TipoActividad.TAREA -> Situacion.ATRASADA
    tipo == TipoActividad.GYM -> Situacion.NO_REALIZADA
    else -> Situacion.NO_MARCADA
}

/** Los examenes y simulacros tienen además el aviso del día anterior (RF-39). */
val TipoActividad.tieneAvisoDiaAnterior: Boolean
    get() = this == TipoActividad.EXAMEN || this == TipoActividad.SIMULACRO
