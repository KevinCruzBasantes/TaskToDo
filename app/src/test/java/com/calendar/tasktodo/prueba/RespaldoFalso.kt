package com.calendar.tasktodo.prueba

import com.calendar.tasktodo.data.ajustes.Frecuencia
import com.calendar.tasktodo.data.respaldo.AlmacenRespaldos
import com.calendar.tasktodo.data.respaldo.PlanificadorRespaldo
import java.io.IOException

/** Una carpeta en memoria: nombre de archivo -> contenido. */
class AlmacenFalso : AlmacenRespaldos {
    val archivos = linkedMapOf<String, String>()
    var registrada: String? = null
    var fallarAlEscribir = false

    override fun registrar(carpeta: String) {
        registrada = carpeta
    }

    override fun disponible(carpeta: String) = carpeta == registrada

    override fun nombre(carpeta: String) = "AgendaRespaldos"

    override fun escribir(carpeta: String, nombre: String, contenido: String) {
        if (fallarAlEscribir) throw IOException("Sin espacio")
        archivos[nombre] = contenido
    }

    override fun listar(carpeta: String) = archivos.keys.map { it to "uri:$it" }

    override fun borrar(uri: String) {
        archivos.remove(uri.removePrefix("uri:"))
    }

    override fun leer(uri: String) = archivos[uri.removePrefix("uri:")] ?: throw IOException("No existe")

    override fun escribirEn(uri: String, contenido: String) {
        if (fallarAlEscribir) throw IOException("Sin conexión")
        archivos[uri.removePrefix("uri:")] = contenido
    }
}

class PlanificadorRespaldoFalso : PlanificadorRespaldo {
    var programado: Frecuencia? = null
    var ejecutadoAhora = 0

    override fun programar(frecuencia: Frecuencia) {
        programado = frecuencia
    }

    override fun cancelar() {
        programado = null
    }

    override fun ejecutarAhora() {
        ejecutadoAhora++
    }
}
