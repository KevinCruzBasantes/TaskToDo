package com.calendar.tasktodo.prueba

import com.calendar.tasktodo.data.db.dao.GymDao
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.DiaGym
import com.calendar.tasktodo.data.db.entidad.Ejercicio
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.EstadoSesion
import com.calendar.tasktodo.data.db.entidad.Rutina
import com.calendar.tasktodo.data.db.entidad.RutinaConConteo
import com.calendar.tasktodo.data.db.entidad.SerieRealizada
import com.calendar.tasktodo.data.db.entidad.SesionGym
import com.calendar.tasktodo.data.db.entidad.TipoActividad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime

/**
 * El `GymDao` en memoria, con las reglas de Room que importan: la rutina asignada a un día no se
 * borra (RESTRICT), sus ejercicios se borran en cascada y las actividades generadas son únicas por
 * (`dia_gym_id`, `fecha_programada`). Comparte las actividades con [HorarioFalso].
 */
class GymFalso(private val actividades: MutableStateFlow<List<Actividad>>) : GymDao {

    val rutinas = MutableStateFlow<List<Rutina>>(emptyList())
    val ejercicios = MutableStateFlow<List<Ejercicio>>(emptyList())
    val dias = MutableStateFlow<List<DiaGym>>(emptyList())
    val sesiones = MutableStateFlow<List<SesionGym>>(emptyList())
    val series = MutableStateFlow<List<SerieRealizada>>(emptyList())

    private var siguienteId = 1_000L

    override fun observarRutinas() = combine(rutinas, ejercicios) { lista, todos ->
        lista.sortedBy { it.nombre.lowercase() }.map { r -> RutinaConConteo(r, todos.count { it.rutinaId == r.id }) }
    }

    override fun observarRutina(id: Long) = rutinas.map { lista -> lista.firstOrNull { it.id == id } }

    override suspend fun obtenerRutina(id: Long) = rutinas.value.firstOrNull { it.id == id }

    override suspend fun rutinas() = rutinas.value.sortedBy { it.id }

    override suspend fun insertarRutina(rutina: Rutina): Long {
        val id = siguienteId++
        rutinas.update { it + rutina.copy(id = id) }
        return id
    }

    override suspend fun actualizarRutina(rutina: Rutina) = rutinas.update { l -> l.map { if (it.id == rutina.id) rutina else it } }

    override suspend fun eliminarRutina(rutina: Rutina) {
        require(dias.value.none { it.rutinaId == rutina.id }) { "FOREIGN KEY constraint failed (RESTRICT)" }
        ejercicios.update { l -> l.filterNot { it.rutinaId == rutina.id } }
        actividades.update { l -> l.map { if (it.rutinaId == rutina.id) it.copy(rutinaId = null) else it } }
        sesiones.update { l -> l.map { if (it.rutinaId == rutina.id) it.copy(rutinaId = null) else it } }
        rutinas.update { l -> l.filterNot { it.id == rutina.id } }
    }

    override fun observarEjercicios(rutinaId: Long) = ejercicios.map { l -> l.filter { it.rutinaId == rutinaId }.sortedBy { it.orden } }

    override suspend fun ejercicios(rutinaId: Long) = ejercicios.value.filter { it.rutinaId == rutinaId }.sortedBy { it.orden }

    override suspend fun obtenerEjercicio(id: Long) = ejercicios.value.firstOrNull { it.id == id }

    override suspend fun insertarEjercicio(ejercicio: Ejercicio): Long {
        val id = siguienteId++
        ejercicios.update { it + ejercicio.copy(id = id) }
        return id
    }

    override suspend fun actualizarEjercicios(ejercicios: List<Ejercicio>) {
        val porId = ejercicios.associateBy { it.id }
        this.ejercicios.update { l -> l.map { porId[it.id] ?: it } }
    }

    override suspend fun eliminarEjercicio(ejercicio: Ejercicio) {
        series.update { l -> l.map { if (it.ejercicioId == ejercicio.id) it.copy(ejercicioId = null) else it } }
        ejercicios.update { l -> l.filterNot { it.id == ejercicio.id } }
    }

    override fun observarDias() = dias.map { l -> l.sortedBy { it.diaSemana } }

    override suspend fun dias() = dias.value.sortedBy { it.diaSemana }

    override suspend fun diasDeRutina(rutinaId: Long) = dias.value.filter { it.rutinaId == rutinaId }

    override suspend fun insertarDia(dia: DiaGym): Long {
        val id = siguienteId++
        dias.update { it + dia.copy(id = id) }
        return id
    }

    override suspend fun actualizarDia(dia: DiaGym) = dias.update { l -> l.map { if (it.id == dia.id) dia else it } }

    override suspend fun eliminarDia(dia: DiaGym) {
        actividades.update { l -> l.map { if (it.diaGymId == dia.id) it.copy(diaGymId = null) else it } }
        dias.update { l -> l.filterNot { it.id == dia.id } }
    }

    override suspend fun insertarGenerada(actividad: Actividad): Long {
        val existe = actividades.value.any { it.diaGymId == actividad.diaGymId && it.fechaProgramada == actividad.fechaProgramada }
        if (existe) return -1
        val id = siguienteId++
        actividades.update { it + actividad.copy(id = id) }
        return id
    }

    override suspend fun borrarGeneradasFuturas(desde: LocalDate) = actividades.update { l ->
        l.filterNot { it.tipo == TipoActividad.GYM && it.estado == EstadoActividad.PENDIENTE && !it.fecha.isBefore(desde) && it.fecha == it.fechaProgramada }
    }

