# Matriz de trazabilidad

Estado: en revisión · Base: todos los documentos anteriores

---

La matriz conecta cada requerimiento con su caso de uso, los datos que usa, las pantallas donde se ve, las pruebas que lo verifican y el incremento en que llega. Sirve para dos cosas:

- **Ver el impacto de un cambio:** si cambia un RF, aquí aparecen los casos de uso, tablas, pantallas y pruebas que hay que revisar.
- **Cerrar un incremento:** un RF solo se marca como verificado cuando pasan todas sus pruebas (criterio 6 del [plan de pruebas §4](06-plan-de-pruebas.md#4-criterios-de-aceptación)).

**Columnas**

- **CU:** casos de uso, de los [casos de uso §3](01-casos-de-uso.md#3-lista-de-casos-de-uso).
- **Datos:** tablas de Room del [modelo de datos](02-modelo-de-datos.md); *Ajustes* es DataStore y *Archivo* es el respaldo JSON.
- **Pantallas:** las del [diseño de interfaz §2](04-diseno-de-interfaz.md#2-mapa-de-navegación).
- **Pruebas:** los casos del [plan de pruebas §3](06-plan-de-pruebas.md#3-casos-de-prueba-por-incremento).
- **Inc.:** el incremento.
- **Verificado:** se llena al cerrar el incremento con la fecha y la versión (`versionName`). Todos empiezan en "—".

---

## 1. Requerimientos funcionales

### 1.1 Materias y horario académico

| RF | Resumen | CU | Datos | Pantallas | Pruebas | Inc. | Verificado |
|---|---|---|---|---|---|---|---|
| RF-01 | Guiar el registro inicial sin materias | CU-01 | `materia` | Bienvenida | CP-1.01 | 1 | 2026-10-31 · 1.1 |
| RF-02 | Registrar materia con nombre y color | CU-02 | `materia` | Materias, Formulario de materia | CP-1.03 | 1 | 2026-10-31 · 1.1 |
| RF-03 | Editar y eliminar materia | CU-02 | `materia`, `clase`, `actividad` | Materias, Formulario de materia | CP-1.04 | 1 | 2026-10-31 · 1.1 |
| RF-04 | Eliminar todas las materias | CU-03 | `materia`, `clase`, `actividad` | Materias | CP-1.05, CP-3.14 | 1 y 3 | 2026-10-31 · 3.0 |
| RF-05 | Registrar clase | CU-04 | `clase` | Formulario de clase | CP-1.06 | 1 | 2026-10-31 · 1.1 |
| RF-06 | Consultar horario semanal | CU-05 | `clase`, `materia` | Horario | CP-1.09 | 1 | 2026-10-31 · 1.1 |
| RF-07 | Editar y eliminar clase | CU-04 | `clase` | Formulario de clase, Detalle de clase | CP-1.14 | 1 | 2026-10-31 · 1.1 |
| RF-08 | Repetir clases cada semana | CU-04 | `clase` | Hoy, Semana, Horario | CP-1.08 | 1 | 2026-10-31 · 1.1 |

### 1.2 Actividades

| RF | Resumen | CU | Datos | Pantallas | Pruebas | Inc. | Verificado |
|---|---|---|---|---|---|---|---|
| RF-09 | Crear actividad | CU-06 | `actividad` | Formulario de actividad | CP-2.01 | 2 | 2026-10-31 · 2.0 |
| RF-10 | Consultar y filtrar actividades | CU-07 | `actividad`, `materia` | Actividades | CP-2.05 | 2 | 2026-10-31 · 2.0 |
| RF-11 | Editar actividad | CU-06 | `actividad` | Formulario de actividad, Detalle de actividad | CP-2.03 | 2 | 2026-10-31 · 2.0 |
| RF-12 | Eliminar actividad | CU-06 | `actividad` | Detalle de actividad | CP-2.04 | 2 | 2026-10-31 · 2.0 |
| RF-13 | Marcar y desmarcar con check | CU-08 | `actividad` | Hoy, Actividades | CP-2.06 | 2 | 2026-10-31 · 2.0 |
| RF-14 | Tareas atrasadas en el módulo de hoy | CU-09 | `actividad` | Hoy | CP-2.07, CP-2.09 | 2 | 2026-10-31 · 2.0 |
| RF-15 | Exámenes, simulacros y charlas no pasan a hoy | CU-10 | `actividad` | Hoy | CP-2.08 | 2 | 2026-10-31 · 2.0 |
| RF-16 | Próximas actividades en el detalle de clase | CU-05 | `actividad`, `clase` | Detalle de clase | CP-2.10 | 2 | 2026-10-31 · 2.0 |

### 1.3 Módulo del día

| RF | Resumen | CU | Datos | Pantallas | Pruebas | Inc. | Verificado |
|---|---|---|---|---|---|---|---|
| RF-17 | Clases y actividades del día por hora | CU-10 | `clase`, `actividad`, `materia` | Hoy | CP-1.11, CP-2.11 | 1 y 2 | 2026-10-31 · 2.0 |
| RF-18 | Abrir en el día actual | CU-10 | — | Hoy | CP-1.02 | 1 | 2026-10-31 · 1.1 |
| RF-19 | Cambiar de día | CU-10 | — | Hoy | CP-1.10 | 1 | 2026-10-31 · 1.1 |
| RF-20 | Marcar desde el módulo | CU-08 | `actividad` | Hoy | CP-2.06 | 2 | 2026-10-31 · 2.0 |
| RF-21 | Agregar con la fecha del día | CU-06 | `actividad` | Hoy, Formulario de actividad | CP-2.02 | 2 | 2026-10-31 · 2.0 |
| RF-22 | Abrir detalle, o rutina si es gym | CU-10 | `actividad` | Hoy, Detalle de actividad, Sesión de gym | CP-2.13, CP-5.37 | 2 y 5 | Parcial: 2026-10-31 · 2.0 (detalle; la rutina de gym en el inc. 5) |

### 1.4 Gym

| RF | Resumen | CU | Datos | Pantallas | Pruebas | Inc. | Verificado |
|---|---|---|---|---|---|---|---|
| RF-23 | Programar el gym por semana | CU-14 | `dia_gym`, `actividad`, Ajustes | Programación del gym | CP-5.10, CP-5.11, CP-5.12, CP-5.15 | 5 | — |
| RF-24 | Mover o saltar una sesión | CU-15 | `actividad` | Detalle de actividad, Gym | CP-5.13, CP-5.14 | 5 | — |
| RF-25 | Gestionar rutinas | CU-12 | `rutina` | Rutinas, Detalle de rutina | CP-5.01, CP-5.08, CP-5.09 | 5 | — |
| RF-26 | Gestionar ejercicios como bloques reordenables | CU-13 | `ejercicio` | Detalle de rutina, Formulario de ejercicio | CP-5.02 a CP-5.07 | 5 | — |
| RF-27 | Iniciar sesión con cronómetro | CU-16 | `sesion_gym` | Sesión de gym | CP-5.20 | 5 | — |
| RF-28 | Contador de series, prellenado y orden en la sesión | CU-16 | `sesion_gym`, `serie_realizada`, `ejercicio` | Sesión de gym | CP-5.21, CP-5.22, CP-5.26 a CP-5.28 | 5 | — |
| RF-29 | Descansos y cuenta regresiva | CU-16 | `sesion_gym` | Sesión de gym | CP-5.22 a CP-5.25, CP-5.33 | 5 | — |
| RF-30 | Notificación fija de la sesión | CU-16 | `sesion_gym` | Notificación de la sesión | CP-5.33 | 5 | — |
| RF-31 | Guardar sesión; no realizada si no se hizo | CU-16 | `sesion_gym`, `serie_realizada`, `actividad` | Sesión de gym, Hoy | CP-5.29 a CP-5.31 | 5 | — |
| RF-32 | Registros de peso, estatura e IMC | CU-17 | `registro_peso`, Ajustes | Peso e IMC | CP-6.01 a CP-6.05 | 6 | — |
| RF-33 | Estadísticas con gráficas | CU-18 | `sesion_gym`, `serie_realizada`, `actividad`, `registro_peso` | Estadísticas | CP-6.06 a CP-6.10 | 6 | — |

### 1.5 Vistas jerárquicas

| RF | Resumen | CU | Datos | Pantallas | Pruebas | Inc. | Verificado |
|---|---|---|---|---|---|---|---|
| RF-34 | Vista de semana | CU-11 | `clase`, `actividad` | Semana | CP-4.01, CP-4.05 | 4 | 2026-10-31 · 4.0 |
| RF-35 | Vista de mes | CU-11 | `actividad` | Mes | CP-4.02 | 4 | 2026-10-31 · 4.0 |
| RF-36 | Mes → semana → día | CU-11 | — | Mes, Semana, Hoy | CP-4.03 | 4 | 2026-10-31 · 4.0 |
| RF-37 | Señal de días pasados sin check | CU-11 | `actividad` | Semana, Mes | CP-4.04 | 4 | 2026-10-31 · 4.0 |

### 1.6 Notificaciones

| RF | Resumen | CU | Datos | Pantallas | Pruebas | Inc. | Verificado |
|---|---|---|---|---|---|---|---|
| RF-38 | Aviso por actividad, con hora fija si no tiene hora | CU-19, CU-20 | `actividad`, Ajustes | Formulario de actividad, Ajustes y avisos | CP-2.20, CP-2.21, CP-2.23, CP-2.24 | 2 | 2026-10-31 · 2.0 (manuales CP-2.32 a CP-2.38 hechas por el usuario) |
| RF-39 | Aviso del día anterior en exámenes y simulacros | CU-19, CU-20 | `actividad`, Ajustes | Ajustes y avisos | CP-2.22 | 2 | 2026-10-31 · 2.0 (manuales CP-2.32 a CP-2.38 hechas por el usuario) |
| RF-40 | Aviso de gym con la rutina | CU-20 | `actividad`, `dia_gym`, Ajustes | Ajustes y avisos | CP-5.16 | 5 | — |
| RF-41 | Aviso de clases, desactivado por defecto | CU-19, CU-20 | `clase`, Ajustes | Ajustes y avisos | CP-2.26 | 2 | 2026-10-31 · 2.0 (manuales CP-2.32 a CP-2.38 hechas por el usuario) |
| RF-42 | Resumen del día | CU-20 | `actividad`, Ajustes | Ajustes y avisos | CP-2.27 | 2 | 2026-10-31 · 2.0 (manuales CP-2.32 a CP-2.38 hechas por el usuario) |
| RF-43 | Marcar desde la notificación, activar por tipo, reprogramar al reiniciar | CU-08, CU-19, CU-20 | `actividad`, Ajustes | Notificación, Ajustes y avisos | CP-2.25, CP-2.30, CP-2.31, CP-2.33, CP-2.37 | 2 | 2026-10-31 · 2.0 (manuales CP-2.32 a CP-2.38 hechas por el usuario) |

### 1.7 Respaldo

| RF | Resumen | CU | Datos | Pantallas | Pruebas | Inc. | Verificado |
|---|---|---|---|---|---|---|---|
| RF-44 | Respaldar en la carpeta, y copia en otra ubicación | CU-21 | Todas las tablas, Ajustes, Archivo | Respaldo | CP-3.04, CP-3.05, CP-3.17 | 3 | 2026-10-31 · 3.0 |
| RF-45 | Restaurar desde la lista de la carpeta u otra ubicación | CU-22 | Todas las tablas, Ajustes, Archivo | Respaldo, Bienvenida | CP-3.06 a CP-3.10 | 3 | 2026-10-31 · 3.0 |
| RF-46 | Respaldo automático, últimos 5 | CU-23, CU-24 | Todas las tablas, Ajustes, Archivo | Respaldo | CP-3.11, CP-3.13 | 3 | 2026-10-31 · 3.0 (automático cubierto por pruebas unitarias) |
| RF-47 | Registrar la carpeta, último respaldo y advertencia | CU-23 | Ajustes | Respaldo, Ajustes | CP-3.03, CP-3.12, CP-3.15, CP-3.16 | 3 | 2026-10-31 · 3.0 |

---

## 2. Requerimientos no funcionales

Los RNF valen para toda la app, así que se verifican en cada incremento desde el que aplican.

| RNF | Resumen | Dónde se resuelve | Pruebas | Desde inc. | Verificado |
|---|---|---|---|---|---|
| RNF-01 · Plataforma | Android 8.0+, Kotlin y Compose; probada en Android 12 con MIUI 13 | [Arquitectura §5.1](03-arquitectura.md#51-base-del-proyecto), §4.6 | CP-1.13 | 1 | 2026-10-31 · 1.1 |
| RNF-02 · Alcance | Un usuario, sin cuentas ni servidor | Arquitectura §1, §4.5 (sin permiso de internet) | Criterio de calidad "Sin cuentas ni red" | 1 | — |
| RNF-03 · Almacenamiento | Room local, funciona sin internet | [Modelo de datos](02-modelo-de-datos.md) | Criterio de calidad "Sin internet" | 1 | — |
| RNF-04 · Persistencia | No perder datos; instalar encima con la misma firma | [Modelo de datos §5](02-modelo-de-datos.md#5-versionado-y-migraciones), [arquitectura §5.3](03-arquitectura.md#53-firma-versiones-e-instalación) | CP-1.12, CP-1.15 a CP-1.19, CP-2.12, CP-3.18, CP-5.32, CP-6.11 | 1 | Parcial: 2026-10-31 · 3.0 (firma, instalación encima, migración 1→2 y reinstalación con restauración) |
| RNF-05 · Respaldo | Fuera de la app, en una carpeta registrada | Arquitectura §4.4 | CP-3.15, CP-3.18 | 3 | 2026-10-31 · 3.0 (reinstalación CP-3.18 hecha por el usuario) |
| RNF-06 · Respaldo | JSON con versiones y fecha | [Diagramas §2.2](05-diagramas-de-comportamiento.md#22-respaldar-ahora) | CP-3.01, CP-3.02, CP-3.10 | 3 | 2026-10-31 · 3.0 |
| RNF-07 · Confiabilidad | Cronómetro desde la hora guardada y servicio en primer plano | Arquitectura §4.3, [diagramas §1.3](05-diagramas-de-comportamiento.md#13-recuperar-una-sesión-después-de-un-cierre) | CP-5.34, CP-5.35 | 5 | — |
| RNF-08 · Usabilidad | Un toque para marcar y para confirmar una serie | [Diseño de interfaz §1](04-diseno-de-interfaz.md#1-principios-de-diseño) | CP-2.06, CP-5.22 | 2 | 2026-10-31 · 2.0 |
| RNF-09 · Mantenibilidad | MVVM por capas | [Arquitectura §2](03-arquitectura.md#2-capas-mvvm) | Criterio de calidad "Capas MVVM" | 1 | — |
| RNF-10 · Notificaciones | Avisos con ±1 minuto, con alarmas del sistema | Arquitectura §4.2, [diagramas §3](05-diagramas-de-comportamiento.md#3-programación-de-avisos) | CP-2.28, CP-2.29, CP-2.32, CP-2.38 | 2 | 2026-10-31 · 2.0 (manuales CP-2.32 y CP-2.38 hechas por el usuario) |
| RNF-11 · MIUI | Guía de MIUI y comprobación al abrir | Arquitectura §4.6, [diagramas §6](05-diagramas-de-comportamiento.md#6-comprobación-al-abrir) | CP-2.34, CP-2.35, CP-2.36, CP-3.13 | 2 | 2026-10-31 · 2.0 (manuales CP-2.33 a CP-2.36 hechas por el usuario) |

---

## 3. Cobertura

| Elemento | Total | Con caso de uso | Con prueba |
|---|---|---|---|
| Requerimientos funcionales | 47 | 47 | 47 |
| Requerimientos no funcionales | 11 | — | 11 (8 con casos de prueba, 3 con criterios de calidad) |
| Casos de uso | 24 | — | 24, a través de las pruebas de sus RF |

**Requerimientos por incremento**

| Incremento | RF | Cantidad |
|---|---|---|
| 1 | RF-01 a RF-08, RF-17 a RF-19 | 11 |
| 2 | RF-09 a RF-16, RF-20 a RF-22, RF-38, RF-39, RF-41 a RF-43 | 16 |
| 3 | RF-44 a RF-47 | 4 |
| 4 | RF-34 a RF-37 | 4 |
| 5 | RF-23 a RF-31, RF-40 | 10 |
| 6 | RF-32, RF-33 | 2 |

RF-04, RF-17 y RF-22 se completan en un incremento posterior (respaldo antes de borrar, actividades del día y rutina de gym, respectivamente), y se cuentan en el incremento donde empiezan.

**Cómo se mantiene la matriz**

1. Si cambia un RF, se actualiza primero su fila aquí y luego cada documento que aparece en ella.
2. Si se agrega un caso de prueba, su columna "Verifica" en el plan de pruebas debe nombrar el RF, y se agrega aquí.
3. Al cerrar un incremento, se llena "Verificado" con la fecha y el `versionName`, por ejemplo `2026-11-20 · 1.0`.
