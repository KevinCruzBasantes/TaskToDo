package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.db.dao.GymDao
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.SerieRealizada
import com.calendar.tasktodo.data.db.entidad.SesionGym
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Periodos del filtro de estadísticas (RF-33). Todos terminan hoy. */
enum class Periodo(val texto: String) {
    SEMANA("Semana"),
    MES("Mes"),
    TRES_MESES("3 meses"),
    TODO("Todo");

    /** Primer día del periodo; `null` es todo el historial. */
    fun desde(hoy: LocalDate): LocalDate? = when (this) {
        SEMANA -> hoy.minusDays(6)
        MES -> hoy.minusMonths(1).plusDays(1)
        TRES_MESES -> hoy.minusMonths(3).plusDays(1)
        TODO -> null
    }

    /** La semana se ve por día, el mes y los 3 meses por semana, y todo el historial por mes. */
    val agrupacion: Agrupacion
        get() = when (this) {
            SEMANA -> Agrupacion.DIA
            MES, TRES_MESES -> Agrupacion.SEMANA
            TODO -> Agrupacion.MES
        }
}

enum class Agrupacion { DIA, SEMANA, MES }

/** Una barra de minutos: el día, el lunes de la semana o el 1 del mes. */
data class Barra(val inicio: LocalDate, val minutos: Int)

data class Cumplimiento(val hechas: Int, val planificadas: Int) {
    val porcentaje: Int? get() = if (planificadas == 0) null else hechas * 100 / planificadas
}

data class PuntoFuerza(val fecha: LocalDate, val pesoMaximo: Double, val volumen: Double)

data class EjercicioConDatos(val id: Long, val nombre: String)

data class PuntoPeso(val fecha: LocalDate, val pesoKg: Double, val imc: Double?)

data class Estadisticas(
    val periodo: Periodo,
    val minutosTotal: Int,
    val barras: List<Barra>,
    val cumplimiento: Cumplimiento,
    val pesos: List<PuntoPeso>,
    /** Ejercicios con series con peso en el periodo, para el selector de fuerza. */
    val ejercicios: List<EjercicioConDatos>,
    val fuerza: Map<Long, List<PuntoFuerza>>,
)

/** Los cálculos de las gráficas, sin base de datos, para probarlos con datos fijos (plan de pruebas CP-6.06 a CP-6.10). */
object CalculoEstadisticas {

    fun calcular(
        periodo: Periodo,
        hoy: LocalDate,
        sesiones: List<SesionGym>,
        series: List<SerieRealizada>,
        actividadesGym: List<Actividad>,
        registros: List<RegistroConImc>,
    ): Estadisticas {
        val desde = periodo.desde(hoy)
        fun enPeriodo(fecha: LocalDate) = !fecha.isAfter(hoy) && (desde == null || !fecha.isBefore(desde))

        val delPeriodo = sesiones.filter { enPeriodo(it.fecha) }
        val idsDelPeriodo = delPeriodo.associateBy { it.id }
        val seriesDelPeriodo = series.filter { it.sesionId in idsDelPeriodo }
        val fuerza = fuerza(seriesDelPeriodo, idsDelPeriodo)
        return Estadisticas(
            periodo = periodo,
            minutosTotal = delPeriodo.sumOf { it.duracionSeg ?: 0 } / 60,
            barras = barras(delPeriodo, periodo.agrupacion, desde ?: delPeriodo.minOfOrNull { it.fecha } ?: hoy, hoy),
            cumplimiento = cumplimiento(actividadesGym.filter { desde == null || !it.fecha.isBefore(desde) }, hoy),
            pesos = registros.filter { enPeriodo(it.registro.fecha) }
                .map { PuntoPeso(it.registro.fecha, it.registro.pesoKg, it.imc) }
                .sortedBy { it.fecha },
            ejercicios = seriesDelPeriodo.filter { it.pesoKg != null && it.ejercicioId != null }
                .sortedByDescending { it.completadaEn }
                .distinctBy { it.ejercicioId }
                .map { EjercicioConDatos(it.ejercicioId!!, it.nombreEjercicio) }
                .sortedBy { it.nombre.lowercase() },
            fuerza = fuerza,
        )
    }

    fun inicioDe(fecha: LocalDate, agrupacion: Agrupacion): LocalDate = when (agrupacion) {
        Agrupacion.DIA -> fecha
        Agrupacion.SEMANA -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Agrupacion.MES -> fecha.withDayOfMonth(1)
    }

    /** Una barra por día, semana o mes del periodo, también las que no tienen sesiones. */
    fun barras(sesiones: List<SesionGym>, agrupacion: Agrupacion, desde: LocalDate, hasta: LocalDate): List<Barra> {
        val segundos = sesiones.groupBy { inicioDe(it.fecha, agrupacion) }.mapValues { (_, l) -> l.sumOf { it.duracionSeg ?: 0 } }
        val barras = mutableListOf<Barra>()
        var inicio = inicioDe(desde, agrupacion)
        while (!inicio.isAfter(hasta)) {
            barras += Barra(inicio, (segundos[inicio] ?: 0) / 60)
            inicio = when (agrupacion) {
                Agrupacion.DIA -> inicio.plusDays(1)
                Agrupacion.SEMANA -> inicio.plusWeeks(1)
                Agrupacion.MES -> inicio.plusMonths(1)
            }
        }
        return barras
    }

    /**
     * Hasta hoy; las saltadas no cuentan como planificadas (modelo de datos §4). Una sesión hecha
     * antes de su día sí cuenta, aunque su fecha sea futura.
     */
    fun cumplimiento(actividadesGym: List<Actividad>, hoy: LocalDate): Cumplimiento {
        val planificadas = actividadesGym.filter {
            (!it.fecha.isAfter(hoy) || it.estado == EstadoActividad.CUMPLIDA) && it.estado != EstadoActividad.SALTADA
        }
        return Cumplimiento(planificadas.count { it.estado == EstadoActividad.CUMPLIDA }, planificadas.size)
    }

    /** Por sesión y ejercicio: el peso máximo y el volumen (repeticiones × peso). */
    fun fuerza(series: List<SerieRealizada>, sesiones: Map<Long, SesionGym>): Map<Long, List<PuntoFuerza>> =
        series.filter { it.ejercicioId != null && it.pesoKg != null }
            .groupBy { it.ejercicioId!! }
            .mapValues { (_, delEjercicio) ->
                delEjercicio.groupBy { it.sesionId }
                    .mapNotNull { (sesionId, deLaSesion) ->
                        val sesion = sesiones[sesionId] ?: return@mapNotNull null
                        PuntoFuerza(
                            fecha = sesion.fecha,
                            pesoMaximo = deLaSesion.maxOf { it.pesoKg!! },
                            volumen = deLaSesion.sumOf { (it.repeticiones ?: 0) * it.pesoKg!! },
                        )
                    }
                    .sortedBy { it.fecha }
            }
}

/** Estadísticas (CU-18): solo lee. */
class EstadisticasRepository(private val gym: GymDao, private val peso: PesoRepository) {

    fun estadisticas(periodo: Periodo, hoy: LocalDate): Flow<Estadisticas> = combine(
        gym.observarTerminadas(),
        gym.observarSeriesTerminadas(),
        // Hasta un año adelante, por las sesiones hechas antes de su día
        gym.observarGymEntre(periodo.desde(hoy) ?: LocalDate.of(2000, 1, 1), hoy.plusYears(1)),
        peso.registros,
    ) { sesiones, series, actividades, registros ->
        CalculoEstadisticas.calcular(periodo, hoy, sesiones, series, actividades, registros)
    }
}
