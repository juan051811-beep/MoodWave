
package com.example.pp1jd.recordatorios

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.content.pm.PackageManager

import android.os.Build

import com.example.pp1jd.MainActivity

import java.util.Calendar

// ============================================
// CONFIGURACION DEL RECORDATORIO
// ============================================
//
// Valores predeterminados:
// - Desactivado inicialmente.
// - Hora sugerida: 20:00.
//
// El usuario podra cambiar estos valores
// desde la interfaz de MoodWave.

data class ConfiguracionRecordatorio(
    val activo: Boolean = false,
    val hora: Int = 20,
    val minuto: Int = 0
)

// ============================================
// MOTOR PRINCIPAL DE RECORDATORIOS
// ============================================

object RecordatorioManager {

    // ========================================
    // CONSTANTES
    // ========================================

    private const val PREFERENCIAS =
        "moodwave_recordatorios"

    private const val CLAVE_ACTIVO =
        "recordatorio_activo"

    private const val CLAVE_HORA =
        "recordatorio_hora"

    private const val CLAVE_MINUTO =
        "recordatorio_minuto"

    private const val ID_CANAL =
        "moodwave_recordatorios_diarios"

    private const val ID_NOTIFICACION =
        3701

    private const val CODIGO_ALARMA =
        3702

    private const val CODIGO_APERTURA =
        3703

    const val ACCION_RECORDATORIO =
        "com.example.pp1jd.RECORDATORIO_DIARIO"

    // ========================================
    // LEER CONFIGURACION GUARDADA
    // ========================================
    //
    // SharedPreferences permite conservar
    // la hora y el estado del recordatorio
    // aunque se cierre la aplicacion.

    fun obtenerConfiguracion(
        context: Context
    ): ConfiguracionRecordatorio {

        val preferencias =
            context.applicationContext
                .getSharedPreferences(
                    PREFERENCIAS,
                    Context.MODE_PRIVATE
                )

        return ConfiguracionRecordatorio(
            activo = preferencias.getBoolean(
                CLAVE_ACTIVO,
                false
            ),

            hora = preferencias.getInt(
                CLAVE_HORA,
                20
            ).coerceIn(0, 23),

            minuto = preferencias.getInt(
                CLAVE_MINUTO,
                0
            ).coerceIn(0, 59)
        )
    }

    // ========================================
    // ACTIVAR RECORDATORIO
    // ========================================
    //
    // Guarda la configuracion y programa
    // la proxima notificacion.
    //
    // No activamos recordatorios sin
    // la accion expresa del usuario.

    fun activar(
        context: Context,
        hora: Int,
        minuto: Int
    ) {

        require(hora in 0..23) {
            "La hora debe estar entre 0 y 23."
        }

        require(minuto in 0..59) {
            "El minuto debe estar entre 0 y 59."
        }

        val preferencias =
            context.applicationContext
                .getSharedPreferences(
                    PREFERENCIAS,
                    Context.MODE_PRIVATE
                )

        preferencias.edit()
            .putBoolean(
                CLAVE_ACTIVO,
                true
            )
            .putInt(
                CLAVE_HORA,
                hora
            )
            .putInt(
                CLAVE_MINUTO,
                minuto
            )
            .apply()

        crearCanalNotificaciones(context)

        reprogramar(context)
    }

    // ========================================
    // DESACTIVAR RECORDATORIO
    // ========================================
    //
    // Desactiva la configuracion
    // y cancela la alarma programada.

    fun desactivar(
        context: Context
    ) {

        val preferencias =
            context.applicationContext
                .getSharedPreferences(
                    PREFERENCIAS,
                    Context.MODE_PRIVATE
                )

        preferencias.edit()
            .putBoolean(
                CLAVE_ACTIVO,
                false
            )
            .apply()

        cancelarAlarma(context)
    }

