package com.calendar.tasktodo.ui.navegacion

import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.HorarioFalso
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.ui.componentes.PaletaMaterias
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @Test
    fun `sin materias abre la bienvenida (CP-1_01)`() = runTest {
        val vm = AppViewModel(HorarioFalso().repositorio)

        assertEquals(Bienvenida, vm.destinoInicial.value)
    }

    @Test
    fun `con materias abre el dia actual (CP-1_02)`() = runTest {
        val vm = AppViewModel(DatosDePrueba.horarioCargado().repositorio)

        assertEquals(Dia(), vm.destinoInicial.value)
    }

    @Test
    fun `cuando todos los colores estan usados la paleta sigue la rueda`() {
        val todos = PaletaMaterias.colores.map { it.argb }

        assertEquals(PaletaMaterias.colores[0].argb, PaletaMaterias.siguienteLibre(todos))
        assertEquals(PaletaMaterias.colores[1].argb, PaletaMaterias.siguienteLibre(todos + todos.first()))
    }
}
