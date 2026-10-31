package com.calendar.tasktodo.sistema.avisos

import com.calendar.tasktodo.data.ajustes.AjustesAvisos
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.ReglaAjustes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ServicioAvisosTest {

    @get:Rule
    val ajustes = ReglaAjustes()

    private class ProgramadorFalso : ProgramadorAvisos {
        val programados = mutableMapOf<Int, AvisoProgramado>()
        override fun puedeExactas() = true
        override fun programar(aviso: AvisoProgramado) {
            programados[aviso.codigo] = aviso
        }
        override fun cancelar(codigo: Int) {
            programados.remove(codigo)
        }
    }

    private class NotificadorFalso : Notificador {
        val actividades = mutableListOf<Pair<String, Boolean>>()
        val clases = mutableListOf<String>()
        var resumenes = mutableListOf<Pair<Int, Int>>()
        val cerradas = mutableListOf<Long>()
        override fun actividad(actividad: ActividadConMateria, diaAnterior: Boolean) {
            actividades += actividad.actividad.titulo to diaAnterior
        }
        override fun clase(clase: ClaseConMateria) {
            clases += clase.materiaNombre
        }
        override fun resumen(delDia: List<ActividadConMateria>, atrasadas: List<ActividadConMateria>) {
            resumenes += delDia.size to atrasadas.size
        }
        override fun cerrarActividad(id: Long) {
            cerradas += id
        }
    }

    private val horario = DatosDePrueba.horarioCargado()
    private val programador = ProgramadorFalso()
    private val notificador = NotificadorFalso()

    private fun servicio() = ServicioAvisos(
        horario.actividadDao, horario.claseDao, ajustes.repositorio, programador, notificador, DatosDePrueba.reloj,
    )

    private val examen = DatosDePrueba.examenProximo

    @Test
    fun `reprogramar programa los avisos de la ventana y guarda sus codigos`() = runTest {
        servicio().reprogramarTodo()

        val tipos = programador.programados.values.map { it.tipo to it.refId }.toSet()
        assertTrue(TipoAviso.DIA_ANTERIOR to examen.id in tipos)
        assertTrue(TipoAviso.RESUMEN to 0L in tipos)
        assertEquals(programador.programados.keys, ajustes.repositorio.codigosProgramados())
    }

    @Test
    fun `al marcar una actividad y reprogramar, sus avisos se cancelan (CP-2_25)`() = runTest {
        val servicio = servicio()
        servicio.reprogramarTodo()
        assertTrue(AvisoProgramado.codigoDe(TipoAviso.DIA_ANTERIOR, examen.id) in programador.programados)

        horario.actividadDao.cambiarEstado(examen.id, EstadoActividad.CUMPLIDA)
        servicio.reprogramarTodo()

        assertFalse(AvisoProgramado.codigoDe(TipoAviso.DIA_ANTERIOR, examen.id) in programador.programados)
        assertFalse(examen.id * 10 in ajustes.repositorio.codigosProgramados().map { it.toLong() })
    }

    @Test
    fun `reprogramar dos veces no duplica (CP-2_24)`() = runTest {
        val servicio = servicio()
        servicio.reprogramarTodo()
        val primera = programador.programados.toMap()

        servicio.reprogramarTodo()

        assertEquals(primera, programador.programados)
    }

    @Test
    fun `una alarma de una actividad ya cumplida no muestra nada (CP-2_30)`() = runTest {
        horario.actividadDao.cambiarEstado(examen.id, EstadoActividad.CUMPLIDA)

        servicio().alSonar(TipoAviso.ACTIVIDAD, examen.id)

        assertTrue(notificador.actividades.isEmpty())
    }

    @Test
    fun `una alarma de una actividad pendiente muestra su aviso`() = runTest {
        servicio().alSonar(TipoAviso.DIA_ANTERIOR, examen.id)

        assertEquals(listOf("Examen parcial" to true), notificador.actividades)
    }

    @Test
    fun `con el tipo desactivado no se muestra el aviso`() = runTest {
        ajustes.repositorio.guardarAvisos(AjustesAvisos(diaAnterior = false))

        servicio().alSonar(TipoAviso.DIA_ANTERIOR, examen.id)

        assertTrue(notificador.actividades.isEmpty())
    }

    @Test
    fun `el resumen cuenta lo de hoy y las atrasadas, y sin nada no se envia (CP-2_27)`() = runTest {
        val servicio = servicio()
        servicio.alSonar(TipoAviso.RESUMEN, 0)
        assertEquals(listOf(1 to 1), notificador.resumenes)

        horario.actividades.value = emptyList()
        servicio.alSonar(TipoAviso.RESUMEN, 0)
        assertEquals(1, notificador.resumenes.size)
    }

    @Test
    fun `el aviso de clase solo llega si esta activado`() = runTest {
        val servicio = servicio()
        servicio.alSonar(TipoAviso.CLASE, 13)
        assertTrue(notificador.clases.isEmpty())

        ajustes.repositorio.guardarAvisos(AjustesAvisos(clases = true))
        servicio.alSonar(TipoAviso.CLASE, 13)
        assertEquals(listOf("Física"), notificador.clases)
    }

    @Test
    fun `marcar desde la notificacion la cumple, la cierra y cancela sus avisos (CP-2_31)`() = runTest {
        val servicio = servicio()
        servicio.reprogramarTodo()

        servicio.marcarDesdeNotificacion(examen.id)

        assertEquals(EstadoActividad.CUMPLIDA, horario.actividades.value.first { it.id == examen.id }.estado)
        assertEquals(listOf(examen.id), notificador.cerradas)
        assertTrue(programador.programados.values.none { it.refId == examen.id })
    }

    @Test
    fun `los ajustes guardados se leen igual`() = runTest {
        val cambiados = AjustesAvisos(clases = true, minutosClase = 15, horaResumen = DatosDePrueba.hora(6, 30), prioridadAlarma = false)
        ajustes.repositorio.guardarAvisos(cambiados)

        assertEquals(cambiados, ajustes.repositorio.avisos.first())
    }
}
