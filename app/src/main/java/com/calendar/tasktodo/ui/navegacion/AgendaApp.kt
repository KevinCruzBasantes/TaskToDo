package com.calendar.tasktodo.ui.navegacion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import com.calendar.tasktodo.ui.actividades.ActividadesScreen
import com.calendar.tasktodo.ui.actividades.DetalleActividadScreen
import com.calendar.tasktodo.ui.actividades.FormActividadScreen
import com.calendar.tasktodo.Pedido
import com.calendar.tasktodo.ui.ajustes.AjustesAvisosScreen
import com.calendar.tasktodo.ui.ajustes.AjustesScreen
import com.calendar.tasktodo.ui.ajustes.GuiaMiuiScreen
import com.calendar.tasktodo.ui.ajustes.RespaldoScreen
import com.calendar.tasktodo.ui.dia.DiaScreen
import com.calendar.tasktodo.ui.gym.DetalleRutinaScreen
import com.calendar.tasktodo.ui.gym.EstadisticasScreen
import com.calendar.tasktodo.ui.gym.PesoScreen
import com.calendar.tasktodo.ui.gym.FormEjercicioScreen
import com.calendar.tasktodo.ui.gym.FormRutinaScreen
import com.calendar.tasktodo.ui.gym.GymScreen
import com.calendar.tasktodo.ui.gym.ProgramacionScreen
import com.calendar.tasktodo.ui.gym.RutinasScreen
import com.calendar.tasktodo.ui.gym.SesionScreen
import com.calendar.tasktodo.ui.horario.DetalleClaseScreen
import com.calendar.tasktodo.ui.horario.FormClaseScreen
import com.calendar.tasktodo.ui.horario.FormMateriaScreen
import com.calendar.tasktodo.ui.calendario.CalendarioScreen
import com.calendar.tasktodo.ui.horario.MateriasScreen
import com.calendar.tasktodo.ui.inicio.BienvenidaScreen
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

private data class Seccion(val ruta: Any, val etiqueta: String, val icono: ImageVector)

