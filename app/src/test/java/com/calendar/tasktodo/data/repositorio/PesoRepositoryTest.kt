package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.db.entidad.RegistroPeso
import com.calendar.tasktodo.prueba.DatosDePrueba
import com.calendar.tasktodo.prueba.DatosDePrueba.HOY
import com.calendar.tasktodo.prueba.PesoFalso
import com.calendar.tasktodo.prueba.ReglaAjustes
import com.calendar.tasktodo.prueba.ReglaDespachadorPrincipal
import com.calendar.tasktodo.ui.gym.PesoViewModel
import com.calendar.tasktodo.ui.gym.textoImc
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Peso e IMC (CU-17). */
class PesoRepositoryTest {

    @get:Rule
    val despachador = ReglaDespachadorPrincipal()

    @get:Rule
    val ajustes = ReglaAjustes()

    private val dao = PesoFalso()
    private val repo by lazy { PesoRepository(dao, ajustes.repositorio) }

    @Test
    fun `72_5 kg con 175 cm es IMC 23_7 Normal (CP-6_01)`() {
        val imc = Imc.calcular(72.5, 175)

        assertEquals("23.7", textoImc(imc))
        assertEquals(CategoriaImc.NORMAL, Imc.categoria(imc))
    }

    @Test
    fun `los limites de las categorias de la OMS (CP-6_02)`() {
        assertEquals(
            listOf(CategoriaImc.BAJO_PESO, CategoriaImc.NORMAL, CategoriaImc.NORMAL, CategoriaImc.SOBREPESO, CategoriaImc.SOBREPESO, CategoriaImc.OBESIDAD),
            listOf(18.49, 18.5, 24.99, 25.0, 29.99, 30.0).map { Imc.categoria(it) },
        )
    }

    @Test
    fun `cambiar la estatura recalcula el IMC de todos los registros (CP-6_03)`() = runTest {
        repo.guardar(RegistroPeso(fecha = HOY, pesoKg = 72.5))
        repo.guardar(RegistroPeso(fecha = HOY.minusDays(7), pesoKg = 73.1))
        repo.guardarEstatura(175)
        assertEquals(listOf("23.7", "23.9"), repo.registros.first().map { textoImc(it.imc!!) })

        assertTrue(repo.guardarEstatura(180))

        assertEquals(listOf("22.4", "22.6"), repo.registros.first().map { textoImc(it.imc!!) })
    }

    @Test
    fun `sin estatura los registros no tienen IMC y se pide la estatura (CP-6_04)`() = runTest {
        repo.guardar(RegistroPeso(fecha = HOY, pesoKg = 72.5))
        val vm = PesoViewModel(repo, DatosDePrueba.reloj)
        val estado = vm.estado.first { !it.cargando }

        assertNull(estado.estaturaCm)
        assertNull(estado.registros.single().imc)

        vm.guardarEstatura("12")
        assertEquals(null, ajustes.repositorio.estaturaCm.first())
        assertFalse(repo.guardarEstatura(300))
    }

    @Test
    fun `un peso de 0 no se guarda y una fecha repetida ofrece reemplazar (CP-6_05)`() = runTest {
        assertEquals(ResultadoPeso.PesoInvalido, repo.guardar(RegistroPeso(fecha = HOY, pesoKg = 0.0)))
        assertTrue(dao.registros.value.isEmpty())

        repo.guardar(RegistroPeso(fecha = HOY, pesoKg = 72.5))
        val repetida = repo.guardar(RegistroPeso(fecha = HOY, pesoKg = 71.0))
        assertTrue(repetida is ResultadoPeso.FechaOcupada)
        assertEquals(listOf(72.5), dao.registros.value.map { it.pesoKg })

        assertEquals(ResultadoPeso.Guardado, repo.guardar(RegistroPeso(fecha = HOY, pesoKg = 71.0), reemplazar = true))
        assertEquals(listOf(71.0), dao.registros.value.map { it.pesoKg })
    }

    @Test
    fun `el formulario no acepta un peso vacio y al editar cambia la fecha`() = runTest {
        val vm = PesoViewModel(repo, DatosDePrueba.reloj)
        vm.nuevo()
        vm.guardar()
        assertEquals("Ingresa un peso mayor que 0", vm.estado.first { !it.cargando }.editor?.error)

        vm.cambiarPeso("72,5")
        vm.guardar()
        val guardado = dao.registros.value.single()
        assertEquals(72.5, guardado.pesoKg, 0.0)
        assertEquals(HOY, guardado.fecha)

        vm.editar(guardado)
        vm.cambiarFecha(HOY.minusDays(1))
        vm.guardar()
        assertEquals(listOf(HOY.minusDays(1)), dao.registros.value.map { it.fecha })
    }
}
