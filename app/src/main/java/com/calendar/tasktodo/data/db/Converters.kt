package com.calendar.tasktodo.data.db

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.Instant
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

    /** Instantes en milisegundos desde 1970 (modelo de datos §2.1). */
    @TypeConverter
    fun instanteAMillis(instante: Instant?): Long? = instante?.toEpochMilli()

    @TypeConverter
    fun millisAInstante(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)

    /** Lista de ids como texto separado por comas (`12,15,13`), para `orden_ejercicios`. */
    @TypeConverter
    fun idsATexto(ids: List<Long>?): String? = ids?.joinToString(",")

    @TypeConverter
    fun textoAIds(texto: String?): List<Long>? = texto?.split(",")?.filter { it.isNotBlank() }?.map(String::toLong)

    /** 1 = lunes … 7 = domingo, igual que [DayOfWeek.getValue]. */
    @TypeConverter
    fun diaAEntero(dia: DayOfWeek?): Int? = dia?.value

    @TypeConverter
    fun enteroADia(valor: Int?): DayOfWeek? = valor?.let(DayOfWeek::of)

    private companion object {
        val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
