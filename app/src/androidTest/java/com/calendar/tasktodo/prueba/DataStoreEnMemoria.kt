package com.calendar.tasktodo.prueba

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/** Ajustes en memoria, para que cada prueba empiece sin ajustes guardados. */
class DataStoreEnMemoria : DataStore<Preferences> {
    private val estado = MutableStateFlow(emptyPreferences())
    override val data = estado
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) =
        transform(estado.value).also { estado.value = it }
}
