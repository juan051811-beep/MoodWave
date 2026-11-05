# Instalación y ejecución

## Requisitos

- Android Studio con JDK y Android SDK compatibles con las versiones declaradas en `app/build.gradle.kts`.
- Android SDK **37** para compilar el proyecto en su configuración actual.
- Teléfono o emulador Android **API 26+**.
- Conexión para descargar dependencias Gradle la primera vez.

## Abrir el proyecto

1. Clona el repositorio o descarga su código fuente.
2. Abre **Android Studio → Open** y selecciona la raíz del repositorio (donde se encuentra `settings.gradle.kts`).
3. Espera la sincronización de Gradle. Android Studio puede crear `local.properties` con la ruta local al SDK: **no lo compartas ni lo subas**.
4. Selecciona un dispositivo o emulador compatible y ejecuta la configuración `app`.

## Terminal

En macOS/Linux, desde la raíz:

```bash
chmod +x gradlew
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

En Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

## Health Connect

El módulo de pasos es opcional. Requiere que el dispositivo tenga Health Connect disponible, datos de pasos y que se autorice a MoodWave el acceso **READ_STEPS**. El acceso puede revocarse desde los ajustes de Health Connect. La aplicación no escribe pasos en Health Connect.

## Datos de prueba

El ViewModel incluye importación controlada de los generadores V1 y V2 (90 + 210 registros). Los datos generados están marcados como sintéticos. Esta característica permite mostrar análisis incluso en una instalación sin historial personal.

## Problemas frecuentes

- **No compila**: confirma la versión del SDK, disponibilidad del JDK y conexión para descargar dependencias.
- **No hay pasos**: revisa la disponibilidad de Health Connect, el permiso y la existencia de mediciones.
- **No aparece la notificación emergente**: Android puede mostrar avisos en el panel sin superponer una ventana; el comportamiento también depende de la configuración del canal de notificaciones.
- **No llegan exactamente a la hora**: las alarmas usadas por la app son aproximadas y Android puede retrasarlas.
- **Se desinstaló la app**: los datos locales pueden perderse; no existe una función propia de exportación y restauración.

## Precaución

No publiques datos personales, claves de firma, capturas de salud identificables, bases de datos reales ni archivos locales de Android Studio.