    // ========================================
    // CALCULAR PROXIMA NOTIFICACION
    // ========================================
    //
    // Utilizamos la hora local del telefono.
    //
    // Si hoy todavia no llega la hora,
    // programamos para hoy.
    //
    // Si la hora ya paso,
    // programamos para mañana.
    //
    // El calculo es dinamico.

    fun proximaNotificacionMillis(
        context: Context,
        ahoraMillis: Long =
            System.currentTimeMillis()
    ): Long? {

        val configuracion =
            obtenerConfiguracion(context)

        if (!configuracion.activo) {
            return null
        }

        val calendario =
            Calendar.getInstance().apply {

                timeInMillis = ahoraMillis

                set(
                    Calendar.HOUR_OF_DAY,
                    configuracion.hora
                )

                set(
                    Calendar.MINUTE,
                    configuracion.minuto
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )

                if (
                    timeInMillis <= ahoraMillis
                ) {

                    add(
                        Calendar.DAY_OF_YEAR,
                        1
                    )
                }
            }

        return calendario.timeInMillis
    }

    // ========================================
    // CREAR PENDINGINTENT DE LA ALARMA
    // ========================================
    //
    // Android utilizara este PendingIntent
    // para llamar al BroadcastReceiver
    // incluso si la aplicacion no esta abierta.

    private fun pendingIntentAlarma(
        context: Context,
        crear: Boolean
    ): PendingIntent? {

        val intent = Intent(
            context,
            RecordatorioReceiver::class.java
        ).apply {

            action = ACCION_RECORDATORIO
        }

        val banderas =

            PendingIntent.FLAG_IMMUTABLE or

                    if (crear) {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        PendingIntent.FLAG_NO_CREATE
                    }

        return PendingIntent.getBroadcast(
            context,
            CODIGO_ALARMA,
            intent,
            banderas
        )
    }

    // ========================================
    // PROGRAMAR LA SIGUIENTE ALARMA
    // ========================================
    //
    // setAndAllowWhileIdle puede ejecutarse
    // cuando el telefono se encuentra
    // en modo de ahorro de energia.
    //
    // No garantiza puntualidad exacta.

