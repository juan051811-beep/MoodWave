package com.example.pp1jd.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.pp1jd.analytics.DatoEmocional
import com.example.pp1jd.analytics.DatoActividadAnimo
import com.example.pp1jd.analytics.MotorEstadisticas
import com.example.pp1jd.analytics.MotorTendencias
import com.example.pp1jd.analytics.MotorActividadAnimo
import com.example.pp1jd.analytics.ResultadoTendencia
import com.example.pp1jd.analytics.EstadisticaPorActividad
import com.example.pp1jd.analytics.MotorPasosAnimo
import com.example.pp1jd.analytics.RegistroParaRelacionPasos
import com.example.pp1jd.analytics.ResultadoRelacionPasos

import com.example.pp1jd.data.local.MoodWaveDatabase
import com.example.pp1jd.data.local.PasosDiariosEntity

import com.example.pp1jd.ui.viewmodel.DiaryEntry
import com.example.pp1jd.ui.viewmodel.DiaryViewModel

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ============================================
// COLORES PRINCIPALES DE MOODWAVE
// ============================================

private val Morado = Color(0xFF7C3AED)
private val Verde = Color(0xFF10B981)
private val Fondo = Color(0xFFF8F9FA)
private val Texto = Color(0xFF1F2937)
private val Secundario = Color(0xFF6B7280)

// ============================================
// CONVERTIR EMOCIONES A NUMEROS
// ============================================
// Se aceptan los nombres originales y
// algunas variantes equivalentes.
//
// Los calculos siguen usando la escala 1..5.

private fun valorAnimo(animo: String): Int? {

    return when (
        animo.trim().lowercase(Locale.ROOT)
    ) {

        "muy mal", "muy triste" -> 1

        "mal", "triste" -> 2

        "normal", "neutral", "regular" -> 3

        "bien", "feliz" -> 4

        "muy bien", "muy feliz", "excelente" -> 5

        else -> null
    }
}

// ============================================
// CONVERTIR NUMERO A EMOCION
// ============================================

private fun nombreAnimo(valor: Int): String {

    return when (valor) {
        1 -> "Muy mal"
        2 -> "Mal"
        3 -> "Normal"
        4 -> "Bien"
        5 -> "Muy bien"
        else -> "Desconocido"
    }
}

// ============================================
// FORMATEAR DECIMALES
// ============================================

private fun decimal(valor: Double?): String {

    if (valor == null) {
        return "Sin datos"
    }

    return String.format(
        Locale.forLanguageTag("es-MX"),
        "%.2f",
        valor
    )
}

// ============================================
// MOSTRAR NUMEROS CON SIGNO
// ============================================

private fun decimalConSigno(
    valor: Double?
): String {

    if (valor == null) {
        return "Sin datos"
    }

    return String.format(
        Locale.forLanguageTag("es-MX"),
        "%+.2f",
        valor
    )
}

// ============================================
// CONVERTIR FECHA A TEXTO ISO
// ============================================

private fun fechaISO(
    calendario: Calendar
): String {

    val formato = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    )

    return formato.format(
        calendario.time
    )
}

// ============================================
// CONVERTIR TEXTO ISO A CALENDARIO
// ============================================
// Permite utilizar la fecha del registro
// mas reciente como referencia.
//
// Esto evita que el resumen aparezca
// vacio solo porque hoy pertenece a
// una semana o mes sin registros.

private fun calendarioDesdeISO(
    fechaIso: String
): Calendar? {

    return try {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            isLenient = false
        }

        val fecha = formato.parse(fechaIso)
            ?: return null

        if (
            formato.format(fecha) != fechaIso
        ) {
            return null
        }

        Calendar.getInstance().apply {

            time = fecha

            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    } catch (e: Exception) {

        null
    }
}

// ============================================
// ENCONTRAR LA FECHA MAS RECIENTE
// ============================================
// Buscamos en TODOS los registros de Room.
//
// No importan los 90 casos iniciales:
// si despues se agregan mas registros,
// esta fecha se actualiza automaticamente.

private fun fechaMasReciente(
    registros: List<DiaryEntry>
): String {

    val hoy = fechaISO(
        Calendar.getInstance()
    )

    val fechasValidas = registros
        .map { it.dateIso }
        .filter { fecha ->

            fecha <= hoy &&
                    calendarioDesdeISO(fecha) != null
        }

    return fechasValidas.maxOrNull() ?: hoy
}

// ============================================
// OBTENER SEMANA O MES DEL RESUMEN
// ============================================
// retrocesos = 0:
// periodo mas reciente con informacion.
//
// retrocesos = 1:
// periodo anterior.
//
// retrocesos = 2:
// dos periodos anteriores.
//
// Funciona tanto para semanas como meses.

private fun obtenerRangoResumen(
    periodo: String,
    fechaReferencia: String,
    retrocesos: Int
): Pair<String, String> {

    val inicio =
        calendarioDesdeISO(fechaReferencia)
            ?: Calendar.getInstance()

    if (periodo == "Semana") {

        // ====================================
        // LUNES DE LA SEMANA
        // ====================================

        val diasDesdeLunes = (
                inicio.get(Calendar.DAY_OF_WEEK) -
                        Calendar.MONDAY + 7
                ) % 7

        inicio.add(
            Calendar.DAY_OF_YEAR,
            -diasDesdeLunes
        )

        // Retroceder semanas completas.

        inicio.add(
            Calendar.WEEK_OF_YEAR,
            -retrocesos
        )

        val fin =
            inicio.clone() as Calendar

        fin.add(
            Calendar.DAY_OF_YEAR,
            6
        )

        return Pair(
            fechaISO(inicio),
            fechaISO(fin)
        )

    } else {

        // ====================================
        // PRIMER DIA DEL MES
        // ====================================

        inicio.set(
            Calendar.DAY_OF_MONTH,
            1
        )

        // Retroceder meses completos.

        inicio.add(
            Calendar.MONTH,
            -retrocesos
        )

        val fin =
            inicio.clone() as Calendar

        fin.set(
            Calendar.DAY_OF_MONTH,
            fin.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )
        )

        return Pair(
            fechaISO(inicio),
            fechaISO(fin)
        )
    }
}

// ============================================
// OBTENER ULTIMOS 7, 30 O 90 DIAS
// ============================================
// Los demas graficos mantienen sus
// periodos independientes.

