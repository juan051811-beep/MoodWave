
package com.example.pp1jd.analytics

import com.example.pp1jd.data.local.PasosDiariosEntity

import java.time.LocalDate
import java.util.Locale

import kotlin.math.sqrt

// ============================================
// REGISTRO EMOCIONAL PARA EL ANALISIS
// ============================================
//
// Representa la informacion necesaria
// de un registro emocional.
//
// fechaIso:
// Fecha en formato yyyy-MM-dd.
//
// valorAnimo:
// Estado de animo entre 1 y 5.
//
// energia:
// Nivel de energia entre 1 y 10.
//
// esSintetico:
// Indica si el registro proviene
// del generador de datos de prueba.

data class RegistroParaRelacionPasos(

    val fechaIso: String,

    val valorAnimo: Int,

    val energia: Int,

    val esSintetico: Boolean = false
)

// ============================================
// RESULTADO DE UN DIA
// ============================================
//
// Reune la informacion de pasos
// y emociones de una misma fecha.
//
// Si existen varias emociones ese dia,
// se utiliza su promedio.
//
// Los pasos se cuentan una sola vez.

data class DiaRelacionPasos(

    val fechaIso: String,

    val pasos: Long,

    val animoPromedio: Double,

    val energiaPromedio: Double,

    val cantidadRegistros: Int,

    val incluyeDatosSinteticos: Boolean
)

// ============================================
// RESULTADO GENERAL DEL ANALISIS
// ============================================

data class ResultadoRelacionPasos(

    val periodoDias: Int,

    // Dias con pasos almacenados.

    val diasConPasos: Int,

    // Dias con registros emocionales.

    val diasConEmociones: Int,

    // Dias que tienen ambos tipos de datos.

    val diasComparables: Int,

    // Suma de los pasos disponibles.

    val totalPasos: Long,

    // Promedio por dia con pasos disponibles.

    val promedioPasos: Double?,

    // Mayor cantidad diaria registrada.

    val maximoPasos: Long?,

    // Correlaciones de Pearson.
    //
    // null significa que no pueden
    // calcularse de forma valida.

    val correlacionPasosAnimo: Double?,

    val correlacionPasosEnergia: Double?,

    // Advierte sobre datos de prueba
    // utilizados en el analisis.

    val incluyeDatosSinteticos: Boolean,

    // Explicacion sencilla.

    val mensaje: String,

    // Datos diarios listos para graficas.

    val dias: List<DiaRelacionPasos>
)

// ============================================
// MOTOR DE ANALISIS PASOS Y ANIMO
// ============================================
//
// Este motor es independiente de
// las pantallas de Jetpack Compose.
//
// No modifica Room.
//
// No consulta Health Connect.
//
// Solo procesa los datos recibidos.
//
// Asi podemos utilizarlo desde:
//
// - StatisticsScreen.
// - Otras pantallas.
// - Pruebas unitarias.
//
// No sustituye los motores de IA
// que ya tenemos funcionando.

object MotorPasosAnimo {

    // ========================================
    // CONVERTIR NOMBRE DE ANIMO A NUMERO
    // ========================================
    //
    // Nuestros registros utilizan:
    //
    // Muy mal  = 1
    // Mal      = 2
    // Normal   = 3
    // Bien     = 4
    // Muy bien = 5
    //
    // Esta funcion facilita integrar
    // los registros del DiaryViewModel.
    //
    // Si llega un nombre desconocido,
    // devuelve null en lugar de inventar
    // una puntuacion.

    fun valorDeAnimo(
        nombre: String
    ): Int? {

        val texto = nombre
            .trim()
            .lowercase(
                Locale.forLanguageTag("es-MX")
            )

        return when (texto) {

            "muy mal" -> 1

            "mal" -> 2

            "normal" -> 3

            "bien" -> 4

            "muy bien" -> 5

            else -> null
        }
    }

    // ========================================
    // ANALIZAR PASOS Y EMOCIONES
    // ========================================
    //
    // Recibe:
    //
    // registros:
    // Todos los registros emocionales.
    //
    // pasosDiarios:
    // Todos los pasos guardados en Room.
    //
    // periodoDias:
    // Cantidad de dias a analizar.
    //
    // fechaFinal:
    // Ultimo dia del periodo.
    //
    // Por defecto se utiliza hoy.
    //
    // Se pueden analizar 7, 30 y 90 dias
    // sin limitarse a los datos iniciales.

