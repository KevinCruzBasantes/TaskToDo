package com.calendar.tasktodo.ui.componentes

/**
 * Los 12 colores que se pueden elegir para una materia. Tonos medios, legibles junto al texto
 * en tema claro y oscuro (diseño de interfaz §1).
 */
object PaletaMaterias {

    data class ColorMateria(val argb: Int, val nombre: String)

    val colores: List<ColorMateria> = listOf(
        ColorMateria(0xFF1E88E5.toInt(), "Azul"),
        ColorMateria(0xFF43A047.toInt(), "Verde"),
        ColorMateria(0xFFFB8C00.toInt(), "Naranja"),
        ColorMateria(0xFFE53935.toInt(), "Rojo"),
        ColorMateria(0xFF8E24AA.toInt(), "Morado"),
        ColorMateria(0xFF00ACC1.toInt(), "Celeste"),
        ColorMateria(0xFFF9A825.toInt(), "Ámbar"),
        ColorMateria(0xFF6D4C41.toInt(), "Café"),
        ColorMateria(0xFFD81B60.toInt(), "Rosa"),
        ColorMateria(0xFF3949AB.toInt(), "Índigo"),
        ColorMateria(0xFF00897B.toInt(), "Turquesa"),
        ColorMateria(0xFF7CB342.toInt(), "Lima"),
    )

    /** El primer color que ninguna materia usa todavía, o el siguiente en la rueda si se usan todos. */
    fun siguienteLibre(usados: Collection<Int>): Int =
        colores.firstOrNull { it.argb !in usados }?.argb ?: colores[usados.size % colores.size].argb
}
