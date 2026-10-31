package com.calendar.tasktodo.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
