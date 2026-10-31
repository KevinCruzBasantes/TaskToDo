package com.calendar.tasktodo.data.respaldo

import com.calendar.tasktodo.data.ajustes.AjustesAvisos
import com.calendar.tasktodo.data.ajustes.Frecuencia
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * El archivo de respaldo (RNF-06, diagramas de comportamiento §2.2). Fechas en ISO y horas en
 * `HH:mm`, igual que en la base de datos. Las listas tienen valor por defecto para que un respaldo
 * de una versión anterior, sin las tablas nuevas, se pueda restaurar (modelo de datos §5).
 */
@Serializable
data class RespaldoDto(
    val versionBaseDatos: Int,
    val versionApp: String,
    /** Instante en ISO-8601, en UTC. */
    val fecha: String,
    val ajustes: AjustesDto = AjustesDto(),
    val materias: List<MateriaDto> = emptyList(),
    val clases: List<ClaseDto> = emptyList(),
    val actividades: List<ActividadDto> = emptyList(),
)

/** Los ajustes que viajan en el respaldo; la carpeta y el estado interno son de cada instalación. */
@Serializable
data class AjustesDto(
    val avisosActividades: Boolean = true,
    val horaSinHora: String = "08:00",
    val avisoDiaAnterior: Boolean = true,
    val horaDiaAnterior: String = "20:00",
    val avisosClases: Boolean = false,
    val minutosClase: Int = 10,
    val resumen: Boolean = true,
    val horaResumen: String = "07:00",
    val prioridadAlarma: Boolean = true,
    val respaldoAutomatico: Boolean = false,
    val frecuenciaRespaldo: String = Frecuencia.SEMANAL.name,
)

@Serializable
data class MateriaDto(val id: Long, val nombre: String, val color: Int)

@Serializable
data class ClaseDto(
    val id: Long,
    val materiaId: Long,
    /** 1 = lunes … 7 = domingo. */
    val diaSemana: Int,
    val horaInicio: String,
    val horaFin: String,
    val aula: String? = null,
)

@Serializable
data class ActividadDto(
    val id: Long,
    val titulo: String,
    val tipo: String,
    val fecha: String,
    val hora: String? = null,
    val materiaId: Long? = null,
    val notas: String? = null,
    val avisoMinutosAntes: Int? = null,
    val estado: String = EstadoActividad.PENDIENTE.name,
)

fun Materia.aDto() = MateriaDto(id, nombre, color)

fun MateriaDto.aEntidad() = Materia(id, nombre, color)

fun Clase.aDto() = ClaseDto(id, materiaId, diaSemana.value, horaInicio.toString(), horaFin.toString(), aula)

fun ClaseDto.aEntidad() = Clase(
    id = id,
    materiaId = materiaId,
    diaSemana = DayOfWeek.of(diaSemana),
    horaInicio = LocalTime.parse(horaInicio),
    horaFin = LocalTime.parse(horaFin),
    aula = aula,
)

fun Actividad.aDto() = ActividadDto(
    id, titulo, tipo.name, fecha.toString(), hora?.toString(), materiaId, notas, avisoMinutosAntes, estado.name,
)

fun ActividadDto.aEntidad() = Actividad(
    id = id,
    titulo = titulo,
    tipo = TipoActividad.valueOf(tipo),
    fecha = LocalDate.parse(fecha),
    hora = hora?.let(LocalTime::parse),
    materiaId = materiaId,
    notas = notas,
    avisoMinutosAntes = avisoMinutosAntes,
    estado = EstadoActividad.valueOf(estado),
)

fun AjustesAvisos.aDto(automatico: Boolean, frecuencia: Frecuencia) = AjustesDto(
    actividades, horaSinHora.toString(), diaAnterior, horaDiaAnterior.toString(), clases, minutosClase,
    resumen, horaResumen.toString(), prioridadAlarma, automatico, frecuencia.name,
)

fun AjustesDto.avisos() = AjustesAvisos(
    actividades = avisosActividades,
    horaSinHora = LocalTime.parse(horaSinHora),
    diaAnterior = avisoDiaAnterior,
    horaDiaAnterior = LocalTime.parse(horaDiaAnterior),
    clases = avisosClases,
    minutosClase = minutosClase,
    resumen = resumen,
    horaResumen = LocalTime.parse(horaResumen),
    prioridadAlarma = prioridadAlarma,
)