private fun obtenerRangoDias(
    dias: Int
): Pair<String, String> {

    require(
        dias == 7 || dias == 30 || dias == 90
    )

    val fin = Calendar.getInstance()

    val inicio =
        fin.clone() as Calendar

    inicio.add(
        Calendar.DAY_OF_YEAR,
        -(dias - 1)
    )

    return Pair(
        fechaISO(inicio),
        fechaISO(fin)
    )
}

// ============================================
// SELECTOR REUTILIZABLE DE DIAS
// ============================================

@Composable
private fun SelectorDias(
    seleccion: Int,
    onSeleccionar: (Int) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        listOf(7, 30, 90).forEach { dias ->

            FilterChip(
                selected = seleccion == dias,

                onClick = {
                    onSeleccionar(dias)
                },

                label = {
                    Text(
                        text = "$dias días",
                        fontSize = 12.sp
                    )
                },

                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor =
                            Color(0xFFF3E8FF),

                        selectedLabelColor = Morado
                    )
            )
        }
    }
}

// ============================================
// PANTALLA PRINCIPAL
// ============================================

@Composable
fun StatisticsScreen(
    viewModel: DiaryViewModel
) {

    // ========================================
    // TODOS LOS REGISTROS DE ROOM
    // ========================================
    // No se separan por origen.
    //
    // Incluye todos los registros anteriores
    // y todos los que se agreguen despues.

    val registros =
        viewModel.historyEntries.toList()

    // ========================================
    // SELECTOR DE RESUMEN SEMANA / MES
    // ========================================

    var periodoResumen by rememberSaveable {
        mutableStateOf("Mes")
    }

    // Cantidad de periodos que retrocedemos.

    var retrocesosResumen by rememberSaveable {
        mutableIntStateOf(0)
    }

    // ========================================
    // SELECTOR DE TENDENCIAS
    // ========================================

    var diasTendencia by rememberSaveable {
        mutableIntStateOf(30)
    }

    // ========================================
    // FECHA MAS RECIENTE DE ROOM
    // ========================================

    val ultimaFecha = fechaMasReciente(
        registros
    )

    // ========================================
    // PREPARAR DATOS EMOCIONALES
    // ========================================

    val datosEmocionales = remember(registros) {

        registros.mapNotNull { registro ->

            val animoNumerico =
                valorAnimo(registro.mood)

            if (animoNumerico == null) {

                null

            } else {

                DatoEmocional(
                    fechaIso = registro.dateIso,
                    animo = animoNumerico,
                    energia = registro.energy
                )
            }
        }
    }

    // ========================================
    // CORRECCION DEL RESUMEN EMOCIONAL
    // ========================================
    //
    // Antes dependiamos de la fecha actual.
    //
    // Ahora buscamos la semana o el mes
    // correspondiente al ultimo registro
    // disponible.
    //
    // Luego permitimos navegar por
    // periodos anteriores.

    val rangoResumen = obtenerRangoResumen(
        periodo = periodoResumen,
        fechaReferencia = ultimaFecha,
        retrocesos = retrocesosResumen
    )

    // Registros existentes en el periodo.

    val registrosResumen =
        registros.filter { registro ->

            registro.dateIso >= rangoResumen.first &&
                    registro.dateIso <= rangoResumen.second
        }

    // Valores numericos del mismo periodo.

    val datosResumen =
        datosEmocionales.filter { dato ->

            dato.fechaIso >= rangoResumen.first &&
                    dato.fechaIso <= rangoResumen.second
        }

    // Ejecutar motor de estadisticas.

    val resumen = remember(datosResumen) {

        MotorEstadisticas.resumir(
            datosResumen
        )
    }

    // Detectar registros con nombres de
    // emociones que no se pudieron convertir.

    val registrosSinAnimoValido =
        registrosResumen.size -
                resumen.numeroRegistros

    // ========================================
    // TENDENCIA EMOCIONAL
    // ========================================

    val tendencia = remember(
        datosEmocionales,
        diasTendencia
    ) {

        MotorTendencias.calcular(
            datos = datosEmocionales,
            dias = diasTendencia,
            referencia = Date()
        )
    }

    // ========================================
    // INTERFAZ PRINCIPAL
    // ========================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(18.dp)
    ) {

        // ====================================
        // ENCABEZADO
        // ====================================

        Text(
            text = "Tus Estadísticas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Texto
        )

        Text(
            text = "Descubre tus patrones emocionales",
            fontSize = 14.sp,
            color = Secundario
        )

        Text(
            text = "MoodWave no realiza diagnósticos médicos ni sustituye atención profesional.",
            fontSize = 11.sp,
            color = Secundario
        )

        // ====================================
        // RESUMEN EMOCIONAL
        // ====================================

        Text(
            text = "Resumen emocional",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Texto
        )

        // ====================================
        // SELECTOR DE SEMANA Y MES
        // ====================================

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            listOf("Semana", "Mes").forEach { opcion ->

                FilterChip(
                    selected =
                        periodoResumen == opcion,

                    onClick = {

                        periodoResumen = opcion

                        // Al cambiar de tipo de
                        // periodo, mostramos
                        // el mas reciente.
                        retrocesosResumen = 0
                    },

                    label = {
                        Text(opcion)
                    },

                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor =
                                Color(0xFFF3E8FF),

                            selectedLabelColor =
                                Morado
                        )
                )
            }
        }

        // ====================================
        // FECHAS EXACTAS DEL RESUMEN
        // ====================================

        Text(
            text = "Periodo: ${rangoResumen.first} al ${rangoResumen.second}",
            fontSize = 12.sp,
            color = Secundario
        )

        // ====================================
        // NAVEGACION ENTRE PERIODOS
        // ====================================
        //
        // Permite consultar semanas o meses
        // anteriores, sin perder informacion.

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            OutlinedButton(
                onClick = {
                    retrocesosResumen++
                }
            ) {

                Text(
                    text = "← Anterior",
                    fontSize = 12.sp
                )
            }

            TextButton(
                enabled = retrocesosResumen > 0,

                onClick = {

                    retrocesosResumen =
                        (retrocesosResumen - 1)
                            .coerceAtLeast(0)
                }
            ) {

                Text(
                    text = "Siguiente →",
                    fontSize = 12.sp
                )
            }
        }

        // ====================================
        // TARJETA DE DATOS ANALIZADOS
        // ====================================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(18.dp),

            colors = CardDefaults.cardColors(
                containerColor =
                    Color(0xFFF3E8FF)
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Datos analizados",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Morado
                )

                Text(
                    text = "${resumen.numeroRegistros} registros",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Texto
                )

                Text(
                    text = "${resumen.diasRegistrados} días diferentes",
                    fontSize = 13.sp,
                    color = Secundario
                )

                // Mostramos todos los registros
                // localizados para facilitar la
                // comprobacion de los calculos.

                Text(
                    text = "Registros guardados en este periodo: ${registrosResumen.size}",
                    fontSize = 11.sp,
                    color = Secundario
                )
            }
        }

        // ====================================
        // POSIBLES EMOCIONES NO RECONOCIDAS
        // ====================================

        if (registrosSinAnimoValido > 0) {

            Text(
                text = "Hay $registrosSinAnimoValido registros con un nombre de emoción no reconocido. No se incluyen en los cálculos.",
                fontSize = 12.sp,
                color = Color(0xFF92400E)
            )
        }

        // ====================================
        // ESTADISTICAS DESCRIPTIVAS
        // ====================================

        if (resumen.numeroRegistros == 0) {

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
                        text = "Sin datos para este periodo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Texto
                    )

                    Text(
                        text = "Puedes utilizar Anterior o Siguiente para consultar otras semanas o meses con registros.",
                        fontSize = 12.sp,
                        color = Secundario
                    )
                }
            }

        } else {

            // =================================
            // PROMEDIO Y MEDIANA
            // =================================

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                TarjetaEstadistica(
                    titulo = "Promedio",
                    valor = decimal(
                        resumen.promedio
                    ),
                    detalle = "Ánimo de 1 a 5",
                    modifier = Modifier.weight(1f)
                )

                TarjetaEstadistica(
                    titulo = "Mediana",
                    valor = decimal(
                        resumen.mediana
                    ),
                    detalle = "Valor central",
                    modifier = Modifier.weight(1f)
                )
            }

            // =================================
            // MODA Y DESVIACION
            // =================================

            val moda =
                if (resumen.modas.isEmpty()) {

                    "Sin moda"

                } else {

                    resumen.modas.joinToString(", ") {
                        nombreAnimo(it)
                    }
                }

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                TarjetaEstadistica(
                    titulo = "Moda",
                    valor = moda,
                    detalle = "Más frecuente",
                    modifier = Modifier.weight(1f)
                )

                TarjetaEstadistica(
                    titulo = "Desviación",
                    valor = decimal(
                        resumen.desviacionEstandar
                    ),
                    detalle = "Variación del ánimo",
                    modifier = Modifier.weight(1f)
                )
            }

            // =================================
            // ENERGIA Y RACHA MAXIMA
            // =================================

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                TarjetaEstadistica(
                    titulo = "Energía",
                    valor = decimal(
                        resumen.energiaPromedio
                    ) + "/10",
                    detalle = "Energía promedio",
                    modifier = Modifier.weight(1f)
                )

                TarjetaEstadistica(
                    titulo = "Racha máxima",
                    valor = "${resumen.rachaMaxima} días",
                    detalle = "Días consecutivos",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Cálculos realizados con n = ${resumen.numeroRegistros} registros. Desviación estándar poblacional.",
                fontSize = 11.sp,
                color = Secundario
            )
        }

        // ====================================
        // GRAFICA 1: TENDENCIA EMOCIONAL
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
                modifier = Modifier.padding(18.dp),

                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text = "📈 Tendencia emocional",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Texto
                )

                Text(
                    text = "Evolución del estado de ánimo.",
                    fontSize = 12.sp,
                    color = Secundario
                )

                // SELECTOR INDEPENDIENTE

                SelectorDias(
                    seleccion = diasTendencia,

                    onSeleccionar = {
                        diasTendencia = it
                    }
                )

                Text(
                    text = "n = ${tendencia.totalRegistros} registros · ${tendencia.diasConDatos} de ${tendencia.diasSolicitados} días",
                    fontSize = 12.sp,
                    color = Secundario
                )

                if (tendencia.totalRegistros > 0) {

                    GraficaTendencia(
                        resultado = tendencia
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(16.dp)
                    ) {

                        LeyendaGrafica(
                            nombre = "Ánimo diario",
                            color = Morado
                        )

                        LeyendaGrafica(
                            nombre = "Media móvil",
                            color = Verde
                        )
                    }

                    Text(
                        text = "La media móvil utiliza 7 días consecutivos. No se rellenan fechas sin registros.",
                        fontSize = 11.sp,
                        color = Secundario
                    )

                } else {

                    Text(
                        text = "No hay registros en los últimos $diasTendencia días.",
                        fontSize = 13.sp,
                        color = Secundario
                    )
                }
            }
        }

        // ====================================
        // GRAFICA 2: ACTIVIDADES FRECUENTES
        // ====================================

        SeccionActividadesFrecuentes(
            registros = registros
        )

        // ====================================
        // GRAFICA 3: MAPA DE CALOR
        // ====================================

        MapaCalorEmocional(
            registros = registros
        )

        // ====================================
        // ACTIVIDAD VS ANIMO
        // ====================================

        SeccionActividadAnimo(
            registros = registros
        )

        // ====================================
        // PASOS REALES VS. ANIMO Y ENERGIA
        // ====================================
        // Nueva seccion al final. Las graficas
        // anteriores permanecen intactas.

        SeccionPasosAnimo(
            registros = registros
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )
    }
}

