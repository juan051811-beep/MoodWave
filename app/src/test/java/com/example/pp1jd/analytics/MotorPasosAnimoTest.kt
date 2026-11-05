
package com.example.pp1jd.analytics

import com.example.pp1jd.data.local.PasosDiariosEntity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

import java.time.LocalDate

// ============================================
// PRUEBAS UNITARIAS DE PASOS Y EMOCIONES
// ============================================
//
// Verificamos matematicamente el motor:
//
// MotorPasosAnimo.kt
//
// Estas pruebas no necesitan:
// - Un celular.
// - Health Connect instalado.
// - Una base de datos real.
// - Conexion a Internet.
//
// Utilizamos datos controlados para
// conocer los resultados esperados.
//
// Las pruebas no modifican los
// registros reales de MoodWave.

class MotorPasosAnimoTest {

    // ========================================
    // FECHA FIJA PARA LAS PRUEBAS
    // ========================================
    //
    // Usamos una fecha fija para que las
    // pruebas no cambien de resultado
    // dependiendo del dia de ejecucion.

    private val fechaFinal =
        LocalDate.of(2026, 11, 4)

    // ========================================
    // CREAR REGISTRO EMOCIONAL
    // ========================================

    private fun emocion(
        diasAtras: Long,
        animo: Int,
        energia: Int,
        sintetico: Boolean = false
    ): RegistroParaRelacionPasos {

        return RegistroParaRelacionPasos(
            fechaIso = fechaFinal
                .minusDays(diasAtras)
                .toString(),

            valorAnimo = animo,

            energia = energia,

            esSintetico = sintetico
        )
    }

    // ========================================
    // CREAR REGISTRO DE PASOS
    // ========================================

    private fun pasos(
        diasAtras: Long,
        total: Long
    ): PasosDiariosEntity {

        return PasosDiariosEntity(
            fechaIso = fechaFinal
                .minusDays(diasAtras)
                .toString(),

            pasos = total,

            actualizadoEn = 123456789L
        )
    }

    // ========================================
    // EJECUTAR MOTOR
    // ========================================

    private fun analizar(
        emociones: List<RegistroParaRelacionPasos>,
        pasosDiarios: List<PasosDiariosEntity>,
        periodo: Int = 30
    ): ResultadoRelacionPasos {

        return MotorPasosAnimo.analizar(
            registros = emociones,
            pasosDiarios = pasosDiarios,
            periodoDias = periodo,
            fechaFinal = fechaFinal
        )
    }

    // ========================================
    // PRUEBA 1
    // CORRELACION POSITIVA PERFECTA
    // ========================================
    //
    // Construimos 7 dias.
    //
    // A mayor cantidad de pasos,
    // mayor animo y energia.
    //
    // La relacion es perfectamente
    // lineal, por lo que Pearson
    // debe producir +1.

    @Test
    fun correlacionPositivaPerfecta() {

        val animos = listOf(
            1, 2, 3, 4, 5, 4, 3
        )

        val emociones = animos.mapIndexed {
                indice, valor ->

            emocion(
                diasAtras = (6 - indice).toLong(),
                animo = valor,
                energia = valor * 2
            )
        }

        val pasosDiarios = animos.mapIndexed {
                indice, valor ->

            pasos(
                diasAtras = (6 - indice).toLong(),
                total = valor * 100L
            )
        }

        val resultado = analizar(
            emociones,
            pasosDiarios,
            periodo = 7
        )

        assertEquals(
            7,
            resultado.diasComparables
        )

        assertEquals(
            2200L,
            resultado.totalPasos
        )

        assertNotNull(
            resultado.correlacionPasosAnimo
        )

        assertNotNull(
            resultado.correlacionPasosEnergia
        )

        assertEquals(
            1.0,
            resultado.correlacionPasosAnimo!!,
            0.000001
        )

        assertEquals(
            1.0,
            resultado.correlacionPasosEnergia!!,
            0.000001
        )
    }

