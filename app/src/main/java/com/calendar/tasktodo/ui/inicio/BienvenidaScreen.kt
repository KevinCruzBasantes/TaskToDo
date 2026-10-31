package com.calendar.tasktodo.ui.inicio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.componentes.PuntoColor
import com.calendar.tasktodo.ui.horario.textoClases

/** Configuración inicial (CU-01). */
@Composable
fun BienvenidaScreen(
    onNuevaMateria: () -> Unit,
    onEditarMateria: (Long) -> Unit,
    onAgregarClases: (Long) -> Unit,
    onTerminar: () -> Unit,
    onRestaurar: () -> Unit,
    viewModel: BienvenidaViewModel = viewModel(factory = BienvenidaViewModel.Factory),
) {
    val materias by viewModel.materias.collectAsStateWithLifecycle()
    var desdeCero by rememberSaveable { mutableStateOf(false) }

    // Primero la decisión que importa: empezar de cero o recuperar los datos (CU-01 2a)
    if (materias.isEmpty() && !desdeCero) {
        PreguntaInicial(onDesdeCero = { desdeCero = true }, onRestaurar = onRestaurar)
        return
    }

    Pantalla(enBarra = false) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text("TaskToDo", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Empecemos con tu horario. Agrega tus materias y en qué días y horas tienes clase.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(24.dp))
            if (materias.isNotEmpty()) {
                Text("Materias", style = MaterialTheme.typography.labelLarge)
            }
            LazyColumn(Modifier.weight(1f)) {
                items(materias, key = { it.materia.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.materia.nombre) },
                        supportingContent = { Text("${textoClases(item.clases)} · toca para agregar") },
                        leadingContent = { PuntoColor(item.materia.color, tamano = 20) },
                        trailingContent = {
                            IconButton(onClick = { onEditarMateria(item.materia.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar ${item.materia.nombre}")
                            }
                        },
                        modifier = Modifier.clickable { onAgregarClases(item.materia.id) },
                    )
                }
                item {
                    OutlinedButton(
                        onClick = onNuevaMateria,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Agregar materia", Modifier.padding(start = 8.dp))
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onTerminar) { Text("Omitir") }
                Button(onClick = onTerminar, enabled = materias.isNotEmpty()) { Text("Listo") }
            }
        }
    }
}

@Composable
private fun PreguntaInicial(onDesdeCero: () -> Unit, onRestaurar: () -> Unit) {
    Pantalla(enBarra = false) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("TaskToDo", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("¿Es la primera vez que usas la app?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(32.dp))
            Opcion("Empezar desde cero", "Agrega tus materias y tu horario.", onDesdeCero)
            Spacer(Modifier.height(16.dp))
            Opcion("Restaurar mis datos", "Reinstalé la app: elijo la carpeta de mis respaldos.", onRestaurar)
        }
    }
}

@Composable
private fun Opcion(titulo: String, detalle: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