    fun reprogramar(
        context: Context
    ) {

        val configuracion =
            obtenerConfiguracion(context)

        if (!configuracion.activo) {

            cancelarAlarma(context)

            return
        }

        val proximaFecha =
            proximaNotificacionMillis(context)
                ?: return

        val alarmaManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val pendingIntent =
            pendingIntentAlarma(
                context = context,
                crear = true
            ) ?: return

        // Cancelar cualquier programacion
        // anterior para evitar duplicados.

        alarmaManager.cancel(
            pendingIntent
        )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {

            alarmaManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                proximaFecha,
                pendingIntent
            )

        } else {

            alarmaManager.set(
                AlarmManager.RTC_WAKEUP,
                proximaFecha,
                pendingIntent
            )
        }
    }

    // ========================================
    // CANCELAR ALARMA
    // ========================================

    private fun cancelarAlarma(
        context: Context
    ) {

        val pendingIntent =
            pendingIntentAlarma(
                context = context,
                crear = false
            ) ?: return

        val alarmaManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        alarmaManager.cancel(
            pendingIntent
        )

        pendingIntent.cancel()
    }

    // ========================================
    // CREAR CANAL DE NOTIFICACIONES
    // ========================================
    //
    // Android 8.0 y superiores necesitan
    // un canal para publicar notificaciones.

    fun crearCanalNotificaciones(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val canal = NotificationChannel(
                ID_CANAL,
                "Recordatorios diarios",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {

                description =
                    "Avisos para registrar tu día en MoodWave"
            }

            val manager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(
                canal
            )
        }
    }

    // ========================================
    // COMPROBAR PERMISO DE NOTIFICACIONES
    // ========================================
    //
    // En Android 13 o superior,
    // el usuario debe conceder permiso.
    //
    // La pantalla de configuracion se
    // encargara de solicitarlo.

    fun tienePermisoNotificaciones(
        context: Context
    ): Boolean {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permiso =
                context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                permiso !=
                PackageManager.PERMISSION_GRANTED
            ) {

                return false
            }
        }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {

            if (!manager.areNotificationsEnabled()) {
                return false
            }
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val canal =
                manager.getNotificationChannel(
                    ID_CANAL
                )

            if (
                canal != null &&
                canal.importance ==
                NotificationManager.IMPORTANCE_NONE
            ) {

                return false
            }
        }

        return true
    }

    // ========================================
    // MOSTRAR NOTIFICACION
    // ========================================
    //
    // Utilizaremos un mensaje breve
    // y respetuoso.
    //
    // No contiene resultados emocionales
    // ni datos privados del usuario.
    //
    // Al tocarla, abrira MoodWave.

    private fun mostrarNotificacion(
        context: Context
    ): Boolean {

        crearCanalNotificaciones(context)

        if (
            !tienePermisoNotificaciones(context)
        ) {

            return false
        }

        val intentAbrir = Intent(
            context,
            MainActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingAbrir =
            PendingIntent.getActivity(
                context,
                CODIGO_APERTURA,
                intentAbrir,

                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val constructor =

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                Notification.Builder(
                    context,
                    ID_CANAL
                )

            } else {

                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

        val notificacion =
            constructor

                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )

                .setContentTitle(
                    "🌊 MoodWave"
                )

                .setContentText(
                    "Es un buen momento para registrar tu día."
                )

                .setContentIntent(
                    pendingAbrir
                )

                .setAutoCancel(true)

                .setCategory(
                    Notification.CATEGORY_REMINDER
                )

                .setVisibility(
                    Notification.VISIBILITY_PRIVATE
                )

                .build()

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        return try {

            manager.notify(
                ID_NOTIFICACION,
                notificacion
            )

            true

        } catch (e: SecurityException) {

            false
        }
    }

    // ========================================
    // NOTIFICACION DE PRUEBA
    // ========================================
    //
    // Permite al usuario comprobar que
    // las notificaciones funcionan
    // sin esperar hasta mañana.
    //
    // NO modifica la hora del recordatorio.

    fun enviarNotificacionPrueba(
        context: Context
    ): Boolean {

        return mostrarNotificacion(context)
    }

    // ========================================
    // ATENDER RECORDATORIO REAL
    // ========================================
    //
    // Primero programamos el siguiente dia.
    //
    // Despues enviamos la notificacion.
    //
    // Si el permiso fue rechazado,
    // la programacion no queda detenida.

    fun atenderRecordatorio(
        context: Context
    ) {

        if (
            !obtenerConfiguracion(context).activo
        ) {
            return
        }

        reprogramar(context)

        mostrarNotificacion(context)
    }
}

// ============================================
// RECEIVER DEL RECORDATORIO DIARIO
// ============================================
//
// Android invoca este componente
// cuando llega la hora programada.
//
// No depende de que la pantalla
// de MoodWave este abierta.

class RecordatorioReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {

        if (
            intent?.action !=
            RecordatorioManager.ACCION_RECORDATORIO
        ) {
            return
        }

        RecordatorioManager.atenderRecordatorio(
            context
        )
    }
}

// ============================================
// RECEIVER DE REINICIO DEL TELEFONO
// ============================================
//
// Las alarmas programadas se pierden
// cuando Android se reinicia.
//
// Este receptor las vuelve a programar
// si el usuario tenia los recordatorios
// activados.
//
// Tambien permite recalcular la hora
// cuando cambia la zona horaria
// o la hora del dispositivo.

class RecordatorioReinicioReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {

        val accion = intent?.action

        if (
            accion == Intent.ACTION_BOOT_COMPLETED ||

            accion ==
            Intent.ACTION_MY_PACKAGE_REPLACED ||

            accion == Intent.ACTION_TIME_CHANGED ||

            accion == Intent.ACTION_TIMEZONE_CHANGED
        ) {

            RecordatorioManager.reprogramar(
                context
            )
        }
    }
}
