package com.calendar.tasktodo.data.respaldo

import com.calendar.tasktodo.data.ajustes.AjustesAvisos
import com.calendar.tasktodo.data.ajustes.Frecuencia
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.prueba.AlmacenFalso
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.PlanificadorRespaldoFalso
import com.calendar.tasktodo.prueba.ReglaAjustes
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.prueba.observar
import com.calendar.tasktodo.ui.horario.MateriasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

class RespaldoRepositoryTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @get:Rule
    val ajustes = ReglaAjustes()

    private val horario = DatosDePrueba.horarioCargado()
    private val almacen = AlmacenFalso()
    private val planificador = PlanificadorRespaldoFalso()
    private var cambiosAvisados = 0

    private fun repositorio(reloj: Clock = DatosDePrueba.reloj) = RespaldoRepository(
        materias = horario.materiaDao,
        clases = horario.claseDao,
        actividades = horario.actividadDao,
        ajustes = ajustes.repositorio,
        almacen = almacen,
        planificador = planificador,
        transaccion = { it() },
        reloj = reloj,
        versionApp = "3.0",
        observador = { cambiosAvisados++ },
        io = Dispatchers.Unconfined,
    )

    private suspend fun conCarpeta() = repositorio().also { it.registrarCarpeta("carpeta") }

    @Test
    fun `generar y leer devuelve exactamente los mismos datos (CP-3_01)`() = runTest {
        val repo = repositorio()
        val json = repo.generar()
        val dto = kotlinx.serialization.json.Json.decodeFromString(RespaldoDto.serializer(), json)

        assertEquals(horario.materias.value.sortedBy { it.id }, dto.materias.map { it.aEntidad() })
        assertEquals(horario.clases.value.sortedBy { it.id }, dto.clases.map { it.aEntidad() })
        assertEquals(horario.actividades.value.sortedBy { it.id }, dto.actividades.map { it.aEntidad() })
    }

    @Test
    fun `el JSON incluye versiones, fecha, ajustes y todas las tablas (CP-3_02)`() = runTest {
        ajustes.repositorio.guardarAvisos(AjustesAvisos(clases = true))
        val json = repositorio().generar()

        listOf("\"versionBaseDatos\": ${AgendaDatabase.VERSION}", "\"versionApp\": \"3.0\"", "\"fecha\": \"2026-10-30T10:00:00Z\"",
            "\"avisosClases\": true", "\"materias\"", "\"clases\"", "\"actividades\"").forEach {
            assertTrue("falta $it", it in json)
        }
        assertFalse("la carpeta no viaja en el respaldo", "carpeta" in json)
    }

    @Test
    fun `respaldar sin carpeta lo indica y no crea nada (CP-3_03)`() = runTest {
        assertEquals(ResultadoRespaldo.SinCarpeta, repositorio().respaldar(TipoRespaldo.MANUAL))
        assertTrue(almacen.archivos.isEmpty())
    }

    @Test
    fun `respaldar ahora crea agenda-manual y actualiza la fecha (CP-3_04)`() = runTest {
        val resultado = conCarpeta().respaldar(TipoRespaldo.MANUAL)

        assertEquals(ResultadoRespaldo.Exito("agenda-manual-2026-10-30-1000.json"), resultado)
        assertEquals(setOf("agenda-manual-2026-10-30-1000.json"), almacen.archivos.keys)
        assertEquals(Instant.parse("2026-10-30T10:00:00Z"), ajustes.repositorio.respaldo.first().ultimo)
    }

    @Test
    fun `si falla la escritura no cambia la fecha del ultimo respaldo (CP-3_05)`() = runTest {
        val repo = conCarpeta()
        almacen.fallarAlEscribir = true

        assertTrue(repo.respaldar(TipoRespaldo.MANUAL) is ResultadoRespaldo.Error)
        assertTrue(repo.copiarA("uri:drive.json") is ResultadoRespaldo.Error)
        assertNull(ajustes.repositorio.respaldo.first().ultimo)
    }

    @Test
    fun `la lista muestra solo respaldos de la app, del mas reciente al mas antiguo (CP-3_06)`() = runTest {
        val repo = conCarpeta()
        listOf(
            "agenda-manual-2026-10-01-0900.json", "agenda-auto-2026-10-20-0300.json",
            "agenda-seguridad-2026-10-25-1200.json", "foto.jpg", "notas.json",
        ).forEach { almacen.archivos[it] = "{}" }

        val lista = repo.listar()

        assertEquals(
            listOf(TipoRespaldo.SEGURIDAD, TipoRespaldo.AUTOMATICO, TipoRespaldo.MANUAL),
            lista.map { it.tipo },
        )
        assertEquals(java.time.LocalDateTime.of(2026, 10, 25, 12, 0), lista.first().momento)
    }

    @Test
    fun `un archivo danado o de una version mas nueva se rechaza (CP-3_07)`() = runTest {
        val repo = repositorio()

        assertEquals(LecturaRespaldo.Invalido, repo.validar("esto no es json"))
        assertEquals(LecturaRespaldo.Invalido, repo.validar("""{"versionApp":"1.0"}"""))
        assertEquals(
            LecturaRespaldo.MasNuevo(99),
            repo.validar("""{"versionBaseDatos":99,"versionApp":"9.0","fecha":"2026-10-30T10:00:00Z"}"""),
        )
    }

    @Test
    fun `restaurar guarda antes la seguridad y deja exactamente los datos del archivo (CP-3_08)`() = runTest {
        val repo = conCarpeta()
        val contenido = repo.generar()
        val lectura = repo.validar(contenido) as LecturaRespaldo.Valido
        assertEquals(3, lectura.resumen.materias)
        assertEquals(4, lectura.resumen.actividades)
        // Los datos cambian después del respaldo
        horario.materias.value = listOf(DatosDePrueba.calculo.copy(id = 99, nombre = "Otra"))
        horario.clases.value = emptyList()
        horario.actividades.value = emptyList()

        assertEquals(ResultadoRestauracion.Exito, repo.restaurar(contenido))

        assertEquals(listOf("agenda-seguridad-2026-10-30-1000.json"), almacen.archivos.keys.toList())
        assertTrue("\"Otra\"" in almacen.archivos.values.single())
        assertEquals(listOf("Cálculo", "Física", "Inglés"), horario.materias.value.map { it.nombre })
        assertEquals(4, horario.clases.value.size)
        assertEquals(4, horario.actividades.value.size)
        assertEquals(1, cambiosAvisados)
    }

    @Test
    fun `restaurar sin poder guardar la seguridad no toca nada (CU-22 7a)`() = runTest {
        val repo = conCarpeta()
        val contenido = repo.generar()
        horario.materias.value = emptyList()
        almacen.fallarAlEscribir = true

        assertTrue(repo.restaurar(contenido) is ResultadoRestauracion.SinSeguridad)
        assertTrue(horario.materias.value.isEmpty())
    }

    @Test
    fun `restaurar un archivo con datos inconsistentes falla sin cambiar los ajustes (CU-22 8a)`() = runTest {
        val repo = conCarpeta()
        val malo = """{"versionBaseDatos":2,"versionApp":"2.0","fecha":"2026-10-30T10:00:00Z",
            "ajustes":{"avisosClases":true},
            "clases":[{"id":1,"materiaId":77,"diaSemana":1,"horaInicio":"07:00","horaFin":"09:00"}]}"""

        assertTrue(repo.restaurar(malo) is ResultadoRestauracion.Error)
        assertFalse(ajustes.repositorio.avisos.first().clases)
        assertEquals(0, cambiosAvisados)
    }

    @Test
    fun `un respaldo de la version 1, sin actividades, se restaura (CP-3_10)`() = runTest {
        val repo = conCarpeta()
        val version1 = """{"versionBaseDatos":1,"versionApp":"1.1","fecha":"2026-10-31T12:20:00Z",
            "materias":[{"id":1,"nombre":"Prueba 1.0","color":-14575885}],
            "clases":[{"id":5,"materiaId":1,"diaSemana":1,"horaInicio":"07:00","horaFin":"09:00","aula":"sala b"}]}"""

        assertTrue(repo.validar(version1) is LecturaRespaldo.Valido)
        assertEquals(ResultadoRestauracion.Exito, repo.restaurar(version1))

        assertEquals(listOf("Prueba 1.0"), horario.materias.value.map { it.nombre })
        assertEquals("sala b", horario.clases.value.single().aula)
        assertTrue(horario.actividades.value.isEmpty())
        assertEquals(AjustesAvisos(), ajustes.repositorio.avisos.first())
    }

    @Test
    fun `el automatico conserva los ultimos 5 y no toca manuales ni de seguridad (CP-3_11)`() = runTest {
        val repo = conCarpeta()
        (1..5).forEach { almacen.archivos["agenda-auto-2026-10-0$it-0300.json"] = "{}" }
        almacen.archivos["agenda-manual-2026-09-01-0900.json"] = "{}"
        almacen.archivos["agenda-manual-2026-09-02-0900.json"] = "{}"
        almacen.archivos["agenda-seguridad-2026-09-03-0900.json"] = "{}"

        repo.respaldar(TipoRespaldo.AUTOMATICO)

        val autos = almacen.archivos.keys.filter { it.startsWith("agenda-auto-") }.sorted()
        assertEquals(5, autos.size)
        assertFalse("agenda-auto-2026-10-01-0300.json" in autos)
        assertTrue("agenda-auto-2026-10-30-1000.json" in autos)
        assertEquals(3, almacen.archivos.keys.count { !it.startsWith("agenda-auto-") })
    }

    @Test
    fun `los dias sin respaldo cuentan desde el ultimo (CP-3_12)`() = runTest {
        val repo = repositorio()

        assertNull(repo.diasSinRespaldo(null))
        assertEquals(9L, repo.diasSinRespaldo(Instant.parse("2026-10-21T09:00:00Z")))
    }

    @Test
    fun `el automatico atrasado se encola al abrir, el que esta al dia no (CP-3_13)`() = runTest {
        val repo = conCarpeta()
        repo.configurarAutomatico(true, Frecuencia.SEMANAL)
        assertEquals(Frecuencia.SEMANAL, planificador.programado)

        ajustes.repositorio.guardarUltimoRespaldo(Instant.parse("2026-10-27T10:00:00Z"))
        repo.ponerAlDia()
        assertEquals(0, planificador.ejecutadoAhora)

        repositorio(Clock.offset(DatosDePrueba.reloj, Duration.ofDays(5))).ponerAlDia()
        assertEquals(1, planificador.ejecutadoAhora)

        repo.configurarAutomatico(false, Frecuencia.SEMANAL)
        assertNull(planificador.programado)
    }

    @Test
    fun `eliminar todas las materias puede respaldar antes (CP-3_14)`() = runTest {
        val repo = conCarpeta()
        val vm = MateriasViewModel(horario.repositorio, repo)

        vm.respaldarYEliminar()

        assertEquals(1, almacen.archivos.keys.count { it.startsWith("agenda-manual-") })
        assertTrue(horario.materias.value.isEmpty())
        assertTrue("Cálculo" in almacen.archivos.values.single())
    }

    @Test
    fun `sin carpeta, eliminar con respaldo pide la carpeta y sigue al registrarla`() = runTest {
        val vm = MateriasViewModel(horario.repositorio, repositorio())
        val estado = observar(vm.estado)

        vm.respaldarYEliminar()
        assertTrue(estado.value.pedirCarpeta)
        assertEquals(3, horario.materias.value.size)

        vm.carpetaPedida()
        vm.registrarCarpetaYSeguir("carpeta")

        assertTrue(horario.materias.value.isEmpty())
        assertEquals(1, almacen.archivos.size)
    }
}
