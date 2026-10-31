package com.calendar.tasktodo.ui.inicio

import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.HorarioFalso
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.prueba.observar
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BienvenidaViewModelTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @Test
    fun `muestra las materias que se van registrando con sus clases (CP-1_01)`() = runTest {
        val horario = HorarioFalso()
        val materias = observar(BienvenidaViewModel(horario.repositorio).materias)
        assertTrue(materias.value.isEmpty())

        horario.materias.update { it + DatosDePrueba.calculo }
        horario.clases.update { DatosDePrueba.clases.filter { it.materiaId == DatosDePrueba.calculo.id } }

        assertEquals(listOf("Cálculo" to 3), materias.value.map { it.materia.nombre to it.clases })
    }
}
