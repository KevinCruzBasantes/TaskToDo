# Requerimientos de la Agenda Personal

Fecha: 31 de octubre de 2026 · Autor: Ismael · Versión 2.1 (IMC, gestión de ejercicios y MIUI)

---

## 1. Descripción general

La app es una agenda personal para Android. Reúne el horario académico, las actividades de cada día y el seguimiento del gym, y guarda los datos solo en el celular.

- **Usuario:** una sola persona, el dueño del celular. No hay cuentas ni inicio de sesión.
- **Uso:** personal y no comercial. Se instala desde Android Studio o con un APK, sin pasar por la Play Store.
- **Punto de partida:** la primera vez que se abre, el usuario registra sus materias y su horario académico.
- **Pantalla principal:** el módulo del día, con todo lo que hay que hacer ese día.
- **Datos:** base de datos local, con respaldo en un archivo para recuperarlos después de reinstalar.

---

## 2. Conceptos clave

- **Materia:** asignatura con nombre y color. Agrupa las clases y puede vincularse a actividades.
- **Clase:** bloque del horario académico de una materia, con día de la semana, hora de inicio, hora de fin y aula opcional. Se repite cada semana hasta que se elimina.
- **Actividad:** algo por hacer en una fecha: tarea, charla, simulacro, examen o gym. Funciona como recordatorio y se marca con un check al cumplirla.
- **Actividad atrasada:** tarea que pasó su fecha sin check. Aparece en el módulo de hoy hasta que se marca, se reprograma o se borra.
- **Módulo del día:** la vista de un día con sus clases y actividades en orden de hora. Es la vista más detallada.
- **Vistas jerárquicas:** semana y mes. Muestran la planificación a nivel macro.
- **Programación de gym:** los días de la semana en que el usuario entrena, cada uno con su hora y su rutina. Se repite cada semana.
- **Rutina:** lista ordenada de ejercicios, con un descanso por defecto entre ejercicios. Hay varias rutinas, por ejemplo pierna y torso.
- **Ejercicio:** elemento de una rutina que se mide por repeticiones o por tiempo, con series, peso opcional, descansos opcionales y nota opcional.
- **Sesión de gym:** cada vez que se realiza una actividad de gym, con su inicio, su duración y las series realizadas. Si no se hace, queda como no realizada.
- **Registro de peso:** el peso corporal del usuario en una fecha, para seguir su evolución.
- **IMC:** índice de masa corporal, calculado como el peso en kg dividido entre la estatura en metros al cuadrado. Se calcula para cada registro de peso con la estatura del usuario.
- **Respaldo:** archivo con todos los datos de la app, guardado fuera de ella para restaurarlos después de reinstalar.
- **Carpeta de respaldos:** carpeta del celular que el usuario registra una vez y donde la app guarda todos sus respaldos.

---

## 3. Requerimientos funcionales

Son 47 requerimientos en siete módulos.

### 3.1 Materias y horario académico

- **RF-01.** Si al abrir la app no hay materias registradas, guiar al usuario para registrar sus materias y sus clases.
- **RF-02.** Registrar una materia con nombre y color.
- **RF-03.** Editar y eliminar una materia. Al eliminarla se eliminan sus clases, y sus actividades se conservan sin materia.
- **RF-04.** Eliminar todas las materias de una vez, por ejemplo al terminar el semestre, con confirmación. Se eliminan todas las clases y las actividades se conservan sin materia. Antes de borrar, la app ofrece crear un respaldo.
- **RF-05.** Registrar una clase con materia, día de la semana, hora de inicio, hora de fin y aula opcional.
- **RF-06.** Consultar el horario académico completo de la semana.
- **RF-07.** Editar y eliminar una clase.
- **RF-08.** Repetir las clases cada semana, sin fecha de fin, hasta que se eliminen, y mostrarlas en el módulo del día y en las vistas de semana y mes.

### 3.2 Actividades

- **RF-09.** Crear una actividad con título, tipo (tarea, charla, simulacro, examen o gym), fecha, hora opcional, materia opcional, notas y aviso opcional. Las de tipo gym se generan desde su programación (RF-23).
- **RF-10.** Consultar las actividades, con filtro por tipo y por materia.
- **RF-11.** Editar una actividad.
- **RF-12.** Eliminar una actividad.
- **RF-13.** Marcar una actividad como cumplida con un check, y desmarcarla.
- **RF-14.** Una tarea que pasa su fecha sin check aparece como atrasada en una sección destacada del módulo de hoy, con los días que lleva de atraso. Desde ahí se puede marcar, reprogramar a otra fecha o borrar.
- **RF-15.** Un examen, simulacro o charla que pasa su fecha sin check queda en su fecha como no marcado y no pasa a hoy.
- **RF-16.** En el detalle de una clase, mostrar las próximas actividades de su materia.

