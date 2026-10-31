package com.calendar.tasktodo.ui.gym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.ui.componentes.ContadorNumero
import com.calendar.tasktodo.ui.componentes.DialogoConfirmar
import com.calendar.tasktodo.ui.componentes.EstadoVacio
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.componentes.SelectorSegundos
import com.calendar.tasktodo.ui.componentes.comoMinutos
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Lista de rutinas (CU-12). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinasScreen(
    onAtras: () -> Unit,
    onAbrir: (Long) -> Unit,
    onNueva: () -> Unit,
    viewModel: RutinasViewModel = viewModel(factory = RutinasViewModel.Factory),
) {
    val rutinas by viewModel.rutinas.collectAsStateWithLifecycle()
    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text("Rutinas") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onNueva) { Icon(Icons.Filled.Add, contentDescription = "Agregar rutina") } },
    ) { modifier ->
        val lista = rutinas ?: return@Pantalla
        if (lista.isEmpty()) {
            EstadoVacio("Aún no tienes rutinas", detalle = "Por ejemplo, pierna y torso.", accion = "Crear rutina", onAccion = onNueva, modifier = modifier)
            return@Pantalla
        }
        LazyColumn(modifier.fillMaxSize()) {
            items(lista, key = { it.rutina.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.rutina.nombre) },
                    supportingContent = { Text(if (item.ejercicios == 1) "1 ejercicio" else "${item.ejercicios} ejercicios") },
                    modifier = Modifier.clickable { onAbrir(item.rutina.id) },
                )
            }
        }
    }
}

