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

@Serializable
data object Actividades

@Serializable
data class FormActividad(val id: Long? = null, val fecha: String? = null)

@Serializable
data class DetalleActividad(val id: Long)

@Serializable
data object AjustesAvisos

@Serializable
data object GuiaMiui

/** Desde la bienvenida, al restaurar se va directo al día (CU-01 2a). */
@Serializable
data class Respaldo(val desdeBienvenida: Boolean = false)

/** Un día abierto desde la semana del calendario (RF-36); vuelve al calendario con atrás. */
@Serializable
data class DiaDesdeCalendario(val fecha: String)

@Serializable
data object Gym

@Serializable
data object Rutinas

@Serializable
data class FormRutina(val id: Long? = null)

@Serializable
data class DetalleRutina(val id: Long)

@Serializable
data class FormEjercicio(val rutinaId: Long, val id: Long? = null)

@Serializable
data object Programacion

/** La sesión de gym de una actividad (CU-16). */
@Serializable
data class Sesion(val actividadId: Long)

@Serializable
data object Peso

@Serializable
data object Estadisticas