    // ========================================
    // PRUEBA 2
    // CORRELACION NEGATIVA PERFECTA
    // ========================================
    //
    // Cuando los pasos aumentan,
    // animo y energia disminuyen.
    //
    // El resultado esperado es -1.
    //
    // Esto NO significa que caminar
    // cause una disminucion del animo;
    // solo verifica el calculo matematico.

    @Test
    fun correlacionNegativaPerfecta() {

        val animos = listOf(
            5, 4, 3, 2, 1, 2, 3
        )

        val emociones = animos.mapIndexed {
                indice, valor ->

            emocion(
                diasAtras = (6 - indice).toLong(),
                animo = valor,
                energia = valor * 2
            )
        }

        val pasosDiarios = animos.mapIndexed {
                indice, valor ->

            pasos(
                diasAtras = (6 - indice).toLong(),
                total = (6 - valor) * 100L
            )
        }

        val resultado = analizar(
            emociones,
            pasosDiarios,
            periodo = 7
        )

        assertEquals(
            -1.0,
            resultado.correlacionPasosAnimo!!,
            0.000001
        )

        assertEquals(
            -1.0,
            resultado.correlacionPasosEnergia!!,
            0.000001
        )
    }

    // ========================================
    // PRUEBA 3
    // MENOS DE 7 DIAS COMPARABLES
    // ========================================
    //
    // Aunque exista una relacion clara,
    // el motor no debe calcular
    // Pearson con menos de 7 dias.

    @Test
    fun seisDiasNoSonSuficientes() {

        val emociones = (0 until 6).map {

            emocion(
                diasAtras = it.toLong(),
                animo = (it % 5) + 1,
                energia = (it % 5) + 1
            )
        }

        val pasosDiarios = (0 until 6).map {

            pasos(
                diasAtras = it.toLong(),
                total = (it + 1) * 100L
            )
        }

        val resultado = analizar(
            emociones,
            pasosDiarios,
            periodo = 7
        )

        assertEquals(
            6,
            resultado.diasComparables
        )

        assertNull(
            resultado.correlacionPasosAnimo
        )

        assertNull(
            resultado.correlacionPasosEnergia
        )
    }

    // ========================================
    // PRUEBA 4
    // PASOS SIN VARIACION
    // ========================================
    //
    // Todos los dias presentan
    // exactamente 3500 pasos.
    //
    // Pearson no puede calcularse
    // si una variable es constante.

    @Test
    fun pasosConstantesNoPermitenPearson() {

        val emociones = (0 until 7).map {

            emocion(
                diasAtras = it.toLong(),
                animo = (it % 5) + 1,
                energia = (it % 5) + 2
            )
        }

        val pasosDiarios = (0 until 7).map {

            pasos(
                diasAtras = it.toLong(),
                total = 3500L
            )
        }

        val resultado = analizar(
            emociones,
            pasosDiarios,
            periodo = 7
        )

        assertEquals(
            7,
            resultado.diasComparables
        )

        assertNull(
            resultado.correlacionPasosAnimo
        )

        assertNull(
            resultado.correlacionPasosEnergia
        )
    }

    // ========================================
    // PRUEBA 5
    // ANIMO Y ENERGIA SIN VARIACION
    // ========================================
    //
    // Los pasos cambian cada dia.
    //
    // Sin embargo, el animo siempre
    // es 3 y la energia siempre es 5.
    //
    // No puede calcularse Pearson.

    @Test
    fun emocionesConstantesNoPermitenPearson() {

        val emociones = (0 until 7).map {

            emocion(
                diasAtras = it.toLong(),
                animo = 3,
                energia = 5
            )
        }

        val pasosDiarios = (0 until 7).map {

            pasos(
                diasAtras = it.toLong(),
                total = (it + 1) * 500L
            )
        }

        val resultado = analizar(
            emociones,
            pasosDiarios,
            periodo = 7
        )

        assertNull(
            resultado.correlacionPasosAnimo
        )

        assertNull(
            resultado.correlacionPasosEnergia
        )
    }

