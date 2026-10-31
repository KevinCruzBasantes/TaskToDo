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
import com.calendar.tasktodo.data.db.entidad.Materia

/**
 * Cada cambio de esquema sube [VERSION] y lleva su migración probada; nunca se usa
 * `fallbackToDestructiveMigration` (modelo de datos §5).
 *
 * - 1: materia, clase (incremento 1)
 * - 2: actividad (incremento 2)
 */
@Database(
    entities = [Materia::class, Clase::class, Actividad::class],
    version = AgendaDatabase.VERSION,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class AgendaDatabase : RoomDatabase() {

    abstract fun materiaDao(): MateriaDao

    abstract fun claseDao(): ClaseDao

    abstract fun actividadDao(): ActividadDao

    companion object {
        const val VERSION = 2
        const val NOMBRE = "agenda.db"

        fun crear(context: Context): AgendaDatabase =
            Room.databaseBuilder(context, AgendaDatabase::class.java, NOMBRE).build()
    }
}
