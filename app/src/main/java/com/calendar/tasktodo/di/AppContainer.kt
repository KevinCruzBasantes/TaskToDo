package com.calendar.tasktodo.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.calendar.tasktodo.data.ajustes.AjustesRepository
import androidx.room.withTransaction
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.GymRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.data.repositorio.SesionRepository
import com.calendar.tasktodo.data.respaldo.RespaldoRepository
import com.calendar.tasktodo.sistema.avisos.NotificadorAndroid
import com.calendar.tasktodo.sistema.avisos.ProgramadorAlarmas
import com.calendar.tasktodo.sistema.avisos.ServicioAvisos
import com.calendar.tasktodo.sistema.compatibilidad.EstadoSistema
import com.calendar.tasktodo.sistema.compatibilidad.EstadoSistemaAndroid
import com.calendar.tasktodo.sistema.compatibilidad.PendientesAlAbrir
import com.calendar.tasktodo.sistema.respaldo.AlmacenSaf
import com.calendar.tasktodo.sistema.sesion.ControlSesion
import com.calendar.tasktodo.sistema.sesion.ControlSesionAndroid
import com.calendar.tasktodo.sistema.respaldo.PlanificadorRespaldoWork
import java.time.Clock

private val Context.almacenAjustes by preferencesDataStore(name = "ajustes")

/** Crea una sola vez la base de datos, los repositorios y los componentes del sistema (arquitectura §2). */
class AppContainer(context: Context) {

    /** Ninguna clase llama a `LocalDate.now()` sin reloj: así las pruebas fijan "hoy". */
    val reloj: Clock = Clock.systemDefaultZone()

    private val baseDeDatos = AgendaDatabase.crear(context)

    val ajustesRepository = AjustesRepository(context.almacenAjustes)

    val estadoSistema: EstadoSistema = EstadoSistemaAndroid(context)

    val servicioAvisos: ServicioAvisos = ServicioAvisos(
        actividades = baseDeDatos.actividadDao(),
        clases = baseDeDatos.claseDao(),
        ajustes = ajustesRepository,
        programador = ProgramadorAlarmas(context),
        notificador = NotificadorAndroid(context),
        reloj = reloj,
        gym = baseDeDatos.gymDao(),
        alMantenimiento = { gymRepository.generar() },
    )

    val horarioRepository = HorarioRepository(baseDeDatos.materiaDao(), baseDeDatos.claseDao(), servicioAvisos)

    val actividadRepository = ActividadRepository(baseDeDatos.actividadDao(), servicioAvisos)

    val gymRepository: GymRepository = GymRepository(
        dao = baseDeDatos.gymDao(),
        ajustes = ajustesRepository,
        reloj = reloj,
        observador = servicioAvisos,
        transaccion = { bloque -> baseDeDatos.withTransaction { bloque() } },
    )

    val sesionRepository = SesionRepository(baseDeDatos.gymDao(), baseDeDatos.actividadDao(), reloj, servicioAvisos)

    val controlSesion: ControlSesion = ControlSesionAndroid(context, sesionRepository)

    val respaldoRepository = RespaldoRepository(
        materias = baseDeDatos.materiaDao(),
        clases = baseDeDatos.claseDao(),
        actividades = baseDeDatos.actividadDao(),
        gym = baseDeDatos.gymDao(),
        ajustes = ajustesRepository,
        almacen = AlmacenSaf(context),
        planificador = PlanificadorRespaldoWork(context),
        transaccion = { bloque -> baseDeDatos.withTransaction { bloque() } },
        reloj = reloj,
        versionApp = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
        observador = servicioAvisos,
    )

    val pendientesAlAbrir = PendientesAlAbrir(ajustesRepository, servicioAvisos, reloj, respaldoRepository, gymRepository)
}
