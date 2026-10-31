package com.calendar.tasktodo.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.GymRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.data.repositorio.SesionRepository
import com.calendar.tasktodo.prueba.DataStoreEnMemoria
import com.calendar.tasktodo.sistema.sesion.ControlSesion
import com.calendar.tasktodo.ui.dia.DiaScreen
import com.calendar.tasktodo.ui.dia.DiaViewModel
import com.calendar.tasktodo.ui.gym.SesionScreen
import com.calendar.tasktodo.ui.gym.SesionViewModel
import com.calendar.tasktodo.ui.theme.TaskToDoTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class GymUiTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: AgendaDatabase

    /** Viernes 30 de octubre de 2026, 10:00. */
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 10, 30, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private val sinServicio = object : ControlSesion {
        override fun iniciar() = Unit
        override suspend fun asegurar() = Unit
    }

    private var actividadId = 0L

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AgendaDatabase::class.java).build()
        runBlocking {
            val pierna = db.gymDao().insertarRutina(Rutina(nombre = "Pierna"))
            db.gymDao().insertarEjercicio(Ejercicio(rutinaId = pierna, orden = 0, nombre = "Sentadilla", medida = Medida.REPETICIONES, series = 4, repeticiones = 10))
            actividadId = db.actividadDao().insertar(
                Actividad(titulo = "Pierna", tipo = TipoActividad.GYM, fecha = LocalDate.of(2026, 10, 30), hora = LocalTime.of(18, 0), rutinaId = pierna),
            )
        }
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun tocarElGymEnElDiaAbreSuRutinaParaIniciar_CP_5_37() {
        var sesionPedida by mutableStateOf<Long?>(null)
        var detallePedido: Long? = null
        val dia = DiaViewModel(HorarioRepository(db.materiaDao(), db.claseDao()), ActividadRepository(db.actividadDao()), reloj)
        val gym = GymRepository(db.gymDao(), AjustesRepository(DataStoreEnMemoria()), reloj)
        val sesion = SesionViewModel(SesionRepository(db.gymDao(), db.actividadDao(), reloj), gym, ActividadRepository(db.actividadDao()), sinServicio, actividadId)
        compose.setContent {
            TaskToDoTheme {
                // Como la navegación: tocar el gym lleva a la sesión de esa actividad
                val pedida = sesionPedida
                if (pedida == null) {
                    DiaScreen(null, {}, onAbrirActividad = { detallePedido = it }, onNuevaActividad = {}, onAbrirSesion = { sesionPedida = it }, viewModel = dia)
                } else {
                    SesionScreen(pedida, onAtras = {}, viewModel = sesion)
                }
            }
        }
        esperarTexto("Pierna", sustring = true)
        compose.onAllNodesWithText("Pierna", substring = true)[0].performClick()

        compose.waitForIdle()
        assertEquals(actividadId, sesionPedida)
        assertNull(detallePedido)
        // La pantalla a la que lleva muestra la rutina con "Iniciar sesión"
        esperarTexto("1. Sentadilla", sustring = true)
        compose.onNodeWithText("Iniciar sesión").assertExists()
    }

    private fun esperarTexto(texto: String, sustring: Boolean = false) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodesWithText(texto, substring = sustring).fetchSemanticsNodes().isNotEmpty() }
}
