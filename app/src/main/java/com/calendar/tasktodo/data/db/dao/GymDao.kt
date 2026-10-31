package com.calendar.tasktodo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.DiaGym
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.RutinaConConteo
import com.calendar.tasktodo.data.db.entidad.SerieRealizada
import com.calendar.tasktodo.data.db.entidad.SesionGym
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/** Rutinas, ejercicios, programación y sesiones (CU-12 a CU-16). */
@Dao
interface GymDao {

    // Rutinas

    @Query(
        """
        SELECT rutina.*, (SELECT COUNT(*) FROM ejercicio WHERE ejercicio.rutina_id = rutina.id) AS ejercicios
        FROM rutina ORDER BY nombre
        """,
    )
    fun observarRutinas(): Flow<List<RutinaConConteo>>

    @Query("SELECT * FROM rutina WHERE id = :id")
    fun observarRutina(id: Long): Flow<Rutina?>

    @Query("SELECT * FROM rutina WHERE id = :id")
    suspend fun obtenerRutina(id: Long): Rutina?

    @Query("SELECT * FROM rutina ORDER BY id")
    suspend fun rutinas(): List<Rutina>

    @Insert
    suspend fun insertarRutina(rutina: Rutina): Long

    @Update
    suspend fun actualizarRutina(rutina: Rutina)

    @Delete
    suspend fun eliminarRutina(rutina: Rutina)

    // Ejercicios

    @Query("SELECT * FROM ejercicio WHERE rutina_id = :rutinaId ORDER BY orden")
    fun observarEjercicios(rutinaId: Long): Flow<List<Ejercicio>>

    @Query("SELECT * FROM ejercicio WHERE rutina_id = :rutinaId ORDER BY orden")
    suspend fun ejercicios(rutinaId: Long): List<Ejercicio>

    @Query("SELECT * FROM ejercicio WHERE id = :id")
    suspend fun obtenerEjercicio(id: Long): Ejercicio?

    @Insert
    suspend fun insertarEjercicio(ejercicio: Ejercicio): Long

    @Update
    suspend fun actualizarEjercicios(ejercicios: List<Ejercicio>)

    @Delete
    suspend fun eliminarEjercicio(ejercicio: Ejercicio)

    // Programación

    @Query("SELECT * FROM dia_gym ORDER BY dia_semana")
    fun observarDias(): Flow<List<DiaGym>>

    @Query("SELECT * FROM dia_gym ORDER BY dia_semana")
    suspend fun dias(): List<DiaGym>

    @Query("SELECT * FROM dia_gym WHERE rutina_id = :rutinaId")
    suspend fun diasDeRutina(rutinaId: Long): List<DiaGym>

    @Insert
    suspend fun insertarDia(dia: DiaGym): Long

    @Update
    suspend fun actualizarDia(dia: DiaGym)

    @Delete
    suspend fun eliminarDia(dia: DiaGym)

    // Actividades de gym

    /** Si ya existe esa ocurrencia (`dia_gym_id` + `fecha_programada`) se ignora (diagramas §5). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarGenerada(actividad: Actividad): Long

    /** Las futuras sin mover ni hacer, antes de regenerar (CU-14 6a). */
    @Query(
        """
        DELETE FROM actividad WHERE tipo = 'GYM' AND estado = 'PENDIENTE'
        AND fecha >= :desde AND fecha = fecha_programada
        """,
    )
    suspend fun borrarGeneradasFuturas(desde: LocalDate)

    /** Antes de borrar una rutina: sus actividades futuras pendientes, también las movidas (modelo §3). */
    @Query("DELETE FROM actividad WHERE tipo = 'GYM' AND estado = 'PENDIENTE' AND fecha >= :desde AND rutina_id = :rutinaId")
    suspend fun borrarFuturasDeRutina(rutinaId: Long, desde: LocalDate)

    @Query("UPDATE actividad SET titulo = :nombre WHERE tipo = 'GYM' AND estado = 'PENDIENTE' AND rutina_id = :rutinaId")
    suspend fun renombrarActividadesDeRutina(rutinaId: Long, nombre: String)

    @Query("UPDATE actividad SET fecha = :fecha, hora = :hora WHERE id = :id")
    suspend fun moverActividad(id: Long, fecha: LocalDate, hora: LocalTime)

    @Query("SELECT * FROM actividad WHERE tipo = 'GYM' AND estado = 'PENDIENTE' AND fecha >= :hoy ORDER BY fecha, hora LIMIT 1")
    fun observarProximaGym(hoy: LocalDate): Flow<Actividad?>