// ============================================
// GRAFICA DE TENDENCIAS
// ============================================

@Composable
private fun GraficaTendencia(
    resultado: ResultadoTendencia
) {

    val puntos = resultado.puntos

    if (puntos.isEmpty()) return

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {

            // =================================
            // EJE VERTICAL
            // =================================

            Column(
                modifier = Modifier
                    .width(22.dp)
                    .fillMaxHeight(),

                verticalArrangement =
                    Arrangement.SpaceBetween
            ) {

                (5 downTo 1).forEach { numero ->

                    Text(
                        text = numero.toString(),
                        fontSize = 10.sp,
                        color = Secundario
                    )
                }
            }

            // =================================
            // DIBUJAR GRAFICA
            // =================================

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {

                val izquierda = 8.dp.toPx()

                val derecha =
                    size.width - 8.dp.toPx()

                val arriba = 8.dp.toPx()

                val abajo =
                    size.height - 8.dp.toPx()

                // =================================
                // CONVERTIR DATOS A COORDENADAS
                // =================================

                fun posicion(
                    indice: Int,
                    valor: Double
                ): Offset {

                    val proporcionX =
                        indice.toFloat() /
                                (puntos.size - 1)
                                    .coerceAtLeast(1)
                                    .toFloat()

                    val proporcionY =
                        ((valor.toFloat() - 1f) / 4f)
                            .coerceIn(0f, 1f)

                    return Offset(
                        x = izquierda +
                                (derecha - izquierda) *
                                proporcionX,

                        y = abajo -
                                (abajo - arriba) *
                                proporcionY
                    )
                }

                // =================================
                // LINEAS HORIZONTALES
                // =================================

                for (nivel in 1..5) {

                    val y = posicion(
                        0,
                        nivel.toDouble()
                    ).y

                    drawLine(
                        color = Color(0xFFE5E7EB),

                        start = Offset(
                            izquierda,
                            y
                        ),

                        end = Offset(
                            derecha,
                            y
                        ),

                        strokeWidth = 1.dp.toPx()
                    )
                }

                // =================================
                // LINEA MORADA: ANIMO DIARIO
                // =================================

                for (i in 1 until puntos.size) {

                    val anterior =
                        puntos[i - 1].promedioAnimo

                    val actual =
                        puntos[i].promedioAnimo

                    if (
                        anterior != null &&
                        actual != null
                    ) {

                        drawLine(
                            color = Morado,

                            start = posicion(
                                i - 1,
                                anterior
                            ),

                            end = posicion(
                                i,
                                actual
                            ),

                            strokeWidth =
                                2.5.dp.toPx(),

                            cap = StrokeCap.Round
                        )
                    }
                }

                // =================================
                // LINEA VERDE: MEDIA MOVIL
                // =================================

                for (i in 1 until puntos.size) {

                    val anterior =
                        puntos[i - 1].mediaMovil7

                    val actual =
                        puntos[i].mediaMovil7

                    if (
                        anterior != null &&
                        actual != null
                    ) {

                        drawLine(
                            color = Verde,

                            start = posicion(
                                i - 1,
                                anterior
                            ),

                            end = posicion(
                                i,
                                actual
                            ),

                            strokeWidth =
                                3.dp.toPx(),

                            cap = StrokeCap.Round
                        )
                    }
                }

                // =================================
                // CIRCULOS PARA 7 Y 30 DIAS
                // =================================

                if (puntos.size <= 30) {

                    puntos.forEachIndexed {
                            indice, punto ->

                        punto.promedioAnimo?.let { valor ->

                            drawCircle(
                                color = Morado,

                                radius = 3.dp.toPx(),

                                center = posicion(
                                    indice,
                                    valor
                                )
                            )
                        }

                        punto.mediaMovil7?.let { valor ->

                            drawCircle(
                                color = Verde,

                                radius = 2.5.dp.toPx(),

                                center = posicion(
                                    indice,
                                    valor
                                )
                            )
                        }
                    }
                }
            }
        }

        // ====================================
        // FECHAS DEL EJE HORIZONTAL
        // ====================================

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = puntos.first().fechaIso,
                fontSize = 10.sp,
                color = Secundario
            )

            Text(
                text = puntos.last().fechaIso,
                fontSize = 10.sp,
                color = Secundario
            )
        }
    }
}

