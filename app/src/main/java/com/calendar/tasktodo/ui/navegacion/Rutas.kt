package com.calendar.tasktodo.ui.navegacion

import kotlinx.serialization.Serializable

// Rutas tipadas (diseño de interfaz §2). Las fechas viajan como texto ISO.

@Serializable
data object Bienvenida

@Serializable
data class Dia(val fecha: String? = null)

/** En el incremento 1 solo tiene la pestaña Horario; Semana y Mes llegan en el incremento 4. */
@Serializable
data object Calendario

@Serializable
data object Ajustes

@Serializable
data object Materias

@Serializable
data class FormMateria(val id: Long? = null)

@Serializable
data class FormClase(val id: Long? = null, val materiaId: Long? = null)

@Serializable
data class DetalleClase(val id: Long)
