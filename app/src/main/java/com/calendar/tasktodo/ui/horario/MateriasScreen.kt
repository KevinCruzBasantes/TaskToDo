package com.calendar.tasktodo.ui.horario

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.ui.componentes.DialogoConfirmar
import com.calendar.tasktodo.ui.componentes.EstadoVacio
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.componentes.PuntoColor
import com.calendar.tasktodo.ui.componentes.rememberSelectorCarpeta

/** Lista de materias (CU-02) y "Eliminar todas las materias" (CU-03). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriasScreen(
    onAtras: () -> Unit,
    onAbrirMateria: (Long) -> Unit,
    onNuevaMateria: () -> Unit,
    viewModel: MateriasViewModel = viewModel(factory = MateriasViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var confirmando by rememberSaveable { mutableStateOf(false) }
    val elegirCarpeta = rememberSelectorCarpeta(onElegida = viewModel::registrarCarpetaYSeguir)
    LaunchedEffect(estado.pedirCarpeta) {
        if (estado.pedirCarpeta) {
            viewModel.carpetaPedida()
            elegirCarpeta()
        }
    }

    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text("Materias") },
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaMateria) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar materia")
            }
        },
    ) { modifier ->
        when {
            estado.cargando -> Unit
            estado.materias.isEmpty() -> EstadoVacio(
                mensaje = "No tienes materias",
                accion = "Agregar materia",
                onAccion = onNuevaMateria,
                modifier = modifier,
            )
            else -> LazyColumn(modifier.fillMaxSize()) {
                items(estado.materias, key = { it.materia.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.materia.nombre) },
                        supportingContent = { Text(textoClases(item.clases)) },
                        leadingContent = { PuntoColor(item.materia.color, tamano = 20) },
                        modifier = Modifier.clickable { onAbrirMateria(item.materia.id) },
                    )
                }
                item {
                    TextButton(
                        onClick = { confirmando = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text("Eliminar todas las materias", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmando) {
        // Antes de borrar se ofrece un respaldo (CU-03 2)
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("¿Eliminar todas las materias?") },
            text = {
                Column {
                    Text(
                        "Se borrarán ${estado.materias.size} materias y ${textoClases(estado.totalClases)}. " +
                            "Tus actividades se conservarán sin materia.",
                    )
                    TextButton(onClick = {
                        confirmando = false
                        viewModel.respaldarYEliminar()
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Respaldar y eliminar") }
                    TextButton(onClick = {
                        confirmando = false
                        viewModel.eliminarTodas()
                    }) { Text("Eliminar sin respaldo", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text("Cancelar") } },
        )
    }
    if (estado.respaldoFallido) {
        DialogoConfirmar(
            titulo = "No se pudo crear el respaldo",
            texto = "¿Eliminar todas las materias de todas formas?",
            confirmar = "Eliminar sin respaldo",
            onConfirmar = {
                viewModel.cerrarFallo()
                viewModel.eliminarTodas()
            },
            onCancelar = viewModel::cerrarFallo,
        )
    }
    estado.mensaje?.let { mensaje ->
        AlertDialog(
            onDismissRequest = viewModel::mensajeMostrado,
            text = { Text(mensaje) },
            confirmButton = { TextButton(onClick = viewModel::mensajeMostrado) { Text("Aceptar") } },
        )
    }
}

fun textoClases(cantidad: Int): String = if (cantidad == 1) "1 clase" else "$cantidad clases"
