package com.calendar.tasktodo.prueba

import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.Materia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/** Datos y reloj comunes a las pruebas (plan de pruebas §1). */
object DatosDePrueba {

    /** "Hoy" en todas las pruebas: viernes 30 de octubre de 2026, 10:00. */
    val HOY: LocalDate = LocalDate.of(2026, 10, 30)

    val reloj: Clock = Clock.fixed(LocalDateTime.of(HOY, LocalTime.of(10, 0)).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    const val AZUL = 0xFF1E88E5.toInt()
    const val VERDE = 0xFF43A047.toInt()
    const val NARANJA = 0xFFFB8C00.toInt()

    val calculo = Materia(id = 1, nombre = "Cálculo", color = AZUL)
    val fisica = Materia(id = 2, nombre = "Física", color = VERDE)
    val ingles = Materia(id = 3, nombre = "Inglés", color = NARANJA)

    /** Cálculo lunes, miércoles y viernes 07:00–09:00; Física martes 09:00–11:00. */
    val clases = listOf(
        Clase(id = 10, materiaId = 1, diaSemana = DayOfWeek.MONDAY, horaInicio = hora(7), horaFin = hora(9), aula = "A-204"),
        Clase(id = 11, materiaId = 1, diaSemana = DayOfWeek.WEDNESDAY, horaInicio = hora(7), horaFin = hora(9), aula = "A-204"),
        Clase(id = 12, materiaId = 1, diaSemana = DayOfWeek.FRIDAY, horaInicio = hora(7), horaFin = hora(9), aula = "A-204"),
        Clase(id = 13, materiaId = 2, diaSemana = DayOfWeek.TUESDAY, horaInicio = hora(9), horaFin = hora(11), aula = "Lab 2"),
    )

    fun hora(h: Int, m: Int = 0): LocalTime = LocalTime.of(h, m)

    /** Un horario falso cargado con las tres materias y las cuatro clases. */
    fun horarioCargado(): HorarioFalso = HorarioFalso().apply {
        materias.value = listOf(calculo, fisica, ingles)
        clases.value = DatosDePrueba.clases
    }
}

/** Corre `viewModelScope` en un despachador de prueba. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReglaDespachadorPrincipal(
    private val despachador: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(despachador)

    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Mantiene suscrito un `stateIn(WhileSubscribed)` durante la prueba, como lo haría la pantalla. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.observar(flujo: StateFlow<T>): StateFlow<T> {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect {} }
    return flujo
}