    fun analizar(

        registros: List<RegistroParaRelacionPasos>,

        pasosDiarios: List<PasosDiariosEntity>,

        periodoDias: Int = 30,

        fechaFinal: LocalDate = LocalDate.now()

    ): ResultadoRelacionPasos {

        // ====================================
        // VALIDAR PERIODO
        // ====================================

        require(
            periodoDias in 1..365
        ) {

            "El periodo debe estar entre " +
                    "1 y 365 días."
        }

        // ====================================
        // CALCULAR FECHAS
        // ====================================
        //
        // El periodo incluye la fecha final.
        //
        // Ejemplo:
        //
        // 7 dias = hoy y los 6 anteriores.

        val fechaInicio = fechaFinal.minusDays(
            periodoDias.toLong() - 1L
        )

        val inicioIso = fechaInicio.toString()

        val finIso = fechaFinal.toString()

        // ====================================
        // COMPROBAR FECHA EN EL PERIODO
        // ====================================
        //
        // Como usamos yyyy-MM-dd,
        // las fechas ISO permiten
        // comparaciones ordenadas.

        fun estaEnPeriodo(
            fechaIso: String
        ): Boolean {

            return fechaIso >= inicioIso &&
                    fechaIso <= finIso
        }

        // ====================================
        // FILTRAR REGISTROS EMOCIONALES
        // ====================================
        //
        // Incluimos los registros reales
        // y los sinteticos.
        //
        // No dividimos la informacion.
        //
        // Descartamos valores invalidos
        // para evitar resultados incorrectos.

        val registrosValidos = registros.filter {

            estaEnPeriodo(it.fechaIso) &&

                    it.valorAnimo in 1..5 &&

                    it.energia in 1..10
        }

        // ====================================
        // AGRUPAR EMOCIONES POR FECHA
        // ====================================
        //
        // Si una persona registra:
        //
        // 08:00 -> animo 3
        // 14:00 -> animo 4
        // 21:00 -> animo 5
        //
        // Su promedio de ese dia sera:
        //
        // (3 + 4 + 5) / 3 = 4
        //
        // De esta forma cada dia cuenta
        // una sola vez en la correlacion.

        val emocionesPorFecha =
            registrosValidos.groupBy {

                it.fechaIso
            }

        // ====================================
        // FILTRAR PASOS VALIDOS
        // ====================================
        //
        // Conservamos exclusivamente
        // los dias del periodo solicitado.
        //
        // Un total de cero pasos es valido.
        //
        // Un dia sin registro de pasos
        // no equivale a cero.
        //
        // Room ya garantiza una fila
        // por fecha mediante PrimaryKey.

        val pasosPorFecha =
            pasosDiarios
                .filter {

                    estaEnPeriodo(it.fechaIso) &&
                            it.pasos >= 0L
                }
                .associateBy {

                    it.fechaIso
                }

        // ====================================
        // PREPARAR DIAS COMPARABLES
        // ====================================
        //
        // Solo relacionamos las fechas
        // que tienen:
        //
        // 1. Pasos disponibles.
        // 2. Registros emocionales.
        //
        // Nunca inventamos datos
        // para completar fechas faltantes.

        val diasComparables =
            emocionesPorFecha.mapNotNull {
                    (fecha, emocionesDelDia) ->

                val registroPasos =
                    pasosPorFecha[fecha]

                if (registroPasos == null) {

                    null

                } else {

                    val promedioAnimo =
                        emocionesDelDia
                            .map {

                                it.valorAnimo.toDouble()
                            }
                            .average()

                    val promedioEnergia =
                        emocionesDelDia
                            .map {

                                it.energia.toDouble()
                            }
                            .average()

                    val tieneSinteticos =
                        emocionesDelDia.any {

                            it.esSintetico
                        }

                    DiaRelacionPasos(

                        fechaIso = fecha,

                        pasos = registroPasos.pasos,

                        animoPromedio =
                            promedioAnimo,

                        energiaPromedio =
                            promedioEnergia,

                        cantidadRegistros =
                            emocionesDelDia.size,

                        incluyeDatosSinteticos =
                            tieneSinteticos
                    )
                }
            }
                .sortedBy {

                    it.fechaIso
                }

        // ====================================
        // ESTADISTICAS DE PASOS
        // ====================================
        //
        // Se calculan con los dias que
        // realmente tienen pasos.
        //
        // No se consideran las fechas
        // que carecen de informacion.

        val cantidadesPasos =
            pasosPorFecha.values.map {

                it.pasos
            }

        val totalPasos =
            cantidadesPasos.sum()

        val promedioPasos =
            if (cantidadesPasos.isNotEmpty()) {

                cantidadesPasos.average()

            } else {

                null
            }

        val maximoPasos =
            cantidadesPasos.maxOrNull()

        // ====================================
        // CALCULAR CORRELACIONES
        // ====================================
        //
        // Pearson mide si dos variables
        // cambian juntas.
        //
        // Sus resultados van de -1 a 1.
        //
        // Cerca de +1:
        // Relacion positiva.
        //
        // Cerca de -1:
        // Relacion negativa.
        //
        // Cerca de 0:
        // No se observa una relacion
        // lineal clara.
        //
        // IMPORTANTE:
        //
        // Correlacion NO significa
        // causa y efecto.
        //
        // Pedimos al menos 7 dias
        // comparables para calcularla.
        //
        // Esto no garantiza un resultado
        // estadisticamente significativo.

        val correlacionAnimo =
            calcularCorrelacion(

                dias = diasComparables,

                obtenerValor = {

                    it.animoPromedio
                }
            )

        val correlacionEnergia =
            calcularCorrelacion(

                dias = diasComparables,

                obtenerValor = {

                    it.energiaPromedio
                }
            )

        // ====================================
        // IDENTIFICAR DATOS SINTETICOS
        // ====================================
        //
        // No los separamos del analisis.
        //
        // Pero avisamos si participaron,
        // porque los datos de prueba
        // no demuestran patrones reales.

        val incluyeSinteticos =
            diasComparables.any {

                it.incluyeDatosSinteticos
            }

        // ====================================
        // MENSAJE EXPLICATIVO
        // ====================================

        val mensaje = when {

            pasosPorFecha.isEmpty() -> {

                "Todavía no hay pasos guardados " +
                        "en este periodo. " +
                        "Sincroniza Health Connect."
            }

            emocionesPorFecha.isEmpty() -> {

                "Hay pasos registrados, pero " +
                        "no existen registros " +
                        "emocionales en este periodo."
            }

            diasComparables.size < 7 -> {

                "Hay ${diasComparables.size} " +
                        "día(s) con pasos y emociones. " +
                        "Necesitamos al menos 7 días " +
                        "comparables para calcular " +
                        "una correlación."
            }

            correlacionAnimo == null &&
                    correlacionEnergia == null -> {

                "No hay suficiente variación " +
                        "en los datos para calcular " +
                        "las correlaciones."
            }

            incluyeSinteticos -> {

                "Se calcularon relaciones " +
                        "estadísticas, pero algunos " +
                        "registros emocionales son " +
                        "datos de prueba. " +
                        "No deben interpretarse " +
                        "como conclusiones personales."
            }

            else -> {

                "Se encontraron suficientes " +
                        "datos para explorar las " +
                        "relaciones entre pasos, " +
                        "ánimo y energía. " +
                        "Recuerda que correlación " +
                        "no significa causalidad."
            }
        }

        // ====================================
        // DEVOLVER RESULTADOS
        // ====================================

        return ResultadoRelacionPasos(

            periodoDias = periodoDias,

            diasConPasos =
                pasosPorFecha.size,

            diasConEmociones =
                emocionesPorFecha.size,

            diasComparables =
                diasComparables.size,

            totalPasos =
                totalPasos,

            promedioPasos =
                promedioPasos,

            maximoPasos =
                maximoPasos,

            correlacionPasosAnimo =
                correlacionAnimo,

            correlacionPasosEnergia =
                correlacionEnergia,

            incluyeDatosSinteticos =
                incluyeSinteticos,

            mensaje = mensaje,

            dias = diasComparables
        )
    }

