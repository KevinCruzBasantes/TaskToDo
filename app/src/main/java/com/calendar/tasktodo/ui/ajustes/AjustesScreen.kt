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

/** Ajustes. Avisos, respaldo y la guía de MIUI llegan en los incrementos 2 y 3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(onMaterias: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    Pantalla(enBarra = true, topBar = { TopAppBar(title = { Text("Ajustes") }) }) { modifier ->
        Column(modifier) {
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
