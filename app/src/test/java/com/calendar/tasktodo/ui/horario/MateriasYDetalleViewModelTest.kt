package com.calendar.tasktodo.ui.horario

import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.prueba.observar
import com.calendar.tasktodo.ui.dia.DiaViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class MateriasYDetalleViewModelTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    private val horario = DatosDePrueba.horarioCargado()

    @Test
    fun `la lista cuenta las clases de cada materia`() = runTest {
        val estado = observar(MateriasViewModel(horario.repositorio).estado)

        assertEquals(listOf("Cálculo", "Física", "Inglés"), estado.value.materias.map { it.materia.nombre })
        assertEquals(listOf(3, 1, 0), estado.value.materias.map { it.clases })
        assertEquals(4, estado.value.totalClases)
    }

    @Test
    fun `eliminar todas las materias borra materias y clases (CP-1_05)`() = runTest {
        val vm = MateriasViewModel(horario.repositorio)
        val estado = observar(vm.estado)

        vm.eliminarTodas()

        assertTrue(estado.value.materias.isEmpty())
        assertTrue(horario.clases.value.isEmpty())
    }

    @Test
    fun `eliminar una clase la quita de todas las semanas (CP-1_14)`() = runTest {
        val detalle = DetalleClaseViewModel(horario.repositorio, id = 12)
        val estadoDetalle = observar(detalle.estado)
        val dia = DiaViewModel(horario.repositorio, DatosDePrueba.reloj)
        val estadoDia = observar(dia.estado)
        assertEquals("Cálculo", estadoDetalle.value.clase?.materiaNombre)

        detalle.eliminar()

        assertNull(estadoDetalle.value.clase)
        listOf(DatosDePrueba.HOY, LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 13)).forEach {
            dia.irA(it)
            assertTrue("el $it", estadoDia.value.clases.isEmpty())
        }
    }
}
