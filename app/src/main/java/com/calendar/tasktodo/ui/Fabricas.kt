package com.calendar.tasktodo.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.calendar.tasktodo.TaskToDoApp
import com.calendar.tasktodo.di.AppContainer

/** Para los `viewModelFactory { initializer { … } }` de cada ViewModel. */
fun CreationExtras.contenedor(): AppContainer = (this[APPLICATION_KEY] as TaskToDoApp).contenedor
