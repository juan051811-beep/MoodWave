
package com.example.pp1jd.data.local

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

import kotlin.math.roundToInt
import kotlin.random.Random

// ============================================
// MODELO DE REGISTRO SINTETICO
// ============================================
//
// Representa un registro emocional
// generado para nuestras pruebas.
//
// Todavia no se inserta en Room.
// Primero generamos los datos y luego
// los conectaremos con el repositorio.
//
// Los pasos siempre quedan en null
// porque Health Connect es la fuente
// de los datos reales de actividad fisica.

data class RegistroSinteticoRealista(

    val fecha: String,

    val creadoEn: Long,

    val animo: String,

    val valorAnimo: Int,

    val emojiAnimo: String,

    val energia: Int,

    val actividades: List<String>,

    val reflexion: String,

    val horasSueno: Double?,

    val pasos: Long? = null,

    val esSintetico: Boolean = true
)

// ============================================
// GENERADOR DE DATOS REALISTAS
// ============================================
//
// Genera:
// - 90 registros emocionales.
// - Un periodo de 105 dias.
// - 83 fechas distintas.
// - 7 fechas con dos registros.
// - Dias sin registros.
// - Distintos niveles de animo.
// - Diferentes actividades.
// - Sueno y energia variables.
// - Reflexiones variadas.
//
// Utiliza una semilla fija para
// reproducir los mismos patrones
// durante las pruebas.
//
// Las fechas se calculan respecto
// al dia en que se ejecuta.
//
// NO inserta datos automaticamente.
// NO modifica la base de datos.

object GeneradorDatosRealistas {

    private const val SEMILLA = 20261104

    private const val TOTAL_REGISTROS = 90

    // ========================================
    // GENERAR LOS 90 REGISTROS
    // ========================================

    fun generar(
        hoy: LocalDate = LocalDate.now()
    ): List<RegistroSinteticoRealista> {

        val random = Random(SEMILLA)

        // ====================================
        // SELECCIONAR FECHAS
        // ====================================
        //
        // Trabajamos con los ultimos
        // 105 dias del calendario.
        //
        // No todas las fechas tendran
        // registros, porque buscamos
        // un comportamiento natural.
        //
        // Garantizamos registros en
        // los ultimos 7 dias para
        // evaluar cambios emocionales.

        val ultimosSieteDias =
            (0..6).map {

                hoy.minusDays(
                    it.toLong()
                )
            }

        val fechasAnteriores =
            (7..104).map {

                hoy.minusDays(
                    it.toLong()
                )
            }

        // Elegimos 76 fechas anteriores.

        val fechasElegidas =
            ultimosSieteDias +
                    fechasAnteriores
                        .shuffled(random)
                        .take(76)

        // Tenemos 83 fechas.
        //
        // Elegimos 7 de ellas para
        // registrar dos emociones
        // en el mismo dia.

        val fechasDobles =
            fechasElegidas
                .shuffled(random)
                .take(7)

        // Total:
        //
        // 83 registros normales
        // + 7 registros adicionales
        // = 90 registros.

        val fechasDeRegistros =
            (fechasElegidas + fechasDobles)
                .sorted()

        val registros =
            mutableListOf<RegistroSinteticoRealista>()

        // Controlamos cuantas entradas
        // hemos generado por fecha.

        val entradasPorFecha =
            mutableMapOf<LocalDate, Int>()

        // ====================================
        // GENERAR INFORMACION DE CADA DIA
        // ====================================

        fechasDeRegistros.forEach { fecha ->

            val numeroEntrada =
                entradasPorFecha[fecha] ?: 0

            entradasPorFecha[fecha] =
                numeroEntrada + 1

            // =================================
            // IDENTIFICAR ANTIGUEDAD
            // =================================

            val diasAtras =
                ChronoUnit.DAYS.between(
                    fecha,
                    hoy
                ).toInt()

            // =================================
            // FIN DE SEMANA
            // =================================

            val finDeSemana =
                fecha.dayOfWeek ==
                        DayOfWeek.SATURDAY ||
                        fecha.dayOfWeek ==
                        DayOfWeek.SUNDAY

            // =================================
            // ESTADO EMOCIONAL
            // =================================

            val valorAnimo =
                generarAnimo(
                    diasAtras,
                    finDeSemana,
                    random
                )

            val (animo, emoji) =
                obtenerEmocion(valorAnimo)

            // =================================
            // HORAS DE SUENO
            // =================================

            val horasSueno =
                generarSueno(
                    valorAnimo,
                    finDeSemana,
                    random
                )

            // =================================
            // NIVEL DE ENERGIA
            // =================================

            val energia =
                generarEnergia(
                    valorAnimo,
                    horasSueno,
                    random
                )

            // =================================
            // ACTIVIDADES
            // =================================

            val actividades =
                generarActividades(
                    finDeSemana,
                    random
                )

            // =================================
            // REFLEXION PERSONAL
            // =================================

            val reflexion =
                generarReflexion(
                    valorAnimo,
                    actividades,
                    random
                )

            // =================================
            // HORA DEL REGISTRO
            // =================================
            //
            // Normalmente se registra
            // durante la tarde o noche.
            //
            // En los dias con dos
            // entradas, la segunda
            // corresponde a otro momento.

            val hora =
                if (numeroEntrada == 0) {

                    random.nextInt(17, 24)

                } else {

                    random.nextInt(9, 16)
                }

            val minuto =
                random.nextInt(0, 60)

            val creadoEn =
                fecha
                    .atTime(hora, minuto)
                    .atZone(
                        ZoneId.systemDefault()
                    )
                    .toInstant()
                    .toEpochMilli()

            // =================================
            // CREAR REGISTRO
            // =================================

            registros.add(

                RegistroSinteticoRealista(

                    fecha = fecha.toString(),

                    creadoEn = creadoEn,

                    animo = animo,

                    valorAnimo = valorAnimo,

                    emojiAnimo = emoji,

                    energia = energia,

                    actividades = actividades,

                    reflexion = reflexion,

                    horasSueno = horasSueno,

                    // Nunca inventamos pasos.
                    pasos = null,

                    esSintetico = true
                )
            )
        }

        return registros
            .sortedWith(
                compareBy(
                    { it.fecha },
                    { it.creadoEn }
                )
            )
            .also {

                check(
                    it.size == TOTAL_REGISTROS
                ) {

                    "No se generaron los 90 registros."
                }
            }
    }

