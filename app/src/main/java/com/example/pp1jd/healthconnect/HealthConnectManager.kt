
package com.example.pp1jd.healthconnect

import android.content.Context

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter

import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

// ============================================
// ESTADO DE HEALTH CONNECT
// ============================================
//
// Permite conocer si el telefono
// puede utilizar Health Connect.

enum class EstadoHealthConnect {

    DISPONIBLE,

    REQUIERE_INSTALACION,

    NO_DISPONIBLE
}

// ============================================
// RESULTADO DE PASOS POR DIA
// ============================================
//
// fechaIso:
// Fecha del dia en formato yyyy-MM-dd.
//
// pasos:
// Total obtenido de Health Connect.
//
// Si pasos es null significa que
// no hay informacion disponible.
// No significa necesariamente cero pasos.

data class PasosDiaHealthConnect(
    val fechaIso: String,
    val pasos: Long?
)

// ============================================
// MOTOR DE HEALTH CONNECT
// ============================================
//
// Este motor se encargara de:
//
// 1. Comprobar disponibilidad.
// 2. Verificar permisos.
// 3. Leer pasos diarios.
// 4. Leer varios dias.
// 5. Preparar resultados para MoodWave.
//
// No escribe informacion en Health Connect.
// No modifica los registros de Room.

