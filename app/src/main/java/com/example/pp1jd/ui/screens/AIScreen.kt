
package com.example.pp1jd.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.pp1jd.analytics.*
import com.example.pp1jd.ui.viewmodel.DiaryViewModel

import java.util.Locale

// ============================================
// COLORES DE MOODWAVE
// ============================================

private val MoradoIA = Color(0xFF7C3AED)
private val VerdeIA = Color(0xFF10B981)
private val NaranjaIA = Color(0xFFF97316)

private val FondoIA = Color(0xFFF8F9FA)
private val TextoIA = Color(0xFF1F2937)
private val SecundarioIA = Color(0xFF6B7280)

// ============================================
// FORMATEAR NUMEROS
// ============================================

private fun decimalIA(
    numero: Double?
): String {

    if (numero == null) {
        return "Sin datos"
    }

    return String.format(
        Locale.forLanguageTag("es-MX"),
        "%.2f",
        numero
    )
}

// ============================================
// FORMATEAR DIFERENCIAS
// ============================================

private fun signoIA(
    numero: Double
): String {

    return String.format(
        Locale.forLanguageTag("es-MX"),
        "%+.2f",
        numero
    )
}

// ============================================
// COLOR DE CADA GRUPO
// ============================================

private fun colorGrupo(
    numero: Int
): Color {

    return when ((numero - 1) % 3) {
        0 -> NaranjaIA
        1 -> MoradoIA
        else -> VerdeIA
    }
}

// ============================================
// PANTALLA PRINCIPAL DE INTELIGENCIA ARTIFICIAL
// ============================================

