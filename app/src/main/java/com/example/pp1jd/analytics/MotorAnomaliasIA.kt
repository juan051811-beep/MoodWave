
package com.example.pp1jd.analytics

import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Locale
import java.util.TimeZone

// ============================================
// TIPO DE CAMBIO DETECTADO
// ============================================

enum class TipoCambioIA {
    DESCENSO,
    AUMENTO,
    MIXTO
}

// ============================================
// PERIODO CON CAMBIO SOSTENIDO
// ============================================
// Guarda las fechas y los valores que
// justifican cada resultado de la IA.

data class AnomaliaSostenidaIA(

    val tipo: TipoCambioIA,

    val fechaInicio: String,
    val fechaFin: String,

    val diasConsecutivos: Int,

    // Fechas exactas del cambio.
    val fechas: List<String>,

    // Registros incluidos en el periodo.
    val numeroRegistros: Int,

    // Promedios durante el cambio.
    val animoPromedio: Double,
    val energiaPromedio: Double,

    // Promedios historicos de referencia.
    val animoReferencia: Double,
    val energiaReferencia: Double,

    // Diferencias con la referencia.
    val diferenciaAnimo: Double,
    val diferenciaEnergia: Double,

    // Explicacion para la interfaz.
    val explicacion: String
)

// ============================================
// RESULTADO GENERAL DEL MOTOR
// ============================================

data class ResultadoAnomaliasIA(

    val diasAnalizados: Int,
    val registrosValidos: Int,

    // Dias que tuvieron una referencia
    // historica suficiente.
    val diasConReferencia: Int,

    // Dias con cambios notables,
    // aunque no todos formen una racha.
    val diasConCambioNotable: Int,

    val datosSuficientes: Boolean,

    // Periodos detectados.
    val anomalias: List<AnomaliaSostenidaIA>,

    val mensaje: String
)

// ============================================
// MOTOR DE DETECCION DE ANOMALIAS
// ============================================

object MotorAnomaliasIA {

    // ========================================
    // PARAMETROS DEL ALGORITMO
    // ========================================

    // Cantidad de dias del calendario que
    // utilizaremos para buscar antecedentes.

    const val VENTANA_HISTORICA = 21

    // Dias con informacion necesarios
    // dentro de la ventana historica.

    const val MINIMO_DIAS_REFERENCIA = 14

    // Minimo de dias consecutivos
    // para reconocer un cambio sostenido.

    const val MINIMO_DIAS_CONSECUTIVOS = 3

    // Diferencias minimas respecto al
    // promedio historico.

    const val UMBRAL_ANIMO = 1.0
    const val UMBRAL_ENERGIA = 2.0

    // ========================================
    // ESTRUCTURAS INTERNAS
    // ========================================

    private data class DiaConFecha(
        val dia: DiaIA,

        // Dia expresado como numero entero.
        // Facilita comprobar continuidad.
        val numeroDia: Long
    )

    private data class DiaConCambio(
        val dia: DiaIA,
        val numeroDia: Long,
        val tipo: TipoCambioIA,

        val animoReferencia: Double,
        val energiaReferencia: Double
    )

    // ========================================
    // CONVERTIR FECHA A NUMERO DE DIA
    // ========================================
    // Utilizamos UTC para que los cambios
    // de horario no alteren la diferencia
    // entre fechas del calendario.

    private fun numeroDeDia(
        fechaIso: String,
        formato: SimpleDateFormat
    ): Long? {

        return try {

            val fecha = formato.parse(fechaIso)
                ?: return null

            // Un dia tiene 86,400,000 ms
            // en la escala UTC utilizada.

            Math.floorDiv(
                fecha.time,
                86_400_000L
            )

        } catch (e: Exception) {

            null
        }
    }

    // ========================================
    // CLASIFICAR TIPO DE CAMBIO
    // ========================================
    //
    // DESCENSO:
    // Baja notable del animo o energia.
    //
    // AUMENTO:
    // Subida notable del animo o energia.
    //
    // MIXTO:
    // Una variable sube y otra baja.
    //
    // null:
    // No supera nuestros umbrales.

    private fun clasificarCambio(
        diferenciaAnimo: Double,
        diferenciaEnergia: Double
    ): TipoCambioIA? {

        val descenso =
            diferenciaAnimo <= -UMBRAL_ANIMO ||
                    diferenciaEnergia <= -UMBRAL_ENERGIA

        val aumento =
            diferenciaAnimo >= UMBRAL_ANIMO ||
                    diferenciaEnergia >= UMBRAL_ENERGIA

        return when {

            descenso && aumento ->
                TipoCambioIA.MIXTO

            descenso ->
                TipoCambioIA.DESCENSO

            aumento ->
                TipoCambioIA.AUMENTO

            else ->
                null
        }
    }

    // ========================================
    // ANALIZAR TODOS LOS REGISTROS
    // ========================================
    // Recibe la informacion desde Room
    // mediante nuestro ViewModel.
    //
    // No tiene limite de 90 registros.
    //
    // Cada vez que ejecutemos este metodo,
    // se utilizaran los datos disponibles.

