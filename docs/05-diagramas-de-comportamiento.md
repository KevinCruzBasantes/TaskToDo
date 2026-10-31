# Diagramas de comportamiento

Estado: en revisión · Base: [Casos de uso](01-casos-de-uso.md), [Modelo de datos](02-modelo-de-datos.md), [Arquitectura](03-arquitectura.md)

---

Este documento muestra cómo se comportan los flujos más delicados de la app: los que dependen del tiempo, trabajan en segundo plano o tocan muchos datos a la vez. Usa tres tipos de diagrama:

- **Estados:** en qué situaciones puede estar algo y qué lo hace cambiar.
- **Secuencia:** qué clases participan y en qué orden se llaman.
- **Actividad:** los pasos y las decisiones de un proceso.

Los nombres de las clases son los de la [arquitectura §3](03-arquitectura.md#3-estructura-de-paquetes).

---

## 1. Sesión de gym

Cubre CU-16 y RF-27 a RF-31.

### 1.1 Estados de la sesión

```mermaid
stateDiagram-v2
    [*] --> Preparada: abrir la actividad de gym
    Preparada --> EnSerie: Iniciar sesión
    Preparada --> [*]: volver sin iniciar

    state EnSerie {
        [*] --> Esperando
        Esperando --> CuentaRegresiva: ejercicio por tiempo, Iniciar
        CuentaRegresiva --> Esperando: termina la cuenta
    }

    EnSerie --> DescansoSeries: Serie hecha, quedan series y hay descanso
    EnSerie --> EnSerie: Serie hecha, quedan series sin descanso
    DescansoSeries --> EnSerie: termina, se salta o llega a 0

    EnSerie --> DescansoEjercicio: última serie del ejercicio y hay descanso
    EnSerie --> EnSerie: última serie sin descanso, pasa al siguiente ejercicio
    DescansoEjercicio --> EnSerie: siguiente ejercicio

    EnSerie --> Resumen: último ejercicio terminado o Terminar sesión
    DescansoSeries --> Resumen: Terminar sesión
    DescansoEjercicio --> Resumen: Terminar sesión

    Resumen --> Guardada: Guardar
    Resumen --> Descartada: Descartar y confirmar
    Resumen --> EnSerie: Seguir entrenando

    Guardada --> [*]
    Descartada --> [*]
```

- **Cambiar el orden de los ejercicios** (CU-16 9b) no cambia de estado. Solo cambia cuál es el siguiente, y se puede hacer en `EnSerie` y en los dos descansos.
- **Lo que se guarda en la base de datos:** desde `EnSerie` hasta `Resumen`, la sesión existe como `EN_CURSO`. `Guardada` la pasa a `TERMINADA` y `Descartada` la borra con sus series.

### 1.2 Iniciar la sesión y confirmar una serie

```mermaid
sequenceDiagram
    actor U as Usuario
    participant S as SesionScreen
    participant V as SesionViewModel
    participant R as SesionRepository
    participant DB as Room
    participant F as SesionGymService
    participant AM as AlarmManager

    U->>S: Iniciar sesión
    S->>V: iniciar()
    V->>R: iniciarSesion(actividadId)
    R->>DB: insertar sesion_gym EN_CURSO, inicio = ahora, orden_ejercicios = orden de la rutina
    R-->>V: sesión creada
    V->>F: arrancar servicio
    F-->>U: notificación fija con cronómetro
    V->>R: prellenado(ejercicioId)
    R->>DB: series de la última sesión con ese ejercicio
    R-->>V: valores de la última vez o de la rutina
    V-->>S: Serie 1 de N con valores

    U->>S: Serie hecha
    S->>V: confirmarSerie(reps, peso)
    V->>R: guardarSerie(...)
    R->>DB: insertar serie_realizada
    alt hay descanso configurado
        V->>R: iniciarDescanso(segundos)
        R->>DB: descanso_termina_en = ahora + segundos
        V->>AM: setAlarmClock en descanso_termina_en
        V->>F: mostrar cuenta regresiva en la notificación
        AM-->>F: fin del descanso
        F-->>U: vibración o sonido
    end
    V-->>S: siguiente serie
```

- **El ViewModel no lleva el tiempo con un contador.** El cronómetro siempre es `ahora − inicio`, y el descanso es `descanso_termina_en − ahora`. Así se ven bien aunque la app se haya detenido (RNF-07).
- **Ajustar el descanso** con ±15 s o saltarlo cambia `descanso_termina_en` y reemplaza la alarma, que tiene un código fijo.

### 1.3 Recuperar una sesión después de un cierre

```mermaid
flowchart TD
    A[La app vuelve a primer plano<br>o se abre desde la notificación] --> B{¿Hay una sesión EN_CURSO?}
    B -- No --> Z[Flujo normal]
    B -- Sí --> C[Leer la sesión, sus series y orden_ejercicios]
    C --> D[Calcular el ejercicio y la serie actuales<br>con el orden y las series hechas]
    D --> E{¿descanso_termina_en es futuro?}
    E -- Sí --> F[Mostrar el descanso con el tiempo restante]
    E -- No --> G[Mostrar la serie actual]
    F --> H{¿El servicio está activo?}
    G --> H
    H -- No --> I[Volver a arrancar SesionGymService]
    H -- Sí --> J[Listo]
    I --> J
```

Esto cubre el caso de MIUI que cierra la app al deslizarla en recientes (arquitectura §4.6). No se pierde ninguna serie confirmada, porque cada una se guarda al momento.

### 1.4 Terminar y guardar

```mermaid
sequenceDiagram
    actor U as Usuario
    participant V as SesionViewModel
    participant R as SesionRepository
    participant G as GymRepository
    participant DB as Room
    participant F as SesionGymService

    U->>V: Guardar
    V->>R: terminarSesion()
    R->>DB: transacción: sesion_gym TERMINADA, fin y duracion_seg, actividad CUMPLIDA
    alt el orden cambió respecto de la rutina
        V-->>U: ¿Guardar este orden en la rutina?
        U->>V: Sí
        V->>G: reordenarEjercicios(rutinaId, orden_ejercicios)
        G->>DB: actualizar ejercicio.orden
    end
    V->>F: detener servicio
    F-->>U: quita la notificación
```

---

## 2. Respaldo y restauración

Cubre CU-21 a CU-24 y RF-44 a RF-47.

### 2.1 Registrar la carpeta de respaldos

```mermaid
flowchart TD
    A[Hace falta la carpeta:<br>Respaldar ahora, Restaurar, borrar materias,<br>activar el automático o primera apertura] --> B{¿Hay carpeta registrada<br>y conserva el permiso?}
    B -- Sí --> Z[Seguir con la acción]
    B -- No --> C[Explicar: aquí se guardarán todos tus respaldos]
    C --> D[OpenDocumentTree con inicio en Documentos<br>sugiriendo crear AgendaRespaldos]
    D --> E{¿El usuario eligió una carpeta?}
    E -- No --> F[Cancelar la acción<br>Sin respaldo]
    E -- Sí --> G[takePersistableUriPermission<br>lectura y escritura]
    G --> H[Guardar el URI en DataStore]
    H --> Z
```

### 2.2 Respaldar ahora

```mermaid
sequenceDiagram
    actor U as Usuario
    participant S as AjustesRespaldoScreen
    participant V as RespaldoViewModel
    participant R as RespaldoRepository
    participant DB as Room
    participant DS as DataStore
    participant C as Carpeta de respaldos

    U->>S: Respaldar ahora
    S->>V: respaldar()
    V->>R: respaldar(MANUAL)
    R->>DS: leer el URI de la carpeta
    alt no hay carpeta o se perdió el permiso
        R-->>V: falta carpeta
        V-->>S: registrar la carpeta, sección 2.1
    else carpeta disponible
        R->>DB: leer todas las tablas en una transacción de lectura
        R->>DS: leer los ajustes que van en el respaldo
        R->>R: armar RespaldoDto y convertirlo a JSON
        R->>C: crear agenda-manual-AAAA-MM-DD-HHmm.json y escribir
        alt escritura correcta
            R->>DS: fecha del último respaldo = ahora
            V-->>S: Respaldo creado
        else error de escritura
            V-->>S: No se pudo guardar, sin cambiar la fecha
        end
    end
```

- **Una foto coherente:** la lectura de todas las tablas se hace en una sola transacción, para que el respaldo sea coherente aunque haya cambios al mismo tiempo.
- **Copia en otra ubicación (CU-21 1a):** sigue el mismo camino, pero en vez de crear el archivo en la carpeta abre `CreateDocument` y escribe en el URI que elige el usuario, por ejemplo en Google Drive.

**Formato del archivo**

```json
{
  "versionBaseDatos": 4,
  "versionApp": "1.0",
  "fecha": "2026-10-31T21:15:00Z",
  "ajustes": { "estaturaCm": 175, "avisos": { "…": "…" } },
  "materias": [ { "id": 1, "nombre": "Cálculo", "color": -11567956 } ],
  "clases": [ "…" ],
  "actividades": [ "…" ],
  "rutinas": [ "…" ],
  "ejercicios": [ "…" ],
  "diasGym": [ "…" ],
  "sesiones": [ "…" ],
  "series": [ "…" ],
  "registrosPeso": [ "…" ]
}
```

### 2.3 Restaurar un respaldo

```mermaid
flowchart TD
    A[Restaurar respaldo] --> A1{¿Hay carpeta registrada?}
    A1 -- No --> A2[Registrar la carpeta, sección 2.1]
    A2 --> B
    A1 -- Sí --> B[Listar agenda-*.json de la carpeta<br>del más reciente al más antiguo]
    B --> B1{¿Elige uno de la lista<br>o busca en otra ubicación?}
    B1 -- Otra ubicación --> B2[OpenDocument, por ejemplo Google Drive]
    B1 -- De la lista --> D
    B2 --> C{¿Se eligió un archivo?}
    C -- No --> Z0[Sin cambios]
    C -- Sí --> D[Leer y convertir el JSON]
    D --> E{¿Es un respaldo válido?}
    E -- No --> Z1[Archivo no válido o dañado<br>Sin cambios]
    E -- Sí --> F{¿versionBaseDatos mayor que la actual?}
    F -- Sí --> Z2[Actualiza la app para restaurarlo<br>Sin cambios]
    F -- No --> G[Mostrar fecha, versión y cuántos datos de cada tipo]
    G --> H{¿El usuario confirma?}
    H -- No --> Z0
    H -- Sí --> I[Crear agenda-seguridad-AAAA-MM-DD-HHmm.json<br>con los datos actuales en la carpeta]
    I --> J{¿Se creó?}
    J -- No --> Z3[No se pudo respaldar lo actual<br>Sin cambios]
    J -- Sí --> K[Adaptar el respaldo a la versión actual<br>campos nuevos con su valor por defecto]
    K --> L[Transacción: vaciar todas las tablas<br>e insertar en orden de dependencia]
    L --> M{¿Terminó bien?}
    M -- No --> N[Rollback: los datos actuales quedan intactos]
    M -- Sí --> O[Guardar los ajustes del respaldo]
    O --> P[Cancelar todos los avisos y reprogramarlos]
    P --> Q[Regenerar la ventana del gym]
    Q --> R[Restauración completa]
```

- **Respaldo de seguridad:** siempre va a la carpeta de respaldos, que a esta altura ya está registrada. Se conservan los últimos 5 de seguridad.
- **Orden de inserción:** primero los padres y después los hijos, para respetar las claves foráneas. El orden es `materia`, `clase`, `rutina`, `ejercicio`, `dia_gym`, `actividad`, `sesion_gym`, `serie_realizada` y `registro_peso`.
- **Identificadores:** se conservan los `id` del archivo, para que las relaciones sigan apuntando a lo mismo.

### 2.4 Respaldo automático

```mermaid
flowchart TD
    A[RespaldoAutomaticoWorker se ejecuta<br>o PendientesAlAbrir detecta atraso] --> B{¿Está activado y hay carpeta registrada?}
    B -- No --> Z0[Terminar]
    B -- Sí --> C{¿Hay permiso sobre la carpeta?}
    C -- No --> Z1[Notificación: vuelve a elegir la carpeta]
    C -- Sí --> D[Generar el JSON igual que en 2.2]
    D --> E[Crear agenda-auto-AAAA-MM-DD-HHmm.json]
    E --> F{¿Se escribió?}
    F -- No --> Z2[Reintentar más tarde<br>WorkManager con espera creciente]
    F -- Sí --> G[Listar archivos agenda-auto-*]
    G --> H{¿Hay más de 5?}
    H -- Sí --> I[Borrar los más antiguos hasta dejar 5]
    H -- No --> J[Actualizar la fecha del último respaldo]
    I --> J
```

- **Nombre de los archivos:** el prefijo `agenda-auto-` distingue los automáticos de los manuales (`agenda-manual-`) y los de seguridad (`agenda-seguridad-`). Al rotar solo se borran los automáticos (CU-24 4a).
- **El worker** es periódico, con la frecuencia elegida, y no pide condiciones de red ni de carga.

---

## 3. Programación de avisos

Cubre CU-19 y CU-20, RF-38 a RF-43 y RNF-10.

### 3.1 Cuándo suena cada aviso

| Tipo de aviso | Hora en que suena | Tipo de alarma | Se omite si |
|---|---|---|---|
| Actividad con hora | `fecha + hora − aviso_minutos_antes` | `setAlarmClock` | Está cumplida, no tiene aviso o el tipo está desactivado. |
| Actividad sin hora | `fecha` a la hora fija de los ajustes | `setAlarmClock` | Igual que la anterior. |
| Día anterior | `fecha − 1 día` a la hora configurada | `setAlarmClock` | No es examen ni simulacro, está cumplida o está desactivado. |
| Gym | `fecha + hora − minutos de gym` | `setAlarmClock` | Está cumplida, saltada o desactivada. |
| Fin de descanso | `descanso_termina_en` | `setAlarmClock` | La sesión terminó o el descanso se saltó. |
| Clase | Próxima fecha con ese `dia_semana`, `hora_inicio − minutos de clase` | `setAlarmClock` | Está desactivado (por defecto). |
| Resumen del día | Cada día a la hora configurada | `setAlarmClock` | No hay actividades ni tareas atrasadas, o está desactivado. |

Con "Avisos con prioridad de alarma" desactivado, todos usan `setExactAndAllowWhileIdle` y pueden retrasarse en reposo ([arquitectura §4.2](03-arquitectura.md#42-avisos)).

- **Ventana de 7 días:** solo se programan los avisos de los próximos 7 días, para no tener cientos de alarmas activas. La ventana avanza cada día con `MantenimientoDiarioWorker` y con `PendientesAlAbrir`.
- **Código de alarma:** `id × 10 + tipo de aviso`, con el tipo como un dígito del 0 al 5. El mismo aviso siempre tiene el mismo código, así que volver a programarlo reemplaza al anterior en lugar de duplicarlo.
- **Avisos en el pasado:** si la hora calculada ya pasó, no se programa.

### 3.2 Programar al guardar una actividad

```mermaid
sequenceDiagram
    actor U as Usuario
    participant V as FormActividadViewModel
    participant R as ActividadRepository
    participant DB as Room
    participant P as ProgramadorAvisos
    participant AM as AlarmManager

    U->>V: Guardar
    V->>R: guardar(actividad)
    R->>DB: insertar o actualizar
    R->>P: reprogramar(actividad)
    P->>AM: cancelar los códigos de esa actividad
    alt tiene aviso, está pendiente y cae en la ventana
        P->>P: calcular la hora de cada aviso que le toca
        alt canScheduleExactAlarms() y prioridad de alarma activada
            P->>AM: setAlarmClock con el detalle de la actividad como showIntent
        else canScheduleExactAlarms() y prioridad desactivada
            P->>AM: setExactAndAllowWhileIdle
        else sin permiso de alarmas exactas
            P->>AM: setAndAllowWhileIdle, puede llegar tarde
        end
    end
```

Lo mismo pasa al marcar o desmarcar (CU-08), al reprogramar una atrasada (CU-09), al mover o saltar una sesión (CU-15) y al borrar, que solo cancela.

### 3.3 Cuando suena una alarma

```mermaid
flowchart TD
    A[AlarmManager despierta a AvisoReceiver<br>con id y tipo de aviso] --> B{¿Es el resumen del día?}
    B -- Sí --> C[Programar el resumen de mañana]
    C --> D[Leer actividades de hoy y tareas atrasadas]
    D --> E{¿Hay algo?}
    E -- No --> Z0[No mostrar nada]
    E -- Sí --> F[Mostrar el resumen]
    B -- No --> G[Leer la actividad o la clase desde Room]
    G --> H{¿Existe, sigue pendiente<br>y el tipo está activado?}
    H -- No --> Z0
    H -- Sí --> I[Mostrar la notificación en su canal]
    I --> J{¿Es una actividad con check?}
    J -- Sí --> K[Agregar el botón Marcar como cumplida]
    J -- No --> L[Solo abrir el detalle al tocar]
    B -. clase .-> M[Programar la clase de la semana siguiente]
```

El receptor vuelve a leer la base de datos antes de mostrar el aviso. Si la actividad se marcó después de programar la alarma, no se muestra nada (CU-20 2a).

### 3.4 Marcar desde la notificación

```mermaid
sequenceDiagram
    actor U as Usuario
    participant N as Notificación
    participant A as AccionAvisoReceiver
    participant R as ActividadRepository
    participant P as ProgramadorAvisos
    participant NM as NotificationManager

    U->>N: Marcar como cumplida
    N->>A: PendingIntent con el id
    A->>R: marcarCumplida(id)
    R->>P: cancelar los demás avisos de la actividad
    A->>NM: cerrar la notificación
```

El receptor usa `goAsync()` para terminar la escritura en la base de datos sin abrir la app.

### 3.5 Reinicio del celular y cambios de hora

```mermaid
flowchart TD
    A[ArranqueReceiver recibe BOOT_COMPLETED,<br>TIME_SET, TIMEZONE_CHANGED o MY_PACKAGE_REPLACED] --> B[Cancelar todo lo programado]
    B --> C[Leer actividades pendientes de los próximos 7 días]
    C --> D[Leer clases si su aviso está activado]
    D --> E[Programar cada aviso con su código fijo]
    E --> F[Programar el resumen del día]
    F --> G{¿Hay una sesión EN_CURSO con descanso futuro?}
    G -- Sí --> H[Volver a programar el fin del descanso]
    G -- No --> I[Listo]
    H --> I
```

En MIUI, `BOOT_COMPLETED` solo llega si está activado el inicio automático. Si no lo está, este mismo proceso lo hace `PendientesAlAbrir` la próxima vez que se abra la app (sección 6).

---

## 4. Estados de una actividad

Cubre RF-13, RF-14, RF-15, RF-24 y RF-31. En la base de datos solo se guardan `PENDIENTE`, `CUMPLIDA` y `SALTADA` ([modelo de datos §2.2](02-modelo-de-datos.md#actividad)). Los estados de fondo gris se calculan al consultar, comparando la fecha con hoy.

### 4.1 Tarea, charla, simulacro y examen

```mermaid
stateDiagram-v2
    [*] --> Pendiente: crear
    Pendiente --> Cumplida: check
    Cumplida --> Pendiente: quitar check

    Pendiente --> Atrasada: es tarea y pasa su fecha
    Atrasada --> Cumplida: check
    Atrasada --> Pendiente: reprogramar a hoy o después

    Pendiente --> NoMarcada: charla, simulacro o examen y pasa su fecha
    NoMarcada --> Cumplida: check

    Pendiente --> [*]: borrar
    Atrasada --> [*]: borrar
    Cumplida --> [*]: borrar
    NoMarcada --> [*]: borrar

    classDef derivado fill:#e0e0e0,color:#333
    class Atrasada,NoMarcada derivado
```

- **Atrasada:** aparece en la sección destacada del módulo de hoy, con sus días de atraso (RF-14).
- **No marcada:** se queda en su fecha y no pasa a hoy (RF-15).
- **Con check o sin él:** en ambos casos la actividad se puede editar, y al cambiar su fecha vuelve a evaluarse.

### 4.2 Actividad de gym

```mermaid
stateDiagram-v2
    [*] --> Pendiente: la genera la programación
    Pendiente --> Pendiente: mover a otra fecha u hora
    Pendiente --> EnCurso: iniciar sesión
    EnCurso --> Cumplida: guardar la sesión
    EnCurso --> Pendiente: descartar la sesión
    Pendiente --> Saltada: saltar
    Pendiente --> NoRealizada: pasa su fecha sin sesión
    Pendiente --> [*]: cambia la programación y no se había movido

    classDef derivado fill:#e0e0e0,color:#333
    class EnCurso,NoRealizada derivado
```

- **En curso:** la actividad sigue `PENDIENTE` y la sesión está `EN_CURSO`.
- **Cumplida:** se muestra como "Hecha · 52 min".
- **Saltada:** no cuenta como planificada en las estadísticas.
- **No realizada:** sí cuenta como planificada, pero no como hecha.
- **Al cambiar la programación** (CU-14), solo se borran las pendientes futuras que no se movieron.

---

## 5. Generación de las actividades de gym

Cubre CU-14, CU-15 y RF-23.

```mermaid
flowchart TD
    A[Guardar la programación, MantenimientoDiarioWorker<br>o PendientesAlAbrir] --> B{¿Cambió la programación?}
    B -- Sí --> C[Borrar actividades de gym con fecha ≥ hoy,<br>PENDIENTE y fecha = fecha_programada]
    B -- No --> D
    C --> D[Calcular el límite:<br>el menor entre hoy + 4 semanas y la fecha de fin]
    D --> E[Para cada día de gym y cada fecha<br>con ese día de la semana hasta el límite]
    E --> F{¿Ya existe esa actividad<br>dia_gym_id + fecha_programada?}
    F -- Sí --> G[No tocarla: puede estar movida,<br>saltada o hecha]
    F -- No --> H[Crear la actividad PENDIENTE<br>con la hora y la rutina del día]
    G --> I{¿Quedan fechas?}
    H --> I
    I -- Sí --> E
    I -- No --> J[Guardar hasta qué fecha se generó]
    J --> K[Programar los avisos que caen en la ventana de 7 días]
```

- **Sin duplicados:** el índice único `(dia_gym_id, fecha_programada)` evita duplicar actividades aunque la generación corra dos veces.
- **Todo en una transacción:** la generación se hace en una sola transacción, para que el módulo del día no muestre la semana a medias.

---

## 6. Comprobación al abrir

Cubre RNF-07 y RNF-11. La ejecuta `PendientesAlAbrir` cada vez que la app pasa a primer plano. Recupera lo que MIUI pudo haber impedido en segundo plano.

```mermaid
flowchart TD
    A[La app pasa a primer plano] --> B{¿Pasó más de 1 hora<br>desde la última comprobación?}
    B -- No --> Z[Solo recuperar la sesión en curso, sección 1.3]
    B -- Sí --> C[Reprogramar los avisos de los próximos 7 días<br>igual que en 3.5]
    C --> D[Extender la ventana del gym, sección 5]
    D --> E{¿El respaldo automático está atrasado<br>según su frecuencia?}
    E -- Sí --> F[Encolar el respaldo ahora, sección 2.4]
    E -- No --> G
    F --> G[Recuperar la sesión en curso, sección 1.3]
    G --> H[Guardar la hora de esta comprobación]
```

- **Límite de una vez por hora:** evita repetir todo el trabajo cada vez que se vuelve a la app.
- **Seguro de repetir:** todos los pasos dan el mismo resultado aunque se ejecuten varias veces, gracias a los códigos fijos de alarma y al índice único del gym.
