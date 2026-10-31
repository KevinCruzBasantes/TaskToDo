package com.calendar.tasktodo.ui.gym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calendar.tasktodo.data.db.entidad.Actividad
import com.calendar.tasktodo.data.db.entidad.EstadoActividad
import com.calendar.tasktodo.data.db.entidad.RutinaConConteo
import com.calendar.tasktodo.data.db.entidad.SesionGym
import com.calendar.tasktodo.data.repositorio.ActividadRepository
import com.calendar.tasktodo.data.repositorio.EleccionDia
import com.calendar.tasktodo.data.repositorio.GymRepository
import com.calendar.tasktodo.data.repositorio.SesionRepository
import com.calendar.tasktodo.ui.calendario.lunesDe
import com.calendar.tasktodo.ui.componentes.CampoHora
import com.calendar.tasktodo.ui.componentes.DialogoConfirmar
import com.calendar.tasktodo.ui.componentes.DialogoFecha
import com.calendar.tasktodo.ui.componentes.Pantalla
import com.calendar.tasktodo.ui.contenedor
import com.calendar.tasktodo.ui.nombre
import com.calendar.tasktodo.ui.rememberFormatoHora
import com.calendar.tasktodo.ui.textoCorto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class GymUiState(
    val cargando: Boolean = true,
    val proxima: Actividad? = null,
    val enCurso: SesionGym? = null,
    val hayRutinas: Boolean = false,
    val hayProgramacion: Boolean = false,
    val hechasSemana: Int = 0,
    val planificadasSemana: Int = 0,
    val hoy: LocalDate = LocalDate.MIN,
)

/** Inicio del gym (diseño de interfaz §3.8). */
class GymViewModel(
    private val gym: GymRepository,
    private val actividades: ActividadRepository,
    sesiones: SesionRepository,
    reloj: Clock,
) : ViewModel() {

    private val hoy = LocalDate.now(reloj)

    val estado: StateFlow<GymUiState> = combine(
        gym.proximaSesion(),
        sesiones.enCurso,
        gym.rutinas,
        gym.dias,
        gym.gymEntre(lunesDe(hoy), lunesDe(hoy).plusDays(6)),
    ) { proxima, enCurso, rutinas, dias, semana ->
        // Las saltadas no cuentan como planificadas (modelo de datos §6)
        val planificadas = semana.filter { it.estado != EstadoActividad.SALTADA }
        GymUiState(
            cargando = false,
            proxima = proxima,
            enCurso = enCurso,
            hayRutinas = rutinas.isNotEmpty(),
            hayProgramacion = dias.isNotEmpty(),
            hechasSemana = planificadas.count { it.estado == EstadoActividad.CUMPLIDA },
            planificadasSemana = planificadas.size,
            hoy = hoy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GymUiState())

    /** Solo esa sesión; la programación semanal no cambia (CU-15). */
    fun mover(actividadId: Long, fecha: LocalDate, hora: LocalTime) {
        viewModelScope.launch { gym.moverSesion(actividadId, fecha, hora) }
    }

    fun saltar(actividadId: Long) {
        viewModelScope.launch { actividades.saltar(actividadId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                GymViewModel(c.gymRepository, c.actividadRepository, c.sesionRepository, c.reloj)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScreen(
    onAbrirSesion: (Long) -> Unit,
    onRutinas: () -> Unit,
    onProgramacion: () -> Unit,
    viewModel: GymViewModel = viewModel(factory = GymViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    var moviendo by remember { mutableStateOf<Actividad?>(null) }
    var saltando by remember { mutableStateOf<Actividad?>(null) }

    Pantalla(enBarra = true, topBar = { TopAppBar(title = { Text("Gym") }) }) { modifier ->
        if (estado.cargando) return@Pantalla
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            val enCurso = estado.enCurso
            val proxima = estado.proxima
            when {
                enCurso != null -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("SESIÓN EN CURSO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("🏋 ${enCurso.nombreRutina}", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = { enCurso.actividadId?.let(onAbrirSesion) }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Volver a la sesión")
                        }
                    }
                }
                proxima != null -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("PRÓXIMA SESIÓN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        val cuando = if (proxima.fecha == estado.hoy) "Hoy" else proxima.fecha.textoCorto()
                        Text("🏋 ${proxima.titulo} · $cuando ${proxima.hora?.format(formatoHora).orEmpty()}", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Button(onClick = { onAbrirSesion(proxima.id) }) { Text("Iniciar") }
                            TextButton(onClick = { moviendo = proxima }) { Text("Mover") }
                            TextButton(onClick = { saltando = proxima }) { Text("Saltar") }
                        }
                    }
                }
                !estado.hayProgramacion -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Aún no programas tu gym", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = if (estado.hayRutinas) onProgramacion else onRutinas, modifier = Modifier.padding(top = 8.dp)) {
                            Text(if (estado.hayRutinas) "Programar" else "Crear una rutina")
                        }
                    }
                }
            }
            if (estado.planificadasSemana > 0) {
                Text(
                    "Esta semana: ${estado.hechasSemana} de ${estado.planificadasSemana} sesiones",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                Spacer(Modifier.height(16.dp))
            }
            ListItem(headlineContent = { Text("📋 Rutinas") }, modifier = Modifier.clickable(onClick = onRutinas))
            HorizontalDivider()
            ListItem(headlineContent = { Text("🗓 Programación") }, modifier = Modifier.clickable(onClick = onProgramacion))
            HorizontalDivider()
        }
    }

    moviendo?.let { actividad ->
        DialogoMoverSesion(actividad, onMover = { fecha, hora ->
            viewModel.mover(actividad.id, fecha, hora)
            moviendo = null
        }, onCancelar = { moviendo = null })
    }
    saltando?.let { actividad ->
        DialogoConfirmar(
            titulo = "¿Saltar esta sesión?",
            texto = "${actividad.titulo} del ${actividad.fecha.textoCorto()} quedará como saltada. Las demás semanas no cambian.",
            confirmar = "Saltar",
            onConfirmar = {
                viewModel.saltar(actividad.id)
                saltando = null
            },
            onCancelar = { saltando = null },
        )
    }
}

