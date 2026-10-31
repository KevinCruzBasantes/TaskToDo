package com.calendar.tasktodo.data.respaldo

import com.calendar.tasktodo.data.ajustes.AjustesRepository
import com.calendar.tasktodo.data.ajustes.Frecuencia
import com.calendar.tasktodo.data.db.AgendaDatabase
import com.calendar.tasktodo.data.db.dao.ActividadDao
import com.calendar.tasktodo.data.db.dao.ClaseDao
import com.calendar.tasktodo.data.db.dao.GymDao
import com.calendar.tasktodo.data.db.dao.PesoDao
import com.calendar.tasktodo.data.db.dao.MateriaDao
import com.calendar.tasktodo.data.repositorio.ObservadorCambios
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Prefijo de cada archivo según su origen (arquitectura §4.4). Solo los automáticos y de seguridad rotan. */
enum class TipoRespaldo(val prefijo: String, val conservar: Int?) {
    MANUAL("agenda-manual-", null),
    AUTOMATICO("agenda-auto-", 5),
    SEGURIDAD("agenda-seguridad-", 5),
}

data class ArchivoRespaldo(val nombre: String, val uri: String, val tipo: TipoRespaldo) {
    /** La fecha va en el nombre (`agenda-auto-2026-11-07-0300.json`), así se ordena sin leerlo. */
    val momento: LocalDateTime? = runCatching {
        LocalDateTime.parse(nombre.removePrefix(tipo.prefijo).take(15), FORMATO_NOMBRE)
    }.getOrNull()
}

/** Acceso a la carpeta registrada y a archivos sueltos. La versión real usa el Storage Access Framework. */
interface AlmacenRespaldos {
    /** Conserva el permiso sobre la carpeta para que sobreviva a reinicios. */
    fun registrar(carpeta: String)

    /** La carpeta existe y la app conserva el permiso de escritura. */
    fun disponible(carpeta: String): Boolean

    fun nombre(carpeta: String): String?

    /** Crea un archivo nuevo en la carpeta. Lanza `IOException` si no puede. */
    fun escribir(carpeta: String, nombre: String, contenido: String)

    /** Archivos de la carpeta como (nombre, uri). */
    fun listar(carpeta: String): List<Pair<String, String>>

    fun borrar(uri: String)

    fun leer(uri: String): String

    fun escribirEn(uri: String, contenido: String)
}

/** Programa el respaldo automático. La versión real usa WorkManager. */
interface PlanificadorRespaldo {
    fun programar(frecuencia: Frecuencia)

    fun cancelar()

    /** Encola un respaldo automático ya, cuando quedó atrasado (CU-24 1b). */
    fun ejecutarAhora()
}

sealed interface ResultadoRespaldo {
    data class Exito(val nombre: String) : ResultadoRespaldo

    /** No hay carpeta registrada o se perdió el permiso: hay que registrarla (CU-21 2a). */
    data object SinCarpeta : ResultadoRespaldo

    data class Error(val mensaje: String) : ResultadoRespaldo
}

data class ResumenRespaldo(
    val fecha: Instant,
    val versionApp: String,
    val versionBaseDatos: Int,
    val materias: Int,
    val clases: Int,
    val actividades: Int,
)

sealed interface LecturaRespaldo {
    data class Valido(val resumen: ResumenRespaldo, val contenido: String) : LecturaRespaldo

    data object Invalido : LecturaRespaldo

    /** De una versión de la base de datos más nueva que la app (CU-22 4b). */
    data class MasNuevo(val version: Int) : LecturaRespaldo
}

sealed interface ResultadoRestauracion {
    data object Exito : ResultadoRestauracion

    data object SinCarpeta : ResultadoRestauracion

    /** El respaldo de seguridad de los datos actuales falló: no se tocó nada (CU-22 7a). */
    data class SinSeguridad(val mensaje: String) : ResultadoRestauracion

    /** Falló al reemplazar: la transacción se deshizo y los datos siguen como estaban (CU-22 8a). */
    data class Error(val mensaje: String) : ResultadoRestauracion
}

private val FORMATO_NOMBRE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")

