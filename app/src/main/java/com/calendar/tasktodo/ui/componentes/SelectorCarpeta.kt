package com.calendar.tasktodo.ui.componentes

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.calendar.tasktodo.ui.ESPANOL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** El selector de carpetas empieza en Documentos, donde se sugiere crear `AgendaRespaldos` (CU-23 4). */
private val DOCUMENTOS: Uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Documents")

/**
 * Abre el selector de carpetas de Android. Devuelve la función que lo abre; el resultado llega a
 * [onElegida], o a [onCancelada] si el usuario sale sin elegir.
 */
@Composable
fun rememberSelectorCarpeta(onElegida: (String) -> Unit, onCancelada: () -> Unit = {}): () -> Unit {
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onElegida(uri.toString()) else onCancelada()
    }
    return { lanzador.launch(DOCUMENTOS) }
}

private val FORMATO_MOMENTO = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", ESPANOL)

/** "vie 30 oct, 21:15" en la zona del celular. */
fun Instant.textoMomento(): String =
    LocalDateTime.ofInstant(this, ZoneId.systemDefault()).format(FORMATO_MOMENTO).replace(".", "")
