package com.calendar.tasktodo.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Las reglas que solo Room garantiza: claves foráneas, cascadas, índices únicos y consultas. */
@RunWith(AndroidJUnit4::class)
class HorarioDaoTest {

    private lateinit var db: AgendaDatabase

    private val materias get() = db.materiaDao()
    private val clases get() = db.claseDao()

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AgendaDatabase::class.java).build()
    }

    @After
    fun cerrar() = db.close()

    private suspend fun cargar(): Pair<Long, Long> {
        val calculo = materias.insertar(Materia(nombre = "Cálculo", color = 1))
        val fisica = materias.insertar(Materia(nombre = "Física", color = 2))
        clases.insertar(Clase(materiaId = calculo, diaSemana = DayOfWeek.MONDAY, horaInicio = hora(7), horaFin = hora(9)))
        clases.insertar(Clase(materiaId = calculo, diaSemana = DayOfWeek.WEDNESDAY, horaInicio = hora(7), horaFin = hora(9)))
        clases.insertar(Clase(materiaId = calculo, diaSemana = DayOfWeek.FRIDAY, horaInicio = hora(7), horaFin = hora(9)))
        clases.insertar(Clase(materiaId = fisica, diaSemana = DayOfWeek.TUESDAY, horaInicio = hora(9), horaFin = hora(11)))
        return calculo to fisica
    }

    @Test
    fun eliminarUnaMateriaBorraSusClasesEnCascada_CP_1_04() = runTest {
        val (calculo, _) = cargar()

        materias.eliminar(materias.obtener(calculo)!!)

        assertEquals(0, clases.contarDeMateria(calculo))
        assertEquals(listOf("Física"), clases.observarTodas().first().map { it.materiaNombre })
    }

    @Test
    fun eliminarTodasLasMateriasVaciaMateriasYClases_CP_1_05() = runTest {
        cargar()

        materias.eliminarTodas()

        assertEquals(0, materias.observarCantidad().first())
        assertTrue(clases.observarTodas().first().isEmpty())
    }

    @Test
    fun elIndiceUnicoRechazaElMismoNombreConOtrasMayusculas_CP_1_03() = runTest {
        cargar()

        // NOCASE solo pliega letras ASCII; las tildes las cubre HorarioRepository.existeMateria
        val repetida = runCatching { materias.insertar(Materia(nombre = "CáLCULO", color = 3)) }
        assertTrue(repetida.exceptionOrNull() is SQLiteConstraintException)
        val distinta = runCatching { materias.insertar(Materia(nombre = "Química", color = 3)) }
        assertTrue(distinta.isSuccess)
    }

    @Test
    fun elRepositorioDetectaNombresRepetidosConTildes_CP_1_03() = runTest {
        materias.insertar(Materia(nombre = "Álgebra", color = 1))
        val repositorio = com.calendar.tasktodo.data.repositorio.HorarioRepository(materias, clases)

        assertTrue(repositorio.existeMateria("ÁLGEBRA"))
        assertFalse(repositorio.existeMateria("Geometría"))
    }

    @Test
    fun unaClaseNecesitaUnaMateriaExistente() = runTest {
        val resultado = runCatching {
            clases.insertar(Clase(materiaId = 999, diaSemana = DayOfWeek.MONDAY, horaInicio = hora(7), horaFin = hora(8)))
        }

        assertTrue(resultado.exceptionOrNull() is SQLiteConstraintException)
    }

    @Test
    fun lasClasesDeUnDiaVanOrdenadasPorHora_CP_1_11() = runTest {
        val (calculo, fisica) = cargar()
        clases.insertar(Clase(materiaId = fisica, diaSemana = DayOfWeek.FRIDAY, horaInicio = hora(11), horaFin = hora(12)))
        clases.insertar(Clase(materiaId = calculo, diaSemana = DayOfWeek.FRIDAY, horaInicio = hora(9, 30), horaFin = hora(10)))

        val viernes = clases.observarPorDia(DayOfWeek.FRIDAY).first()

        assertEquals(listOf(hora(7), hora(9, 30), hora(11)), viernes.map { it.clase.horaInicio })
    }

    @Test
    fun losCrucesExcluyenLasQueSoloSeTocanYLaPropiaClase_CP_1_07() = runTest {
        cargar()
        val martes = clases.observarPorDia(DayOfWeek.TUESDAY).first().single().clase

        assertEquals(1, clases.cruces(DayOfWeek.TUESDAY, hora(10), hora(12), excluirId = 0).size)
        assertTrue(clases.cruces(DayOfWeek.TUESDAY, hora(11), hora(13), excluirId = 0).isEmpty())
        assertTrue(clases.cruces(DayOfWeek.TUESDAY, hora(7), hora(9), excluirId = 0).isEmpty())
        assertTrue(clases.cruces(DayOfWeek.TUESDAY, hora(9), hora(12), excluirId = martes.id).isEmpty())
    }

    @Test
    fun elConteoDeClasesPorMateria() = runTest {
        cargar()

        val conteo = materias.observarConConteo().first().associate { it.materia.nombre to it.clases }

        assertEquals(mapOf("Cálculo" to 3, "Física" to 1), conteo)
    }

    @Test
    fun eliminarUnaMateriaDejaSusActividadesSinMateria_RF_03() = runTest {
        val (calculo, _) = cargar()
        val actividades = db.actividadDao()
        val id = actividades.insertar(
            Actividad(titulo = "Ejercicios", tipo = TipoActividad.TAREA, fecha = LocalDate.of(2026, 11, 2), materiaId = calculo),
        )

        materias.eliminar(materias.obtener(calculo)!!)

        val actividad = actividades.obtenerConMateria(id)!!
        assertNull(actividad.actividad.materiaId)
        assertNull(actividad.materiaNombre)
    }

    @Test
    fun lasConsultasDeActividadesRespetanFechaEstadoYOrden() = runTest {
        val (calculo, _) = cargar()
        val actividades = db.actividadDao()
        val hoy = LocalDate.of(2026, 10, 30)
        actividades.insertar(Actividad(titulo = "Sin hora", tipo = TipoActividad.TAREA, fecha = hoy))
        actividades.insertar(Actividad(titulo = "Tarde", tipo = TipoActividad.CHARLA, fecha = hoy, hora = hora(16)))
        actividades.insertar(Actividad(titulo = "Mañana", tipo = TipoActividad.EXAMEN, fecha = hoy, hora = hora(8), materiaId = calculo))
        actividades.insertar(Actividad(titulo = "Atrasada", tipo = TipoActividad.TAREA, fecha = hoy.minusDays(2)))
        actividades.insertar(Actividad(titulo = "Examen viejo", tipo = TipoActividad.EXAMEN, fecha = hoy.minusDays(1)))
        actividades.insertar(
            Actividad(titulo = "Hecha", tipo = TipoActividad.TAREA, fecha = hoy.minusDays(1), estado = EstadoActividad.CUMPLIDA),
        )

        assertEquals(listOf("Mañana", "Tarde", "Sin hora"), actividades.observarDelDia(hoy).first().map { it.actividad.titulo })
        assertEquals(listOf("Atrasada"), actividades.observarAtrasadas(hoy).first().map { it.actividad.titulo })
        assertEquals("Cálculo", actividades.observarDelDia(hoy).first().first().materiaNombre)
        assertEquals(
            listOf("Mañana"),
            actividades.observarProximasDeMateria(calculo, hoy, 5).first().map { it.actividad.titulo },
        )
        // Las tres de hoy, la atrasada y el examen de ayer; la cumplida no cuenta
        assertEquals(5, actividades.pendientesEntre(hoy.minusDays(2), hoy).size)
    }

    private fun hora(h: Int, m: Int = 0) = LocalTime.of(h, m)
}
