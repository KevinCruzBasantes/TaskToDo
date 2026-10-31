package com.calendar.tasktodo.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Cada versión del esquema se crea desde su JSON exportado y migra a la siguiente sin perder
 * datos (RNF-04, modelo de datos §5).
 */
@RunWith(AndroidJUnit4::class)
class EsquemaTest {

    @get:Rule
    val ayudante = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AgendaDatabase::class.java)

    @Test
    fun laVersion1SeCreaDesdeSuEsquemaExportado_CP_1_12() {
        ayudante.createDatabase("esquema-v1.db", 1).use { db ->
            db.execSQL("INSERT INTO materia (id, nombre, color) VALUES (1, 'Cálculo', 1)")
            db.execSQL(
                "INSERT INTO clase (materia_id, dia_semana, hora_inicio, hora_fin, aula) VALUES (1, 1, '07:00', '09:00', NULL)",
            )
            db.query("SELECT COUNT(*) FROM clase").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun laMigracionDe1A2ConservaMateriasYClases_CP_2_12() {
        val nombre = "migracion-1-2.db"
        ayudante.createDatabase(nombre, 1).use { db ->
            db.execSQL("INSERT INTO materia (id, nombre, color) VALUES (1, 'Prueba 1.0', -14575885)")
            db.execSQL(
                "INSERT INTO clase (id, materia_id, dia_semana, hora_inicio, hora_fin, aula) VALUES (5, 1, 1, '07:00', '09:00', 'sala b')",
            )
        }

        // Valida que el esquema migrado sea igual al exportado de la versión 2
        ayudante.runMigrationsAndValidate(nombre, 2, true).close()

        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.databaseBuilder(contexto, AgendaDatabase::class.java, nombre).build()
        try {
            runBlocking {
                val clase = db.claseDao().observarTodas().first().single()
                assertEquals("Prueba 1.0", clase.materiaNombre)
                assertEquals(DayOfWeek.MONDAY, clase.clase.diaSemana)
                assertEquals(LocalTime.of(7, 0), clase.clase.horaInicio)
                assertEquals("sala b", clase.clase.aula)
                assertEquals(0, db.actividadDao().observarTodas().first().size)

                // La tabla nueva funciona después de migrar
                val id = db.actividadDao().insertar(
                    com.calendar.tasktodo.data.db.entidad.Actividad(
                        titulo = "Primera", tipo = com.calendar.tasktodo.data.db.entidad.TipoActividad.TAREA,
                        fecha = java.time.LocalDate.of(2026, 11, 2), materiaId = 1,
                    ),
                )
                assertEquals(EstadoActividad.PENDIENTE, db.actividadDao().obtener(id)?.estado)
            }
        } finally {
            db.close()
            contexto.deleteDatabase(nombre)
        }
    }

    @Test
    fun laMigracionDe2A3ConservaLosDatosYAgregaElGym_CP_5_32() {
        val nombre = "migracion-2-3.db"
        ayudante.createDatabase(nombre, 2).use { db ->
            db.execSQL("INSERT INTO materia (id, nombre, color) VALUES (1, 'Cálculo', 1)")
            db.execSQL("INSERT INTO clase (id, materia_id, dia_semana, hora_inicio, hora_fin, aula) VALUES (5, 1, 1, '07:00', '09:00', NULL)")
            db.execSQL(
                "INSERT INTO actividad (id, titulo, tipo, fecha, hora, materia_id, notas, aviso_minutos_antes, estado) " +
                    "VALUES (7, 'Examen', 'EXAMEN', '2026-11-06', '10:30', 1, NULL, 30, 'PENDIENTE')",
            )
        }

        ayudante.runMigrationsAndValidate(nombre, 3, true).close()

        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.databaseBuilder(contexto, AgendaDatabase::class.java, nombre).build()
        try {
            runBlocking {
                val examen = db.actividadDao().obtener(7)!!
                assertEquals("Examen", examen.titulo)
                assertEquals(null, examen.rutinaId)
                assertEquals(1, db.claseDao().todas().size)

                val rutina = db.gymDao().insertarRutina(com.calendar.tasktodo.data.db.entidad.Rutina(nombre = "Pierna"))
                db.gymDao().insertarEjercicio(
                    com.calendar.tasktodo.data.db.entidad.Ejercicio(
                        rutinaId = rutina, orden = 0, nombre = "Sentadilla",
                        medida = com.calendar.tasktodo.data.db.entidad.Medida.REPETICIONES, series = 4, repeticiones = 10,
                    ),
                )
                assertEquals(1, db.gymDao().ejercicios(rutina).size)
            }
        } finally {
            db.close()
            contexto.deleteDatabase(nombre)
        }
    }

    @Test
    fun laMigracionDe3A4ConservaTodoYAgregaElRegistroDePeso_CP_6_11() {
        val nombre = "migracion-3-4.db"
        ayudante.createDatabase(nombre, 3).use { db ->
            db.execSQL("INSERT INTO materia (id, nombre, color) VALUES (1, 'Cálculo', 1)")
            db.execSQL("INSERT INTO clase (id, materia_id, dia_semana, hora_inicio, hora_fin, aula) VALUES (5, 1, 1, '07:00', '09:00', NULL)")
            db.execSQL("INSERT INTO rutina (id, nombre, descanso_entre_ejercicios_seg) VALUES (2, 'Pierna', 120)")
            db.execSQL(
                "INSERT INTO ejercicio (id, rutina_id, orden, nombre, medida, series, repeticiones, duracion_seg, peso_kg, " +
                    "descanso_series_seg, descanso_tras_ejercicio_seg, nota) VALUES (3, 2, 0, 'Sentadilla', 'REPETICIONES', 4, 10, NULL, 60.0, 90, NULL, NULL)",
            )
            db.execSQL("INSERT INTO dia_gym (id, dia_semana, hora, rutina_id) VALUES (4, 1, '18:00', 2)")
            db.execSQL(
                "INSERT INTO actividad (id, titulo, tipo, fecha, hora, materia_id, notas, aviso_minutos_antes, estado, rutina_id, dia_gym_id, fecha_programada) " +
                    "VALUES (7, 'Pierna', 'GYM', '2026-11-02', '18:00', NULL, NULL, NULL, 'PENDIENTE', 2, 4, '2026-11-02')",
            )
            db.execSQL(
                "INSERT INTO sesion_gym (id, actividad_id, rutina_id, nombre_rutina, fecha, inicio, fin, duracion_seg, estado, descanso_termina_en, orden_ejercicios) " +
                    "VALUES (8, NULL, 2, 'Pierna', '2026-10-26', 1793037600000, 1793040600000, 3000, 'TERMINADA', NULL, '3')",
            )
            db.execSQL(
                "INSERT INTO serie_realizada (id, sesion_id, ejercicio_id, nombre_ejercicio, orden_ejercicio, numero_serie, medida, repeticiones, duracion_seg, peso_kg, completada_en) " +
                    "VALUES (9, 8, 3, 'Sentadilla', 1, 1, 'REPETICIONES', 10, NULL, 60.0, 1793037700000)",
            )
        }

        ayudante.runMigrationsAndValidate(nombre, 4, true).close()

        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.databaseBuilder(contexto, AgendaDatabase::class.java, nombre).build()
        try {
            runBlocking {
                assertEquals(1, db.claseDao().todas().size)
                assertEquals("Pierna", db.gymDao().obtenerRutina(2)?.nombre)
                assertEquals(1, db.gymDao().ejercicios(2).size)
                assertEquals(1, db.gymDao().dias().size)
                assertEquals(java.time.LocalDate.of(2026, 11, 2), db.actividadDao().obtener(7)?.fechaProgramada)
                assertEquals(3000, db.gymDao().obtenerSesion(8)?.duracionSeg)
                assertEquals(listOf(3L), db.gymDao().obtenerSesion(8)?.ordenEjercicios)
                assertEquals(1, db.gymDao().series(8).size)

                db.pesoDao().insertar(com.calendar.tasktodo.data.db.entidad.RegistroPeso(fecha = java.time.LocalDate.of(2026, 10, 30), pesoKg = 72.5))
                assertEquals(1, db.pesoDao().todos().size)
            }
        } finally {
            db.close()
            contexto.deleteDatabase(nombre)
        }
    }
}
