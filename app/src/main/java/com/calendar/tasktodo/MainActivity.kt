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

    /** Lo que pidió abrir una notificación o la alarma de la pantalla de bloqueo. */
    private val pedido = MutableStateFlow<Pedido?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) leerPedido(intent)
        setContent {
            TaskToDoTheme {
                AgendaApp(pedido = pedido, onPedidoAtendido = { pedido.value = null })
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
        lifecycleScope.launch {
            contenedor.pendientesAlAbrir.ejecutar()
            // Si Android cerró la app con una sesión en curso, se vuelve a mostrar su notificación (RNF-07)
            contenedor.controlSesion.asegurar()
        }
    }

    private fun leerPedido(intent: Intent?) {
        intent ?: return
        intent.getLongExtra(EXTRA_SESION, 0).takeIf { it > 0 }?.let { pedido.value = Pedido.Sesion(it) }
            ?: intent.getLongExtra(EXTRA_ACTIVIDAD, 0).takeIf { it > 0 }?.let { pedido.value = Pedido.Actividad(it) }
    }

    companion object {
        private const val EXTRA_ACTIVIDAD = "actividad"
        private const val EXTRA_SESION = "sesion"

        /** Abre la sesión de gym de esa actividad (RF-22, RF-30). */
        fun intentDeSesion(context: Context, actividadId: Long?): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { actividadId?.let { putExtra(EXTRA_SESION, it) } }

        /** Abre la app; si hay [actividadId], directo en su detalle. */
        fun intentDeActividad(context: Context, actividadId: Long?): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { actividadId?.let { putExtra(EXTRA_ACTIVIDAD, it) } }
    }
}

/** Pantalla que pidió abrir una notificación. */
sealed interface Pedido {
    data class Actividad(val id: Long) : Pedido

    data class Sesion(val actividadId: Long) : Pedido
}
