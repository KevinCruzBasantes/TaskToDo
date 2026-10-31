package com.calendar.tasktodo.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.ui.rangoHoras
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Scaffold de cada pantalla. Las que están en la barra inferior no suman otra vez el borde
 * inferior del sistema, porque ya lo pone la barra.
 */
@Composable
fun Pantalla(
    enBarra: Boolean,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    contenido: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        contentWindowInsets = if (enBarra) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
    ) { padding -> contenido(Modifier.padding(padding)) }
}

/** Barra vertical con el color de la materia. */
@Composable
fun FranjaMateria(color: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(4.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(2.dp))
            .background(Color(color)),
    )
}

@Composable
fun PuntoColor(color: Int, modifier: Modifier = Modifier, tamano: Int = 14) {
    Box(
        modifier
            .size(tamano.dp)
            .clip(CircleShape)
            .background(Color(color)),
    )
}

/** Hora de inicio y fin, materia y aula. La materia va escrita, no solo por color. */
@Composable
fun FilaClase(
    clase: ClaseConMateria,
    formatoHora: DateTimeFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        FranjaMateria(clase.materiaColor)
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                rangoHoras(clase.clase.horaInicio, clase.clase.horaFin, formatoHora),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(clase.materiaNombre, style = MaterialTheme.typography.titleMedium)
            val detalle = listOfNotNull("Clase", clase.clase.aula?.takeIf { it.isNotBlank() }).joinToString(" · ")
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Nunca una lista vacía sin salida: explica por qué y ofrece la acción (diseño de interfaz §1). */
@Composable
fun EstadoVacio(
    mensaje: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(mensaje, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (detalle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (accion != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAccion) { Text(accion) }
        }
    }
}

/** Confirmación de lo que no se puede deshacer, con el botón destructivo en rojo. */
@Composable
fun DialogoConfirmar(
    titulo: String,
    texto: String,
    confirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(confirmar, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorColorMateria(seleccionado: Int, onSeleccionar: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PaletaMaterias.colores.forEach { opcion ->
            val elegido = opcion.argb == seleccionado
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(opcion.argb))
                    .border(
                        width = if (elegido) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                    )
                    .clickable(role = Role.RadioButton) { onSeleccionar(opcion.argb) }
                    .semantics {
                        contentDescription = opcion.nombre
                        selected = elegido
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (elegido) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
            }
        }
    }
}

/** Campo que muestra una hora y abre el selector de Material 3 al tocarlo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(
    etiqueta: String,
    hora: LocalTime,
    formato: DateTimeFormatter,
    onCambio: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(hora.format(formato), style = MaterialTheme.typography.titleMedium)
        }
    }
    if (abierto) {
        val es24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
        val estado = rememberTimePickerState(hora.hour, hora.minute, es24)
        AlertDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    onCambio(LocalTime.of(estado.hour, estado.minute))
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
            text = { TimePicker(estado) },
        )
    }
}
