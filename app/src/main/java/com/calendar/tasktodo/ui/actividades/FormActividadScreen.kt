package com.calendar.tasktodo.ui.actividades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.db.entidad.tieneAvisoDiaAnterior
import com.calendar.tasktodo.ui.OPCIONES_AVISO
import com.calendar.tasktodo.ui.componentes.CampoHoraOpcional
import com.calendar.tasktodo.ui.componentes.DialogoConfirmar
import com.calendar.tasktodo.ui.componentes.DialogoFecha
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.componentes.PuntoColor
import com.calendar.tasktodo.ui.icono
import com.calendar.tasktodo.ui.nombre
import com.calendar.tasktodo.ui.rememberFormatoHora
import com.calendar.tasktodo.ui.textoAviso
import com.calendar.tasktodo.ui.textoCorto
import java.time.LocalDate

/** Formulario de actividad (CU-06). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormActividadScreen(
    id: Long?,
    fecha: LocalDate?,
    onTerminar: () -> Unit,
    viewModel: FormActividadViewModel = viewModel(factory = FormActividadViewModel.factory(id, fecha)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    var eligiendoFecha by rememberSaveable { mutableStateOf(false) }
    var confirmando by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado.terminado) { if (estado.terminado) onTerminar() }

    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Editar actividad" else "Nueva actividad") },
                navigationIcon = {
                    IconButton(onClick = onTerminar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") }
                },
                actions = {
                    TextButton(onClick = viewModel::guardar, enabled = !estado.cargando && !estado.ocupado) { Text("Guardar") }
                },
            )
        },
    ) { modifier ->
        if (estado.cargando) return@Pantalla
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = estado.titulo,
                onValueChange = viewModel::cambiarTitulo,
                label = { Text("Título") },
                singleLine = true,
                isError = estado.errorTitulo != null,
                supportingText = estado.errorTitulo?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Tipo", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // El tipo gym no se ofrece: esas actividades salen de la programación (CU-06 2a)
                TipoActividad.entries.filter { it != TipoActividad.GYM }.forEach { tipo ->
                    FilterChip(
                        selected = estado.tipo == tipo,
                        onClick = { viewModel.cambiarTipo(tipo) },
                        label = { Text("${tipo.icono()} ${tipo.nombre()}") },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Fecha", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = { eligiendoFecha = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(estado.fecha.textoCorto(), style = MaterialTheme.typography.titleMedium)
                    }
                }
                CampoHoraOpcional("Hora", estado.hora, formatoHora, viewModel::cambiarHora, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            SelectorMateriaOpcional(estado, viewModel::cambiarMateria)
            Spacer(Modifier.height(8.dp))
            SelectorAviso(estado, formatoHora, viewModel::cambiarAviso)
            val ajustes = estado.ajustesAvisos
            if (estado.tipo.tieneAvisoDiaAnterior && ajustes.diaAnterior) {
                Text(
                    "También se avisa el día anterior a las ${ajustes.horaDiaAnterior.format(formatoHora)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = estado.notas,
                onValueChange = viewModel::cambiarNotas,
                label = { Text("Notas") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            if (estado.esEdicion) {
                Spacer(Modifier.height(32.dp))
                OutlinedButton(
                    onClick = { confirmando = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Eliminar actividad") }
            }
        }
    }

    if (eligiendoFecha) {
        DialogoFecha(
            inicial = estado.fecha,
            onElegir = {
                viewModel.cambiarFecha(it)
                eligiendoFecha = false
            },
            onCancelar = { eligiendoFecha = false },
        )
    }
    if (confirmando) {
        DialogoConfirmar(
            titulo = "¿Eliminar ${estado.titulo.trim()}?",
            texto = "La actividad y su aviso se eliminarán.",
            confirmar = "Eliminar",
            onConfirmar = {
                confirmando = false
                viewModel.eliminar()
            },
            onCancelar = { confirmando = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorMateriaOpcional(estado: FormActividadUiState, onElegir: (Long?) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val elegida = estado.materias.firstOrNull { it.id == estado.materiaId }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = elegida?.nombre ?: "Sin materia",
            onValueChange = {},
            readOnly = true,
            label = { Text("Materia") },
            leadingIcon = elegida?.let { { PuntoColor(it.color) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(text = { Text("Sin materia") }, onClick = {
                onElegir(null)
                abierto = false
            })
            estado.materias.forEach { materia ->
                DropdownMenuItem(
                    text = { Text(materia.nombre) },
                    leadingIcon = { PuntoColor(materia.color) },
                    onClick = {
                        onElegir(materia.id)
                        abierto = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorAviso(
    estado: FormActividadUiState,
    formatoHora: java.time.format.DateTimeFormatter,
    onElegir: (Int?) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    val conHora = estado.hora != null
    fun texto(minutos: Int?) = textoAviso(minutos, conHora, estado.ajustesAvisos.horaSinHora, formatoHora)
    // Sin hora no hay "minutos antes": solo con aviso (a la hora fija) o sin aviso
    val opciones = if (conHora) OPCIONES_AVISO else listOf(null, 0)
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = "🔔 ${texto(estado.avisoMinutos)}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Aviso") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { minutos ->
                DropdownMenuItem(text = { Text(texto(minutos)) }, onClick = {
                    onElegir(minutos)
                    abierto = false
                })
            }
        }
    }
}
