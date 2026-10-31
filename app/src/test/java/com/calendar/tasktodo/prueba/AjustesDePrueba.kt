package com.calendar.tasktodo.prueba

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.calendar.tasktodo.data.ajustes.AjustesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.rules.ExternalResource

/**
 * DataStore en memoria. En Windows, el DataStore real no puede renombrar su archivo temporal
 * dentro de las pruebas de JVM; el real se prueba en el celular.
 */
class DataStoreEnMemoria : DataStore<Preferences> {
    private val estado = MutableStateFlow(emptyPreferences())
    private val candado = Mutex()

    override val data: StateFlow<Preferences> = estado

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        candado.withLock { transform(estado.value).also { estado.value = it } }
}

/** Unos ajustes nuevos, con sus valores por defecto, en cada prueba. */
class ReglaAjustes : ExternalResource() {

    lateinit var repositorio: AjustesRepository
        private set

    override fun before() {
        repositorio = AjustesRepository(DataStoreEnMemoria())
    }
}
