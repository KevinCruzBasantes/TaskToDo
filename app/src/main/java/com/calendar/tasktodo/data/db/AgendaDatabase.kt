package com.calendar.tasktodo.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.calendar.tasktodo.data.db.dao.ActividadDao
import com.calendar.tasktodo.data.db.dao.ClaseDao
import com.calendar.tasktodo.data.db.dao.MateriaDao
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.DiaGym
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.SerieRealizada
import com.calendar.tasktodo.data.db.entidad.SesionGym
import com.calendar.tasktodo.data.db.dao.GymDao
import com.calendar.tasktodo.data.db.dao.PesoDao
import com.calendar.tasktodo.data.db.entidad.RegistroPeso

/**
 * Cada cambio de esquema sube [VERSION] y lleva su migración probada; nunca se usa
 * `fallbackToDestructiveMigration` (modelo de datos §5).
 *
 * - 1: materia, clase (incremento 1)
 * - 2: actividad (incremento 2)
 * - 3: rutina, ejercicio, dia_gym, sesion_gym, serie_realizada y las columnas de gym de
 *   actividad (incremento 5); Room recrea `actividad` para agregar sus claves foráneas
 * - 4: registro_peso (incremento 6)
 */
@Database(
    entities = [
        Materia::class, Clase::class, Actividad::class,
        Rutina::class, Ejercicio::class, DiaGym::class, SesionGym::class, SerieRealizada::class,
        RegistroPeso::class,
    ],
    version = AgendaDatabase.VERSION,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
@TypeConverters(Converters::class)
abstract class AgendaDatabase : RoomDatabase() {

    abstract fun materiaDao(): MateriaDao

    abstract fun claseDao(): ClaseDao

    abstract fun actividadDao(): ActividadDao

    abstract fun gymDao(): GymDao

    abstract fun pesoDao(): PesoDao

    companion object {
        const val VERSION = 4
        const val NOMBRE = "agenda.db"

        fun crear(context: Context): AgendaDatabase =
            Room.databaseBuilder(context, AgendaDatabase::class.java, NOMBRE).build()
    }
}
