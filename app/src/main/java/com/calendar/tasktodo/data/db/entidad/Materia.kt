package com.calendar.tasktodo.data.db.entidad

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "materia",
    indices = [Index(value = ["nombre"], unique = true)],
)
data class Materia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Único sin distinguir mayúsculas (CU-02 5b); el repositorio lo valida también con tildes. */
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nombre: String,
    /** Color ARGB de la paleta de materias. */
    val color: Int,
)

/** Materia con la cantidad de clases que tiene, para listas y confirmaciones. */
data class MateriaConConteo(
    @Embedded val materia: Materia,
    @ColumnInfo(name = "clases") val clases: Int,
)
