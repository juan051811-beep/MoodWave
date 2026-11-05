
package com.example.pp1jd.healthconnect

import android.content.Context

import com.example.pp1jd.data.local.MoodWaveDatabase
import com.example.pp1jd.data.local.PasosDiariosEntity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import java.time.LocalDate

// ============================================
// RESULTADO DE LA SINCRONIZACION
// ============================================
//
// Esta clase resume lo que sucedio
// al sincronizar los pasos.
//
// diasConsultados:
// Cantidad de fechas consultadas.
//
// diasGuardados:
// Cantidad de fechas con datos validos.
//
// pasosHoy:
// Total de pasos de hoy, si existe.
//
// sincronizadoEn:
// Momento en que se hizo la consulta.

data class ResultadoSincronizacionPasos(

    val diasConsultados: Int,

    val diasGuardados: Int,

    val pasosHoy: Long?,

    val sincronizadoEn: Long
)

// ============================================
// SINCRONIZADOR DE PASOS
// ============================================
//
// Conecta Health Connect con Room.
//
// Su funcionamiento sera:
//
// 1. Comprobar Health Connect.
//
// 2. Verificar permisos.
//
// 3. Consultar pasos reales.
//
// 4. Ignorar dias sin informacion.
//
// 5. Guardar o actualizar en Room.
//
// 6. Devolver el resultado.
//
// IMPORTANTE:
//
// No modifica RegistroEntity.
//
// No altera las emociones.
//
// No modifica los motores de IA.
//
// No genera pasos artificiales.

class SincronizadorPasos(
    context: Context
) {

    // ========================================
    // CONTEXTO DE LA APLICACION
    // ========================================

    private val contexto =
        context.applicationContext

    // ========================================
    // MOTOR DE HEALTH CONNECT
    // ========================================

    private val healthConnectManager =
        HealthConnectManager(contexto)

    // ========================================
    // ACCESO A ROOM
    // ========================================

    private val pasosDao =
        MoodWaveDatabase
            .obtenerInstancia(contexto)
            .pasosDiariosDao()

    // ========================================
    // SINCRONIZAR ULTIMOS DIAS
    // ========================================
    //
    // Ejemplos:
    //
    // sincronizarUltimosDias(7)
    //
    // sincronizarUltimosDias(30)
    //
    // El valor predeterminado sera
    // de 7 dias.
    //
    // Por ahora limitamos a 30 dias
    // para trabajar con el acceso
    // historico normal de Health Connect.
    //
    // Mas adelante podremos ampliar
    // el historial con otro permiso.

    suspend fun sincronizarUltimosDias(
        cantidadDias: Int = 7
    ): ResultadoSincronizacionPasos =
        withContext(Dispatchers.IO) {

            // ================================
            // VALIDAR PERIODO
            // ================================

            require(
                cantidadDias in 1..30
            ) {
                "El periodo debe estar " +
                        "entre 1 y 30 días."
            }

            // ================================
            // COMPROBAR DISPONIBILIDAD
            // ================================

            if (
                healthConnectManager.obtenerEstado() !=
                EstadoHealthConnect.DISPONIBLE
            ) {

                throw IllegalStateException(
                    "Health Connect no está disponible."
                )
            }

            // ================================
            // COMPROBAR AUTORIZACION
            // ================================

            if (
                !healthConnectManager.tienePermisoPasos()
            ) {

                throw SecurityException(
                    "MoodWave necesita permiso " +
                            "para leer los pasos."
                )
            }

            // ================================
            // LEER DATOS DE HEALTH CONNECT
            // ================================
            //
            // Esta funcion ya la creamos
            // en HealthConnectManager.kt.
            //
            // Devuelve los pasos agrupados
            // por fecha.

            val datosHealth =
                healthConnectManager
                    .leerPasosUltimosDias(
                        cantidadDias
                    )

            // ================================
            // FECHA DE SINCRONIZACION
            // ================================

            val momentoActual =
                System.currentTimeMillis()

            // ================================
            // CONVERTIR DATOS A ENTIDADES
            // ================================
            //
            // mapNotNull descarta los dias
            // sin informacion.
            //
            // Si Health Connect devuelve
            // null, no guardamos cero.
            //
            // Tambien descartamos valores
            // negativos, que no tendrian
            // sentido para los pasos.
            //
            // Cada entidad guarda:
            //
            // - fechaIso
            // - pasos
            // - actualizadoEn

            val registrosParaGuardar =
                datosHealth.mapNotNull { dia ->

                    val totalPasos = dia.pasos

                    if (
                        totalPasos == null ||
                        totalPasos < 0L
                    ) {

                        null

                    } else {

                        PasosDiariosEntity(

                            fechaIso = dia.fechaIso,

                            pasos = totalPasos,

                            actualizadoEn =
                                momentoActual
                        )
                    }
                }

            // ================================
            // GUARDAR EN ROOM
            // ================================
            //
            // Usamos @Upsert del DAO.
            //
            // Si el dia no existe:
            // Se inserta.
            //
            // Si ya existe:
            // Se actualiza.
            //
            // NO sumamos los totales.
            //
            // Si no hay datos nuevos,
            // no insertamos registros.
            //
            // Los valores anteriores
            // permanecen almacenados
            // hasta que se actualicen
            // o el usuario los elimine.

            if (
                registrosParaGuardar.isNotEmpty()
            ) {

                pasosDao
                    .guardarOActualizarVarios(
                        registrosParaGuardar
                    )
            }

            // ================================
            // CONSULTAR PASOS DE HOY
            // ================================

            val fechaHoy =
                LocalDate.now().toString()

            val pasosHoy =
                datosHealth.firstOrNull {

                    it.fechaIso == fechaHoy

                }?.pasos

            // ================================
            // RESULTADO FINAL
            // ================================

            ResultadoSincronizacionPasos(

                diasConsultados =
                    datosHealth.size,

                diasGuardados =
                    registrosParaGuardar.size,

                pasosHoy = pasosHoy,

                sincronizadoEn =
                    momentoActual
            )
        }

    // ========================================
    // OBSERVAR PASOS GUARDADOS EN ROOM
    // ========================================
    //
    // Devuelve un Flow que se actualiza
    // cuando cambia la tabla.
    //
    // Lo utilizaremos despues para
    // mostrar estadisticas dinamicas.

    fun observarPasosGuardados():
            Flow<List<PasosDiariosEntity>> {

        return pasosDao.observarTodos()
    }

    // ========================================
    // OBTENER PASOS DE UNA FECHA
    // ========================================
    //
    // Consulta exclusivamente Room.
    //
    // No necesita realizar otra lectura
    // de Health Connect.
    //
    // Si no hay registro, devuelve null.

    suspend fun obtenerPasosGuardados(
        fechaIso: String
    ): PasosDiariosEntity? =
        withContext(Dispatchers.IO) {

            pasosDao.obtenerPorFecha(
                fechaIso
            )
        }

    // ========================================
    // ELIMINAR PASOS LOCALES
    // ========================================
    //
    // Esta funcion solo borra la copia
    // almacenada en MoodWave.
    //
    // No elimina los datos originales
    // de Health Connect.
    //
    // Posteriormente la conectaremos
    // con la seccion de privacidad.

    suspend fun eliminarPasosLocales() =
        withContext(Dispatchers.IO) {

            pasosDao.eliminarTodos()
        }
}
