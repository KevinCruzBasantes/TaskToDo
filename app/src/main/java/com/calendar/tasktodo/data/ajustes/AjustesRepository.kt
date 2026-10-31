package com.calendar.tasktodo.data.ajustes

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Configuración de los avisos (CU-19). Los valores por defecto son los del diseño de interfaz §3.14. */
data class AjustesAvisos(
    val actividades: Boolean = true,
    val horaSinHora: LocalTime = LocalTime.of(8, 0),
    val diaAnterior: Boolean = true,
    val horaDiaAnterior: LocalTime = LocalTime.of(20, 0),
    val clases: Boolean = false,
    val minutosClase: Int = 10,
    val resumen: Boolean = true,
    val horaResumen: LocalTime = LocalTime.of(7, 0),
    /** `setAlarmClock` para cumplir el margen de ±1 minuto (RNF-10). */
    val prioridadAlarma: Boolean = true,
    /** Aviso antes de cada sesión de gym, con el nombre de la rutina (RF-40). */
    val gym: Boolean = true,
    val minutosGym: Int = 15,
)

/** Cada cuánto se hace el respaldo automático (RF-46). */
enum class Frecuencia(val dias: Long) { DIARIA(1), SEMANAL(7), MENSUAL(30) }

data class AjustesRespaldo(
    /** URI de la carpeta registrada; no viaja en el respaldo porque el permiso es de cada instalación. */
    val carpeta: String? = null,
    val ultimo: Instant? = null,
    val automatico: Boolean = false,
    val frecuencia: Frecuencia = Frecuencia.SEMANAL,
)

/** Ajustes en DataStore (modelo de datos §2.3). */
class AjustesRepository(private val datos: DataStore<Preferences>) {

    val avisos: Flow<AjustesAvisos> = datos.data.map { p ->
        val porDefecto = AjustesAvisos()
        AjustesAvisos(
            actividades = p[ACTIVIDADES] ?: porDefecto.actividades,
            horaSinHora = p[HORA_SIN_HORA]?.let(LocalTime::parse) ?: porDefecto.horaSinHora,
            diaAnterior = p[DIA_ANTERIOR] ?: porDefecto.diaAnterior,
            horaDiaAnterior = p[HORA_DIA_ANTERIOR]?.let(LocalTime::parse) ?: porDefecto.horaDiaAnterior,
            clases = p[CLASES] ?: porDefecto.clases,
            minutosClase = p[MINUTOS_CLASE] ?: porDefecto.minutosClase,
            resumen = p[RESUMEN] ?: porDefecto.resumen,
            horaResumen = p[HORA_RESUMEN]?.let(LocalTime::parse) ?: porDefecto.horaResumen,
            prioridadAlarma = p[PRIORIDAD_ALARMA] ?: porDefecto.prioridadAlarma,
            gym = p[AVISO_GYM] ?: porDefecto.gym,
            minutosGym = p[MINUTOS_GYM] ?: porDefecto.minutosGym,
        )
    }

    /** Fecha de fin opcional de la programación del gym (RF-23). */
    val finProgramacionGym: Flow<LocalDate?> = datos.data.map { p -> p[FIN_GYM]?.let(LocalDate::parse) }

    suspend fun guardarFinProgramacionGym(fecha: LocalDate?) {
        datos.edit { if (fecha == null) it.remove(FIN_GYM) else it[FIN_GYM] = fecha.toString() }
    }

    val respaldo: Flow<AjustesRespaldo> = datos.data.map { p ->
        AjustesRespaldo(
            carpeta = p[CARPETA_RESPALDOS],
            ultimo = p[ULTIMO_RESPALDO]?.let(Instant::ofEpochMilli),
            automatico = p[RESPALDO_AUTOMATICO] ?: false,
            frecuencia = p[FRECUENCIA_RESPALDO]?.let { nombre -> Frecuencia.entries.firstOrNull { it.name == nombre } }
                ?: Frecuencia.SEMANAL,
        )
    }

    suspend fun guardarCarpeta(uri: String) {
        datos.edit { it[CARPETA_RESPALDOS] = uri }
    }

    suspend fun guardarUltimoRespaldo(instante: Instant) {
        datos.edit { it[ULTIMO_RESPALDO] = instante.toEpochMilli() }
    }

    suspend fun guardarAutomatico(activado: Boolean, frecuencia: Frecuencia) {
        datos.edit {
            it[RESPALDO_AUTOMATICO] = activado
            it[FRECUENCIA_RESPALDO] = frecuencia.name
        }
    }

