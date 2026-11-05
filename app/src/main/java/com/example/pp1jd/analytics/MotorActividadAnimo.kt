
package com.example.pp1jd.analytics

import kotlin.math.abs
import kotlin.math.sqrt

// ============================================
// DATOS DE ENTRADA
// ============================================

data class DatoActividadAnimo(
    val fechaIso: String,
    val animo: Int,
    val energia: Int,
    val actividades: List<String>
)

// ============================================
// RESULTADO POR ACTIVIDAD
// ============================================

data class EstadisticaPorActividad(
    val nombre: String,
    val diasConActividad: Int,
    val diasSinActividad: Int,
    val animoPromedio: Double?,
    val energiaPromedio: Double?,
    val diferenciaAnimoGeneral: Double?,
    val diferenciaEnergiaGeneral: Double?,
    val diferenciaAnimoSinActividad: Double?,
    val dCohen: Double?,
    val magnitudEfecto: String,
    val datosSuficientes: Boolean
)

// ============================================
// RESULTADO GENERAL
// ============================================

data class ResultadoActividadAnimo(
    val registrosValidos: Int,
    val diasAnalizados: Int,
    val animoGeneral: Double?,
    val energiaGeneral: Double?,
    val actividades: List<EstadisticaPorActividad>
)

// ============================================
// RESUMEN INTERNO DE CADA DIA
// ============================================
// Si existen varios registros en un dia,
// usamos sus promedios y reunimos las
// actividades sin duplicarlas.

private data class DiaActividad(
    val animo: Double,
    val energia: Double,
    val actividades: Set<String>
)

// ============================================
// MOTOR DE ACTIVIDAD VS ANIMO
// ============================================

object MotorActividadAnimo {

    // Umbrales minimos para reportar patrones
    const val MINIMO_DIAS_TOTALES = 14
    const val MINIMO_DIAS_POR_GRUPO = 5

    // ========================================
    // ANALIZAR ACTIVIDADES
    // ========================================
    // Metodo: diferencia de medias.
    //
    // Comparamos dias CON una actividad
    // contra dias SIN esa actividad.
    //
    // Cada fecha cuenta una sola vez,
    // evitando inflar el tamaño de muestra.

