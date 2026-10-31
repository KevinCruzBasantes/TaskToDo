package com.calendar.tasktodo.ui.ajustes

import com.calendar.tasktodo.data.ajustes.AjustesAvisos
import com.calendar.tasktodo.data.db.entidad.ActividadConMateria
import com.calendar.tasktodo.data.db.entidad.ClaseConMateria
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.ReglaAjustes
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.prueba.observar
import com.calendar.tasktodo.sistema.avisos.AvisoProgramado
import com.calendar.tasktodo.sistema.avisos.Notificador
import com.calendar.tasktodo.sistema.avisos.ProgramadorAvisos
import com.calendar.tasktodo.sistema.avisos.ServicioAvisos
import com.calendar.tasktodo.sistema.avisos.TipoAviso
import com.calendar.tasktodo.sistema.compatibilidad.EstadoSistema
import com.calendar.tasktodo.sistema.compatibilidad.PendientesAlAbrir
import com.calendar.tasktodo.ui.actividades.DetalleActividadViewModel
import com.calendar.tasktodo.ui.navegacion.AppViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration

class AjustesViewModelsTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @get:Rule
    val ajustes = ReglaAjustes()

    private class EstadoFalso(
        override val esXiaomi: Boolean = true,
        var exactas: Boolean = false,
        var bateria: Boolean = false,
        var notificaciones: Boolean = true,
    ) : EstadoSistema {
        override fun alarmasExactas() = exactas
        override fun bateriaSinRestricciones() = bateria
        override fun notificacionesActivas() = notificaciones
    }

    @Test
    fun `cambiar un aviso lo guarda y recalcula los avisos (CP-2_28)`() = runTest {
        var recalculos = 0
        val vm = AjustesAvisosViewModel(ajustes.repositorio, EstadoFalso()) { recalculos++ }
        val estado = observar(vm.estado)
        assertEquals(AjustesAvisos(), estado.value)

        vm.cambiar { it.copy(clases = true, prioridadAlarma = false) }

        assertTrue(estado.value!!.clases)
        assertFalse(estado.value!!.prioridadAlarma)
        assertEquals(1, recalculos)
        assertFalse(vm.alarmasExactasPermitidas())
    }

    @Test
    fun `la guia lee el estado del celular y recuerda los pasos manuales (CP-2_36)`() = runTest {
        val sistema = EstadoFalso()
        val vm = GuiaMiuiViewModel(sistema, ajustes.repositorio)
        val manuales = observar(vm.pasosManuales)

        assertEquals(EstadoGuia(alarmasExactas = false, bateria = false, notificaciones = true), vm.leerEstado())
        sistema.bateria = true
        assertTrue(vm.leerEstado().bateria)

        vm.marcarHecho(GuiaMiuiViewModel.INICIO_AUTOMATICO)
        assertEquals(setOf(GuiaMiuiViewModel.INICIO_AUTOMATICO), manuales.value)
    }

    @Test
    fun `la guia se ofrece una sola vez y solo en Xiaomi (CU-01 7a)`() = runTest {
        val app = AppViewModel(DatosDePrueba.horarioCargado().repositorio, ajustes.repositorio, esXiaomi = true)
        val mostrar = observar(app.mostrarGuiaMiui)
        assertTrue(mostrar.value)

        GuiaMiuiViewModel(EstadoFalso(), ajustes.repositorio).marcarMostrada()

        assertFalse(mostrar.value)
        assertFalse(AppViewModel(DatosDePrueba.horarioCargado().repositorio, ajustes.repositorio, esXiaomi = false).mostrarGuiaMiui.value)
    }

    @Test
    fun `la comprobacion al abrir se hace como maximo una vez por hora (RNF-11)`() = runTest {
        val horario = DatosDePrueba.horarioCargado()
        var recalculos = 0
        val programador = object : ProgramadorAvisos {
            override fun puedeExactas() = true
            override fun programar(aviso: AvisoProgramado) {
                // Cada recálculo programa exactamente un mantenimiento
                if (aviso.tipo == TipoAviso.MANTENIMIENTO) recalculos++
            }
            override fun cancelar(codigo: Int) = Unit
        }
        val notificador = object : Notificador {
            override fun actividad(actividad: ActividadConMateria, diaAnterior: Boolean) = Unit
            override fun clase(clase: ClaseConMateria) = Unit
            override fun resumen(delDia: List<ActividadConMateria>, atrasadas: List<ActividadConMateria>) = Unit
            override fun cerrarActividad(id: Long) = Unit
            override fun finDescanso(sesion: com.calendar.tasktodo.data.db.entidad.SesionGym) = Unit
        }
        fun comprobacion(reloj: Clock) = PendientesAlAbrir(
            ajustes.repositorio,
            ServicioAvisos(horario.actividadDao, horario.claseDao, ajustes.repositorio, programador, notificador, reloj),
            reloj,
        )

        comprobacion(DatosDePrueba.reloj).ejecutar()
        comprobacion(Clock.offset(DatosDePrueba.reloj, Duration.ofMinutes(59))).ejecutar()
        assertEquals(1, recalculos)

        comprobacion(Clock.offset(DatosDePrueba.reloj, Duration.ofMinutes(61))).ejecutar()
        assertEquals(2, recalculos)
    }

    @Test
    fun `el detalle de actividad marca, desmarca y elimina`() = runTest {
        val horario = DatosDePrueba.horarioCargado()
        val id = DatosDePrueba.examenProximo.id
        val vm = DetalleActividadViewModel(horario.actividadRepositorio, DatosDePrueba.reloj, id)
        val estado = observar(vm.estado)
        assertEquals("Examen parcial", estado.value.actividad?.actividad?.titulo)
        assertEquals("Física", estado.value.actividad?.materiaNombre)

        vm.cambiarCumplida(true)
        assertEquals(EstadoActividad.CUMPLIDA, estado.value.actividad?.actividad?.estado)

        vm.eliminar()
        assertNull(estado.value.actividad)
        assertFalse(estado.value.cargando)
    }
}
