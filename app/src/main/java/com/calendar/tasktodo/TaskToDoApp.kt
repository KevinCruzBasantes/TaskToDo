package com.calendar.tasktodo

import android.app.Application
import com.calendar.tasktodo.di.AppContainer
import com.calendar.tasktodo.sistema.avisos.Canales

class TaskToDoApp : Application() {

    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContainer(this)
        Canales.crear(this)
    }
}