    // ========================================
    // GENERAR ESTADO EMOCIONAL
    // ========================================
    //
    // Diseñamos varios periodos:
    //
    // - Dias normales.
    // - Periodo positivo.
    // - Etapa de estres.
    // - Recuperacion.
    // - Dias mixtos.
    // - Descenso emocional reciente.
    //
    // Agregamos variaciones aleatorias
    // para que los cambios no sean
    // perfectamente repetitivos.
    //
    // Esto permite probar K-means
    // y el detector de anomalias.

    private fun generarAnimo(
        diasAtras: Int,
        finDeSemana: Boolean,
        random: Random
    ): Int {

        val base = when {

            diasAtras <= 6 -> 2.1

            diasAtras <= 12 -> 3.2

            diasAtras <= 29 -> 3.7

            diasAtras <= 48 -> 3.3

            diasAtras <= 64 -> 2.7

            diasAtras <= 85 -> 3.8

            else -> 3.2
        }

        // Ligera mejora posible
        // durante los fines de semana.
        //
        // No todos los fines de semana
        // deben ser necesariamente buenos.

        val ajusteFinDeSemana =
            if (finDeSemana) 0.25 else 0.0

        // Variacion aleatoria.

        val variacion =
            (random.nextDouble() - 0.5) * 2.1

        val valor =
            (
                    base +
                            ajusteFinDeSemana +
                            variacion
                    ).roundToInt()

        return valor.coerceIn(1, 5)
    }

    // ========================================
    // OBTENER NOMBRE Y EMOJI
    // ========================================

    private fun obtenerEmocion(
        valor: Int
    ): Pair<String, String> {

        return when (valor) {

            1 -> "Muy mal" to "😭"

            2 -> "Mal" to "😕"

            3 -> "Normal" to "😐"

            4 -> "Bien" to "😊"

            5 -> "Muy bien" to "😁"

            else -> "Normal" to "😐"
        }
    }

    // ========================================
    // GENERAR HORAS DE SUENO
    // ========================================
    //
    // El sueno cambia entre dias.
    //
    // Puede tener cierta relacion
    // con el animo, pero sin hacer
    // que ambos valores sean identicos.

