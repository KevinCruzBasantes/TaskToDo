package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.EstadoSesion
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.SerieRealizada
import com.calendar.tasktodo.data.db.entidad.SesionGym
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.db.entidad.situacion
import com.calendar.tasktodo.data.db.entidad.Situacion
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.HOY
import com.calendar.tasktodo.prueba.DatosDePrueba.hora
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** La sesión de gym (CU-16): motor, descansos, orden y guardado. */
class SesionRepositoryTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    private val horario = DatosDePrueba.horarioCargado()
    private val dao = horario.gymDao
    private var reloj: Clock = DatosDePrueba.reloj
    private var cambios = 0

    private fun repo() = SesionRepository(dao, horario.actividadDao, reloj, observador = { cambios++ })

    private val sentadilla = Ejercicio(id = 1, rutinaId = 500, orden = 0, nombre = "Sentadilla", medida = Medida.REPETICIONES, series = 2,
        repeticiones = 10, pesoKg = 60.0, descansoSeriesSeg = 90, descansoTrasEjercicioSeg = 60)
    private val prensa = Ejercicio(id = 2, rutinaId = 500, orden = 1, nombre = "Prensa", medida = Medida.REPETICIONES, series = 2,
        repeticiones = 12, pesoKg = 120.0)
    private val plancha = Ejercicio(id = 3, rutinaId = 500, orden = 2, nombre = "Plancha", medida = Medida.TIEMPO, series = 1, duracionSeg = 45)
    private val pierna = Rutina(id = 500, nombre = "Pierna", descansoEntreEjerciciosSeg = 120)
    private val actividad = Actividad(id = 900, titulo = "Pierna", tipo = TipoActividad.GYM, fecha = HOY, hora = hora(18), rutinaId = 500)

    init {
        dao.rutinas.value = listOf(pierna)
        dao.ejercicios.value = listOf(sentadilla, prensa, plancha)
        horario.actividades.value = listOf(actividad)
    }

    private suspend fun iniciar(): SesionGym {
        val inicio = repo().iniciar(actividad.id) as Inicio.Sesion
        return dao.obtenerSesion(inicio.id)!!
    }

    private suspend fun avance(sesion: SesionGym? = null): Avance {
        val s = sesion ?: dao.enCurso()!!
        return MotorSesion.avance(s.ordenEjercicios, dao.ejercicios(500), dao.series(s.id))
    }

    private suspend fun confirmar(reps: Int? = 10, peso: Double? = 60.0) {
        val sesion = dao.enCurso()!!
        repo().confirmarSerie(sesion, avance(sesion), reps, null, peso)
    }

    @Test
    fun `iniciar guarda el inicio, el orden de la rutina y queda en curso (CP-5_20)`() = runTest {
        val sesion = iniciar()

        assertEquals(Instant.parse("2026-10-30T10:00:00Z"), sesion.inicio)
        assertEquals(listOf(1L, 2L, 3L), sesion.ordenEjercicios)
        assertEquals(EstadoSesion.EN_CURSO, sesion.estado)
        assertEquals("Pierna", sesion.nombreRutina)
    }

    @Test
    fun `solo puede haber una sesion en curso`() = runTest {
        iniciar()
        horario.actividades.value = horario.actividades.value + actividad.copy(id = 901)

        assertTrue(repo().iniciar(901) is Inicio.OtraEnCurso)
        assertTrue(repo().iniciar(actividad.id) is Inicio.Sesion)
    }

    @Test
    fun `el prellenado usa la ultima vez o, si no hay, la rutina (CP-5_21)`() = runTest {
        assertEquals(Prellenado(10, null, 60.0, deLaUltimaVez = false), repo().prellenado(sentadilla, 2))

        dao.sesiones.value = listOf(
            SesionGym(id = 70, actividadId = null, rutinaId = 500, nombreRutina = "Pierna", fecha = HOY.minusDays(7),
                inicio = Instant.parse("2026-10-23T18:00:00Z"), estado = EstadoSesion.TERMINADA, ordenEjercicios = listOf(1, 2, 3)),
        )
        dao.series.value = listOf(
            SerieRealizada(sesionId = 70, ejercicioId = 1, nombreEjercicio = "Sentadilla", ordenEjercicio = 1, numeroSerie = 2,
                medida = Medida.REPETICIONES, repeticiones = 8, pesoKg = 65.0, completadaEn = Instant.parse("2026-10-23T18:10:00Z")),
        )

        assertEquals(Prellenado(8, null, 65.0, deLaUltimaVez = true), repo().prellenado(sentadilla, 2))
    }

    @Test
    fun `confirmar una serie avanza el contador e inicia el descanso entre series (CP-5_22)`() = runTest {
        iniciar()
        assertEquals(1, avance().serieActual)

        confirmar()

        val sesion = dao.enCurso()!!
        assertEquals(2, avance(sesion).serieActual)
        assertEquals(Instant.parse("2026-10-30T10:01:30Z"), sesion.descansoTerminaEn)
        assertEquals(1, dao.series.value.size)
    }

    @Test
    fun `el descanso se alarga, se acorta o se salta (CP-5_23)`() = runTest {
        iniciar()
        confirmar()

        repo().ajustarDescanso(dao.enCurso()!!, 15)
        assertEquals(Instant.parse("2026-10-30T10:01:45Z"), dao.enCurso()!!.descansoTerminaEn)
        repo().ajustarDescanso(dao.enCurso()!!, -15)
        assertEquals(Instant.parse("2026-10-30T10:01:30Z"), dao.enCurso()!!.descansoTerminaEn)
        repo().saltarDescanso(dao.enCurso()!!)
        assertNull(dao.enCurso()!!.descansoTerminaEn)
    }

    @Test
    fun `tras la ultima serie va el descanso del ejercicio o el de la rutina (CP-5_24)`() = runTest {
        assertEquals(90, MotorSesion.descansoTras(sentadilla, 1, pierna, quedanEjercicios = true))
        assertEquals(60, MotorSesion.descansoTras(sentadilla, 2, pierna, quedanEjercicios = true))
        assertEquals(120, MotorSesion.descansoTras(prensa, 2, pierna, quedanEjercicios = true))
        assertNull(MotorSesion.descansoTras(prensa, 2, pierna.copy(descansoEntreEjerciciosSeg = null), quedanEjercicios = true))
        assertNull(MotorSesion.descansoTras(plancha, 1, pierna, quedanEjercicios = false))
        assertNull(MotorSesion.descansoTras(prensa, 1, pierna, quedanEjercicios = true))
    }

    @Test
    fun `un ejercicio por tiempo guarda su duracion (CP-5_25)`() = runTest {
        val sesion = iniciar()
        repo().cambiarOrden(sesion, listOf(3L, 1L, 2L))
        val conPlancha = dao.enCurso()!!
        assertEquals("Plancha", avance(conPlancha).actual?.nombre)

        repo().confirmarSerie(conPlancha, avance(conPlancha), null, 45, null)

        assertEquals(45, dao.series.value.single().duracionSeg)
        assertEquals(Medida.TIEMPO, dao.series.value.single().medida)
    }

    @Test
    fun `hacer ahora cambia el actual y el que estaba a medias vuelve a pendientes (CP-5_26)`() = runTest {
        iniciar()
        confirmar() // Sentadilla 1 de 2

        val nuevo = MotorSesion.hacerAhora(avance(), plancha.id)
        repo().cambiarOrden(dao.enCurso()!!, nuevo)

        val a = avance()
        assertEquals("Plancha", a.actual?.nombre)
        assertEquals(listOf("Sentadilla", "Prensa"), a.pendientes.map { it.nombre })
        assertEquals(1, a.hechasPorEjercicio[sentadilla.id])
    }

    @Test
    fun `un pendiente se mueve a cualquier posicion entre los pendientes (CP-5_27)`() = runTest {
        iniciar()

        repo().cambiarOrden(dao.enCurso()!!, MotorSesion.moverPendiente(avance(), 1, 0))

        val a = avance()
        assertEquals("Sentadilla", a.actual?.nombre)
        assertEquals(listOf("Plancha", "Prensa"), a.pendientes.map { it.nombre })
    }

    @Test
    fun `al terminar avisa si el orden cambio respecto de la rutina (CP-5_28)`() = runTest {
        iniciar()
        assertFalse(repo().terminar(dao.sesiones.value.single()))

        dao.sesiones.value = emptyList()
        horario.actividades.value = listOf(actividad)
        iniciar()
        repo().cambiarOrden(dao.enCurso()!!, listOf(2L, 1L, 3L))
        assertTrue(repo().terminar(dao.enCurso()!!))
    }

    @Test
    fun `guardar la sesion la cierra con su duracion y deja la actividad realizada (CP-5_29)`() = runTest {
        iniciar()
        confirmar()
        reloj = Clock.offset(DatosDePrueba.reloj, Duration.ofMinutes(52))

        repo().terminar(dao.enCurso()!!)

        val sesion = dao.sesiones.value.single()
        assertEquals(EstadoSesion.TERMINADA, sesion.estado)
        assertEquals(52 * 60, sesion.duracionSeg)
        assertNull(sesion.descansoTerminaEn)
        assertEquals(EstadoActividad.CUMPLIDA, horario.actividades.value.single().estado)
        assertNull(dao.enCurso())
    }

    @Test
    fun `descartar borra la sesion y sus series y la actividad sigue pendiente (CP-5_30)`() = runTest {
        iniciar()
        confirmar()

        repo().descartar(dao.enCurso()!!)

        assertTrue(dao.sesiones.value.isEmpty())
        assertTrue(dao.series.value.isEmpty())
        assertEquals(EstadoActividad.PENDIENTE, horario.actividades.value.single().estado)
    }

    @Test
    fun `una sesion de ayer sin hacer queda no realizada (CP-5_31)`() = runTest {
        assertEquals(Situacion.NO_REALIZADA, actividad.copy(fecha = HOY.minusDays(1)).situacion(HOY))
    }

    @Test
    fun `anterior quita la ultima serie para volver a hacerla`() = runTest {
        iniciar()
        confirmar()
        assertEquals(2, avance().serieActual)

        repo().deshacerUltimaSerie(dao.enCurso()!!)

        assertEquals(1, avance().serieActual)
        assertNull(dao.enCurso()!!.descansoTerminaEn)
    }

    @Test
    fun `cuando se hacen todas las series la rutina esta completa`() = runTest {
        iniciar()
        repeat(4) { confirmar() }
        repo().confirmarSerie(dao.enCurso()!!, avance(), null, 45, null)

        assertTrue(avance().terminada)
        assertNull(dao.enCurso()!!.descansoTerminaEn)
        assertTrue(cambios > 0)
    }
}