### 3.3 Módulo del día

- **RF-17.** Mostrar las clases y actividades de un día ordenadas por hora, con el nombre y el color de la materia cuando la tengan.
- **RF-18.** Abrir la app en el módulo del día actual.
- **RF-19.** Cambiar al día anterior, al siguiente o a una fecha elegida.
- **RF-20.** Marcar actividades como cumplidas desde el módulo.
- **RF-21.** Agregar una actividad desde el módulo, con la fecha de ese día ya puesta.
- **RF-22.** Al tocar una actividad, abrir su detalle; si es de gym, abrir su rutina para iniciar la sesión.

### 3.4 Gym

- **RF-23.** Programar el gym: elegir los días de la semana y, para cada día, su hora y su rutina, con una fecha de fin opcional. La programación se repite cada semana.
- **RF-24.** Mover o saltar una sesión puntual sin cambiar la programación semanal.
- **RF-25.** Registrar, consultar, editar y eliminar rutinas. Cada rutina tiene nombre, una lista ordenada de ejercicios y un descanso entre ejercicios por defecto, que es opcional.
- **RF-26.** Agregar, consultar, editar, eliminar, duplicar y reordenar los ejercicios de una rutina, uno por uno, desde el detalle de la rutina. Cada ejercicio es un bloque que el usuario arrastra a la posición que quiera. Cada ejercicio tiene nombre, forma de medirse (repeticiones o tiempo), series, repeticiones o duración, peso opcional, descanso entre series opcional, descanso tras el ejercicio opcional (reemplaza el de la rutina) y nota opcional.
- **RF-27.** Iniciar una sesión desde una actividad de gym, con un cronómetro de la sesión.
- **RF-28.** Durante la sesión, mostrar un contador de series ("Serie 2 de 4"). Cada serie viene rellenada con lo de la rutina o con lo que se hizo la última vez, y se confirma con un toque o se corrige. Los ejercicios pendientes se pueden mover a cualquier posición durante la sesión, por ejemplo para dejar el más difícil para el final, y al terminar se puede guardar ese orden en la rutina.
- **RF-29.** Al confirmar una serie, iniciar el descanso entre series; al terminar la última serie de un ejercicio, iniciar el descanso entre ejercicios. Solo si están configurados. El descanso se puede alargar o acortar 15 segundos o saltar, y avisa con vibración o sonido al terminar. Los ejercicios por tiempo tienen una cuenta regresiva.
- **RF-30.** Mientras la sesión está activa, mostrar una notificación fija con el cronómetro y el descanso en curso.
- **RF-31.** Guardar cada sesión con su fecha, hora de inicio, duración, rutina y las series realizadas (repeticiones o tiempo, y peso). Una sesión programada que no se hizo queda como no realizada.
- **RF-32.** Registrar el peso corporal con fecha, y consultar, editar o eliminar esos registros. Registrar la estatura del usuario y mostrar el IMC de cada registro con su categoría según la OMS (bajo peso, normal, sobrepeso u obesidad).
- **RF-33.** Mostrar estadísticas con gráficas: minutos entrenados por semana y por mes, sesiones hechas frente a planificadas, evolución del peso corporal y del IMC, y progreso de fuerza por ejercicio (peso máximo y volumen). Con filtro de semana, mes, 3 meses y todo el historial.

### 3.5 Vistas jerárquicas

- **RF-34.** Vista de semana: mostrar las clases y actividades de los 7 días para ver cómo está programada la semana.
- **RF-35.** Vista de mes: mostrar por día un resumen de lo programado para ver la carga del mes.
- **RF-36.** Pasar del mes a la semana y de la semana al módulo del día tocando un día.
- **RF-37.** En las vistas de semana y mes, marcar con una señal discreta los días pasados con actividades sin check.

### 3.6 Notificaciones

