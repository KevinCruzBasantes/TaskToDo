package com.calendar.tasktodo.ui.dia

import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.Situacion
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.db.entidad.situacion
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.HOY
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

    private fun vm(fecha: LocalDate? = null) = DiaViewModel(horario.repositorio, horario.actividadRepositorio, DatosDePrueba.reloj, fecha)

    private fun DiaUiState.clases() = conHora.filterIsInstance<ElementoDia.DeClase>().map { it.clase.materiaNombre }

    private fun DiaUiState.titulosConHora() = conHora.map {
        when (it) {
            is ElementoDia.DeClase -> it.clase.materiaNombre
            is ElementoDia.DeActividad -> it.actividad.actividad.titulo
        }
    }

    @Test
    fun `abre en el dia actual (RF-18)`() = runTest {
        val estado = observar(vm().estado)

        assertEquals(HOY, estado.value.fecha)
        assertTrue(estado.value.esHoy)
    }

    @Test
    fun `clases y actividades del dia van juntas por hora (CP-1_11 y CP-2_11)`() = runTest {
        horario.clases.update {
            it + Clase(id = 20, materiaId = 3, diaSemana = DayOfWeek.FRIDAY, horaInicio = hora(11), horaFin = hora(12))
        }
        horario.actividades.update {
            it + Actividad(id = 50, titulo = "Charla de seguridad", tipo = TipoActividad.CHARLA, fecha = HOY, hora = hora(9, 30))
        }
        val estado = observar(vm().estado)

        assertEquals(listOf("Cálculo", "Charla de seguridad", "Inglés"), estado.value.titulosConHora())
        assertEquals(listOf("Leer artículo"), estado.value.sinHora.map { it.actividad.titulo })
        assertFalse(estado.value.cargando)
    }

    @Test
    fun `una tarea vencida sin check aparece en atrasadas solo hoy (CP-2_07)`() = runTest {
        val viewModel = vm()
        val estado = observar(viewModel.estado)

        assertEquals(listOf("Informe de laboratorio"), estado.value.atrasadas.map { it.actividad.titulo })

        viewModel.siguiente()
        assertTrue(estado.value.atrasadas.isEmpty())
    }

    @Test
    fun `un examen vencido no pasa a hoy y queda no marcado en su fecha (CP-2_08)`() = runTest {
        val viewModel = vm()
        val estado = observar(viewModel.estado)
        assertTrue(estado.value.atrasadas.none { it.actividad.tipo == TipoActividad.EXAMEN })

        viewModel.anterior()

        val examen = estado.value.conHora.filterIsInstance<ElementoDia.DeActividad>().single().actividad.actividad
        assertEquals("Parcial de Inglés", examen.titulo)
        assertEquals(Situacion.NO_MARCADA, examen.situacion(HOY))
    }

    @Test
    fun `reprogramar una atrasada a ayer no se permite y a hoy o despues si (CP-2_09)`() = runTest {
        val viewModel = vm()
        val estado = observar(viewModel.estado)
        val id = DatosDePrueba.tareaAtrasada.id

        viewModel.reprogramar(id, HOY.minusDays(1))
        assertEquals(HOY.minusDays(3), horario.actividades.value.first { it.id == id }.fecha)

        viewModel.reprogramar(id, LocalDate.of(2026, 11, 3))
        assertEquals(LocalDate.of(2026, 11, 3), horario.actividades.value.first { it.id == id }.fecha)
        assertTrue(estado.value.atrasadas.isEmpty())
    }

    @Test
    fun `marcar con un toque y desmarcar avisa a los avisos (CP-2_06 y CP-2_25)`() = runTest {
        val viewModel = vm()
        val id = DatosDePrueba.tareaHoySinHora.id
        val antes = horario.cambiosAvisados

        viewModel.cambiarCumplida(id, true)
        assertEquals(EstadoActividad.CUMPLIDA, horario.actividades.value.first { it.id == id }.estado)

        viewModel.cambiarCumplida(id, false)
        assertEquals(EstadoActividad.PENDIENTE, horario.actividades.value.first { it.id == id }.estado)
        assertEquals(antes + 2, horario.cambiosAvisados)
    }

    @Test
    fun `marcar una atrasada la saca de atrasadas`() = runTest {
        val viewModel = vm()
        val estado = observar(viewModel.estado)

        viewModel.cambiarCumplida(DatosDePrueba.tareaAtrasada.id, true)

        assertTrue(estado.value.atrasadas.isEmpty())
    }

    @Test
    fun `borrar una atrasada la elimina`() = runTest {
        val viewModel = vm()
        val estado = observar(viewModel.estado)

        viewModel.eliminar(DatosDePrueba.tareaAtrasada)

        assertTrue(estado.value.atrasadas.isEmpty())
        assertTrue(horario.actividades.value.none { it.id == DatosDePrueba.tareaAtrasada.id })
    }

    @Test
    fun `las clases se repiten cada semana sin guardarse por fecha (CP-1_08)`() = runTest {
        val viewModel = vm()
        val estado = observar(viewModel.estado)

        listOf(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 6)).forEach { fecha ->
            viewModel.irA(fecha)
            assertEquals("el $fecha", listOf("Cálculo"), estado.value.clases())
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

        viewModel.siguiente()
        viewModel.siguiente()
        assertEquals(LocalDate.of(2026, 10, 31), estado.value.fecha)
        assertTrue(estado.value.vacio)

        viewModel.irAHoy()
        assertEquals(HOY, estado.value.fecha)
        assertTrue(estado.value.esHoy)
    }

    @Test
    fun `abre en la fecha pedida`() = runTest {
        val estado = observar(vm(LocalDate.of(2026, 11, 3)).estado)

        assertEquals(listOf("Física"), estado.value.clases())
        assertFalse(estado.value.esHoy)
    }
}
