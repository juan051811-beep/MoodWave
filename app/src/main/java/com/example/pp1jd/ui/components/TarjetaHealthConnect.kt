
package com.example.pp1jd.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult

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

import androidx.health.connect.client.PermissionController

import com.example.pp1jd.healthconnect.EstadoHealthConnect
import com.example.pp1jd.healthconnect.HealthConnectManager
import com.example.pp1jd.healthconnect.HealthConnectPrivacyActivity
import com.example.pp1jd.healthconnect.ResultadoSincronizacionPasos
import com.example.pp1jd.healthconnect.SincronizadorPasos

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

// ============================================
// COLORES
// ============================================

private val MoradoHealth =
    Color(0xFF7C3AED)

private val GrisHealth =
    Color(0xFF6B7280)

private val OscuroHealth =
    Color(0xFF1F2937)

private val VerdeHealth =
    Color(0xFF059669)

// ============================================
// TARJETA PRINCIPAL DE HEALTH CONNECT
// ============================================

@Composable
fun TarjetaHealthConnect() {

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    // ========================================
    // HEALTH CONNECT
    // ========================================

    val manager = remember(context) {

        HealthConnectManager(context)
    }

    // ========================================
    // SINCRONIZADOR CON ROOM
    // ========================================

    val sincronizador = remember(context) {

        SincronizadorPasos(context)
    }

    // ========================================
    // OBSERVAR PASOS ALMACENADOS
    // ========================================
    //
    // Room emite los registros cada vez
    // que se insertan, actualizan o borran.
    //
    // Esta lista incluye todas las fechas
    // que hemos guardado en MoodWave.
    //
    // No depende de los 90 registros
    // emocionales de prueba.

    val flujoPasos = remember(sincronizador) {

        sincronizador.observarPasosGuardados()
    }

    val pasosGuardados by flujoPasos.collectAsState(
        initial = emptyList()
    )

    // ========================================
    // ESTADOS DE HEALTH CONNECT
    // ========================================

    var estado by remember {

        mutableStateOf(
            manager.obtenerEstado()
        )
    }

    var tienePermiso by remember {
        mutableStateOf(false)
    }

    var cargando by remember {
        mutableStateOf(false)
    }

    var mensaje by remember {
        mutableStateOf<String?>(null)
    }

    // ========================================
    // PASOS DE HEALTH CONNECT
    // ========================================

    var pasosHoy by remember {
        mutableStateOf<Long?>(null)
    }

    var pasosAyer by remember {
        mutableStateOf<Long?>(null)
    }

    // ========================================
    // RESULTADO DE LA SINCRONIZACION
    // ========================================
    //
    // Solo se actualiza al ejecutar
    // una sincronizacion con Room.

    var ultimoResultado by remember {

        mutableStateOf<
                ResultadoSincronizacionPasos?
                >(null)
    }

    // ========================================
    // FECHAS ACTUALES
    // ========================================

    val hoy = LocalDate.now()

    val ayer = hoy.minusDays(1)

    // ========================================
    // DATOS YA GUARDADOS EN ROOM
    // ========================================

    val diasAlmacenados =
        pasosGuardados.size

    val pasosHoyGuardados =
        pasosGuardados.firstOrNull {

            it.fechaIso == hoy.toString()

        }?.pasos

    val ultimaActualizacion =
        pasosGuardados.maxOfOrNull {

            it.actualizadoEn
        }

    // ========================================
    // CONSULTAR Y SINCRONIZAR
    // ========================================
    //
    // sincronizar = false:
    //
    // Consulta Health Connect,
    // pero no guarda pasos en Room.
    //
    // sincronizar = true:
    //
    // Consulta los ultimos 7 dias
    // y guarda los valores validos.
    //
    // Despues actualiza los totales
    // de hoy y ayer en la interfaz.

    suspend fun actualizarDatos(
        sincronizar: Boolean = false
    ) {

        cargando = true

        mensaje = null

        try {

            // =================================
            // COMPROBAR DISPONIBILIDAD
            // =================================

            estado = manager.obtenerEstado()

            if (
                estado !=
                EstadoHealthConnect.DISPONIBLE
            ) {

                tienePermiso = false

                pasosHoy = null
                pasosAyer = null

                return
            }

            // =================================
            // COMPROBAR AUTORIZACION
            // =================================

            tienePermiso =
                manager.tienePermisoPasos()

            if (!tienePermiso) {

                pasosHoy = null
                pasosAyer = null

                mensaje =
                    "Concede el permiso para " +
                            "consultar tus pasos."

                return
            }

            // =================================
            // SINCRONIZAR CON ROOM
            // =================================

            if (sincronizar) {

                ultimoResultado =
                    sincronizador
                        .sincronizarUltimosDias(
                            cantidadDias = 7
                        )
            }

            // =================================
            // CONSULTAR PASOS ACTUALES
            // =================================

            pasosHoy =
                manager.leerPasosDelDia(
                    LocalDate.now()
                )

            pasosAyer =
                manager.leerPasosDelDia(
                    LocalDate.now().minusDays(1)
                )

            // =================================
            // MENSAJE DE SINCRONIZACION
            // =================================

            if (sincronizar) {

                val resultado =
                    ultimoResultado

                if (resultado != null) {

                    mensaje =

                        if (
                            resultado.diasGuardados > 0
                        ) {

                            "Sincronización completada: " +
                                    "${resultado.diasGuardados} " +
                                    "día(s) con pasos guardados " +
                                    "o actualizados en Room."

                        } else {

                            "La consulta terminó, " +
                                    "pero no se encontraron " +
                                    "días con datos de pasos " +
                                    "para guardar."
                        }
                }
            }

        } catch (
            error: CancellationException
        ) {

            throw error

        } catch (
            error: Exception
        ) {

            mensaje =
                "No se pudo completar la consulta: " +
                        (
                                error.localizedMessage
                                    ?: "Error desconocido."
                                )

        } finally {

            cargando = false
        }
    }

    // ========================================
    // SOLICITAR PERMISOS
    // ========================================
    //
    // Se abre la ventana de Health Connect.
    //
    // El usuario decide si autoriza
    // la lectura de pasos.
    //
    // Conceder permisos NO inicia
    // automaticamente el guardado local.
    //
    // Para guardar datos se utiliza
    // el boton de sincronizacion.

    val solicitarPermisos =
        rememberLauncherForActivityResult(

            contract =
                PermissionController
                    .createRequestPermissionResultContract()

        ) { permisosConcedidos ->

            tienePermiso =
                permisosConcedidos.containsAll(
                    manager.permisosNecesarios
                )

            if (tienePermiso) {

                scope.launch {

                    actualizarDatos(
                        sincronizar = false
                    )
                }

            } else {

                mensaje =
                    "No se concedió el permiso " +
                            "para leer los pasos."
            }
        }

    // ========================================
    // CONSULTA INICIAL
    // ========================================
    //
    // Al abrir la pantalla, consultamos
    // disponibilidad y pasos actuales.
    //
    // No escribimos en Room hasta que
    // el usuario pulse el boton.

    LaunchedEffect(manager) {

        actualizarDatos(
            sincronizar = false
        )
    }

    // ========================================
    // FORMATEAR NUMEROS
    // ========================================

    val formatoNumero = remember {

        NumberFormat.getNumberInstance(
            Locale.forLanguageTag("es-MX")
        )
    }

    fun mostrarPasos(
        cantidad: Long?
    ): String {

        return if (cantidad == null) {

            "Sin datos"

        } else {

            formatoNumero.format(cantidad)
        }
    }

    // ========================================
    // FECHA DE ULTIMA SINCRONIZACION
    // ========================================

    val fechaSincronizacion =
        if (ultimaActualizacion != null) {

            SimpleDateFormat(
                "dd/MM/yyyy · HH:mm",
                Locale.forLanguageTag("es-MX")
            ).format(
                Date(ultimaActualizacion)
            )

        } else {

            "Aún no hay pasos guardados"
        }

    // ========================================
    // INTERFAZ PRINCIPAL
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
                Arrangement.spacedBy(14.dp)
        ) {

            // =================================
            // ENCABEZADO
            // =================================

            Row(
                modifier = Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "👟",
                    fontSize = 30.sp
                )

                Column {

                    Text(
                        text = "Actividad física",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OscuroHealth
                    )

                    Text(
                        text = "Health Connect + Room",
                        fontSize = 12.sp,
                        color = GrisHealth
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFE5E7EB)
            )

            // =================================
            // ESTADO DE LA CONEXION
            // =================================

            val textoEstado = when {

                estado ==
                        EstadoHealthConnect.NO_DISPONIBLE -> {

                    "No disponible en este dispositivo"
                }

                estado ==
                        EstadoHealthConnect.REQUIERE_INSTALACION -> {

                    "Necesita instalación o actualización"
                }

                tienePermiso -> {

                    "Conectado y autorizado"
                }

                else -> {

                    "Esperando autorización"
                }
            }

            Text(
                text = textoEstado,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,

                color = if (tienePermiso) {
                    VerdeHealth
                } else {
                    GrisHealth
                }
            )

            Text(
                text =
                    "MoodWave lee los pasos " +
                            "autorizados de Health Connect. " +
                            "Puedes guardar una copia local " +
                            "para analizar tu actividad " +
                            "junto con tus registros emocionales.",
                fontSize = 12.sp,
                color = GrisHealth
            )

            // =================================
            // HEALTH CONNECT DISPONIBLE
            // =================================

            if (
                estado ==
                EstadoHealthConnect.DISPONIBLE
            ) {

                if (tienePermiso) {

                    // =========================
                    // PASOS DE HOY Y AYER
                    // =========================

                    Row(
                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        TarjetaCantidadPasos(
                            titulo = "Hoy",
                            cantidad = mostrarPasos(
                                pasosHoy
                            ),
                            modifier =
                                Modifier.weight(1f)
                        )

                        TarjetaCantidadPasos(
                            titulo = "Ayer",
                            cantidad = mostrarPasos(
                                pasosAyer
                            ),
                            modifier =
                                Modifier.weight(1f)
                        )
                    }

                    Text(
                        text =
                            "Estos valores se consultan " +
                                    "directamente de Health Connect.",
                        fontSize = 11.sp,
                        color = GrisHealth
                    )

                } else {

                    // =========================
                    // PEDIR AUTORIZACION
                    // =========================

                    Button(
                        modifier =
                            Modifier.fillMaxWidth(),

                        enabled = !cargando,

                        onClick = {

                            try {

                                solicitarPermisos.launch(
                                    manager.permisosNecesarios
                                )

                            } catch (
                                error: Exception
                            ) {

                                mensaje =
                                    "No se pudo abrir " +
                                            "la autorización " +
                                            "de Health Connect."
                            }
                        },

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MoradoHealth
                            )
                    ) {

                        Text(
                            text = "Conectar Health Connect"
                        )
                    }
                }

                // =============================
                // BOTON DE SINCRONIZACION
                // =============================
                //
                // Este es el boton principal.
                //
                // Lee los ultimos 7 dias,
                // guarda fechas con datos
                // y actualiza los valores.
                //
                // Si hoy tenia 57 pasos
                // y ahora registra 120,
                // Room guardara 120.

                if (tienePermiso) {

                    Button(
                        modifier =
                            Modifier.fillMaxWidth(),

                        enabled = !cargando,

                        onClick = {

                            scope.launch {

                                actualizarDatos(
                                    sincronizar = true
                                )
                            }
                        },

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MoradoHealth
                            )
                    ) {

                        Text(
                            text = "Guardar y actualizar pasos"
                        )
                    }

                } else {

                    OutlinedButton(
                        modifier =
                            Modifier.fillMaxWidth(),

                        enabled = !cargando,

                        onClick = {

                            scope.launch {

                                actualizarDatos(
                                    sincronizar = false
                                )
                            }
                        }
                    ) {

                        Text(
                            text = "Comprobar conexión"
                        )
                    }
                }
            }

            // =================================
            // INSTALAR HEALTH CONNECT
            // =================================

            if (
                estado ==
                EstadoHealthConnect.REQUIERE_INSTALACION
            ) {

                Text(
                    text =
                        "Instala o actualiza " +
                                "Health Connect para " +
                                "consultar tus pasos.",
                    fontSize = 12.sp,
                    color = GrisHealth
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),

                    onClick = {

                        val paquete =
                            "com.google.android.apps.healthdata"

                        val intent = Intent(
                            Intent.ACTION_VIEW,

                            Uri.parse(
                                "market://details?id=$paquete"
                            )
                        )

                        try {

                            context.startActivity(
                                intent
                            )

                        } catch (
                            error: ActivityNotFoundException
                        ) {

                            val navegador = Intent(
                                Intent.ACTION_VIEW,

                                Uri.parse(
                                    "https://play.google.com/store/apps/details?id=$paquete"
                                )
                            )

                            try {

                                context.startActivity(
                                    navegador
                                )

                            } catch (
                                error: Exception
                            ) {

                                mensaje =
                                    "No se pudo abrir " +
                                            "Google Play."
                            }
                        }
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                MoradoHealth
                        )
                ) {

                    Text(
                        text = "Instalar Health Connect"
                    )
                }
            }

            // =================================
            // ESTADO DE ROOM
            // ========================================
            //
            // Esta seccion presenta datos
            // almacenados localmente.
            //
            // Se actualiza mediante Flow.
            //
            // Puede mostrar registros
            // incluso si Health Connect
            // esta desconectado.
            //
            // Dejamos claro que son copias
            // locales, no una lectura nueva.

            HorizontalDivider(
                color = Color(0xFFE5E7EB)
            )

            Text(
                text = "💾 Pasos guardados en MoodWave",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OscuroHealth
            )

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                TarjetaCantidadPasos(
                    titulo = "Días guardados",
                    cantidad =
                        diasAlmacenados.toString(),
                    modifier = Modifier.weight(1f),
                    unidad = "días"
                )

                TarjetaCantidadPasos(
                    titulo = "Hoy guardados",
                    cantidad = mostrarPasos(
                        pasosHoyGuardados
                    ),
                    modifier = Modifier.weight(1f),
                    unidad = "pasos"
                )
            }

            Text(
                text =
                    "Última sincronización local: " +
                            fechaSincronizacion,
                fontSize = 11.sp,
                color = GrisHealth
            )

            if (diasAlmacenados == 0) {

                Text(
                    text =
                        "Todavía no hay pasos en Room. " +
                                "Utiliza 'Guardar y actualizar pasos' " +
                                "para crear la primera copia local.",
                    fontSize = 12.sp,
                    color = GrisHealth
                )
            }

            // =================================
            // RESULTADO DE LA ULTIMA OPERACION
            // =================================

            ultimoResultado?.let { resultado ->

                Text(
                    text =
                        "Última consulta: " +
                                "${resultado.diasConsultados} días. " +
                                "Días con datos guardados " +
                                "o actualizados: " +
                                "${resultado.diasGuardados}.",
                    fontSize = 11.sp,
                    color = GrisHealth
                )
            }

            // =================================
            // PRIVACIDAD DE LOS DATOS
            // ========================================
            //
            // Permite consultar el uso
            // de los datos y eliminar
            // la copia local de pasos.
            //
            // No altera Health Connect
            // ni los registros emocionales.

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),

                onClick = {

                    val intent = Intent(
                        context,
                        HealthConnectPrivacyActivity::class.java
                    )

                    context.startActivity(
                        intent
                    )
                }
            ) {

                Text(
                    text = "🔒 Privacidad y datos guardados",
                    color = MoradoHealth
                )
            }

            // =================================
            // ESTADO DE CARGA
            // =================================

            if (cargando) {

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.Center,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MoradoHealth
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = "Consultando y actualizando...",
                        fontSize = 12.sp,
                        color = GrisHealth
                    )
                }
            }

            // =================================
            // MENSAJES
            // =================================

            mensaje?.let { texto ->

                Text(
                    text = texto,
                    fontSize = 12.sp,
                    color = MoradoHealth
                )
            }

            // =================================
            // ACLARACION
            // =================================

            Text(
                text =
                    "Los pasos guardados todavía " +
                            "no se utilizan en las gráficas " +
                            "ni en los motores de IA. " +
                            "Esa integración será el " +
                            "siguiente paso.",
                fontSize = 10.sp,
                color = GrisHealth,
                textAlign = TextAlign.Start
            )
        }
    }
}

// ============================================
// TARJETA DE CANTIDAD
// ============================================
//
// Componente reutilizable.
//
// Muestra una etiqueta, un valor
// y la unidad correspondiente.

@Composable
private fun TarjetaCantidadPasos(
    titulo: String,
    cantidad: String,
    modifier: Modifier = Modifier,
    unidad: String = "pasos"
) {

    Card(
        modifier = modifier,

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E8FF)
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {

            Text(
                text = titulo,
                fontSize = 12.sp,
                color = GrisHealth,
                textAlign = TextAlign.Center
            )

            Text(
                text = cantidad,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MoradoHealth,
                textAlign = TextAlign.Center
            )

            Text(
                text = unidad,
                fontSize = 11.sp,
                color = GrisHealth
            )
        }
    }
}
