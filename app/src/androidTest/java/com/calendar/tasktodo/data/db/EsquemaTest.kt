package com.calendar.tasktodo.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El esquema exportado de cada versión debe existir para poder probar las migraciones
 * (CP-1.12). Cada incremento que cambie el esquema agrega aquí su prueba de migración.
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
        assertEquals(1, AgendaDatabase.VERSION)
    }
}
