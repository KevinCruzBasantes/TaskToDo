package com.calendar.tasktodo.ui.gym

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calendar.tasktodo.data.db.entidad.RegistroPeso
import com.calendar.tasktodo.data.repositorio.CategoriaImc
import com.calendar.tasktodo.data.repositorio.Imc
import com.calendar.tasktodo.data.repositorio.RegistroConImc
import com.calendar.tasktodo.ui.componentes.DialogoConfirmar
import com.calendar.tasktodo.ui.componentes.DialogoFecha
import com.calendar.tasktodo.ui.componentes.EstadoVacio
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.textoCorto

/** Peso e IMC (CU-17, diseño de interfaz §3.12). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesoScreen(onAtras: () -> Unit, viewModel: PesoViewModel = viewModel(factory = PesoViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var editandoEstatura by rememberSaveable { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<RegistroPeso?>(null) }

    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text("Peso e IMC") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = viewModel::nuevo) { Icon(Icons.Filled.Add, contentDescription = "Agregar registro de peso") } },
    ) { modifier ->
        if (estado.cargando) return@Pantalla
        LazyColumn(modifier.fillMaxSize()) {
            item {
                val estatura = estado.estaturaCm
                if (estatura == null) {
                    CampoEstatura(null, estado.errorEstatura, viewModel::guardarEstatura, Modifier.padding(16.dp))
                } else {
                    TarjetaImc(estado.actual, estatura, onEditarEstatura = { editandoEstatura = true }, modifier = Modifier.padding(16.dp))
                }
            }
            if (estado.registros.isEmpty()) {
                item { EstadoVacio("Aún no registras tu peso", accion = "Agregar registro", onAccion = viewModel::nuevo) }
            }
            items(estado.registros, key = { it.registro.id }) { item ->
                FilaPeso(item, onEditar = { viewModel.editar(item.registro) }, onEliminar = { aEliminar = item.registro })
                HorizontalDivider()
            }
        }
    }

    estado.editor?.let { editor ->
        HojaPeso(editor, viewModel)
    }
    if (editandoEstatura) {
        AlertDialog(
            onDismissRequest = { editandoEstatura = false },
            title = { Text("Estatura") },
            text = {
                CampoEstatura(estado.estaturaCm, estado.errorEstatura, onGuardar = {
                    viewModel.guardarEstatura(it)
                    editandoEstatura = false
                })
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { editandoEstatura = false }) { Text("Cancelar") } },
        )
    }
    aEliminar?.let { registro ->
        DialogoConfirmar(
            titulo = "¿Eliminar el registro del ${registro.fecha.textoCorto()}?",
            texto = "${textoPeso(registro.pesoKg)} kg. No se puede deshacer.",
            confirmar = "Eliminar",
            onConfirmar = {
                viewModel.eliminar(registro)
                aEliminar = null
            },
            onCancelar = { aEliminar = null },
        )
    }
}

/** Peso, IMC, categoría y barra de categorías (diseño de interfaz §4, `TarjetaIMC`). */
@Composable
fun TarjetaImc(actual: RegistroConImc?, estaturaCm: Int, onEditarEstatura: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    actual?.let { "${textoPeso(it.registro.pesoKg)} kg" } ?: "— kg",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                actual?.imc?.let { imc ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text("IMC ${textoImc(imc)}", style = MaterialTheme.typography.headlineSmall)
                        Text(Imc.categoria(imc).texto, style = MaterialTheme.typography.titleMedium, color = colorCategoria(Imc.categoria(imc)))
                    }
                }
            }
            actual?.imc?.let { BarraImc(it, Modifier.padding(top = 12.dp)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Estatura $estaturaCm cm", Modifier.weight(1f))
                IconButton(onClick = onEditarEstatura) { Icon(Icons.Filled.Edit, contentDescription = "Cambiar estatura") }
            }
            Text(
                "Dato orientativo; no reemplaza una evaluación médica.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val IMC_MIN = 15.0
private const val IMC_MAX = 35.0

@Composable
private fun colorCategoria(categoria: CategoriaImc): Color = when (categoria) {
    CategoriaImc.BAJO_PESO -> Color(0xFF1E88E5)
    CategoriaImc.NORMAL -> Color(0xFF43A047)
    CategoriaImc.SOBREPESO -> Color(0xFFFB8C00)
    CategoriaImc.OBESIDAD -> Color(0xFFE53935)
}

/** Las cuatro categorías de la OMS en una barra, con un punto donde está el IMC actual. */
@Composable
fun BarraImc(imc: Double, modifier: Modifier = Modifier) {
    val tramos = listOf(
        IMC_MIN to 18.5 to colorCategoria(CategoriaImc.BAJO_PESO),
        18.5 to 25.0 to colorCategoria(CategoriaImc.NORMAL),
        25.0 to 30.0 to colorCategoria(CategoriaImc.SOBREPESO),
        30.0 to IMC_MAX to colorCategoria(CategoriaImc.OBESIDAD),
    )
    val marca = MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(18.dp),
        ) {
            fun x(valor: Double) = ((valor.coerceIn(IMC_MIN, IMC_MAX) - IMC_MIN) / (IMC_MAX - IMC_MIN) * size.width).toFloat()
            val alto = 8.dp.toPx()
            val arriba = (size.height - alto) / 2
            tramos.forEach { (rango, color) ->
                drawRect(color, topLeft = Offset(x(rango.first), arriba), size = Size(x(rango.second) - x(rango.first), alto))
            }
            drawCircle(marca, radius = size.height / 2, center = Offset(x(imc), size.height / 2))
        }
        Box(Modifier.fillMaxWidth()) {
            listOf(18.5, 25.0, 30.0).forEach { limite ->
                val fraccion = ((limite - IMC_MIN) / (IMC_MAX - IMC_MIN)).toFloat()
                Text(
                    if (limite % 1.0 == 0.0) "${limite.toInt()}" else "$limite",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth(fraccion)
                        .padding(end = 0.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun CampoEstatura(actual: Int?, error: String?, onGuardar: (String) -> Unit, modifier: Modifier = Modifier) {
    var texto by rememberSaveable { mutableStateOf(actual?.toString().orEmpty()) }
    Column(modifier) {
        if (actual == null) Text("Agrega tu estatura para ver tu IMC", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.filter(Char::isDigit).take(3) },
                label = { Text("Estatura en cm") },
                singleLine = true,
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { onGuardar(texto) }) { Text("Guardar") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Tocar edita; deslizar elimina, con confirmación. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaPeso(item: RegistroConImc, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val deslizar = rememberSwipeToDismissBoxState()
    LaunchedEffect(deslizar.currentValue) {
        if (deslizar.currentValue != SwipeToDismissBoxValue.Settled) {
            onEliminar()
            deslizar.reset()
        }
    }
    SwipeToDismissBox(
        state = deslizar,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Text("Eliminar", color = MaterialTheme.colorScheme.onErrorContainer) }
        },
    ) {
        ListItem(
            headlineContent = { Text(item.registro.fecha.textoCorto()) },
            trailingContent = {
                Text(
                    "${textoPeso(item.registro.pesoKg)} kg" + (item.imc?.let { "   IMC ${textoImc(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            modifier = Modifier.clickable(onClick = onEditar),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaPeso(editor: EditorPeso, viewModel: PesoViewModel) {
    var eligiendoFecha by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = viewModel::cerrarEditor) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text(if (editor.esEdicion) "Editar registro" else "Nuevo registro", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = editor.peso,
                onValueChange = viewModel::cambiarPeso,
                label = { Text("Peso en kg") },
                singleLine = true,
                isError = editor.error != null,
                supportingText = editor.error?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            OutlinedButton(onClick = { eligiendoFecha = true }) { Text(editor.fecha.textoCorto()) }
            Button(onClick = { viewModel.guardar() }, modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)) { Text("Guardar") }
        }
    }
    if (eligiendoFecha) {
        DialogoFecha(inicial = editor.fecha, onElegir = {
            viewModel.cambiarFecha(it)
            eligiendoFecha = false
        }, onCancelar = { eligiendoFecha = false })
    }
    editor.ocupada?.let { existente ->
        DialogoConfirmar(
            titulo = "Ya hay un registro ese día",
            texto = "El ${existente.fecha.textoCorto()} tiene ${textoPeso(existente.pesoKg)} kg. ¿Lo reemplazas por ${editor.peso} kg?",
            confirmar = "Reemplazar",
            onConfirmar = { viewModel.guardar(reemplazar = true) },
            onCancelar = viewModel::noReemplazar,
        )
    }
}
