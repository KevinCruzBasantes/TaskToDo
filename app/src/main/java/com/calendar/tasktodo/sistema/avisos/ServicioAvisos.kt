package com.calendar.tasktodo.sistema.avisos

import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.data.db.dao.ActividadDao
import com.calendar.tasktodo.data.db.dao.ClaseDao
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.repositorio.ObservadorCambios
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

/** Programa y cancela alarmas. La versión real usa `AlarmManager`; las pruebas, una lista. */
interface ProgramadorAvisos {
    fun puedeExactas(): Boolean

    fun programar(aviso: AvisoProgramado)

    fun cancelar(codigo: Int)
}

/** Muestra las notificaciones. La versión real usa `NotificationManager`. */
interface Notificador {
    fun actividad(actividad: ActividadConMateria, diaAnterior: Boolean)

    fun clase(clase: ClaseConMateria)

    fun resumen(delDia: List<ActividadConMateria>, atrasadas: List<ActividadConMateria>)

    fun cerrarActividad(id: Long)
}

/**
 * Cada cambio recalcula todos los avisos de la ventana y reemplaza los anteriores por su código
 * fijo, así nunca quedan duplicados ni avisos viejos (diagramas de comportamiento §3).
 */
class ServicioAvisos(
    private val actividades: ActividadDao,
    private val clases: ClaseDao,
    private val ajustes: AjustesRepository,
    private val programador: ProgramadorAvisos,
    private val notificador: Notificador,
    private val reloj: Clock,
) : ObservadorCambios {

    private val candado = Mutex()

    override suspend fun huboCambios() = reprogramarTodo()

    suspend fun reprogramarTodo() = candado.withLock {
        val configuracion = ajustes.avisos.first()
        val ahora = ZonedDateTime.now(reloj)
        val hoy = ahora.toLocalDate()
        val pendientes = actividades.pendientesEntre(hoy, hoy.plusDays(PlanificadorAvisos.VENTANA_DIAS + 1))
        val todasLasClases = clases.observarTodas().first().map { it.clase }

        val avisos = PlanificadorAvisos.planificar(pendientes, todasLasClases, configuracion, ahora)
        val codigos = avisos.map { it.codigo }.toSet()

        (ajustes.codigosProgramados() - codigos).forEach(programador::cancelar)
        avisos.forEach(programador::programar)
        ajustes.guardarCodigosProgramados(codigos)
    }

    /** Cuando suena una alarma: vuelve a leer la base de datos antes de avisar (CU-20 2a). */
    suspend fun alSonar(tipo: TipoAviso, refId: Long) {
        val configuracion = ajustes.avisos.first()
        when (tipo) {
            TipoAviso.ACTIVIDAD, TipoAviso.DIA_ANTERIOR -> {
                val activado = if (tipo == TipoAviso.ACTIVIDAD) configuracion.actividades else configuracion.diaAnterior
                val actividad = actividades.obtenerConMateria(refId)
                if (activado && actividad?.actividad?.estado == EstadoActividad.PENDIENTE) {
                    notificador.actividad(actividad, diaAnterior = tipo == TipoAviso.DIA_ANTERIOR)
                }
            }
            TipoAviso.CLASE -> if (configuracion.clases) {
                clases.observar(refId).first()?.let(notificador::clase)
            }
            TipoAviso.RESUMEN -> if (configuracion.resumen) {
                val hoy = LocalDate.now(reloj)
                val delDia = actividades.delDia(hoy).filter { it.actividad.estado == EstadoActividad.PENDIENTE }
                val atrasadas = actividades.atrasadas(hoy)
                // Si no hay nada, no se envía (CU-20 1e)
                if (delDia.isNotEmpty() || atrasadas.isNotEmpty()) notificador.resumen(delDia, atrasadas)
            }
            TipoAviso.MANTENIMIENTO -> Unit
        }
        reprogramarTodo()
    }

    /** El botón "Marcar como cumplida" de la notificación (CU-08 1a). */
    suspend fun marcarDesdeNotificacion(id: Long) {
        actividades.cambiarEstado(id, EstadoActividad.CUMPLIDA)
        notificador.cerrarActividad(id)
        reprogramarTodo()
    }
}