// ============================================
// LEYENDA DE LAS GRAFICAS
// ============================================

@Composable
private fun LeyendaGrafica(
    nombre: String,
    color: Color
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .width(18.dp)
                .height(4.dp)
                .clip(
                    RoundedCornerShape(2.dp)
                )
                .background(color)
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(
            text = nombre,
            fontSize = 11.sp,
            color = Secundario
        )
    }
}

// ============================================
// GRAFICA 2: ACTIVIDADES MAS COMUNES
// ============================================
// Periodos independientes de 7, 30 y 90 dias.

@Composable
private fun SeccionActividadesFrecuentes(
    registros: List<DiaryEntry>
) {

    var diasActividades by rememberSaveable {
        mutableIntStateOf(30)
    }

    // ========================================
    // FILTRAR REGISTROS POR PERIODO
    // ========================================

    val rango = obtenerRangoDias(
        diasActividades
    )

    val registrosPeriodo = registros.filter {
        it.dateIso in rango.first..rango.second
    }

    // ========================================
    // CONTAR ACTIVIDADES
    // ========================================

    val actividades = registrosPeriodo
        .flatMap { registro ->
            registro.activities.distinct()
        }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }

    val mayorFrecuencia =
        actividades.maxOfOrNull {
            it.second
        } ?: 1

    val diasConDatos =
        registrosPeriodo.map {
            it.dateIso
        }.distinct().size

    // ========================================
    // INTERFAZ
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
            modifier = Modifier.padding(20.dp),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "📊 Actividades más comunes",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Texto
            )

            Text(
                text = "Actividades registradas con mayor frecuencia.",
                fontSize = 12.sp,
                color = Secundario
            )

            // SELECTOR INDEPENDIENTE

            SelectorDias(
                seleccion = diasActividades,

                onSeleccionar = {
                    diasActividades = it
                }
            )

            Text(
                text = "n = ${registrosPeriodo.size} registros · $diasConDatos días con datos",
                fontSize = 12.sp,
                color = Secundario
            )

            if (actividades.isEmpty()) {

                Text(
                    text = "No hay actividades registradas en este periodo.",
                    fontSize = 13.sp,
                    color = Secundario
                )

            } else {

                // =================================
                // BARRAS DE ACTIVIDADES
                // =================================

                actividades.forEachIndexed {
                        indice, actividad ->

                    val nombre = actividad.first
                    val cantidad = actividad.second

                    val progreso =
                        cantidad.toFloat() /
                                mayorFrecuencia.toFloat()

                    val color = when (indice % 3) {
                        0 -> Morado
                        1 -> Verde
                        else -> Color(0xFFF97316)
                    }

                    Column {

                        Row(
                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = nombre,
                                fontSize = 13.sp,
                                fontWeight =
                                    FontWeight.Medium,
                                color = Texto
                            )

                            Text(
                                text = "n = $cantidad",
                                fontSize = 12.sp,
                                color = Secundario
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        LinearProgressIndicator(
                            progress = {
                                progreso.coerceIn(0f, 1f)
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(
                                    RoundedCornerShape(5.dp)
                                ),

                            color = color,

                            trackColor =
                                Color(0xFFE5E7EB)
                        )
                    }
                }

                Text(
                    text = "Un registro puede contener varias actividades. Las barras se comparan con la actividad más frecuente.",
                    fontSize = 11.sp,
                    color = Secundario
                )
            }
        }
    }
}

// ============================================
// MODELO DEL MAPA DE CALOR
// ============================================

private data class CeldaCalor(
    val fechaIso: String,
    val numeroDia: Int,
    val enPeriodo: Boolean,
    val promedio: Double?,
    val cantidad: Int
)

// ============================================
// COLORES SEGUN ESTADO DE ANIMO
// ============================================

