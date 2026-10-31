package com.calendar.tasktodo.sistema.avisos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.calendar.tasktodo.MainActivity
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.ui.ESPANOL
import java.time.format.DateTimeFormatter

/** Canales de notificación (arquitectura §4.2). Los de gym llegan con su incremento. */
object Canales {
    const val ACTIVIDADES = "actividades"
    const val CLASES = "clases"
    const val RESUMEN = "resumen"
    const val RESPALDO = "respaldo"

    fun crear(context: Context) {
        val gestor = context.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannels(
            listOf(
                NotificationChannel(ACTIVIDADES, "Actividades", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Avisos de actividades y del día anterior a exámenes y simulacros" },
                NotificationChannel(CLASES, "Clases", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Aviso antes de cada clase" },
                NotificationChannel(RESUMEN, "Resumen del día", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Lo que hay para hoy y las tareas atrasadas" },
                NotificationChannel(RESPALDO, "Respaldo", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Respaldo automático fallido" },
            ),
        )
    }
}

class NotificadorAndroid(private val context: Context) : Notificador {

    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm", ESPANOL)

    override fun actividad(actividad: ActividadConMateria, diaAnterior: Boolean) {
        val datos = actividad.actividad
        val tipo = if (diaAnterior) TipoAviso.DIA_ANTERIOR else TipoAviso.ACTIVIDAD
        val codigo = AvisoProgramado.codigoDe(tipo, datos.id)
        val hora = datos.hora?.format(formatoHora)
        val cuando = if (diaAnterior) listOfNotNull("Mañana", hora).joinToString(" ") else hora ?: "Hoy"
        val texto = listOfNotNull(cuando, actividad.materiaNombre).joinToString(" · ")
        val marcar = PendingIntent.getBroadcast(
            context,
            codigo,
            Intent(context, AccionAvisoReceiver::class.java).putExtra(AccionAvisoReceiver.EXTRA_ID, datos.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mostrar(
            codigo,
            NotificationCompat.Builder(context, Canales.ACTIVIDADES)
                .setContentTitle(datos.titulo)
                .setContentText(texto)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(abrir(codigo, datos.id))
                .addAction(0, "Marcar como cumplida", marcar),
        )
    }

    override fun clase(clase: ClaseConMateria) {
        val codigo = AvisoProgramado.codigoDe(TipoAviso.CLASE, clase.clase.id)
        val texto = listOfNotNull("A las ${clase.clase.horaInicio.format(formatoHora)}", clase.clase.aula?.let { "Aula $it" })
            .joinToString(" · ")
        mostrar(
            codigo,
            NotificationCompat.Builder(context, Canales.CLASES)
                .setContentTitle(clase.materiaNombre)
                .setContentText(texto)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(abrir(codigo, null)),
        )
    }

    override fun resumen(delDia: List<ActividadConMateria>, atrasadas: List<ActividadConMateria>) {
        val codigo = AvisoProgramado.codigoDe(TipoAviso.RESUMEN, 0)
        val lineas = delDia.map { a -> listOfNotNull(a.actividad.hora?.format(formatoHora), a.actividad.titulo).joinToString(" ") } +
            atrasadas.map { "Atrasada: ${it.actividad.titulo}" }
        val titulo = buildList {
            if (delDia.isNotEmpty()) add(if (delDia.size == 1) "1 actividad hoy" else "${delDia.size} actividades hoy")
            if (atrasadas.isNotEmpty()) add(if (atrasadas.size == 1) "1 atrasada" else "${atrasadas.size} atrasadas")
        }.joinToString(" · ")
        val estilo = NotificationCompat.InboxStyle().also { e -> lineas.take(6).forEach(e::addLine) }
        mostrar(
            codigo,
            NotificationCompat.Builder(context, Canales.RESUMEN)
                .setContentTitle(titulo)
                .setContentText(lineas.firstOrNull().orEmpty())
                .setStyle(estilo)
                .setContentIntent(abrir(codigo, null)),
        )
    }

    override fun cerrarActividad(id: Long) {
        val gestor = NotificationManagerCompat.from(context)
        gestor.cancel(AvisoProgramado.codigoDe(TipoAviso.ACTIVIDAD, id))
        gestor.cancel(AvisoProgramado.codigoDe(TipoAviso.DIA_ANTERIOR, id))
    }

    private fun abrir(codigo: Int, actividadId: Long?): PendingIntent = PendingIntent.getActivity(
        context,
        codigo,
        MainActivity.intentDeActividad(context, actividadId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun mostrar(codigo: Int, constructor: NotificationCompat.Builder) {
        // En Android 13 o superior hace falta el permiso; en Android 12 no existe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(
            codigo,
            constructor.setSmallIcon(android.R.drawable.ic_popup_reminder).setAutoCancel(true).build(),
        )
    }
}
