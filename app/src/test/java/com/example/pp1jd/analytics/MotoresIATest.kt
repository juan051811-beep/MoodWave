
package com.example.pp1jd.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

import java.time.LocalDate

// ============================================
// PRUEBAS AUTOMATICAS DE LA IA DE MOODWAVE
// ============================================
//
// Verificaremos:
//
// 1. Agrupamiento K-means.
// 2. Cantidad variable de registros.
// 3. Varios registros en un mismo dia.
// 4. Minimo de informacion.
// 5. Deteccion de descensos sostenidos.
// 6. Ausencia de falsas alertas.
// 7. Continuidad de fechas.
// 8. Referencia historica.
// 9. Actualizacion con nuevos registros.
//
// Estas pruebas NO modifican Room.
// Trabajan con datos temporales en memoria.

class MotoresIATest {

    // ========================================
    // GENERAR FECHAS CONSECUTIVAS
    // ========================================

    private fun fecha(
        desplazamiento: Int
    ): String {

        return LocalDate
            .of(2026, 1, 1)
            .plusDays(desplazamiento.toLong())
            .toString()
    }

    // ========================================
    // CREAR UN REGISTRO PARA PRUEBA
    // ========================================

    private fun registro(
        dia: Int,
        animo: Int,
        energia: Int
    ): RegistroParaIA {

        return RegistroParaIA(
            fechaIso = fecha(dia),
            animo = animo,
            energia = energia,
            horasSueno = 8.0,
            actividades = listOf("Estudio")
        )
    }

    // ========================================
    // PRUEBA 1
    // K-MEANS ENCUENTRA TRES GRUPOS CLAROS
    // ========================================
    //
    // Creamos 45 dias:
    //
    // 15 dias: animo 2, energia 2
    // 15 dias: animo 3, energia 6
    // 15 dias: animo 5, energia 9
    //
    // Como los tres tipos de dia son
    // distintos, esperamos tres grupos.

    @Test
    fun kmeansEncuentraTresGrupos() {

        val datos = (0 until 45).map { dia ->

            when (dia % 3) {

                0 -> registro(
                    dia = dia,
                    animo = 2,
                    energia = 2
                )

                1 -> registro(
                    dia = dia,
                    animo = 3,
                    energia = 6
                )

                else -> registro(
                    dia = dia,
                    animo = 5,
                    energia = 9
                )
            }
        }

        val resultado =
            MotorAgrupamientoIA.analizar(datos)

        assertTrue(
            resultado.datosSuficientes
        )

        assertEquals(
            45,
            resultado.diasAnalizados
        )

        assertEquals(
            3,
            resultado.grupos.size
        )

        // Cada grupo debe tener 15 dias.

        assertEquals(
            listOf(15, 15, 15),
            resultado.grupos
                .map { it.dias.size }
                .sorted()
        )

        // La dispersion debe ser cero
        // porque cada grupo contiene
        // dias exactamente iguales.

        val dispersion =
            resultado.dispersionKMeans

        assertNotNull(dispersion)

        assertEquals(
            0.0,
            dispersion!!,
            0.0000001
        )

        // Los dos metodos utilizan
        // tres grupos en este ejemplo.

        assertTrue(
            resultado.comparacionDirecta
        )
    }

    // ========================================
    // PRUEBA 2
    // K-MEANS ACEPTA MAS DE 90 REGISTROS
    // ========================================
    //
    // Probamos con 150 dias.
    //
    // No debe existir un limite fijo
    // de 90 registros.

    @Test
    fun kmeansFuncionaConMasDe90Registros() {

        val datos = (0 until 150).map { dia ->

            when (dia % 3) {

                0 -> registro(dia, 2, 3)

                1 -> registro(dia, 3, 6)

                else -> registro(dia, 5, 9)
            }
        }

        val resultado =
            MotorAgrupamientoIA.analizar(datos)

        assertTrue(
            resultado.datosSuficientes
        )

        assertEquals(
            150,
            resultado.registrosValidos
        )

        assertEquals(
            150,
            resultado.diasAnalizados
        )

        assertEquals(
            3,
            resultado.grupos.size
        )

        // Todos los dias deben pertenecer
        // a uno de los grupos encontrados.

        assertEquals(
            150,
            resultado.grupos.sumOf {
                it.dias.size
            }
        )
    }

