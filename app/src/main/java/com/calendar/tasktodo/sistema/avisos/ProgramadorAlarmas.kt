package com.calendar.tasktodo.sistema.avisos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.calendar.tasktodo.MainActivity

/** Programa los avisos con `AlarmManager` (arquitectura §4.2). */
class ProgramadorAlarmas(private val context: Context) : ProgramadorAvisos {

    private val alarmas = context.getSystemService(AlarmManager::class.java)

    override fun puedeExactas(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms()

    override fun programar(aviso: AvisoProgramado) {
        val disparo = intentDelAviso(aviso.tipo, aviso.refId)
        val momento = aviso.instante.toEpochMilli()
        try {
            when (elegirModo(aviso, puedeExactas())) {
                ModoAlarma.RELOJ -> alarmas.setAlarmClock(AlarmManager.AlarmClockInfo(momento, alTocarLaAlarma(aviso)), disparo)
                ModoAlarma.EXACTA -> alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, disparo)
                ModoAlarma.INEXACTA -> alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, disparo)
            }
        } catch (_: SecurityException) {
            // Se quitó el permiso entre la comprobación y la llamada: mejor tarde que nunca
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, disparo)
        }
    }

    override fun cancelar(codigo: Int) {
        // Un PendingIntent se identifica por su código y su componente, no por los extras
        PendingIntent.getBroadcast(
            context,
            codigo,
            Intent(context, AvisoReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let {
            alarmas.cancel(it)
            it.cancel()
        }
    }

    private fun intentDelAviso(tipo: TipoAviso, refId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        AvisoProgramado.codigoDe(tipo, refId),
        Intent(context, AvisoReceiver::class.java)
            .putExtra(AvisoReceiver.EXTRA_TIPO, tipo.name)
            .putExtra(AvisoReceiver.EXTRA_REF, refId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Al tocar la próxima alarma en la pantalla de bloqueo se abre el detalle de la actividad. */
    private fun alTocarLaAlarma(aviso: AvisoProgramado): PendingIntent = PendingIntent.getActivity(
        context,
        aviso.codigo,
        MainActivity.intentDeActividad(context, aviso.refId.takeIf { aviso.tipo.esDeActividad }),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

val TipoAviso.esDeActividad: Boolean get() = this == TipoAviso.ACTIVIDAD || this == TipoAviso.DIA_ANTERIOR