    @Query("SELECT * FROM actividad WHERE tipo = 'GYM' AND fecha BETWEEN :desde AND :hasta")
    fun observarGymEntre(desde: LocalDate, hasta: LocalDate): Flow<List<Actividad>>

    // Sesiones

    @Query("SELECT * FROM sesion_gym WHERE estado = 'EN_CURSO' LIMIT 1")
    fun observarEnCurso(): Flow<SesionGym?>

    @Query("SELECT * FROM sesion_gym WHERE estado = 'EN_CURSO' LIMIT 1")
    suspend fun enCurso(): SesionGym?

    @Query("SELECT * FROM sesion_gym WHERE id = :id")
    fun observarSesion(id: Long): Flow<SesionGym?>

    @Query("SELECT * FROM sesion_gym WHERE id = :id")
    suspend fun obtenerSesion(id: Long): SesionGym?

    @Query("SELECT * FROM sesion_gym WHERE actividad_id = :actividadId")
    suspend fun sesionDeActividad(actividadId: Long): SesionGym?

    @Insert
    suspend fun insertarSesion(sesion: SesionGym): Long

    @Update
    suspend fun actualizarSesion(sesion: SesionGym)

    @Delete
    suspend fun eliminarSesion(sesion: SesionGym)

    @Query("SELECT * FROM serie_realizada WHERE sesion_id = :sesionId ORDER BY completada_en")
    fun observarSeries(sesionId: Long): Flow<List<SerieRealizada>>

    @Query("SELECT * FROM serie_realizada WHERE sesion_id = :sesionId ORDER BY completada_en")
    suspend fun series(sesionId: Long): List<SerieRealizada>

    @Insert
    suspend fun insertarSerie(serie: SerieRealizada): Long

    @Delete
    suspend fun eliminarSerie(serie: SerieRealizada)

    // Respaldo: todo en bloque, conservando los ids

    @Query("SELECT * FROM ejercicio ORDER BY id")
    suspend fun todosLosEjercicios(): List<Ejercicio>

    @Query("SELECT * FROM sesion_gym ORDER BY id")
    suspend fun todasLasSesiones(): List<SesionGym>

    @Query("SELECT * FROM serie_realizada ORDER BY id")
    suspend fun todasLasSeries(): List<SerieRealizada>

    @Insert
    suspend fun insertarRutinas(rutinas: List<Rutina>)

    @Insert
    suspend fun insertarEjercicios(ejercicios: List<Ejercicio>)

    @Insert
    suspend fun insertarDias(dias: List<DiaGym>)

    @Insert
    suspend fun insertarSesiones(sesiones: List<SesionGym>)

    @Insert
    suspend fun insertarSeries(series: List<SerieRealizada>)

    /** Primero los hijos: las series y sesiones, después la programación, los ejercicios y las rutinas. */
    @Query("DELETE FROM serie_realizada")
    suspend fun vaciarSeries()

    @Query("DELETE FROM sesion_gym")
    suspend fun vaciarSesiones()

    @Query("DELETE FROM dia_gym")
    suspend fun vaciarDias()

    @Query("DELETE FROM ejercicio")
    suspend fun vaciarEjercicios()

    @Query("DELETE FROM rutina")
    suspend fun vaciarRutinas()

    // Estadísticas (RF-33)

    @Query("SELECT * FROM sesion_gym WHERE estado = 'TERMINADA' ORDER BY inicio")
    fun observarTerminadas(): Flow<List<SesionGym>>

    /** Las series de sesiones guardadas cuyo ejercicio todavía existe. */
    @Query(
        """
        SELECT serie_realizada.* FROM serie_realizada
        INNER JOIN sesion_gym ON sesion_gym.id = serie_realizada.sesion_id
        WHERE sesion_gym.estado = 'TERMINADA' AND serie_realizada.ejercicio_id IS NOT NULL
        """,
    )
    fun observarSeriesTerminadas(): Flow<List<SerieRealizada>>

    /** Lo que se hizo la última vez en esa serie de ese ejercicio, para prellenar (RF-28). */
    @Query(
        """
        SELECT serie_realizada.* FROM serie_realizada
        INNER JOIN sesion_gym ON sesion_gym.id = serie_realizada.sesion_id
        WHERE serie_realizada.ejercicio_id = :ejercicioId AND serie_realizada.numero_serie = :numero
          AND sesion_gym.estado = 'TERMINADA'
        ORDER BY sesion_gym.inicio DESC LIMIT 1
        """,
    )
    suspend fun ultimaSerie(ejercicioId: Long, numero: Int): SerieRealizada?
}