    val guiaMiuiMostrada: Flow<Boolean> = datos.data.map { it[GUIA_MIUI_MOSTRADA] ?: false }

    /** Pasos de la guía de MIUI que no se pueden comprobar y el usuario marcó con "Hecho". */
    val pasosGuiaHechos: Flow<Set<String>> = datos.data.map { it[PASOS_GUIA_HECHOS].orEmpty() }

    suspend fun guardarAvisos(ajustes: AjustesAvisos) {
        datos.edit { p ->
            p[ACTIVIDADES] = ajustes.actividades
            p[HORA_SIN_HORA] = ajustes.horaSinHora.toString()
            p[DIA_ANTERIOR] = ajustes.diaAnterior
            p[HORA_DIA_ANTERIOR] = ajustes.horaDiaAnterior.toString()
            p[CLASES] = ajustes.clases
            p[MINUTOS_CLASE] = ajustes.minutosClase
            p[RESUMEN] = ajustes.resumen
            p[HORA_RESUMEN] = ajustes.horaResumen.toString()
            p[PRIORIDAD_ALARMA] = ajustes.prioridadAlarma
            p[AVISO_GYM] = ajustes.gym
            p[MINUTOS_GYM] = ajustes.minutosGym
        }
    }

    suspend fun marcarGuiaMiuiMostrada() {
        datos.edit { it[GUIA_MIUI_MOSTRADA] = true }
    }

    suspend fun marcarPasoGuia(paso: String) {
        datos.edit { it[PASOS_GUIA_HECHOS] = it[PASOS_GUIA_HECHOS].orEmpty() + paso }
    }

    /** Códigos de las alarmas que hay programadas, para cancelar las que ya no tocan. */
    suspend fun codigosProgramados(): Set<Int> =
        datos.data.first()[CODIGOS_PROGRAMADOS].orEmpty().mapNotNull(String::toIntOrNull).toSet()

    suspend fun guardarCodigosProgramados(codigos: Set<Int>) {
        datos.edit { it[CODIGOS_PROGRAMADOS] = codigos.map(Int::toString).toSet() }
    }

    suspend fun ultimaComprobacion(): Instant? = datos.data.first()[ULTIMA_COMPROBACION]?.let(Instant::ofEpochMilli)

    suspend fun guardarUltimaComprobacion(instante: Instant) {
        datos.edit { it[ULTIMA_COMPROBACION] = instante.toEpochMilli() }
    }

    private companion object {
        val ACTIVIDADES = booleanPreferencesKey("avisos_actividades")
        val HORA_SIN_HORA = stringPreferencesKey("avisos_hora_sin_hora")
        val DIA_ANTERIOR = booleanPreferencesKey("avisos_dia_anterior")
        val HORA_DIA_ANTERIOR = stringPreferencesKey("avisos_hora_dia_anterior")
        val CLASES = booleanPreferencesKey("avisos_clases")
        val MINUTOS_CLASE = intPreferencesKey("avisos_minutos_clase")
        val RESUMEN = booleanPreferencesKey("avisos_resumen")
        val HORA_RESUMEN = stringPreferencesKey("avisos_hora_resumen")
        val PRIORIDAD_ALARMA = booleanPreferencesKey("avisos_prioridad_alarma")
        val GUIA_MIUI_MOSTRADA = booleanPreferencesKey("guia_miui_mostrada")
        val PASOS_GUIA_HECHOS = stringSetPreferencesKey("pasos_guia_hechos")
        val CODIGOS_PROGRAMADOS = stringSetPreferencesKey("codigos_programados")
        val ULTIMA_COMPROBACION = longPreferencesKey("ultima_comprobacion")
        val AVISO_GYM = booleanPreferencesKey("avisos_gym")
        val MINUTOS_GYM = intPreferencesKey("avisos_minutos_gym")
        val FIN_GYM = stringPreferencesKey("fin_programacion_gym")
        val CARPETA_RESPALDOS = stringPreferencesKey("carpeta_respaldos")
        val ULTIMO_RESPALDO = longPreferencesKey("ultimo_respaldo")
        val RESPALDO_AUTOMATICO = booleanPreferencesKey("respaldo_automatico")
        val FRECUENCIA_RESPALDO = stringPreferencesKey("frecuencia_respaldo")
    }
}
