
package com.example.pp1jd.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

import kotlinx.coroutines.flow.Flow

// ============================================
// DAO DE PASOS DIARIOS
// ============================================
//
// DAO significa Data Access Object.
//
// Es el encargado de comunicar
// nuestra aplicacion con la tabla
// pasos_diarios de Room.
//
// Sus funciones permiten:
//
// 1. Guardar pasos reales.
// 2. Actualizar pasos existentes.
// 3. Consultar pasos por fecha.
// 4. Consultar periodos completos.
// 5. Observar cambios en tiempo real.
// 6. Eliminar datos cuando sea necesario.
//
// IMPORTANTE:
//
// Este DAO no modifica la tabla
// de registros emocionales.
//
// La relacion entre pasos y emociones
// se realizara mediante fechaIso.

@Dao
interface PasosDiariosDao {

    // ========================================
    // 1. GUARDAR O ACTUALIZAR UN DIA
    // ========================================
    //
    // @Upsert realiza dos operaciones:
    //
    // Si la fecha no existe:
    // Guarda un nuevo registro.
    //
    // Si la fecha ya existe:
    // Actualiza el registro existente.
    //
    // Ejemplo:
    //
    // Primera sincronizacion:
    // 57 pasos.
    //
    // Segunda sincronizacion:
    // 120 pasos.
    //
    // Resultado guardado:
    // 120 pasos, no 177.
    //
    // Asi evitamos duplicar pasos.

    @Upsert
    suspend fun guardarOActualizar(
        registro: PasosDiariosEntity
    )

    // ========================================
    // 2. GUARDAR VARIOS DIAS
    // ========================================
    //
    // Permite sincronizar varios dias
    // obtenidos desde Health Connect.
    //
    // Ejemplos:
    //
    // Ultimos 7 dias.
    // Ultimos 30 dias.
    // Ultimos 90 dias.
    //
    // No depende de los registros
    // sinteticos iniciales.

    @Upsert
    suspend fun guardarOActualizarVarios(
        registros: List<PasosDiariosEntity>
    )

    // ========================================
    // 3. CONSULTAR PASOS DE UNA FECHA
    // ========================================
    //
    // Recibe una fecha ISO:
    //
    // "2026-11-04"
    //
    // Devuelve el registro si existe.
    //
    // Si no existe devuelve null.
    //
    // No confundimos falta de datos
    // con cero pasos.

    @Query(
        """
        SELECT *
        FROM pasos_diarios
        WHERE fechaIso = :fechaIso
        LIMIT 1
        """
    )
    suspend fun obtenerPorFecha(
        fechaIso: String
    ): PasosDiariosEntity?

    // ========================================
    // 4. CONSULTAR UN RANGO DE FECHAS
    // ========================================
    //
    // Ejemplo:
    //
    // desde = "2026-11-01"
    // hasta = "2026-11-30"
    //
    // Solo devuelve los dias que
    // tienen pasos guardados.
    //
    // ORDER BY organiza los resultados
    // desde la fecha mas antigua.

    @Query(
        """
        SELECT *
        FROM pasos_diarios
        WHERE fechaIso BETWEEN :desde AND :hasta
        ORDER BY fechaIso ASC
        """
    )
    suspend fun obtenerEnRango(
        desde: String,
        hasta: String
    ): List<PasosDiariosEntity>

    // ========================================
    // 5. OBSERVAR PASOS EN TIEMPO REAL
    // ========================================
    //
    // Flow permite que Room avise
    // cuando cambian los datos.
    //
    // Por ejemplo:
    //
    // Antes: 57 pasos.
    // Despues: 120 pasos.
    //
    // Cuando sincronizamos los datos,
    // las pantallas que observan este
    // Flow pueden actualizarse.
    //
    // Esto sera muy importante para
    // nuestras estadisticas.

    @Query(
        """
        SELECT *
        FROM pasos_diarios
        WHERE fechaIso BETWEEN :desde AND :hasta
        ORDER BY fechaIso ASC
        """
    )
    fun observarEnRango(
        desde: String,
        hasta: String
    ): Flow<List<PasosDiariosEntity>>

    // ========================================
    // 6. OBSERVAR TODOS LOS PASOS
    // ========================================
    //
    // Permite observar todos los datos
    // de pasos guardados en Room.
    //
    // Se ordenan del dia mas reciente
    // al mas antiguo.
    //
    // Util para estadisticas generales
    // y futuras funciones de MoodWave.

    @Query(
        """
        SELECT *
        FROM pasos_diarios
        ORDER BY fechaIso DESC
        """
    )
    fun observarTodos():
            Flow<List<PasosDiariosEntity>>

    // ========================================
    // 7. ELIMINAR PASOS DE UNA FECHA
    // ========================================
    //
    // Permite borrar un dia especifico.
    //
    // No elimina el registro emocional
    // correspondiente.
    //
    // Esto ayudara a mantener el control
    // sobre la informacion almacenada.

    @Query(
        """
        DELETE FROM pasos_diarios
        WHERE fechaIso = :fechaIso
        """
    )
    suspend fun eliminarPorFecha(
        fechaIso: String
    )

    // ========================================
    // 8. ELIMINAR TODOS LOS PASOS
    // ========================================
    //
    // Permitira implementar una opcion
    // de privacidad para borrar
    // los pasos almacenados en MoodWave.
    //
    // No borra informacion de
    // Health Connect.
    //
    // No modifica los registros
    // emocionales existentes.

    @Query(
        """
        DELETE FROM pasos_diarios
        """
    )
    suspend fun eliminarTodos()
}
