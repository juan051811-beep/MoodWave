
package com.example.pp1jd.analytics

import java.text.SimpleDateFormat
import java.util.Locale

// ============================================
// DATOS DE ENTRADA PARA LA IA
// ============================================

data class RegistroParaIA(
    val fechaIso: String,
    val animo: Int,
    val energia: Int,
    val horasSueno: Double? = null,
    val actividades: List<String> = emptyList()
)

// ============================================
// RESUMEN EMOCIONAL DE UN DIA
// ============================================
// Si existen varios registros en una fecha,
// calculamos un promedio diario.
//
// De esta manera, cada dia tiene el mismo
// peso dentro del agrupamiento.

data class DiaIA(
    val fechaIso: String,
    val promedioAnimo: Double,
    val promedioEnergia: Double,
    val promedioSueno: Double?,
    val actividades: Set<String>,
    val numeroRegistros: Int
)

// ============================================
// RESULTADO DE UN GRUPO
// ============================================

data class GrupoIA(
    val numero: Int,
    val nombre: String,
    val dias: List<DiaIA>,
    val promedioAnimo: Double,
    val promedioEnergia: Double,
    val actividadesFrecuentes: List<String>
)

// ============================================
// RESULTADO DEL AGRUPAMIENTO
// ============================================

data class ResultadoAgrupamientoIA(
    val diasAnalizados: Int,
    val registrosValidos: Int,
    val datosSuficientes: Boolean,
    val grupos: List<GrupoIA>,

    // Cuanto menor sea esta dispersion,
    // mas similares son los dias de cada grupo.
    val dispersionKMeans: Double?,
    val dispersionBase: Double?,

    // Comparamos justamente cuando ambos
    // metodos forman igual numero de grupos.
    val comparacionDirecta: Boolean,

    val mensaje: String
)

// ============================================
// MOTOR DE AGRUPAMIENTO
// ============================================

object MotorAgrupamientoIA {

    const val MINIMO_DIAS = 14

    private const val MAX_ITERACIONES = 100

    // ========================================
    // REPRESENTACION NUMERICA
    // ========================================
    // Normalizamos las variables:
    //
    // Animo 1..5    -> 0..1
    // Energia 1..10 -> 0..1
    //
    // Evitamos que energia tenga mas peso
    // simplemente porque utiliza una
    // escala numerica mas grande.

    private data class Punto(
        val animo: Double,
        val energia: Double
    )

    private fun normalizar(
        dia: DiaIA
    ): Punto {

        return Punto(
            animo = (dia.promedioAnimo - 1.0) / 4.0,
            energia = (dia.promedioEnergia - 1.0) / 9.0
        )
    }

    // ========================================
    // DISTANCIA ENTRE DOS PUNTOS
    // ========================================
    // Distancia euclidiana al cuadrado.
    // No necesitamos calcular raiz cuadrada
    // para comparar cuales puntos estan cerca.

    private fun distanciaCuadrada(
        a: Punto,
        b: Punto
    ): Double {

        val diferenciaAnimo =
            a.animo - b.animo

        val diferenciaEnergia =
            a.energia - b.energia

        return diferenciaAnimo * diferenciaAnimo +
                diferenciaEnergia * diferenciaEnergia
    }

    // ========================================
    // PREPARAR LOS DIAS
    // ========================================
    // Esta funcion tambien podra utilizarse
    // en el futuro motor de anomalias.

    fun prepararDias(
        registros: List<RegistroParaIA>
    ): List<DiaIA> {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            isLenient = false
        }

        // Verificar que las fechas y las
        // puntuaciones sean validas.

        val validos = registros.filter { registro ->

            val fechaCorrecta = try {

                val fecha = formato.parse(
                    registro.fechaIso
                )

                fecha != null &&
                        formato.format(fecha) ==
                        registro.fechaIso

            } catch (e: Exception) {
                false
            }

            fechaCorrecta &&
                    registro.animo in 1..5 &&
                    registro.energia in 1..10
        }

        // Agrupar por fecha y ordenar
        // cronologicamente.

