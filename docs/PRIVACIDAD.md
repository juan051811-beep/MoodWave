# Privacidad y tratamiento de datos

MoodWave es un proyecto académico que procesa información potencialmente sensible.

## Qué datos maneja

- Registro voluntario de estado de ánimo, energía, actividades, reflexión y horas de sueño.
- Fecha y metadatos necesarios para organizar el historial.
- Pasos diarios leídos desde Health Connect **solo tras autorización**.
- Registros sintéticos para demostrar las funcionalidades de análisis.

## Almacenamiento

Los registros emocionales se almacenan en Room/SQLite (`registros`). La copia local de pasos se almacena en una tabla separada (`pasos_diarios`). El código incluye una pantalla de privacidad de Health Connect y una opción para borrar **los pasos locales** sin eliminar los registros emocionales.

## Permisos

El `AndroidManifest.xml` declara `android.permission.health.READ_STEPS`, `POST_NOTIFICATIONS` y `RECEIVE_BOOT_COMPLETED`. El primero se usa para consultar pasos; los demás para mostrar y restaurar recordatorios programados.

## Límites actuales de protección

- **No** existe en esta versión una función propia de exportación/restauración de emociones.
- **No** se implementó respaldo cifrado en la nube ni cifrado de extremo a extremo.
- **No** se declara implementada protección por PIN o biometría.
- El `AndroidManifest.xml` mantiene `android:allowBackup="true"`; la configuración y el comportamiento de las copias dependen de Android y del dispositivo. No deben interpretarse como un mecanismo de restauración garantizado ni como respaldo cifrado propio.
- Al desinstalar la aplicación, los datos locales pueden perderse.

## Datos sintéticos y resultados

Los 300 registros de demostración no corresponden a una persona real. Los análisis pueden detectar patrones en estos datos, pero **no deben interpretarse como resultados clínicos, diagnósticos ni conclusiones acerca de una persona**. No se simulan pasos para fabricar asociaciones con Health Connect.

## Publicación en GitHub

Este repositorio debe contener **código fuente y documentación**, nunca exportaciones de bases de datos con información personal, historiales de salud, capturas con información identificable, tokens o claves de firma.
