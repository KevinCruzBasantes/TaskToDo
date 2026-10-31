package com.calendar.tasktodo.ui.horario

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.ui.componentes.DialogoConfirmar
import com.calendar.tasktodo.ui.componentes.FilaActividad
import com.calendar.tasktodo.ui.componentes.FranjaMateria
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.nombre
import com.calendar.tasktodo.ui.rangoHoras
import com.calendar.tasktodo.ui.rememberFormatoHora
import com.calendar.tasktodo.ui.textoCorto

/** Detalle de una clase (CU-05), con editar y eliminar (CU-04). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleClaseScreen(
    id: Long,
    onAtras: () -> Unit,
    onEditar: (Long) -> Unit,
    onAbrirActividad: (Long) -> Unit,
    viewModel: DetalleClaseViewModel = viewModel(factory = DetalleClaseViewModel.factory(id)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    var confirmando by rememberSaveable { mutableStateOf(false) }

    // Si la clase ya no existe (se eliminó aquí o con su materia), se vuelve atrás
    LaunchedEffect(estado) { if (!estado.cargando && estado.clase == null) onAtras() }

    val clase = estado.clase
    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text(clase?.materiaNombre.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
                actions = {
                    if (clase != null) {
                        IconButton(onClick = { onEditar(id) }) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                        IconButton(onClick = { confirmando = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                    }
                },
            )
        },
    ) { modifier ->
        if (clase == null) return@Pantalla
        Column(modifier) {
        Row(
            Modifier
                .padding(16.dp)
                .height(IntrinsicSize.Min),
        ) {
            FranjaMateria(clase.materiaColor)
            Column(Modifier.padding(start = 16.dp)) {
                Text(clase.materiaNombre, style = MaterialTheme.typography.headlineSmall)
                Text(clase.clase.diaSemana.nombre(), style = MaterialTheme.typography.titleMedium)
                Text(
                    rangoHoras(clase.clase.horaInicio, clase.clase.horaFin, formatoHora),
                    style = MaterialTheme.typography.titleMedium,
                )
                clase.clase.aula?.let {
                    Text("Aula $it", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider()
        Text(
            "PRÓXIMAS ACTIVIDADES DE ${clase.materiaNombre.uppercase()}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        if (estado.proximas.isEmpty()) {
            Text(
                "No tiene actividades próximas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        estado.proximas.forEach { item ->
            FilaActividad(
                item, estado.hoy, formatoHora,
                onCambiarCumplida = { viewModel.cambiarCumplida(item.actividad.id, it) },
                onClick = { onAbrirActividad(item.actividad.id) },
                detalleExtra = item.actividad.fecha.textoCorto(),
            )
        }
        }
    }

    if (confirmando && clase != null) {
        DialogoConfirmar(
            titulo = "¿Eliminar esta clase?",
            texto = "Dejará de aparecer todos los ${clase.clase.diaSemana.nombre().lowercase()} en tu horario.",
            confirmar = "Eliminar",
            onConfirmar = {
                confirmando = false
                viewModel.eliminar()
            },
            onCancelar = { confirmando = false },
        )
    }
}