    private fun generarSueno(
        animo: Int,
        finDeSemana: Boolean,
        random: Random
    ): Double {

        val base = 6.7

        val ajusteAnimo =
            (animo - 3) * 0.25

        val ajusteFinDeSemana =
            if (finDeSemana) 0.6 else 0.0

        val variacion =
            (random.nextDouble() - 0.5) * 3.0

        val horas =
            (
                    base +
                            ajusteAnimo +
                            ajusteFinDeSemana +
                            variacion
                    ).coerceIn(4.0, 9.5)

        // Redondeamos a medias horas:
        //
        // 6.0, 6.5, 7.0, 7.5...

        return (
                horas * 2
                ).roundToInt() / 2.0
    }

    // ========================================
    // GENERAR ENERGIA
    // ========================================
    //
    // La energia tiene una relacion
    // parcial con el estado de animo
    // y las horas de sueno.
    //
    // Incluimos variacion adicional
    // para simular dias donde alguien
    // se siente bien, pero cansado,
    // o animado pese a dormir poco.

    private fun generarEnergia(
        animo: Int,
        horasSueno: Double,
        random: Random
    ): Int {

        val base = 5.8

        val ajusteAnimo =
            (animo - 3) * 0.8

        val ajusteSueno =
            (horasSueno - 7.0) * 0.65

        val variacion =
            (random.nextDouble() - 0.5) * 4.0

        return (
                base +
                        ajusteAnimo +
                        ajusteSueno +
                        variacion
                ).roundToInt().coerceIn(1, 10)
    }

    // ========================================
    // GENERAR ACTIVIDADES VARIADAS
    // ========================================
    //
    // Mezclamos actividades de estudio,
    // trabajo, convivencia, ejercicio,
    // descanso y entretenimiento.
    //
    // Elegimos entre una y tres
    // actividades por registro.

    private fun generarActividades(
        finDeSemana: Boolean,
        random: Random
    ): List<String> {

        val actividadesSemana = listOf(

            "Estudiar",
            "Trabajar",
            "Caminar",
            "Hacer ejercicio",
            "Escuchar música",
            "Leer",
            "Descansar",
            "Tiempo en familia",
            "Ver series",
            "Hacer tareas",
            "Jugar videojuegos",
            "Salir con amigos"
        )

        val actividadesFinDeSemana = listOf(

            "Salir con amigos",
            "Tiempo en familia",
            "Caminar",
            "Descansar",
            "Ver películas",
            "Escuchar música",
            "Jugar videojuegos",
            "Hacer ejercicio",
            "Leer",
            "Salir a comer",
            "Hacer tareas",
            "Ordenar la casa"
        )

        val opciones =
            if (finDeSemana) {

                actividadesFinDeSemana

            } else {

                actividadesSemana
            }

        val cantidad =
            random.nextInt(1, 4)

        return opciones
            .shuffled(random)
            .take(cantidad)
            .distinct()
    }

    // ========================================
    // GENERAR REFLEXIONES NATURALES
    // ========================================
    //
    // Cada nivel emocional tiene
    // distintas frases de inicio.
    //
    // Agregamos detalles relacionados
    // con las actividades realizadas.
    //
    // Esto produce muchas combinaciones
    // posibles y evita que todas las
    // reflexiones sean iguales.

