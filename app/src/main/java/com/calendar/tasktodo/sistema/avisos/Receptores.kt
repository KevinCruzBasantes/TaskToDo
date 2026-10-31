package com.calendar.tasktodo.sistema.avisos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calendar.tasktodo.TaskToDoApp
import com.calendar.tasktodo.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private fun Context.contenedor(): AppContainer = (applicationContext as TaskToDoApp).contenedor

/** Termina el trabajo con la base de datos sin abrir la app (diagramas de comportamiento §3.4). */
private fun BroadcastReceiver.enSegundoPlano(trabajo: suspend () -> Unit) {
    val pendiente = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            trabajo()
        } finally {
            pendiente.finish()
        }
    }
}

/** Lo despierta `AlarmManager` (CU-20). */
class AvisoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tipo = intent.getStringExtra(EXTRA_TIPO)?.let { nombre -> TipoAviso.entries.firstOrNull { it.name == nombre } } ?: return
        val refId = intent.getLongExtra(EXTRA_REF, 0)
        enSegundoPlano { context.contenedor().servicioAvisos.alSonar(tipo, refId) }
    }

    companion object {
        const val EXTRA_TIPO = "tipo"
        const val EXTRA_REF = "ref"
    }
}

/** El botón "Marcar como cumplida" de la notificación (CU-08 1a). */
class AccionAvisoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, 0).takeIf { it > 0 } ?: return
        enSegundoPlano { context.contenedor().servicioAvisos.marcarDesdeNotificacion(id) }
    }

    companion object {
        const val EXTRA_ID = "actividad"
    }
}

/** Reinicio, cambio de hora o de zona, o actualización de la app: se reprograma todo (CU-20 1f). */
class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACCIONES) return
        enSegundoPlano { context.contenedor().servicioAvisos.reprogramarTodo() }
    }

    private companion object {
        val ACCIONES = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
