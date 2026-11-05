
package com.example.pp1jd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================
// COMPONENTES DE MOODWAVE
// ============================================

import com.example.pp1jd.ui.components.TarjetaRecordatorio
import com.example.pp1jd.ui.components.TarjetaHealthConnect
import com.example.pp1jd.ui.viewmodel.DiaryViewModel

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ============================================
// FUNCION AUXILIAR: FECHA ISO
// ============================================
//
// Convierte una fecha a yyyy-MM-dd.
// Es el mismo formato usado en Room.

private fun fechaISO(
    calendario: Calendar
): String {

    val formato = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    )

    return formato.format(calendario.time)
}

// ============================================
// CALCULAR RACHA DE REGISTROS
// ============================================
//
// Cuenta los dias consecutivos registrados.
//
// Si hoy no hay registro, empieza desde ayer.
// Varios registros en un mismo dia
// cuentan como un solo dia.

private fun calcularRacha(
    diasRegistrados: Set<String>,
    hoy: Calendar
): Int {

    val fecha = hoy.clone() as Calendar

    if (
        fechaISO(fecha) !in diasRegistrados
    ) {

        fecha.add(Calendar.DAY_OF_YEAR, -1)
    }

    var racha = 0

    while (
        fechaISO(fecha) in diasRegistrados
    ) {

        racha++

        fecha.add(Calendar.DAY_OF_YEAR, -1)
    }

    return racha
}

// ============================================
// PANTALLA PRINCIPAL DE MOODWAVE
// ============================================