    // ========================================
    // PRUEBA 3
    // VARIOS REGISTROS EN EL MISMO DIA
    // ========================================
    //
    // Creamos 14 dias diferentes.
    //
    // En el primer dia guardamos:
    //
    // Registro A: animo 1, energia 2
    // Registro B: animo 5, energia 10
    //
    // El promedio diario esperado es:
    //
    // Animo = (1 + 5) / 2 = 3
    // Energia = (2 + 10) / 2 = 6

    @Test
    fun variosRegistrosMismoDiaSePromedian() {

        val datos = mutableListOf(
            registro(0, 1, 2),
            registro(0, 5, 10)
        )

        for (dia in 1 until 14) {

            datos.add(
                registro(dia, 4, 8)
            )
        }

        val dias =
            MotorAgrupamientoIA.prepararDias(
                datos
            )

        assertEquals(
            14,
            dias.size
        )

        // Aunque hay 15 registros,
        // solamente existen 14 dias.

        assertEquals(
            15,
            dias.sumOf {
                it.numeroRegistros
            }
        )

        val primerDia = dias.first()

        assertEquals(
            3.0,
            primerDia.promedioAnimo,
            0.000001
        )

        assertEquals(
            6.0,
            primerDia.promedioEnergia,
            0.000001
        )

        assertEquals(
            2,
            primerDia.numeroRegistros
        )
    }

    // ========================================
    // PRUEBA 4
    // NO AGRUPAR CON DATOS INSUFICIENTES
    // ========================================
    //
    // El motor requiere al menos
    // 14 dias diferentes.
    //
    // Con 13 dias debe indicar
    // que faltan datos.

    @Test
    fun kmeansRespetaMinimoDeDias() {

        val datos = (0 until 13).map { dia ->

            registro(
                dia = dia,
                animo = if (dia % 2 == 0) 2 else 4,
                energia = if (dia % 2 == 0) 3 else 8
            )
        }

        val resultado =
            MotorAgrupamientoIA.analizar(datos)

        assertFalse(
            resultado.datosSuficientes
        )

        assertTrue(
            resultado.grupos.isEmpty()
        )

        assertEquals(
            13,
            resultado.diasAnalizados
        )
    }

    // ========================================
    // PRUEBA 5
    // DETECTAR UN DESCENSO SOSTENIDO
    // ========================================
    //
    // Creamos un historial estable:
    //
    // 21 dias:
    // Animo 5, energia 9
    //
    // Despues agregamos:
    //
    // 4 dias:
    // Animo 2, energia 3
    //
    // Esperamos detectar un descenso
    // de cuatro dias consecutivos.

    @Test
    fun detectaDescensoSostenido() {

        val datos =
            mutableListOf<RegistroParaIA>()

        // Historial estable.

        for (dia in 0 until 21) {

            datos.add(
                registro(dia, 5, 9)
            )
        }

        // Descenso emocional.

        for (dia in 21 until 25) {

            datos.add(
                registro(dia, 2, 3)
            )
        }

        val resultado =
            MotorAnomaliasIA.analizar(datos)

        assertTrue(
            resultado.datosSuficientes
        )

        assertEquals(
            25,
            resultado.diasAnalizados
        )

        assertEquals(
            1,
            resultado.anomalias.size
        )

        val anomalia =
            resultado.anomalias.first()

        assertEquals(
            TipoCambioIA.DESCENSO,
            anomalia.tipo
        )

        assertEquals(
            4,
            anomalia.diasConsecutivos
        )

        assertEquals(
            fecha(21),
            anomalia.fechaInicio
        )

        assertEquals(
            fecha(24),
            anomalia.fechaFin
        )

        // El promedio observado debe
        // quedar por debajo del historial.

        assertTrue(
            anomalia.diferenciaAnimo < 0.0
        )

        assertTrue(
            anomalia.diferenciaEnergia < 0.0
        )
    }

    // ========================================
    // PRUEBA 6
    // NO CONFUNDIR CAMBIO AISLADO
    // CON CAMBIO SOSTENIDO
    // ========================================
    //
    // Historial estable durante 21 dias.
    //
    // Despues:
    //
    // 2 dias con descenso.
    // 1 dia de regreso al estado habitual.
    //
    // No hay tres dias consecutivos
    // de descenso.
    //
    // Por lo tanto NO debe generar
    // una anomalia sostenida.

