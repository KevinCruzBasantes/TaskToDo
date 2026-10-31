package com.calendar.tasktodo.sistema.avisos

import com.calendar.tasktodo.data.ajustes.AjustesAvisos
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.HOY
import com.calendar.tasktodo.prueba.DatosDePrueba.hora
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class PlanificadorAvisosTest {

    /** Viernes 30 de octubre de 2026, 10:00. */
    private val ahora = ZonedDateTime.now(DatosDePrueba.reloj)

    private fun instante(dia: Int, mes: Int, h: Int, m: Int = 0) =
        LocalDateTime.of(2026, mes, dia, h, m).toInstant(ZoneOffset.UTC)

    private fun planificar(
        actividades: List<Actividad> = emptyList(),
        ajustes: AjustesAvisos = AjustesAvisos(),
        momento: ZonedDateTime = ahora,
    ) = PlanificadorAvisos.planificar(actividades, DatosDePrueba.clases, ajustes, momento)

    private fun List<AvisoProgramado>.de(tipo: TipoAviso) = filter { it.tipo == tipo }

    private val examen = DatosDePrueba.examenProximo

    @Test
    fun `un aviso 30 minutos antes de las 10_30 suena a las 10_00 (CP-2_20)`() {
        // El 6 de noviembre a las 10:00 queda justo en el borde de la ventana: entra al día siguiente
        val avisos = planificar(listOf(examen), momento = ahora.plusDays(1))

        assertEquals(listOf(instante(6, 11, 10)), avisos.de(TipoAviso.ACTIVIDAD).map { it.instante })
    }

    @Test
    fun `una actividad sin hora avisa a la hora fija de los ajustes (CP-2_21)`() {
        val manana = Actividad(id = 60, titulo = "Leer", tipo = TipoActividad.TAREA, fecha = HOY.plusDays(1), avisoMinutosAntes = 0)

        val avisos = planificar(listOf(manana), AjustesAvisos(horaSinHora = hora(8)))

        assertEquals(listOf(instante(31, 10, 8)), avisos.de(TipoAviso.ACTIVIDAD).map { it.instante })
    }

    @Test
    fun `un examen avisa ademas el dia anterior (CP-2_22)`() {
        val avisos = planificar(listOf(examen), AjustesAvisos(horaDiaAnterior = hora(20)))

        assertEquals(listOf(instante(5, 11, 20)), avisos.de(TipoAviso.DIA_ANTERIOR).map { it.instante })
    }

    @Test
    fun `una actividad a 10 dias no se programa hasta que entra en la ventana (CP-2_23)`() {
        val lejana = examen.copy(id = 61, fecha = HOY.plusDays(10), avisoMinutosAntes = 15)

        assertTrue(planificar(listOf(lejana)).de(TipoAviso.ACTIVIDAD).isEmpty())
        assertEquals(1, planificar(listOf(lejana), momento = ahora.plusDays(4)).de(TipoAviso.ACTIVIDAD).size)
    }

    @Test
    fun `el mismo aviso tiene siempre el mismo codigo (CP-2_24)`() {
        val primero = planificar(listOf(examen)).map { it.codigo }
        val segundo = planificar(listOf(examen.copy(hora = hora(11)))).map { it.codigo }

        assertEquals(primero, segundo)
        assertEquals(431, AvisoProgramado.codigoDe(TipoAviso.DIA_ANTERIOR, 43))
    }

    @Test
    fun `los avisos que ya pasaron y los de actividades cumplidas no se programan (CP-2_25)`() {
        val hoySinHora = DatosDePrueba.tareaHoySinHora
        val cumplida = examen.copy(estado = EstadoActividad.CUMPLIDA)

        val avisos = planificar(listOf(hoySinHora, cumplida), momento = ahora.plusDays(1))

        assertTrue(avisos.none { it.refId == hoySinHora.id || it.refId == cumplida.id })
    }

    @Test
    fun `los avisos de clase vienen desactivados y al activarlos hay uno por clase (CP-2_26)`() {
        assertTrue(planificar().de(TipoAviso.CLASE).isEmpty())

        val avisos = planificar(ajustes = AjustesAvisos(clases = true, minutosClase = 10)).de(TipoAviso.CLASE)

        // Lunes 2, martes 3, miércoles 4 y el viernes siguiente (la de hoy a las 7:00 ya pasó)
        assertEquals(
            listOf(instante(2, 11, 6, 50), instante(3, 11, 8, 50), instante(4, 11, 6, 50), instante(6, 11, 6, 50)),
            avisos.map { it.instante },
        )
    }

    @Test
    fun `el resumen va a la proxima hora configurada y se puede desactivar (CP-2_27)`() {
        assertEquals(listOf(instante(31, 10, 7)), planificar().de(TipoAviso.RESUMEN).map { it.instante })
        assertTrue(planificar(ajustes = AjustesAvisos(resumen = false)).de(TipoAviso.RESUMEN).isEmpty())
    }

    @Test
    fun `desactivar los avisos de actividad no quita el del dia anterior`() {
        val avisos = planificar(listOf(examen), AjustesAvisos(actividades = false), momento = ahora.plusDays(1))

        assertTrue(avisos.de(TipoAviso.ACTIVIDAD).isEmpty())
        assertEquals(1, avisos.de(TipoAviso.DIA_ANTERIOR).size)
    }

    @Test
    fun `con prioridad todos los avisos usan la alarma del sistema, sin prioridad la exacta (CP-2_28)`() {
        val ajustes = AjustesAvisos(clases = true)
        val conPrioridad = planificar(listOf(examen), ajustes).filter { it.tipo != TipoAviso.MANTENIMIENTO }
        val sinPrioridad = planificar(listOf(examen), ajustes.copy(prioridadAlarma = false)).filter { it.tipo != TipoAviso.MANTENIMIENTO }

        assertEquals(setOf(TipoAviso.DIA_ANTERIOR, TipoAviso.CLASE, TipoAviso.RESUMEN), conPrioridad.map { it.tipo }.toSet())
        assertTrue(conPrioridad.all { elegirModo(it, puedeExactas = true) == ModoAlarma.RELOJ })
        assertTrue(sinPrioridad.all { elegirModo(it, puedeExactas = true) == ModoAlarma.EXACTA })
    }

    @Test
    fun `sin permiso de alarmas exactas todo va como alarma inexacta (CP-2_29)`() {
        assertTrue(planificar(listOf(examen)).all { elegirModo(it, puedeExactas = false) == ModoAlarma.INEXACTA })
    }

    @Test
    fun `siempre hay un mantenimiento diario silencioso a las 3_00`() {
        val mantenimiento = planificar(ajustes = AjustesAvisos(resumen = false, actividades = false)).single()

        assertEquals(TipoAviso.MANTENIMIENTO, mantenimiento.tipo)
        assertEquals(instante(31, 10, 3), mantenimiento.instante)
        assertEquals(ModoAlarma.INEXACTA, elegirModo(mantenimiento, puedeExactas = true))
    }

    @Test
    fun `los avisos salen ordenados por hora`() {
        val avisos = planificar(listOf(examen), AjustesAvisos(clases = true))

        assertEquals(avisos.sortedBy { it.instante }, avisos)
        assertTrue(avisos.all { it.instante.isAfter(ahora.toInstant()) })
        assertTrue(avisos.all { LocalDate.ofInstant(it.instante, ZoneOffset.UTC) <= HOY.plusDays(7) })
    }

    @Test
    fun `el gym avisa con los minutos de los ajustes y no usa el aviso de la actividad (CP-5_16)`() {
        val gym = Actividad(id = 80, titulo = "Pierna", tipo = TipoActividad.GYM, fecha = HOY, hora = hora(18), avisoMinutosAntes = 120)

        val avisos = planificar(listOf(gym), AjustesAvisos(minutosGym = 15))
        assertEquals(listOf(instante(30, 10, 17, 45)), avisos.de(TipoAviso.GYM).map { it.instante })
        assertTrue(avisos.de(TipoAviso.ACTIVIDAD).isEmpty())

        assertTrue(planificar(listOf(gym), AjustesAvisos(gym = false)).de(TipoAviso.GYM).isEmpty())
        assertTrue(planificar(listOf(gym.copy(estado = EstadoActividad.SALTADA))).de(TipoAviso.GYM).isEmpty())
    }

    @Test
    fun `el fin del descanso es una alarma del sistema aunque los avisos esten apagados (RF-29)`() {
        val fin = ahora.toInstant().plusSeconds(90)
        val apagados = AjustesAvisos(actividades = false, diaAnterior = false, resumen = false, gym = false, prioridadAlarma = false)

        val descanso = PlanificadorAvisos.planificar(emptyList(), emptyList(), apagados, ahora, descanso = 77L to fin).de(TipoAviso.DESCANSO).single()

        assertEquals(fin, descanso.instante)
        assertEquals(775, descanso.codigo)
        assertEquals(ModoAlarma.RELOJ, elegirModo(descanso, puedeExactas = true))
        assertTrue(PlanificadorAvisos.planificar(emptyList(), emptyList(), apagados, ahora, descanso = 77L to ahora.toInstant().minusSeconds(1)).de(TipoAviso.DESCANSO).isEmpty())
    }
}