        return validos
            .groupBy { it.fechaIso }
            .toSortedMap()
            .map { (fecha, registrosDia) ->

                val valoresSueno =
                    registrosDia.mapNotNull { registro ->

                        registro.horasSueno?.takeIf {
                            it in 0.0..24.0
                        }
                    }

                DiaIA(
                    fechaIso = fecha,

                    promedioAnimo = registrosDia
                        .map { it.animo }
                        .average(),

                    promedioEnergia = registrosDia
                        .map { it.energia }
                        .average(),

                    promedioSueno =
                        if (valoresSueno.isEmpty()) {
                            null
                        } else {
                            valoresSueno.average()
                        },

                    actividades = registrosDia
                        .flatMap { it.actividades }
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet(),

                    numeroRegistros =
                        registrosDia.size
                )
            }
    }

    // ========================================
    // EJECUTAR AGRUPAMIENTO K-MEANS
    // ========================================

    fun analizar(
        registros: List<RegistroParaIA>
    ): ResultadoAgrupamientoIA {

        val dias = prepararDias(registros)

        val cantidadRegistros = dias.sumOf {
            it.numeroRegistros
        }

        // ====================================
        // VALIDAR CANTIDAD DE DIAS
        // ====================================

        if (dias.size < MINIMO_DIAS) {

            return ResultadoAgrupamientoIA(
                diasAnalizados = dias.size,
                registrosValidos = cantidadRegistros,
                datosSuficientes = false,
                grupos = emptyList(),
                dispersionKMeans = null,
                dispersionBase = null,
                comparacionDirecta = false,
                mensaje = "Se necesitan al menos " +
                        "$MINIMO_DIAS días diferentes. " +
                        "Actualmente hay ${dias.size}."
            )
        }

        // ====================================
        // PREPARAR PUNTOS DE LA IA
        // ====================================

        val puntos = dias.map {
            normalizar(it)
        }

        val puntosDistintos = puntos
            .distinct()
            .sortedWith(
                compareBy<Punto> { it.animo }
                    .thenBy { it.energia }
            )

        // Si todos los dias tienen exactamente
        // el mismo animo y energia, no existe
        // variacion suficiente para agrupar.

        if (puntosDistintos.size < 2) {

            return ResultadoAgrupamientoIA(
                diasAnalizados = dias.size,
                registrosValidos = cantidadRegistros,
                datosSuficientes = false,
                grupos = emptyList(),
                dispersionKMeans = null,
                dispersionBase = null,
                comparacionDirecta = false,
                mensaje = "Los días son demasiado " +
                        "similares para formar grupos " +
                        "diferentes."
            )
        }

        // ====================================
        // NUMERO DE GRUPOS
        // ====================================
        // Intentamos obtener tres grupos.
        // Si solo existen dos tipos distintos
        // de dias, utilizamos dos.

        val k = minOf(
            3,
            puntosDistintos.size
        )

        // ====================================
        // CENTROS INICIALES
        // ====================================
        // Elegimos el primer centro y despues
        // puntos alejados de los anteriores.
        //
        // No usamos numeros aleatorios:
        // el resultado es reproducible.

        var centros = mutableListOf(
            puntosDistintos.first()
        )

        while (centros.size < k) {

            val disponibles = puntosDistintos.filter {
                it !in centros
            }

            val siguiente = disponibles.maxByOrNull {
                    candidato ->

                centros.minOf { centro ->
                    distanciaCuadrada(
                        candidato,
                        centro
                    )
                }
            }

            if (siguiente == null) break

            centros.add(siguiente)
        }

        // ====================================
        // ASIGNACIONES INICIALES
        // ====================================

        var asignaciones = IntArray(
            puntos.size
        ) {
            -1
        }

        // ====================================
        // ENTRENAR K-MEANS
        // ====================================
        // 1. Asignar dias al centro mas cercano.
        // 2. Recalcular el centro de cada grupo.
        // 3. Repetir hasta estabilizar.

        for (iteracion in 0 until MAX_ITERACIONES) {

            val nuevasAsignaciones =
                IntArray(puntos.size) { indice ->

                    centros.indices.minByOrNull {
                            numeroCentro ->

                        distanciaCuadrada(
                            puntos[indice],
                            centros[numeroCentro]
                        )
                    } ?: 0
                }

            // Si ya no cambian las asignaciones,
            // el modelo ha convergido.

            if (
                nuevasAsignaciones.contentEquals(
                    asignaciones
                )
            ) {
                break
            }

            asignaciones = nuevasAsignaciones

            val centrosActualizados =
                centros.indices.map { numeroCentro ->

                    val miembros = puntos.filterIndexed {
                            indice, _ ->

                        asignaciones[indice] ==
                                numeroCentro
                    }

                    if (miembros.isEmpty()) {

                        // Conservamos el centro
                        // si temporalmente no
                        // tiene puntos asignados.
                        centros[numeroCentro]

                    } else {

                        Punto(
                            animo = miembros
                                .map { it.animo }
                                .average(),

                            energia = miembros
                                .map { it.energia }
                                .average()
                        )
                    }
                }

            centros = centrosActualizados
                .toMutableList()
        }

        // Reasignacion final a los centros
        // encontrados por el algoritmo.

        asignaciones = IntArray(
            puntos.size
        ) { indice ->

            centros.indices.minByOrNull {
                    numeroCentro ->

                distanciaCuadrada(
                    puntos[indice],
                    centros[numeroCentro]
                )
            } ?: 0
        }

        // ====================================
        // CONSTRUIR GRUPOS EXPLICABLES
        // ====================================

        val gruposSinOrdenar =
            centros.indices.mapNotNull { numeroCentro ->

                val diasGrupo = dias.filterIndexed {
                        indice, _ ->

                    asignaciones[indice] ==
                            numeroCentro
                }

                if (diasGrupo.isEmpty()) {

                    null

                } else {

                    val promedioAnimo =
                        diasGrupo.map {
                            it.promedioAnimo
                        }.average()

                    val promedioEnergia =
                        diasGrupo.map {
                            it.promedioEnergia
                        }.average()

                    val actividadesFrecuentes =
                        diasGrupo
                            .flatMap { it.actividades }
                            .groupingBy { it }
                            .eachCount()
                            .toList()
                            .sortedWith(
                                compareByDescending<Pair<String, Int>> {
                                    it.second
                                }.thenBy {
                                    it.first
                                }
                            )
                            .take(3)
                            .map { it.first }

                    GrupoIA(
                        numero = numeroCentro + 1,

                        nombre = nombreGrupo(
                            promedioAnimo,
                            promedioEnergia
                        ),

                        dias = diasGrupo,

                        promedioAnimo =
                            promedioAnimo,

                        promedioEnergia =
                            promedioEnergia,

                        actividadesFrecuentes =
                            actividadesFrecuentes
                    )
                }
            }

        // Ordenamos del animo mas bajo
        // al mas alto para facilitar
        // la interpretacion visual.

        val gruposOrdenados = gruposSinOrdenar
            .sortedWith(
                compareBy<GrupoIA> {
                    it.promedioAnimo
                }.thenBy {
                    it.promedioEnergia
                }
            )
            .mapIndexed { indice, grupo ->

                grupo.copy(
                    numero = indice + 1
                )
            }

        // ====================================
        // CALCULAR DISPERSION DE K-MEANS
        // ====================================

        val dispersionKMeans = calcularDispersion(
            puntos = puntos,
            asignaciones = asignaciones.toList()
        )

        // ====================================
        // CREAR METODO BASE DE COMPARACION
        // ====================================
        // Baseline: clasificar solo por animo.
        //
        // 1..2 -> bajo
        // 3    -> medio
        // 4..5 -> alto
        //
        // K-means utiliza animo Y energia.
        // Compararemos la dispersion interna
        // sobre las mismas dos variables.

        val asignacionesBase = dias.map { dia ->

            when {
                dia.promedioAnimo < 2.5 -> 0
                dia.promedioAnimo < 3.5 -> 1
                else -> 2
            }
        }

        val dispersionBase = calcularDispersion(
            puntos = puntos,
            asignaciones = asignacionesBase
        )

        // Una comparacion directa requiere
        // que ambos metodos produzcan el
        // mismo numero de grupos.

        val comparacionDirecta =
            gruposOrdenados.size ==
                    asignacionesBase.distinct().size

        // ====================================
        // DEVOLVER RESULTADO
        // ====================================

        return ResultadoAgrupamientoIA(
            diasAnalizados = dias.size,
            registrosValidos = cantidadRegistros,
            datosSuficientes = true,
            grupos = gruposOrdenados,
            dispersionKMeans = dispersionKMeans,
            dispersionBase = dispersionBase,
            comparacionDirecta = comparacionDirecta,

            mensaje = "Se encontraron " +
                    "${gruposOrdenados.size} grupos " +
                    "de días con características " +
                    "emocionales similares."
        )
    }

    // ========================================
    // NOMBRAR GRUPOS AUTOMATICAMENTE
    // ========================================

    private fun nombreGrupo(
        animo: Double,
        energia: Double
    ): String {

        val nivelAnimo = when {

            animo < 2.5 -> "bajo"
            animo < 3.75 -> "medio"
            else -> "alto"
        }

        val nivelEnergia = when {

            energia < 4.0 -> "baja"
            energia < 7.0 -> "media"
            else -> "alta"
        }

        return "Ánimo $nivelAnimo · " +
                "Energía $nivelEnergia"
    }

    // ========================================
    // DISPERSION INTERNA DE LOS GRUPOS
    // ========================================
    // Calcula el promedio de distancias
    // al cuadrado respecto al centro
    // de cada grupo.
    //
    // Menor dispersion significa que
    // los puntos quedaron mas agrupados.
    //
    // Esta medida NO demuestra que una
    // agrupacion sea clinicamente valida.

    private fun calcularDispersion(
        puntos: List<Punto>,
        asignaciones: List<Int>
    ): Double {

        if (puntos.isEmpty()) return 0.0

        val indicesPorGrupo =
            puntos.indices.groupBy {
                asignaciones[it]
            }

        val sumaDistancias =
            indicesPorGrupo.values.sumOf { indices ->

                val centro = Punto(
                    animo = indices
                        .map { puntos[it].animo }
                        .average(),

                    energia = indices
                        .map { puntos[it].energia }
                        .average()
                )

                indices.sumOf { indice ->
                    distanciaCuadrada(
                        puntos[indice],
                        centro
                    )
                }
            }

        return sumaDistancias / puntos.size
    }
}
