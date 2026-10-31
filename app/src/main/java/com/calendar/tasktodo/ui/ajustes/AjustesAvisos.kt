package com.calendar.tasktodo.ui.ajustes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.ajustes.AjustesAvisos
import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.sistema.compatibilidad.AjustesFabricante
import com.calendar.tasktodo.sistema.compatibilidad.EstadoSistema
import com.calendar.tasktodo.ui.componentes.CampoHora
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.contenedor
import com.calendar.tasktodo.ui.rememberFormatoHora
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Configurar avisos (CU-19). Guardar recalcula todos los avisos (diagramas §3). */
class AjustesAvisosViewModel(
    private val ajustes: AjustesRepository,
    private val estadoSistema: EstadoSistema,
    private val alCambiar: suspend () -> Unit,
) : ViewModel() {

    fun alarmasExactasPermitidas(): Boolean = estadoSistema.alarmasExactas()

    val estado: StateFlow<AjustesAvisos?> = ajustes.avisos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cambiar(transformar: (AjustesAvisos) -> AjustesAvisos) {
        viewModelScope.launch {
            ajustes.guardarAvisos(transformar(ajustes.avisos.first()))
            alCambiar()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                AjustesAvisosViewModel(c.ajustesRepository, c.estadoSistema) { c.servicioAvisos.reprogramarTodo() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesAvisosScreen(
    onAtras: () -> Unit,
    viewModel: AjustesAvisosViewModel = viewModel(factory = AjustesAvisosViewModel.Factory),
) {
    val ajustes by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    val context = LocalContext.current
    var alarmasExactas by remember { mutableStateOf(viewModel.alarmasExactasPermitidas()) }
    var confirmandoPrioridad by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { alarmasExactas = viewModel.alarmasExactasPermitidas() }

    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text("Avisos") },
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
    ) { modifier ->
        val a = ajustes ?: return@Pantalla
        Column(modifier.verticalScroll(rememberScrollState())) {
            if (!alarmasExactas) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        "Falta el permiso de alarmas exactas: los avisos pueden llegar tarde.",
                        Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    )
                    TextButton(onClick = { AjustesFabricante.alarmasExactas(context) }, modifier = Modifier.padding(8.dp)) {
                        Text("Permitir")
                    }
                }
            }
            Interruptor("Actividades", "Con la anticipación que elijas en cada una", a.actividades) { v -> viewModel.cambiar { it.copy(actividades = v) } }
            if (a.actividades) {
                CampoHora(
                    "Hora para las actividades sin hora", a.horaSinHora, formatoHora,
                    { h -> viewModel.cambiar { it.copy(horaSinHora = h) } },
                    Modifier.padding(horizontal = 16.dp),
                )
            }
            HorizontalDivider(Modifier.padding(top = 16.dp))
            Interruptor("Día anterior", "Para exámenes y simulacros", a.diaAnterior) { v -> viewModel.cambiar { it.copy(diaAnterior = v) } }
            if (a.diaAnterior) {
                CampoHora("Hora", a.horaDiaAnterior, formatoHora, { h -> viewModel.cambiar { it.copy(horaDiaAnterior = h) } }, Modifier.padding(horizontal = 16.dp))
            }
            HorizontalDivider(Modifier.padding(top = 16.dp))
            Interruptor("Clases", "${a.minutosClase} minutos antes de cada clase", a.clases) { v -> viewModel.cambiar { it.copy(clases = v) } }
            if (a.clases) {
                Selector("Minutos antes", listOf(5, 10, 15, 30), a.minutosClase, { "$it min" }) { m -> viewModel.cambiar { it.copy(minutosClase = m) } }
            }
            HorizontalDivider(Modifier.padding(top = 16.dp))
            Interruptor("Gym", "${a.minutosGym} minutos antes de cada sesión, con el nombre de la rutina", a.gym) { v -> viewModel.cambiar { it.copy(gym = v) } }
            if (a.gym) {
                Selector("Minutos antes", listOf(5, 10, 15, 30, 60), a.minutosGym, { "$it min" }) { m -> viewModel.cambiar { it.copy(minutosGym = m) } }
            }
            HorizontalDivider(Modifier.padding(top = 16.dp))
            Interruptor("Resumen del día", "Lo de hoy y las tareas atrasadas", a.resumen) { v -> viewModel.cambiar { it.copy(resumen = v) } }
            if (a.resumen) {
                CampoHora("Hora", a.horaResumen, formatoHora, { h -> viewModel.cambiar { it.copy(horaResumen = h) } }, Modifier.padding(horizontal = 16.dp))
            }
            HorizontalDivider(Modifier.padding(top = 16.dp))
            Interruptor(
                "Avisos con prioridad de alarma",
                "Usa la alarma del sistema para que los avisos lleguen a la hora justa. Muestra el ícono de alarma en la barra de estado.",
                a.prioridadAlarma,
            ) { v -> if (v) viewModel.cambiar { it.copy(prioridadAlarma = true) } else confirmandoPrioridad = true }
        }
    }

    if (confirmandoPrioridad) {
        AlertDialog(
            onDismissRequest = { confirmandoPrioridad = false },
            title = { Text("¿Desactivar la prioridad de alarma?") },
            text = { Text("Los avisos pueden llegar con varios minutos de retraso cuando el celular está en reposo.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cambiar { it.copy(prioridadAlarma = false) }
                    confirmandoPrioridad = false
                }) { Text("Desactivar") }
            },
            dismissButton = { TextButton(onClick = { confirmandoPrioridad = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Interruptor(titulo: String, detalle: String, activo: Boolean, onCambio: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = { Text(detalle) },
        trailingContent = { Switch(checked = activo, onCheckedChange = onCambio) },
    )
}

@Composable
private fun <T> Selector(titulo: String, opciones: List<T>, actual: T, texto: (T) -> String, onElegir: (T) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.Row {
            opciones.forEach { opcion ->
                androidx.compose.material3.FilterChip(
                    selected = opcion == actual,
                    onClick = { onElegir(opcion) },
                    label = { Text(texto(opcion)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }
}