- **RF-38.** Avisar de una actividad con la anticipación que elija el usuario. Si la actividad no tiene hora, avisar a una hora fija del día, configurable.
- **RF-39.** Para exámenes y simulacros, dar un aviso extra el día anterior a una hora configurable.
- **RF-40.** Avisar del gym unos minutos antes de su hora, con el nombre de la rutina.
- **RF-41.** Avisar de las clases unos minutos antes. Desactivado por defecto.
- **RF-42.** Enviar un resumen del día en la mañana, con las actividades del día y las tareas atrasadas.
- **RF-43.** Desde la notificación se puede marcar la actividad como cumplida. Las actividades cumplidas no avisan. Cada tipo de aviso se activa o desactiva en los ajustes, y los avisos se reprograman al reiniciar el celular.

### 3.7 Respaldo

- **RF-44.** Exportar un respaldo con todos los datos de la app a la carpeta de respaldos, con un toque. Como opción aparte, guardar una copia en otra ubicación, por ejemplo Google Drive.
- **RF-45.** Importar un respaldo, elegido de la lista de respaldos de la carpeta o buscado en otra ubicación. Mostrar su fecha y lo que contiene, pedir confirmación porque reemplaza los datos actuales, y crear antes un respaldo de seguridad de los datos actuales en la carpeta.
- **RF-46.** Crear un respaldo automático semanal, o con la frecuencia elegida, en la carpeta de respaldos, conservando los últimos 5.
- **RF-47.** Pedir al usuario que registre la carpeta de respaldos la primera vez que se necesite, y permitir cambiarla. Mostrar en los ajustes la carpeta, la fecha del último respaldo, y avisar si pasó más de una semana sin respaldar.

---

## 4. Requerimientos no funcionales

La app es local, de un solo usuario, y no debe perder datos ni al reinstalarla.

- **RNF-01 · Plataforma.** Android 8.0 (API 26) o superior. Desarrollada en Android Studio con Kotlin y Jetpack Compose. El dispositivo principal es un Xiaomi con Android 12 (API 31) y MIUI 13, donde se prueba cada incremento.
- **RNF-02 · Alcance.** Un solo usuario, sin cuentas, sin servidor y sin publicación en tiendas.
- **RNF-03 · Almacenamiento.** Base de datos local en el celular (Room sobre SQLite). La app funciona sin internet.
- **RNF-04 · Persistencia.** Los datos se conservan al cerrar la app, al reiniciar el celular y al instalar versiones nuevas de la app. Cada versión nueva se instala encima de la anterior, sin desinstalarla, y por eso todas las versiones se firman con la misma clave y llevan un número de versión mayor que la anterior.
- **RNF-05 · Respaldo.** El archivo se guarda fuera del almacenamiento privado de la app, porque Android borra los datos internos al desinstalar. Todos los respaldos (manuales, automáticos y de seguridad) van a una sola carpeta del celular que registra el usuario, para tenerlos juntos y no dispersos por el dispositivo. Una app de sincronización externa puede subir esa carpeta a la nube, y el respaldo manual puede además copiarse a Google Drive.
- **RNF-06 · Respaldo.** El archivo es JSON e incluye la versión de la base de datos, la fecha y la versión de la app, para restaurar un respaldo antiguo en una versión nueva.
- **RNF-07 · Confiabilidad.** El cronómetro sigue midiendo aunque la app pase a segundo plano o se cierre: se calcula a partir de la hora de inicio guardada, y un servicio en primer plano mantiene la notificación de la sesión.
- **RNF-08 · Usabilidad.** Marcar una actividad como cumplida, o confirmar una serie en el gym, toma un solo toque.
- **RNF-09 · Mantenibilidad.** Arquitectura MVVM por capas: pantallas, ViewModels, repositorio y base de datos.
- **RNF-10 · Notificaciones.** La app pide el permiso de notificaciones (Android 13 o superior) y usa alarmas exactas para que los avisos lleguen con un margen de ±1 minuto respecto de su hora, también con el celular en reposo. Para lograrlo, todos los avisos usan el mecanismo de alarmas del sistema, el mismo que usa la app de reloj, que no se retrasa en reposo.
- **RNF-11 · Compatibilidad con MIUI.** Los avisos, la notificación de la sesión de gym y el respaldo automático funcionan en MIUI 13, que por defecto limita las apps en segundo plano. La app guía al usuario para activar el inicio automático y quitar las restricciones de batería, y comprueba al abrirse si quedó pendiente un respaldo o un aviso.

---

## 5. Fuera de alcance

Esta versión no incluye lo siguiente. Cualquiera puede sumarse después como una mejora en mantenimiento.