    fun analizar(
        registros: List<RegistroParaIA>
    ): ResultadoAnomaliasIA {

        // ====================================
        // PREPARAR DATOS POR DIA
        // ====================================
        // Reutilizamos el motor de agrupamiento
        // para validar y agrupar registros.
        //
        // Si hay varios registros en un dia,
        // se calcula un promedio diario.

        val dias = MotorAgrupamientoIA.prepararDias(
            registros
        )

        val totalRegistros = dias.sumOf {
            it.numeroRegistros
        }

        // ====================================
        // COMPROBAR MINIMO DE INFORMACION
        // ====================================

        val minimoInicial =
            MINIMO_DIAS_REFERENCIA +
                    MINIMO_DIAS_CONSECUTIVOS

        if (dias.size < minimoInicial) {

            return ResultadoAnomaliasIA(
                diasAnalizados = dias.size,
                registrosValidos = totalRegistros,
                diasConReferencia = 0,
                diasConCambioNotable = 0,
                datosSuficientes = false,
                anomalias = emptyList(),

                mensaje = "Se necesitan al menos " +
                        "$minimoInicial días diferentes " +
                        "para comenzar a evaluar cambios " +
                        "sostenidos. Actualmente hay " +
                        "${dias.size}."
            )
        }

        // ====================================
        // PREPARAR FECHAS UTC
        // ====================================

        val formato = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {

            isLenient = false

            timeZone = TimeZone.getTimeZone(
                "UTC"
            )
        }

        val diasOrdenados = dias.mapNotNull { dia ->

            val numero = numeroDeDia(
                dia.fechaIso,
                formato
            )

            if (numero == null) {

                null

            } else {

                DiaConFecha(
                    dia = dia,
                    numeroDia = numero
                )
            }
        }.sortedBy {
            it.numeroDia
        }

        // ====================================
        // VENTANA HISTORICA DINAMICA
        // ====================================
        //
        // Guardamos los dias anteriores
        // que esten dentro de los ultimos
        // 21 dias del calendario.
        //
        // No incluimos el dia actual
        // dentro de su propia referencia.

        val historial =
            ArrayDeque<DiaConFecha>()

        var sumaAnimo = 0.0
        var sumaEnergia = 0.0

        // ====================================
        // CONTADORES
        // ====================================

        var diasConReferencia = 0
        var diasConCambio = 0

        // ====================================
        // RESULTADOS Y RACHA ACTUAL
        // ====================================

        val anomalias =
            mutableListOf<AnomaliaSostenidaIA>()

        var rachaActual =
            mutableListOf<DiaConCambio>()

        // ====================================
        // CERRAR UNA RACHA
        // ====================================
        //
        // Solo guardaremos rachas con
        // tres o mas dias consecutivos.

        fun cerrarRacha() {

            if (
                rachaActual.size >=
                MINIMO_DIAS_CONSECUTIVOS
            ) {

                val racha = rachaActual.toList()

                val tipo = racha.first().tipo

                val promedioAnimo = racha.map {
                    it.dia.promedioAnimo
                }.average()

                val promedioEnergia = racha.map {
                    it.dia.promedioEnergia
                }.average()

                val referenciaAnimo = racha.map {
                    it.animoReferencia
                }.average()

                val referenciaEnergia = racha.map {
                    it.energiaReferencia
                }.average()

                val diferenciaAnimo =
                    promedioAnimo - referenciaAnimo

                val diferenciaEnergia =
                    promedioEnergia - referenciaEnergia

                val fechas = racha.map {
                    it.dia.fechaIso
                }

                val numeroRegistros = racha.sumOf {
                    it.dia.numeroRegistros
                }

                // =================================
                // EXPLICACION NO DIAGNOSTICA
                // =================================

                val descripcion = when (tipo) {

                    TipoCambioIA.DESCENSO ->
                        "Se observó un descenso " +
                                "sostenido en el ánimo o " +
                                "la energía respecto a " +
                                "los días anteriores."

                    TipoCambioIA.AUMENTO ->
                        "Se observó un aumento " +
                                "sostenido en el ánimo o " +
                                "la energía respecto a " +
                                "los días anteriores."

                    TipoCambioIA.MIXTO ->
                        "Se observaron cambios " +
                                "en direcciones diferentes " +
                                "entre el ánimo y la energía."
                }

                val explicacion =
                    "$descripcion El cambio abarcó " +
                            "${racha.size} días consecutivos, " +
                            "del ${fechas.first()} " +
                            "al ${fechas.last()}. " +
                            "Es una observación estadística, " +
                            "no un diagnóstico."

                // =================================
                // GUARDAR PERIODO DETECTADO
                // =================================

                anomalias.add(
                    AnomaliaSostenidaIA(
                        tipo = tipo,

                        fechaInicio =
                            fechas.first(),

                        fechaFin =
                            fechas.last(),

                        diasConsecutivos =
                            racha.size,

                        fechas = fechas,

                        numeroRegistros =
                            numeroRegistros,

                        animoPromedio =
                            promedioAnimo,

                        energiaPromedio =
                            promedioEnergia,

                        animoReferencia =
                            referenciaAnimo,

                        energiaReferencia =
                            referenciaEnergia,

                        diferenciaAnimo =
                            diferenciaAnimo,

                        diferenciaEnergia =
                            diferenciaEnergia,

                        explicacion =
                            explicacion
                    )
                )
            }

            // Limpiar la racha para
            // comenzar una nueva.

            rachaActual =
                mutableListOf()
        }

        // ====================================
        // RECORRER TODO EL HISTORIAL
        // ====================================

        for (actual in diasOrdenados) {

            // =================================
            // ELIMINAR DATOS DEMASIADO ANTIGUOS
            // =================================
            //
            // Conservamos solo registros
            // de los ultimos 21 dias previos.

            val fechaMinima =
                actual.numeroDia -
                        VENTANA_HISTORICA

            while (
                historial.isNotEmpty() &&
                historial.first().numeroDia <
                fechaMinima
            ) {

                val antiguo =
                    historial.removeFirst()

                sumaAnimo -=
                    antiguo.dia.promedioAnimo

                sumaEnergia -=
                    antiguo.dia.promedioEnergia
            }

            // =================================
            // CALCULAR REFERENCIA HISTORICA
            // =================================

            if (
                historial.size >=
                MINIMO_DIAS_REFERENCIA
            ) {

                diasConReferencia++

                val referenciaAnimo =
                    sumaAnimo / historial.size

                val referenciaEnergia =
                    sumaEnergia / historial.size

                // =================================
                // COMPARAR CON EL DIA ACTUAL
                // =================================

                val diferenciaAnimo =
                    actual.dia.promedioAnimo -
                            referenciaAnimo

                val diferenciaEnergia =
                    actual.dia.promedioEnergia -
                            referenciaEnergia

                val tipo = clasificarCambio(
                    diferenciaAnimo,
                    diferenciaEnergia
                )

                // =================================
                // DIA CON CAMBIO NOTABLE
                // =================================

                if (tipo != null) {

                    diasConCambio++

                    // Si el cambio es diferente
                    // o falta un dia consecutivo,
                    // cerramos la racha anterior.

                    if (rachaActual.isNotEmpty()) {

                        val ultimo =
                            rachaActual.last()

                        val sonConsecutivos =
                            actual.numeroDia ==
                                    ultimo.numeroDia + 1L

                        val mismaDireccion =
                            tipo == ultimo.tipo

                        if (
                            !sonConsecutivos ||
                            !mismaDireccion
                        ) {

                            cerrarRacha()
                        }
                    }

                    // Agregar dia a la racha.

                    rachaActual.add(
                        DiaConCambio(
                            dia = actual.dia,

                            numeroDia =
                                actual.numeroDia,

                            tipo = tipo,

                            animoReferencia =
                                referenciaAnimo,

                            energiaReferencia =
                                referenciaEnergia
                        )
                    )

                } else {

                    // Si hoy no supera los
                    // umbrales, termina la racha.

                    cerrarRacha()
                }

            } else {

                // Sin referencia suficiente,
                // no afirmamos que exista
                // un cambio anomalo.

                cerrarRacha()
            }

            // =================================
            // ACTUALIZAR HISTORIAL
            // =================================
            //
            // El dia actual se agrega despues
            // de evaluar sus diferencias.
            //
            // Asi no participa en su propia
            // referencia historica.

            historial.addLast(actual)

            sumaAnimo +=
                actual.dia.promedioAnimo

            sumaEnergia +=
                actual.dia.promedioEnergia
        }

        // ====================================
        // GUARDAR ULTIMA RACHA
        // ====================================

        cerrarRacha()

        // ====================================
        // ORDENAR RESULTADOS
        // ====================================
        // Mostramos primero los cambios
        // mas recientes.

        val resultadosOrdenados =
            anomalias.sortedByDescending {
                it.fechaFin
            }

        // ====================================
        // VERIFICAR SI HUBO REFERENCIA
        // ====================================

        val hayReferencia =
            diasConReferencia > 0

        // ====================================
        // MENSAJE FINAL
        // ====================================

        val mensaje = when {

            !hayReferencia ->

                "No hay suficientes días " +
                        "registrados dentro de " +
                        "ventanas históricas de " +
                        "$VENTANA_HISTORICA días. " +
                        "Se necesitan al menos " +
                        "$MINIMO_DIAS_REFERENCIA " +
                        "días con información."

            resultadosOrdenados.isEmpty() ->

                "No se identificaron cambios " +
                        "sostenidos que cumplan " +
                        "los criterios establecidos."

            else ->

                "Se identificaron " +
                        "${resultadosOrdenados.size} " +
                        "periodos con cambios " +
                        "sostenidos respecto a " +
                        "los registros anteriores."
        }

        // ====================================
        // DEVOLVER RESULTADO
        // ====================================

        return ResultadoAnomaliasIA(
            diasAnalizados =
                diasOrdenados.size,

            registrosValidos =
                totalRegistros,

            diasConReferencia =
                diasConReferencia,

            diasConCambioNotable =
                diasConCambio,

            datosSuficientes =
                hayReferencia,

            anomalias =
                resultadosOrdenados,

            mensaje =
                mensaje
        )
    }
}