    fun analizar(
        registros: List<DatoActividadAnimo>
    ): ResultadoActividadAnimo {

        // ====================================
        // VALIDAR LOS REGISTROS
        // ====================================

        val validos = registros.filter {
            it.fechaIso.isNotBlank() &&
                    it.animo in 1..5 &&
                    it.energia in 1..10
        }

        // ====================================
        // AGRUPAR REGISTROS POR DIA
        // ====================================

        val dias = validos
            .groupBy { it.fechaIso }
            .values
            .map { registrosDia ->

                DiaActividad(
                    animo = registrosDia
                        .map { it.animo }
                        .average(),

                    energia = registrosDia
                        .map { it.energia }
                        .average(),

                    actividades = registrosDia
                        .flatMap { it.actividades }
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                )
            }

        // ====================================
        // CASO SIN DATOS
        // ====================================

        if (dias.isEmpty()) {

            return ResultadoActividadAnimo(
                registrosValidos = 0,
                diasAnalizados = 0,
                animoGeneral = null,
                energiaGeneral = null,
                actividades = emptyList()
            )
        }

        // ====================================
        // PROMEDIOS GENERALES
        // ====================================

        val mediaAnimoGeneral =
            dias.map { it.animo }.average()

        val mediaEnergiaGeneral =
            dias.map { it.energia }.average()

        // ====================================
        // OBTENER ACTIVIDADES DISTINTAS
        // ====================================

        val nombres = dias
            .flatMap { it.actividades }
            .distinct()
            .sorted()

        // ====================================
        // ANALIZAR CADA ACTIVIDAD
        // ====================================

        val resultados = nombres.map { nombre ->

            // Dias donde se registro
            // esta actividad.
            val con = dias.filter {
                nombre in it.actividades
            }

            // Dias donde NO se registro.
            val sin = dias.filter {
                nombre !in it.actividades
            }

            // =================================
            // COMPROBAR DATOS SUFICIENTES
            // =================================

            val hayPromedio =
                con.size >= MINIMO_DIAS_POR_GRUPO

            val suficientes =
                dias.size >= MINIMO_DIAS_TOTALES &&
                        con.size >= MINIMO_DIAS_POR_GRUPO &&
                        sin.size >= MINIMO_DIAS_POR_GRUPO

            // =================================
            // PROMEDIOS DE LA ACTIVIDAD
            // =================================

            val mediaConAnimo =
                con.map { it.animo }.average()

            val mediaConEnergia =
                con.map { it.energia }.average()

            // =================================
            // TAMAÑO DE EFECTO
            // =================================
            // Cohen d permite evaluar si la
            // diferencia es pequeña o grande
            // en relacion con la variabilidad.

            val efecto = if (suficientes) {

                calcularDCohen(
                    con.map { it.animo },
                    sin.map { it.animo }
                )

            } else {
                null
            }

            // =================================
            // CONSTRUIR RESULTADO
            // =================================

            EstadisticaPorActividad(
                nombre = nombre,

                diasConActividad = con.size,
                diasSinActividad = sin.size,

                animoPromedio =
                    if (hayPromedio)
                        mediaConAnimo
                    else null,

                energiaPromedio =
                    if (hayPromedio)
                        mediaConEnergia
                    else null,

                diferenciaAnimoGeneral =
                    if (suficientes)
                        mediaConAnimo - mediaAnimoGeneral
                    else null,

                diferenciaEnergiaGeneral =
                    if (suficientes)
                        mediaConEnergia - mediaEnergiaGeneral
                    else null,

                diferenciaAnimoSinActividad =
                    if (suficientes) {
                        mediaConAnimo -
                                sin.map { it.animo }.average()
                    } else null,

                dCohen = efecto,

                magnitudEfecto = when {

                    !suficientes ->
                        "Datos insuficientes"

                    efecto == null ->
                        "No estimable (sin variación)"

                    abs(efecto) < 0.2 ->
                        "Muy pequeña"

                    abs(efecto) < 0.5 ->
                        "Pequeña"

                    abs(efecto) < 0.8 ->
                        "Moderada"

                    else ->
                        "Grande"
                },

                datosSuficientes = suficientes
            )
        }

        // ====================================
        // DEVOLVER RESULTADO COMPLETO
        // ====================================

        return ResultadoActividadAnimo(
            registrosValidos = validos.size,
            diasAnalizados = dias.size,
            animoGeneral = mediaAnimoGeneral,
            energiaGeneral = mediaEnergiaGeneral,

            actividades = resultados
                .sortedByDescending {
                    it.diasConActividad
                }
        )
    }

    // ========================================
    // CALCULAR D DE COHEN
    // ========================================
    // Compara la diferencia de promedios
    // con la variabilidad de los grupos.
    //
    // No demuestra causalidad ni constituye
    // por si mismo significancia estadistica.

    private fun calcularDCohen(
        con: List<Double>,
        sin: List<Double>
    ): Double? {

        val nCon = con.size
        val nSin = sin.size

        val varianzaCombinada = (
                (nCon - 1) * varianzaMuestral(con) +
                        (nSin - 1) * varianzaMuestral(sin)
                ) / (nCon + nSin - 2)

        val diferencia =
            con.average() - sin.average()

        val desviacion =
            sqrt(varianzaCombinada)

        return when {

            desviacion > 0.0 ->
                diferencia / desviacion

            abs(diferencia) < 1e-9 ->
                0.0

            else ->
                null
        }
    }

    // ========================================
    // VARIANZA MUESTRAL
    // ========================================

    private fun varianzaMuestral(
        valores: List<Double>
    ): Double {

        if (valores.size < 2) return 0.0

        val promedio = valores.average()

        return valores.sumOf {
            (it - promedio) * (it - promedio)
        } / (valores.size - 1)
    }
}
