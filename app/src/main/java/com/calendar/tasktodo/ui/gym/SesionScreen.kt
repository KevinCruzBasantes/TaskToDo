package com.calendar.tasktodo.ui.gym

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.data.db.entidad.EstadoSesion
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.data.repositorio.Avance
import com.calendar.tasktodo.ui.componentes.ContadorNumero
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.componentes.comoMinutos
import com.calendar.tasktodo.ui.componentes.comoReloj
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.Duration
import java.time.Instant

/** Sesión de gym (CU-16, diseño de interfaz §3.11). La pantalla no se apaga mientras está abierta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SesionScreen(
    actividadId: Long,
    onAtras: () -> Unit,
    viewModel: SesionViewModel = viewModel(factory = SesionViewModel.factory(actividadId)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val vista = LocalView.current
    DisposableEffect(Unit) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }
    // El reloj de la pantalla; el tiempo real siempre sale de los instantes guardados (RNF-07)
    var ahora by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            ahora = Instant.now()
            delay(250)
        }
    }
    LaunchedEffect(estado.salir) { if (estado.salir) onAtras() }

    var verEjercicios by remember { mutableStateOf(false) }
    var terminando by remember { mutableStateOf(false) }
    val sesion = estado.sesion

    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(sesion?.nombreRutina ?: estado.rutina?.nombre ?: estado.actividad?.titulo.orEmpty())
                        estado.avance?.let { a ->
                            if (sesion?.estado == EstadoSesion.EN_CURSO) {
                                Text("Ejercicio ${a.posicionActual} de ${a.ordenados.size}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
                actions = {
                    if (sesion?.estado == EstadoSesion.EN_CURSO) {
                        Text(
                            "⏱ ${Duration.between(sesion.inicio, ahora).seconds.coerceAtLeast(0).comoReloj()}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(onClick = { verEjercicios = true }) { Text("☰ Ejercicios") }
                    }
                },
            )
        },
    ) { modifier ->
        if (estado.cargando) return@Pantalla
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                sesion == null -> Preparada(estado, viewModel::iniciar)
                sesion.estado == EstadoSesion.TERMINADA -> Text(
                    "Sesión hecha · ${(sesion.duracionSeg ?: 0) / 60} min · ${estado.series.size} series",
                    style = MaterialTheme.typography.titleLarge,
                )
                else -> EnCurso(estado, ahora, viewModel, onTerminar = { terminando = true })
            }
        }
    }

    estado.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarError,
            text = { Text(error) },
            confirmButton = { TextButton(onClick = viewModel::cerrarError) { Text("Entendido") } },
        )
    }
    if (terminando && sesion != null) {
        val avance = estado.avance
        AlertDialog(
            onDismissRequest = { terminando = false },
            title = { Text("Terminar sesión") },
            text = {
                Text(
                    "Duración: ${Duration.between(sesion.inicio, ahora).seconds.comoReloj()}\n" +
                        "Ejercicios completos: ${avance?.completos?.size ?: 0} de ${avance?.ordenados?.size ?: 0}\n" +
                        "Series: ${estado.series.size}" +
                        if (avance?.terminada == false) "\n\nTodavía hay series sin hacer: se guardan solo las hechas." else "",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    terminando = false
                    viewModel.terminar()
                }) { Text("Guardar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        terminando = false
                        viewModel.descartar()
                    }) { Text("Descartar sesión", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { terminando = false }) { Text("Seguir") }
                }
            },
        )
    }
    if (estado.preguntarOrden) {
        AlertDialog(
            onDismissRequest = { viewModel.guardarOrden(false) },
            title = { Text("¿Guardar este orden en la rutina?") },
            text = { Text("Hiciste los ejercicios en otro orden. La próxima vez pueden salir así.") },
            confirmButton = { TextButton(onClick = { viewModel.guardarOrden(true) }) { Text("Guardar orden") } },
            dismissButton = { TextButton(onClick = { viewModel.guardarOrden(false) }) { Text("No") } },
        )
    }
    if (verEjercicios) {
        estado.avance?.let { avance ->
            ModalBottomSheet(onDismissRequest = { verEjercicios = false }) {
                ListaEjerciciosSesion(avance, viewModel)
            }
        }
    }
}

@Composable
private fun Preparada(estado: SesionUiState, onIniciar: () -> Unit) {
    Text("${estado.ejerciciosRutina.size} ejercicios", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(12.dp))
    estado.ejerciciosRutina.forEachIndexed { i, e ->
        Text("${i + 1}. ${e.nombre} · ${e.resumen()}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onIniciar,
        enabled = estado.ejerciciosRutina.isNotEmpty(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) { Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge) }
}

@Composable
private fun EnCurso(estado: SesionUiState, ahora: Instant, viewModel: SesionViewModel, onTerminar: () -> Unit) {
    val sesion = estado.sesion ?: return
    val avance = estado.avance ?: return
    val actual = avance.actual
    val descanso = sesion.descansoTerminaEn?.takeIf { it.isAfter(ahora) }

    when {
        descanso != null -> {
            val restante = Duration.between(ahora, descanso).seconds + 1
            Text("DESCANSO", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(restante.toInt().comoMinutos(), fontSize = 72.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { viewModel.ajustarDescanso(-15) }) { Text("−15 s") }
                OutlinedButton(onClick = { viewModel.ajustarDescanso(15) }) { Text("+15 s") }
            }
            TextButton(onClick = viewModel::saltarDescanso) { Text("Saltar") }
            actual?.let { Text("Siguiente: ${it.nombre} · Serie ${avance.serieActual}", style = MaterialTheme.typography.bodyLarge) }
        }
        actual != null -> {
            Text(actual.nombre.uppercase(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("Serie ${avance.serieActual} de ${actual.series}", style = MaterialTheme.typography.titleLarge)
            actual.nota?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                if (actual.medida == Medida.REPETICIONES) {
                    ContadorNumero("Repeticiones", estado.repeticiones, viewModel::cambiarRepeticiones, grande = true)
                } else {
                    ContadorNumero("Duración", estado.duracionSeg, viewModel::cambiarDuracion, paso = 5, grande = true, formato = { it.comoMinutos() })
                }
                ContadorPeso(estado.pesoKg, viewModel::cambiarPeso)
            }
            estado.ultimaVez?.let { Text("La última vez: $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(20.dp))
            if (actual.medida == Medida.TIEMPO) {
                CuentaRegresiva(estado.duracionSeg, clave = actual.id to avance.serieActual, onConfirmar = viewModel::confirmarSerie)
            } else {
                Button(
                    onClick = viewModel::confirmarSerie,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                ) { Text("✓  Serie hecha", style = MaterialTheme.typography.headlineSmall) }
            }
            Spacer(Modifier.height(12.dp))
            estado.series.filter { it.ejercicioId == actual.id }.forEach { s ->
                Text("✓ Serie ${s.numeroSerie} · ${s.repeticiones ?: s.duracionSeg?.comoMinutos()} ${s.pesoKg?.let { "× ${textoPeso(it)} kg" }.orEmpty()}")
            }
        }
        else -> {
            Text("¡Rutina completa!", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onTerminar, modifier = Modifier.padding(top = 16.dp)) { Text("Terminar y guardar") }
        }
    }
    Spacer(Modifier.height(24.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = viewModel::anterior, enabled = estado.series.isNotEmpty()) { Text("‹ Anterior") }
        TextButton(onClick = onTerminar) { Text("Terminar sesión") }
    }
}

@Composable
private fun ContadorPeso(peso: Double?, onCambio: (Double?) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Peso", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { onCambio((peso ?: 0.0) - 2.5) }) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text(peso?.let { "${textoPeso(it)} kg" } ?: "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            FilledTonalIconButton(onClick = { onCambio((peso ?: 0.0) + 2.5) }) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

/** Ejercicio por tiempo: el usuario inicia la cuenta, el celular vibra al terminar y se confirma (CU-16 5a). */
@Composable
private fun CuentaRegresiva(segundos: Int, clave: Any, onConfirmar: () -> Unit) {
    val context = LocalContext.current
    var fin by remember(clave) { mutableStateOf<Instant?>(null) }
    var restante by remember(clave) { mutableLongStateOf(segundos.toLong()) }
    var terminada by remember(clave) { mutableStateOf(false) }
    LaunchedEffect(fin) {
        val hasta = fin ?: return@LaunchedEffect
        while (true) {
            restante = Duration.between(Instant.now(), hasta).seconds.coerceAtLeast(0)
            if (!Instant.now().isBefore(hasta)) break
            delay(200)
        }
        terminada = true
        context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
    }
    when {
        fin == null -> Button(
            onClick = { fin = Instant.now().plusSeconds(segundos.toLong()) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
        ) { Text("▶  Iniciar ${segundos.comoMinutos()}", style = MaterialTheme.typography.headlineSmall) }
        !terminada -> {
            Text(restante.toInt().comoMinutos(), fontSize = 64.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { 1f - restante.toFloat() / segundos.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
        }
        else -> Button(
            onClick = onConfirmar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
        ) { Text("✓  Serie hecha", style = MaterialTheme.typography.headlineSmall) }
    }
}

/** Hechos, el actual y los pendientes como bloques que se mueven a cualquier posición (CU-16 9b). */
@Composable
private fun ListaEjerciciosSesion(avance: Avance, viewModel: SesionViewModel) {
    var pendientes by remember(avance) { mutableStateOf(avance.pendientes) }
    var aMover by remember { mutableStateOf<Int?>(null) }
    val estadoLista = rememberLazyListState()
    val inicioPendientes = avance.completos.size + (if (avance.actual != null) 1 else 0) + 3
    val reordenable = rememberReorderableLazyListState(estadoLista) { desde, hasta ->
        val d = desde.index - inicioPendientes
        val h = hasta.index - inicioPendientes
        if (d in pendientes.indices && h in pendientes.indices) {
            pendientes = pendientes.toMutableList().apply { add(h, removeAt(d)) }
        }
    }
    fun guardar(id: Long) {
        val desde = avance.pendientes.indexOfFirst { it.id == id }
        val hasta = pendientes.indexOfFirst { it.id == id }
        if (desde >= 0 && desde != hasta) viewModel.moverPendiente(desde, hasta)
    }
    LazyColumn(
        state = estadoLista,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { Text("Ejercicios de la sesión", style = MaterialTheme.typography.titleLarge) }
        item { Text("HECHOS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        items(avance.completos, key = { "h${it.id}" }) { e ->
            Text("✓ ${e.nombre} · ${avance.hechasPorEjercicio[e.id] ?: 0} de ${e.series}")
        }
        avance.actual?.let { e ->
            item(key = "a${e.id}") {
                Text("▶ AHORA: ${e.nombre} · ${avance.hechasPorEjercicio[e.id] ?: 0} de ${e.series}", fontWeight = FontWeight.Bold)
            }
        }
        item { Text("PENDIENTES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        itemsIndexed(pendientes, key = { _, e -> e.id }) { i, e ->
            ReorderableItem(reordenable, key = e.id) { arrastrando ->
                BloqueEjercicio(
                    numero = avance.completos.size + (if (avance.actual != null) 1 else 0) + i + 1,
                    ejercicio = e,
                    arrastrando = arrastrando,
                    asa = Modifier.draggableHandle(onDragStopped = { guardar(e.id) }),
                    cuerpo = Modifier.longPressDraggableHandle(onDragStopped = { guardar(e.id) }),
                    onAbrir = {},
                    detalle = "${avance.hechasPorEjercicio[e.id] ?: 0} de ${e.series} series",
                    opciones = listOf(
                        "Hacer ahora" to { viewModel.hacerAhora(e.id) },
                        "Mover a la posición…" to { aMover = i },
                    ),
                )
            }
        }
    }
    aMover?.let { desde ->
        DialogoPosicion(
            total = pendientes.size,
            actual = desde + 1,
            onElegir = { posicion ->
                viewModel.moverPendiente(desde, posicion - 1)
                aMover = null
            },
            onCancelar = { aMover = null },
        )
    }
}
