package com.calendar.tasktodo.sistema.sesion

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.calendar.tasktodo.MainActivity
import com.calendar.tasktodo.TaskToDoApp
import com.calendar.tasktodo.data.db.entidad.SesionGym
import com.calendar.tasktodo.data.repositorio.Avance
import com.calendar.tasktodo.data.repositorio.MotorSesion
import com.calendar.tasktodo.data.repositorio.SesionRepository
import com.calendar.tasktodo.sistema.avisos.Canales
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant

/** Arranca el servicio de la sesión; la versión real usa un servicio en primer plano. */
interface ControlSesion {
    fun iniciar()

    /** Si hay una sesión en curso, vuelve a mostrar su notificación (RNF-07). */
    suspend fun asegurar()
}

class ControlSesionAndroid(
    private val context: Context,
    private val sesiones: SesionRepository,
) : ControlSesion {

    override fun iniciar() {
        ContextCompat.startForegroundService(context, Intent(context, SesionGymService::class.java))
    }

    override suspend fun asegurar() {
        if (sesiones.enCurso.first() != null) iniciar()
    }
}

/**
 * Mantiene la notificación fija de la sesión (RF-30). No cuenta el tiempo: la notificación usa el
 * cronómetro de Android desde el instante guardado, así sigue bien aunque Android cierre la app
 * (arquitectura §4.3). Se detiene solo cuando ya no hay sesión en curso.
 */
class SesionGymService : Service() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observacion: Job? = null

    private val sesiones get() = (application as TaskToDoApp).contenedor.sesionRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android exige mostrar la notificación enseguida después de iniciar el servicio
        entrarEnPrimerPlano(construir(null, null))
        if (intent?.action == ACCION_SALTAR_DESCANSO) {
            alcance.launch { sesiones.enCurso.first()?.let { sesiones.saltarDescanso(it) } }
        }
        if (observacion == null) observacion = alcance.launch { observar() }
        return START_STICKY
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observar() {
        sesiones.enCurso
            .flatMapLatest { sesion ->
                if (sesion == null) {
                    flowOf(null)
                } else {
                    val ejercicios = sesion.rutinaId?.let { sesiones.ejercicios(it) } ?: flowOf(emptyList())
                    combine(sesiones.series(sesion.id), ejercicios) { series, lista ->
                        sesion to MotorSesion.avance(sesion.ordenEjercicios, lista, series)
                    }
                }
            }
            .collect { estado ->
                if (estado == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val notificacion = construir(estado.first, estado.second)
                    try {
                        NotificationManagerCompat.from(this).notify(ID, notificacion)
                    } catch (_: SecurityException) {
                        // Sin permiso de notificaciones en Android 13 o superior: el servicio sigue
                    }
                }
            }
    }

    private fun entrarEnPrimerPlano(notificacion: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID, notificacion)
        }
    }

    private fun construir(sesion: SesionGym?, avance: Avance?): Notification {
        val constructor = NotificationCompat.Builder(this, Canales.SESION)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle("🏋 ${sesion?.nombreRutina ?: "Sesión de gym"}")
            .setContentIntent(
                PendingIntent.getActivity(
                    this, ID, MainActivity.intentDeSesion(this, sesion?.actividadId),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        if (sesion == null || avance == null) return constructor.setContentText("Sesión en curso").build()

        val actual = avance.actual
        val siguiente = actual?.let { "${it.nombre} · Serie ${avance.serieActual} de ${it.series}" } ?: "Sesión completa: toca para guardar"
        val descanso = sesion.descansoTerminaEn?.takeIf { it.isAfter(Instant.now()) }
        if (descanso != null) {
            // Durante el descanso, el cronómetro cuenta hacia atrás hasta su fin
            constructor
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(descanso.toEpochMilli())
                .setContentText("Descanso · Siguiente: $siguiente")
                .addAction(
                    0,
                    "Saltar descanso",
                    PendingIntent.getService(
                        this, ID + 1, Intent(this, SesionGymService::class.java).setAction(ACCION_SALTAR_DESCANSO),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
        } else {
            constructor
                .setUsesChronometer(true)
                .setWhen(sesion.inicio.toEpochMilli())
                .setContentText(siguiente)
        }
        return constructor.build()
    }

    override fun onDestroy() {
        alcance.cancel()
        super.onDestroy()
    }

    companion object {
        const val ID = 9_000_002
        const val ACCION_SALTAR_DESCANSO = "com.calendar.tasktodo.SALTAR_DESCANSO"
    }
}
