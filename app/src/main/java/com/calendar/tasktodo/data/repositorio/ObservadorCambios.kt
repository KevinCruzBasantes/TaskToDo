package com.calendar.tasktodo.data.repositorio

/**
 * Lo avisan los repositorios cuando cambia algo que puede tener avisos, para que se vuelvan a
 * calcular (arquitectura §4.2).
 */
fun interface ObservadorCambios {
    suspend fun huboCambios()

    companion object {
        val NINGUNO = ObservadorCambios {}
    }
}
