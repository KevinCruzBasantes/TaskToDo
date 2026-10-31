package com.calendar.tasktodo.ui.ajustes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.sistema.compatibilidad.AjustesFabricante
import com.calendar.tasktodo.sistema.compatibilidad.EstadoSistema
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lo que se puede comprobar solo; se vuelve a leer cada vez que la pantalla aparece. */
data class EstadoGuia(
    val alarmasExactas: Boolean,
    val bateria: Boolean,
    val notificaciones: Boolean,
)

/** Guía de MIUI (arquitectura §4.6, diseño de interfaz §3.14). */
class GuiaMiuiViewModel(
    private val estadoSistema: EstadoSistema,
    private val ajustes: AjustesRepository,
) : ViewModel() {

    val esXiaomi: Boolean = estadoSistema.esXiaomi

    /** Los pasos que MIUI no deja comprobar, marcados a mano con "Hecho". */
    val pasosManuales: StateFlow<Set<String>> = ajustes.pasosGuiaHechos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun leerEstado() = EstadoGuia(
        alarmasExactas = estadoSistema.alarmasExactas(),
        bateria = estadoSistema.bateriaSinRestricciones(),
        notificaciones = estadoSistema.notificacionesActivas(),
    )

    fun marcarHecho(paso: String) {
        viewModelScope.launch { ajustes.marcarPasoGuia(paso) }
    }

    /** La guía se ofrece una sola vez después de la bienvenida (CU-01 7a). */
    fun marcarMostrada() {
        viewModelScope.launch { ajustes.marcarGuiaMiuiMostrada() }
    }

    companion object {
        const val INICIO_AUTOMATICO = "inicio_automatico"
        const val CANDADO = "candado"

        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                GuiaMiuiViewModel(c.estadoSistema, c.ajustesRepository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuiaMiuiScreen(
    onTerminar: () -> Unit,
    viewModel: GuiaMiuiViewModel = viewModel(factory = GuiaMiuiViewModel.Factory),
) {
    val context = LocalContext.current
    var estado by remember { mutableStateOf(viewModel.leerEstado()) }
    val manuales by viewModel.pasosManuales.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { estado = viewModel.leerEstado() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.marcarMostrada() }

    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.esXiaomi) "Guía de MIUI" else "Revisar avisos") },
                navigationIcon = {
                    IconButton(onClick = onTerminar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
    ) { modifier ->
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("Para que los avisos lleguen a tiempo${if (viewModel.esXiaomi) " en tu Xiaomi" else ""}:")
            Paso("Alarmas exactas", "Permite que los avisos suenen a la hora justa.", estado.alarmasExactas) {
                AjustesFabricante.alarmasExactas(context)
            }
            Paso("Batería sin restricciones", "Elige \"Sin restricciones\" para TaskToDo.", estado.bateria) {
                AjustesFabricante.bateria(context)
            }
            Paso("Notificaciones activadas", "Activa las notificaciones de TaskToDo.", estado.notificaciones) {
                AjustesFabricante.notificaciones(context)
            }
            if (viewModel.esXiaomi) {
                PasoManual(
                    "Inicio automático",
                    "Actívalo en Seguridad → Inicio automático, para que los avisos se reprogramen al reiniciar.",
                    GuiaMiuiViewModel.INICIO_AUTOMATICO in manuales,
                    onAbrir = { AjustesFabricante.inicioAutomatico(context) },
                    onHecho = { viewModel.marcarHecho(GuiaMiuiViewModel.INICIO_AUTOMATICO) },
                )
                PasoManual(
                    "Bloquear en recientes",
                    "Abre recientes, mantén la app presionada y toca el candado 🔒.",
                    GuiaMiuiViewModel.CANDADO in manuales,
                    onAbrir = null,
                    onHecho = { viewModel.marcarHecho(GuiaMiuiViewModel.CANDADO) },
                )
            }
            Button(onClick = onTerminar, modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)) { Text("Listo") }
        }
    }
}

@Composable
private fun Paso(titulo: String, detalle: String, listo: Boolean, onAbrir: () -> Unit) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text("${if (listo) "✅" else "⚠"} $titulo", style = MaterialTheme.typography.titleMedium)
        if (!listo) {
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onAbrir) { Text("Abrir ajuste") }
        }
    }
    HorizontalDivider()
}

@Composable
private fun PasoManual(titulo: String, detalle: String, hecho: Boolean, onAbrir: (() -> Unit)?, onHecho: () -> Unit) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text("${if (hecho) "✅" else "◻"} $titulo", style = MaterialTheme.typography.titleMedium)
        if (!hecho) {
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                onAbrir?.let { OutlinedButton(onClick = it, modifier = Modifier.padding(end = 8.dp)) { Text("Abrir ajuste") } }
                OutlinedButton(onClick = onHecho) { Text("Hecho") }
            }
        }
    }
    HorizontalDivider()
}
