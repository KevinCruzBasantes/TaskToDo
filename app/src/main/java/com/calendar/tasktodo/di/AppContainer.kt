package com.calendar.tasktodo.di

import android.content.Context
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import java.time.Clock

/** Crea una sola vez la base de datos y los repositorios (arquitectura §2). */
class AppContainer(context: Context) {

    /** Ninguna clase llama a `LocalDate.now()` sin reloj: así las pruebas fijan "hoy". */
    val reloj: Clock = Clock.systemDefaultZone()

    private val baseDeDatos = AgendaDatabase.crear(context)

    val horarioRepository = HorarioRepository(baseDeDatos.materiaDao(), baseDeDatos.claseDao())
}
