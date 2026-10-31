package com.calendar.tasktodo.sistema.compatibilidad

import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.data.respaldo.RespaldoRepository
import com.calendar.tasktodo.sistema.avisos.ServicioAvisos
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Lo que la guía de MIUI puede comprobar sola (arquitectura §4.6). */
interface EstadoSistema {
    val esXiaomi: Boolean

    fun alarmasExactas(): Boolean

    fun bateriaSinRestricciones(): Boolean

    fun notificacionesActivas(): Boolean
}

class EstadoSistemaAndroid(private val context: Context) : EstadoSistema {

    override val esXiaomi: Boolean = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    override fun alarmasExactas(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    override fun bateriaSinRestricciones(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    override fun notificacionesActivas(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
}

/** Abre el ajuste de MIUI o de Android que corresponde; si no existe, los detalles de la app. */
object AjustesFabricante {

    fun inicioAutomatico(context: Context) = abrir(
        context,
        Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        ),
    )

    fun bateria(context: Context) = abrir(
        context,
        Intent()
            .setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
            .putExtra("package_name", context.packageName)
            .putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager)),
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
    )

    fun notificaciones(context: Context) = abrir(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )

    fun alarmasExactas(context: Context) = abrir(
        context,
        *listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            } else {
                null
            },
        ).toTypedArray(),
    )

    private fun abrir(context: Context, vararg intentos: Intent) {
        val detalles = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        for (intento in intentos.toList() + detalles) {
            try {
                context.startActivity(intento.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
                // Esta versión de MIUI no tiene esa pantalla: se prueba la siguiente
            } catch (_: SecurityException) {
                // MIUI a veces protege su pantalla: se prueba la siguiente
            }
        }
    }
}

/**
 * Al pasar a primer plano, rehace lo que MIUI pudo haber impedido en segundo plano, como máximo
 * una vez por hora (diagramas de comportamiento §6).
 */
class PendientesAlAbrir(
    private val ajustes: AjustesRepository,
    private val avisos: ServicioAvisos,
    private val reloj: Clock,
    private val respaldo: RespaldoRepository? = null,
) {
    suspend fun ejecutar() {
        val ahora = Instant.now(reloj)
        val ultima = ajustes.ultimaComprobacion()
        if (ultima != null && Duration.between(ultima, ahora) < INTERVALO) return
        avisos.reprogramarTodo()
        respaldo?.ponerAlDia()
        ajustes.guardarUltimaComprobacion(ahora)
    }

    companion object {
        val INTERVALO: Duration = Duration.ofHours(1)
    }
}
