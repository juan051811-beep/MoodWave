package com.example.pp1jd.data.local

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Segunda etapa de datos de demostracion de MoodWave.
 *
 * Genera exactamente 210 registros adicionales, repartidos entre
 * 105 y 364 dias atras. Los primeros 90 registros no se modifican.
 *
 * Este objeto NO escribe en Room. La insercion segura e idempotente
 * se hara desde DiaryViewModel en el siguiente paso.
 */
object GeneradorDatosRealistasV2 {

    const val TOTAL_NUEVOS = 210

    // Estas son las 8 actividades reales del formulario NewEntryScreen.
    val ACTIVIDADES = listOf(
        "Ejercicio", "Trabajo", "Social", "Familia",
        "Descanso", "Lectura", "Meditación", "Naturaleza"
    )

    // Distribucion objetivo de 300 registros sinteticos en total.
    // El indice 0 no se usa: los estados emocionales van del 1 al 5.
    private val objetivoAnimo = intArrayOf(0, 35, 50, 80, 85, 50)

    fun generar(
        registrosExistentes: List<RegistroEntity>,
        fechaBase: LocalDate = LocalDate.now()
    ): List<RegistroSinteticoRealista> {
        val random = Random(20261104)

        // Solo usamos los registros sinteticos de la primera etapa
        // para calcular las cuotas. Esto permite reintentar V2 sin
        // alterar el resultado, aunque alguna insercion haya ocurrido.
        val inicioPrimeraEtapa = fechaBase.minusDays(104)
        val conteosIniciales = registrosExistentes
            .filter { it.esSintetico }
            .filter { registro ->
                val fecha = runCatching {
                    LocalDate.parse(registro.fecha)
                }.getOrNull()
                fecha != null &&
                        !fecha.isBefore(inicioPrimeraEtapa) &&
                        !fecha.isAfter(fechaBase)
            }
            .groupingBy { it.valorAnimo }
            .eachCount()

        // Adaptamos las cuotas a los 90 registros existentes.
        // Si las cuotas iniciales no suman 210, normalizamos sin
        // perder la cobertura de ninguno de los cinco estados.
        val pendientes = IntArray(6) { animo ->
            if (animo == 0) 0
            else (objetivoAnimo[animo] - (conteosIniciales[animo] ?: 0))
                .coerceAtLeast(1)
        }
        while (pendientes.sum() > TOTAL_NUEVOS) {
            val indice = (1..5).maxBy { pendientes[it] }
            pendientes[indice]--
        }
        while (pendientes.sum() < TOTAL_NUEVOS) {
            val indice = (1..5).maxBy {
                objetivoAnimo[it].toDouble() / (pendientes[it] + 1)
            }
            pendientes[indice]++
        }

        // 260 fechas candidatas y 210 fechas elegidas: hay dias
        // sin registro, como sucede en un diario real.
        val fechas = (105L..364L)
            .map { fechaBase.minusDays(it) }
            .shuffled(random)
            .take(TOTAL_NUEVOS)
            .sorted()

        val resultados = fechas.mapIndexed { indice, fecha ->
            val diasAtras = ChronoUnit.DAYS.between(fecha, fechaBase).toInt()
            val finDeSemana = fecha.dayOfWeek == DayOfWeek.SATURDAY ||
                    fecha.dayOfWeek == DayOfWeek.SUNDAY

            // Periodos positivos, neutros y de dificultad para
            // que tendencias y deteccion de cambios tengan variedad.
            val animoEsperado = when (diasAtras) {
                in 105..124 -> 3.8
                in 125..141 -> 2.1
                in 142..165 -> 3.5
                in 166..184 -> 4.2
                in 185..207 -> 2.3
                in 208..236 -> 3.1
                in 237..259 -> 4.2
                in 260..282 -> 2.6
                in 283..314 -> 3.5
                in 315..340 -> 4.0
                else -> 2.9
            } + if (finDeSemana) 0.2 else 0.0

            val animo = elegirAnimo(animoEsperado, pendientes, random)
            pendientes[animo]--

            val (nombre, emoji) = when (animo) {
                1 -> "Muy mal" to "😭"
                2 -> "Mal" to "😕"
                3 -> "Normal" to "😐"
                4 -> "Bien" to "😊"
                else -> "Muy bien" to "😁"
            }

            val horasSueno = (
                    7.0 + (animo - 3) * 0.25 +
                            (if (finDeSemana) 0.5 else 0.0) +
                            random.nextDouble(-2.0, 1.7)
                    ).coerceIn(4.0, 9.5).let {
                    (it * 2).roundToInt() / 2.0
                }

            val energia = (
                    5.7 + (animo - 3) * 1.25 +
                            (horasSueno - 7.0) * 0.75 +
                            random.nextDouble(-2.2, 2.2)
                    ).roundToInt().coerceIn(1, 10)

            // La primera actividad rota en las 8 categorias.
            // Las demas crean combinaciones realistas.
            val primeraActividad = ACTIVIDADES[indice % ACTIVIDADES.size]
            val otras = ACTIVIDADES
                .filter { it != primeraActividad }
                .shuffled(random)
                .take(random.nextInt(0, 3))
            val actividades = listOf(primeraActividad) + otras

            // Horario plausible de escritura: tarde/noche,
            // a veces por la mañana o mediodia.
            val hora = if (random.nextDouble() < 0.75) {
                random.nextInt(17, 24)
            } else {
                random.nextInt(8, 17)
            }
            val minuto = random.nextInt(0, 60)
            val creadoEn = fecha.atTime(hora, minuto)
                .atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()

            RegistroSinteticoRealista(
                fecha = fecha.toString(),
                creadoEn = creadoEn,
                animo = nombre,
                valorAnimo = animo,
                emojiAnimo = emoji,
                energia = energia,
                actividades = actividades,
                reflexion = crearReflexion(animo, actividades, random),
                horasSueno = horasSueno,
                pasos = null,                // Nunca inventamos pasos.
                esSintetico = true
            )
        }.toMutableList()

        // Garantizamos tambien la representacion de los 10 niveles
        // de energia. Solo ajustamos registros con valores repetidos,
        // buscando el caso emocionalmente mas coherente.
        val cantidadesEnergia = IntArray(11)
        resultados.forEach { cantidadesEnergia[it.energia]++ }
        for (nivel in 1..10) {
            if (cantidadesEnergia[nivel] != 0) continue

            val candidato = resultados.indices
                .filter { cantidadesEnergia[resultados[it].energia] > 1 }
                .minByOrNull { indice ->
                    val dato = resultados[indice]
                    val energiaEsperada = 1 + (dato.valorAnimo - 1) * 2
                    abs(dato.energia - nivel) +
                            abs(energiaEsperada - nivel) * 0.5
                } ?: error("No hay un registro para completar la energia")

            cantidadesEnergia[resultados[candidato].energia]--
            resultados[candidato] = resultados[candidato].copy(energia = nivel)
            cantidadesEnergia[nivel]++
        }

        check(resultados.size == TOTAL_NUEVOS)
        check(resultados.map { it.fecha }.distinct().size == TOTAL_NUEVOS)
        check((1..5).all { nivel -> resultados.any { it.valorAnimo == nivel } })
        check((1..10).all { nivel -> resultados.any { it.energia == nivel } })
        check(ACTIVIDADES.all { actividad ->
            resultados.any { actividad in it.actividades }
        })

        return resultados
    }