@Composable
fun HomeScreen(
    viewModel: DiaryViewModel,
    onNavigateToHistory: () -> Unit
) {

    val scrollState = rememberScrollState()

    // ========================================
    // FECHA ACTUAL
    // ========================================

    val hoy = Calendar.getInstance()

    val ayer = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }

    val fechaHoy = fechaISO(hoy)
    val fechaAyer = fechaISO(ayer)

    val formatoFecha = SimpleDateFormat(
        "EEEE, d 'de' MMMM 'de' yyyy",
        Locale.forLanguageTag("es-MX")
    )

    val fechaMostrada = formatoFecha
        .format(hoy.time)
        .replaceFirstChar {
            it.uppercaseChar()
        }

    // ========================================
    // REGISTROS REALES DE ROOM
    // ========================================

    val registrosHoy =
        viewModel.historyEntries.filter {
            it.dateIso == fechaHoy
        }

    val diasRegistrados =
        viewModel.historyEntries
            .map { it.dateIso }
            .toSet()

    val racha = calcularRacha(
        diasRegistrados,
        hoy
    )

    // ========================================
    // ENERGIA PROMEDIO DE HOY
    // ========================================

    val energiaPromedio =
        if (registrosHoy.isNotEmpty()) {

            registrosHoy
                .map { it.energy }
                .average()

        } else {

            null
        }

    val progresoEnergia =
        ((energiaPromedio ?: 0.0) / 10.0)
            .toFloat()
            .coerceIn(0f, 1f)

    // ========================================
    // ULTIMO REGISTRO DE AYER
    // ========================================

    val registroAyer =
        viewModel.historyEntries.firstOrNull {
            it.dateIso == fechaAyer
        }

    // ========================================
    // INTERFAZ PRINCIPAL
    // ========================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {

        // ====================================
        // 1. SALUDO Y FECHA
        // ====================================

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "¡Hola! ✨",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = fechaMostrada,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE9FE)),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "🌊",
                    fontSize = 24.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // ====================================
        // 2. SELECCIONAR ESTADO DE ANIMO
        // ====================================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "¿Cómo te sientes en este momento?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    // =========================
                    // EMOCIONES DISPONIBLES
                    // =========================

                    val moods = listOf(

                        Triple(
                            "Muy mal",
                            "😭",
                            Color(0xFFEF4444)
                        ),

                        Triple(
                            "Mal",
                            "😕",
                            Color(0xFFF97316)
                        ),

                        Triple(
                            "Normal",
                            "😐",
                            Color(0xFFF59E0B)
                        ),

                        Triple(
                            "Bien",
                            "😊",
                            Color(0xFF10B981)
                        ),

                        Triple(
                            "Muy bien",
                            "😁",
                            Color(0xFF3B82F6)
                        )
                    )

                    moods.forEach {
                            (label, emoji, color) ->

                        val isSelected =
                            viewModel.selectedMood == label

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(12.dp)
                                )
                                .background(
                                    if (isSelected) {
                                        Color(0xFFF3E8FF)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable {

                                    viewModel.updateMood(
                                        label,
                                        emoji
                                    )
                                }
                                .padding(6.dp)
                        ) {

                            // =================
                            // CIRCULO DEL EMOJI
                            // =================

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        color.copy(alpha = 0.15f)
                                    ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text = emoji,
                                    fontSize = 19.sp
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            // =================
                            // NOMBRE DE EMOCION
                            // =================

                            Text(
                                text = label,
                                fontSize = 10.sp,

                                color = if (isSelected) {
                                    Color(0xFF7C3AED)
                                } else {
                                    Color(0xFF6B7280)
                                },

                                fontWeight = if (isSelected) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // ====================================
        // 3. RACHA REAL DE REGISTROS
        // ====================================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF7ED)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEDD5)),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.LocalFireDepartment,

                        contentDescription =
                            "Racha de registros",

                        tint = Color(0xFFF97316)
                    )
                }

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                Column {

                    Text(
                        text = "Racha de registros",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412)
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    val mensajeRacha = when {

                        racha == 0 ->
                            "Aún no tienes una racha activa."

                        racha == 1 ->
                            "Tienes 1 día registrado en tu racha."

                        else ->
                            "Tienes $racha días consecutivos registrados."
                    }

                    Text(
                        text = mensajeRacha,
                        fontSize = 13.sp,
                        color = Color(0xFFC2410C)
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // ====================================
        // 4. ENERGIA PROMEDIO DE HOY
        // ====================================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "Energía promedio de hoy",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )

                    Text(
                        text = if (energiaPromedio != null) {

                            String.format(
                                Locale.forLanguageTag("es-MX"),
                                "%.1f/10",
                                energiaPromedio
                            )

                        } else {

                            "Sin datos"
                        },

                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                // ============================
                // BARRA DE ENERGIA
                // ============================

                LinearProgressIndicator(
                    progress = {
                        progresoEnergia
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(
                            RoundedCornerShape(4.dp)
                        ),

                    color = Color(0xFF10B981),

                    trackColor = Color(0xFFE5E7EB)
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                // ============================
                // EXPLICACION DE ENERGIA
                // ============================

                val textoEnergia =
                    if (registrosHoy.isEmpty()) {

                        "Todavía no registras tu energía hoy."

                    } else {

                        val cantidad = registrosHoy.size

                        if (cantidad == 1) {

                            "Calculado con 1 registro de hoy."

                        } else {

                            "Calculado con $cantidad registros de hoy."
                        }
                    }

                Text(
                    text = textoEnergia,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // ====================================
        // 5. ULTIMO REGISTRO DE AYER
        // ====================================

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {

                    onNavigateToHistory()
                },

            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF0FDF4)
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "Tu último registro de ayer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )

                    if (registroAyer != null) {

                        Surface(
                            shape = RoundedCornerShape(12.dp),

                            color = Color(0xFFDCFCE7)
                        ) {

                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text = registroAyer.moodEmoji,
                                    fontSize = 12.sp
                                )

                                Spacer(
                                    modifier = Modifier.width(4.dp)
                                )

                                Text(
                                    text = registroAyer.mood,
                                    fontSize = 12.sp,

                                    fontWeight =
                                        FontWeight.SemiBold,

                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                if (registroAyer != null) {

                    Text(
                        text = if (
                            registroAyer.reflection.isNotBlank()
                        ) {

                            registroAyer.reflection

                        } else {

                            "Sin reflexión escrita."
                        },

                        fontSize = 13.sp,
                        color = Color(0xFF15803D)
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Energía: ${registroAyer.energy}/10",
                        fontSize = 12.sp,
                        color = Color(0xFF166534)
                    )

                } else {

                    Text(
                        text = "No hay registros del día de ayer.",
                        fontSize = 13.sp,
                        color = Color(0xFF15803D)
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // ====================================
        // 6. RECORDATORIOS DIARIOS
        // ====================================
        //
        // Conservamos el componente
        // que ya funciona.
        //
        // Incluye:
        // - Activar o desactivar.
        // - Configurar la hora.
        // - Cuenta regresiva.
        // - Notificacion de prueba.
        //
        // Sigue utilizando AlarmManager.

        TarjetaRecordatorio()

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // ====================================
        // 7. HEALTH CONNECT - PASOS REALES
        // ====================================
        //
        // NUEVO COMPONENTE.
        //
        // Aparece al final de Home.
        //
        // Permite:
        // - Comprobar disponibilidad.
        // - Solicitar permiso de pasos.
        // - Consultar pasos de hoy.
        // - Consultar pasos de ayer.
        // - Actualizar los datos.
        //
        // Esta etapa solo lee y muestra
        // datos de Health Connect.
        //
        // No modifica Room ni la IA.

        TarjetaHealthConnect()

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}
