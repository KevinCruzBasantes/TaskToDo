package com.calendar.tasktodo.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.dia.DiaScreen
import com.calendar.tasktodo.ui.dia.DiaViewModel
import com.calendar.tasktodo.ui.inicio.BienvenidaScreen
import com.calendar.tasktodo.ui.inicio.BienvenidaViewModel
import com.calendar.tasktodo.ui.theme.TaskToDoTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class DiaYBienvenidaUiTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: AgendaDatabase
    private lateinit var repositorio: HorarioRepository

    /** Viernes 30 de octubre de 2026, 10:00. */
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 10, 30, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AgendaDatabase::class.java).build()
        repositorio = HorarioRepository(db.materiaDao(), db.claseDao())
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun laBienvenidaNoDejaTerminarSinMaterias_CP_1_01() {
        val viewModel = BienvenidaViewModel(repositorio)
        compose.setContent {
            TaskToDoTheme {
                BienvenidaScreen({}, {}, {}, {}, viewModel = viewModel)
            }
        }
        compose.onNodeWithText("Listo").assertIsNotEnabled()

        runBlocking { db.materiaDao().insertar(Materia(nombre = "Cálculo", color = 1)) }

        esperarTexto("Cálculo")
        compose.onNodeWithText("Listo").assertIsEnabled()
    }

    @Test
    fun elModuloDelDiaCambiaDeDiaYVuelveAHoy_CP_1_10() {
        runBlocking {
            val calculo = db.materiaDao().insertar(Materia(nombre = "Cálculo", color = 1))
            db.claseDao().insertar(
                Clase(materiaId = calculo, diaSemana = DayOfWeek.FRIDAY, horaInicio = LocalTime.of(7, 0), horaFin = LocalTime.of(9, 0)),
            )
        }
        val viewModel = DiaViewModel(repositorio, reloj)
        compose.setContent {
            TaskToDoTheme {
                DiaScreen(null, {}, {}, viewModel = viewModel)
            }
        }
        compose.onNodeWithText("Viernes 30 de octubre").assertExists()
        esperarTexto("Cálculo")

        compose.onNodeWithContentDescription("Día anterior").performClick()
        compose.onNodeWithText("Jueves 29 de octubre").assertExists()
        esperarTexto("No hay clases este día")

        compose.onNodeWithText("Hoy").performClick()
        compose.onNodeWithText("Viernes 30 de octubre").assertExists()

        compose.onNodeWithContentDescription("Día siguiente").performClick()
        compose.onNodeWithText("Sábado 31 de octubre").assertExists()
    }

    /** Room emite en su propio hilo: se espera a que el dato llegue a la pantalla. */
    private fun esperarTexto(texto: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
}
