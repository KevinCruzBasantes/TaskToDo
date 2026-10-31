package com.calendar.tasktodo.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.sistema.avisos.NotificadorAndroid
import com.calendar.tasktodo.sistema.avisos.ProgramadorAlarmas
import com.calendar.tasktodo.sistema.avisos.ServicioAvisos
import com.calendar.tasktodo.sistema.compatibilidad.EstadoSistema
import com.calendar.tasktodo.sistema.compatibilidad.EstadoSistemaAndroid
import com.calendar.tasktodo.sistema.compatibilidad.PendientesAlAbrir
import java.time.Clock

private val Context.almacenAjustes by preferencesDataStore(name = "ajustes")

/** Crea una sola vez la base de datos, los repositorios y los componentes del sistema (arquitectura §2). */
class AppContainer(context: Context) {

    /** Ninguna clase llama a `LocalDate.now()` sin reloj: así las pruebas fijan "hoy". */
    val reloj: Clock = Clock.systemDefaultZone()

    private val baseDeDatos = AgendaDatabase.crear(context)

    val ajustesRepository = AjustesRepository(context.almacenAjustes)

    val estadoSistema: EstadoSistema = EstadoSistemaAndroid(context)

    val servicioAvisos = ServicioAvisos(
        actividades = baseDeDatos.actividadDao(),
        clases = baseDeDatos.claseDao(),
        ajustes = ajustesRepository,
        programador = ProgramadorAlarmas(context),
        notificador = NotificadorAndroid(context),
        reloj = reloj,
    )

    val horarioRepository = HorarioRepository(baseDeDatos.materiaDao(), baseDeDatos.claseDao(), servicioAvisos)

    val actividadRepository = ActividadRepository(baseDeDatos.actividadDao(), servicioAvisos)

    val pendientesAlAbrir = PendientesAlAbrir(ajustesRepository, servicioAvisos, reloj)
}
