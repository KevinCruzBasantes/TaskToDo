package com.calendar.tasktodo.ui.horario

import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.HorarioFalso
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.ui.componentes.PaletaMaterias
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FormMateriaViewModelTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @Test
    fun `una materia sin nombre no se guarda (CP-1_03)`() = runTest {
        val horario = HorarioFalso()
        val vm = FormMateriaViewModel(horario.repositorio, id = null)

        vm.cambiarNombre("   ")
        vm.guardar()

        assertEquals("El nombre es obligatorio", vm.estado.value.error)
        assertFalse(vm.estado.value.terminado)
        assertTrue(horario.materias.value.isEmpty())
    }

    @Test
    fun `un nombre repetido con otras mayusculas no se guarda (CP-1_03)`() = runTest {
        val horario = DatosDePrueba.horarioCargado()
        val vm = FormMateriaViewModel(horario.repositorio, id = null)

        vm.cambiarNombre("cálculo")
        vm.guardar()

        assertEquals("Ya existe una materia con ese nombre", vm.estado.value.error)
        assertFalse(vm.estado.value.terminado)
        assertEquals(3, horario.materias.value.size)
    }

    @Test
    fun `un nombre repetido con la tilde en mayuscula tampoco se guarda`() = runTest {
        val horario = HorarioFalso().apply { materias.value = listOf(DatosDePrueba.calculo.copy(nombre = "Álgebra")) }
        val vm = FormMateriaViewModel(horario.repositorio, id = null)

        vm.cambiarNombre("ÁLGEBRA")
        vm.guardar()

        assertEquals("Ya existe una materia con ese nombre", vm.estado.value.error)
    }

    @Test
    fun `una materia valida se guarda sin espacios sobrantes`() = runTest {
        val horario = HorarioFalso()
        val vm = FormMateriaViewModel(horario.repositorio, id = null)

        vm.cambiarNombre("  Química ")
        vm.cambiarColor(DatosDePrueba.VERDE)
        vm.guardar()

        assertTrue(vm.estado.value.terminado)
        val guardada = horario.materias.value.single()
        assertEquals("Química", guardada.nombre)
        assertEquals(DatosDePrueba.VERDE, guardada.color)
    }

    @Test
    fun `al editar se puede conservar el propio nombre`() = runTest {
        val horario = DatosDePrueba.horarioCargado()
        val vm = FormMateriaViewModel(horario.repositorio, id = DatosDePrueba.calculo.id)

        assertEquals("Cálculo", vm.estado.value.nombre)
        assertEquals(3, vm.estado.value.clasesDeLaMateria)
        vm.cambiarColor(DatosDePrueba.NARANJA)
        vm.guardar()

        assertNull(vm.estado.value.error)
        assertTrue(vm.estado.value.terminado)
        assertEquals(DatosDePrueba.NARANJA, horario.materias.value.first { it.id == 1L }.color)
    }

    @Test
    fun `eliminar una materia borra sus clases (CP-1_04)`() = runTest {
        val horario = DatosDePrueba.horarioCargado()
        val vm = FormMateriaViewModel(horario.repositorio, id = DatosDePrueba.calculo.id)

        vm.eliminar()

        assertTrue(vm.estado.value.terminado)
        assertNull(horario.materias.value.firstOrNull { it.id == 1L })
        assertTrue(horario.clases.value.none { it.materiaId == 1L })
        assertEquals(1, horario.clases.value.size)
    }

    @Test
    fun `una materia nueva propone un color que nadie usa`() = runTest {
        val horario = DatosDePrueba.horarioCargado()
        val vm = FormMateriaViewModel(horario.repositorio, id = null)

        val usados = horario.materias.value.map { it.color }
        assertFalse(vm.estado.value.color in usados)
        assertNotNull(PaletaMaterias.colores.firstOrNull { it.argb == vm.estado.value.color })
    }
}
