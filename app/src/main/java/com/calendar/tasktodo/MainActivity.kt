package com.calendar.tasktodo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.calendar.tasktodo.ui.navegacion.AgendaApp
import com.calendar.tasktodo.ui.theme.TaskToDoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Única Activity: toda la navegación vive en Compose (arquitectura §6, decisión 1). */
class MainActivity : ComponentActivity() {

    /** Actividad que pidió abrir una notificación o la alarma de la pantalla de bloqueo. */
    private val actividadPedida = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) leerPedido(intent)
        setContent {
            TaskToDoTheme {
                AgendaApp(actividadPedida = actividadPedida, onPedidoAtendido = { actividadPedida.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerPedido(intent)
    }

    override fun onStart() {
        super.onStart()
        val contenedor = (application as TaskToDoApp).contenedor
        lifecycleScope.launch { contenedor.pendientesAlAbrir.ejecutar() }
    }

    private fun leerPedido(intent: Intent?) {
        intent?.getLongExtra(EXTRA_ACTIVIDAD, 0)?.takeIf { it > 0 }?.let { actividadPedida.value = it }
    }

    companion object {
        private const val EXTRA_ACTIVIDAD = "actividad"

        /** Abre la app; si hay [actividadId], directo en su detalle. */
        fun intentDeActividad(context: Context, actividadId: Long?): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { actividadId?.let { putExtra(EXTRA_ACTIVIDAD, it) } }
    }
}