    override suspend fun borrarFuturasDeRutina(rutinaId: Long, desde: LocalDate) = actividades.update { l ->
        l.filterNot { it.tipo == TipoActividad.GYM && it.estado == EstadoActividad.PENDIENTE && !it.fecha.isBefore(desde) && it.rutinaId == rutinaId }
    }

    override suspend fun renombrarActividadesDeRutina(rutinaId: Long, nombre: String) = actividades.update { l ->
        l.map { if (it.tipo == TipoActividad.GYM && it.estado == EstadoActividad.PENDIENTE && it.rutinaId == rutinaId) it.copy(titulo = nombre) else it }
    }

    override suspend fun moverActividad(id: Long, fecha: LocalDate, hora: LocalTime) =
        actividades.update { l -> l.map { if (it.id == id) it.copy(fecha = fecha, hora = hora) else it } }

    override fun observarProximaGym(hoy: LocalDate) = actividades.map { l ->
        l.filter { it.tipo == TipoActividad.GYM && it.estado == EstadoActividad.PENDIENTE && !it.fecha.isBefore(hoy) }
            .sortedWith(compareBy({ it.fecha }, { it.hora })).firstOrNull()
    }

    override fun observarGymEntre(desde: LocalDate, hasta: LocalDate) = actividades.map { l ->
        l.filter { it.tipo == TipoActividad.GYM && !it.fecha.isBefore(desde) && !it.fecha.isAfter(hasta) }
    }

    override fun observarEnCurso() = sesiones.map { l -> l.firstOrNull { it.estado == EstadoSesion.EN_CURSO } }

    override suspend fun enCurso() = sesiones.value.firstOrNull { it.estado == EstadoSesion.EN_CURSO }

    override fun observarSesion(id: Long) = sesiones.map { l -> l.firstOrNull { it.id == id } }

    override suspend fun obtenerSesion(id: Long) = sesiones.value.firstOrNull { it.id == id }

    override suspend fun sesionDeActividad(actividadId: Long) = sesiones.value.firstOrNull { it.actividadId == actividadId }

    override suspend fun insertarSesion(sesion: SesionGym): Long {
        val id = siguienteId++
        sesiones.update { it + sesion.copy(id = id) }
        return id
    }

    override suspend fun actualizarSesion(sesion: SesionGym) = sesiones.update { l -> l.map { if (it.id == sesion.id) sesion else it } }

    override suspend fun eliminarSesion(sesion: SesionGym) {
        series.update { l -> l.filterNot { it.sesionId == sesion.id } }
        sesiones.update { l -> l.filterNot { it.id == sesion.id } }
    }

    override fun observarSeries(sesionId: Long) = series.map { l -> l.filter { it.sesionId == sesionId }.sortedBy { it.completadaEn } }

    override suspend fun series(sesionId: Long) = series.value.filter { it.sesionId == sesionId }.sortedBy { it.completadaEn }

    override suspend fun insertarSerie(serie: SerieRealizada): Long {
        val id = siguienteId++
        series.update { it + serie.copy(id = id) }
        return id
    }

    override suspend fun eliminarSerie(serie: SerieRealizada) = series.update { l -> l.filterNot { it.id == serie.id } }

    override suspend fun todosLosEjercicios() = ejercicios.value.sortedBy { it.id }

    override suspend fun todasLasSesiones() = sesiones.value.sortedBy { it.id }

    override suspend fun todasLasSeries() = series.value.sortedBy { it.id }

    override suspend fun insertarRutinas(rutinas: List<Rutina>) = this.rutinas.update { it + rutinas }

    override suspend fun insertarEjercicios(ejercicios: List<Ejercicio>) = this.ejercicios.update { it + ejercicios }

    override suspend fun insertarDias(dias: List<DiaGym>) = this.dias.update { it + dias }

    override suspend fun insertarSesiones(sesiones: List<SesionGym>) = this.sesiones.update { it + sesiones }

    override suspend fun insertarSeries(series: List<SerieRealizada>) = this.series.update { it + series }

    override suspend fun vaciarSeries() {
        series.value = emptyList()
    }

    override suspend fun vaciarSesiones() {
        sesiones.value = emptyList()
    }

    override suspend fun vaciarDias() {
        dias.value = emptyList()
    }

    override suspend fun vaciarEjercicios() {
        ejercicios.value = emptyList()
    }

    override suspend fun vaciarRutinas() {
        rutinas.value = emptyList()
    }

    override fun observarTerminadas() = sesiones.map { l -> l.filter { it.estado == EstadoSesion.TERMINADA }.sortedBy { it.inicio } }

    override fun observarSeriesTerminadas() = combine(sesiones, series) { s, l ->
        val terminadas = s.filter { it.estado == EstadoSesion.TERMINADA }.map { it.id }.toSet()
        l.filter { it.sesionId in terminadas && it.ejercicioId != null }
    }

    override suspend fun ultimaSerie(ejercicioId: Long, numero: Int): SerieRealizada? {
        val terminadas = sesiones.value.filter { it.estado == EstadoSesion.TERMINADA }.associateBy { it.id }
        return series.value
            .filter { it.ejercicioId == ejercicioId && it.numeroSerie == numero && it.sesionId in terminadas }
            .maxByOrNull { terminadas.getValue(it.sesionId).inicio }
    }
}
