package com.calendar.tasktodo.ui.actividades

import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.HOY
import com.calendar.tasktodo.prueba.DatosDePrueba.hora
import com.calendar.tasktodo.prueba.ReglaAjustes
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.prueba.observar
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ActividadesViewModelsTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @get:Rule
    val ajustes = ReglaAjustes()

    private val horario = DatosDePrueba.horarioCargado()

    private fun formulario(id: Long? = null, fecha: LocalDate? = null) = FormActividadViewModel(
        horario.repositorio, horario.actividadRepositorio, ajustes.repositorio, DatosDePrueba.reloj, id, fecha,
    )

    private fun lista() = ActividadesViewModel(horario.repositorio, horario.actividadRepositorio, DatosDePrueba.reloj)

    @Test
    fun `una actividad sin titulo no se guarda (CP-2_01)`() = runTest {
        val vm = formulario()
        vm.cambiarTitulo("  ")

        vm.guardar()

        assertEquals("El título es obligatorio", vm.estado.value.errorTitulo)
        assertFalse(vm.estado.value.terminado)
        assertEquals(4, horario.actividades.value.size)
    }

    @Test
    fun `desde el modulo del dia la fecha viene puesta (CP-2_02)`() = runTest {
        val vm = formulario(fecha = LocalDate.of(2026, 11, 6))

        assertEquals(LocalDate.of(2026, 11, 6), vm.estado.value.fecha)
        assertEquals(HOY, formulario().estado.value.fecha)
    }

    @Test
    fun `una actividad nueva se guarda con todos sus datos y avisa a los avisos`() = runTest {
        val vm = formulario()
        val antes = horario.cambiosAvisados
        vm.cambiarTitulo(" Exposición ")
        vm.cambiarTipo(TipoActividad.CHARLA)
        vm.cambiarFecha(LocalDate.of(2026, 11, 4))
        vm.cambiarHora(hora(15))
        vm.cambiarMateria(DatosDePrueba.calculo.id)
        vm.cambiarAviso(30)
        vm.cambiarNotas("Llevar diapositivas")

        vm.guardar()

        assertTrue(vm.estado.value.terminado)
        val nueva = horario.actividades.value.last()
        assertEquals("Exposición", nueva.titulo)
        assertEquals(TipoActividad.CHARLA, nueva.tipo)
        assertEquals(hora(15), nueva.hora)
        assertEquals(30, nueva.avisoMinutosAntes)
        assertEquals(EstadoActividad.PENDIENTE, nueva.estado)
        assertEquals(antes + 1, horario.cambiosAvisados)
    }

    @Test
    fun `editar cambia fecha y hora y conserva el check (CP-2_03)`() = runTest {
        horario.actividadDao.cambiarEstado(DatosDePrueba.examenProximo.id, EstadoActividad.CUMPLIDA)
        val vm = formulario(id = DatosDePrueba.examenProximo.id)
        assertEquals("Examen parcial", vm.estado.value.titulo)

        vm.cambiarFecha(LocalDate.of(2026, 11, 9))
        vm.cambiarHora(null)
        vm.guardar()

        val editada = horario.actividades.value.first { it.id == DatosDePrueba.examenProximo.id }
        assertEquals(LocalDate.of(2026, 11, 9), editada.fecha)
        assertNull(editada.hora)
        assertEquals(EstadoActividad.CUMPLIDA, editada.estado)
    }

    @Test
    fun `eliminar una actividad la borra (CP-2_04)`() = runTest {
        val vm = formulario(id = DatosDePrueba.examenProximo.id)

        vm.eliminar()

        assertTrue(vm.estado.value.terminado)
        assertTrue(horario.actividades.value.none { it.id == DatosDePrueba.examenProximo.id })
    }

    @Test
    fun `los filtros por tipo y materia se combinan (CP-2_05)`() = runTest {
        val vm = lista()
        val estado = observar(vm.estado)
        fun titulos() = (estado.value.proximas + estado.value.pasadas).map { it.actividad.titulo }.toSet()

        vm.filtrarTipo(TipoActividad.EXAMEN)
        assertEquals(setOf("Examen parcial", "Parcial de Inglés"), titulos())

        vm.filtrarMateria(DatosDePrueba.fisica.id)
        assertEquals(setOf("Examen parcial"), titulos())

        vm.filtrarTipo(null)
        assertEquals(setOf("Examen parcial", "Informe de laboratorio"), titulos())

        vm.filtrarMateria(DatosDePrueba.calculo.id)
        assertTrue(titulos().isEmpty())

        vm.quitarFiltros()
        assertEquals(4, titulos().size)
    }

    @Test
    fun `las cumplidas se ocultan salvo que se pidan y las pasadas van de la mas reciente`() = runTest {
        val vm = lista()
        val estado = observar(vm.estado)
        vm.cambiarCumplida(DatosDePrueba.tareaHoySinHora.id, true)

        assertTrue(estado.value.proximas.none { it.actividad.id == DatosDePrueba.tareaHoySinHora.id })
        assertEquals(listOf("Parcial de Inglés", "Informe de laboratorio"), estado.value.pasadas.map { it.actividad.titulo })

        vm.cambiarVerCumplidas(true)
        assertTrue(estado.value.proximas.any { it.actividad.id == DatosDePrueba.tareaHoySinHora.id })
    }
}
