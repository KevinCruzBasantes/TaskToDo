package com.calendar.tasktodo.data.db.entidad

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Lista ordenada de ejercicios (RF-25). */
@Entity(tableName = "rutina", indices = [Index(value = ["nombre"], unique = true)])
data class Rutina(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nombre: String,
    /** Descanso por defecto entre ejercicios, opcional. */
    @ColumnInfo(name = "descanso_entre_ejercicios_seg") val descansoEntreEjerciciosSeg: Int? = null,
)

data class RutinaConConteo(
    @Embedded val rutina: Rutina,
    @ColumnInfo(name = "ejercicios") val ejercicios: Int,
)

enum class Medida { REPETICIONES, TIEMPO }

/** Un bloque de la rutina; [orden] es su posición desde 0 (RF-26). */
@Entity(
    tableName = "ejercicio",
    foreignKeys = [
        ForeignKey(entity = Rutina::class, parentColumns = ["id"], childColumns = ["rutina_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["rutina_id", "orden"])],
)
data class Ejercicio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "rutina_id") val rutinaId: Long,
    val orden: Int,
    val nombre: String,
    val medida: Medida,
    val series: Int,
    /** Obligatorio si [medida] es REPETICIONES; lo valida el ViewModel. */
    val repeticiones: Int? = null,
    /** Obligatorio si [medida] es TIEMPO. */
    @ColumnInfo(name = "duracion_seg") val duracionSeg: Int? = null,
    @ColumnInfo(name = "peso_kg") val pesoKg: Double? = null,
    @ColumnInfo(name = "descanso_series_seg") val descansoSeriesSeg: Int? = null,
    /** Si existe, reemplaza el descanso entre ejercicios de la rutina. */
    @ColumnInfo(name = "descanso_tras_ejercicio_seg") val descansoTrasEjercicioSeg: Int? = null,
    val nota: String? = null,
)

/** Un día de la programación semanal del gym (RF-23). No se puede borrar una rutina asignada. */
@Entity(
    tableName = "dia_gym",
    foreignKeys = [
        ForeignKey(entity = Rutina::class, parentColumns = ["id"], childColumns = ["rutina_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["dia_semana"], unique = true), Index("rutina_id")],
)
data class DiaGym(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "dia_semana") val diaSemana: DayOfWeek,
    val hora: LocalTime,
    @ColumnInfo(name = "rutina_id") val rutinaId: Long,
)

enum class EstadoSesion { EN_CURSO, TERMINADA }

/** Cada vez que se hace una actividad de gym (RF-31). Conserva el nombre de la rutina por si se borra. */
@Entity(
    tableName = "sesion_gym",
    foreignKeys = [
        ForeignKey(entity = Actividad::class, parentColumns = ["id"], childColumns = ["actividad_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Rutina::class, parentColumns = ["id"], childColumns = ["rutina_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index(value = ["actividad_id"], unique = true), Index("rutina_id")],
)
data class SesionGym(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "actividad_id") val actividadId: Long?,
    @ColumnInfo(name = "rutina_id") val rutinaId: Long?,
    @ColumnInfo(name = "nombre_rutina") val nombreRutina: String,
    val fecha: LocalDate,
    /** El cronómetro siempre es ahora − inicio (RNF-07). */
    val inicio: Instant,
    val fin: Instant? = null,
    @ColumnInfo(name = "duracion_seg") val duracionSeg: Int? = null,
    val estado: EstadoSesion = EstadoSesion.EN_CURSO,
    @ColumnInfo(name = "descanso_termina_en") val descansoTerminaEn: Instant? = null,
    /** Orden de esta sesión; empieza igual al de la rutina y cambia si se reordena (CU-16 9b). */
    @ColumnInfo(name = "orden_ejercicios") val ordenEjercicios: List<Long>,
)

/** Lo realmente hecho en cada serie; conserva el nombre del ejercicio por si se borra. */
@Entity(
    tableName = "serie_realizada",
    foreignKeys = [
        ForeignKey(entity = SesionGym::class, parentColumns = ["id"], childColumns = ["sesion_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Ejercicio::class, parentColumns = ["id"], childColumns = ["ejercicio_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("sesion_id"), Index("ejercicio_id")],
)
data class SerieRealizada(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sesion_id") val sesionId: Long,
    @ColumnInfo(name = "ejercicio_id") val ejercicioId: Long?,
    @ColumnInfo(name = "nombre_ejercicio") val nombreEjercicio: String,
    @ColumnInfo(name = "orden_ejercicio") val ordenEjercicio: Int,
    @ColumnInfo(name = "numero_serie") val numeroSerie: Int,
    val medida: Medida,
    val repeticiones: Int? = null,
    @ColumnInfo(name = "duracion_seg") val duracionSeg: Int? = null,
    @ColumnInfo(name = "peso_kg") val pesoKg: Double? = null,
    @ColumnInfo(name = "completada_en") val completadaEn: Instant,
)
