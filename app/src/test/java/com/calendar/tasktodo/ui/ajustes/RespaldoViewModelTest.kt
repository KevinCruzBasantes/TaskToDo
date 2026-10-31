package com.calendar.tasktodo.ui.ajustes

import com.calendar.tasktodo.data.respaldo.LecturaRespaldo
import com.calendar.tasktodo.data.respaldo.RespaldoRepository
import com.calendar.tasktodo.prueba.AlmacenFalso
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.PlanificadorRespaldoFalso
import com.calendar.tasktodo.prueba.ReglaAjustes
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RespaldoViewModelTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @get:Rule
    val ajustes = ReglaAjustes()

    private val horario = DatosDePrueba.horarioCargado()
    private val almacen = AlmacenFalso()
    private val planificador = PlanificadorRespaldoFalso()

    private fun vm() = RespaldoViewModel(
        RespaldoRepository(
            horario.materiaDao, horario.claseDao, horario.actividadDao, ajustes.repositorio, almacen, planificador,
            transaccion = { it() }, reloj = DatosDePrueba.reloj, versionApp = "3.0", io = Dispatchers.Unconfined,
        ),
    )

    @Test
    fun `respaldar sin carpeta la pide y al registrarla sigue respaldando (CU-21 2a)`() = runTest {
        val vm = vm()
        assertFalse(vm.estado.value.carpetaDisponible)

        vm.respaldarAhora()
        assertTrue(vm.estado.value.pedirCarpeta)
        assertTrue(almacen.archivos.isEmpty())

        vm.carpetaPedida()
        vm.registrarCarpeta("carpeta")

        assertTrue(vm.estado.value.carpetaDisponible)
        assertEquals("AgendaRespaldos", vm.estado.value.nombreCarpeta)
        assertEquals(1, almacen.archivos.size)
        assertEquals("Respaldo creado", vm.estado.value.mensaje)
        assertTrue(vm.estado.value.ultimo != null)
    }

    @Test
    fun `restaurar desde la lista pasa por el resumen y la confirmacion (CU-22)`() = runTest {
        val vm = vm()
        vm.registrarCarpeta("carpeta")
        vm.respaldarAhora()
        horario.materias.value = emptyList()
        horario.clases.value = emptyList()

        vm.abrirRestaurar()
        val archivo = vm.estado.value.archivos!!.single()
        vm.elegirArchivo(archivo.uri)
        assertNull(vm.estado.value.archivos)
        val lectura = vm.estado.value.lectura as LecturaRespaldo.Valido
        assertEquals(3, lectura.resumen.materias)

        vm.confirmarRestaurar()

        assertTrue(vm.estado.value.restaurado)
        assertEquals(3, horario.materias.value.size)
        assertTrue(almacen.archivos.keys.any { it.startsWith("agenda-seguridad-") })
    }

    @Test
    fun `un archivo invalido se informa y no cambia nada`() = runTest {
        val vm = vm()
        almacen.archivos["otro.json"] = "basura"

        vm.elegirArchivo("uri:otro.json")

        assertEquals(LecturaRespaldo.Invalido, vm.estado.value.lectura)
        vm.confirmarRestaurar()
        assertFalse(vm.estado.value.restaurado)
        assertEquals(3, horario.materias.value.size)
    }

    @Test
    fun `activar el automatico sin carpeta la pide y despues lo programa (CU-23)`() = runTest {
        val vm = vm()

        vm.cambiarAutomatico(true)
        assertTrue(vm.estado.value.pedirCarpeta)
        assertNull(planificador.programado)

        vm.carpetaPedida()
        vm.registrarCarpeta("carpeta")

        assertTrue(vm.estado.value.automatico)
        assertEquals(com.calendar.tasktodo.data.ajustes.Frecuencia.SEMANAL, planificador.programado)

        vm.cambiarFrecuencia(com.calendar.tasktodo.data.ajustes.Frecuencia.DIARIA)
        assertEquals(com.calendar.tasktodo.data.ajustes.Frecuencia.DIARIA, planificador.programado)
    }

    @Test
    fun `una copia en otra ubicacion cuenta como respaldo (CU-21 1a)`() = runTest {
        val vm = vm()

        vm.copiarA("uri:drive.json")

        assertTrue("drive.json" in almacen.archivos)
        assertEquals("Copia guardada", vm.estado.value.mensaje)
        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }
}
