package com.calendar.tasktodo.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** "1:30"; "0:45". */
fun Int.comoMinutos(): String = "%d:%02d".format(this / 60, this % 60)

/** "1 h 05 min" para duraciones largas, como la de una sesión. */
fun Long.comoReloj(): String {
    val horas = this / 3600
    val minutos = (this % 3600) / 60
    val segundos = this % 60
    return if (horas > 0) "%d:%02d:%02d".format(horas, minutos, segundos) else "%d:%02d".format(minutos, segundos)
}

/**
 * Número con botones − y + para no abrir el teclado (diseño de interfaz §3.9). [formato] cambia
 * cómo se muestra, por ejemplo en minutos y segundos.
 */
@Composable
fun ContadorNumero(
    etiqueta: String,
    valor: Int,
    onCambio: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minimo: Int = 0,
    paso: Int = 1,
    grande: Boolean = false,
    formato: (Int) -> String = { it.toString() },
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onCambio((valor - paso).coerceAtLeast(minimo)) },
                modifier = Modifier.semantics { contentDescription = "Menos $etiqueta" },
            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text(
                formato(valor),
                Modifier.widthIn(min = if (grande) 72.dp else 48.dp),
                style = if (grande) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            FilledTonalIconButton(
                onClick = { onCambio(valor + paso) },
                modifier = Modifier.semantics { contentDescription = "Más $etiqueta" },
            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

/** Opciones de descanso en segundos; nulo es "sin descanso" o "usar el de la rutina". */
val OPCIONES_DESCANSO: List<Int> = listOf(15, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorSegundos(
    etiqueta: String,
    valor: Int?,
    textoNulo: String,
    onCambio: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            value = valor?.comoMinutos() ?: textoNulo,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            (listOf<Int?>(null) + OPCIONES_DESCANSO).forEach { opcion ->
                DropdownMenuItem(text = { Text(opcion?.comoMinutos() ?: textoNulo) }, onClick = {
                    onCambio(opcion)
                    abierto = false
                })
            }
        }
    }
}
