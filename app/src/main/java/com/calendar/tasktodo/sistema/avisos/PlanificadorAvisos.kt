package com.calendar.tasktodo.sistema.avisos

import com.calendar.tasktodo.data.ajustes.AjustesAvisos
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.tieneAvisoDiaAnterior
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** El dígito final del código de alarma: un aviso siempre tiene el mismo código (diagramas §3.1). */
enum class TipoAviso(val digito: Int) {
    ACTIVIDAD(0),
    DIA_ANTERIOR(1),
    CLASE(2),
    RESUMEN(3),

    /** Alarma silenciosa diaria que solo vuelve a calcular la ventana. */
    MANTENIMIENTO(9),
}

data class AvisoProgramado(
    val tipo: TipoAviso,
    /** Id de la actividad o de la clase; 0 en el resumen y el mantenimiento. */
    val refId: Long,
    val instante: Instant,
    val prioridad: Boolean,
) {
    val codigo: Int get() = codigoDe(tipo, refId)

    companion object {
        fun codigoDe(tipo: TipoAviso, refId: Long): Int = (refId * 10 + tipo.digito).toInt()
    }
}

enum class ModoAlarma {
    /** `setAlarmClock`: la alarma del sistema, no se retrasa en reposo (RNF-10). */
    RELOJ,

    /** `setExactAndAllowWhileIdle`. */
    EXACTA,

    /** `setAndAllowWhileIdle`: sin permiso de alarmas exactas, o para el mantenimiento. */
    INEXACTA,
}

fun elegirModo(aviso: AvisoProgramado, puedeExactas: Boolean): ModoAlarma = when {
    aviso.tipo == TipoAviso.MANTENIMIENTO || !puedeExactas -> ModoAlarma.INEXACTA
    aviso.prioridad -> ModoAlarma.RELOJ
    else -> ModoAlarma.EXACTA
}

/**
 * Qué avisos tocan en los próximos [VENTANA_DIAS] días. Es una función pura: el mismo estado
 * siempre da los mismos avisos, así que recalcular todo es seguro (diagramas de comportamiento §3).
 */
object PlanificadorAvisos {

    const val VENTANA_DIAS = 7L
    val HORA_MANTENIMIENTO: LocalTime = LocalTime.of(3, 0)

    fun planificar(
        actividades: List<Actividad>,
        clases: List<Clase>,
        ajustes: AjustesAvisos,
        ahora: ZonedDateTime,
    ): List<AvisoProgramado> {
        val limite = ahora.plusDays(VENTANA_DIAS)
        val hoy = ahora.toLocalDate()
        fun en(fecha: LocalDate, hora: LocalTime) = ZonedDateTime.of(fecha, hora, ahora.zone)
        fun dentro(momento: ZonedDateTime) = momento.isAfter(ahora) && momento.isBefore(limite)
        fun aviso(tipo: TipoAviso, refId: Long, momento: ZonedDateTime, prioridad: Boolean = ajustes.prioridadAlarma) =
            AvisoProgramado(tipo, refId, momento.toInstant(), prioridad)

        val avisos = mutableListOf<AvisoProgramado>()

        actividades.filter { it.estado == EstadoActividad.PENDIENTE }.forEach { actividad ->
            val minutos = actividad.avisoMinutosAntes
            if (ajustes.actividades && minutos != null) {
                // Sin hora, el aviso va a la hora fija de los ajustes (RF-38)
                val momento = actividad.hora?.let { en(actividad.fecha, it).minusMinutes(minutos.toLong()) }
                    ?: en(actividad.fecha, ajustes.horaSinHora)
                if (dentro(momento)) avisos += aviso(TipoAviso.ACTIVIDAD, actividad.id, momento)
            }
            if (ajustes.diaAnterior && actividad.tipo.tieneAvisoDiaAnterior) {
                val momento = en(actividad.fecha.minusDays(1), ajustes.horaDiaAnterior)
                if (dentro(momento)) avisos += aviso(TipoAviso.DIA_ANTERIOR, actividad.id, momento)
            }
        }

        if (ajustes.clases) {
            clases.forEach { clase ->
                // La próxima vez que toca esta clase; en una ventana de 7 días hay a lo sumo una
                (0..VENTANA_DIAS).map { hoy.plusDays(it) }
                    .filter { it.dayOfWeek == clase.diaSemana }
                    .map { en(it, clase.horaInicio).minusMinutes(ajustes.minutosClase.toLong()) }
                    .firstOrNull(::dentro)
                    ?.let { avisos += aviso(TipoAviso.CLASE, clase.id, it) }
            }
        }

        if (ajustes.resumen) avisos += aviso(TipoAviso.RESUMEN, 0, siguiente(ahora, ajustes.horaResumen))
        avisos += aviso(TipoAviso.MANTENIMIENTO, 0, siguiente(ahora, HORA_MANTENIMIENTO), prioridad = false)

        return avisos.sortedBy { it.instante }
    }

    /** La próxima vez que el reloj marca [hora]: hoy si aún no pasó, si no mañana. */
    private fun siguiente(ahora: ZonedDateTime, hora: LocalTime): ZonedDateTime {
        val hoy = ZonedDateTime.of(ahora.toLocalDate(), hora, ahora.zone)
        return if (hoy.isAfter(ahora)) hoy else hoy.plusDays(1)
    }
}
