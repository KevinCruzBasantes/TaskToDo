package com.calendar.tasktodo.ui.horario

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.ui.componentes.DialogoConfirmar
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.componentes.SelectorColorMateria

/** Formulario de materia (CU-02). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormMateriaScreen(
    id: Long?,
    onTerminar: () -> Unit,
    viewModel: FormMateriaViewModel = viewModel(factory = FormMateriaViewModel.factory(id)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var confirmando by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado.terminado) { if (estado.terminado) onTerminar() }

    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Editar materia" else "Nueva materia") },
                navigationIcon = {
                    IconButton(onClick = onTerminar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") }
                },
                actions = {
                    TextButton(onClick = viewModel::guardar, enabled = !estado.cargando && !estado.ocupado) {
                        Text("Guardar")
                    }
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
                value = estado.nombre,
                onValueChange = viewModel::cambiarNombre,
                label = { Text("Nombre") },
                singleLine = true,
                isError = estado.error != null,
                supportingText = estado.error?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text("Color", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            SelectorColorMateria(seleccionado = estado.color, onSeleccionar = viewModel::cambiarColor)
            if (estado.esEdicion) {
                Spacer(Modifier.height(32.dp))
                OutlinedButton(
                    onClick = { confirmando = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Eliminar materia") }
            }
        }
    }

    if (confirmando) {
        DialogoConfirmar(
            titulo = "¿Eliminar ${estado.nombre.trim()}?",
            texto = "Se borrarán sus ${textoClases(estado.clasesDeLaMateria)}. Sus actividades se conservarán sin materia.",
            confirmar = "Eliminar",
            onConfirmar = {
                confirmando = false
                viewModel.eliminar()
            },
            onCancelar = { confirmando = false },
        )
    }
}
