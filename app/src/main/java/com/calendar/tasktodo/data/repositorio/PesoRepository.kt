package com.calendar.tasktodo.data.repositorio

import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.data.db.dao.PesoDao
import com.calendar.tasktodo.data.db.entidad.RegistroPeso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Categorías de la OMS para adultos (CU-17). */
enum class CategoriaImc(val texto: String) {
    BAJO_PESO("Bajo peso"),
    NORMAL("Normal"),
    SOBREPESO("Sobrepeso"),
    OBESIDAD("Obesidad"),
}

object Imc {
    const val ESTATURA_MINIMA = 50
    const val ESTATURA_MAXIMA = 250

    /** `peso_kg / (estatura_cm / 100)²` (modelo de datos §2.2). */
    fun calcular(pesoKg: Double, estaturaCm: Int): Double {
        val metros = estaturaCm / 100.0
        return pesoKg / (metros * metros)
    }

    /** Sobre el valor sin redondear: 24.99 es Normal aunque se muestre 25.0. */
    fun categoria(imc: Double): CategoriaImc = when {
        imc < 18.5 -> CategoriaImc.BAJO_PESO
        imc < 25.0 -> CategoriaImc.NORMAL
        imc < 30.0 -> CategoriaImc.SOBREPESO
        else -> CategoriaImc.OBESIDAD
    }
}

data class RegistroConImc(val registro: RegistroPeso, val imc: Double?)

sealed interface ResultadoPeso {
    data object Guardado : ResultadoPeso
    data object PesoInvalido : ResultadoPeso

    /** Ya hay un registro ese día: se ofrece reemplazarlo (CU-17 6b). */
    data class FechaOcupada(val existente: RegistroPeso) : ResultadoPeso
}

/** Registros de peso e IMC (CU-17). */
class PesoRepository(private val dao: PesoDao, private val ajustes: AjustesRepository) {

    val estaturaCm: Flow<Int?> = ajustes.estaturaCm

    /** Del más reciente al más antiguo, con el IMC calculado con la estatura actual. */
    val registros: Flow<List<RegistroConImc>> = combine(dao.observarTodos(), ajustes.estaturaCm) { lista, estatura ->
        lista.map { RegistroConImc(it, estatura?.let { cm -> Imc.calcular(it.pesoKg, cm) }) }
    }

    suspend fun obtener(id: Long) = dao.obtener(id)

    suspend fun guardar(registro: RegistroPeso, reemplazar: Boolean = false): ResultadoPeso {
        if (!(registro.pesoKg > 0)) return ResultadoPeso.PesoInvalido
        val enEsaFecha = dao.porFecha(registro.fecha)
        if (enEsaFecha != null && enEsaFecha.id != registro.id) {
            if (!reemplazar) return ResultadoPeso.FechaOcupada(enEsaFecha)
            dao.eliminar(enEsaFecha)
        }
        if (registro.id == 0L) dao.insertar(registro) else dao.actualizar(registro)
        return ResultadoPeso.Guardado
    }

    suspend fun eliminar(registro: RegistroPeso) = dao.eliminar(registro)

    /** El IMC de todos los registros se recalcula solo, porque no se guarda (CU-17 2a). */
    suspend fun guardarEstatura(cm: Int): Boolean {
        if (cm !in Imc.ESTATURA_MINIMA..Imc.ESTATURA_MAXIMA) return false
        ajustes.guardarEstatura(cm)
        return true
    }
}