private fun colorCalor(
    promedio: Double?
): Color {

    if (promedio == null) {
        return Color(0xFFE5E7EB)
    }

    return when {

        promedio < 1.5 ->
            Color(0xFFFCA5A5)

        promedio < 2.5 ->
            Color(0xFFFDBA74)

        promedio < 3.5 ->
            Color(0xFFFDE68A)

        promedio < 4.5 ->
            Color(0xFF86EFAC)

        else ->
            Color(0xFF93C5FD)
    }
}

// ============================================
// GENERAR CELDAS DEL MAPA DE CALOR
// ============================================

private fun construirCeldasMapa(
    registros: List<DiaryEntry>,
    dias: Int
): List<CeldaCalor> {

    val hoy = Calendar.getInstance().apply {

        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // ========================================
    // RANGO DEL PERIODO SELECCIONADO
    // ========================================

    val inicio = hoy.clone() as Calendar

    inicio.add(
        Calendar.DAY_OF_YEAR,
        -(dias - 1)
    )

    val fechaInicio = fechaISO(inicio)
    val fechaFin = fechaISO(hoy)

    // ========================================
    // ALINEAR EL PRIMER DIA AL LUNES
    // ========================================

    val primerLunes =
        inicio.clone() as Calendar

    val desplazamiento = (
            primerLunes.get(Calendar.DAY_OF_WEEK) -
                    Calendar.MONDAY + 7
            ) % 7

    primerLunes.add(
        Calendar.DAY_OF_YEAR,
        -desplazamiento
    )

    // ========================================
    // TERMINAR LA CUADRICULA EN DOMINGO
    // ========================================

    val ultimoDomingo =
        hoy.clone() as Calendar

    val indiceSemana = (
            ultimoDomingo.get(Calendar.DAY_OF_WEEK) -
                    Calendar.MONDAY + 7
            ) % 7

    ultimoDomingo.add(
        Calendar.DAY_OF_YEAR,
        6 - indiceSemana
    )

    // ========================================
    // AGRUPAR REGISTROS POR FECHA
    // ========================================

    val registrosPorFecha =
        registros.groupBy {
            it.dateIso
        }

    val celdas =
        mutableListOf<CeldaCalor>()

    val cursor =
        primerLunes.clone() as Calendar

    // ========================================
    // CREAR CELDA PARA CADA DIA
    // ========================================

    while (!cursor.after(ultimoDomingo)) {

        val fecha = fechaISO(cursor)

        val enPeriodo =
            fecha in fechaInicio..fechaFin

        val registrosDia =
            if (enPeriodo) {

                registrosPorFecha[fecha].orEmpty()

            } else {

                emptyList()
            }

        val valores = registrosDia.mapNotNull {
            valorAnimo(it.mood)?.toDouble()
        }

        val promedio =
            if (valores.isNotEmpty()) {

                valores.average()

            } else {

                null
            }

        celdas.add(
            CeldaCalor(
                fechaIso = fecha,

                numeroDia = cursor.get(
                    Calendar.DAY_OF_MONTH
                ),

                enPeriodo = enPeriodo,

                promedio = promedio,

                cantidad = valores.size
            )
        )

        cursor.add(
            Calendar.DAY_OF_YEAR,
            1
        )
    }

    return celdas
}

// ============================================
// GRAFICA 3: MAPA DE CALOR EMOCIONAL
// ============================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapaCalorEmocional(
    registros: List<DiaryEntry>
) {

    // ========================================
    // SELECTOR INDEPENDIENTE
    // ========================================

    var diasMapa by rememberSaveable {
        mutableIntStateOf(30)
    }

    var fechaSeleccionada by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // ========================================
    // GENERAR MAPA
    // ========================================

    val celdas = remember(
        registros,
        diasMapa
    ) {

        construirCeldasMapa(
            registros = registros,
            dias = diasMapa
        )
    }

    val semanas =
        celdas.chunked(7)

    val registrosMapa =
        celdas.sumOf {
            it.cantidad
        }

    val diasConDatos =
        celdas.count {
            it.cantidad > 0
        }

    val celdaSeleccionada =
        celdas.firstOrNull {

            it.fechaIso == fechaSeleccionada &&
                    it.enPeriodo
        }

    // ========================================
    // INTERFAZ
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
            modifier = Modifier.padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "🗓️ Mapa de calor emocional",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Texto
            )

            Text(
                text = "Distribución del ánimo durante el periodo seleccionado.",
                fontSize = 12.sp,
                color = Secundario
            )

            // SELECTOR PROPIO

            SelectorDias(
                seleccion = diasMapa,

                onSeleccionar = {

                    diasMapa = it

                    fechaSeleccionada = null
                }
            )

            Text(
                text = "n = $registrosMapa registros · $diasConDatos de $diasMapa días",
                fontSize = 12.sp,
                color = Secundario
            )

            // =================================
            // ENCABEZADOS
            // =================================

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {

                listOf(
                    "L", "M", "M", "J", "V", "S", "D"
                ).forEach { dia ->

                    Text(
                        text = dia,

                        modifier = Modifier.weight(1f),

                        textAlign =
                            TextAlign.Center,

                        fontSize = 11.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color = Secundario
                    )
                }
            }

            // =================================
            // CUADRICULA DE FECHAS
            // =================================

            semanas.forEach { semana ->

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    semana.forEach { celda ->

                        val fondoCelda =
                            if (celda.enPeriodo) {

                                colorCalor(
                                    celda.promedio
                                )

                            } else {

                                Color.Transparent
                            }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(
                                    RoundedCornerShape(7.dp)
                                )
                                .background(
                                    fondoCelda
                                )
                                .clickable(
                                    enabled =
                                        celda.enPeriodo
                                ) {

                                    fechaSeleccionada =
                                        celda.fechaIso
                                },

                            contentAlignment =
                                Alignment.Center
                        ) {

                            if (celda.enPeriodo) {

                                Text(
                                    text = celda.numeroDia
                                        .toString(),

                                    fontSize = 10.sp,

                                    fontWeight =
                                        FontWeight.Medium,

                                    color = Texto
                                )
                            }
                        }
                    }
                }
            }

            // =================================
            // DETALLE DEL DIA SELECCIONADO
            // =================================

            if (celdaSeleccionada != null) {

                Surface(
                    color = Color(0xFFF3E8FF),

                    shape =
                        RoundedCornerShape(12.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text =
                                celdaSeleccionada.fechaIso,

                            fontSize = 13.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color = Morado
                        )

                        Text(
                            text =
                                if (
                                    celdaSeleccionada.promedio
                                    != null
                                ) {

                                    "Ánimo promedio: " +
                                            decimal(
                                                celdaSeleccionada.promedio
                                            ) +
                                            "/5 · n = " +
                                            celdaSeleccionada.cantidad

                                } else {

                                    "No hay registros para este día."
                                },

                            fontSize = 12.sp,
                            color = Texto
                        )
                    }
                }
            }

            // =================================
            // LEYENDA DE COLORES
            // =================================

            Text(
                text = "Escala emocional",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Texto
            )

            FlowRow(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                val leyenda = listOf(
                    "Muy mal" to 1.0,
                    "Mal" to 2.0,
                    "Normal" to 3.0,
                    "Bien" to 4.0,
                    "Muy bien" to 5.0
                )

                leyenda.forEach {
                        (nombre, valor) ->

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(
                                    RoundedCornerShape(3.dp)
                                )
                                .background(
                                    colorCalor(valor)
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.width(4.dp)
                        )

                        Text(
                            text = nombre,
                            fontSize = 10.sp,
                            color = Secundario
                        )
                    }
                }
            }

            Text(
                text = "Los colores muestran el promedio diario. El gris indica ausencia de registros. Toca una fecha para ver sus datos.",
                fontSize = 11.sp,
                color = Secundario
            )
        }
    }
}

