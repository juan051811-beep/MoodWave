
package com.example.pp1jd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.pp1jd.ui.viewmodel.DiaryViewModel

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ============================================
// FUNCIONES AUXILIARES
// ============================================

// Convierte una fecha a formato yyyy-MM-dd.
// Es el mismo formato utilizado por Room.

private fun crearFechaISO(
    anio: Int,
    mes: Int,
    dia: Int
): String {
    return String.format(
        Locale.US,
        "%04d-%02d-%02d",
        anio,
        mes + 1,
        dia
    )
}

// Define el color de cada estado emocional.

private fun colorDelAnimo(animo: String): Color {
    return when (animo) {
        "Muy bien" -> Color(0xFF3B82F6)
        "Bien" -> Color(0xFF10B981)
        "Normal" -> Color(0xFFF59E0B)
        "Mal" -> Color(0xFFF97316)
        "Muy mal" -> Color(0xFFEF4444)
        else -> Color(0xFF9CA3AF)
    }
}

// ============================================
// PANTALLA DE CALENDARIO - MOODWAVE
// ============================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    viewModel: DiaryViewModel
) {

    val scrollState = rememberScrollState()

    // ========================================
    // FECHA REAL DEL DISPOSITIVO
    // ========================================

    val hoy = remember {
        Calendar.getInstance()
    }

    val anioActual = hoy.get(Calendar.YEAR)
    val mesActual = hoy.get(Calendar.MONTH)
    val diaActual = hoy.get(Calendar.DAY_OF_MONTH)

    // Mes que estamos visualizando.
    var anioMostrado by remember {
        mutableIntStateOf(anioActual)
    }

    var mesMostrado by remember {
        mutableIntStateOf(mesActual)
    }

    // Día seleccionado por el usuario.
    var diaSeleccionado by remember {
        mutableIntStateOf(diaActual)
    }

    // ========================================
    // DATOS DEL MES
    // ========================================

    val calendario = Calendar.getInstance().apply {
        clear()
        set(anioMostrado, mesMostrado, 1)
    }

    val diasDelMes = calendario.getActualMaximum(
        Calendar.DAY_OF_MONTH
    )

    val primerDiaSemana = calendario.get(
        Calendar.DAY_OF_WEEK
    )

    // Nuestra semana comienza en lunes.
    val espacioInicial = (
            primerDiaSemana - Calendar.MONDAY + 7
            ) % 7

    val idioma = Locale.forLanguageTag("es-MX")

    val formatoMes = SimpleDateFormat(
        "MMMM yyyy",
        idioma
    )

    val tituloMes = formatoMes
        .format(calendario.time)
        .replaceFirstChar { it.uppercaseChar() }

    // ========================================
    // REGISTROS REALES DE ROOM
    // ========================================

    // Agrupamos todos los registros según
    // su fecha real, sin utilizar datos falsos.

    val registrosPorFecha =
        viewModel.historyEntries.groupBy {
            it.dateIso
        }

    // Fecha seleccionada en el calendario.
    val fechaSeleccionada = crearFechaISO(
        anioMostrado,
        mesMostrado,
        diaSeleccionado
    )

    // Registros de ese día.
    val registrosSeleccionados =
        registrosPorFecha[fechaSeleccionada].orEmpty()

    // Fecha legible para la tarjeta inferior.
    val calendarioSeleccionado =
        Calendar.getInstance().apply {
            clear()
            set(
                anioMostrado,
                mesMostrado,
                diaSeleccionado
            )
        }

    val formatoDia = SimpleDateFormat(
        "EEEE d 'de' MMMM 'de' yyyy",
        idioma
    )

    val tituloDia = formatoDia
        .format(calendarioSeleccionado.time)
        .replaceFirstChar { it.uppercaseChar() }

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
        // ENCABEZADO DEL CALENDARIO
        // ====================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = tituloMes,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                // MES ANTERIOR
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {

                            if (mesMostrado == Calendar.JANUARY) {
                                mesMostrado = Calendar.DECEMBER
                                anioMostrado--
                            } else {
                                mesMostrado--
                            }

                            diaSeleccionado = 1
                        },

                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.ChevronLeft,
                            contentDescription = "Mes anterior",
                            tint = Color(0xFF4B5563)
                        )
                    }
                }

                // MES SIGUIENTE
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {

                            if (mesMostrado == Calendar.DECEMBER) {
                                mesMostrado = Calendar.JANUARY
                                anioMostrado++
                            } else {
                                mesMostrado++
                            }

                            diaSeleccionado = 1
                        },

                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.ChevronRight,
                            contentDescription = "Mes siguiente",
                            tint = Color(0xFF4B5563)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ====================================
        // TARJETA DEL CALENDARIO
        // ====================================

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
                modifier = Modifier.padding(20.dp)
            ) {

                // NOMBRES DE LOS DIAS

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    listOf(
                        "L", "M", "M", "J", "V", "S", "D"
                    ).forEach { nombreDia ->

                        Text(
                            text = nombreDia,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =================================
                // GENERAR LOS DIAS DEL MES
                // =================================

                val dias = List(espacioInicial) { 0 } +
                        (1..diasDelMes).toList()

                val semanas = dias.chunked(7)

                semanas.forEach { semana ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),

                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        semana.forEach { dia ->

                            if (dia == 0) {

                                // Espacio antes del primer día.
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(48.dp)
                                )

                            } else {

                                // Fecha exacta de esta celda.
                                val fechaDia = crearFechaISO(
                                    anioMostrado,
                                    mesMostrado,
                                    dia
                                )

                                // Buscamos registros reales.
                                val registrosDia =
                                    registrosPorFecha[fechaDia]
                                        .orEmpty()

                                val tieneRegistro =
                                    registrosDia.isNotEmpty()

                                // Identificamos el día actual.
                                val esHoy =
                                    dia == diaActual &&
                                            mesMostrado == mesActual &&
                                            anioMostrado == anioActual

                                val estaSeleccionado =
                                    dia == diaSeleccionado

                                val colorFondo = when {
                                    esHoy ->
                                        Color(0xFF7C3AED)

                                    estaSeleccionado ->
                                        Color(0xFFF3E8FF)

                                    else ->
                                        Color.Transparent
                                }

                                // Si existen varios registros,
                                // usamos el último registrado
                                // para el indicador de color.

                                val ultimoRegistro =
                                    registrosDia.firstOrNull()

                                val colorIndicador =
                                    if (esHoy) {
                                        Color.White
                                    } else {
                                        colorDelAnimo(
                                            ultimoRegistro?.mood ?: ""
                                        )
                                    }

                                // CELDA DEL DIA
                                Column(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(48.dp)
                                        .clip(
                                            RoundedCornerShape(12.dp)
                                        )
                                        .background(colorFondo)
                                        .clickable {
                                            diaSeleccionado = dia
                                        },

                                    horizontalAlignment =
                                        Alignment.CenterHorizontally,

                                    verticalArrangement =
                                        Arrangement.Center
                                ) {

                                    Text(
                                        text = dia.toString(),
                                        fontSize = 14.sp,

                                        fontWeight =
                                            if (esHoy || estaSeleccionado)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Medium,

                                        color =
                                            if (esHoy)
                                                Color.White
                                            else
                                                Color(0xFF1F2937)
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    // Solo mostramos el indicador
                                    // cuando existen registros reales.

                                    if (tieneRegistro) {

                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    colorIndicador
                                                )
                                        )

                                    } else {

                                        Spacer(
                                            modifier =
                                                Modifier.height(6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Completar la última semana.
                        repeat(7 - semana.size) {

                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = Color(0xFFE5E7EB),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // =================================
                // LEYENDA DE ESTADOS EMOCIONALES
                // =================================

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    val emociones = listOf(
                        "Muy bien",
                        "Bien",
                        "Normal",
                        "Mal",
                        "Muy mal"
                    )

                    emociones.forEach { emocion ->

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        colorDelAnimo(emocion)
                                    )
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Text(
                                text = emocion,
                                fontSize = 10.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ====================================
        // RESUMEN DEL DIA SELECCIONADO
        // ====================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3E8FF)
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = tituloDia,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D28D9)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (registrosSeleccionados.isEmpty()) {

                    // No inventamos información
                    // cuando no existen registros.

                    Text(
                        text = "No hay registros para este día.",
                        fontSize = 13.sp,
                        color = Color(0xFF7C3AED)
                    )

                } else {

                    Text(
                        text = if (registrosSeleccionados.size == 1)
                            "1 registro encontrado"
                        else
                            "${registrosSeleccionados.size} registros encontrados",

                        fontSize = 12.sp,
                        color = Color(0xFF7C3AED)
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    // Mostramos los registros reales.

                    registrosSeleccionados.forEachIndexed {
                            indice, registro ->

                        if (indice > 0) {

                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    vertical = 12.dp
                                ),
                                color = Color(0xFFD8B4FE)
                            )
                        }

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text = registro.moodEmoji,
                                    fontSize = 22.sp
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(14.dp)
                            )

                            Column {

                                Text(
                                    text = registro.mood,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6D28D9)
                                )

                                Text(
                                    text = "Energía: ${registro.energy}/10",
                                    fontSize = 13.sp,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                        }

                        // HORAS DE SUEÑO

                        registro.hoursSleep?.let { horas ->

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Sueño: $horas horas",
                                fontSize = 13.sp,
                                color = Color(0xFF6D28D9)
                            )
                        }

                        // ACTIVIDADES

                        if (registro.activities.isNotEmpty()) {

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Actividades: " +
                                        registro.activities.joinToString(", "),

                                fontSize = 13.sp,
                                color = Color(0xFF6D28D9)
                            )
                        }

                        // REFLEXIÓN

                        if (registro.reflection.isNotBlank()) {

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = registro.reflection,
                                fontSize = 13.sp,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
