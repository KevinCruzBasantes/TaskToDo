package com.calendar.tasktodo.ui.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.ui.componentes.Pantalla

/** Ajustes. El respaldo llega en el incremento 3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(onMaterias: () -> Unit, onAvisos: () -> Unit, onGuiaMiui: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    Pantalla(enBarra = true, topBar = { TopAppBar(title = { Text("Ajustes") }) }) { modifier ->
        Column(modifier) {
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
