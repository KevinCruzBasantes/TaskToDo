package com.calendar.tasktodo.ui.horario

import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.hora
import com.calendar.tasktodo.prueba.HorarioFalso
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek

class FormClaseViewModelTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    private val horario = DatosDePrueba.horarioCargado()

    private fun nueva(materiaId: Long? = DatosDePrueba.ingles.id) = FormClaseViewModel(horario.repositorio, id = null, materiaId)

    @Test
    fun `una clase con fin igual al inicio no se guarda (CP-1_06)`() = runTest {
        val vm = nueva()
        vm.cambiarInicio(hora(10))
        vm.cambiarFin(hora(10))

        vm.guardar()

        assertEquals("La hora de fin debe ser posterior a la de inicio", vm.estado.value.errorHoras)
        assertFalse(vm.estado.value.terminado)
        assertEquals(4, horario.clases.value.size)
    }

    @Test
    fun `una clase con fin anterior al inicio no se guarda (CP-1_06)`() = runTest {
        val vm = nueva()
        vm.cambiarInicio(hora(10))
        vm.cambiarFin(hora(9, 30))

        vm.guardar()

        assertEquals("La hora de fin debe ser posterior a la de inicio", vm.estado.value.errorHoras)
        assertFalse(vm.estado.value.terminado)
    }

    @Test
    fun `sin materia elegida no se guarda`() = runTest {
        val vm = nueva(materiaId = null)
        vm.guardar()

        assertEquals("Elige una materia", vm.estado.value.errorMateria)
        assertFalse(vm.estado.value.terminado)
    }

    @Test
    fun `una clase que se cruza con otra pide confirmar y no se guarda (CP-1_07)`() = runTest {
        val vm = nueva()
        vm.cambiarDia(DayOfWeek.TUESDAY)
        vm.cambiarInicio(hora(10))
        vm.cambiarFin(hora(12))

        vm.guardar()

        assertEquals(listOf("Física"), vm.estado.value.cruces.map { it.materiaNombre })
        assertFalse(vm.estado.value.terminado)
        assertEquals(4, horario.clases.value.size)
    }

    @Test
    fun `al confirmar el cruce la clase se guarda (CP-1_07)`() = runTest {
        val vm = nueva()
        vm.cambiarDia(DayOfWeek.TUESDAY)
        vm.cambiarInicio(hora(10))
        vm.cambiarFin(hora(12))
        vm.guardar()

        vm.guardar(confirmarCruce = true)

        assertTrue(vm.estado.value.terminado)
        assertTrue(vm.estado.value.cruces.isEmpty())
        assertEquals(5, horario.clases.value.size)
    }

    @Test
    fun `dos clases que solo se tocan no se cruzan`() = runTest {
        val vm = nueva()
        vm.cambiarDia(DayOfWeek.TUESDAY)
        vm.cambiarInicio(hora(11))
        vm.cambiarFin(hora(13))

        vm.guardar()

        assertTrue(vm.estado.value.cruces.isEmpty())
        assertTrue(vm.estado.value.terminado)
    }

    @Test
    fun `una clase nueva guarda el aula sin espacios y nula si esta vacia`() = runTest {
        val vm = nueva()
        vm.cambiarDia(DayOfWeek.THURSDAY)
        vm.cambiarAula("   ")
        vm.guardar()

        val guardada = horario.clases.value.last()
        assertEquals(DatosDePrueba.ingles.id, guardada.materiaId)
        assertEquals(DayOfWeek.THURSDAY, guardada.diaSemana)
        assertNull(guardada.aula)
    }

    @Test
    fun `al editar una clase no se cruza consigo misma (CP-1_14)`() = runTest {
        val vm = FormClaseViewModel(horario.repositorio, id = 13, materiaInicial = null)
        assertEquals(DayOfWeek.TUESDAY, vm.estado.value.dia)
        assertEquals(hora(9), vm.estado.value.horaInicio)

        vm.cambiarFin(hora(12))
        vm.guardar()

        assertTrue(vm.estado.value.terminado)
        assertEquals(hora(12), horario.clases.value.first { it.id == 13L }.horaFin)
        assertEquals(4, horario.clases.value.size)
    }

    @Test
    fun `con una sola materia queda elegida sola`() = runTest {
        val unaMateria = HorarioFalso().apply { materias.value = listOf(DatosDePrueba.fisica) }
        val vm = FormClaseViewModel(unaMateria.repositorio, id = null, materiaInicial = null)

        assertEquals(DatosDePrueba.fisica.id, vm.estado.value.materiaId)
    }
}
