# Pruebas y verificación

## Pruebas unitarias incluidas

Archivos fuente:

- `app/src/test/java/com/example/pp1jd/analytics/MotoresIATest.kt` — casos de agrupamiento K-means, manejo de varios registros por día, mínimos de información y detección de anomalías sostenidas.
- `app/src/test/java/com/example/pp1jd/analytics/MotorPasosAnimoTest.kt` — correlaciones positivas y negativas, mínimos de días comparables, datos constantes, días duplicados, faltantes y límites de período.
- `app/src/test/java/com/example/pp1jd/ExampleUnitTest.kt` — prueba de plantilla.
- `app/src/androidTest/java/com/example/pp1jd/ExampleInstrumentedTest.kt` — prueba instrumentada de plantilla.

## Comandos

```bash
./gradlew testDebugUnitTest
```

Para ejecutar solamente las pruebas específicas:

```bash
./gradlew testDebugUnitTest --tests "com.example.pp1jd.analytics.MotoresIATest"
./gradlew testDebugUnitTest --tests "com.example.pp1jd.analytics.MotorPasosAnimoTest"
```

## Validaciones manuales recomendadas

1. Abrir la aplicación, verificar navegación entre Inicio, Registro, Historial, Calendario, Estadísticas e IA.
2. Guardar un registro personal y comprobar su aparición en las vistas.
3. Verificar que se distingan registros personales de sintéticos y que la importación no produzca duplicados al reiniciar.
4. Comprobar que los análisis respondan a las modificaciones de registros.
5. Autorizar Health Connect, sincronizar pasos y verificar la visualización de valores reales.
6. Eliminar la **copia local de pasos**, comprobar que el historial emocional permanece y volver a sincronizar.
7. Activar una notificación de prueba y un recordatorio programado; recordar que el sistema puede retrasar las alarmas aproximadas.

## Estado de verificación

Durante el desarrollo del proyecto se reportaron compilaciones y pruebas unitarias exitosas, así como pruebas manuales en dispositivo Android. **Este paquete de publicación se preparó a partir del ZIP recibido; no se ejecutó una nueva compilación de Android ni las pruebas Gradle en el entorno de preparación.** Para verificar exactamente esta copia, ejecutar los comandos anteriores en Android Studio o una máquina configurada con Android SDK.