@Composable
fun AIScreen(
    viewModel: DiaryViewModel
) {

    // ========================================
    // LEER RESULTADOS DINAMICOS
    // ========================================
    // DiaryViewModel recalcula estos resultados
    // al cambiar los registros de Room.

    val estado = viewModel.estadoIA

    // ========================================
    // ESTADOS PARA MOSTRAR DETALLES
    // ========================================

    var grupoSeleccionado by remember {
        mutableStateOf<GrupoIA?>(null)
    }

    var anomaliaSeleccionada by remember {
        mutableStateOf<AnomaliaSostenidaIA?>(null)
    }

    // ========================================
    // INTERFAZ PRINCIPAL
    // ========================================

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoIA),

        contentPadding = PaddingValues(18.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        // ====================================
        // ENCABEZADO
        // ====================================

        item {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Inteligencia Artificial",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoIA
                )

                Text(
                    text = "Descubre similitudes entre tus días y cambios en tus tendencias emocionales.",
                    fontSize = 13.sp,
                    color = SecundarioIA
                )
            }
        }

        // ====================================
        // ESTADO DE ACTUALIZACION
        // ====================================

        if (estado.calculando) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {

                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MoradoIA,
                            strokeWidth = 3.dp
                        )

                        Column {

                            Text(
                                text = "Analizando registros",
                                fontWeight = FontWeight.Bold,
                                color = TextoIA
                            )

                            Text(
                                text = "Calculando agrupamientos y cambios sostenidos...",
                                fontSize = 12.sp,
                                color = SecundarioIA
                            )
                        }
                    }
                }
            }

        } else if (estado.error != null) {

            // =================================
            // ERROR CONTROLADO
            // =================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF1F2)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text = "No se pudo completar el análisis",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )

                        Text(
                            text = estado.error,
                            fontSize = 12.sp,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

        } else {

            // =================================
            // RESUMEN GENERAL
            // =================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFF3E8FF)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "Análisis actualizado",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoradoIA
                        )

                        Text(
                            text = "${estado.registrosProcesados} registros disponibles",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoIA
                        )

                        Text(
                            text = "Días analizados: ${estado.agrupamiento?.diasAnalizados ?: 0}",
                            fontSize = 13.sp,
                            color = SecundarioIA
                        )

                        Text(
                            text = "Los resultados se recalculan cuando cambian los registros guardados.",
                            fontSize = 12.sp,
                            color = SecundarioIA
                        )
                    }
                }
            }

            // =================================
            // SECCION DE AGRUPAMIENTO
            // =================================

            item {

                TituloSeccionIA(
                    titulo = "Agrupamiento de días",
                    subtitulo = "K-means identifica días con valores similares de ánimo y energía."
                )
            }

            val agrupamiento = estado.agrupamiento

            if (
                agrupamiento == null ||
                !agrupamiento.datosSuficientes
            ) {

                // =================================
                // INFORMACION INSUFICIENTE
                // =================================

                item {

                    TarjetaMensajeIA(
                        titulo = "Análisis no disponible",
                        mensaje = agrupamiento?.mensaje
                            ?: "Todavía no hay datos para analizar."
                    )
                }

            } else {

                // =================================
                // CANTIDAD DE GRUPOS
                // =================================

                item {

                    Text(
                        text = agrupamiento.mensaje,
                        fontSize = 13.sp,
                        color = SecundarioIA
                    )
                }

                // =================================
                // GRAFICA DE AGRUPAMIENTO
                // =================================

                item {

                    GraficaAgrupamientoIA(
                        grupos = agrupamiento.grupos
                    )
                }

                // =================================
                // TARJETAS DE CADA GRUPO
                // =================================

                items(
                    items = agrupamiento.grupos,
                    key = { "grupo_${it.numero}" }
                ) { grupo ->

                    TarjetaGrupoIA(
                        grupo = grupo,
                        onVerDias = {
                            grupoSeleccionado = grupo
                        }
                    )
                }

                // =================================
                // COMPARACION CON METODO BASE
                // =================================

                item {

                    ComparacionMetodosIA(
                        resultado = agrupamiento
                    )
                }
            }

            // =================================
            // SECCION DE ANOMALIAS
            // =================================

            item {

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                TituloSeccionIA(
                    titulo = "Cambios sostenidos",
                    subtitulo = "Compara cada día con su historial anterior para identificar cambios persistentes."
                )
            }

            val resultadoAnomalias =
                estado.anomalias

            if (
                resultadoAnomalias == null ||
                !resultadoAnomalias.datosSuficientes
            ) {

                item {

                    TarjetaMensajeIA(
                        titulo = "Análisis no disponible",
                        mensaje = resultadoAnomalias?.mensaje
                            ?: "Todavía no hay información suficiente."
                    )
                }

            } else {

                // =================================
                // RESUMEN DE ANOMALIAS
                // =================================

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {

                            Text(
                                text = resultadoAnomalias.mensaje,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextoIA
                            )

                            Text(
                                text = "${resultadoAnomalias.diasConReferencia} días evaluados con referencia histórica",
                                fontSize = 12.sp,
                                color = SecundarioIA
                            )

                            Text(
                                text = "${resultadoAnomalias.diasConCambioNotable} días con cambios notables",
                                fontSize = 12.sp,
                                color = SecundarioIA
                            )

                            Text(
                                text = "Periodos sostenidos encontrados: ${resultadoAnomalias.anomalias.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MoradoIA
                            )
                        }
                    }
                }

                // =================================
                // TARJETAS DE ANOMALIAS
                // =================================

                items(
                    items = resultadoAnomalias.anomalias,
                    key = {
                        "anomalia_${it.fechaInicio}_${it.fechaFin}_${it.tipo}"
                    }
                ) { anomalia ->

                    TarjetaAnomaliaIA(
                        anomalia = anomalia,
                        onVerDetalle = {
                            anomaliaSeleccionada = anomalia
                        }
                    )
                }
            }

            // =================================
            // EXPLICACION GENERAL
            // =================================

            item {

                Text(
                    text = "Estos análisis describen patrones estadísticos, no diagnósticos. Un cambio detectado no indica por sí solo un problema de salud.",
                    fontSize = 11.sp,
                    color = SecundarioIA
                )
            }
        }
    }

    // ========================================
    // VENTANA CON TODOS LOS DIAS DEL GRUPO
    // ========================================

    grupoSeleccionado?.let { grupo ->

        AlertDialog(
            onDismissRequest = {
                grupoSeleccionado = null
            },

            title = {
                Text(
                    text = "Días del grupo ${grupo.numero}"
                )
            },

            text = {

                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = grupo.dias,
                        key = { it.fechaIso }
                    ) { dia ->

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {

                            Text(
                                text = dia.fechaIso,
                                fontWeight = FontWeight.Bold,
                                color = TextoIA
                            )

                            Text(
                                text = "Ánimo: ${decimalIA(dia.promedioAnimo)}/5 · Energía: ${decimalIA(dia.promedioEnergia)}/10",
                                fontSize = 12.sp
                            )

                            Text(
                                text = "${dia.numeroRegistros} registro(s)",
                                fontSize = 11.sp,
                                color = SecundarioIA
                            )

                            if (dia.actividades.isNotEmpty()) {

                                Text(
                                    text = "Actividades: " +
                                            dia.actividades.joinToString(", "),
                                    fontSize = 11.sp,
                                    color = SecundarioIA
                                )
                            }

                            HorizontalDivider()
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        grupoSeleccionado = null
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    // ========================================
    // VENTANA DE DETALLES DE UNA ANOMALIA
    // ========================================

    anomaliaSeleccionada?.let { anomalia ->

        AlertDialog(
            onDismissRequest = {
                anomaliaSeleccionada = null
            },

            title = {
                Text(
                    text = "Detalle del cambio"
                )
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        text = anomalia.explicacion,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "Fechas incluidas",
                        fontWeight = FontWeight.Bold,
                        color = TextoIA
                    )

                    LazyColumn(
                        modifier = Modifier.height(220.dp)
                    ) {

                        items(
                            items = anomalia.fechas,
                            key = { it }
                        ) { fecha ->

                            Text(
                                text = fecha,
                                modifier =
                                    Modifier.padding(
                                        vertical = 5.dp
                                    ),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        anomaliaSeleccionada = null
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// ============================================
// TITULOS DE SECCION
// ============================================

@Composable
private fun TituloSeccionIA(
    titulo: String,
    subtitulo: String
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        Text(
            text = titulo,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = TextoIA
        )

        Text(
            text = subtitulo,
            fontSize = 12.sp,
            color = SecundarioIA
        )
    }
}

// ============================================
// TARJETA DE INFORMACION
// ============================================

@Composable
private fun TarjetaMensajeIA(
    titulo: String,
    mensaje: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = titulo,
                fontWeight = FontWeight.Bold,
                color = TextoIA
            )

            Text(
                text = mensaje,
                fontSize = 13.sp,
                color = SecundarioIA
            )
        }
    }
}

// ============================================
// GRAFICA DE AGRUPAMIENTO K-MEANS
// ============================================
// Cada punto representa un dia.
//
// Eje horizontal: energia de 1 a 10.
// Eje vertical: animo de 1 a 5.
//
// Los colores identifican los grupos
// encontrados automaticamente.

@Composable
private fun GraficaAgrupamientoIA(
    grupos: List<GrupoIA>
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Mapa de días similares",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextoIA
            )

            Text(
                text = "Cada círculo representa un día. Los círculos del mismo color pertenecen al mismo grupo.",
                fontSize = 12.sp,
                color = SecundarioIA
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {

                // =================================
                // ETIQUETAS DEL EJE VERTICAL
                // =================================

                Column(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight(),

                    verticalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    (5 downTo 1).forEach { valor ->

                        Text(
                            text = valor.toString(),
                            fontSize = 10.sp,
                            color = SecundarioIA
                        )
                    }
                }

                // =================================
                // DIBUJAR LOS DIAS
                // =================================

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {

                    val izquierda = 10.dp.toPx()
                    val derecha =
                        size.width - 10.dp.toPx()

                    val arriba = 10.dp.toPx()
                    val abajo =
                        size.height - 10.dp.toPx()

                    fun coordenada(
                        animo: Double,
                        energia: Double
                    ): Offset {

                        val xNormalizado =
                            ((energia - 1.0) / 9.0)
                                .coerceIn(0.0, 1.0)
                                .toFloat()

                        val yNormalizado =
                            ((animo - 1.0) / 4.0)
                                .coerceIn(0.0, 1.0)
                                .toFloat()

                        return Offset(
                            x = izquierda +
                                    (derecha - izquierda) *
                                    xNormalizado,

                            y = abajo -
                                    (abajo - arriba) *
                                    yNormalizado
                        )
                    }

                    // =================================
                    // LINEAS DE REFERENCIA
                    // =================================

                    for (nivel in 1..5) {

                        val y = coordenada(
                            nivel.toDouble(),
                            1.0
                        ).y

                        drawLine(
                            color = Color(0xFFE5E7EB),
                            start = Offset(izquierda, y),
                            end = Offset(derecha, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // =================================
                    // PUNTOS DE CADA GRUPO
                    // =================================

                    grupos.forEach { grupo ->

                        val color = colorGrupo(
                            grupo.numero
                        )

                        grupo.dias.forEach { dia ->

                            drawCircle(
                                color = color,
                                radius = 4.dp.toPx(),

                                center = coordenada(
                                    animo =
                                        dia.promedioAnimo,

                                    energia =
                                        dia.promedioEnergia
                                )
                            )
                        }
                    }
                }
            }

            // =================================
            // ETIQUETA HORIZONTAL
            // =================================

            Text(
                text = "Energía: 1 — 10",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = SecundarioIA
            )

            // =================================
            // LEYENDA DE GRUPOS
            // =================================

            grupos.forEach { grupo ->

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .background(
                                colorGrupo(grupo.numero),
                                RoundedCornerShape(50)
                            )
                    )

                    Text(
                        text = "Grupo ${grupo.numero}: ${grupo.nombre}",
                        fontSize = 11.sp,
                        color = TextoIA
                    )
                }
            }

            Text(
                text = "Si varios días tienen exactamente los mismos valores, sus círculos pueden superponerse.",
                fontSize = 11.sp,
                color = SecundarioIA
            )
        }
    }
}

// ============================================
// TARJETA DE UN GRUPO K-MEANS
// ============================================

@Composable
private fun TarjetaGrupoIA(
    grupo: GrupoIA,
    onVerDias: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .background(
                            colorGrupo(grupo.numero),
                            RoundedCornerShape(50)
                        )
                )

                Text(
                    text = "Grupo ${grupo.numero}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoIA
                )
            }

            Text(
                text = grupo.nombre,
                fontSize = 14.sp,
                color = SecundarioIA
            )

            Text(
                text = "${grupo.dias.size} días similares",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextoIA
            )

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                ValorGrupoIA(
                    titulo = "Ánimo",
                    valor = "${decimalIA(grupo.promedioAnimo)}/5",
                    modifier = Modifier.weight(1f)
                )

                ValorGrupoIA(
                    titulo = "Energía",
                    valor = "${decimalIA(grupo.promedioEnergia)}/10",
                    modifier = Modifier.weight(1f)
                )
            }

            // =================================
            // ACTIVIDADES FRECUENTES
            // =================================

            Text(
                text = "Actividades más frecuentes",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoIA
            )

            Text(
                text = if (
                    grupo.actividadesFrecuentes.isEmpty()
                ) {
                    "Sin actividades registradas"
                } else {
                    grupo.actividadesFrecuentes
                        .joinToString(", ")
                },
                fontSize = 12.sp,
                color = SecundarioIA
            )

            Text(
                text = "Los días se agruparon por su similitud en ánimo y energía; las actividades sirven para describir el grupo.",
                fontSize = 11.sp,
                color = SecundarioIA
            )

            OutlinedButton(
                onClick = onVerDias,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ver días del grupo"
                )
            }
        }
    }
}