    // Seleccion ponderada: la emocion se acerca a la tendencia del
    // periodo, pero respeta las cantidades que todavia faltan.
    private fun elegirAnimo(
        esperado: Double,
        pendientes: IntArray,
        random: Random
    ): Int {
        val pesos = (1..5).map { nivel ->
            val distancia = abs(nivel - esperado)
            pendientes[nivel] * (0.15 + 1.0 / (1.0 + distancia.pow(2.0)))
        }
        val total = pesos.sum()
        require(total > 0.0)
        val objetivo = random.nextDouble(total)
        var acumulado = 0.0
        for (i in pesos.indices) {
            acumulado += pesos[i]
            if (objetivo < acumulado) return i + 1
        }
        return (1..5).last { pendientes[it] > 0 }
    }

    private fun crearReflexion(
        animo: Int,
        actividades: List<String>,
        random: Random
    ): String {
        val comienzos = when (animo) {
            1 -> listOf(
                "Me costó mucho concentrarme hoy.",
                "Fue un día agotador y terminé bastante desanimado.",
                "Sentí que las preocupaciones se fueron acumulando.",
                "Hoy tuve poca motivación y muchas cosas en la cabeza.",
                "No me sentí bien y preferí bajar el ritmo."
            )
            2 -> listOf(
                "Estuve algo cansado y con poco ánimo.",
                "El día tuvo varios momentos de estrés.",
                "Me costó avanzar con algunos pendientes.",
                "Me sentí inquieto, aunque hubo ratos tranquilos.",
                "No todo salió como lo esperaba."
            )
            3 -> listOf(
                "Fue un día bastante normal.",
                "Mi ánimo estuvo estable durante casi todo el día.",
                "Tuve una jornada tranquila, sin grandes sorpresas.",
                "Hubo momentos productivos y otros de cansancio.",
                "Hoy seguí mi rutina sin demasiadas complicaciones."
            )
            4 -> listOf(
                "Hoy tuve un buen día y avancé bastante.",
                "Me sentí con buen ánimo durante la mayor parte del día.",
                "Las cosas salieron mejor de lo que esperaba.",
                "Pude disfrutar de varios momentos agradables.",
                "Terminé satisfecho con lo que hice hoy."
            )
            else -> listOf(
                "Me sentí excelente desde temprano.",
                "Hoy fue uno de esos días que quisiera repetir.",
                "Disfruté mucho lo que hice y tuve bastante motivación.",
                "Terminé el día contento y con mucha energía.",
                "Tuve varios momentos que me hicieron sonreír."
            )
        }

        val detalle = when (actividades.first()) {
            "Ejercicio" -> "Hice ejercicio y noté cómo respondió mi cuerpo."
            "Trabajo" -> "Dediqué varias horas al trabajo y a mis pendientes."
            "Social" -> "Compartí un rato con otras personas."
            "Familia" -> "Pasé tiempo con mi familia."
            "Descanso" -> "Busqué un espacio para descansar."
            "Lectura" -> "Pude dedicar un rato a la lectura."
            "Meditación" -> "Me tomé unos minutos para despejar la mente."
            else -> "Salí a disfrutar un poco de la naturaleza."
        }

        val cierres = listOf(
            "Mañana quiero organizarme mejor.",
            "Creo que me vendría bien dormir un poco más.",
            "Me gustaría repetir algunos momentos del día.",
            "Aprendí que necesito tomar pausas.",
            "Intentaré mantener un mejor equilibrio esta semana.",
            "Al final pude dedicarme un momento a mí.",
            "Espero tener un poco más de tiempo libre mañana.",
            "Me quedé pensando en lo que podría mejorar."
        )

        val inicio = comienzos.random(random)
        return if (random.nextBoolean()) {
            "$inicio $detalle"
        } else {
            "$inicio $detalle ${cierres.random(random)}"
        }
    }
}
