
package com.example.pp1jd.analytics

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

// ============================================
// DATOS PARA EL ANALISIS EMOCIONAL
// ============================================
// Este modelo no depende de Android ni Room.
// Asi podemos probar los calculos por separado.

data class DatoEmocional(
    val fechaIso: String,
    val animo: Int,
    val energia: Int
)

// ============================================
// RESULTADO DE LAS ESTADISTICAS
// ============================================

data class ResumenEmocional(
    val numeroRegistros: Int,
    val diasRegistrados: Int,
    val promedio: Double?,
    val mediana: Double?,
    val modas: List<Int>,
    val desviacionEstandar: Double?,
    val energiaPromedio: Double?,
    val rachaMaxima: Int
)

// ============================================
// MOTOR DE ESTADISTICAS DE MOODWAVE
// ============================================

object MotorEstadisticas {

    // ========================================
    // CALCULAR RESUMEN DE REGISTROS
    // ========================================

    fun resumir(
        datos: List<DatoEmocional>
    ): ResumenEmocional {

        // Si no hay datos, no inventamos valores.
        if (datos.isEmpty()) {
            return ResumenEmocional(
                numeroRegistros = 0,
                diasRegistrados = 0,
                promedio = null,
                mediana = null,
                modas = emptyList(),
                desviacionEstandar = null,
                energiaPromedio = null,
                rachaMaxima = 0
            )
        }

        // Obtenemos los valores del animo
        // y los ordenamos de menor a mayor.
        val valores = datos
            .map { it.animo }
            .sorted()

        val n = valores.size

        // ====================================
        // PROMEDIO
        // ====================================

        val promedio = valores.average()

        // ====================================
        // MEDIANA
        // ====================================
        // Si la cantidad es impar, tomamos
        // el valor central.
        // Si es par, promediamos los dos
        // valores centrales.

        val mediana = if (n % 2 == 1) {

            valores[n / 2].toDouble()

        } else {

            (
                    valores[n / 2 - 1] +
                            valores[n / 2]
                    ) / 2.0
        }

        // ====================================
        // MODA
        // ====================================
        // Contamos cuantas veces aparece
        // cada valor emocional.

        val frecuencias = valores
            .groupingBy { it }
            .eachCount()

        val frecuenciaMayor =
            frecuencias.values.maxOrNull() ?: 0

        // Puede haber mas de una moda.
        // Si todos aparecen una vez,
        // consideramos que no hay moda.

        val modas = if (frecuenciaMayor > 1) {

            frecuencias
                .filterValues {
                    it == frecuenciaMayor
                }
                .keys
                .sorted()

        } else {
            emptyList()
        }

        // ====================================
        // DESVIACION ESTANDAR
        // ====================================
        // Utilizamos la desviacion poblacional
        // de los registros del periodo.
        //
        // Mide que tanto varian los valores
        // respecto al promedio.

        val varianza = valores.sumOf {
            (it - promedio).pow(2)
        } / n

        val desviacion = sqrt(varianza)

        // ====================================
        // DIAS DIFERENTES REGISTRADOS
        // ====================================
        // Varios registros de un mismo dia
        // cuentan como un solo dia.

        val dias = datos
            .map { it.fechaIso }
            .distinct()

        // ====================================
        // DEVOLVER LOS RESULTADOS
        // ====================================

        return ResumenEmocional(
            numeroRegistros = n,
            diasRegistrados = dias.size,
            promedio = promedio,
            mediana = mediana,
            modas = modas,
            desviacionEstandar = desviacion,
            energiaPromedio = datos
                .map { it.energia }
                .average(),
            rachaMaxima = calcularRachaMaxima(dias)
        )
    }

    // ========================================
    // ESTADISTICAS DE LA SEMANA ACTUAL
    // ========================================
    // Consideramos lunes a domingo.

    fun resumirSemana(
        datos: List<DatoEmocional>,
        referencia: Date = Date()
    ): ResumenEmocional {

        val lunes = Calendar.getInstance().apply {
            time = referencia
        }

        val diasDesdeLunes = (
                lunes.get(Calendar.DAY_OF_WEEK) -
                        Calendar.MONDAY + 7
                ) % 7

        lunes.add(
            Calendar.DAY_OF_MONTH,
            -diasDesdeLunes
        )

        val inicio = fechaIso(lunes)

        val domingo = lunes.clone() as Calendar

        domingo.add(
            Calendar.DAY_OF_MONTH,
            6
        )

        val fin = fechaIso(domingo)

        // Filtramos los registros de la semana.
        return resumir(
            datos.filter {
                it.fechaIso in inicio..fin
            }
        )
    }

    // ========================================
    // ESTADISTICAS DEL MES ACTUAL
    // ========================================

    fun resumirMes(
        datos: List<DatoEmocional>,
        referencia: Date = Date()
    ): ResumenEmocional {

        val mes = Calendar.getInstance().apply {
            time = referencia
        }

        // Primer dia del mes.
        mes.set(Calendar.DAY_OF_MONTH, 1)

        val inicio = fechaIso(mes)

        // Ultimo dia del mes.
        mes.set(
            Calendar.DAY_OF_MONTH,
            mes.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )
        )

        val fin = fechaIso(mes)

        return resumir(
            datos.filter {
                it.fechaIso in inicio..fin
            }
        )
    }

    // ========================================
    // CALCULAR RACHA MAXIMA
    // ========================================

    private fun calcularRachaMaxima(
        dias: List<String>
    ): Int {

        val ordenados = dias
            .distinct()
            .sorted()

        if (ordenados.isEmpty()) {
            return 0
        }

        var maxima = 1
        var actual = 1

        for (i in 1 until ordenados.size) {

            actual = if (
                diaSiguiente(ordenados[i - 1]) ==
                ordenados[i]
            ) {
                actual + 1
            } else {
                1
            }

            maxima = maxOf(maxima, actual)
        }

        return maxima
    }

    // ========================================
    // OBTENER EL DIA SIGUIENTE
    // ========================================

    private fun diaSiguiente(
        fechaIso: String
    ): String {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            isLenient = false
        }

        val fecha = formato.parse(fechaIso)
            ?: return fechaIso

        val calendario = Calendar.getInstance().apply {
            time = fecha
        }

        calendario.add(
            Calendar.DAY_OF_MONTH,
            1
        )

        return fechaIso(calendario)
    }

    // ========================================
    // CONVERTIR FECHA A FORMATO ISO
    // ========================================

    private fun fechaIso(
        calendario: Calendar
    ): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(calendario.time)
    }
}
