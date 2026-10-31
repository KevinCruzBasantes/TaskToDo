package com.calendar.tasktodo.ui.horario

import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.hora
import com.calendar.tasktodo.prueba.HorarioFalso
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

class HorarioViewModelTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @Test
    fun `el horario va de lunes a domingo, solo con los dias que tienen clases (CP-1_09)`() = runTest {
        val horario = DatosDePrueba.horarioCargado()
        val estado = observar(HorarioViewModel(horario.repositorio).estado)

        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            estado.value.dias.map { it.first },
        )
        assertEquals(listOf("Física"), estado.value.dias[1].second.map { it.materiaNombre })
        assertTrue(estado.value.hayMaterias)
    }

    @Test
    fun `dentro de cada dia las clases van por hora`() = runTest {
        val horario = DatosDePrueba.horarioCargado()
        horario.clases.update { lista ->
            lista + com.calendar.tasktodo.data.db.entidad.Clase(
                id = 30, materiaId = 3, diaSemana = DayOfWeek.MONDAY, horaInicio = hora(6), horaFin = hora(7),
            )
        }
        val estado = observar(HorarioViewModel(horario.repositorio).estado)

        assertEquals(listOf(hora(6), hora(7)), estado.value.dias.first().second.map { it.clase.horaInicio })
    }

    @Test
    fun `sin materias lo indica`() = runTest {
        val estado = observar(HorarioViewModel(HorarioFalso().repositorio).estado)

        assertFalse(estado.value.cargando)
        assertFalse(estado.value.hayMaterias)
        assertTrue(estado.value.dias.isEmpty())
    }
}
