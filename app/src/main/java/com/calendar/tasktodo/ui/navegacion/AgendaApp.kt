package com.calendar.tasktodo.ui.navegacion

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.calendar.tasktodo.ui.ajustes.AjustesScreen
import com.calendar.tasktodo.ui.dia.DiaScreen
import com.calendar.tasktodo.ui.horario.DetalleClaseScreen
import com.calendar.tasktodo.ui.horario.FormClaseScreen
import com.calendar.tasktodo.ui.horario.FormMateriaScreen
import com.calendar.tasktodo.ui.horario.HorarioScreen
import com.calendar.tasktodo.ui.horario.MateriasScreen
import com.calendar.tasktodo.ui.inicio.BienvenidaScreen
import java.time.LocalDate

private data class Seccion(val ruta: Any, val etiqueta: String, val icono: ImageVector)

/** En el incremento 1 la barra solo tiene Hoy, Calendario y Ajustes (diseño de interfaz §2). */
private val SECCIONES = listOf(
    Seccion(Dia(), "Hoy", Icons.Filled.Home),
    Seccion(Calendario, "Calendario", Icons.Filled.DateRange),
    Seccion(Ajustes, "Ajustes", Icons.Filled.Settings),
)

private fun NavDestination.esSeccion(seccion: Seccion) = hasRoute(seccion.ruta::class)

/** Atrás desde una sección vuelve a Hoy; desde Hoy, cierra la app. */
private fun NavController.irASeccion(ruta: Any) = navigate(ruta) {
    popUpTo<Dia> { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Vuelve solo si [entrada] sigue arriba, para que un doble toque no saque dos pantallas. */
private fun NavController.volverDesde(entrada: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entrada.id) popBackStack()
}

@Composable
fun AgendaApp(viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)) {
    val destinoInicial by viewModel.destinoInicial.collectAsStateWithLifecycle()
    val inicio = destinoInicial ?: return

    val nav = rememberNavController()
    val entradaActual by nav.currentBackStackEntryAsState()
    val destino = entradaActual?.destination

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (destino != null && SECCIONES.any { destino.esSeccion(it) }) {
                NavigationBar {
                    SECCIONES.forEach { seccion ->
                        NavigationBarItem(
                            selected = destino.esSeccion(seccion),
                            onClick = { nav.irASeccion(seccion.ruta) },
                            icon = { Icon(seccion.icono, contentDescription = null) },
                            label = { Text(seccion.etiqueta) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = inicio, modifier = Modifier.padding(padding)) {
            composable<Bienvenida> {
                BienvenidaScreen(
                    onNuevaMateria = { nav.navigate(FormMateria()) },
                    onEditarMateria = { nav.navigate(FormMateria(it)) },
                    onAgregarClases = { nav.navigate(FormClase(materiaId = it)) },
                    onTerminar = { nav.navigate(Dia()) { popUpTo<Bienvenida> { inclusive = true } } },
                )
            }
            composable<Dia> { entrada ->
                DiaScreen(
                    fechaInicial = entrada.toRoute<Dia>().fecha?.let(LocalDate::parse),
                    onAbrirClase = { nav.navigate(DetalleClase(it)) },
                    onVerHorario = { nav.irASeccion(Calendario) },
                )
            }
            composable<Calendario> {
                HorarioScreen(
                    onAbrirClase = { nav.navigate(DetalleClase(it)) },
                    onNuevaClase = { nav.navigate(FormClase()) },
                    onNuevaMateria = { nav.navigate(FormMateria()) },
                    onMaterias = { nav.navigate(Materias) },
                )
            }
            composable<Ajustes> {
                AjustesScreen(onMaterias = { nav.navigate(Materias) })
            }
            composable<Materias> { entrada ->
                MateriasScreen(
                    onAtras = { nav.volverDesde(entrada) },
                    onAbrirMateria = { nav.navigate(FormMateria(it)) },
                    onNuevaMateria = { nav.navigate(FormMateria()) },
                )
            }
            composable<FormMateria> { entrada ->
                FormMateriaScreen(
                    id = entrada.toRoute<FormMateria>().id,
                    onTerminar = { nav.volverDesde(entrada) },
                )
            }
            composable<FormClase> { entrada ->
                val ruta = entrada.toRoute<FormClase>()
                FormClaseScreen(
                    id = ruta.id,
                    materiaId = ruta.materiaId,
                    onTerminar = { nav.volverDesde(entrada) },
                    onNuevaMateria = { nav.navigate(FormMateria()) },
                )
            }
            composable<DetalleClase> { entrada ->
                DetalleClaseScreen(
                    id = entrada.toRoute<DetalleClase>().id,
                    onAtras = { nav.volverDesde(entrada) },
                    onEditar = { nav.navigate(FormClase(id = it)) },
                )
            }
        }
    }
}