/** Nombre y descanso por defecto de una rutina (CU-12). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormRutinaScreen(
    id: Long?,
    onCerrar: () -> Unit,
    onGuardada: (Long, Boolean) -> Unit,
    viewModel: FormRutinaViewModel = viewModel(factory = FormRutinaViewModel.factory(id)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.guardada) { estado.guardada?.let { onGuardada(it, estado.esEdicion) } }
    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Editar rutina" else "Nueva rutina") },
                navigationIcon = { IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") } },
                actions = { TextButton(onClick = viewModel::guardar, enabled = !estado.cargando) { Text("Guardar") } },
            )
        },
    ) { modifier ->
        if (estado.cargando) return@Pantalla
        Column(modifier.padding(16.dp)) {
            OutlinedTextField(
                value = estado.nombre,
                onValueChange = viewModel::cambiarNombre,
                label = { Text("Nombre") },
                singleLine = true,
                isError = estado.error != null,
                supportingText = estado.error?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            SelectorSegundos(
                "Descanso entre ejercicios",
                estado.descansoEntreEjercicios,
                textoNulo = "Sin descanso",
                onCambio = viewModel::cambiarDescanso,
            )
        }
    }
}

/** Detalle de rutina: los ejercicios son bloques que se arrastran a cualquier posición (CU-13 2d). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleRutinaScreen(
    id: Long,
    onAtras: () -> Unit,
    onEditarRutina: () -> Unit,
    onNuevoEjercicio: () -> Unit,
    onEditarEjercicio: (Long) -> Unit,
    viewModel: DetalleRutinaViewModel = viewModel(factory = DetalleRutinaViewModel.factory(id)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    var menuRutina by remember { mutableStateOf(false) }
    var confirmarBorrar by rememberSaveable { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Ejercicio?>(null) }
    var aMover by remember { mutableStateOf<Ejercicio?>(null) }

    LaunchedEffect(estado) { if (!estado.cargando && estado.rutina == null) onAtras() }

    // Copia local para que el bloque se mueva al arrastrar; se guarda al soltar
    var lista by remember(estado.ejercicios) { mutableStateOf(estado.ejercicios) }
    val estadoLista = rememberLazyListState()
    val reordenable = rememberReorderableLazyListState(estadoLista) { desde, hasta ->
        lista = lista.toMutableList().apply { add(hasta.index, removeAt(desde.index)) }
    }

    fun guardarMovimiento(ejercicio: Ejercicio) {
        val desde = estado.ejercicios.indexOfFirst { it.id == ejercicio.id }
        val hasta = lista.indexOfFirst { it.id == ejercicio.id }
        if (desde == hasta || desde < 0) return
        viewModel.mover(desde, hasta)
        alcance.launch {
            val r = avisos.showSnackbar("Orden guardado", actionLabel = "Deshacer", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) viewModel.mover(hasta, desde)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(estado.rutina?.nombre.orEmpty())
                        estado.rutina?.descansoEntreEjerciciosSeg?.let {
                            Text("Descanso entre ejercicios ${it.comoMinutos()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
                actions = {
                    IconButton(onClick = onEditarRutina) { Icon(Icons.Filled.Edit, contentDescription = "Editar rutina") }
                    Box {
                        IconButton(onClick = { menuRutina = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones") }
                        DropdownMenu(expanded = menuRutina, onDismissRequest = { menuRutina = false }) {
                            DropdownMenuItem(text = { Text("Eliminar rutina") }, onClick = {
                                menuRutina = false
                                confirmarBorrar = true
                            })
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(avisos) },
    ) { padding ->
        if (estado.rutina == null) return@Scaffold
        if (lista.isEmpty()) {
            EstadoVacio(
                "Esta rutina no tiene ejercicios",
                accion = "Agregar el primero",
                onAccion = onNuevoEjercicio,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            state = estadoLista,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(lista, key = { _, e -> e.id }) { indice, ejercicio ->
                ReorderableItem(reordenable, key = ejercicio.id) { arrastrando ->
                    BloqueEjercicio(
                        numero = indice + 1,
                        ejercicio = ejercicio,
                        arrastrando = arrastrando,
                        asa = Modifier.draggableHandle(onDragStopped = { guardarMovimiento(ejercicio) }),
                        cuerpo = Modifier.longPressDraggableHandle(onDragStopped = { guardarMovimiento(ejercicio) }),
                        onAbrir = { onEditarEjercicio(ejercicio.id) },
                        opciones = listOf(
                            "Editar" to { onEditarEjercicio(ejercicio.id) },
                            "Duplicar" to { viewModel.duplicar(ejercicio) },
                            "Mover a la posición…" to { aMover = ejercicio },
                            "Eliminar" to { aEliminar = ejercicio },
                        ),
                        // Con lector de pantalla no se puede arrastrar (diseño §3.9)
                        accionesAccesibles = listOfNotNull(
                            if (indice > 0) "Subir una posición" to { viewModel.mover(indice, indice - 1) } else null,
                            if (indice < lista.lastIndex) "Bajar una posición" to { viewModel.mover(indice, indice + 1) } else null,
                        ),
                    )
                }
            }
            item {
                OutlinedButton(onClick = onNuevoEjercicio, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Agregar ejercicio", Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (confirmarBorrar) {
        DialogoConfirmar(
            titulo = "¿Eliminar ${estado.rutina?.nombre.orEmpty()}?",
            texto = "Se borrarán sus ejercicios. Las sesiones ya hechas se conservan.",
            confirmar = "Eliminar",
            onConfirmar = {
                confirmarBorrar = false
                viewModel.eliminarRutina()
            },
            onCancelar = { confirmarBorrar = false },
        )
    }
    estado.enUso?.let { dias ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarEnUso,
            title = { Text("La rutina está en uso") },
            text = { Text("Se usa los ${dias.joinToString(", ") { it.lowercase() }}. Asigna otra rutina a esos días en la programación antes de eliminarla.") },
            confirmButton = { TextButton(onClick = viewModel::cerrarEnUso) { Text("Entendido") } },
        )
    }
    aEliminar?.let { ejercicio ->
        DialogoConfirmar(
            titulo = "¿Eliminar ${ejercicio.nombre}?",
            texto = "Sale de la rutina. Las sesiones ya hechas conservan sus series.",
            confirmar = "Eliminar",
            onConfirmar = {
                viewModel.eliminarEjercicio(ejercicio)
                aEliminar = null
            },
            onCancelar = { aEliminar = null },
        )
    }
    aMover?.let { ejercicio ->
        DialogoPosicion(
            total = lista.size,
            actual = lista.indexOfFirst { it.id == ejercicio.id } + 1,
            onElegir = { posicion ->
                viewModel.mover(lista.indexOfFirst { it.id == ejercicio.id }, posicion - 1)
                aMover = null
            },
            onCancelar = { aMover = null },
        )
    }
}

/** [asa] y [cuerpo] son los modificadores de arrastre de Reorderable, no el de diseño. */
@Suppress("ModifierParameter")
@Composable
fun BloqueEjercicio(
    numero: Int,
    ejercicio: Ejercicio,
    arrastrando: Boolean,
    asa: Modifier,
    cuerpo: Modifier,
    onAbrir: () -> Unit,
    opciones: List<Pair<String, () -> Unit>>,
    accionesAccesibles: List<Pair<String, () -> Unit>> = emptyList(),
    detalle: String = ejercicio.resumen(),
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = if (arrastrando) 8.dp else 1.dp),
        modifier = cuerpo
            .fillMaxWidth()
            .clickable(onClick = onAbrir)
            .semantics {
                customActions = accionesAccesibles.map { (texto, accion) ->
                    CustomAccessibilityAction(texto) {
                        accion()
                        true
                    }
                }
            },
    ) {
        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}, modifier = asa) { Icon(Icons.Filled.Menu, contentDescription = "Arrastrar ${ejercicio.nombre}") }
            Text("$numero", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(ejercicio.nombre, style = MaterialTheme.typography.titleMedium)
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (opciones.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de ${ejercicio.nombre}") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        opciones.forEach { (texto, accion) ->
                            DropdownMenuItem(text = { Text(texto) }, onClick = {
                                menu = false
                                accion()
                            })
                        }
                    }
                }
            }
        }
    }
}

