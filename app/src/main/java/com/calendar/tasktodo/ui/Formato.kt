package com.calendar.tasktodo.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Formato local: fechas en español y semana que empieza el lunes (diseño de interfaz §1). */
val ESPANOL: Locale = Locale.forLanguageTag("es")

private val FORMATO_FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL)

/** "Viernes 30 de octubre". */
fun LocalDate.textoLargo(): String = format(FORMATO_FECHA_LARGA).replaceFirstChar { it.titlecase(ESPANOL) }

/** "Lunes". */
fun DayOfWeek.nombre(): String = getDisplayName(TextStyle.FULL, ESPANOL).replaceFirstChar { it.titlecase(ESPANOL) }

/** "lun". */
fun DayOfWeek.nombreCorto(): String = getDisplayName(TextStyle.SHORT, ESPANOL).removeSuffix(".")

/** Formato de hora en 12 o 24 horas, según el ajuste del celular. */
@Composable
fun rememberFormatoHora(): DateTimeFormatter {
    val context = LocalContext.current
    val es24 = DateFormat.is24HourFormat(context)
    return remember(es24) { DateTimeFormatter.ofPattern(if (es24) "HH:mm" else "h:mm a", ESPANOL) }
}

fun rangoHoras(inicio: LocalTime, fin: LocalTime, formato: DateTimeFormatter): String =
    "${inicio.format(formato)} – ${fin.format(formato)}"

private val FORMATO_FECHA_CORTA = DateTimeFormatter.ofPattern("EEE d MMM", ESPANOL)

/** "vie 30 oct". */
fun LocalDate.textoCorto(): String = format(FORMATO_FECHA_CORTA).replace(".", "")

fun TipoActividad.nombre(): String = when (this) {
    TipoActividad.TAREA -> "Tarea"
    TipoActividad.CHARLA -> "Charla"
    TipoActividad.SIMULACRO -> "Simulacro"
    TipoActividad.EXAMEN -> "Examen"
    TipoActividad.GYM -> "Gym"
}

/** Un ícono fijo por tipo, para distinguirlos aunque tengan la misma materia (diseño de interfaz §1). */
fun TipoActividad.icono(): String = when (this) {
    TipoActividad.TAREA -> "🗒"
    TipoActividad.CHARLA -> "🎤"
    TipoActividad.SIMULACRO -> "⏱"
    TipoActividad.EXAMEN -> "📝"
    TipoActividad.GYM -> "🏋"
}

/** Opciones de aviso del formulario de actividad, en minutos antes; nulo = sin aviso. */
val OPCIONES_AVISO: List<Int?> = listOf(null, 0, 5, 10, 15, 30, 60, 120, 1440)

fun textoAviso(minutos: Int?, conHora: Boolean, horaSinHora: LocalTime, formato: DateTimeFormatter): String = when {
    minutos == null -> "Sin aviso"
    !conHora -> "A las ${horaSinHora.format(formato)}"
    minutos == 0 -> "A la hora"
    minutos < 60 -> "$minutos min antes"
    minutos == 1440 -> "1 día antes"
    else -> "${minutos / 60} h antes"
}
