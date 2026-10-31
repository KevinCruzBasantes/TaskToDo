package com.calendar.tasktodo.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.ajustes.Frecuencia
import com.calendar.tasktodo.data.respaldo.ArchivoRespaldo
import com.calendar.tasktodo.data.respaldo.LecturaRespaldo
import com.calendar.tasktodo.data.respaldo.RespaldoRepository
import com.calendar.tasktodo.data.respaldo.ResultadoRespaldo
import com.calendar.tasktodo.data.respaldo.ResultadoRestauracion
import com.calendar.tasktodo.data.respaldo.TipoRespaldo
import com.calendar.tasktodo.ui.contenedor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** Qué estaba haciendo el usuario cuando hubo que pedirle la carpeta, para seguir después. */
enum class Pendiente { RESPALDAR, ABRIR_RESTAURAR, CONFIRMAR_RESTAURAR, ACTIVAR_AUTOMATICO }

data class RespaldoUiState(
    val cargando: Boolean = true,
    val nombreCarpeta: String? = null,
    val carpetaDisponible: Boolean = false,
    val ultimo: Instant? = null,
    val diasSinRespaldo: Long? = null,
    val automatico: Boolean = false,
    val frecuencia: Frecuencia = Frecuencia.SEMANAL,
    val ocupado: Boolean = false,
    /** La pantalla debe abrir el selector de carpetas. */
    val pedirCarpeta: Boolean = false,
    /** Lista para elegir qué restaurar; nula si el diálogo está cerrado. */
    val archivos: List<ArchivoRespaldo>? = null,
    /** Archivo leído que espera confirmación. */
    val lectura: LecturaRespaldo? = null,
    val mensaje: String? = null,
    val restaurado: Boolean = false,
)

/** Carpeta, respaldar, restaurar y respaldo automático (CU-21 a CU-23). */
class RespaldoViewModel(private val respaldo: RespaldoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RespaldoUiState())
    val estado: StateFlow<RespaldoUiState> = _estado.asStateFlow()

    private var pendiente: Pendiente? = null

    init {
        viewModelScope.launch {
            respaldo.estado.collect { r ->
                _estado.update {
                    it.copy(
                        ultimo = r.ultimo,
                        diasSinRespaldo = respaldo.diasSinRespaldo(r.ultimo),
                        automatico = r.automatico,
                        frecuencia = r.frecuencia,
                    )
                }
                refrescarCarpeta()
            }
        }
    }

    private suspend fun refrescarCarpeta() {
        val disponible = respaldo.carpetaDisponible()
        val nombre = if (disponible) respaldo.nombreCarpeta() else null
        _estado.update { it.copy(cargando = false, carpetaDisponible = disponible, nombreCarpeta = nombre) }
    }

    private fun pedirCarpeta(para: Pendiente) {
        pendiente = para
        _estado.update { it.copy(pedirCarpeta = true, ocupado = false) }
    }

    /** La pantalla ya abrió el selector. */
    fun carpetaPedida() = _estado.update { it.copy(pedirCarpeta = false) }

    /** "Elegir carpeta" o "Cambiar": no queda nada pendiente después (CU-23 3a). */
    fun elegirCarpeta() {
        pendiente = null
        _estado.update { it.copy(pedirCarpeta = true) }
    }

    fun carpetaCancelada() {
        pendiente = null
    }

    /** Registra la carpeta y sigue con lo que el usuario estaba haciendo (diagramas de comportamiento §2.1). */
    fun registrarCarpeta(uri: String) {
        viewModelScope.launch {
            respaldo.registrarCarpeta(uri)
            refrescarCarpeta()
            when (pendiente.also { pendiente = null }) {
                Pendiente.RESPALDAR -> respaldarAhora()
                Pendiente.ABRIR_RESTAURAR -> abrirRestaurar()
                Pendiente.CONFIRMAR_RESTAURAR -> confirmarRestaurar()
                Pendiente.ACTIVAR_AUTOMATICO -> cambiarAutomatico(true)
                null -> _estado.update { it.copy(mensaje = "Carpeta de respaldos registrada") }
            }
        }
    }

    /** Un toque, sin selector (CU-21). */
    fun respaldarAhora() {
        _estado.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            when (val resultado = respaldo.respaldar(TipoRespaldo.MANUAL)) {
                is ResultadoRespaldo.Exito -> _estado.update { it.copy(ocupado = false, mensaje = "Respaldo creado") }
                ResultadoRespaldo.SinCarpeta -> pedirCarpeta(Pendiente.RESPALDAR)
                is ResultadoRespaldo.Error -> _estado.update { it.copy(ocupado = false, mensaje = "No se pudo guardar: ${resultado.mensaje}") }
            }
        }
    }

    /** Copia en otra ubicación, por ejemplo Google Drive (CU-21 1a). */
    fun copiarA(uri: String) {
        _estado.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            val mensaje = when (val resultado = respaldo.copiarA(uri)) {
                is ResultadoRespaldo.Error -> "No se pudo guardar la copia: ${resultado.mensaje}"
                else -> "Copia guardada"
            }
            _estado.update { it.copy(ocupado = false, mensaje = mensaje) }
        }
    }

    fun abrirRestaurar() {
        viewModelScope.launch {
            if (!respaldo.carpetaDisponible()) {
                pedirCarpeta(Pendiente.ABRIR_RESTAURAR)
                return@launch
            }
            _estado.update { it.copy(archivos = respaldo.listar()) }
        }
    }

    fun cerrarLista() = _estado.update { it.copy(archivos = null) }

    /** Un archivo de la lista o de otra ubicación: se lee y se valida antes de confirmar (CU-22 4). */
    fun elegirArchivo(uri: String) {
        _estado.update { it.copy(archivos = null, ocupado = true) }
        viewModelScope.launch {
            val lectura = respaldo.leer(uri)
            _estado.update { it.copy(ocupado = false, lectura = lectura) }
        }
    }

    fun cancelarLectura() = _estado.update { it.copy(lectura = null) }

    fun confirmarRestaurar() {
        val lectura = _estado.value.lectura as? LecturaRespaldo.Valido ?: return
        _estado.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            val resultado = respaldo.restaurar(lectura.contenido)
            if (resultado == ResultadoRestauracion.SinCarpeta) {
                pedirCarpeta(Pendiente.CONFIRMAR_RESTAURAR)
                return@launch
            }
            _estado.update {
                it.copy(
                    ocupado = false,
                    lectura = null,
                    restaurado = resultado == ResultadoRestauracion.Exito,
                    mensaje = when (resultado) {
                        ResultadoRestauracion.Exito -> "Respaldo restaurado"
                        is ResultadoRestauracion.SinSeguridad -> "No se pudo respaldar lo actual, así que no se cambió nada"
                        is ResultadoRestauracion.Error -> "No se pudo restaurar; tus datos siguen como estaban"
                        ResultadoRestauracion.SinCarpeta -> null
                    },
                )
            }
        }
    }

    fun cambiarAutomatico(activado: Boolean) {
        viewModelScope.launch {
            if (activado && !respaldo.carpetaDisponible()) {
                pedirCarpeta(Pendiente.ACTIVAR_AUTOMATICO)
                return@launch
            }
            respaldo.configurarAutomatico(activado, _estado.value.frecuencia)
        }
    }

    fun cambiarFrecuencia(frecuencia: Frecuencia) {
        viewModelScope.launch { respaldo.configurarAutomatico(_estado.value.automatico, frecuencia) }
    }

    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer { RespaldoViewModel(contenedor().respaldoRepository) }
        }
    }
}
