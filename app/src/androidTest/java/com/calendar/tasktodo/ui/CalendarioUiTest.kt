package com.calendar.tasktodo.ui

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
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.HorarioRepository
import com.calendar.tasktodo.ui.calendario.CalendarioScreen
import com.calendar.tasktodo.ui.calendario.MesViewModel
import com.calendar.tasktodo.ui.calendario.SemanaViewModel
import com.calendar.tasktodo.ui.theme.TaskToDoTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class CalendarioUiTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: AgendaDatabase

    /** Viernes 30 de octubre de 2026, 10:00. */
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 10, 30, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AgendaDatabase::class.java).build()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun delMesALaSemanaYDeLaSemanaAlDia_CP_4_03() {
        runBlocking {
            db.actividadDao().insertar(Actividad(titulo = "Proyecto final", tipo = TipoActividad.TAREA, fecha = LocalDate.of(2026, 10, 14)))
        }
        val horario = HorarioRepository(db.materiaDao(), db.claseDao())
        val actividades = ActividadRepository(db.actividadDao())
        val semana = SemanaViewModel(horario, actividades, reloj)
        val mes = MesViewModel(actividades, reloj)
        var diaAbierto: LocalDate? = null
        compose.setContent {
            TaskToDoTheme {
                CalendarioScreen(
                    onAbrirDia = { diaAbierto = it }, onAbrirClase = {}, onNuevaClase = {}, onNuevaMateria = {}, onMaterias = {},
                    semana = semana, mes = mes,
                )
            }
        }

        compose.onNodeWithText("Mes").performClick()
        compose.onNodeWithText("Octubre 2026").assertExists()
        compose.onNodeWithContentDescription("Miércoles 14 de octubre, 1 actividades").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Proyecto final", substring = true).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithText("Ver semana").performClick()
        compose.onNodeWithText("12 oct – 18 oct 2026").assertExists()

        compose.onNodeWithContentDescription("Abrir Miércoles 14 de octubre").performClick()
        compose.waitUntil(5_000) { diaAbierto == LocalDate.of(2026, 10, 14) }
    }
}
