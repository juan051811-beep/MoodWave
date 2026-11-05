
package com.example.pp1jd.analytics

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ============================================
// PUNTO DE LA GRAFICA DE TENDENCIAS
// ============================================
// Representa un dia del calendario.
//
// Si no existen registros en ese dia,
// promedioAnimo sera null, no cero.

data class PuntoTendencia(

    // Fecha en formato yyyy-MM-dd
    val fechaIso: String,

    // Promedio del animo de ese dia
    val promedioAnimo: Double?,

    // Promedio movil de 7 dias completos
    val mediaMovil7: Double?,

    // Numero de registros de ese dia
    val cantidadRegistros: Int
)

// ============================================
// RESULTADO GENERAL DE TENDENCIAS
// ============================================

data class ResultadoTendencia(

    // Periodo solicitado: 7, 30 o 90 dias
    val diasSolicitados: Int,

    // Cantidad total de registros encontrados
    val totalRegistros: Int,

    // Dias diferentes con informacion
    val diasConDatos: Int,

    // Puntos que dibujara la grafica
    val puntos: List<PuntoTendencia>
)

// ============================================
// MOTOR MATEMATICO DE TENDENCIAS
// ============================================
// No depende de Android ni de Compose.
//
// Puede utilizar datos reales o sinteticos.
//
// Calcula:
// 1. Promedios diarios.
// 2. Tendencias de 7, 30 y 90 dias.
// 3. Media movil de 7 dias.

object MotorTendencias {

    private const val VENTANA_MEDIA_MOVIL = 7

    // ========================================
    // CALCULAR TENDENCIAS
    // ========================================

    fun calcular(
        datos: List<DatoEmocional>,
        dias: Int = 30,
        referencia: Date = Date()
    ): ResultadoTendencia {

        // Solo permitimos los tres periodos
        // solicitados en el proyecto.

        require(dias == 7 || dias == 30 || dias == 90) {
            "El periodo debe ser de 7, 30 o 90 dias."
        }

        // ====================================
        // PREPARAR LAS FECHAS
        // ====================================

        val calendario = Calendar.getInstance().apply {

            time = referencia

            // Trabajamos al mediodia para evitar
            // problemas con cambios de horario.
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Retrocedemos hasta el primer dia
            // que debe aparecer en la grafica.
            add(Calendar.DAY_OF_YEAR, -(dias - 1))
        }

        val formato = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

        // Generamos todas las fechas del rango.
        // Incluye los dias sin registros.

        val fechas = List(dias) {

            val fecha = formato.format(
                calendario.time
            )

            calendario.add(
                Calendar.DAY_OF_YEAR,
                1
            )

            fecha
        }

        // ====================================
        // AGRUPAR REGISTROS POR FECHA
        // ====================================

        // Utilizamos solamente valores de
        // animo validos entre 1 y 5.

        val registrosPorFecha = datos
            .filter { it.animo in 1..5 }
            .groupBy { it.fechaIso }

        // ====================================
        // CALCULAR PROMEDIO DE CADA DIA
        // ====================================

        val puntosIniciales = fechas.map { fecha ->

            val registrosDia =
                registrosPorFecha[fecha].orEmpty()

            // Varios registros en un mismo dia
            // se convierten en un promedio diario.

            val promedioDia =
                if (registrosDia.isNotEmpty()) {

                    registrosDia
                        .map { it.animo }
                        .average()

                } else {

                    // No inventamos una emocion
                    // cuando no existe registro.
                    null
                }

            PuntoTendencia(
                fechaIso = fecha,
                promedioAnimo = promedioDia,
                mediaMovil7 = null,
                cantidadRegistros = registrosDia.size
            )
        }

        // ====================================
        // CALCULAR MEDIA MOVIL DE 7 DIAS
        // ====================================
        //
        // Cada punto utiliza los ultimos
        // 7 dias del calendario, incluido
        // el propio dia.
        //
        // Para evitar conclusiones engañosas,
        // exigimos datos en los siete dias.
        //
        // Si falta alguno, la media movil
        // de esa ventana queda como null.

        val puntosFinales =
            puntosIniciales.mapIndexed { indice, punto ->

                val mediaMovil =
                    if (indice >= VENTANA_MEDIA_MOVIL - 1) {

                        val ultimosSieteDias =
                            puntosIniciales
                                .subList(
                                    indice - VENTANA_MEDIA_MOVIL + 1,
                                    indice + 1
                                )
                                .mapNotNull {
                                    it.promedioAnimo
                                }

                        if (
                            ultimosSieteDias.size ==
                            VENTANA_MEDIA_MOVIL
                        ) {

                            ultimosSieteDias.average()

                        } else {

                            null
                        }

                    } else {

                        null
                    }

                punto.copy(
                    mediaMovil7 = mediaMovil
                )
            }

        // ====================================
        // CONTAR LOS DATOS UTILIZADOS
        // ====================================

        val totalRegistros = puntosFinales.sumOf {
            it.cantidadRegistros
        }

        val diasConDatos = puntosFinales.count {
            it.cantidadRegistros > 0
        }

        // ====================================
        // DEVOLVER EL RESULTADO
        // ====================================

        return ResultadoTendencia(
            diasSolicitados = dias,
            totalRegistros = totalRegistros,
            diasConDatos = diasConDatos,
            puntos = puntosFinales
        )
    }
}
