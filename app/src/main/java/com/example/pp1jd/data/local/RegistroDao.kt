
package com.example.pp1jd.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ============================================
// DAO - BASE DE DATOS DE MOODWAVE
// ============================================

@Dao
interface RegistroDao {

    // GUARDAR UN REGISTRO
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRegistro(
        registro: RegistroEntity
    ): Long

    // ACTUALIZAR UN REGISTRO
    @Update
    suspend fun actualizarRegistro(
        registro: RegistroEntity
    )

    // ELIMINAR UN REGISTRO
    @Delete
    suspend fun eliminarRegistro(
        registro: RegistroEntity
    )

    // OBTENER TODOS LOS REGISTROS
    @Query(
        "SELECT * FROM registros " +
                "ORDER BY fecha DESC, creadoEn DESC"
    )
    fun obtenerTodosLosRegistros():
            Flow<List<RegistroEntity>>

    // BUSCAR UN REGISTRO POR ID
    @Query(
        "SELECT * FROM registros WHERE id = :id LIMIT 1"
    )
    suspend fun obtenerRegistroPorId(
        id: Long
    ): RegistroEntity?

    // OBTENER REGISTROS DE UNA FECHA
    @Query(
        "SELECT * FROM registros " +
                "WHERE fecha = :fecha " +
                "ORDER BY creadoEn DESC"
    )
    fun obtenerRegistrosPorFecha(
        fecha: String
    ): Flow<List<RegistroEntity>>

    // OBTENER REGISTROS ENTRE DOS FECHAS
    @Query(
        "SELECT * FROM registros " +
                "WHERE fecha BETWEEN :inicio AND :fin " +
                "ORDER BY fecha ASC, creadoEn ASC"
    )
    fun obtenerRegistrosPorRango(
        inicio: String,
        fin: String
    ): Flow<List<RegistroEntity>>

    // CONTAR REGISTROS
    @Query("SELECT COUNT(*) FROM registros")
    fun contarRegistros(): Flow<Int>

    // CONTAR DIAS REGISTRADOS
    @Query(
        "SELECT COUNT(DISTINCT fecha) FROM registros"
    )
    fun contarDiasRegistrados(): Flow<Int>

    // ELIMINAR TODOS LOS REGISTROS
    @Query("DELETE FROM registros")
    suspend fun eliminarTodosLosRegistros()
}
