package com.calendar.tasktodo

import android.app.Application
import com.calendar.tasktodo.di.AppContainer

class TaskToDoApp : Application() {

    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContainer(this)
    }
}