// ============================================
// TARJETA PEQUEÑA DE VALOR
// ============================================

@Composable
private fun ValorGrupoIA(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E8FF)
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp),

            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {

            Text(
                text = titulo,
                fontSize = 11.sp,
                color = SecundarioIA
            )

            Text(
                text = valor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MoradoIA
            )
        }
    }
}

// ============================================
// COMPARACION CON EL METODO BASE
// ============================================

@Composable
private fun ComparacionMetodosIA(
    resultado: ResultadoAgrupamientoIA
) {

    val dispersionKMeans =
        resultado.dispersionKMeans

    val dispersionBase =
        resultado.dispersionBase

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Comparación de métodos",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextoIA
            )

            Text(
                text = "Comparamos K-means con un método sencillo que agrupa los días según su nivel de ánimo.",
                fontSize = 12.sp,
                color = SecundarioIA
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                ValorGrupoIA(
                    titulo = "K-means",
                    valor = decimalIA(
                        dispersionKMeans
                    ),
                    modifier = Modifier.weight(1f)
                )

                ValorGrupoIA(
                    titulo = "Método base",
                    valor = decimalIA(
                        dispersionBase
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (
                dispersionKMeans != null &&
                dispersionBase != null &&
                resultado.comparacionDirecta
            ) {

                val mensaje = when {

                    dispersionKMeans <
                            dispersionBase - 1e-9 ->

                        "K-means obtuvo una menor dispersión interna."

                    dispersionKMeans >
                            dispersionBase + 1e-9 ->

                        "El método base obtuvo una menor dispersión interna."

                    else ->

                        "Ambos métodos obtuvieron una dispersión similar."
                }

                Text(
                    text = mensaje,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoradoIA
                )

            } else {

                Text(
                    text = "Los métodos formaron distintas cantidades de grupos, por lo que sus dispersiones no se comparan directamente.",
                    fontSize = 12.sp,
                    color = SecundarioIA
                )
            }

            Text(
                text = "La dispersión mide qué tan cercanos quedan los días de cada grupo. Un valor menor indica grupos más compactos, pero no demuestra que el resultado tenga significado médico.",
                fontSize = 11.sp,
                color = SecundarioIA
            )
        }
    }
}