    // ========================================
    // PRUEBA 6
    // VARIAS EMOCIONES EN EL MISMO DIA
    // ========================================
    //
    // Hoy:
    // Animo = 1 y 5
    // Energia = 2 y 8
    //
    // Promedio de animo = 3
    // Promedio de energia = 5
    //
    // Los 57 pasos del dia solamente
    // deben contarse una vez.
    //
    // Tambien comprobamos la inclusion
    // de datos sinteticos.

    @Test
    fun variasEmocionesSePromedianPorDia() {

        val emociones = listOf(

            emocion(
                diasAtras = 0,
                animo = 1,
                energia = 2,
                sintetico = true
            ),

            emocion(
                diasAtras = 0,
                animo = 5,
                energia = 8
            ),

            emocion(
                diasAtras = 1,
                animo = 4,
                energia = 6
            )
        )

        val pasosDiarios = listOf(

            pasos(0, 57L),

            pasos(1, 100L),

            pasos(2, 200L)
        )

        val resultado = analizar(
            emociones,
            pasosDiarios,
            periodo = 7
        )

        val diaHoy = resultado.dias.first {

            it.fechaIso == fechaFinal.toString()
        }

        assertEquals(
            3.0,
            diaHoy.animoPromedio,
            0.000001
        )

        assertEquals(
            5.0,
            diaHoy.energiaPromedio,
            0.000001
        )

        assertEquals(
            2,
            diaHoy.cantidadRegistros
        )

        assertEquals(
            2,
            resultado.diasComparables
        )

        assertEquals(
            3,
            resultado.diasConPasos
        )

        // 57 + 100 + 200 = 357.
        //
        // No sumamos los 57 pasos
        // dos veces por tener dos
        // emociones en ese dia.

        assertEquals(
            357L,
            resultado.totalPasos
        )

        assertTrue(
            diaHoy.incluyeDatosSinteticos
        )

        assertTrue(
            resultado.incluyeDatosSinteticos
        )
    }

    // ========================================
    // PRUEBA 7
    // CERO PASOS VS AUSENCIA DE DATOS
    // ========================================
    //
    // Hoy: 0 pasos registrados.
    //
    // Ayer: 57 pasos registrados.
    //
    // Anteayer: hay emocion,
    // pero no existe registro de pasos.
    //
    // Cero es un dato valido.
    //
    // Ausencia de datos NO es cero.

    @Test
    fun ceroPasosEsDistintoDeSinDatos() {

        val emociones = listOf(

            emocion(0, 3, 5),

            emocion(1, 4, 7),

            emocion(2, 2, 3)
        )

        val pasosDiarios = listOf(

            pasos(0, 0L),

            pasos(1, 57L)
        )

        val resultado = analizar(
            emociones,
            pasosDiarios,
            periodo = 7
        )

        assertEquals(
            2,
            resultado.diasConPasos
        )

        assertEquals(
            3,
            resultado.diasConEmociones
        )

        assertEquals(
            2,
            resultado.diasComparables
        )

        assertEquals(
            57L,
            resultado.totalPasos
        )

        assertTrue(
            resultado.dias.any {
                it.pasos == 0L
            }
        )

        assertFalse(
            resultado.dias.any {

                it.fechaIso ==
                        fechaFinal.minusDays(2).toString()
            }
        )
    }

    // ========================================
    // PRUEBA 8
    // RESPETAR EL PERIODO SELECCIONADO
    // ========================================
    //
    // Analizamos 7 dias.
    //
    // Un registro de hace 10 dias
    // debe quedar fuera del calculo.
    //
    // Tambien excluimos datos futuros.

    @Test
    fun excluyeDatosFueraDelPeriodo() {

        val emociones = listOf(

            emocion(0, 4, 8),

            emocion(1, 3, 6),

            emocion(10, 1, 2),

            emocion(-1, 5, 10)
        )

        val pasosDiarios = listOf(

            pasos(0, 57L),

            pasos(1, 100L),

            pasos(10, 9000L),

            pasos(-1, 7000L)
        )

        val resultado = analizar(
            emociones,
            pasosDiarios,
            periodo = 7
        )

        assertEquals(
            2,
            resultado.diasComparables
        )

        assertEquals(
            157L,
            resultado.totalPasos
        )

        assertEquals(
            2,
            resultado.diasConPasos
        )

        assertEquals(
            2,
            resultado.diasConEmociones
        )
    }