/** Nueva fecha y hora para una sesión puntual (CU-15). */
@Composable
fun DialogoMoverSesion(actividad: Actividad, onMover: (LocalDate, LocalTime) -> Unit, onCancelar: () -> Unit) {
    val formatoHora = rememberFormatoHora()
    var fecha by remember { mutableStateOf(actividad.fecha) }
    var hora by remember { mutableStateOf(actividad.hora ?: LocalTime.of(18, 0)) }
    var eligiendoFecha by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Mover sesión") },
        text = {
            Column {
                Text("Solo cambia esta sesión. La programación semanal sigue igual.")
                OutlinedButton(onClick = { eligiendoFecha = true }, modifier = Modifier.padding(top = 12.dp)) { Text(fecha.textoCorto()) }
                CampoHora("Hora", hora, formatoHora, { hora = it })
            }
        },
        confirmButton = { TextButton(onClick = { onMover(fecha, hora) }) { Text("Mover") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
    if (eligiendoFecha) {
        DialogoFecha(inicial = fecha, desde = LocalDate.now(), onElegir = {
            fecha = it
            eligiendoFecha = false
        }, onCancelar = { eligiendoFecha = false })
    }
}

data class FilaProgramacion(val activo: Boolean = false, val hora: LocalTime = LocalTime.of(18, 0), val rutinaId: Long? = null)

data class ProgramacionUiState(
    val cargando: Boolean = true,
    val dias: Map<DayOfWeek, FilaProgramacion> = DayOfWeek.entries.associateWith { FilaProgramacion() },
    /** Solo las que tienen ejercicios se pueden asignar (CU-14 4a). */
    val rutinas: List<RutinaConConteo> = emptyList(),
    val fin: LocalDate? = null,
    val error: String? = null,
    val guardada: Boolean = false,
)

/** Programar el gym (CU-14). */
class ProgramacionViewModel(private val gym: GymRepository) : ViewModel() {

    private val _estado = MutableStateFlow(ProgramacionUiState())
    val estado: StateFlow<ProgramacionUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val dias = gym.dias.first().associateBy { it.diaSemana }
            val fin = gym.finProgramacion.first()
            _estado.update { actual ->
                actual.copy(
                    cargando = false,
                    fin = fin,
                    dias = DayOfWeek.entries.associateWith { d ->
                        dias[d]?.let { FilaProgramacion(true, it.hora, it.rutinaId) } ?: FilaProgramacion()
                    },
                )
            }
        }
        viewModelScope.launch {
            gym.rutinas.collect { lista -> _estado.update { it.copy(rutinas = lista.filter { r -> r.ejercicios > 0 }) } }
        }
    }

    fun cambiarDia(dia: DayOfWeek, transformar: (FilaProgramacion) -> FilaProgramacion) =
        _estado.update { it.copy(dias = it.dias + (dia to transformar(it.dias.getValue(dia))), error = null) }

    fun cambiarFin(fecha: LocalDate?) = _estado.update { it.copy(fin = fecha) }

    fun guardar() {
        val actual = _estado.value
        val activos = actual.dias.filterValues { it.activo }
        if (activos.values.any { it.rutinaId == null }) {
            _estado.update { it.copy(error = "Elige una rutina para cada día marcado") }
            return
        }
        viewModelScope.launch {
            gym.guardarProgramacion(activos.mapValues { (_, f) -> EleccionDia(f.hora, f.rutinaId!!) }, actual.fin)
            _estado.update { it.copy(guardada = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { ProgramacionViewModel(contenedor().gymRepository) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramacionScreen(
    onAtras: () -> Unit,
    onCrearRutina: () -> Unit,
    viewModel: ProgramacionViewModel = viewModel(factory = ProgramacionViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val formatoHora = rememberFormatoHora()
    var eligiendoFin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(estado.guardada) { if (estado.guardada) onAtras() }

    Pantalla(
        enBarra = false,
        topBar = {
            TopAppBar(
                title = { Text("Programación") },
                navigationIcon = { IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
                actions = { TextButton(onClick = viewModel::guardar, enabled = !estado.cargando && estado.rutinas.isNotEmpty()) { Text("Guardar") } },
            )
        },
    ) { modifier ->
        if (estado.cargando) return@Pantalla
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (estado.rutinas.isEmpty()) {
                Text("Primero crea una rutina con ejercicios.")
                Button(onClick = onCrearRutina, modifier = Modifier.padding(top = 8.dp)) { Text("Crear rutina") }
                return@Column
            }
            DayOfWeek.entries.forEach { dia ->
                val fila = estado.dias.getValue(dia)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = fila.activo, onCheckedChange = { v -> viewModel.cambiarDia(dia) { it.copy(activo = v) } })
                    Text(dia.nombre(), style = MaterialTheme.typography.titleMedium)
                }
                if (fila.activo) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)) {
                        CampoHora("Hora", fila.hora, formatoHora, { h -> viewModel.cambiarDia(dia) { it.copy(hora = h) } }, Modifier.weight(1f))
                        SelectorRutina(estado.rutinas, fila.rutinaId, { id -> viewModel.cambiarDia(dia) { it.copy(rutinaId = id) } }, Modifier.weight(1f))
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Termina el", Modifier.weight(1f))
                TextButton(onClick = { eligiendoFin = true }) { Text(estado.fin?.textoCorto() ?: "Sin fecha de fin") }
                if (estado.fin != null) TextButton(onClick = { viewModel.cambiarFin(null) }) { Text("Quitar") }
            }
            estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (eligiendoFin) {
        DialogoFecha(inicial = estado.fin ?: LocalDate.now().plusMonths(3), desde = LocalDate.now(), onElegir = {
            viewModel.cambiarFin(it)
            eligiendoFin = false
        }, onCancelar = { eligiendoFin = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRutina(rutinas: List<RutinaConConteo>, elegida: Long?, onElegir: (Long) -> Unit, modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            value = rutinas.firstOrNull { it.rutina.id == elegida }?.rutina?.nombre ?: "Elegir",
            onValueChange = {},
            readOnly = true,
            label = { Text("Rutina") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            rutinas.forEach { r ->
                DropdownMenuItem(text = { Text(r.rutina.nombre) }, onClick = {
                    onElegir(r.rutina.id)
                    abierto = false
                })
            }
        }
    }
}