// ============================================
// ACTIVIDAD VS ANIMO
// ============================================

@Composable
private fun SeccionActividadAnimo(
    registros: List<DiaryEntry>
) {

    // ========================================
    // SELECTOR PROPIO
    // ========================================

    var diasComparacion by rememberSaveable {
        mutableIntStateOf(30)
    }

    // ========================================
    // FILTRAR POR PERIODO
    // ========================================

    val rango = obtenerRangoDias(
        diasComparacion
    )

    val registrosPeriodo = registros.filter {
        it.dateIso in rango.first..rango.second
    }

    // ========================================
    // CONVERTIR DATOS PARA EL MOTOR
    // ========================================

    val datos = remember(
        registrosPeriodo
    ) {

        registrosPeriodo.mapNotNull {
                registro ->

            val animoNumerico =
                valorAnimo(registro.mood)

            if (animoNumerico == null) {

                null

            } else {

                DatoActividadAnimo(
                    fechaIso = registro.dateIso,
                    animo = animoNumerico,
                    energia = registro.energy,
                    actividades = registro.activities
                )
            }
        }
    }

    // ========================================
    // CALCULAR RESULTADOS
    // ========================================

    val resultado = remember(datos) {

        MotorActividadAnimo.analizar(
            datos
        )
    }

    // ========================================
    // INTERFAZ
    // ========================================

    Column(
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Actividad vs. Ánimo",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Texto
        )

        Text(
            text = "Compara las actividades con el ánimo y la energía.",
            fontSize = 12.sp,
            color = Secundario
        )

        // SELECTOR INDEPENDIENTE

        SelectorDias(
            seleccion = diasComparacion,

            onSeleccionar = {
                diasComparacion = it
            }
        )

        // ====================================
        // RESUMEN GENERAL
        // ====================================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(
                containerColor =
                    Color(0xFFF3E8FF)
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "Resumen de actividades",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Morado
                )

                Text(
                    text = "Periodo: ${rango.first} al ${rango.second}",
                    fontSize = 12.sp,
                    color = Secundario
                )

                Text(
                    text = "${resultado.diasAnalizados} días analizados · ${resultado.registrosValidos} registros",
                    fontSize = 13.sp,
                    color = Texto
                )

                Text(
                    text = "Ánimo general: ${decimal(resultado.animoGeneral)}/5",
                    fontSize = 13.sp,
                    color = Texto
                )

                Text(
                    text = "Energía general: ${decimal(resultado.energiaGeneral)}/10",
                    fontSize = 13.sp,
                    color = Texto
                )

                Text(
                    text = "Cada día se considera una vez mediante sus promedios diarios.",
                    fontSize = 11.sp,
                    color = Secundario
                )
            }
        }

        // ====================================
        // RESULTADOS POR ACTIVIDAD
        // ====================================

        if (resultado.actividades.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(18.dp),

                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Text(
                    text = "No hay actividades para analizar en este periodo.",
                    modifier = Modifier.padding(18.dp),
                    fontSize = 13.sp,
                    color = Secundario
                )
            }

        } else {

            resultado.actividades.forEach {
                    actividad ->

                TarjetaActividadAnimo(
                    actividad = actividad
                )
            }
        }

        // ====================================
        // EXPLICACION DEL METODO
        // ====================================

        Text(
            text = "Método: diferencia de medias entre días con y sin actividad. Se requieren al menos 14 días totales y 5 días en cada grupo.",
            fontSize = 11.sp,
            color = Secundario
        )

        Text(
            text = "El tamaño del efecto se calcula con d de Cohen. Las asociaciones no demuestran causalidad ni significancia estadística.",
            fontSize = 11.sp,
            color = Secundario
        )
    }
}

// ============================================
// TARJETA INDIVIDUAL DE ACTIVIDAD
// ============================================

@Composable
private fun TarjetaActividadAnimo(
    actividad: EstadisticaPorActividad
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
            modifier = Modifier.padding(18.dp),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = actividad.nombre,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Texto
            )

            Text(
                text = "Con actividad: ${actividad.diasConActividad} días · Sin actividad: ${actividad.diasSinActividad} días",
                fontSize = 12.sp,
                color = Secundario
            )

            if (!actividad.datosSuficientes) {

                Text(
                    text = "Aún no hay datos suficientes para evaluar esta relación.",
                    fontSize = 13.sp,
                    color = Color(0xFF92400E)
                )

            } else {

                // =================================
                // PROMEDIOS DE ANIMO Y ENERGIA
                // =================================

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    TarjetaEstadistica(
                        titulo = "Ánimo",
                        valor = decimal(
                            actividad.animoPromedio
                        ) + "/5",
                        detalle = "Con actividad",
                        modifier = Modifier.weight(1f)
                    )

                    TarjetaEstadistica(
                        titulo = "Energía",
                        valor = decimal(
                            actividad.energiaPromedio
                        ) + "/10",
                        detalle = "Con actividad",
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(
                    color = Color(0xFFE5E7EB)
                )

                Text(
                    text = "Diferencia de ánimo frente al promedio general: ${decimalConSigno(actividad.diferenciaAnimoGeneral)} puntos",
                    fontSize = 13.sp,
                    color = Texto
                )

                Text(
                    text = "Diferencia de energía frente al promedio general: ${decimalConSigno(actividad.diferenciaEnergiaGeneral)} puntos",
                    fontSize = 13.sp,
                    color = Texto
                )

                Text(
                    text = "Diferencia frente a días sin esta actividad: ${decimalConSigno(actividad.diferenciaAnimoSinActividad)} puntos de ánimo",
                    fontSize = 13.sp,
                    color = Texto
                )

                HorizontalDivider(
                    color = Color(0xFFE5E7EB)
                )

                Text(
                    text = "Tamaño del efecto: ${actividad.magnitudEfecto}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Morado
                )

                Text(
                    text = "d de Cohen: ${decimal(actividad.dCohen)}",
                    fontSize = 12.sp,
                    color = Secundario
                )

                Text(
                    text = "Se observa una asociación entre la actividad y el ánimo en esta muestra. No demuestra una relación causal.",
                    fontSize = 12.sp,
                    color = Secundario
                )
            }
        }
    }
}

