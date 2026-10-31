package com.calendar.tasktodo.ui.dia

import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.hora
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.prueba.observar
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DiaViewModelTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    private val horario = DatosDePrueba.horarioCargado()

    private fun vm(fecha: LocalDate? = null) = DiaViewModel(horario.repositorio, DatosDePrueba.reloj, fecha)

    @Test
    fun `abre en el dia actual (RF-18)`() = runTest {
        val estado = observar(vm().estado)

        assertEquals(DatosDePrueba.HOY, estado.value.fecha)
        assertTrue(estado.value.esHoy)
    }

    @Test
    fun `el viernes muestra sus clases ordenadas por hora con su materia (CP-1_11)`() = runTest {
        horario.clases.update {
            it + Clase(id = 20, materiaId = 3, diaSemana = DayOfWeek.FRIDAY, horaInicio = hora(11), horaFin = hora(12)) +
                Clase(id = 21, materiaId = 2, diaSemana = DayOfWeek.FRIDAY, horaInicio = hora(9, 30), horaFin = hora(10, 30))
        }
        val estado = observar(vm().estado)

        assertEquals(listOf("Cálculo", "Física", "Inglés"), estado.value.clases.map { it.materiaNombre })
        assertEquals(DatosDePrueba.AZUL, estado.value.clases.first().materiaColor)
        assertFalse(estado.value.cargando)
    }

    @Test
    fun `las clases se repiten cada semana sin guardarse por fecha (CP-1_08)`() = runTest {
        val viewModel = vm()
        val estado = observar(viewModel.estado)

        listOf(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 6)).forEach { fecha ->
            viewModel.irA(fecha)
            assertEquals("el $fecha", listOf("Cálculo"), estado.value.clases.map { it.materiaNombre })
        }
        assertEquals(4, horario.clases.value.size)
    }

    @Test
    fun `anterior siguiente y volver a hoy (CP-1_10)`() = runTest {
        val viewModel = vm()
        val estado = observar(viewModel.estado)

        viewModel.anterior()
        assertEquals(LocalDate.of(2026, 10, 29), estado.value.fecha)
        assertFalse(estado.value.esHoy)
        assertTrue(estado.value.clases.isEmpty())

        viewModel.siguiente()
        viewModel.siguiente()
        assertEquals(LocalDate.of(2026, 10, 31), estado.value.fecha)

        viewModel.irAHoy()
        assertEquals(DatosDePrueba.HOY, estado.value.fecha)
        assertTrue(estado.value.esHoy)
    }

    @Test
    fun `abre en la fecha pedida`() = runTest {
        val estado = observar(vm(LocalDate.of(2026, 11, 3)).estado)

        assertEquals(listOf("Física"), estado.value.clases.map { it.materiaNombre })
        assertFalse(estado.value.esHoy)
    }

    @Test
    fun `una clase nueva aparece sin recargar`() = runTest {
        val estado = observar(vm().estado)
        assertEquals(1, estado.value.clases.size)

        horario.clases.update {
            it + Clase(id = 22, materiaId = 3, diaSemana = DayOfWeek.FRIDAY, horaInicio = hora(15), horaFin = hora(16))
        }

        assertEquals(2, estado.value.clases.size)
    }
}
