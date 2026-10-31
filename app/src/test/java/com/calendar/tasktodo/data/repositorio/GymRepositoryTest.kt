package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.HOY
import com.calendar.tasktodo.prueba.DatosDePrueba.hora
import com.calendar.tasktodo.prueba.ReglaAjustes
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.ui.gym.FormEjercicioViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** Rutinas, bloques, programación y generación (CU-12 a CU-15). */
class GymRepositoryTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @get:Rule
    val ajustes = ReglaAjustes()

    private val horario = DatosDePrueba.horarioCargado().apply { actividades.value = emptyList() }
    private val dao = horario.gymDao
    private val gym by lazy { GymRepository(dao, ajustes.repositorio, DatosDePrueba.reloj) }
    private val actividades = horario.actividadRepositorio

    private suspend fun rutinaConEjercicios(nombre: String, cuantos: Int): Long {
        val id = (gym.guardarRutina(Rutina(nombre = nombre)) as ResultadoRutina.Guardada).id
        repeat(cuantos) { gym.guardarEjercicio(Ejercicio(rutinaId = id, orden = 0, nombre = "E${it + 1}", medida = Medida.REPETICIONES, series = 3, repeticiones = 10)) }
        return id
    }

    private fun nombres(rutinaId: Long) = dao.ejercicios.value.filter { it.rutinaId == rutinaId }.sortedBy { it.orden }.map { it.nombre }

    private fun gymGeneradas() = horario.actividades.value.filter { it.tipo == TipoActividad.GYM }

    @Test
    fun `una rutina con nombre repetido no se guarda (CP-5_01)`() = runTest {
        gym.guardarRutina(Rutina(nombre = "Pierna"))

        assertEquals(ResultadoRutina.NombreRepetido, gym.guardarRutina(Rutina(nombre = "PIERNA")))
        assertEquals(1, dao.rutinas.value.size)
    }

    @Test
    fun `un ejercicio sin lo que pide su medida no se guarda (CP-5_02)`() = runTest {
        val id = rutinaConEjercicios("Pierna", 0)
        val vm = FormEjercicioViewModel(gym, id, null)

        vm.cambiar { it.copy(nombre = "Plancha", medida = Medida.TIEMPO, duracionSeg = 0) }
        vm.guardar()
        assertEquals("La duración debe ser más de 0", vm.estado.value.error)

        vm.cambiar { it.copy(nombre = "", duracionSeg = 45) }
        vm.guardar()
        assertEquals("El nombre es obligatorio", vm.estado.value.error)
        assertTrue(dao.ejercicios.value.isEmpty())
    }

    @Test
    fun `guardar y agregar otro deja el formulario vacio y los agrega en orden (CP-5_03)`() = runTest {
        val id = rutinaConEjercicios("Pierna", 0)
        val vm = FormEjercicioViewModel(gym, id, null)

        listOf("Sentadilla", "Prensa", "Zancadas").forEach { nombre ->
            vm.cambiar { it.copy(nombre = nombre, peso = "60,5") }
            vm.guardar(yOtro = true)
            assertEquals("", vm.estado.value.nombre)
        }

        assertEquals(listOf("Sentadilla", "Prensa", "Zancadas"), nombres(id))
        assertEquals(60.5, dao.ejercicios.value.first().pesoKg!!, 0.0)
        assertEquals(3, vm.estado.value.guardados)
    }

    @Test
    fun `duplicar deja la copia justo debajo y corre los demas (CP-5_04)`() = runTest {
        val id = rutinaConEjercicios("Pierna", 4)

        gym.duplicarEjercicio(dao.ejercicios.value.first { it.nombre == "E2" })

        assertEquals(listOf("E1", "E2", "E2", "E3", "E4"), nombres(id))
    }

    @Test
    fun `un bloque se mueve a cualquier posicion (CP-5_05 y CP-5_06)`() = runTest {
        val id = rutinaConEjercicios("Pierna", 4)

        gym.moverEjercicio(id, 0, 2)
        assertEquals(listOf("E2", "E3", "E1", "E4"), nombres(id))

        gym.moverEjercicio(id, 3, 0)
        assertEquals(listOf("E4", "E2", "E3", "E1"), nombres(id))

        gym.moverEjercicio(id, 1, 9) // fuera de rango: no cambia nada
        assertEquals(listOf("E4", "E2", "E3", "E1"), nombres(id))
    }

    @Test
    fun `eliminar un ejercicio renumera los demas (CP-5_07)`() = runTest {
        val id = rutinaConEjercicios("Pierna", 3)

        gym.eliminarEjercicio(dao.ejercicios.value.first { it.nombre == "E1" })

        assertEquals(listOf(0, 1), dao.ejercicios.value.sortedBy { it.orden }.map { it.orden })
        assertEquals(listOf("E2", "E3"), nombres(id))
    }

    @Test
    fun `una rutina asignada a un dia no se puede eliminar (CP-5_08)`() = runTest {
        val id = rutinaConEjercicios("Pierna", 2)
        gym.guardarProgramacion(mapOf(DayOfWeek.MONDAY to EleccionDia(hora(18), id)), null)

        assertEquals(ResultadoRutina.EnUso(listOf(DayOfWeek.MONDAY)), gym.eliminarRutina(id))
        assertEquals(1, dao.rutinas.value.size)
    }

    @Test
    fun `al eliminar una rutina se borran sus actividades futuras, tambien las movidas (CP-5_09)`() = runTest {
        val id = rutinaConEjercicios("Pierna", 2)
        horario.actividades.value = listOf(
            Actividad(id = 1, titulo = "Pierna", tipo = TipoActividad.GYM, fecha = HOY.plusDays(3), rutinaId = id, fechaProgramada = HOY.plusDays(2)),
            Actividad(id = 2, titulo = "Pierna", tipo = TipoActividad.GYM, fecha = HOY.minusDays(1), rutinaId = id, estado = EstadoActividad.CUMPLIDA),
        )

        assertEquals(ResultadoRutina.Eliminada, gym.eliminarRutina(id))

        assertEquals(listOf(2L), horario.actividades.value.map { it.id })
        assertTrue(dao.rutinas.value.isEmpty())
    }

    @Test
    fun `programar lunes, miercoles y viernes genera 4 semanas con su hora y rutina (CP-5_10)`() = runTest {
        val pierna = rutinaConEjercicios("Pierna", 2)
        val torso = rutinaConEjercicios("Torso", 2)

        gym.guardarProgramacion(
            mapOf(
                DayOfWeek.MONDAY to EleccionDia(hora(18), pierna),
                DayOfWeek.WEDNESDAY to EleccionDia(hora(18), torso),
                DayOfWeek.FRIDAY to EleccionDia(hora(17), pierna),
            ),
            null,
        )

        val generadas = gymGeneradas()
        // Del viernes 30 de octubre al viernes 27 de noviembre: 5 viernes, 4 lunes y 4 miércoles
        assertEquals(13, generadas.size)
        assertTrue(generadas.all { it.fecha.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY) })
        assertEquals(hora(17), generadas.first { it.fecha == HOY }.hora)
        assertEquals("Torso", generadas.first { it.fecha.dayOfWeek == DayOfWeek.WEDNESDAY }.titulo)
        assertTrue(generadas.all { it.fecha == it.fechaProgramada && it.diaGymId != null })
    }

    @Test
    fun `generar dos veces no duplica (CP-5_11)`() = runTest {
        val pierna = rutinaConEjercicios("Pierna", 2)
        gym.guardarProgramacion(mapOf(DayOfWeek.MONDAY to EleccionDia(hora(18), pierna)), null)
        val antes = gymGeneradas().size

        gym.generar()
        gym.generar()

        assertEquals(antes, gymGeneradas().size)
    }

    @Test
    fun `con fecha de fin no hay actividades despues (CP-5_12)`() = runTest {
        val pierna = rutinaConEjercicios("Pierna", 2)

        gym.guardarProgramacion(mapOf(DayOfWeek.MONDAY to EleccionDia(hora(18), pierna)), LocalDate.of(2026, 11, 15))

        assertEquals(listOf(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9)), gymGeneradas().map { it.fecha }.sorted())
    }

    @Test
    fun `mover una sesion la conserva al cambiar la programacion (CP-5_13)`() = runTest {
        val pierna = rutinaConEjercicios("Pierna", 2)
        gym.guardarProgramacion(mapOf(DayOfWeek.MONDAY to EleccionDia(hora(18), pierna)), null)
        val lunes2 = gymGeneradas().first { it.fecha == LocalDate.of(2026, 11, 2) }

        gym.moverSesion(lunes2.id, LocalDate.of(2026, 11, 3), hora(19))
        gym.guardarProgramacion(mapOf(DayOfWeek.MONDAY to EleccionDia(hora(7), pierna)), null)

        val generadas = gymGeneradas()
        val movida = generadas.single { it.id == lunes2.id }
        assertEquals(LocalDate.of(2026, 11, 3), movida.fecha)
        assertEquals(hora(19), movida.hora)
        // No se generó otra para el lunes 2: la movida la representa
        assertFalse(generadas.any { it.fecha == LocalDate.of(2026, 11, 2) })
        assertTrue(generadas.filter { it.fecha.dayOfWeek == DayOfWeek.MONDAY }.all { it.hora == hora(7) })
    }

    @Test
    fun `saltar una sesion la deja saltada y no la vuelve a generar (CP-5_14)`() = runTest {
        val pierna = rutinaConEjercicios("Pierna", 2)
        gym.guardarProgramacion(mapOf(DayOfWeek.WEDNESDAY to EleccionDia(hora(18), pierna)), null)
        val miercoles4 = gymGeneradas().first { it.fecha == LocalDate.of(2026, 11, 4) }

        actividades.saltar(miercoles4.id)
        gym.guardarProgramacion(mapOf(DayOfWeek.WEDNESDAY to EleccionDia(hora(20), pierna)), null)

        val delDia4 = gymGeneradas().filter { it.fechaProgramada == LocalDate.of(2026, 11, 4) }
        assertEquals(listOf(EstadoActividad.SALTADA), delDia4.map { it.estado })
    }

    @Test
    fun `quitar un dia de la programacion borra sus futuras`() = runTest {
        val pierna = rutinaConEjercicios("Pierna", 2)
        gym.guardarProgramacion(mapOf(DayOfWeek.MONDAY to EleccionDia(hora(18), pierna)), null)

        gym.guardarProgramacion(emptyMap(), null)

        assertTrue(gymGeneradas().isEmpty())
        assertTrue(dao.dias.value.isEmpty())
    }

    @Test
    fun `renombrar una rutina actualiza sus sesiones pendientes`() = runTest {
        val pierna = rutinaConEjercicios("Pierna", 2)
        gym.guardarProgramacion(mapOf(DayOfWeek.MONDAY to EleccionDia(hora(18), pierna)), null)

        gym.guardarRutina(Rutina(id = pierna, nombre = "Pierna y glúteo"))

        assertTrue(gymGeneradas().all { it.titulo == "Pierna y glúteo" })
    }
}
