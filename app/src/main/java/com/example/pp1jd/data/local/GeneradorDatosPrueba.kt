
package com.example.pp1jd.data.local

import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

// ============================================
// GENERADOR DE CASOS DE MOODWAVE
// ============================================
// Genera hasta 90 dias de registros sinteticos.
//
// Los registros permanecen en Room y tienen
// reflexiones naturales.
//
// Los casos sinteticos se identifican mediante
// esSintetico = true.
//
// No modifica los registros personales.

object GeneradorDatosPrueba {

    private const val MARCA_ANTIGUA =
        "[DATOS DE PRUEBA]"

    // ========================================
    // GENERAR O ACTUALIZAR CASOS
    // ========================================

    suspend fun generar(
        dao: RegistroDao,
        cantidadDias: Int = 90
    ): Int {

        require(cantidadDias in 1..365)

        val formatoFecha = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

        // Consultamos los registros existentes.
        val registrosExistentes =
            dao.obtenerTodosLosRegistros()
                .first()
                .groupBy { it.fecha }

        var insertados = 0

        // Recorremos los ultimos 90 dias.
        for (indice in 0 until cantidadDias) {

            val calendario =
                Calendar.getInstance().apply {

                    add(
                        Calendar.DAY_OF_YEAR,
                        -(cantidadDias - 1 - indice)
                    )
                }

            val fecha = formatoFecha.format(
                calendario.time
            )

            val existentesDelDia =
                registrosExistentes[fecha].orEmpty()

            // =================================
            // ACTUALIZAR CASOS ANTERIORES
            // =================================

            if (existentesDelDia.isNotEmpty()) {

                existentesDelDia.forEach { registro ->

                    val tieneMarcaAntigua =
                        registro.reflexion.startsWith(
                            MARCA_ANTIGUA
                        )

                    // Solo modificamos casos sinteticos.
                    if (
                        registro.esSintetico ||
                        tieneMarcaAntigua
                    ) {

                        // Conservamos reflexiones que ya
                        // hayan sido personalizadas.
                        val nuevaReflexion =
                            if (tieneMarcaAntigua) {

                                crearReflexion(
                                    registro.valorAnimo,
                                    indice
                                )

                            } else {

                                registro.reflexion
                            }

                        if (
                            !registro.esSintetico ||
                            tieneMarcaAntigua
                        ) {

                            dao.actualizarRegistro(
                                registro.copy(
                                    reflexion = nuevaReflexion,
                                    esSintetico = true
                                )
                            )
                        }
                    }
                }

                // No creamos registros duplicados
                // en fechas que ya tienen datos.
                continue
            }

            // =================================
            // CREAR NUEVOS CASOS
            // =================================

            val random = Random(2026 + indice)

            val grupo = indice % 3

            val valorAnimo: Int
            val energia: Int
            val horasSueno: Double
            val actividades: List<String>

            if (indice >= cantidadDias - 7) {

                // Cambio sostenido de 7 dias.
                // Servira para detectar anomalias.

                valorAnimo = 2
                energia = random.nextInt(2, 5)
                horasSueno = 5.0
                actividades = listOf("Trabajo")

            } else {

                when (grupo) {

                    0 -> {
                        valorAnimo = 5
                        energia = random.nextInt(8, 11)
                        horasSueno = 8.0

                        actividades = listOf(
                            "Ejercicio",
                            "Naturaleza"
                        )
                    }

                    1 -> {
                        valorAnimo = 3
                        energia = random.nextInt(5, 8)
                        horasSueno = 6.5

                        actividades = listOf(
                            "Trabajo",
                            "Lectura"
                        )
                    }

                    else -> {
                        valorAnimo = 4
                        energia = random.nextInt(7, 10)
                        horasSueno = 8.5

                        actividades = listOf(
                            "Descanso",
                            "Social"
                        )
                    }
                }
            }

            val animo = when (valorAnimo) {
                1 -> "Muy mal"
                2 -> "Mal"
                3 -> "Normal"
                4 -> "Bien"
                else -> "Muy bien"
            }

            val emoji = when (valorAnimo) {
                1 -> "😭"
                2 -> "😕"
                3 -> "😐"
                4 -> "😊"
                else -> "😁"
            }

            // Reflexion acorde con el animo.
            val reflexion = crearReflexion(
                valorAnimo,
                indice
            )

            // =================================
            // GUARDAR EN ROOM
            // =================================

            val registro = RegistroEntity(
                fecha = fecha,
                creadoEn = calendario.timeInMillis,
                animo = animo,
                valorAnimo = valorAnimo,
                emojiAnimo = emoji,
                energia = energia,
                actividades = actividades,
                reflexion = reflexion,
                horasSueno = horasSueno,
                pasos = null,

                // Identifica su origen sintetico.
                esSintetico = true
            )

            dao.insertarRegistro(registro)

            insertados++
        }

        // Devolvemos solamente los nuevos
        // registros insertados.
        return insertados
    }

    // ========================================
    // CREAR REFLEXIONES NATURALES
    // ========================================

    private fun crearReflexion(
        valorAnimo: Int,
        indice: Int
    ): String {

        val reflexiones = when (valorAnimo) {

            1 -> listOf(
                "Hoy fue un día complicado. Me sentí sin energía y me costó mantener el ritmo.",
                "No tuve un buen día. Preferí tomarme las cosas con calma.",
                "Hoy me sentí bastante desanimado y tuve dificultades para concentrarme."
            )

            2 -> listOf(
                "Hoy tuve bastante trabajo y terminé con poca energía.",
                "Me sentí cansado durante buena parte del día. Algunas actividades me costaron más de lo habitual.",
                "Fue un día pesado. Al terminar mis pendientes sentí que necesitaba descansar."
            )

            3 -> listOf(
                "Hoy fue un día tranquilo, sin grandes cambios en mi estado de ánimo.",
                "Cumplí con mis actividades habituales. Mi energía estuvo en un nivel intermedio.",
                "Me sentí normal durante el día. Tuve algunos momentos de cansancio."
            )

            4 -> listOf(
                "Hoy me sentí bien y pude organizar mis actividades con tranquilidad.",
                "Tuve un día agradable. Disfruté algunos momentos de descanso.",
                "Me sentí con buena energía y terminé satisfecho con mis actividades."
            )

            else -> listOf(
                "Hoy tuve un excelente día. Me sentí con mucha energía y buen ánimo.",
                "Disfruté mis actividades y terminé el día sintiéndome muy bien.",
                "Hoy me sentí motivado y con energía para realizar mis pendientes."
            )
        }

        // Seleccionamos diferentes reflexiones
        // de manera reproducible.
        return reflexiones[indice % reflexiones.size]
    }
}