/** Respaldo y restauración (CU-21 a CU-24, diagramas de comportamiento §2). */
class RespaldoRepository(
    private val materias: MateriaDao,
    private val clases: ClaseDao,
    private val actividades: ActividadDao,
    private val gym: GymDao,
    private val peso: PesoDao,
    private val ajustes: AjustesRepository,
    private val almacen: AlmacenRespaldos,
    private val planificador: PlanificadorRespaldo,
    /** `withTransaction` de Room; en las pruebas, solo ejecuta el bloque. */
    private val transaccion: suspend (suspend () -> Unit) -> Unit,
    private val reloj: Clock,
    private val versionApp: String,
    private val observador: ObservadorCambios = ObservadorCambios.NINGUNO,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    val estado = ajustes.respaldo

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Una foto coherente de todos los datos: se leen en una sola transacción. */
    suspend fun generar(): String {
        lateinit var dto: RespaldoDto
        val respaldo = ajustes.respaldo.first()
        val avisos = ajustes.avisos.first()
        val finGym = ajustes.finProgramacionGym.first()
        val estatura = ajustes.estaturaCm.first()
        transaccion {
            dto = RespaldoDto(
                versionBaseDatos = AgendaDatabase.VERSION,
                versionApp = versionApp,
                fecha = Instant.now(reloj).toString(),
                ajustes = avisos.aDto(respaldo.automatico, respaldo.frecuencia, finGym, estatura),
                materias = materias.todas().map { it.aDto() },
                clases = clases.todas().map { it.aDto() },
                actividades = actividades.todas().map { it.aDto() },
                rutinas = gym.rutinas().map { it.aDto() },
                ejercicios = gym.todosLosEjercicios().map { it.aDto() },
                diasGym = gym.dias().map { it.aDto() },
                sesiones = gym.todasLasSesiones().map { it.aDto() },
                series = gym.todasLasSeries().map { it.aDto() },
                registrosPeso = peso.todos().map { it.aDto() },
            )
        }
        return json.encodeToString(RespaldoDto.serializer(), dto)
    }

    suspend fun carpetaDisponible(): Boolean = ajustes.respaldo.first().carpeta?.let { withContext(io) { almacen.disponible(it) } } ?: false

    suspend fun nombreCarpeta(): String? = ajustes.respaldo.first().carpeta?.let { withContext(io) { almacen.nombre(it) } }

    suspend fun registrarCarpeta(uri: String) {
        withContext(io) { almacen.registrar(uri) }
        ajustes.guardarCarpeta(uri)
    }

    /** Un respaldo en la carpeta registrada; los automáticos y de seguridad conservan solo los últimos 5. */
    suspend fun respaldar(tipo: TipoRespaldo): ResultadoRespaldo {
        val carpeta = ajustes.respaldo.first().carpeta
        if (carpeta == null || !withContext(io) { almacen.disponible(carpeta) }) return ResultadoRespaldo.SinCarpeta
        val contenido = generar()
        val nombre = tipo.prefijo + LocalDateTime.now(reloj).format(FORMATO_NOMBRE) + ".json"
        return try {
            withContext(io) {
                almacen.escribir(carpeta, nombre, contenido)
                tipo.conservar?.let { limite -> rotar(carpeta, tipo, limite) }
            }
            ajustes.guardarUltimoRespaldo(Instant.now(reloj))
            ResultadoRespaldo.Exito(nombre)
        } catch (e: Exception) {
            ResultadoRespaldo.Error(e.message ?: "No se pudo guardar el respaldo")
        }
    }

    /** Una copia en otra ubicación, por ejemplo Google Drive (CU-21 1a). */
    suspend fun copiarA(uri: String): ResultadoRespaldo {
        val contenido = generar()
        return try {
            withContext(io) { almacen.escribirEn(uri, contenido) }
            ajustes.guardarUltimoRespaldo(Instant.now(reloj))
            ResultadoRespaldo.Exito(uri)
        } catch (e: Exception) {
            ResultadoRespaldo.Error(e.message ?: "No se pudo guardar la copia")
        }
    }

    /** Los respaldos de la carpeta, del más reciente al más antiguo (CU-22). */
    suspend fun listar(): List<ArchivoRespaldo> {
        val carpeta = ajustes.respaldo.first().carpeta ?: return emptyList()
        return withContext(io) { listarEn(carpeta) }
    }

    private fun listarEn(carpeta: String): List<ArchivoRespaldo> =
        almacen.listar(carpeta)
            .filter { (nombre, _) -> nombre.endsWith(".json") }
            .mapNotNull { (nombre, uri) ->
                TipoRespaldo.entries.firstOrNull { nombre.startsWith(it.prefijo) }?.let { ArchivoRespaldo(nombre, uri, it) }
            }
            .sortedByDescending { it.nombre.removePrefix(it.tipo.prefijo) }

    private fun rotar(carpeta: String, tipo: TipoRespaldo, conservar: Int) {
        listarEn(carpeta).filter { it.tipo == tipo }.drop(conservar).forEach { almacen.borrar(it.uri) }
    }

    /** Lee y valida un archivo antes de pedir confirmación (CU-22 4). */
    suspend fun leer(uri: String): LecturaRespaldo {
        val contenido = try {
            withContext(io) { almacen.leer(uri) }
        } catch (_: Exception) {
            return LecturaRespaldo.Invalido
        }
        return validar(contenido)
    }

    fun validar(contenido: String): LecturaRespaldo {
        val dto = try {
            json.decodeFromString(RespaldoDto.serializer(), contenido)
        } catch (_: SerializationException) {
            return LecturaRespaldo.Invalido
        } catch (_: IllegalArgumentException) {
            return LecturaRespaldo.Invalido
        }
        if (dto.versionBaseDatos > AgendaDatabase.VERSION) return LecturaRespaldo.MasNuevo(dto.versionBaseDatos)
        val fecha = runCatching { Instant.parse(dto.fecha) }.getOrNull() ?: return LecturaRespaldo.Invalido
        return LecturaRespaldo.Valido(
            ResumenRespaldo(fecha, dto.versionApp, dto.versionBaseDatos, dto.materias.size, dto.clases.size, dto.actividades.size),
            contenido,
        )
    }

    /**
     * Reemplaza todos los datos por los del respaldo: primero guarda un respaldo de seguridad de
     * lo actual y después reemplaza todo en una transacción (diagramas de comportamiento §2.3).
     */
    suspend fun restaurar(contenido: String): ResultadoRestauracion {
        val dto = try {
            json.decodeFromString(RespaldoDto.serializer(), contenido)
        } catch (e: Exception) {
            return ResultadoRestauracion.Error("El archivo no es un respaldo válido")
        }
        if (dto.versionBaseDatos > AgendaDatabase.VERSION) {
            return ResultadoRestauracion.Error("Actualiza la app para restaurar este respaldo")
        }
        when (val seguridad = respaldar(TipoRespaldo.SEGURIDAD)) {
            ResultadoRespaldo.SinCarpeta -> return ResultadoRestauracion.SinCarpeta
            is ResultadoRespaldo.Error -> return ResultadoRestauracion.SinSeguridad(seguridad.mensaje)
            is ResultadoRespaldo.Exito -> Unit
        }
        try {
            val nuevasMaterias = dto.materias.map { it.aEntidad() }
            val nuevasClases = dto.clases.map { it.aEntidad() }
            val nuevasActividades = dto.actividades.map { it.aEntidad() }
            val nuevasRutinas = dto.rutinas.map { it.aEntidad() }
            val nuevosEjercicios = dto.ejercicios.map { it.aEntidad() }
            val nuevosDias = dto.diasGym.map { it.aEntidad() }
            val nuevasSesiones = dto.sesiones.map { it.aEntidad() }
            val nuevasSeries = dto.series.map { it.aEntidad() }
            val nuevosPesos = dto.registrosPeso.map { it.aEntidad() }
            transaccion {
                // Primero los hijos y después los padres al borrar; al revés al insertar (diagramas §2.3)
                peso.vaciar()
                gym.vaciarSeries()
                gym.vaciarSesiones()
                actividades.eliminarTodas()
                gym.vaciarDias()
                gym.vaciarEjercicios()
                clases.eliminarTodas()
                gym.vaciarRutinas()
                materias.eliminarTodas()
                materias.insertarTodas(nuevasMaterias)
                clases.insertarTodas(nuevasClases)
                gym.insertarRutinas(nuevasRutinas)
                gym.insertarEjercicios(nuevosEjercicios)
                gym.insertarDias(nuevosDias)
                actividades.insertarTodas(nuevasActividades)
                gym.insertarSesiones(nuevasSesiones)
                gym.insertarSeries(nuevasSeries)
                peso.insertarTodos(nuevosPesos)
            }
        } catch (e: Exception) {
            return ResultadoRestauracion.Error(e.message ?: "No se pudo restaurar")
        }
        ajustes.guardarAvisos(dto.ajustes.avisos())
        ajustes.guardarFinProgramacionGym(dto.ajustes.finProgramacionGym?.let(LocalDate::parse))
        ajustes.guardarEstatura(dto.ajustes.estaturaCm)
        val frecuencia = Frecuencia.entries.firstOrNull { it.name == dto.ajustes.frecuenciaRespaldo } ?: Frecuencia.SEMANAL
        configurarAutomatico(dto.ajustes.respaldoAutomatico, frecuencia)
        observador.huboCambios()
        return ResultadoRestauracion.Exito
    }

    suspend fun configurarAutomatico(activado: Boolean, frecuencia: Frecuencia) {
        ajustes.guardarAutomatico(activado, frecuencia)
        if (activado) planificador.programar(frecuencia) else planificador.cancelar()
    }

    /** Si el automático quedó atrasado, por ejemplo porque MIUI lo impidió, se encola ya (CU-24 1b). */
    suspend fun ponerAlDia() {
        val r = ajustes.respaldo.first()
        if (!r.automatico || r.carpeta == null) return
        val vence = r.ultimo?.plus(Duration.ofDays(r.frecuencia.dias))
        if (vence == null || !vence.isAfter(Instant.now(reloj))) planificador.ejecutarAhora()
    }

    /** Días desde el último respaldo; nulo si nunca hubo uno (RF-47). */
    fun diasSinRespaldo(ultimo: Instant?): Long? = ultimo?.let { Duration.between(it, Instant.now(reloj)).toDays() }

    /** Sin ningún respaldo, o con más de [DIAS_ADVERTENCIA] días desde el último (RF-47). */
    fun necesitaAdvertencia(ultimo: Instant?): Boolean = (diasSinRespaldo(ultimo) ?: Long.MAX_VALUE) > DIAS_ADVERTENCIA

    companion object {
        /** Más de esto sin respaldar muestra la advertencia (RF-47). */
        const val DIAS_ADVERTENCIA = 7L
    }
}