    // ========================================
    // CORRELACION DE PEARSON
    // ========================================
    //
    // Recibe los dias comparables
    // y selecciona el valor emocional.
    //
    // Para animo:
    // obtenerValor = animoPromedio.
    //
    // Para energia:
    // obtenerValor = energiaPromedio.
    //
    // Si hay menos de 7 fechas o alguna
    // variable no cambia, devuelve null.
    //
    // Esta funcion es privada porque
    // solo la utiliza este motor.

    private fun calcularCorrelacion(

        dias: List<DiaRelacionPasos>,

        obtenerValor: (DiaRelacionPasos) -> Double

    ): Double? {

        // ====================================
        // MINIMO DE DIAS
        // ====================================

        if (dias.size < 7) {

            return null
        }

        // ====================================
        // PREPARAR VARIABLES
        // ====================================

        val valoresPasos = dias.map {

            it.pasos.toDouble()
        }

        val valoresEmocionales = dias.map {

            obtenerValor(it)
        }

        // ====================================
        // CALCULAR PROMEDIOS
        // ====================================

        val promedioX =
            valoresPasos.average()

        val promedioY =
            valoresEmocionales.average()

        // ====================================
        // ACUMULADORES
        // ====================================

        var sumaProductos = 0.0

        var sumaCuadradosX = 0.0

        var sumaCuadradosY = 0.0

        // ====================================
        // RECORRER LOS DATOS
        // ====================================

        for (indice in dias.indices) {

            val diferenciaX =
                valoresPasos[indice] -
                        promedioX

            val diferenciaY =
                valoresEmocionales[indice] -
                        promedioY

            sumaProductos +=
                diferenciaX * diferenciaY

            sumaCuadradosX +=
                diferenciaX * diferenciaX

            sumaCuadradosY +=
                diferenciaY * diferenciaY
        }

        // ====================================
        // VALIDAR VARIACION
        // ====================================
        //
        // Si todos los pasos son iguales,
        // no se puede calcular Pearson.
        //
        // Lo mismo sucede si todos
        // los valores emocionales
        // son iguales.

        if (
            sumaCuadradosX <= 0.0 ||
            sumaCuadradosY <= 0.0
        ) {

            return null
        }

        // ====================================
        // FORMULA DE PEARSON
        // ====================================

        val denominador = sqrt(

            sumaCuadradosX *
                    sumaCuadradosY
        )

        if (denominador <= 0.0) {

            return null
        }

        val correlacion =
            sumaProductos / denominador

        // ====================================
        // RESULTADO
        // ====================================
        //
        // Por seguridad numerica,
        // limitamos el resultado
        // al intervalo [-1, 1].

        return correlacion.coerceIn(
            -1.0,
            1.0
        )
    }
}