    // ========================================
    // PRUEBA 9
    // ACTUALIZACION SIN DUPLICAR PASOS
    // ========================================
    //
    // Primera lectura:
    // 57 pasos.
    //
    // Segunda lectura:
    // 120 pasos.
    //
    // El motor debe trabajar con el
    // valor actualizado del registro.
    //
    // La sustitucion del valor en Room
    // corresponde al DAO con @Upsert.

    @Test
    fun actualizacionUtilizaTotalNuevo() {

        val emociones = listOf(

            emocion(0, 4, 8)
        )

        val resultadoInicial = analizar(

            emociones = emociones,

            pasosDiarios = listOf(
                pasos(0, 57L)
            ),

            periodo = 7
        )

        val resultadoNuevo = analizar(

            emociones = emociones,

            pasosDiarios = listOf(
                pasos(0, 120L)
            ),

            periodo = 7
        )

        assertEquals(
            57L,
            resultadoInicial.totalPasos
        )

        assertEquals(
            120L,
            resultadoNuevo.totalPasos
        )

        assertEquals(
            1,
            resultadoNuevo.diasConPasos
        )
    }

    // ========================================
    // PRUEBA 10
    // CONVERSION DE EMOCIONES
    // ========================================
    //
    // Verificamos la escala:
    //
    // Muy mal  = 1
    // Mal      = 2
    // Normal   = 3
    // Bien     = 4
    // Muy bien = 5
    //
    // Los nombres desconocidos
    // deben producir null.

    @Test
    fun convierteNombresDeEmociones() {

        assertEquals(
            1,
            MotorPasosAnimo.valorDeAnimo(
                "Muy mal"
            )
        )

        assertEquals(
            2,
            MotorPasosAnimo.valorDeAnimo(
                "Mal"
            )
        )

        assertEquals(
            3,
            MotorPasosAnimo.valorDeAnimo(
                "Normal"
            )
        )

        assertEquals(
            4,
            MotorPasosAnimo.valorDeAnimo(
                "Bien"
            )
        )

        assertEquals(
            5,
            MotorPasosAnimo.valorDeAnimo(
                "Muy bien"
            )
        )

        assertEquals(
            5,
            MotorPasosAnimo.valorDeAnimo(
                "  MUY BIEN  "
            )
        )

        assertNull(
            MotorPasosAnimo.valorDeAnimo(
                "Emocion desconocida"
            )
        )
    }

    // ========================================
    // PRUEBA 11
    // SIN PASOS GUARDADOS
    // ========================================
    //
    // Existen emociones registradas,
    // pero no tenemos informacion
    // de Health Connect en Room.
    //
    // No debe inventarse informacion
    // ni calcular correlaciones.

    @Test
    fun sinPasosNoInventaResultados() {

        val emociones = listOf(

            emocion(0, 4, 7),

            emocion(1, 3, 5)
        )

        val resultado = analizar(

            emociones = emociones,

            pasosDiarios = emptyList(),

            periodo = 30
        )

        assertEquals(
            0,
            resultado.diasConPasos
        )

        assertEquals(
            0,
            resultado.diasComparables
        )

        assertEquals(
            0L,
            resultado.totalPasos
        )

        assertNull(
            resultado.promedioPasos
        )

        assertNull(
            resultado.maximoPasos
        )

        assertNull(
            resultado.correlacionPasosAnimo
        )

        assertNull(
            resultado.correlacionPasosEnergia
        )

        assertTrue(
            resultado.dias.isEmpty()
        )

        assertTrue(
            resultado.mensaje.contains(
                "Todavía no hay pasos"
            )
        )
    }
}
