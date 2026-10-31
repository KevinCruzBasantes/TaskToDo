package com.calendar.tasktodo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.calendar.tasktodo.data.db.dao.ClaseDao
import com.calendar.tasktodo.data.db.dao.MateriaDao
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.Materia

/**
 * Cada cambio de esquema sube [VERSION] y lleva su migración probada; nunca se usa
 * `fallbackToDestructiveMigration` (modelo de datos §5).
 */
@Database(
    entities = [Materia::class, Clase::class],
    version = AgendaDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AgendaDatabase : RoomDatabase() {

    abstract fun materiaDao(): MateriaDao

    abstract fun claseDao(): ClaseDao

    companion object {
        const val VERSION = 1
        const val NOMBRE = "agenda.db"

        fun crear(context: Context): AgendaDatabase =
            Room.databaseBuilder(context, AgendaDatabase::class.java, NOMBRE).build()
    }
}