// ============================================
// TARJETA REUTILIZABLE DE ESTADISTICAS
// ============================================

@Composable
private fun TarjetaEstadistica(
    titulo: String,
    valor: String,
    detalle: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = titulo,
                fontSize = 12.sp,
                color = Secundario
            )

            Text(
                text = valor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Morado
            )

            Text(
                text = detalle,
                fontSize = 11.sp,
                color = Secundario
            )
        }
    }
}

// ============================================
// GRAFICA 4: PASOS VS. ANIMO Y ENERGIA
// ============================================
//
// Este modulo utiliza los pasos ya guardados
// en Room mediante Health Connect.
//
// Cada dia se compara una sola vez, aunque
// existan varios registros emocionales.
//
// No modifica registros, no sincroniza pasos
// y no altera los motores anteriores.

@Composable
private fun SeccionPasosAnimo(
    registros: List<DiaryEntry>
) {

    val contexto = LocalContext.current

    // Room avisa automaticamente si los pasos
    // cambian o si el usuario elimina su copia.
    val flujoPasos = remember(contexto) {
        MoodWaveDatabase
            .obtenerInstancia(contexto)
            .pasosDiariosDao()
            .observarTodos()
    }

    val pasosGuardados by flujoPasos.collectAsState(
        initial = emptyList()
    )

    // Este selector es independiente de los
    // selectores de las otras graficas.
    var diasPasos by rememberSaveable {
        mutableIntStateOf(30)
    }

    var variableDispersion by rememberSaveable {
        mutableStateOf("Ánimo")
    }

    // Convertimos todos los registros emocionales,
    // incluidos los personales y los sinteticos.
    // Los nombres que no se reconocen no se inventan.
    val emocionesParaPasos = remember(registros) {
        registros.mapNotNull { registro ->
            val puntuacion = valorAnimo(registro.mood)
                ?: return@mapNotNull null

            RegistroParaRelacionPasos(
                fechaIso = registro.dateIso,
                valorAnimo = puntuacion,
                energia = registro.energy,
                esSintetico = registro.isSynthetic
            )
        }
    }

    // Recalculamos cuando hay nuevos registros,
    // cambia Room o el usuario elige otro periodo.
    val resultado = remember(
        emocionesParaPasos,
        pasosGuardados,
        diasPasos
    ) {
        MotorPasosAnimo.analizar(
            registros = emocionesParaPasos,
            pasosDiarios = pasosGuardados,
            periodoDias = diasPasos,
            fechaFinal = LocalDate.now()
        )
    }

    val formatoEnteros = remember {
        NumberFormat.getNumberInstance(
            Locale.forLanguageTag("es-MX")
        )
    }

    val rango = obtenerRangoDias(diasPasos)

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "👟 Pasos, ánimo y energía",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Texto
            )

            Text(
                text = "Datos de pasos guardados desde Health Connect y registros emocionales de MoodWave.",
                fontSize = 12.sp,
                color = Secundario
            )

            SelectorDias(
                seleccion = diasPasos,
                onSeleccionar = { diasPasos = it }
            )

            Text(
                text = "Periodo: ${rango.first} al ${rango.second}",
                fontSize = 11.sp,
                color = Secundario
            )

            // ====================================
            // RESUMEN DE PASOS GUARDADOS
            // ====================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaEstadistica(
                    titulo = "Pasos totales",
                    valor = formatoEnteros.format(resultado.totalPasos),
                    detalle = "Solo días con datos",
                    modifier = Modifier.weight(1f)
                )

                TarjetaEstadistica(
                    titulo = "Promedio diario",
                    valor = resultado.promedioPasos?.let {
                        formatoEnteros.format(it)
                    } ?: "Sin datos",
                    detalle = "Días con pasos",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaEstadistica(
                    titulo = "Mayor registro",
                    valor = resultado.maximoPasos?.let {
                        formatoEnteros.format(it)
                    } ?: "Sin datos",
                    detalle = "Máximo diario",
                    modifier = Modifier.weight(1f)
                )

                TarjetaEstadistica(
                    titulo = "Días comparables",
                    valor = "${resultado.diasComparables}",
                    detalle = "Pasos y ánimo",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Cobertura: ${resultado.diasConPasos} de $diasPasos días con pasos · ${resultado.diasConEmociones} días con emociones.",
                fontSize = 12.sp,
                color = Secundario
            )

            HorizontalDivider(color = Color(0xFFE5E7EB))

            // ====================================
            // GRAFICA DE PASOS DIARIOS
            // ====================================

            Text(
                text = "Pasos diarios",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Texto
            )

            if (resultado.diasConPasos > 0) {
                GraficaPasosDiarios(
                    pasos = pasosGuardados,
                    diasPeriodo = diasPasos
                )
            } else {
                Text(
                    text = "No hay pasos guardados en este periodo. Abre Inicio y pulsa 'Guardar y actualizar pasos'.",
                    fontSize = 12.sp,
                    color = Secundario
                )
            }

            Text(
                text = "Las fechas sin pasos almacenados quedan vacías: no se convierten en 0. La gráfica refleja la última sincronización con Room.",
                fontSize = 11.sp,
                color = Secundario
            )

            HorizontalDivider(color = Color(0xFFE5E7EB))

            // ====================================
            // CORRELACIONES DE PEARSON
            // ====================================

            Text(
                text = "Relación con el bienestar",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Texto
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaEstadistica(
                    titulo = "Pasos y ánimo",
                    valor = decimal(resultado.correlacionPasosAnimo),
                    detalle = "Correlación r",
                    modifier = Modifier.weight(1f)
                )

                TarjetaEstadistica(
                    titulo = "Pasos y energía",
                    valor = decimal(resultado.correlacionPasosEnergia),
                    detalle = "Correlación r",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = resultado.mensaje,
                fontSize = 12.sp,
                color = if (resultado.diasComparables < 7) {
                    Color(0xFF92400E)
                } else {
                    Secundario
                }
            )

            if (resultado.diasComparables > 0) {

                // Visualizamos puntos reales incluso si aun
                // no hay 7 dias para calcular Pearson.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Ánimo", "Energía").forEach { opcion ->
                        FilterChip(
                            selected = variableDispersion == opcion,
                            onClick = { variableDispersion = opcion },
                            label = {
                                Text(opcion, fontSize = 12.sp)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF3E8FF),
                                selectedLabelColor = Morado
                            )
                        )
                    }
                }

                Text(
                    text = "Cada punto representa un día con datos de pasos y de $variableDispersion.",
                    fontSize = 11.sp,
                    color = Secundario
                )

                GraficaDispersionPasos(
                    resultado = resultado,
                    mostrarEnergia = variableDispersion == "Energía"
                )
            }

            if (resultado.incluyeDatosSinteticos) {
                Text(
                    text = "Aviso: algunos días comparados incluyen emociones de prueba. Los resultados no deben interpretarse como conclusiones personales.",
                    fontSize = 11.sp,
                    color = Color(0xFF92400E)
                )
            }

            Text(
                text = "Pearson requiere al menos 7 días comparables y variación en ambas variables. Una correlación no prueba causalidad ni significancia estadística.",
                fontSize = 11.sp,
                color = Secundario
            )
        }
    }
}

