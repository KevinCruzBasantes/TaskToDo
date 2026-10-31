package com.calendar.tasktodo.data.db

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Fechas como texto ISO y horas como `HH:mm`: se ordenan bien en SQL y se leen
 * sin conversión en el JSON del respaldo (modelo de datos §2.1).
 */
class Converters {

    @TypeConverter
    fun horaATexto(hora: LocalTime?): String? = hora?.format(FORMATO_HORA)

    @TypeConverter
    fun textoAHora(texto: String?): LocalTime? = texto?.let { LocalTime.parse(it, FORMATO_HORA) }

    @TypeConverter
    fun fechaATexto(fecha: LocalDate?): String? = fecha?.toString()

    @TypeConverter
    fun textoAFecha(texto: String?): LocalDate? = texto?.let(LocalDate::parse)

    /** 1 = lunes … 7 = domingo, igual que [DayOfWeek.getValue]. */
    @TypeConverter
    fun diaAEntero(dia: DayOfWeek?): Int? = dia?.value

    @TypeConverter
    fun enteroADia(valor: Int?): DayOfWeek? = valor?.let(DayOfWeek::of)

    private companion object {
        val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
