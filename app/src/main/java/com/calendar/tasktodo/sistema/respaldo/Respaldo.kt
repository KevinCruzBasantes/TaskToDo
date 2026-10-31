package com.calendar.tasktodo.sistema.respaldo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.calendar.tasktodo.TaskToDoApp
import com.calendar.tasktodo.data.ajustes.Frecuencia
import com.calendar.tasktodo.data.respaldo.AlmacenRespaldos
import com.calendar.tasktodo.data.respaldo.PlanificadorRespaldo
import com.calendar.tasktodo.data.respaldo.ResultadoRespaldo
import com.calendar.tasktodo.data.respaldo.TipoRespaldo
import com.calendar.tasktodo.sistema.avisos.Canales
import java.io.IOException
import java.util.concurrent.TimeUnit

/** La carpeta de respaldos con el Storage Access Framework (arquitectura §4.4). */
class AlmacenSaf(private val context: Context) : AlmacenRespaldos {

    private val resolver = context.contentResolver

    override fun registrar(carpeta: String) {
        resolver.takePersistableUriPermission(
            Uri.parse(carpeta),
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    override fun disponible(carpeta: String): Boolean {
        val uri = Uri.parse(carpeta)
        val conPermiso = resolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        return conPermiso && DocumentFile.fromTreeUri(context, uri)?.exists() == true
    }

    override fun nombre(carpeta: String): String? = DocumentFile.fromTreeUri(context, Uri.parse(carpeta))?.name

    override fun escribir(carpeta: String, nombre: String, contenido: String) {
        val directorio = DocumentFile.fromTreeUri(context, Uri.parse(carpeta)) ?: throw IOException("La carpeta no existe")
        // Con el tipo application/json, Android agrega la extensión .json
        val archivo = directorio.createFile("application/json", nombre.removeSuffix(".json"))
            ?: throw IOException("No se pudo crear el archivo")
        escribirEn(archivo.uri.toString(), contenido)
    }

    override fun listar(carpeta: String): List<Pair<String, String>> =
        DocumentFile.fromTreeUri(context, Uri.parse(carpeta))
            ?.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .mapNotNull { archivo -> archivo.name?.let { it to archivo.uri.toString() } }

    override fun borrar(uri: String) {
        DocumentFile.fromSingleUri(context, Uri.parse(uri))?.delete()
    }

    override fun leer(uri: String): String =
        resolver.openInputStream(Uri.parse(uri))?.bufferedReader()?.use { it.readText() }
            ?: throw IOException("No se pudo leer el archivo")

    override fun escribirEn(uri: String, contenido: String) {
        resolver.openOutputStream(Uri.parse(uri), "wt")?.use { it.write(contenido.toByteArray()) }
            ?: throw IOException("No se pudo escribir el archivo")
    }
}

/** Programa el respaldo automático con WorkManager (arquitectura §4.1). */
class PlanificadorRespaldoWork(context: Context) : PlanificadorRespaldo {

    private val trabajos = WorkManager.getInstance(context)

    override fun programar(frecuencia: Frecuencia) {
        trabajos.enqueueUniquePeriodicWork(
            PERIODICO,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<RespaldoAutomaticoWorker>(frecuencia.dias, TimeUnit.DAYS).build(),
        )
    }

    override fun cancelar() {
        trabajos.cancelUniqueWork(PERIODICO)
    }

    override fun ejecutarAhora() {
        trabajos.enqueueUniqueWork(ATRASADO, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<RespaldoAutomaticoWorker>().build())
    }

    private companion object {
        const val PERIODICO = "respaldo-automatico"
        const val ATRASADO = "respaldo-atrasado"
    }
}

/** Respaldo automático en la carpeta, conservando los últimos 5 (CU-24). */
class RespaldoAutomaticoWorker(context: Context, parametros: WorkerParameters) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val contenedor = (applicationContext as TaskToDoApp).contenedor
        return when (contenedor.respaldoRepository.respaldar(TipoRespaldo.AUTOMATICO)) {
            is ResultadoRespaldo.Exito -> Result.success()
            // Sin carpeta no sirve reintentar: se avisa para que la vuelva a elegir (CU-24 3a)
            ResultadoRespaldo.SinCarpeta -> {
                avisarSinCarpeta()
                Result.success()
            }
            is ResultadoRespaldo.Error -> Result.retry()
        }
    }

    private fun avisarSinCarpeta() {
        val notificador = NotificationManagerCompat.from(applicationContext)
        if (!notificador.areNotificationsEnabled()) return
        try {
            notificador.notify(
                AVISO_SIN_CARPETA,
                NotificationCompat.Builder(applicationContext, Canales.RESPALDO)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle("No se pudo hacer el respaldo automático")
                    .setContentText("Vuelve a elegir la carpeta en Ajustes → Respaldo.")
                    .setAutoCancel(true)
                    .build(),
            )
        } catch (_: SecurityException) {
            // Sin permiso de notificaciones en Android 13 o superior
        }
    }

    private companion object {
        const val AVISO_SIN_CARPETA = 9_000_001
    }
}
