package com.calendar.tasktodo.ui.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.data.ajustes.Frecuencia
import com.calendar.tasktodo.data.respaldo.ArchivoRespaldo
import com.calendar.tasktodo.data.respaldo.LecturaRespaldo
import com.calendar.tasktodo.data.respaldo.RespaldoRepository
import com.calendar.tasktodo.data.respaldo.TipoRespaldo
import com.calendar.tasktodo.ui.componentes.rememberSelectorCarpeta
import com.calendar.tasktodo.ui.componentes.textoMomento
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Respaldo (CU-21 a CU-23, diseño de interfaz §3.14). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RespaldoScreen(
    onAtras: () -> Unit,
    onRestaurado: () -> Unit,
    /** Al venir de "Restaurar mis datos": pide la carpeta si falta y muestra la lista (CU-01 2a). */
    iniciarRestauracion: Boolean = false,
    viewModel: RespaldoViewModel = viewModel(factory = RespaldoViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    val elegirCarpeta = rememberSelectorCarpeta(onElegida = viewModel::registrarCarpeta, onCancelada = viewModel::carpetaCancelada)
    val guardarCopia = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.copiarA(it.toString()) }
    }
    val buscarArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.elegirArchivo(it.toString()) }
    }

    LaunchedEffect(estado.pedirCarpeta) {
        if (estado.pedirCarpeta) {
            viewModel.carpetaPedida()
            elegirCarpeta()
        }
    }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            avisos.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }
    LaunchedEffect(estado.restaurado) { if (estado.restaurado) onRestaurado() }
    var restauracionIniciada by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(estado.cargando) {
        if (iniciarRestauracion && !estado.cargando && !restauracionIniciada) {
            restauracionIniciada = true
            viewModel.abrirRestaurar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Respaldo") },
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
        snackbarHost = { SnackbarHost(avisos) },
    ) { padding ->
        if (estado.cargando) return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            AdvertenciaRespaldo(estado.diasSinRespaldo, hayRespaldo = estado.ultimo != null)

            Text("CARPETA DE RESPALDOS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (estado.carpetaDisponible) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📁 ${estado.nombreCarpeta ?: "Carpeta registrada"}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::elegirCarpeta) { Text("Cambiar") }
                }
            } else {
                Text(
                    "Elige dónde guardar tus respaldos. Te sugerimos crear una carpeta AgendaRespaldos dentro de Documentos.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = viewModel::elegirCarpeta, modifier = Modifier.padding(top = 8.dp)) { Text("Elegir carpeta") }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = viewModel::respaldarAhora,
                enabled = !estado.ocupado,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) { Text("Respaldar ahora") }
            Text(
                estado.ultimo?.let { "Último: ${it.textoMomento()}" } ?: "Aún no hay respaldos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text("Restaurar respaldo") },
                supportingContent = { Text("Reemplaza tus datos actuales por los de un respaldo") },
                modifier = Modifier.clickable(enabled = !estado.ocupado, onClick = viewModel::abrirRestaurar),
            )
            ListItem(
                headlineContent = { Text("Guardar una copia en otra ubicación") },
                supportingContent = { Text("Por ejemplo, Google Drive") },
                modifier = Modifier.clickable(enabled = !estado.ocupado) {
                    guardarCopia.launch("agenda-copia-${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.json")
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text("Respaldo automático") },
                supportingContent = { Text("Conserva los últimos 5") },
                trailingContent = { Switch(checked = estado.automatico, onCheckedChange = viewModel::cambiarAutomatico) },
            )
            if (estado.automatico) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                    Frecuencia.entries.forEach { frecuencia ->
                        FilterChip(
                            selected = estado.frecuencia == frecuencia,
                            onClick = { viewModel.cambiarFrecuencia(frecuencia) },
                            label = { Text(frecuencia.texto()) },
                        )
                    }
                }
            }
        }
    }

    estado.archivos?.let { archivos ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarLista,
            title = { Text("Restaurar respaldo") },
            text = {
                Column {
                    if (archivos.isEmpty()) Text("La carpeta no tiene respaldos.")
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(archivos, key = { it.uri }) { archivo ->
                            ListItem(
                                headlineContent = { Text(archivo.textoFecha()) },
                                supportingContent = { Text(archivo.tipo.texto()) },
                                modifier = Modifier.clickable { viewModel.elegirArchivo(archivo.uri) },
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.cerrarLista()
                            buscarArchivo.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) { Text("Buscar en otra ubicación") }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::cerrarLista) { Text("Cerrar") } },
        )
    }

    estado.lectura?.let { lectura ->
        when (lectura) {
            is LecturaRespaldo.Valido -> {
                val r = lectura.resumen
                AlertDialog(
                    onDismissRequest = viewModel::cancelarLectura,
                    title = { Text("¿Restaurar este respaldo?") },
                    text = {
                        Text(
                            "Respaldo del ${r.fecha.textoMomento()} (versión ${r.versionApp}).\n" +
                                "${r.materias} materias, ${r.clases} clases y ${r.actividades} actividades.\n\n" +
                                "Se reemplazarán todos tus datos actuales. Antes se guardará una copia de seguridad en la carpeta.",
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::confirmarRestaurar, enabled = !estado.ocupado) {
                            Text("Restaurar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = { TextButton(onClick = viewModel::cancelarLectura) { Text("Cancelar") } },
                )
            }
            LecturaRespaldo.Invalido -> Informar("Archivo no válido", "El archivo no es un respaldo de TaskToDo o está dañado. No se cambió nada.", viewModel::cancelarLectura)
            is LecturaRespaldo.MasNuevo -> Informar(
                "Respaldo más nuevo",
                "Este respaldo es de una versión más nueva de la app. Actualiza la app para restaurarlo. No se cambió nada.",
                viewModel::cancelarLectura,
            )
        }
    }
}

@Composable
private fun Informar(titulo: String, texto: String, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido") } },
    )
}

/** Más de una semana sin respaldo, o ninguno todavía (RF-47). */
@Composable
fun AdvertenciaRespaldo(dias: Long?, hayRespaldo: Boolean, onRespaldar: (() -> Unit)? = null) {
    val texto = when {
        !hayRespaldo -> "Aún no tienes ningún respaldo."
        dias != null && dias > RespaldoRepository.DIAS_ADVERTENCIA -> "Han pasado $dias días sin respaldo."
        else -> return
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠ $texto", Modifier.weight(1f))
            onRespaldar?.let { TextButton(onClick = it) { Text("Respaldar") } }
        }
    }
}

private fun Frecuencia.texto() = when (this) {
    Frecuencia.DIARIA -> "Diario"
    Frecuencia.SEMANAL -> "Semanal"
    Frecuencia.MENSUAL -> "Mensual"
}

private fun TipoRespaldo.texto() = when (this) {
    TipoRespaldo.MANUAL -> "Manual"
    TipoRespaldo.AUTOMATICO -> "Automático"
    TipoRespaldo.SEGURIDAD -> "Seguridad (antes de restaurar)"
}

private fun ArchivoRespaldo.textoFecha(): String =
    momento?.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", com.calendar.tasktodo.ui.ESPANOL))?.replace(".", "") ?: nombre