- Publicación en Play Store u otra tienda.
- Varios usuarios, cuentas o inicio de sesión.
- Sincronización en la nube entre dispositivos, o respaldo automático directo a Google Drive con su API.
- Widget en la pantalla de inicio.
- Compartir la agenda con otras personas.
- Semestres con fechas, historial de horarios pasados y días feriados o de vacaciones.
- Estadísticas extra: racha de semanas, récords personales y conteo por rutina.

---

## 6. Decisiones tomadas

Las 10 preguntas abiertas de la versión 1 quedaron cerradas así:

1. **Repetición.** El gym se repite cada semana en los días que elija el usuario, cada día con su propia hora. Las demás actividades son de una sola fecha. (RF-23, RF-24)
2. **Rutina.** Varias rutinas, una asignada a cada día de gym. Ejercicios por repeticiones o por tiempo, con peso, descansos y nota opcionales. (RF-25, RF-26)
3. **Registro de sesión.** Completo: series con repeticiones o tiempo y peso, rellenadas de antemano para confirmar con un toque. Las sesiones no hechas quedan como no realizadas. (RF-28, RF-31)
4. **Estadísticas.** Tiempo entrenado, cumplimiento, peso corporal y progreso de fuerza. (RF-33)
5. **Actividad y materia.** La materia es una entidad con nombre y color, y es un campo opcional de la actividad. (RF-02, RF-09, RF-10, RF-16, RF-17)
6. **Herramientas.** Cronómetro, contador de series, descansos entre series y entre ejercicios configurables y opcionales, cuenta regresiva y notificación fija. (RF-27 a RF-30)
7. **Notificaciones.** Avisos por actividad, el día anterior para exámenes y simulacros, de gym, de clase (desactivado) y un resumen del día. (RF-38 a RF-43)
8. **Forma del respaldo.** Manual con un toque más un automático semanal, ambos en una carpeta de respaldos que registra el usuario. El manual puede copiarse también a Google Drive. (RF-44 a RF-47)
9. **Actividades vencidas.** Las tareas pasan a hoy como atrasadas; los exámenes, simulacros y charlas quedan en su fecha; el gym queda como no realizado. (RF-14, RF-15, RF-37)
10. **Semestre.** Sin semestres. Las clases se repiten hasta eliminarlas, y hay una opción para eliminar todas las materias de una vez. (RF-04, RF-08)

En la versión 2.1 se sumaron estos cambios:

- **IMC.** Se registra la estatura y se muestra el IMC de cada registro de peso, con su evolución en las estadísticas. (RF-32, RF-33)
- **Ejercicios.** Los ejercicios de una rutina tienen su propia gestión: agregar, consultar, editar, eliminar, reordenar y duplicar, uno por uno. El orden también se puede cambiar durante la sesión. (RF-26, RF-28)
- **Dispositivo.** La app se prueba en Android 12 con MIUI 13 y se adapta a sus restricciones en segundo plano. (RNF-01, RNF-11)
- **Carpeta de respaldos.** Todos los respaldos van a una carpeta que el usuario registra una vez. (RF-44 a RF-47, RNF-05)
- **Alarmas del sistema.** Los avisos puntuales usan el mecanismo de alarmas del sistema para no retrasarse. (RNF-10)

---

## 7. Plan de incrementos

El desarrollo va en 6 incrementos. Cada uno pasa por diseño, código y pruebas, y deja la app lista para usar.

1. **Materias, horario académico y módulo del día con las clases.**
   - Cubre RF-01 a RF-08 y RF-17 a RF-19.
2. **Actividades con check, atrasadas y notificaciones dentro del módulo del día.**
   - Cubre RF-09 a RF-16, RF-20 a RF-22, RF-38, RF-39 y RF-41 a RF-43.
3. **Respaldo y restauración.**
   - Cubre RF-44 a RF-47.
4. **Vistas de semana y mes.**
   - Cubre RF-34 a RF-37.
5. **Gym: programación, rutinas, herramientas y registro de sesiones.**
   - Cubre RF-23 a RF-31 y RF-40.
6. **Peso corporal, IMC y telemetría.**
   - Cubre RF-32 y RF-33.

El respaldo va tercero porque desde el incremento 2 usarás la app a diario, y un error de desarrollo podría borrar tus datos. La parte de RF-22 que abre la rutina llega con el incremento 5, y en el incremento 1 la opción de RF-04 de crear un respaldo antes de borrar queda pendiente hasta el incremento 3.
