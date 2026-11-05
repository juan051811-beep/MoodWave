# 🌊 MoodWave — Diario emocional y análisis de hábitos

Aplicación Android de registro emocional, visualización estadística y análisis exploratorio de patrones. Desarrollada como **Proyecto 1 de Programación de Dispositivos Móviles** en el **Instituto Politécnico Nacional, Unidad Profesional Interdisciplinaria de Ingeniería Campus Tlaxcala (UPIIT)**.

> **Proyecto académico desarrollado por dos integrantes:**
>
> - **Juan De Dios Cocoletzi Musito**
> - **Joel Rojas Jiménez**

## ¿Qué es MoodWave?

MoodWave permite registrar cómo te sientes, tu nivel de energía, las actividades realizadas, horas de sueño y una reflexión personal. A partir de los registros, ofrece calendarios, tendencias, estadísticas y herramientas para explorar relaciones entre hábitos y estados emocionales. **No realiza diagnósticos médicos ni interpreta asociaciones como relaciones causales.**

## Funcionalidades

| Módulo | Función |
| --- | --- |
| 🏠 Inicio | Resumen de actividad, recordatorios y acceso a Health Connect. |
| ✍️ Registro emocional | Estado de ánimo en 5 niveles, energía de 1 a 10, actividades, sueño y reflexión. |
| 📚 Historial | Consulta de registros almacenados en el dispositivo. |
| 📆 Calendario | Navegación de los registros por fecha. |
| 📊 Estadísticas | Resúmenes, medidas descriptivas, tendencias, mapa de calor y comparación actividad–ánimo. |
| 🧠 Análisis de patrones | Agrupamiento K-means y detección de aumentos y descensos sostenidos. |
| 👣 Health Connect | Lectura autorizada de pasos diarios, sincronización local y comparación con ánimo/energía. |
| 🔔 Recordatorios | Programación diaria de notificaciones. |
| 🧪 Datos de demostración | Generadores de hasta 300 registros **sintéticos**, identificables y separados de la medición real de pasos. |

## Tecnologías

- **Kotlin** y **Jetpack Compose** para la aplicación Android.
- **Navigation Compose** y **Android ViewModel** para navegación y estado.
- **Room** (SQLite) para persistencia local de emociones y pasos.
- **Coroutines / Flow** para operaciones asíncronas y observación de datos.
- **Health Connect** (solo permiso de lectura de pasos).
- **JUnit** para pruebas de los motores analíticos.
- **Gradle (Kotlin DSL)** para construcción.

## Requisitos e instalación

- **Android Studio** con Android SDK y JDK compatibles con el proyecto.
- Dispositivo o emulador con **Android 8.0 / API 26 o superior**.
- Acceso a SDK **37** para la compilación, según `app/build.gradle.kts`.
- Health Connect compatible y datos disponibles **solo si se desea probar el módulo de pasos**.

Pasos detallados: [docs/INSTALACION.md](docs/INSTALACION.md).

```bash
git clone URL_DEL_REPOSITORIO
cd MoodWave
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

El comando `git clone` se debe actualizar con el enlace real cuando se publique el repositorio. En Windows, usa `gradlew.bat`.

## Arquitectura

```text
app/src/main/java/com/example/pp1jd/
├── MainActivity.kt                 # Navegación principal
├── ui/                             # Pantallas, componentes, tema y ViewModel
├── data/local/                     # Room, entidades, DAO y generadores sintéticos
├── repository/                     # Acceso a los registros
├── analytics/                      # Estadísticas, K-means, anomalías, correlaciones
├── healthconnect/                  # Lectura, sincronización y privacidad de pasos
└── recordatorios/                  # Alarmas y notificaciones
```

Más información en [Arquitectura](docs/ARQUITECTURA.md) y [Algoritmos](docs/ALGORITMOS.md).

## Datos de demostración

El proyecto incluye `GeneradorDatosRealistas.kt` (90 entradas) y `GeneradorDatosRealistasV2.kt` (210 entradas adicionales). Los datos generados se identifican como **sintéticos** y se usan para ilustrar gráficos, estadísticas y análisis. **No son datos clínicos ni de una persona real.** Los pasos no son simulados por estos generadores: provienen de Health Connect cuando el usuario autoriza su lectura.

**Importante:** la versión de demostración incluye lógica de carga automática de datos sintéticos. Revísala antes de utilizar MoodWave con información personal o distribuirla fuera del ámbito académico.

## Capturas de pantalla

Agrega imágenes de las pantallas Inicio, Nuevo registro, Historial, Calendario, Estadísticas, IA y Health Connect en `docs/capturas/`. No incluyas entradas personales, datos de salud identificables ni información privada.

## Pruebas

Ejecutar la suite local:

```bash
./gradlew testDebugUnitTest
```

Las pruebas incluidas cubren agrupamiento, anomalías y correlación pasos–estado de ánimo. Consulta [docs/PRUEBAS.md](docs/PRUEBAS.md).

## Privacidad y limitaciones

- Los datos emocionales se almacenan localmente mediante Room.
- Health Connect requiere autorización para leer pasos; los datos sincronizados se conservan en una tabla local hasta su eliminación.
- No se implementó exportación/restauración manual de registros ni copia cifrada en la nube.
- No se afirma que exista cifrado de extremo a extremo ni protección por PIN/biometría.
- La aplicación ofrece **análisis exploratorio**, no diagnósticos ni recomendaciones médicas.

Lee [docs/PRIVACIDAD.md](docs/PRIVACIDAD.md).

## Autores

Proyecto realizado conjuntamente por:

**Juan De Dios Cocoletzi Musito**  
**Joel Rojas Jiménez**

**Instituto Politécnico Nacional — UPIIT**  
**Asignatura:** Programación de Dispositivos Móviles

> La documentación acredita a ambos integrantes. El historial de contribuciones de GitHub depende de las cuentas y commits realizados por cada persona, no solo del contenido de este README.

## Licencia

**No se ha seleccionado una licencia de distribución.** Si se desea permitir reutilización, modificación o distribución del código por terceros, ambos integrantes deben acordar y agregar una licencia adecuada.
