
package com.example.pp1jd.healthconnect

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// ============================================
// COLORES DE PRIVACIDAD
// ============================================

private val MoradoPrivacidad =
    Color(0xFF7C3AED)

private val FondoPrivacidad =
    Color(0xFFF8F9FA)

private val TextoPrivacidad =
    Color(0xFF1F2937)

private val GrisPrivacidad =
    Color(0xFF6B7280)

private val RojoPrivacidad =
    Color(0xFFB91C1C)

// ============================================
// ACTIVIDAD DE PRIVACIDAD
// ============================================
//
// Health Connect puede abrir esta
// actividad para mostrar como MoodWave
// utiliza los datos de pasos.
//
// La actividad tambien permite
// eliminar la copia local de pasos.
//
// No solicita nuevos permisos.
//
// No modifica los datos originales
// almacenados en Health Connect.

class HealthConnectPrivacyActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContent {

            MaterialTheme {

                PantallaPrivacidadHealthConnect(
                    onCerrar = {
                        finish()
                    }
                )
            }
        }
    }
}

// ============================================
// PANTALLA DE PRIVACIDAD
// ============================================

@Composable
private fun PantallaPrivacidadHealthConnect(
    onCerrar: () -> Unit
) {

    val context = LocalContext.current

    // ========================================
    // ACCESO AL SINCRONIZADOR
    // ========================================
    //
    // Ya creamos esta clase en el paso
    // 38.8.4.
    //
    // La utilizaremos exclusivamente
    // para eliminar los pasos locales.

    val sincronizador = remember(context) {

        SincronizadorPasos(context)
    }

    // ========================================
    // CORRUTINAS
    // ========================================
    //
    // Room utiliza operaciones suspend.
    //
    // Las ejecutaremos mediante
    // una corrutina para no bloquear
    // la interfaz.

    val scope = rememberCoroutineScope()

    // ========================================
    // ESTADOS DE LA INTERFAZ
    // ========================================

    var mostrarConfirmacion by remember {
        mutableStateOf(false)
    }

    var borrando by remember {
        mutableStateOf(false)
    }

    var mensajeEliminacion by remember {
        mutableStateOf<String?>(null)
    }

    // ========================================
    // CONFIRMACION DE ELIMINACION
    // ========================================
    //
    // Nunca eliminamos informacion
    // inmediatamente al tocar un boton.
    //
    // Primero preguntamos al usuario
    // si realmente quiere continuar.

    if (mostrarConfirmacion) {

        AlertDialog(

            onDismissRequest = {

                if (!borrando) {

                    mostrarConfirmacion = false
                }
            },

            title = {

                Text(
                    text = "¿Eliminar pasos guardados?"
                )
            },

            text = {

                Text(
                    text =
                        "Se eliminarán todos los pasos " +
                                "almacenados localmente en " +
                                "MoodWave.\n\n" +
                                "Tus emociones, reflexiones, " +
                                "estadísticas emocionales y " +
                                "registros personales " +
                                "permanecerán intactos.\n\n" +
                                "Tampoco se borrarán los " +
                                "pasos originales de " +
                                "Health Connect."
                )
            },

            confirmButton = {

                TextButton(

                    enabled = !borrando,

                    onClick = {

                        mostrarConfirmacion = false

                        borrando = true

                        mensajeEliminacion = null

                        // =====================
                        // BORRAR EN ROOM
                        // =====================

                        scope.launch {

                            try {

                                sincronizador
                                    .eliminarPasosLocales()

                                mensajeEliminacion =
                                    "Los pasos guardados " +
                                            "en MoodWave se " +
                                            "eliminaron correctamente."

                            } catch (
                                error: CancellationException
                            ) {

                                throw error

                            } catch (
                                error: Exception
                            ) {

                                mensajeEliminacion =
                                    "No se pudieron eliminar " +
                                            "los pasos: " +
                                            (
                                                    error.localizedMessage
                                                        ?: "Error desconocido."
                                                    )

                            } finally {

                                borrando = false
                            }
                        }
                    }
                ) {

                    Text(
                        text = "Eliminar",
                        color = RojoPrivacidad
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {
                        mostrarConfirmacion = false
                    }
                ) {

                    Text(
                        text = "Cancelar"
                    )
                }
            }
        )
    }

    // ========================================
    // CONTENIDO PRINCIPAL
    // ========================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoPrivacidad)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        // ====================================
        // ENCABEZADO
        // ====================================

        TextButton(
            onClick = onCerrar
        ) {

            Text(
                text = "← Volver",
                color = MoradoPrivacidad
            )
        }

        Text(
            text = "🔒",
            fontSize = 40.sp
        )

        Text(
            text = "Tu privacidad importa",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrivacidad
        )

        Text(
            text =
                "Conoce cómo MoodWave consulta, " +
                        "utiliza y almacena los datos " +
                        "de actividad física " +
                        "de Health Connect.",
            fontSize = 14.sp,
            color = GrisPrivacidad
        )

        // ====================================
        // 1. DATOS SOLICITADOS
        // ====================================

        SeccionPrivacidad(
            titulo =
                "👟 ¿Qué información solicitamos?",

            contenido =
                "MoodWave solicita únicamente " +
                        "permiso para leer la cantidad " +
                        "de pasos registrados en " +
                        "Health Connect.\n\n" +
                        "No solicitamos acceso a " +
                        "frecuencia cardíaca, peso, " +
                        "ubicación, historial médico " +
                        "ni otros datos de salud.\n\n" +
                        "Tampoco escribimos ni " +
                        "modificamos tus datos " +
                        "originales de Health Connect."
        )

        // ====================================
        // 2. FINALIDAD
        // ====================================

        SeccionPrivacidad(
            titulo =
                "📊 ¿Para qué utilizamos los pasos?",

            contenido =
                "Utilizamos los pasos para " +
                        "mostrar tu actividad física " +
                        "diaria dentro de MoodWave.\n\n" +
                        "También podrán relacionarse " +
                        "con tus registros de ánimo " +
                        "y energía, para identificar " +
                        "posibles tendencias " +
                        "personales.\n\n" +
                        "Estas relaciones son " +
                        "estadísticas y no representan " +
                        "diagnósticos médicos " +
                        "ni demuestran causas."
        )

        // ====================================
        // 3. ALMACENAMIENTO LOCAL
        // ====================================
        //
        // ACTUALIZACION IMPORTANTE.
        //
        // Antes solo consultabamos
        // pasos desde Health Connect.
        //
        // Ahora nuestro sincronizador
        // permitira copiarlos a Room.
        //
        // Debemos explicarlo claramente.

        SeccionPrivacidad(
            titulo =
                "💾 ¿Dónde se guardan los datos?",

            contenido =
                "Cuando sincronices tus pasos " +
                        "con MoodWave, la aplicación " +
                        "guardará una copia en su " +
                        "base de datos local Room, " +
                        "dentro de este dispositivo.\n\n" +
                        "Se almacenarán la fecha, " +
                        "la cantidad de pasos y " +
                        "el momento de actualización.\n\n" +
                        "La sincronización actualizará " +
                        "los totales existentes " +
                        "sin duplicar pasos.\n\n" +
                        "Esta integración no envía " +
                        "los pasos a un servidor."
        )

        // ====================================
        // 4. PERMISOS OPCIONALES
        // ====================================

        SeccionPrivacidad(
            titulo =
                "✅ Tú decides qué compartir",

            contenido =
                "La conexión con Health Connect " +
                        "es completamente opcional.\n\n" +
                        "Puedes conceder o rechazar " +
                        "el permiso de lectura.\n\n" +
                        "Si no lo autorizas, podrás " +
                        "seguir utilizando el diario " +
                        "emocional, historial, calendario, " +
                        "estadísticas, inteligencia " +
                        "artificial y recordatorios."
        )

        // ====================================
        // 5. REVOCAR PERMISOS
        // ====================================

        SeccionPrivacidad(
            titulo =
                "🛡️ ¿Cómo retirar la autorización?",

            contenido =
                "Puedes retirar el acceso " +
                        "a tus pasos en cualquier " +
                        "momento desde Health Connect.\n\n" +
                        "Abre Health Connect, entra " +
                        "en los permisos de aplicaciones " +
                        "y selecciona MoodWave.\n\n" +
                        "Al retirar el permiso, " +
                        "MoodWave dejará de poder " +
                        "consultar nuevos datos " +
                        "de pasos.\n\n" +
                        "Importante: retirar el permiso " +
                        "no elimina automáticamente " +
                        "las copias que MoodWave " +
                        "ya haya guardado en Room."
        )

        // ====================================
        // 6. ELIMINACION DE DATOS
        // ====================================

        SeccionPrivacidad(
            titulo =
                "🗑️ ¿Cómo eliminar mis pasos?",

            contenido =
                "Puedes eliminar todos los " +
                        "pasos guardados localmente " +
                        "en MoodWave utilizando " +
                        "el botón que aparece abajo.\n\n" +
                        "Esto no eliminará tus " +
                        "registros emocionales ni " +
                        "los pasos originales " +
                        "de Health Connect.\n\n" +
                        "Si posteriormente vuelves " +
                        "a sincronizar con el permiso " +
                        "activo, los pasos disponibles " +
                        "podrán guardarse nuevamente."
        )

        // ====================================
        // BOTON PARA ELIMINAR DATOS
        // ====================================
        //
        // Elimina exclusivamente
        // la tabla de pasos locales.
        //
        // No modifica RegistroEntity.
        //
        // No afecta los datos originales
        // de Health Connect.

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),

            enabled = !borrando,

            onClick = {

                mostrarConfirmacion = true
            },

            colors = ButtonDefaults
                .outlinedButtonColors(
                    contentColor = RojoPrivacidad
                )
        ) {

            Text(
                text = "🗑️ Eliminar pasos de MoodWave"
            )
        }

        // ====================================
        // INDICADOR DE ELIMINACION
        // ====================================

        if (borrando) {

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
                    color = MoradoPrivacidad
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = "Eliminando pasos...",
                    fontSize = 12.sp,
                    color = GrisPrivacidad
                )
            }
        }

        // ====================================
        // RESULTADO DE LA ELIMINACION
        // ====================================

        mensajeEliminacion?.let { mensaje ->

            Text(
                text = mensaje,
                fontSize = 12.sp,
                color = TextoPrivacidad
            )
        }

        // ====================================
        // COMPROMISO DE MOODWAVE
        // ====================================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(16.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3E8FF)
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "🌊 Compromiso de MoodWave",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoradoPrivacidad
                )

                Text(
                    text =
                        "Solo solicitamos los datos " +
                                "necesarios para las funciones " +
                                "de actividad física.\n\n" +
                                "La copia de pasos se almacena " +
                                "localmente y puedes eliminarla " +
                                "cuando quieras.\n\n" +
                                "No modificamos los datos " +
                                "originales de Health Connect.",
                    fontSize = 12.sp,
                    color = TextoPrivacidad
                )
            }
        }

        // ====================================
        // CERRAR PANTALLA
        // ====================================

        Button(
            modifier = Modifier.fillMaxWidth(),

            onClick = onCerrar,

            colors = ButtonDefaults.buttonColors(
                containerColor = MoradoPrivacidad
            )
        ) {

            Text(
                text = "Entendido"
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )
    }
}

// ============================================
// COMPONENTE REUTILIZABLE
// ============================================
//
// Muestra cada seccion de privacidad
// con su titulo y explicacion.
//
// Mantiene el mismo diseño visual
// del archivo anterior.

@Composable
private fun SeccionPrivacidad(
    titulo: String,
    contenido: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = titulo,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrivacidad
            )

            Text(
                text = contenido,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = GrisPrivacidad
            )
        }
    }
}
