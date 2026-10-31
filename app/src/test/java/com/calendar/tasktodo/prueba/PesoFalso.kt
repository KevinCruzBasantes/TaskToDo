package com.calendar.tasktodo.prueba

import com.calendar.tasktodo.data.db.dao.PesoDao
import com.calendar.tasktodo.data.db.entidad.RegistroPeso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** El `PesoDao` en memoria, con la fecha única de Room. */
class PesoFalso : PesoDao {

    val registros = MutableStateFlow<List<RegistroPeso>>(emptyList())

    private var siguienteId = 1L

    override fun observarTodos() = registros.map { l -> l.sortedByDescending { it.fecha } }

    override suspend fun obtener(id: Long) = registros.value.firstOrNull { it.id == id }

    override suspend fun porFecha(fecha: LocalDate) = registros.value.firstOrNull { it.fecha == fecha }

    override suspend fun insertar(registro: RegistroPeso): Long {
        require(registros.value.none { it.fecha == registro.fecha }) { "UNIQUE constraint failed: registro_peso.fecha" }
        val id = siguienteId++
        registros.update { it + registro.copy(id = id) }
        return id
    }

    override suspend fun actualizar(registro: RegistroPeso) {
        require(registros.value.none { it.fecha == registro.fecha && it.id != registro.id }) { "UNIQUE constraint failed: registro_peso.fecha" }
        registros.update { l -> l.map { if (it.id == registro.id) registro else it } }
    }

    override suspend fun eliminar(registro: RegistroPeso) = registros.update { l -> l.filterNot { it.id == registro.id } }

    override suspend fun todos() = registros.value.sortedBy { it.id }

    override suspend fun insertarTodos(registros: List<RegistroPeso>) = this.registros.update { it + registros }

    override suspend fun vaciar() {
        registros.value = emptyList()
    }
}
