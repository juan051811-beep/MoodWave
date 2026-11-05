
package com.example.pp1jd.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pp1jd.ui.viewmodel.DiaryViewModel

// ============================================
// PANTALLA DE NUEVO REGISTRO - MOODWAVE
// ============================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewEntryScreen(
    viewModel: DiaryViewModel,
    onSaved: () -> Unit
) {

    val scrollState = rememberScrollState()

    // Mensaje de error del formulario
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // Actividades disponibles
    val activities = listOf(
        "Ejercicio",
        "Trabajo",
        "Social",
        "Familia",
        "Descanso",
        "Lectura",
        "Meditación",
        "Naturaleza"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {

        // ====================================
        // ENCABEZADO
        // ====================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Nuevo Registro",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            IconButton(
                onClick = { onSaved() },
                enabled = !viewModel.isSaving
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar sin guardar",
                    tint = Color(0xFF6B7280)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ====================================
        // SELECCIONAR ESTADO DE ANIMO
        // ====================================

        Text(
            text = "¿Cómo te sientes?",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            val moods = listOf(
                Triple("Muy mal", "😭", Color(0xFFEF4444)),
                Triple("Mal", "😕", Color(0xFFF97316)),
                Triple("Normal", "😐", Color(0xFFF59E0B)),
                Triple("Bien", "😊", Color(0xFF10B981)),
                Triple("Muy bien", "😁", Color(0xFF3B82F6))
            )

            moods.forEach { (label, emoji, color) ->

                val isSelected =
                    viewModel.selectedMood == label

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected)
                                Color(0xFFF3E8FF)
                            else
                                Color.Transparent
                        )
                        .clickable(
                            enabled = !viewModel.isSaving
                        ) {
                            viewModel.updateMood(
                                label,
                                emoji
                            )
                        }
                        .padding(6.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                color.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = if (isSelected)
                            Color(0xFF7C3AED)
                        else
                            Color(0xFF6B7280),

                        fontWeight = if (isSelected)
                            FontWeight.Bold
                        else
                            FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ====================================
        // NIVEL DE ENERGIA
        // ====================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Nivel de energía",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )

            Text(
                text = "${viewModel.energyLevel.toInt()}/10",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7C3AED)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = viewModel.energyLevel,
            onValueChange = {
                viewModel.updateEnergy(it)
            },
            valueRange = 1f..10f,
            steps = 8,
            enabled = !viewModel.isSaving,

            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF7C3AED),
                activeTrackColor = Color(0xFF7C3AED),
                inactiveTrackColor = Color(0xFFE5E7EB)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ====================================
        // ACTIVIDADES REALIZADAS
        // ====================================

        Text(
            text = "¿Qué has estado haciendo?",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            activities.forEach { activity ->

                val isSelected =
                    viewModel.selectedActivities.contains(
                        activity
                    )

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            enabled = !viewModel.isSaving
                        ) {
                            viewModel.toggleActivity(
                                activity
                            )
                        },

                    color = if (isSelected)
                        Color(0xFF7C3AED)
                    else
                        Color.White,

                    shape = RoundedCornerShape(20.dp),

                    border = if (!isSelected)
                        BorderStroke(
                            1.dp,
                            Color(0xFFE5E7EB)
                        )
                    else
                        null
                ) {

                    Text(
                        text = activity,
                        fontSize = 13.sp,

                        color = if (isSelected)
                            Color.White
                        else
                            Color(0xFF4B5563),

                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ====================================
        // HORAS DE SUEÑO - NUEVO
        // ====================================

        Text(
            text = "¿Cuántas horas dormiste?",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Registra tus horas de sueño de forma manual.",
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = viewModel.hoursSleepText,

            onValueChange = {
                viewModel.updateHoursSleep(it)
                errorMessage = null
            },

            modifier = Modifier.fillMaxWidth(),

            enabled = !viewModel.isSaving,

            label = {
                Text("Horas de sueño (opcional)")
            },

            placeholder = {
                Text("Ejemplo: 7.5")
            },

            singleLine = true,

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),

            shape = RoundedCornerShape(16.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Puedes usar decimales. Por ejemplo, 7.5 equivale a 7 horas y media.",
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ====================================
        // REFLEXION DEL DIA
        // ====================================

        Text(
            text = "Reflexión del día",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.reflectionText,

            onValueChange = {
                viewModel.updateReflection(it)
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),

            enabled = !viewModel.isSaving,

            shape = RoundedCornerShape(16.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),

            placeholder = {
                Text("Escribe cómo te sentiste hoy...")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ====================================
        // MOSTRAR ERRORES
        // ====================================

        if (errorMessage != null) {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFB91C1C),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ====================================
        // BOTON GUARDAR REGISTRO
        // ====================================

        Button(
            onClick = {

                errorMessage = null

                // Guardamos la información en Room.
                // Solo navegamos si el guardado
                // terminó correctamente.

                viewModel.saveCurrentEntry(

                    onSuccess = {
                        onSaved()
                    },

                    onError = { mensaje ->
                        errorMessage = mensaje
                    }
                )
            },

            enabled = !viewModel.isSaving,

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            shape = RoundedCornerShape(16.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C3AED),
                disabledContainerColor = Color(0xFFB9A3E7)
            )
        ) {

            if (viewModel.isSaving) {

                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Guardando...",
                    fontSize = 16.sp,
                    color = Color.White
                )

            } else {

                Text(
                    text = "Guardar Registro",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
