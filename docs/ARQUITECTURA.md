# Arquitectura y organización del proyecto

## Diseño general

MoodWave utiliza una arquitectura local y reactiva. La interfaz se construye con **Jetpack Compose**, comparte el estado de un **DiaryViewModel**, consulta los registros mediante un **RegistroRepository** y los almacena con **Room**.

```text
Usuario
  ↓
Pantallas Compose (Inicio, Registro, Historial, Calendario, Estadísticas, IA)
  ↓
DiaryViewModel
  ├── RegistroRepository → RegistroDao → Room: tabla registros
  ├── MotorAgrupamientoIA / MotorAnomaliasIA
  └── Observación reactiva de registros mediante Flow

Tarjeta Health Connect → HealthConnectManager
                      → SincronizadorPasos
                      → PasosDiariosDao → Room: tabla pasos_diarios

Tarjeta de recordatorio → RecordatorioManager
                        → AlarmManager → BroadcastReceiver → notificación
```

## Estructura verificada del código

- `MainActivity.kt`: configura la navegación de seis pantallas de Compose.
- `ui/viewmodel/DiaryViewModel.kt`: estado del formulario, guardado, observación del historial, ejecución de IA e importación de datos sintéticos.
- `data/local/RegistroEntity.kt`: entidad `registros` para fechas, ánimo, energía, actividades, reflexión, sueño y marca `esSintetico`.
- `data/local/PasosDiariosEntity.kt`: entidad `pasos_diarios` con fecha, pasos y momento de actualización.
- `data/local/RegistroDao.kt` y `PasosDiariosDao.kt`: operaciones de lectura y escritura.
- `data/local/MoodWaveDatabase.kt`: base Room **versión 3** y migraciones de versiones 1→2 y 2→3.
- `repository/RegistroRepository.kt`: capa de acceso a los registros emocionales.
- `analytics/`: cálculos estadísticos y algoritmos de exploración de patrones.
- `healthconnect/`: lectura autorizada de pasos, sincronización, información de privacidad y eliminación de la copia local de pasos.
- `recordatorios/`: programación, envío y restauración de recordatorios al reiniciar según eventos admitidos por Android.

## Ciclo de un registro

1. El usuario selecciona ánimo, energía, actividades y, opcionalmente, sueño y reflexión.
2. `DiaryViewModel.saveCurrentEntry()` valida y construye un `RegistroEntity`.
3. `RegistroRepository` persiste el registro en Room.
4. El flujo de Room emite una lista actualizada y el ViewModel actualiza el historial.
5. Los motores de K-means y anomalías se recalculan sobre los registros válidos.
6. Las pantallas de estadísticas y análisis consumen los datos actualizados.

## Versionado de la base

- **V1**: registros emocionales.
- **V2**: columna `esSintetico` para distinguir registros generados.
- **V3**: tabla `pasos_diarios`, separada de la información emocional.

No se usa migración destructiva en `MoodWaveDatabase`.

## Datos de ejemplo

- `GeneradorDatosRealistas.kt`: 90 entradas sintéticas.
- `GeneradorDatosRealistasV2.kt`: 210 entradas adicionales.
- `DiaryViewModel.kt`: controla la importación por versión mediante preferencias y lógica para evitar duplicaciones.

La generación se diseñó para pruebas del producto académico. Los pasos de Health Connect no se fabrican para completar correlaciones.
