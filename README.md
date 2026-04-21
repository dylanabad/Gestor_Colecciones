# CollectHub | Gestor de Colecciones (Android + API)

Aplicación Android para gestionar colecciones e ítems (con imágenes), lista de deseos, logros y un sistema de préstamos entre usuarios.

El proyecto está pensado como **offline-first**:
- **Room** como persistencia/caché local.
- **Spring Boot + JWT + MySQL/MariaDB** como backend para persistencia remota, multiusuario y sincronización.

---

## Funcionalidades
- **Colecciones**
  - Crear/editar/eliminar (imagen, color, descripción).
  - Contadores y valor total.
  - Papelera y borrado definitivo.
- **Ítems**
  - CRUD, categoría, estado, valoración, descripción.
  - Imagen desde **galería o cámara** (guardada en galería) + subida al backend.
  - Papelera y borrado definitivo.
  - Favoritos (y ordenación prioritaria).
- **Etiquetas (Tags)**
  - Crear/eliminar y asignar a ítems.
- **Lista de deseos**
  - CRUD + papelera + borrado definitivo.
- **Préstamos**
  - Prestar ítems a otros usuarios, vista de **Prestados** y **Recibidos**.
  - Fecha prevista/real, estado, notas, eliminación.
  - Notificaciones asociadas.
- **Logros**
  - Desbloqueo y persistencia por usuario.
- **Perfil de coleccionista**
  - Avatar (foto), nombre visible y bio (persisten en backend).
- **Temas**
  - Selector de paleta y modo.
- **Widget**
  - Resumen con contadores.

---

## Arquitectura (Android)
- **MVVM + Repository**
- **Coroutines + Flow**
- **ViewBinding**
- **Room** para persistencia local
- **Retrofit + OkHttp** para API (JWT en `Authorization: Bearer <token>`)
- **Glide** para carga de imágenes

---

## Stack
### Android
- Kotlin
- Material Components (Material 3)
- Room (`room-runtime`, `room-ktx`, `kapt`)
- Retrofit + Gson
- OkHttp + Logging Interceptor
- Glide

### Backend
- Spring Boot
- Spring Security + JWT
- Hibernate/JPA
- MySQL/MariaDB
- Subida de ficheros a `uploads/` (se devuelve `/uploads/<uuid>.<ext>`)

---

## Estructura del proyecto (Android)
```text
app/src/main/java/com/example/gestor_colecciones/
├── activities/      # Activities (host)
├── adapters/        # Adapters (RecyclerView)
├── auth/            # AuthStore, repos/viewmodels de auth
├── dao/             # DAOs de Room
├── database/        # DatabaseProvider y migraciones
├── entities/        # Entidades de Room
├── fragment/        # Pantallas (Colecciones, Items, Deseos, Préstamos, Perfil, Papelera...)
├── model/           # Modelos/UI state
├── network/         # ApiService, ApiProvider, DTOs, helpers (uploads)
├── repository/      # Repositorios (sync, papelera, préstamos, etc.)
├── util/            # Temas, imágenes, helpers
└── viewmodel/       # ViewModels + factories
```

---

## Requisitos
- Android Studio actualizado
- Configuración Android del proyecto:
  - `minSdk = 24`
  - `targetSdk = 36`
  - Java/Kotlin target: `11`
- Emulador o dispositivo
- Para backend: Java + MySQL/MariaDB

---

## Cómo ejecutar en local
### 1) Backend (Spring Boot)
Ruta: `Api_Colecciones/ApiColecciones`

1. Arranca MySQL/MariaDB.
2. Revisa `src/main/resources/application.properties` (URL, user, pass, puerto).
3. Ejecuta la aplicación Spring Boot desde tu IDE.

Notas:
- Por defecto: `http://localhost:8080`
- Hibernate: `spring.jpa.hibernate.ddl-auto=update`
- Subidas: carpeta `uploads/` y rutas `/uploads/**`

### 2) Android (emulador)
Ruta: `Gestor_Colecciones`

1. Abre el proyecto en Android Studio.
2. Ejecuta en emulador.

Importante:
- En emulador, `10.0.2.2` apunta al `localhost` de tu PC.
- La app usa `http://10.0.2.2:8080` como base URL para conectar con el backend en local.

---

## Endpoints clave (resumen)
- Auth: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/login-strict`
- Perfil: `GET /api/usuarios/me`, `PUT /api/usuarios/me`
- Uploads: `POST /api/uploads`
- Colecciones/Ítems/Deseos: CRUD + papelera + hard delete
- Préstamos: `POST /api/prestamos`, `PUT /api/prestamos/{id}/devolver`, `GET /api/prestamos/prestados`, `GET /api/prestamos/recibidos`, `DELETE /api/prestamos/{id}/hard`

---

## Troubleshooting
- **No conecta al backend desde el emulador**
  - Usa `10.0.2.2` (no `localhost`) y comprueba que el backend está levantado.
- **HTTP 400**
  - Revisa el mensaje exacto en `Logcat` (Android) y en los logs del backend.
- **Imágenes**
  - Comprueba que el backend sirve `/uploads/**` y que `uploads/` exista.
- **Permisos de cámara**
  - Revisa que el emulador/dispositivo tenga concedido `android.permission.CAMERA` y almacenamiento si aplica.

---

## Nota
Este repositorio está en evolución: se añaden funcionalidades manteniendo sincronización con backend y persistencia local.

