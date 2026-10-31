package com.calendar.tasktodo.data.respaldo

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.data.ajustes.Frecuencia
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.Clase
import com.calendar.tasktodo.data.db.entidad.DiaGym
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.Medida
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.Materia
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** La restauración con Room real: transacción, claves foráneas e ids conservados. */
@RunWith(AndroidJUnit4::class)
class RestauracionRoomTest {

    private lateinit var db: AgendaDatabase
    private val archivos = linkedMapOf<String, String>()

    private class DataStoreEnMemoria : DataStore<Preferences> {
        private val estado = MutableStateFlow(emptyPreferences())
        override val data = estado
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) =
            transform(estado.value).also { estado.value = it }
    }

    private val almacen = object : AlmacenRespaldos {
        override fun registrar(carpeta: String) = Unit
        override fun disponible(carpeta: String) = true
        override fun nombre(carpeta: String) = "Prueba"
        override fun escribir(carpeta: String, nombre: String, contenido: String) {
            archivos[nombre] = contenido
        }
        override fun listar(carpeta: String) = archivos.keys.map { it to it }
        override fun borrar(uri: String) {
            archivos.remove(uri)
        }
        override fun leer(uri: String) = archivos.getValue(uri)
        override fun escribirEn(uri: String, contenido: String) {
            archivos[uri] = contenido
        }
    }

    private val planificador = object : PlanificadorRespaldo {
        override fun programar(frecuencia: Frecuencia) = Unit
        override fun cancelar() = Unit
        override fun ejecutarAhora() = Unit
    }

    private lateinit var repo: RespaldoRepository

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AgendaDatabase::class.java).build()
        repo = RespaldoRepository(
            db.materiaDao(), db.claseDao(), db.actividadDao(), db.gymDao(), AjustesRepository(DataStoreEnMemoria()), almacen, planificador,
            transaccion = { bloque -> db.withTransaction { bloque() } },
            reloj = Clock.systemUTC(),
            versionApp = "prueba",
        )
        runBlocking {
            repo.registrarCarpeta("carpeta")
            val calculo = db.materiaDao().insertar(Materia(nombre = "Cálculo", color = 1))
            db.claseDao().insertar(Clase(materiaId = calculo, diaSemana = DayOfWeek.MONDAY, horaInicio = LocalTime.of(7, 0), horaFin = LocalTime.of(9, 0)))
            db.actividadDao().insertar(Actividad(titulo = "Tarea", tipo = TipoActividad.TAREA, fecha = LocalDate.of(2026, 11, 2), materiaId = calculo))
            val pierna = db.gymDao().insertarRutina(Rutina(nombre = "Pierna", descansoEntreEjerciciosSeg = 120))
            db.gymDao().insertarEjercicio(Ejercicio(rutinaId = pierna, orden = 0, nombre = "Sentadilla", medida = Medida.REPETICIONES, series = 4, repeticiones = 10, pesoKg = 60.0))
            val dia = db.gymDao().insertarDia(DiaGym(diaSemana = DayOfWeek.MONDAY, hora = LocalTime.of(18, 0), rutinaId = pierna))
            db.gymDao().insertarGenerada(
                Actividad(titulo = "Pierna", tipo = TipoActividad.GYM, fecha = LocalDate.of(2026, 11, 2), hora = LocalTime.of(18, 0),
                    rutinaId = pierna, diaGymId = dia, fechaProgramada = LocalDate.of(2026, 11, 2)),
            )
        }
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun respaldarYRestaurarConRoomDejaLosMismosDatosEIds_CP_3_08() = runBlocking {
        val contenido = repo.generar()
        val antes = Triple(db.materiaDao().todas(), db.claseDao().todas(), db.actividadDao().todas())
        val gymAntes = Triple(db.gymDao().rutinas(), db.gymDao().todosLosEjercicios(), db.gymDao().dias())
        db.materiaDao().eliminarTodas()
        db.materiaDao().insertar(Materia(nombre = "Otra", color = 2))

        assertEquals(ResultadoRestauracion.Exito, repo.restaurar(contenido))

        assertEquals(antes, Triple(db.materiaDao().todas(), db.claseDao().todas(), db.actividadDao().todas()))
        assertEquals(gymAntes, Triple(db.gymDao().rutinas(), db.gymDao().todosLosEjercicios(), db.gymDao().dias()))
        assertTrue(archivos.keys.any { it.startsWith("agenda-seguridad-") })
    }

    @Test
    fun siLaRestauracionFallaLaTransaccionSeDeshace_CP_3_09() = runBlocking {
        val antes = Triple(db.materiaDao().todas(), db.claseDao().todas(), db.actividadDao().todas())
        // Una clase que apunta a una materia que no existe: la clave foránea falla a mitad de camino
        val malo = """{"versionBaseDatos":2,"versionApp":"2.0","fecha":"2026-10-30T10:00:00Z",
            "materias":[{"id":1,"nombre":"Nueva","color":3}],
            "clases":[{"id":1,"materiaId":77,"diaSemana":1,"horaInicio":"07:00","horaFin":"09:00"}]}"""

        val resultado = repo.restaurar(malo)

        assertTrue(resultado is ResultadoRestauracion.Error)
        assertEquals(antes, Triple(db.materiaDao().todas(), db.claseDao().todas(), db.actividadDao().todas()))
    }
}