    @Test
    fun dosDiasDeCambioNoSonSostenidos() {

        val datos =
            mutableListOf<RegistroParaIA>()

        for (dia in 0 until 21) {

            datos.add(
                registro(dia, 5, 9)
            )
        }

        datos.add(registro(21, 2, 3))
        datos.add(registro(22, 2, 3))

        // Regreso al estado habitual.

        datos.add(registro(23, 5, 9))

        val resultado =
            MotorAnomaliasIA.analizar(datos)

        assertTrue(
            resultado.datosSuficientes
        )

        assertTrue(
            resultado.diasConCambioNotable >= 2
        )

        assertTrue(
            resultado.anomalias.isEmpty()
        )
    }

    // ========================================
    // PRUEBA 7
    // LOS DIAS DEBEN SER CONSECUTIVOS
    // ========================================
    //
    // Tenemos cambios en:
    //
    // Dia 22
    // Dia 23
    //
    // Falta el dia 24.
    //
    // Hay otro cambio en el dia 25.
    //
    // Aunque tenemos tres registros
    // con descenso, NO forman una
    // racha de tres dias consecutivos.

    @Test
    fun fechasSeparadasNoFormanRacha() {

        val datos =
            mutableListOf<RegistroParaIA>()

        for (dia in 0 until 21) {

            datos.add(
                registro(dia, 5, 9)
            )
        }

        datos.add(registro(21, 2, 3))
        datos.add(registro(22, 2, 3))

        // No agregamos el dia 23.

        datos.add(registro(24, 2, 3))

        val resultado =
            MotorAnomaliasIA.analizar(datos)

        assertTrue(
            resultado.datosSuficientes
        )

        assertTrue(
            resultado.anomalias.isEmpty()
        )
    }

    // ========================================
    // PRUEBA 8
    // HISTORIAL SIN CAMBIOS NOTABLES
    // ========================================
    //
    // Creamos 30 dias iguales.
    //
    // Animo 4, energia 7.
    //
    // El motor no deberia inventar
    // cambios donde no existen.

    @Test
    fun historialEstableNoGeneraAnomalias() {

        val datos = (0 until 30).map { dia ->

            registro(
                dia = dia,
                animo = 4,
                energia = 7
            )
        }

        val resultado =
            MotorAnomaliasIA.analizar(datos)

        assertTrue(
            resultado.datosSuficientes
        )

        assertEquals(
            0,
            resultado.diasConCambioNotable
        )

        assertTrue(
            resultado.anomalias.isEmpty()
        )
    }

    // ========================================
    // PRUEBA 9
    // RECALCULAR CON REGISTROS NUEVOS
    // ========================================
    //
    // Primero analizamos 21 dias estables.
    //
    // Despues agregamos tres registros
    // con un descenso notable.
    //
    // Al volver a ejecutar el motor,
    // el resultado tiene que cambiar.
    //
    // Esto comprueba que el calculo
    // no depende de una lista fija.

    @Test
    fun anomaliasSeActualizanConNuevosDatos() {

        val historial = (0 until 21).map { dia ->

            registro(
                dia = dia,
                animo = 5,
                energia = 9
            )
        }

        // Primer analisis.

        val resultadoInicial =
            MotorAnomaliasIA.analizar(
                historial
            )

        assertEquals(
            21,
            resultadoInicial.diasAnalizados
        )

        assertTrue(
            resultadoInicial.anomalias.isEmpty()
        )

        // Agregamos tres dias nuevos.

        val registrosNuevos = listOf(
            registro(21, 2, 3),
            registro(22, 2, 3),
            registro(23, 2, 3)
        )

        val historialActualizado =
            historial + registrosNuevos

        // Segundo analisis.

        val resultadoActualizado =
            MotorAnomaliasIA.analizar(
                historialActualizado
            )

        assertEquals(
            24,
            resultadoActualizado.diasAnalizados
        )

        assertEquals(
            1,
            resultadoActualizado.anomalias.size
        )

        val anomalia =
            resultadoActualizado.anomalias.first()

        assertEquals(
            3,
            anomalia.diasConsecutivos
        )

        assertEquals(
            TipoCambioIA.DESCENSO,
            anomalia.tipo
        )
    }
}
