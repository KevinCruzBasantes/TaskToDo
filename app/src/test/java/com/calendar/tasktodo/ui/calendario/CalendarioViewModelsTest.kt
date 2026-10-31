package com.calendar.tasktodo.ui.calendario

import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.HOY
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.prueba.observar
import com.calendar.tasktodo.ui.dia.ElementoDia
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

class CalendarioViewModelsTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    private val horario = DatosDePrueba.horarioCargado()

    private fun semana(reloj: Clock = DatosDePrueba.reloj) = SemanaViewModel(horario.repositorio, horario.actividadRepositorio, reloj)

    private fun mes() = MesViewModel(horario.actividadRepositorio, DatosDePrueba.reloj)

    private fun DiaDeSemana.titulos() = conHora.map {
        when (it) {
            is ElementoDia.DeClase -> it.clase.materiaNombre
            is ElementoDia.DeActividad -> it.actividad.actividad.titulo
        }
    } + sinHora.map { it.actividad.titulo }

    @Test
    fun `la semana empieza el lunes 26 y cada dia trae sus clases y actividades en orden (CP-4_01)`() = runTest {
        val estado = observar(semana().estado)

        assertEquals(LocalDate.of(2026, 10, 26), estado.value.lunes)
        assertEquals((26..31).map { LocalDate.of(2026, 10, it) } + LocalDate.of(2026, 11, 1), estado.value.dias.map { it.fecha })
        assertEquals(DayOfWeek.MONDAY, estado.value.dias.first().fecha.dayOfWeek)
        assertEquals(listOf("Física", "Informe de laboratorio"), estado.value.dias[1].titulos())
        assertEquals(listOf("Parcial de Inglés"), estado.value.dias[3].titulos())
        assertEquals(listOf("Cálculo", "Leer artículo"), estado.value.dias[4].titulos())
        assertTrue(estado.value.dias[4].esHoy)
        assertTrue(estado.value.dias[5].vacio)
    }

    @Test
    fun `el mes de noviembre empieza en domingo y muestra solo actividades (CP-4_02)`() = runTest {
        val viewModel = mes()
        val estado = observar(viewModel.estado)

        viewModel.siguiente()

        assertEquals(YearMonth.of(2026, 11), estado.value.mes)
        val primeraFila = estado.value.semanas.first()
        assertEquals(6, primeraFila.count { it == null })
        assertEquals(LocalDate.of(2026, 11, 1), primeraFila.last()?.fecha)
        assertTrue(estado.value.semanas.all { it.size == 7 })
        val dia6 = estado.value.semanas.flatten().first { it?.fecha == LocalDate.of(2026, 11, 6) }!!
        assertEquals(1, dia6.actividades)
        // Los lunes tienen clase de Cálculo, pero el mes no las cuenta
        assertEquals(0, estado.value.semanas.flatten().first { it?.fecha == LocalDate.of(2026, 11, 2) }!!.actividades)
    }

    @Test
    fun `tocar un dia del mes muestra su lista y se puede ir a su semana (CP-4_03)`() = runTest {
        val viewModel = mes()
        val estado = observar(viewModel.estado)
        assertNull(estado.value.seleccionado)

        viewModel.seleccionar(HOY)
        assertEquals(listOf("Leer artículo"), estado.value.delSeleccionado.map { it.actividad.titulo })

        val vistaSemana = semana()
        val estadoSemana = observar(vistaSemana.estado)
        vistaSemana.irA(LocalDate.of(2026, 11, 18))
        assertEquals(LocalDate.of(2026, 11, 16), estadoSemana.value.lunes)
        assertFalse(estadoSemana.value.contieneHoy)

        viewModel.siguiente()
        assertNull(estado.value.seleccionado)
    }

    @Test
    fun `la senal sale solo en dias pasados con actividades sin check y sin contar el gym (CP-4_04)`() = runTest {
        horario.actividades.update {
            it + listOf(
                Actividad(id = 70, titulo = "Gym", tipo = TipoActividad.GYM, fecha = HOY.minusDays(4)),
                Actividad(id = 71, titulo = "Hecha", tipo = TipoActividad.TAREA, fecha = HOY.minusDays(2), estado = EstadoActividad.CUMPLIDA),
                Actividad(id = 72, titulo = "Futura", tipo = TipoActividad.TAREA, fecha = HOY.plusDays(1)),
            )
        }
        val estado = observar(semana().estado)
        val senales = estado.value.dias.associate { it.fecha.dayOfMonth to it.senal }

        assertEquals(true, senales[27]) // tarea atrasada sin check
        assertEquals(true, senales[29]) // examen sin marcar
        assertEquals(false, senales[26]) // solo gym no realizado
        assertEquals(false, senales[28]) // cumplida
        assertEquals(false, senales[30]) // hoy
        assertEquals(false, senales[31]) // futura

        val estadoMes = observar(mes().estado)
        assertTrue(estadoMes.value.semanas.flatten().first { it?.fecha == HOY.minusDays(3) }!!.senal)
    }

    @Test
    fun `una semana que cruza el cambio de anio tiene sus 7 dias correctos (CP-4_05)`() = runTest {
        val diciembre = Clock.fixed(LocalDateTime.of(2026, 12, 30, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        val viewModel = semana(diciembre)
        val estado = observar(viewModel.estado)

        assertEquals(LocalDate.of(2026, 12, 28), estado.value.lunes)
        assertEquals(LocalDate.of(2027, 1, 3), estado.value.dias.last().fecha)

        viewModel.siguiente()
        assertEquals(LocalDate.of(2027, 1, 4), estado.value.lunes)
        viewModel.irAHoy()
        assertTrue(estado.value.contieneHoy)
    }
}