    private fun generarReflexion(
        animo: Int,
        actividades: List<String>,
        random: Random
    ): String {

        val frases = when (animo) {

            1 -> listOf(

                "Hoy fue un día bastante pesado y me costó concentrarme.",

                "Me sentí agotado desde temprano y tuve poca motivación.",

                "Las cosas no salieron como esperaba y terminé frustrado.",

                "Fue uno de esos días en los que todo parece acumularse.",

                "No tuve muchos ánimos y preferí tomar las cosas con calma.",

                "Me sentí abrumado por varios pendientes.",

                "Hoy me costó encontrar algo que me animara.",

                "Tuve un día complicado y terminé con muy poca energía."
            )

            2 -> listOf(

                "Estuve algo desanimado, aunque logré terminar algunas cosas.",

                "Me sentí cansado y me costó mantener el ritmo.",

                "Tuve varios momentos de estrés durante el día.",

                "No fue mi mejor día, pero hubo algunos momentos tranquilos.",

                "Sentí que tenía demasiadas cosas por hacer.",

                "Hoy estuve un poco distraído y sin tanta motivación.",

                "Me hizo falta descansar y organizar mejor mi tiempo.",

                "Me sentí algo preocupado por mis pendientes."
            )

            3 -> listOf(

                "Fue un día normal, con momentos buenos y otros algo pesados.",

                "No pasó nada especialmente emocionante, pero todo estuvo bien.",

                "Tuve una rutina bastante tranquila durante el día.",

                "Me sentí estable, aunque un poco cansado por la tarde.",

                "Hoy avancé a mi ritmo sin demasiadas complicaciones.",

                "Fue un día sencillo y pude hacer varias cosas.",

                "Mi ánimo estuvo bastante equilibrado.",

                "Tuve algunos pendientes, pero el día fue llevadero."
            )

            4 -> listOf(

                "Me sentí motivado y logré avanzar en mis actividades.",

                "Hoy tuve un buen día y terminé bastante satisfecho.",

                "Me gustó cómo aproveché mi tiempo.",

                "Aunque hubo algunas dificultades, me sentí bien.",

                "Tuve suficiente energía para hacer lo que había planeado.",

                "Hoy disfruté de varios momentos agradables.",

                "Me sentí tranquilo y de buen humor casi todo el día.",

                "Las cosas salieron mejor de lo que esperaba."
            )

            else -> listOf(

                "Hoy me sentí excelente y disfruté mucho el día.",

                "Tuve muy buen ánimo y me sentí lleno de energía.",

                "Fue un día especialmente agradable.",

                "Me sentí muy motivado y todo fluyó bastante bien.",

                "Disfruté mucho el tiempo que compartí con otras personas.",

                "Hoy me sentí satisfecho con lo que logré.",

                "Pasaron varias cosas buenas que mejoraron mi ánimo.",

                "Terminé el día muy contento y con ganas de seguir avanzando."
            )
        }

        val inicio =
            frases.random(random)

        // ====================================
        // DETALLES SEGUN LAS ACTIVIDADES
        // ====================================

        val detalles =
            actividades.map { actividad ->

                when (actividad) {

                    "Estudiar",
                    "Hacer tareas" ->

                        "Dediqué parte del tiempo a estudiar y resolver pendientes."

                    "Trabajar" ->

                        "El trabajo me mantuvo ocupado durante varias horas."

                    "Caminar" ->

                        "Salí a caminar un rato para cambiar de ambiente."

                    "Hacer ejercicio" ->

                        "Hice algo de ejercicio y noté el esfuerzo físico."

                    "Escuchar música" ->

                        "Escuchar música me ayudó a desconectarme un poco."

                    "Leer" ->

                        "Aproveché un momento libre para leer."

                    "Descansar" ->

                        "Busqué un espacio para descansar sin tantas distracciones."

                    "Tiempo en familia" ->

                        "Pasé un rato con mi familia."

                    "Salir con amigos" ->

                        "Conviví con mis amigos y salí un poco de la rutina."

                    "Ver series",
                    "Ver películas" ->

                        "Vi algo para entretenerme y relajarme."

                    "Jugar videojuegos" ->

                        "Jugué un rato para distraerme."

                    "Salir a comer" ->

                        "Salí a comer y disfruté el cambio de ambiente."

                    "Ordenar la casa" ->

                        "Organicé algunas cosas de la casa."

                    else ->

                        "También dediqué tiempo a mis actividades habituales."
                }
            }

        val detalle =
            detalles.random(random)

        // En ocasiones agregamos un
        // cierre que aporta contexto.

        val cierres = listOf(

            "Mañana intentaré organizarme mejor.",

            "Espero mantener un buen ritmo durante los próximos días.",

            "Creo que necesito dormir un poco más.",

            "Me gustaría tener más tiempo libre.",

            "Al final, pude encontrar un momento para mí.",

            "Voy a intentar mantener mis hábitos.",

            "Terminé el día pensando en lo que puedo mejorar.",

            "Creo que fue importante darme tiempo para descansar."
        )

        return if (
            random.nextDouble() < 0.45
        ) {

            "$inicio $detalle"

        } else {

            "$inicio $detalle ${cierres.random(random)}"
        }
    }
}
