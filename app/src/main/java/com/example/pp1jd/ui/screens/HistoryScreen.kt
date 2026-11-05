
package com.example.pp1jd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.pp1jd.ui.viewmodel.DiaryEntry
import com.example.pp1jd.ui.viewmodel.DiaryViewModel

import java.util.Locale

// ============================================
// HISTORIAL DE MOODWAVE
// ============================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
) {

    // ========================================
    // ESTADOS DE BUSQUEDA Y FILTROS
    // ========================================

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedMoodFilter by remember {
        mutableStateOf("Todos")
    }

    var showFilterMenu by remember {
        mutableStateOf(false)
    }

    val emociones = listOf(
        "Todos",
        "Muy bien",
        "Bien",
        "Normal",
        "Mal",
        "Muy mal"
    )

    // ========================================
    // REGISTROS REALES DE ROOM
    // ========================================

    val registros = viewModel.historyEntries

    // Filtramos los registros según la
    // búsqueda y el estado de ánimo.

    val registrosFiltrados = registros.filter { registro ->

        val texto = searchQuery.trim()

        val coincideBusqueda =
            texto.isBlank() ||

                    registro.date.contains(
                        texto,
                        ignoreCase = true
                    ) ||

                    registro.dateIso.contains(
                        texto,
                        ignoreCase = true
                    ) ||

                    registro.mood.contains(
                        texto,
                        ignoreCase = true
                    ) ||

                    registro.reflection.contains(
                        texto,
                        ignoreCase = true
                    ) ||

                    registro.activities.any { actividad ->
                        actividad.contains(
                            texto,
                            ignoreCase = true
                        )
                    }

        val coincideEmocion =
            selectedMoodFilter == "Todos" ||
                    registro.mood == selectedMoodFilter

        coincideBusqueda && coincideEmocion
    }

    val hayFiltros =
        searchQuery.isNotBlank() ||
                selectedMoodFilter != "Todos"

    // ========================================
    // INTERFAZ PRINCIPAL
    // ========================================

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),

        contentPadding = PaddingValues(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        // ====================================
        // ENCABEZADO
        // ====================================

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFF1F2937)
                    )
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Column {

                    Text(
                        text = "Mi historial",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )

                    Text(
                        text = "Tus registros emocionales",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }

        // ====================================
        // BUSCADOR Y FILTROS
        // ====================================

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = searchQuery,

                    onValueChange = {
                        searchQuery = it
                    },

                    modifier = Modifier.weight(1f),

                    singleLine = true,

                    shape = RoundedCornerShape(16.dp),

                    placeholder = {
                        Text(
                            "Buscar registros...",
                            fontSize = 13.sp
                        )
                    },

                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color(0xFF9CA3AF)
                        )
                    },

                    trailingIcon = {

                        if (searchQuery.isNotEmpty()) {

                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                }
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda"
                                )
                            }
                        }
                    },

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor =
                            Color(0xFF7C3AED),

                        unfocusedBorderColor =
                            Color(0xFFE5E7EB),

                        focusedContainerColor =
                            Color.White,

                        unfocusedContainerColor =
                            Color.White
                    )
                )

                // BOTON DE FILTROS

                Box {

                    FilledTonalIconButton(
                        onClick = {
                            showFilterMenu = true
                        },

                        modifier = Modifier.size(52.dp),

                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xFFF3E8FF),
                                contentColor = Color(0xFF7C3AED)
                            ),

                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filtrar emociones"
                        )
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,

                        onDismissRequest = {
                            showFilterMenu = false
                        }
                    ) {

                        emociones.forEach { emocion ->

                            DropdownMenuItem(
                                text = {
                                    Text(emocion)
                                },

                                onClick = {
                                    selectedMoodFilter = emocion
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ====================================
        // FILTRO ACTIVO
        // ====================================

        if (hayFiltros) {

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = if (
                            selectedMoodFilter != "Todos"
                        ) {
                            "Filtro: $selectedMoodFilter"
                        } else {
                            "Búsqueda activa"
                        },

                        fontSize = 12.sp,
                        color = Color(0xFF7C3AED)
                    )

                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedMoodFilter = "Todos"
                        }
                    ) {

                        Text(
                            text = "Limpiar filtros",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ====================================
        // CONTADOR DE REGISTROS
        // ====================================

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (hayFiltros)
                        "RESULTADOS"
                    else
                        "TODOS LOS REGISTROS",

                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9CA3AF),
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${registrosFiltrados.size} de ${registros.size}",

                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7C3AED)
                )
            }
        }

        // ====================================
        // ESTADO SIN REGISTROS
        // ====================================

        if (registrosFiltrados.isEmpty()) {

            item {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "📭",
                        fontSize = 42.sp
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = if (registros.isEmpty())
                            "Todavía no tienes registros"
                        else
                            "No se encontraron resultados",

                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4B5563)
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = if (registros.isEmpty())
                            "Registra cómo te sientes para comenzar tu historial."
                        else
                            "Prueba con otra búsqueda o filtro.",

                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }

        } else {

            // =================================
            // REGISTROS REALES
            // =================================

            items(
                items = registrosFiltrados,
                key = { it.id }
            ) { registro ->

                TarjetaRegistroHistorial(
                    registro = registro
                )
            }
        }
    }
}

// ============================================
// TARJETA INDIVIDUAL DE REGISTRO
// ============================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaRegistroHistorial(
    registro: DiaryEntry
) {

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

            // =================================
            // FECHA Y ESTADO DE ANIMO
            // =================================

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment = Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = registro.date,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Energía: ${registro.energy}/10",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3E8FF)
                ) {

                    Row(
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = registro.moodEmoji,
                            fontSize = 14.sp
                        )

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        Text(
                            text = registro.mood,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }
            }

            // =================================
            // HORAS DE SUEÑO
            // =================================

            registro.hoursSleep?.let { horas ->

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "🌙 Sueño: " +
                            String.format(
                                Locale.forLanguageTag("es-MX"),
                                "%.1f horas",
                                horas
                            ),

                    fontSize = 13.sp,
                    color = Color(0xFF4B5563)
                )
            }

            // =================================
            // PASOS DE HEALTH CONNECT
            // =================================

            registro.steps?.let { pasos ->

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "👟 Pasos: $pasos",
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563)
                )
            }

            // =================================
            // REFLEXION REAL
            // =================================

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = if (registro.reflection.isNotBlank())
                    registro.reflection
                else
                    "Sin reflexión escrita.",

                fontSize = 13.sp,
                color = Color(0xFF4B5563)
            )

            // =================================
            // ACTIVIDADES
            // =================================

            if (registro.activities.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                FlowRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    registro.activities.forEach { actividad ->

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF3F4F6)
                        ) {

                            Text(
                                text = actividad,
                                fontSize = 11.sp,
                                color = Color(0xFF4B5563),

                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 5.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
