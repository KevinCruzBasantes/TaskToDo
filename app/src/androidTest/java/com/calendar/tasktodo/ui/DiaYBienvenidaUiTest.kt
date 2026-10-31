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
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.repositorio.ActividadRepository
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class DiaYBienvenidaUiTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: AgendaDatabase
    private lateinit var repositorio: HorarioRepository
    private lateinit var actividades: ActividadRepository

    /** Viernes 30 de octubre de 2026, 10:00. */
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 10, 30, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AgendaDatabase::class.java).build()
        repositorio = HorarioRepository(db.materiaDao(), db.claseDao())
        actividades = ActividadRepository(db.actividadDao())
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun laBienvenidaNoDejaTerminarSinMaterias_CP_1_01() {
        val viewModel = BienvenidaViewModel(repositorio)
        compose.setContent {
            TaskToDoTheme {
                BienvenidaScreen({}, {}, {}, {}, onRestaurar = {}, viewModel = viewModel)
            }
        }
        compose.onNodeWithText("Empezar desde cero").performClick()
        compose.onNodeWithText("Listo").assertIsNotEnabled()

        runBlocking { db.materiaDao().insertar(Materia(nombre = "Cálculo", color = 1)) }

        esperarTexto("Cálculo")
        compose.onNodeWithText("Listo").assertIsEnabled()
    }

    @Test
    fun laBienvenidaPreguntaPrimeroSiEmpezarDeCeroORestaurar_CU_01() {
        var restaurar = 0
        val viewModel = BienvenidaViewModel(repositorio)
        compose.setContent {
            TaskToDoTheme {
                BienvenidaScreen({}, {}, {}, {}, onRestaurar = { restaurar++ }, viewModel = viewModel)
            }
        }
        compose.onNodeWithText("¿Es la primera vez que usas la app?").assertExists()
        compose.onNodeWithText("Listo").assertDoesNotExist()

        compose.onNodeWithText("Restaurar mis datos").performClick()
        compose.waitUntil(5_000) { restaurar == 1 }
    }

    @Test
    fun elModuloDelDiaCambiaDeDiaYVuelveAHoy_CP_1_10() {
        runBlocking {
            val calculo = db.materiaDao().insertar(Materia(nombre = "Cálculo", color = 1))
            db.claseDao().insertar(
                Clase(materiaId = calculo, diaSemana = DayOfWeek.FRIDAY, horaInicio = LocalTime.of(7, 0), horaFin = LocalTime.of(9, 0)),
            )
        }
        val viewModel = DiaViewModel(repositorio, actividades, reloj)
        compose.setContent {
            TaskToDoTheme {
                DiaScreen(null, {}, {}, {}, viewModel = viewModel)
            }
        }
        compose.onNodeWithText("Viernes 30 de octubre").assertExists()
        esperarTexto("Cálculo")

        compose.onNodeWithContentDescription("Día anterior").performClick()
        compose.onNodeWithText("Jueves 29 de octubre").assertExists()
        esperarTexto("No hay nada para este día")

        compose.onNodeWithText("Hoy").performClick()
        compose.onNodeWithText("Viernes 30 de octubre").assertExists()

        compose.onNodeWithContentDescription("Día siguiente").performClick()
        compose.onNodeWithText("Sábado 31 de octubre").assertExists()
    }

    @Test
    fun elCheckSeMarcaYDesmarcaConUnToqueYTocarAbreElDetalle_CP_2_06_CP_2_13() {
        val id = runBlocking {
            db.actividadDao().insertar(Actividad(titulo = "Leer artículo", tipo = TipoActividad.TAREA, fecha = LocalDate.of(2026, 10, 30)))
        }
        var abierta: Long? = null
        val viewModel = DiaViewModel(repositorio, actividades, reloj)
        compose.setContent {
            TaskToDoTheme {
                DiaScreen(null, {}, onAbrirActividad = { abierta = it }, onNuevaActividad = {}, viewModel = viewModel)
            }
        }
        esperarTexto("🗒 Leer artículo")

        compose.onNodeWithContentDescription("Marcar Leer artículo").performClick()
        compose.waitUntil(5_000) { runBlocking { db.actividadDao().obtener(id)?.estado } == EstadoActividad.CUMPLIDA }

        compose.onNodeWithContentDescription("Desmarcar Leer artículo").performClick()
        compose.waitUntil(5_000) { runBlocking { db.actividadDao().obtener(id)?.estado } == EstadoActividad.PENDIENTE }

        compose.onNodeWithText("🗒 Leer artículo").performClick()
        compose.waitUntil(5_000) { abierta == id }
    }

    /** Room emite en su propio hilo: se espera a que el dato llegue a la pantalla. */
    private fun esperarTexto(texto: String) =
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
}
