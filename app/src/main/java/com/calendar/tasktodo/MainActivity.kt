package com.calendar.tasktodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calendar.tasktodo.ui.navegacion.AgendaApp
import com.calendar.tasktodo.ui.theme.TaskToDoTheme

/** Única Activity: toda la navegación vive en Compose (arquitectura §6, decisión 1). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskToDoTheme {
                AgendaApp()
            }
        }
    }
}
