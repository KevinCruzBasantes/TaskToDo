package com.calendar.tasktodo.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.Situacion
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.db.entidad.situacion
import com.calendar.tasktodo.ui.icono
import com.calendar.tasktodo.ui.nombre
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Una opción del menú ⋮ de una fila. */
data class OpcionMenu(val texto: String, val accion: () -> Unit)

/**
 * Check, ícono del tipo, título, materia y estado. El check es de un solo toque y su área es de
 * 48 dp (RNF-08). Una cumplida se ve tachada y atenuada.
 */
@Composable
fun FilaActividad(
    item: ActividadConMateria,
    hoy: LocalDate,
    formatoHora: DateTimeFormatter,
    onCambiarCumplida: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detalleExtra: String? = null,
    menu: List<OpcionMenu> = emptyList(),
) {
    val actividad = item.actividad
    val situacion = actividad.situacion(hoy)
    val cumplida = situacion == Situacion.CUMPLIDA
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
            .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (actividad.tipo == TipoActividad.GYM) {
            // El gym no lleva check: se cumple al guardar su sesión (CU-08)
            Spacer(Modifier.width(48.dp))
        } else {
            Checkbox(
                checked = cumplida,
                onCheckedChange = onCambiarCumplida,
                modifier = Modifier.semantics {
                    contentDescription = if (cumplida) "Desmarcar ${actividad.titulo}" else "Marcar ${actividad.titulo}"
                },
            )
        }
        item.materiaColor?.let { FranjaMateria(it, Modifier.padding(vertical = 4.dp)) }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 10.dp)
                .alpha(if (cumplida) 0.55f else 1f),
        ) {
            Text(
                "${actividad.tipo.icono()} ${actividad.titulo}",
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (cumplida) TextDecoration.LineThrough else null,
            )
            val partes = listOfNotNull(
                actividad.hora?.format(formatoHora),
                actividad.tipo.nombre(),
                item.materiaNombre,
                detalleExtra,
                when (situacion) {
                    Situacion.NO_MARCADA -> "No marcado"
                    Situacion.NO_REALIZADA -> "No realizada"
                    Situacion.SALTADA -> "Saltada"
                    else -> null
                },
            )
            Text(
                partes.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = if (situacion == Situacion.NO_MARCADA) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (menu.isNotEmpty()) MenuFila(actividad.titulo, menu)
    }
}

@Composable
private fun MenuFila(titulo: String, opciones: List<OpcionMenu>) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de $titulo")
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.texto) },
                    onClick = {
                        abierto = false
                        opcion.accion()
                    },
                )
            }
        }
    }
}
