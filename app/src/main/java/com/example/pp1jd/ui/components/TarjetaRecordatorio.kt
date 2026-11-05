
package com.example.pp1jd.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.pp1jd.recordatorios.RecordatorioManager

import kotlinx.coroutines.delay

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================
// COLORES DE LA TARJETA
// ============================================

private val MoradoRecordatorio =
    Color(0xFF7C3AED)

private val FondoMorado =
    Color(0xFFF3E8FF)

private val TextoRecordatorio =
    Color(0xFF1F2937)

private val GrisRecordatorio =
    Color(0xFF6B7280)

// ============================================
// TARJETA PRINCIPAL DE RECORDATORIOS
// ============================================

@Composable
fun TarjetaRecordatorio() {

    val context = LocalContext.current

    // ========================================
    // LEER CONFIGURACION GUARDADA
    // ========================================

    var configuracion by remember {

        mutableStateOf(
            RecordatorioManager.obtenerConfiguracion(
                context
            )
        )
    }

    // ========================================
    // HORA QUE SELECCIONA EL USUARIO
    // ========================================

    var horaSeleccionada by remember {

        mutableIntStateOf(
            configuracion.hora
        )
    }

    var minutoSeleccionado by remember {

        mutableIntStateOf(
            configuracion.minuto
        )
    }

    // ========================================
    // RELOJ PARA EL CONTADOR
    // ========================================

    var ahoraMillis by remember {

        mutableLongStateOf(
            System.currentTimeMillis()
        )
    }

    // ========================================
    // ESTADO DE LOS PERMISOS
    // ========================================

    var permisoDisponible by remember {

        mutableStateOf(
            RecordatorioManager
                .tienePermisoNotificaciones(context)
        )
    }

    // ========================================
    // MENSAJES PARA EL USUARIO
    // ========================================

    var mensaje by remember {
        mutableStateOf<String?>(null)
    }

    // ========================================
    // ACTIVAR RECORDATORIO
    // ========================================

    fun activarRecordatorio() {

        RecordatorioManager.activar(
            context = context,
            hora = horaSeleccionada,
            minuto = minutoSeleccionado
        )

        configuracion =
            RecordatorioManager.obtenerConfiguracion(
                context
            )

        mensaje = "Recordatorio diario activado."
    }

    // ========================================
    // SOLICITAR PERMISO EN ANDROID 13+
    // ========================================
    //
    // Android mostrara una ventana
    // para permitir o rechazar
    // las notificaciones.
    //
    // Solo activamos el recordatorio
    // cuando se concede el permiso.

    val solicitarPermiso =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts
                    .RequestPermission()

        ) { concedido ->

            permisoDisponible =
                RecordatorioManager
                    .tienePermisoNotificaciones(context)

            if (
                concedido &&
                permisoDisponible
            ) {

                activarRecordatorio()

            } else {

                mensaje =
                    "Las notificaciones no están " +
                            "habilitadas. Puedes activarlas " +
                            "en los ajustes de Android."
            }
        }

    // ========================================
    // ACTUALIZAR EL RELOJ CADA SEGUNDO
    // ========================================
    //
    // LaunchedEffect solo funciona
    // mientras esta tarjeta aparece
    // en la interfaz.
    //
    // Cuando se cierra la app,
    // AlarmManager sigue encargado
    // de los recordatorios reales.

    LaunchedEffect(context) {

        while (true) {

            ahoraMillis =
                System.currentTimeMillis()

            // Actualizar el estado si hubo
            // cambios fuera de la pantalla.

            val configuracionActual =
                RecordatorioManager
                    .obtenerConfiguracion(context)

            if (
                configuracionActual != configuracion
            ) {

                configuracion =
                    configuracionActual

                horaSeleccionada =
                    configuracionActual.hora

                minutoSeleccionado =
                    configuracionActual.minuto
            }

            permisoDisponible =
                RecordatorioManager
                    .tienePermisoNotificaciones(context)

            delay(1000L)
        }
    }

    // ========================================
    // CALCULAR PROXIMO RECORDATORIO
    // ========================================

    val proximoRecordatorio =
        RecordatorioManager
            .proximaNotificacionMillis(
                context = context,
                ahoraMillis = ahoraMillis
            )

    // ========================================
    // CALCULAR SEGUNDOS RESTANTES
    // ========================================

    val segundosRestantes =

        if (
            configuracion.activo &&
            proximoRecordatorio != null
        ) {

            (
                    (
                            proximoRecordatorio -
                                    ahoraMillis
                            ).coerceAtLeast(0L) + 999L
                    ) / 1000L

        } else {

            0L
        }

    // ========================================
    // CONVERTIR A HORAS, MINUTOS, SEGUNDOS
    // ========================================

    val horas = segundosRestantes / 3600L

    val minutos =
        (segundosRestantes % 3600L) / 60L

    val segundos =
        segundosRestantes % 60L

    // ========================================
    // FORMATEAR HORA ELEGIDA
    // ========================================

    val horaTexto = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        horaSeleccionada,
        minutoSeleccionado
    )

    // ========================================
    // FORMATEAR PROXIMA FECHA
    // ========================================

    val proximaFechaTexto =

        if (proximoRecordatorio != null) {

            SimpleDateFormat(
                "dd/MM/yyyy · HH:mm",
                Locale.getDefault()
            ).format(
                Date(proximoRecordatorio)
            )

        } else {

            "Sin programar"
        }

    // ========================================
    // TARJETA VISUAL
    // ========================================

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(24.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),

            verticalArrangement =
                Arrangement.spacedBy(15.dp)
        ) {

            // =================================
            // ENCABEZADO Y INTERRUPTOR
            // =================================

            Row(
                modifier = Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier.weight(1f),

                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = "🔔 Tu próximo recordatorio",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoRecordatorio
                    )

                    Text(
                        text = if (configuracion.activo) {
                            "Recordatorio diario activado"
                        } else {
                            "Recordatorio desactivado"
                        },
                        fontSize = 12.sp,
                        color = GrisRecordatorio
                    )
                }

                Switch(
                    checked = configuracion.activo,

                    onCheckedChange = {
                            activar ->

                        if (activar) {

                            // Crear primero el canal.

                            RecordatorioManager
                                .crearCanalNotificaciones(
                                    context
                                )

                            // Android 13 o superior:
                            // solicitar permiso
                            // cuando sea necesario.

                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.TIRAMISU &&

                                context.checkSelfPermission(
                                    Manifest.permission
                                        .POST_NOTIFICATIONS
                                ) !=
                                PackageManager
                                    .PERMISSION_GRANTED
                            ) {

                                solicitarPermiso.launch(
                                    Manifest.permission
                                        .POST_NOTIFICATIONS
                                )

                            } else if (
                                RecordatorioManager
                                    .tienePermisoNotificaciones(
                                        context
                                    )
                            ) {

                                activarRecordatorio()

                            } else {

                                mensaje =
                                    "Las notificaciones están " +
                                            "bloqueadas en Android. " +
                                            "Revisa sus permisos."
                            }

                        } else {

                            // Desactivar y cancelar.

                            RecordatorioManager
                                .desactivar(context)

                            configuracion =
                                RecordatorioManager
                                    .obtenerConfiguracion(
                                        context
                                    )

                            mensaje =
                                "Recordatorio desactivado."
                        }
                    },

                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor =
                                Color.White,

                            checkedTrackColor =
                                MoradoRecordatorio
                        )
                )
            }

            HorizontalDivider(
                color = Color(0xFFE5E7EB)
            )

            // =================================
            // CONTADOR DE TIEMPO
            // =================================

            Text(
                text = "Tiempo hasta el próximo aviso",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = GrisRecordatorio
            )

            if (configuracion.activo) {

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    UnidadContador(
                        valor = String.format(
                            Locale.getDefault(),
                            "%02d",
                            horas
                        ),

                        etiqueta = "Horas",

                        modifier = Modifier.weight(1f)
                    )

                    UnidadContador(
                        valor = String.format(
                            Locale.getDefault(),
                            "%02d",
                            minutos
                        ),

                        etiqueta = "Minutos",

                        modifier = Modifier.weight(1f)
                    )

                    UnidadContador(
                        valor = String.format(
                            Locale.getDefault(),
                            "%02d",
                            segundos
                        ),

                        etiqueta = "Segundos",

                        modifier = Modifier.weight(1f)
                    )
                }

            } else {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            FondoMorado,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(18.dp),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "Activa el recordatorio para comenzar la cuenta regresiva.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MoradoRecordatorio
                    )
                }
            }

            // =================================
            // HORA CONFIGURADA
            // =================================

            Text(
                text = "Hora diaria: $horaTexto",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoRecordatorio
            )

            Text(
                text = "Próximo horario programado: $proximaFechaTexto",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = GrisRecordatorio
            )

            // =================================
            // BOTON PARA CAMBIAR LA HORA
            // =================================

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),

                onClick = {

                    TimePickerDialog(
                        context,

                        {
                                _, horaNueva, minutoNuevo ->

                            horaSeleccionada = horaNueva

                            minutoSeleccionado =
                                minutoNuevo

                            // Si estaba activo,
                            // actualizar tambien
                            // la alarma real.

                            if (configuracion.activo) {

                                RecordatorioManager
                                    .activar(
                                        context,
                                        horaNueva,
                                        minutoNuevo
                                    )

                                configuracion =
                                    RecordatorioManager
                                        .obtenerConfiguracion(
                                            context
                                        )

                                mensaje =
                                    "Se actualizó la hora " +
                                            "del recordatorio."
                            }
                        },

                        horaSeleccionada,
                        minutoSeleccionado,

                        true // Reloj de 24 horas.

                    ).show()
                }
            ) {

                Text(
                    text = "🕒 Elegir hora del recordatorio",
                    color = MoradoRecordatorio
                )
            }

            // =================================
            // BOTON PARA PROBAR NOTIFICACION
            // =================================

            Button(
                modifier = Modifier.fillMaxWidth(),

                enabled = configuracion.activo,

                onClick = {

                    val enviada =
                        RecordatorioManager
                            .enviarNotificacionPrueba(
                                context
                            )

                    mensaje =

                        if (enviada) {

                            "Notificación de prueba enviada."

                        } else {

                            "No se pudo mostrar la notificación. " +
                                    "Verifica los permisos."
                        }
                },

                colors = ButtonDefaults.buttonColors(
                    containerColor = MoradoRecordatorio
                )
            ) {

                Text(
                    text = "🔔 Probar notificación"
                )
            }

            // =================================
            // ACCESO A PERMISOS DE ANDROID
            // =================================
            //
            // Se muestra si los recordatorios
            // estan activos pero Android
            // tiene las notificaciones
            // bloqueadas, o si el usuario
            // recibio un mensaje de permiso.

            if (
                !permisoDisponible &&
                (
                        configuracion.activo ||
                                mensaje != null
                        )
            ) {

                TextButton(
                    modifier = Modifier.fillMaxWidth(),

                    onClick = {

                        val intent =
                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.O
                            ) {

                                Intent(
                                    Settings
                                        .ACTION_APP_NOTIFICATION_SETTINGS
                                ).apply {

                                    putExtra(
                                        Settings.EXTRA_APP_PACKAGE,
                                        context.packageName
                                    )
                                }

                            } else {

                                Intent(
                                    Settings
                                        .ACTION_APPLICATION_DETAILS_SETTINGS,

                                    Uri.parse(
                                        "package:${context.packageName}"
                                    )
                                )
                            }

                        context.startActivity(intent)
                    }
                ) {

                    Text(
                        text = "Abrir permisos de notificaciones"
                    )
                }
            }

            // =================================
            // MENSAJE DE RESULTADO
            // =================================

            mensaje?.let { texto ->

                Text(
                    text = texto,
                    fontSize = 12.sp,
                    color = MoradoRecordatorio
                )
            }

            // =================================
            // NOTA DE PUNTUALIDAD
            // =================================

            Text(
                text = "La cuenta regresiva indica la hora programada. Android puede retrasar ligeramente la entrega para ahorrar batería.",
                fontSize = 10.sp,
                color = GrisRecordatorio
            )
        }
    }
}

// ============================================
// UNIDAD DEL CONTADOR
// ============================================
//
// Componente reutilizable para mostrar
// horas, minutos y segundos.

@Composable
private fun UnidadContador(
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .background(
                FondoMorado,
                RoundedCornerShape(14.dp)
            )
            .padding(
                vertical = 14.dp,
                horizontal = 6.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {

        Text(
            text = valor,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = MoradoRecordatorio
        )

        Text(
            text = etiqueta,
            fontSize = 11.sp,
            color = GrisRecordatorio
        )
    }
}
