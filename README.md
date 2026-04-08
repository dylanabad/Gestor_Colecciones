# 📦  — Gestor de Colecciones Personales

> Organiza, analiza y da vida a tus colecciones desde un solo lugar.

---

## 📋 Descripción del problema

Muchas personas poseen colecciones personales (libros, videojuegos, cómics, figuras, películas, etc.) pero no disponen de una herramienta digital organizada que les permita gestionarlas correctamente.

El control suele realizarse de forma manual, mediante notas, hojas de cálculo o simplemente de memoria, lo que acaba causando problemas como:

- 🗂️ Desorganización y pérdida de información
- 🔁 Duplicación de elementos
- 💰 Dificultad para conocer el valor total de la colección
- 📤 Falta de seguimiento de préstamos o movimientos
- 📊 Imposibilidad de obtener estadísticas o análisis de la colección

Por tanto, existe la necesidad de desarrollar una aplicación que permita gestionar colecciones de manera **estructurada, visual y eficiente**, incorporando funcionalidades avanzadas como estadísticas, historial y exportación de datos.

---

## 🎯 Objetivos del proyecto

| # | Objetivo |
|---|----------|
| 1 | Permitir crear y gestionar diferentes colecciones |
| 2 | Implementar un sistema **CRUD** completo para los ítems |
| 3 | Incorporar filtros y búsqueda avanzada |
| 4 | Registrar historial de movimientos de los elementos |
| 5 | Generar estadísticas visuales de la colección |
| 6 | Permitir exportar los datos a formatos externos (**CSV / PDF**) |
| 7 | Diseñar una interfaz intuitiva y clara |

---

## ✨ Funcionalidades principales

- **Gestión de colecciones** — Crea colecciones personalizadas de cualquier tipo de ítem.
- **CRUD de ítems** — Añade, edita, consulta y elimina elementos de forma sencilla.
- **Búsqueda y filtros** — Encuentra cualquier ítem rápidamente con filtros avanzados.
- **Historial de movimientos** — Registra préstamos, devoluciones y cambios de estado.
- **Estadísticas visuales** — Visualiza el estado de tu colección con gráficos y métricas.
- **Exportación de datos** — Exporta tu colección a CSV o PDF con un solo clic.

---

## 🚀 Instalación y uso

```bash
# Clona el repositorio
git clone https://github.com/tu-usuario/collecthub.git

# Entra en el directorio
cd collecthub

# Instala las dependencias
npm install

# Inicia la aplicación
npm run dev
```

---

## 🛠️ Tecnologías utilizadas



- **Frontend:** Kotlin
- **Backend:** Spring Boot
- **Base de datos:** MariaDB y Room

---

## 📁 Estructura del proyecto

```
collecthub/
├── src/
│   ├── components/       # Componentes de la interfaz
│   ├── pages/            # Vistas principales
│   ├── services/         # Lógica de negocio y API
│   ├── models/           # Modelos de datos
│   └── utils/            # Utilidades (exportación, filtros, etc.)
├── public/
├── tests/
└── README.md
```

---

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Si deseas colaborar:

1. Haz un fork del repositorio
2. Crea una rama para tu funcionalidad (`git checkout -b feature/nueva-funcionalidad`)
3. Realiza tus cambios y haz commit (`git commit -m 'feat: añade nueva funcionalidad'`)
4. Sube los cambios (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

---


---

<div align="center">
  Hecho con ❤️ para los coleccionistas de todo el mundo.
</div>