// ============================================
// GRAFICA DE BARRAS: PASOS POR DIA
// ============================================
//
// Dibuja todos los dias del periodo.
// Un dia sin datos no tiene barra.

@Composable
private fun GraficaPasosDiarios(
    pasos: List<PasosDiariosEntity>,
    diasPeriodo: Int
) {
    val fechas = remember(pasos, diasPeriodo) {
        val porFecha = pasos.associateBy { it.fechaIso }
        val inicio = LocalDate.now()
            .minusDays(diasPeriodo.toLong() - 1L)

        (0 until diasPeriodo).map { indice ->
            val fecha = inicio.plusDays(indice.toLong()).toString()
            fecha to porFecha[fecha]?.pasos
        }
    }

    val maximo = (fechas.mapNotNull { it.second }.maxOrNull() ?: 0L)
        .coerceAtLeast(1L)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        val margenInferior = 8.dp.toPx()
        val margenSuperior = 8.dp.toPx()
        val alturaUtil = (size.height - margenInferior - margenSuperior)
            .coerceAtLeast(1f)
        val yBase = size.height - margenInferior

        drawLine(
            color = Color(0xFFD1D5DB),
            start = Offset(0f, yBase),
            end = Offset(size.width, yBase),
            strokeWidth = 1.dp.toPx()
        )

        val anchoDia = size.width / fechas.size.coerceAtLeast(1)
        val anchoBarra = (anchoDia * 0.72f).coerceAtLeast(1f)

        fechas.forEachIndexed { indice, (_, total) ->
            if (total != null && total >= 0L) {
                val x = indice * anchoDia + (anchoDia - anchoBarra) / 2f
                val proporcion = total.toFloat() / maximo.toFloat()
                val altura = (alturaUtil * proporcion)
                    .coerceAtLeast(2.dp.toPx())

                drawRect(
                    color = Morado,
                    topLeft = Offset(x, yBase - altura),
                    size = androidx.compose.ui.geometry.Size(
                        anchoBarra,
                        altura
                    )
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = fechas.first().first,
            fontSize = 10.sp,
            color = Secundario
        )

        Text(
            text = "Máx.: $maximo pasos",
            fontSize = 10.sp,
            color = Secundario
        )

        Text(
            text = fechas.last().first,
            fontSize = 10.sp,
            color = Secundario
        )
    }
}

// ============================================
// GRAFICA DE DISPERSION: PASOS Y BIENESTAR
// ============================================
//
// Cada punto representa una fecha diferente.
// Eje horizontal: pasos.
// Eje vertical: animo (1..5) o energia (1..10).

@Composable
private fun GraficaDispersionPasos(
    resultado: ResultadoRelacionPasos,
    mostrarEnergia: Boolean
) {
    val dias = resultado.dias
    if (dias.isEmpty()) return

    val maximoPasos = dias.maxOf { it.pasos }
        .coerceAtLeast(1L).toDouble()

    val minimoY = 1.0
    val maximoY = if (mostrarEnergia) 10.0 else 5.0
    val colorPuntos = if (mostrarEnergia) Verde else Morado

    Text(
        text = if (mostrarEnergia) {
            "Vertical: energía (1–10) · Horizontal: pasos"
        } else {
            "Vertical: ánimo (1–5) · Horizontal: pasos"
        },
        fontSize = 11.sp,
        color = Secundario
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        val xIzq = 12.dp.toPx()
        val xDer = size.width - 10.dp.toPx()
        val yArriba = 10.dp.toPx()
        val yAbajo = size.height - 12.dp.toPx()
        val ancho = (xDer - xIzq).coerceAtLeast(1f)
        val alto = (yAbajo - yArriba).coerceAtLeast(1f)

        for (i in 0..4) {
            val y = yArriba + alto * i.toFloat() / 4f
            drawLine(
                color = Color(0xFFE5E7EB),
                start = Offset(xIzq, y),
                end = Offset(xDer, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        drawLine(
            color = Color(0xFF9CA3AF),
            start = Offset(xIzq, yArriba),
            end = Offset(xIzq, yAbajo),
            strokeWidth = 1.dp.toPx()
        )

        dias.forEach { dia ->
            val valor = if (mostrarEnergia) {
                dia.energiaPromedio
            } else {
                dia.animoPromedio
            }

            val x = xIzq + ancho *
                    (dia.pasos.toDouble() / maximoPasos).toFloat()
            val proporcionY = ((valor - minimoY) / (maximoY - minimoY))
                .toFloat().coerceIn(0f, 1f)
            val y = yAbajo - alto * proporcionY

            drawCircle(
                color = colorPuntos,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("0 pasos", fontSize = 10.sp, color = Secundario)
        Text("${dias.size} días comparables", fontSize = 10.sp, color = Secundario)
        Text("${maximoPasos.toLong()} pasos (máx.)", fontSize = 10.sp, color = Secundario)
    }
}
