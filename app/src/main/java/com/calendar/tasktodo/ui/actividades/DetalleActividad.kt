package com.calendar.tasktodo.ui.actividades

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.Situacion
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.db.entidad.situacion
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.ui.componentes.DialogoConfirmar
import com.calendar.tasktodo.ui.componentes.FranjaMateria
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.contenedor
import com.calendar.tasktodo.ui.icono
import com.calendar.tasktodo.ui.nombre
import com.calendar.tasktodo.ui.rememberFormatoHora
import com.calendar.tasktodo.ui.textoLargo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class DetalleActividadUiState(
    val cargando: Boolean = true,
    /** Nula después de cargar significa que la actividad ya no existe. */
    val actividad: ActividadConMateria? = null,
    val hoy: LocalDate = LocalDate.MIN,
)

/** Detalle de una actividad (RF-22), con marcar, editar y eliminar. */
class DetalleActividadViewModel(
    private val actividades: ActividadRepository,
    reloj: Clock,
    private val id: Long,
) : ViewModel() {

    val estado: StateFlow<DetalleActividadUiState> = actividades.actividad(id)
        .map { DetalleActividadUiState(cargando = false, actividad = it, hoy = LocalDate.now(reloj)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleActividadUiState())

    fun cambiarCumplida(cumplida: Boolean) {
        viewModelScope.launch { actividades.cambiarCumplida(id, cumplida) }
    }

    fun eliminar() {
        viewModelScope.launch { actividades.obtener(id)?.let { actividades.eliminar(it) } }
    }

    companion object {
        fun factory(id: Long) = viewModelFactory {
            initializer {
                val c = contenedor()
                DetalleActividadViewModel(c.actividadRepository, c.reloj, id)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleActividadScreen(
    id: Long,
    onAtras: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: DetalleActividadViewModel = viewModel(factory = DetalleActividadViewModel.factory(id)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    var confirmando by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado) { if (!estado.cargando && estado.actividad == null) onAtras() }

    val item = estado.actividad
    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text("Actividad") },
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { onEditar(id) }) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                        IconButton(onClick = { confirmando = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                    }
                },
            )
        },
    ) { modifier ->
        if (item == null) return@Pantalla
        val actividad = item.actividad
        val situacion = actividad.situacion(estado.hoy)
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("${actividad.tipo.icono()} ${actividad.titulo}", style = MaterialTheme.typography.headlineSmall)
            Text(actividad.tipo.nombre(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(
                listOfNotNull(actividad.fecha.textoLargo(), actividad.hora?.format(formatoHora)).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (item.materiaNombre != null && item.materiaColor != null) {
                Row(Modifier.padding(top = 8.dp).height(IntrinsicSize.Min)) {
                    FranjaMateria(item.materiaColor)
                    Text(item.materiaNombre, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
            val textoSituacion = when (situacion) {
                Situacion.CUMPLIDA -> "Cumplida"
                Situacion.ATRASADA -> "Atrasada"
                Situacion.NO_MARCADA -> "No marcado"
                Situacion.NO_REALIZADA -> "No realizada"
                Situacion.SALTADA -> "Saltada"
                Situacion.PENDIENTE -> null
            }
            textoSituacion?.let { Text(it, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary) }
            actividad.notas?.let {
                Spacer(Modifier.height(16.dp))
                Text("Notas", style = MaterialTheme.typography.labelLarge)
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            if (actividad.tipo != TipoActividad.GYM) {
                Spacer(Modifier.height(24.dp))
                if (situacion == Situacion.CUMPLIDA) {
                    OutlinedButton(onClick = { viewModel.cambiarCumplida(false) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Desmarcar")
                    }
                } else {
                    Button(onClick = { viewModel.cambiarCumplida(true) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Marcar como cumplida")
                    }
                }
            }
        }
    }

    if (confirmando && item != null) {
        DialogoConfirmar(
            titulo = "¿Eliminar ${item.actividad.titulo}?",
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
