# Event.ly — Gestión de Eventos

Aplicación Android desarrollada en Kotlin para la gestión de eventos, asistencia, comentarios y control de gastos personales, con autenticación y base de datos en tiempo real mediante Firebase.

---

## Tabla de contenidos

- [Descripción](#descripción)
- [Funcionalidades](#funcionalidades)
- [Roles de usuario](#roles-de-usuario)
- [Tecnologías y dependencias](#tecnologías-y-dependencias)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Configuración del entorno](#configuración-del-entorno)
- [Integrantes](#integrantes)

---

## Descripción

Event.ly es una aplicación móvil que permite a los usuarios explorar eventos, confirmar su asistencia, dejar comentarios con calificación, y llevar un registro de gastos personales. Los organizadores cuentan con herramientas adicionales para crear y administrar sus propios eventos. Toda la información se sincroniza en tiempo real a través de Cloud Firestore.

---

## Funcionalidades

### Autenticación
- Registro con correo y contraseña (con validación de formato)
- Inicio de sesión con correo y contraseña
- Inicio de sesión con Google (Google Sign-In)
- Cierre de sesión con confirmación

### Eventos
- Listado de todos los eventos disponibles con búsqueda y filtros
- Vista de detalle por evento (título, descripción, fecha, hora, lugar e imagen)
- Confirmación o cancelación de asistencia
- Historial de eventos a los que el usuario asistió

### Comentarios y calificaciones
- Publicar comentarios con texto y calificación de 1 a 5 estrellas en cada evento
- Listado de comentarios en tiempo real

### Gastos personales
- Registro de gastos con nombre, monto, categoría y fecha
- Resumen mensual con total y gráfica circular (PieChart) por categoría
- Historial completo de gastos

### Estadísticas
- **Usuario:** gráfica de asistencias (asistió / no asistió) y eventos por mes
- **Organizador:** total de eventos creados y distribución mensual

### Perfil
- Información del usuario (nombre, correo, foto, tipo de cuenta)
- Conteo de eventos asistidos y comentarios realizados

---

## Roles de usuario

| Rol | Capacidades |
|---|---|
| `usuario` | Ver eventos, confirmar asistencia, comentar, registrar gastos, ver historial y estadísticas personales |
| `organizador` | Todo lo anterior + crear eventos propios y ver estadísticas de sus eventos |

---

## Tecnologías y dependencias

| Tecnología | Uso |
|---|---|
| Kotlin | Lenguaje principal |
| Android SDK 35 (min API 24) | Plataforma objetivo |
| ViewBinding | Acceso seguro a vistas |
| Firebase Authentication | Autenticación de usuarios |
| Cloud Firestore | Base de datos en tiempo real |
| Google Sign-In | Autenticación con cuenta Google |
| MPAndroidChart | Gráficas PieChart y BarChart |
| Glide 4.16 | Carga y caché de imágenes |
| RecyclerView + DiffUtil | Listas eficientes con animaciones |
| Material Design 3 | Componentes de UI |
| ActivityResultLauncher | API moderna para resultados entre actividades |

---

## Estructura del proyecto

```
app/src/main/java/com/foro_2/
│
├── AppConstants.kt           # Constantes globales: Roles, AttendanceStatus, Collections
├── FirestoreUtil.kt          # Capa de acceso a Firestore (listeners, queries, escrituras)
├── SystemUIHelper.kt         # Configuración de barras del sistema
│
├── Modelos
│   ├── Event.kt
│   ├── Expense.kt
│   ├── Attendance.kt
│   ├── Comment.kt
│   └── HistoryEntry.kt
│
├── Adaptadores
│   ├── EventsAdapter.kt          # Lista de eventos con DiffUtil
│   ├── CommentsAdapter.kt        # Lista de comentarios con DiffUtil
│   └── HistoryEventAdapter.kt    # Historial de eventos con DiffUtil
│
└── Activities
    ├── SplashActivity.kt
    ├── WelcomeActivity.kt        # Pantalla de bienvenida + Google Sign-In
    ├── LoginActivity.kt
    ├── RegisterActivity.kt
    ├── HomeActivity.kt           # Dashboard: resumen mensual y gráfica de gastos
    ├── EventsListActivity.kt     # Listado y búsqueda de eventos
    ├── EventDetailsActivity.kt   # Detalle del evento, asistencia y comentarios
    ├── CommentsActivity.kt       # Comentarios de un evento
    ├── CreateEventActivity.kt    # Crear evento (solo organizadores)
    ├── HistoryActivity.kt        # Historial de eventos asistidos
    ├── CreateExpenseActivity.kt  # Registrar un gasto
    ├── ViewExpensesActivity.kt   # Ver lista de gastos
    ├── StatisticsActivity.kt     # Estadísticas con gráficas
    └── ProfileActivity.kt        # Perfil del usuario
```

---

## Configuración del entorno

### Requisitos previos
- Android Studio Hedgehog o superior
- JDK 11
- Cuenta de Firebase con un proyecto configurado

### Pasos

1. Clona el repositorio:
   ```bash
   git clone https://github.com/davidvalles1102/DSM-GestionEventos.git
   ```

2. Abre el proyecto en Android Studio y espera a que Gradle sincronice.

3. Configura Firebase:
   - Crea un proyecto en [Firebase Console](https://console.firebase.google.com/)
   - Habilita **Authentication** con los proveedores: correo/contraseña y Google
   - Habilita **Cloud Firestore** en modo de prueba o configura las reglas de seguridad
   - Descarga el archivo `google-services.json` y colócalo en la carpeta `app/`

4. Ejecuta la app en un emulador o dispositivo físico con API 24 o superior.

---

## Integrantes

Actividad: Segundo Proyecto en Android con Kotlin

| # | Nombre | Carnet |
|---|---|---|
| 1 | Jonathan José Flamenco López | FL161275 |
| 2 | Raquel Abigail Cortez Mata | CM162199 |
| 3 | Melvin Alexander Soriano Quijada | SQ242789 |
| 4 | David Alberto Valles Gómez | VG240553 |
| 5 | Herbert William Solano Vásquez | SV202844 |