/** "Mover a la posición…": pide un número de 1 a [total] (CU-13 2d). */
@Composable
fun DialogoPosicion(total: Int, actual: Int, onElegir: (Int) -> Unit, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf(actual.toString()) }
    val posicion = texto.toIntOrNull()
    val valida = posicion != null && posicion in 1..total
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Mover a la posición") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.filter(Char::isDigit).take(3) },
                label = { Text("Posición (1 a $total)") },
                isError = !valida,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = { posicion?.let(onElegir) }, enabled = valida) { Text("Mover") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

/** Formulario de un ejercicio (CU-13, diseño de interfaz §3.9). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormEjercicioScreen(
    rutinaId: Long,
    id: Long?,
    onCerrar: () -> Unit,
    viewModel: FormEjercicioViewModel = viewModel(factory = FormEjercicioViewModel.factory(rutinaId, id)),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.terminado) { if (estado.terminado) onCerrar() }
    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Editar ejercicio" else "Nuevo ejercicio") },
                navigationIcon = { IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") } },
            )
        },
    ) { modifier ->
        if (estado.cargando) return@Pantalla
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (estado.guardados > 0) {
                Text("✓ ${estado.guardados} guardado${if (estado.guardados > 1) "s" else ""}", color = MaterialTheme.colorScheme.primary)
            }
            OutlinedTextField(
                value = estado.nombre,
                onValueChange = { v -> viewModel.cambiar { it.copy(nombre = v) } },
                label = { Text("Nombre") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Se mide por", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(estado.medida == Medida.REPETICIONES, { viewModel.cambiar { it.copy(medida = Medida.REPETICIONES) } }, { Text("Repeticiones") })
                FilterChip(estado.medida == Medida.TIEMPO, { viewModel.cambiar { it.copy(medida = Medida.TIEMPO) } }, { Text("Tiempo") })
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                ContadorNumero("Series", estado.series, { v -> viewModel.cambiar { it.copy(series = v) } }, minimo = 1)
                if (estado.medida == Medida.REPETICIONES) {
                    ContadorNumero("Repeticiones", estado.repeticiones, { v -> viewModel.cambiar { it.copy(repeticiones = v) } }, minimo = 1)
                } else {
                    ContadorNumero("Duración", estado.duracionSeg, { v -> viewModel.cambiar { it.copy(duracionSeg = v) } }, minimo = 5, paso = 5, formato = { it.comoMinutos() })
                }
            }
            OutlinedTextField(
                value = estado.peso,
                onValueChange = { v -> viewModel.cambiar { it.copy(peso = v.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6)) } },
                label = { Text("Peso en kg (opcional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            SelectorSegundos("Descanso entre series", estado.descansoSeries, "Sin descanso", { v -> viewModel.cambiar { it.copy(descansoSeries = v) } })
            SelectorSegundos(
                "Descanso al terminar",
                estado.descansoTras,
                textoNulo = estado.descansoRutina?.let { "El de la rutina (${it.comoMinutos()})" } ?: "Sin descanso",
                onCambio = { v -> viewModel.cambiar { it.copy(descansoTras = v) } },
            )
            OutlinedTextField(
                value = estado.nota,
                onValueChange = { v -> viewModel.cambiar { it.copy(nota = v) } },
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            if (!estado.esEdicion) {
                OutlinedButton(onClick = { viewModel.guardar(yOtro = true) }, modifier = Modifier.fillMaxWidth()) { Text("Guardar y agregar otro") }
            }
            Button(onClick = { viewModel.guardar() }, modifier = Modifier.fillMaxWidth()) { Text("Guardar") }
        }
    }
}
