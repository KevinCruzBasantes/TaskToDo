package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.EstadoSesion
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.data.db.entidad.RegistroPeso
import com.calendar.tasktodo.data.db.entidad.SerieRealizada
import com.calendar.tasktodo.data.db.entidad.SesionGym
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.prueba.DatosDePrueba.HOY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** Los cálculos de las gráficas (CU-18). Hoy es viernes 30 de octubre de 2026. */
class EstadisticasTest {

    private fun sesion(id: Long, fecha: LocalDate, minutos: Int) = SesionGym(
        id = id, actividadId = null, rutinaId = 1, nombreRutina = "Pierna", fecha = fecha,
        inicio = fecha.atTime(18, 0).toInstant(ZoneOffset.UTC), duracionSeg = minutos * 60,
        estado = EstadoSesion.TERMINADA, ordenEjercicios = emptyList(),
    )

    private fun serie(sesionId: Long, numero: Int, reps: Int, kg: Double, ejercicioId: Long = 7) = SerieRealizada(
        sesionId = sesionId, ejercicioId = ejercicioId, nombreEjercicio = if (ejercicioId == 7L) "Sentadilla" else "Prensa",
        ordenEjercicio = 1, numeroSerie = numero, medida = Medida.REPETICIONES, repeticiones = reps, pesoKg = kg,
        completadaEn = HOY.atStartOfDay().toInstant(ZoneOffset.UTC).plusSeconds(sesionId * 100 + numero),
    )

    private fun gym(fecha: LocalDate, estado: EstadoActividad) =
        Actividad(titulo = "Pierna", tipo = TipoActividad.GYM, fecha = fecha, estado = estado)

    private fun calcular(
        periodo: Periodo,
        sesiones: List<SesionGym> = emptyList(),
        series: List<SerieRealizada> = emptyList(),
        actividades: List<Actividad> = emptyList(),
        registros: List<RegistroConImc> = emptyList(),
    ) = CalculoEstadisticas.calcular(periodo, HOY, sesiones, series, actividades, registros)

    @Test
    fun `una semana con sesiones de 50, 40 y 60 minutos suma 150 (CP-6_06)`() {
        // Lunes 26, miércoles 28 y viernes 30 de octubre
        val sesiones = listOf(sesion(1, HOY.minusDays(4), 50), sesion(2, HOY.minusDays(2), 40), sesion(3, HOY, 60))

        val mes = calcular(Periodo.MES, sesiones)

        assertEquals(150, mes.minutosTotal)
        assertEquals(150, mes.barras.single { it.inicio == LocalDate.of(2026, 10, 26) }.minutos)
        // Del 1 de octubre al 30: las semanas que empiezan el 28 de septiembre, 5, 12, 19 y 26
        assertEquals(5, mes.barras.size)
        assertEquals(7, calcular(Periodo.SEMANA, sesiones).barras.size)
    }

    @Test
    fun `3 hechas, 1 no realizada y 1 saltada es 3 de 4 (CP-6_07)`() {
        val actividades = listOf(
            gym(HOY.minusDays(9), EstadoActividad.CUMPLIDA),
            gym(HOY.minusDays(7), EstadoActividad.CUMPLIDA),
            gym(HOY.minusDays(4), EstadoActividad.CUMPLIDA),
            gym(HOY.minusDays(2), EstadoActividad.PENDIENTE),
            gym(HOY.minusDays(1), EstadoActividad.SALTADA),
            gym(HOY.plusDays(3), EstadoActividad.PENDIENTE), // futura: no cuenta
        )
        // Una sesión de la semana que viene hecha hoy sí cuenta
        assertEquals(Cumplimiento(4, 5), calcular(Periodo.MES, actividades = actividades + gym(HOY.plusDays(7), EstadoActividad.CUMPLIDA)).cumplimiento)

        val c = calcular(Periodo.MES, actividades = actividades).cumplimiento

        assertEquals(Cumplimiento(3, 4), c)
        assertEquals(75, c.porcentaje)
    }

    @Test
    fun `progreso de Sentadilla en 3 sesiones (CP-6_08)`() {
        val sesiones = listOf(sesion(1, HOY.minusDays(14), 50), sesion(2, HOY.minusDays(7), 50), sesion(3, HOY, 50))
        val series = listOf(
            serie(1, 1, 10, 60.0), serie(1, 2, 8, 65.0),
            serie(2, 1, 10, 65.0), serie(2, 2, 8, 70.0),
            serie(3, 1, 10, 70.0), serie(3, 2, 6, 75.0),
            serie(3, 1, 12, 120.0, ejercicioId = 8),
        )

        val datos = calcular(Periodo.MES, sesiones, series)

        assertEquals(
            listOf(
                PuntoFuerza(HOY.minusDays(14), 65.0, 10 * 60.0 + 8 * 65.0),
                PuntoFuerza(HOY.minusDays(7), 70.0, 10 * 65.0 + 8 * 70.0),
                PuntoFuerza(HOY, 75.0, 10 * 70.0 + 6 * 75.0),
            ),
            datos.fuerza[7],
        )
        assertEquals(listOf("Prensa", "Sentadilla"), datos.ejercicios.map { it.nombre })
    }

    @Test
    fun `cada periodo usa solo sus datos (CP-6_09)`() {
        val sesiones = listOf(sesion(1, HOY, 30), sesion(2, HOY.minusDays(20), 40), sesion(3, HOY.minusDays(60), 50), sesion(4, HOY.minusDays(200), 60))
        val registros = listOf(HOY, HOY.minusDays(20), HOY.minusDays(60)).map { RegistroConImc(RegistroPeso(fecha = it, pesoKg = 70.0), null) }

        assertEquals(listOf(30, 70, 120, 180), Periodo.entries.map { calcular(it, sesiones).minutosTotal })
        assertEquals(listOf(1, 2, 3, 3), Periodo.entries.map { calcular(it, registros = registros).pesos.size })
        // Todo el historial va por mes, desde el mes de la primera sesión (abril)
        val todo = calcular(Periodo.TODO, sesiones)
        assertEquals(LocalDate.of(2026, 4, 1), todo.barras.first().inicio)
        assertEquals(7, todo.barras.size)
    }

    @Test
    fun `un periodo sin datos deja cada grafica vacia (CP-6_10)`() {
        val datos = calcular(Periodo.SEMANA, sesiones = listOf(sesion(1, HOY.minusDays(30), 50)))

        assertEquals(0, datos.minutosTotal)
        assertTrue(datos.barras.all { it.minutos == 0 })
        assertEquals(0, datos.cumplimiento.planificadas)
        assertNull(datos.cumplimiento.porcentaje)
        assertTrue(datos.pesos.isEmpty())
        assertTrue(datos.fuerza.isEmpty())
    }

    @Test
    fun `el eje vertical se ajusta a los datos para que un solo punto o 1 kg de cambio se vean`() {
        assertEquals(64.0 to 66.0, com.calendar.tasktodo.ui.gym.rangoVertical(listOf(65.0)))
        assertEquals(63.0 to 67.0, com.calendar.tasktodo.ui.gym.rangoVertical(listOf(64.0, 65.4)))
        assertEquals(72.0 to 128.0, com.calendar.tasktodo.ui.gym.rangoVertical(listOf(80.0, 120.0)))
        assertEquals(0.0 to 1.0, com.calendar.tasktodo.ui.gym.rangoVertical(listOf(0.0)))
    }
}