class HealthConnectManager(
    context: Context
) {

    // ========================================
    // CONTEXTO DE LA APLICACION
    // ========================================

    private val contexto =
        context.applicationContext

    // ========================================
    // PERMISO NECESARIO
    // ========================================
    //
    // Solicitamos exclusivamente
    // lectura de pasos.

    val permisoLecturaPasos: String =
        HealthPermission.getReadPermission(
            StepsRecord::class
        )

    val permisosNecesarios: Set<String> =
        setOf(
            permisoLecturaPasos
        )

    // ========================================
    // COMPROBAR DISPONIBILIDAD
    // ========================================
    //
    // Health Connect puede encontrarse:
    //
    // - Disponible.
    // - Sin instalar o desactualizado.
    // - No compatible con el telefono.
    //
    // No intentamos leer informacion
    // cuando el servicio no funciona.

    fun obtenerEstado(): EstadoHealthConnect {

        val estado =
            HealthConnectClient.getSdkStatus(
                contexto
            )

        return when (estado) {

            HealthConnectClient.SDK_AVAILABLE -> {

                EstadoHealthConnect.DISPONIBLE
            }

            HealthConnectClient
                .SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {

                EstadoHealthConnect.REQUIERE_INSTALACION
            }

            else -> {

                EstadoHealthConnect.NO_DISPONIBLE
            }
        }
    }

    // ========================================
    // OBTENER CLIENTE DE HEALTH CONNECT
    // ========================================
    //
    // El cliente nos permite acceder
    // a los datos autorizados.
    //
    // Solo lo creamos si el servicio
    // esta disponible.

    private fun obtenerCliente(): HealthConnectClient {

        if (
            obtenerEstado() !=
            EstadoHealthConnect.DISPONIBLE
        ) {

            throw IllegalStateException(
                "Health Connect no está disponible " +
                        "en este dispositivo."
            )
        }

        return HealthConnectClient.getOrCreate(
            contexto
        )
    }

    // ========================================
    // COMPROBAR PERMISO DE LECTURA
    // ========================================
    //
    // El usuario puede retirar permisos
    // desde los ajustes del telefono.
    //
    // Por eso comprobamos el permiso
    // antes de consultar los pasos.

    suspend fun tienePermisoPasos(): Boolean {

        if (
            obtenerEstado() !=
            EstadoHealthConnect.DISPONIBLE
        ) {

            return false
        }

        val cliente = obtenerCliente()

        val permisosConcedidos =
            cliente.permissionController
                .getGrantedPermissions()

        return permisosConcedidos.contains(
            permisoLecturaPasos
        )
    }

    // ========================================
    // COMPROBAR ACCESO ANTES DE LEER
    // ========================================

    private suspend fun clienteAutorizado():
            HealthConnectClient {

        val cliente = obtenerCliente()

        val permisos =
            cliente.permissionController
                .getGrantedPermissions()

        if (
            !permisos.contains(
                permisoLecturaPasos
            )
        ) {

            throw SecurityException(
                "MoodWave necesita autorización " +
                        "para leer los pasos de " +
                        "Health Connect."
            )
        }

        return cliente
    }

    // ========================================
    // LEER PASOS DE UN DIA ESPECIFICO
    // ========================================
    //
    // Ejemplo:
    //
    // leerPasosDelDia(
    //     LocalDate.now()
    // )
    //
    // Devuelve:
    //
    // 8450L -> se encontraron pasos.
    //
    // null -> no hay datos disponibles.
    //
    // Utilizamos la zona horaria local
    // para respetar el inicio y el
    // final del dia del usuario.

    suspend fun leerPasosDelDia(
        fecha: LocalDate = LocalDate.now()
    ): Long? {

        val cliente =
            clienteAutorizado()

        val zonaHoraria =
            ZoneId.systemDefault()

        // Inicio del dia seleccionado.

        val inicio =
            fecha.atStartOfDay(
                zonaHoraria
            ).toInstant()

        // Inicio del dia siguiente.
        //
        // Este sera el limite superior
        // de nuestra consulta.

        val fin =
            fecha.plusDays(1)
                .atStartOfDay(
                    zonaHoraria
                )
                .toInstant()

        // ====================================
        // CONSULTAR HEALTH CONNECT
        // ====================================

        val respuesta =
            cliente.aggregate(

                AggregateRequest(

                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL
                    ),

                    timeRangeFilter =
                        TimeRangeFilter.between(
                            inicio,
                            fin
                        )
                )
            )

        // ====================================
        // OBTENER RESULTADO
        // ====================================
        //
        // La API permite que el valor
        // sea null si no existen datos.
        //
        // Evitamos convertir null a cero.

        return respuesta[
            StepsRecord.COUNT_TOTAL
        ]
    }

    // ========================================
    // LEER PASOS DE VARIOS DIAS
    // ========================================
    //
    // Esta funcion acepta un numero
    // variable de dias.
    //
    // Ejemplos:
    //
    // leerPasosUltimosDias(7)
    // leerPasosUltimosDias(30)
    // leerPasosUltimosDias(90)
    //
    // No depende de los 90 registros
    // iniciales de MoodWave.
    //
    // IMPORTANTE:
    // Para consultar informacion mas
    // antigua que el limite permitido
    // por Health Connect, necesitaremos
    // autorizacion adicional de historial.

    suspend fun leerPasosUltimosDias(
        cantidadDias: Int = 30
    ): List<PasosDiaHealthConnect> {

        require(cantidadDias in 1..365) {

            "Selecciona entre 1 y 365 días."
        }

        val cliente =
            clienteAutorizado()

        // ====================================
        // CALCULAR RANGO DE FECHAS
        // ====================================

        val hoy = LocalDate.now()

        val primerDia =
            hoy.minusDays(
                cantidadDias.toLong() - 1L
            )

        // Limite superior exclusivo.

        val finExclusivo =
            hoy.plusDays(1)

        // ====================================
        // AGRUPAR PASOS POR DIA
        // ====================================
        //
        // aggregateGroupByPeriod permite
        // obtener los totales diarios
        // en una sola consulta.
        //
        // Utilizamos LocalDateTime y
        // Period.ofDays(1) para que
        // los grupos correspondan a
        // dias del calendario.

        val respuesta =
            cliente.aggregateGroupByPeriod(

                AggregateGroupByPeriodRequest(

                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL
                    ),

                    timeRangeFilter =
                        TimeRangeFilter.between(
                            primerDia.atStartOfDay(),
                            finExclusivo.atStartOfDay()
                        ),

                    timeRangeSlicer =
                        Period.ofDays(1)
                )
            )

        // ====================================
        // CONVERTIR A MAPA DE FECHAS
        // ====================================
        //
        // Cada resultado tiene su
        // fecha y cantidad de pasos.

        val pasosPorFecha =
            respuesta.associate { resultado ->

                val fecha =
                    resultado.startTime
                        .toLocalDate()
                        .toString()

                val pasos =
                    resultado.result[
                        StepsRecord.COUNT_TOTAL
                    ]

                fecha to pasos
            }

        // ====================================
        // CONSTRUIR TODOS LOS DIAS
        // ====================================
        //
        // Aunque no existan datos de pasos
        // para alguna fecha, incluimos
        // ese dia con valor null.
        //
        // Asi las estadisticas pueden
        // distinguir un dia sin datos
        // de un dia con cero pasos.

        return (0 until cantidadDias).map {
                indice ->

            val fecha =
                primerDia.plusDays(
                    indice.toLong()
                ).toString()

            PasosDiaHealthConnect(

                fechaIso = fecha,

                pasos =
                    pasosPorFecha[fecha]
            )
        }
    }
}
