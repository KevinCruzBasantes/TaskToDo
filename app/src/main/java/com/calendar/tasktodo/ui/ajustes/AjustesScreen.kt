package com.calendar.tasktodo.ui.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.respaldo.RespaldoRepository
import com.calendar.tasktodo.ui.componentes.textoMomento
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.ui.componentes.Pantalla

/** Ajustes (diseño de interfaz §3.14). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    onMaterias: () -> Unit,
    onAvisos: () -> Unit,
    onGuiaMiui: () -> Unit,
    onRespaldo: () -> Unit,
    viewModel: AjustesViewModel = viewModel(factory = AjustesViewModel.Factory),
) {
    val respaldo by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    Pantalla(enBarra = true, topBar = { TopAppBar(title = { Text("Ajustes") }) }) { modifier ->
        Column(modifier) {
            respaldo?.let { r ->
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                    AdvertenciaRespaldo(r.dias, hayRespaldo = r.ultimo != null, onRespaldar = onRespaldo)
                }
            }
            ListItem(
                headlineContent = { Text("Respaldo") },
                supportingContent = { Text(respaldo?.ultimo?.let { "Último: ${it.textoMomento()}" } ?: "Aún no hay respaldos") },
                modifier = Modifier.clickable(onClick = onRespaldo),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Avisos") },
                supportingContent = { Text("Qué avisos llegan y a qué hora") },
                modifier = Modifier.clickable(onClick = onAvisos),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Revisar avisos en este celular") },
                supportingContent = { Text("Permisos, batería e inicio automático (guía de MIUI)") },
                modifier = Modifier.clickable(onClick = onGuiaMiui),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Materias") },
                supportingContent = { Text("Agregar, editar o eliminar materias") },
                modifier = Modifier.clickable(onClick = onMaterias),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Acerca de") },
                supportingContent = { Text("Versión $version · base de datos ${AgendaDatabase.VERSION}") },
            )
        }
    }
}

data class AjustesUiState(val ultimo: Instant?, val dias: Long?)

/** Para la advertencia de más de una semana sin respaldo (RF-47). */
class AjustesViewModel(respaldo: RespaldoRepository) : ViewModel() {

    val estado: StateFlow<AjustesUiState?> = respaldo.estado
        .map { AjustesUiState(it.ultimo, respaldo.diasSinRespaldo(it.ultimo)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { AjustesViewModel(contenedor().respaldoRepository) }
        }
    }
}