/** Barra inferior con las cinco secciones (diseño de interfaz §2). */
private val SECCIONES = listOf(
    Seccion(Dia(), "Hoy", Icons.Filled.Home),
    Seccion(Calendario, "Calendario", Icons.Filled.DateRange),
    Seccion(Actividades, "Actividades", Icons.AutoMirrored.Filled.List),
    Seccion(Gym, "Gym", Icons.Filled.Favorite),
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

/**
 * @param pedido lo que pidió abrir una notificación o la alarma de la pantalla de bloqueo; se abre
 * y se avisa con [onPedidoAtendido].
 */
@Composable
fun AgendaApp(
    pedido: StateFlow<Pedido?>,
    onPedidoAtendido: () -> Unit,
    viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory),
) {
    val destinoInicial by viewModel.destinoInicial.collectAsStateWithLifecycle()
    val mostrarGuiaMiui by viewModel.mostrarGuiaMiui.collectAsStateWithLifecycle()
    val pedida by pedido.collectAsStateWithLifecycle()
    PedirPermisoNotificaciones()
    val inicio = destinoInicial ?: return

    val nav = rememberNavController()
    val entradaActual by nav.currentBackStackEntryAsState()
    val destino = entradaActual?.destination

    LaunchedEffect(pedida, entradaActual != null) {
        val abrir = pedida ?: return@LaunchedEffect
        if (entradaActual == null) return@LaunchedEffect
        when (abrir) {
            is Pedido.Actividad -> nav.navigate(DetalleActividad(abrir.id)) { launchSingleTop = true }
            is Pedido.Sesion -> nav.navigate(Sesion(abrir.actividadId)) { launchSingleTop = true }
        }
        onPedidoAtendido()
    }

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
                    onTerminar = {
                        nav.navigate(Dia()) { popUpTo<Bienvenida> { inclusive = true } }
                        if (mostrarGuiaMiui) nav.navigate(GuiaMiui)
                    },
                    onRestaurar = { nav.navigate(Respaldo(desdeBienvenida = true)) },
                )
            }
            composable<Dia> { entrada ->
                DiaScreen(
                    fechaInicial = entrada.toRoute<Dia>().fecha?.let(LocalDate::parse),
                    onAbrirClase = { nav.navigate(DetalleClase(it)) },
                    onAbrirActividad = { nav.navigate(DetalleActividad(it)) },
                    onNuevaActividad = { nav.navigate(FormActividad(fecha = it.toString())) },
                    onAbrirSesion = { nav.navigate(Sesion(it)) },
                )
            }
            composable<Calendario> {
                CalendarioScreen(
                    onAbrirDia = { nav.navigate(DiaDesdeCalendario(it.toString())) },
                    onAbrirClase = { nav.navigate(DetalleClase(it)) },
                    onNuevaClase = { nav.navigate(FormClase()) },
                    onNuevaMateria = { nav.navigate(FormMateria()) },
                    onMaterias = { nav.navigate(Materias) },
                )
            }
            composable<DiaDesdeCalendario> { entrada ->
                DiaScreen(
                    fechaInicial = LocalDate.parse(entrada.toRoute<DiaDesdeCalendario>().fecha),
                    onAbrirClase = { nav.navigate(DetalleClase(it)) },
                    onAbrirActividad = { nav.navigate(DetalleActividad(it)) },
                    onNuevaActividad = { nav.navigate(FormActividad(fecha = it.toString())) },
                    onAbrirSesion = { nav.navigate(Sesion(it)) },
                    onAtras = { nav.volverDesde(entrada) },
                )
            }
            composable<Actividades> {
                ActividadesScreen(
                    onAbrirActividad = { nav.navigate(DetalleActividad(it)) },
                    onNuevaActividad = { nav.navigate(FormActividad()) },
                )
            }
            composable<Ajustes> {
                AjustesScreen(
                    onMaterias = { nav.navigate(Materias) },
                    onAvisos = { nav.navigate(AjustesAvisos) },
                    onGuiaMiui = { nav.navigate(GuiaMiui) },
                    onRespaldo = { nav.navigate(Respaldo()) },
                )
            }
            composable<Materias> { entrada ->
                MateriasScreen(
                    onAtras = { nav.volverDesde(entrada) },
                    onAbrirMateria = { nav.navigate(FormMateria(it)) },
                    onNuevaMateria = { nav.navigate(FormMateria()) },
                )
            }
            composable<FormMateria> { entrada ->
                FormMateriaScreen(id = entrada.toRoute<FormMateria>().id, onTerminar = { nav.volverDesde(entrada) })
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
                    onAbrirActividad = { nav.navigate(DetalleActividad(it)) },
                )
            }
            composable<FormActividad> { entrada ->
                val ruta = entrada.toRoute<FormActividad>()
                FormActividadScreen(
                    id = ruta.id,
                    fecha = ruta.fecha?.let(LocalDate::parse),
                    onTerminar = { nav.volverDesde(entrada) },
                )
            }
            composable<DetalleActividad> { entrada ->
                DetalleActividadScreen(
                    id = entrada.toRoute<DetalleActividad>().id,
                    onAtras = { nav.volverDesde(entrada) },
                    onEditar = { nav.navigate(FormActividad(id = it)) },
                    onAbrirSesion = { nav.navigate(Sesion(it)) },
                )
            }
            composable<Gym> {
                GymScreen(
                    onAbrirSesion = { nav.navigate(Sesion(it)) },
                    onRutinas = { nav.navigate(Rutinas) },
                    onProgramacion = { nav.navigate(Programacion) },
                    onPeso = { nav.navigate(Peso) },
                    onEstadisticas = { nav.navigate(Estadisticas) },
                )
            }
            composable<Peso> { entrada ->
                PesoScreen(onAtras = { nav.volverDesde(entrada) })
            }
            composable<Estadisticas> { entrada ->
                EstadisticasScreen(onAtras = { nav.volverDesde(entrada) })
            }
            composable<Rutinas> { entrada ->
                RutinasScreen(
                    onAtras = { nav.volverDesde(entrada) },
                    onAbrir = { nav.navigate(DetalleRutina(it)) },
                    onNueva = { nav.navigate(FormRutina()) },
                )
            }
            composable<FormRutina> { entrada ->
                FormRutinaScreen(
                    id = entrada.toRoute<FormRutina>().id,
                    onCerrar = { nav.volverDesde(entrada) },
                    onGuardada = { id, eraEdicion ->
                        // Una rutina nueva sigue a su detalle para agregarle ejercicios (CU-12 6 y 7)
                        if (eraEdicion) {
                            nav.volverDesde(entrada)
                        } else {
                            nav.navigate(DetalleRutina(id)) { popUpTo<FormRutina> { inclusive = true } }
                        }
                    },
                )
            }
            composable<DetalleRutina> { entrada ->
                val id = entrada.toRoute<DetalleRutina>().id
                DetalleRutinaScreen(
                    id = id,
                    onAtras = { nav.volverDesde(entrada) },
                    onEditarRutina = { nav.navigate(FormRutina(id)) },
                    onNuevoEjercicio = { nav.navigate(FormEjercicio(id)) },
                    onEditarEjercicio = { nav.navigate(FormEjercicio(id, it)) },
                )
            }
            composable<FormEjercicio> { entrada ->
                val ruta = entrada.toRoute<FormEjercicio>()
                FormEjercicioScreen(rutinaId = ruta.rutinaId, id = ruta.id, onCerrar = { nav.volverDesde(entrada) })
            }
            composable<Programacion> { entrada ->
                ProgramacionScreen(onAtras = { nav.volverDesde(entrada) }, onCrearRutina = { nav.navigate(FormRutina()) })
            }
            composable<Sesion> { entrada ->
                SesionScreen(actividadId = entrada.toRoute<Sesion>().actividadId, onAtras = { nav.volverDesde(entrada) })
            }
            composable<AjustesAvisos> { entrada -> AjustesAvisosScreen(onAtras = { nav.volverDesde(entrada) }) }
            composable<GuiaMiui> { entrada -> GuiaMiuiScreen(onTerminar = { nav.volverDesde(entrada) }) }
            composable<Respaldo> { entrada ->
                val desdeBienvenida = entrada.toRoute<Respaldo>().desdeBienvenida
                RespaldoScreen(
                    iniciarRestauracion = desdeBienvenida,
                    onAtras = { nav.volverDesde(entrada) },
                    onRestaurado = {
                        if (desdeBienvenida) nav.navigate(Dia()) { popUpTo<Bienvenida> { inclusive = true } }
                    },
                )
            }
        }
    }
}

/** En Android 13 o superior se pide el permiso de notificaciones (CU-19 1a). En Android 12 no existe. */
@Composable
private fun PedirPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