// ============================================
// TARJETA DE ANOMALIA SOSTENIDA
// ============================================

@Composable
private fun TarjetaAnomaliaIA(
    anomalia: AnomaliaSostenidaIA,
    onVerDetalle: () -> Unit
) {

    // ========================================
    // COLOR SEGUN TIPO DE CAMBIO
    // ========================================

    val color = when (anomalia.tipo) {

        TipoCambioIA.DESCENSO ->
            NaranjaIA

        TipoCambioIA.AUMENTO ->
            VerdeIA

        TipoCambioIA.MIXTO ->
            MoradoIA
    }

    // ========================================
    // TEXTO DEL TIPO DE CAMBIO
    // ========================================

    val titulo = when (anomalia.tipo) {

        TipoCambioIA.DESCENSO ->
            "Descenso sostenido"

        TipoCambioIA.AUMENTO ->
            "Aumento sostenido"

        TipoCambioIA.MIXTO ->
            "Cambio mixto"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = "${anomalia.fechaInicio} al ${anomalia.fechaFin}",
                fontSize = 13.sp,
                color = TextoIA
            )

            Text(
                text = "${anomalia.diasConsecutivos} días consecutivos · ${anomalia.numeroRegistros} registros",
                fontSize = 12.sp,
                color = SecundarioIA
            )

            HorizontalDivider()

            // =================================
            // VALORES OBSERVADOS
            // =================================

            Text(
                text = "Durante el cambio",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoIA
            )

            Text(
                text = "Ánimo: ${decimalIA(anomalia.animoPromedio)}/5",
                fontSize = 12.sp,
                color = TextoIA
            )

            Text(
                text = "Energía: ${decimalIA(anomalia.energiaPromedio)}/10",
                fontSize = 12.sp,
                color = TextoIA
            )

            // =================================
            // VALORES DE REFERENCIA
            // =================================

            Text(
                text = "Referencia histórica",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoIA
            )

            Text(
                text = "Ánimo: ${decimalIA(anomalia.animoReferencia)}/5",
                fontSize = 12.sp,
                color = SecundarioIA
            )

            Text(
                text = "Energía: ${decimalIA(anomalia.energiaReferencia)}/10",
                fontSize = 12.sp,
                color = SecundarioIA
            )

            HorizontalDivider()

            // =================================
            // DIFERENCIAS DETECTADAS
            // =================================

            Text(
                text = "Cambio de ánimo: ${signoIA(anomalia.diferenciaAnimo)} puntos",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )

            Text(
                text = "Cambio de energía: ${signoIA(anomalia.diferenciaEnergia)} puntos",
                fontSize = 13.sp,
                color = TextoIA
            )

            // =================================
            // DETALLE DE FECHAS Y EXPLICACION
            // =================================

            OutlinedButton(
                onClick = onVerDetalle,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Ver fechas y explicación"
                )
            }
        }
    }
}
