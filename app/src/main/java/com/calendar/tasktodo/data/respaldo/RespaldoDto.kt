package com.calendar.tasktodo.data.respaldo

import com.calendar.tasktodo.data.ajustes.AjustesAvisos
import com.calendar.tasktodo.data.ajustes.Frecuencia
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.DiaGym
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.EstadoSesion
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.data.db.entidad.RegistroPeso
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.SerieRealizada
import com.calendar.tasktodo.data.db.entidad.SesionGym
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
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
    val rutinas: List<RutinaDto> = emptyList(),
    val ejercicios: List<EjercicioDto> = emptyList(),
    val diasGym: List<DiaGymDto> = emptyList(),
    val sesiones: List<SesionDto> = emptyList(),
    val series: List<SerieDto> = emptyList(),
    val registrosPeso: List<RegistroPesoDto> = emptyList(),
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
    val avisoGym: Boolean = true,
    val minutosGym: Int = 15,
    val finProgramacionGym: String? = null,
    val estaturaCm: Int? = null,
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
    val rutinaId: Long? = null,
    val diaGymId: Long? = null,
    val fechaProgramada: String? = null,
)

@Serializable
data class RegistroPesoDto(val id: Long, val fecha: String, val pesoKg: Double)

fun RegistroPeso.aDto() = RegistroPesoDto(id, fecha.toString(), pesoKg)

fun RegistroPesoDto.aEntidad() = RegistroPeso(id, LocalDate.parse(fecha), pesoKg)

@Serializable
data class RutinaDto(val id: Long, val nombre: String, val descansoEntreEjerciciosSeg: Int? = null)

@Serializable
data class EjercicioDto(
    val id: Long,
    val rutinaId: Long,
    val orden: Int,
    val nombre: String,
    val medida: String,
    val series: Int,
    val repeticiones: Int? = null,
    val duracionSeg: Int? = null,
    val pesoKg: Double? = null,
    val descansoSeriesSeg: Int? = null,
    val descansoTrasEjercicioSeg: Int? = null,
    val nota: String? = null,
)

@Serializable
data class DiaGymDto(val id: Long, val diaSemana: Int, val hora: String, val rutinaId: Long)

@Serializable
data class SesionDto(
    val id: Long,
    val actividadId: Long? = null,
    val rutinaId: Long? = null,
    val nombreRutina: String,
    val fecha: String,
    val inicio: String,
    val fin: String? = null,
    val duracionSeg: Int? = null,
    val estado: String,
    val descansoTerminaEn: String? = null,
    val ordenEjercicios: List<Long> = emptyList(),
)

@Serializable
data class SerieDto(
    val id: Long,
    val sesionId: Long,
    val ejercicioId: Long? = null,
    val nombreEjercicio: String,
    val ordenEjercicio: Int,
    val numeroSerie: Int,
    val medida: String,
    val repeticiones: Int? = null,
    val duracionSeg: Int? = null,
    val pesoKg: Double? = null,
    val completadaEn: String,
)

fun Rutina.aDto() = RutinaDto(id, nombre, descansoEntreEjerciciosSeg)

fun RutinaDto.aEntidad() = Rutina(id, nombre, descansoEntreEjerciciosSeg)

fun Ejercicio.aDto() = EjercicioDto(
    id, rutinaId, orden, nombre, medida.name, series, repeticiones, duracionSeg, pesoKg, descansoSeriesSeg, descansoTrasEjercicioSeg, nota,
)

fun EjercicioDto.aEntidad() = Ejercicio(
    id, rutinaId, orden, nombre, Medida.valueOf(medida), series, repeticiones, duracionSeg, pesoKg, descansoSeriesSeg,
    descansoTrasEjercicioSeg, nota,
)

fun DiaGym.aDto() = DiaGymDto(id, diaSemana.value, hora.toString(), rutinaId)

fun DiaGymDto.aEntidad() = DiaGym(id, DayOfWeek.of(diaSemana), LocalTime.parse(hora), rutinaId)

fun SesionGym.aDto() = SesionDto(
    id, actividadId, rutinaId, nombreRutina, fecha.toString(), inicio.toString(), fin?.toString(), duracionSeg,
    estado.name, descansoTerminaEn?.toString(), ordenEjercicios,
)

fun SesionDto.aEntidad() = SesionGym(
    id = id,
    actividadId = actividadId,
    rutinaId = rutinaId,
    nombreRutina = nombreRutina,
    fecha = LocalDate.parse(fecha),
    inicio = Instant.parse(inicio),
    fin = fin?.let(Instant::parse),
    duracionSeg = duracionSeg,
    estado = EstadoSesion.valueOf(estado),
    descansoTerminaEn = descansoTerminaEn?.let(Instant::parse),
    ordenEjercicios = ordenEjercicios,
)

fun SerieRealizada.aDto() = SerieDto(
    id, sesionId, ejercicioId, nombreEjercicio, ordenEjercicio, numeroSerie, medida.name, repeticiones, duracionSeg, pesoKg,
    completadaEn.toString(),
)

fun SerieDto.aEntidad() = SerieRealizada(
    id, sesionId, ejercicioId, nombreEjercicio, ordenEjercicio, numeroSerie, Medida.valueOf(medida), repeticiones, duracionSeg,
    pesoKg, Instant.parse(completadaEn),
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
    rutinaId, diaGymId, fechaProgramada?.toString(),
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
    rutinaId = rutinaId,
    diaGymId = diaGymId,
    fechaProgramada = fechaProgramada?.let(LocalDate::parse),
)

fun AjustesAvisos.aDto(automatico: Boolean, frecuencia: Frecuencia, finGym: LocalDate?, estaturaCm: Int? = null) = AjustesDto(
    actividades, horaSinHora.toString(), diaAnterior, horaDiaAnterior.toString(), clases, minutosClase,
    resumen, horaResumen.toString(), prioridadAlarma, automatico, frecuencia.name, gym, minutosGym, finGym?.toString(),
    estaturaCm,
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
    gym = avisoGym,
    minutosGym = minutosGym,
)
