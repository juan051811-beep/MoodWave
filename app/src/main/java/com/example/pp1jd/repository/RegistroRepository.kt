
package com.example.pp1jd.data.repository

import com.example.pp1jd.data.local.RegistroDao
import com.example.pp1jd.data.local.RegistroEntity
import kotlinx.coroutines.flow.Flow

// ============================================
// REPOSITORIO PRINCIPAL DE MOODWAVE
// ============================================
// Comunica la base de datos Room con
// los ViewModels de nuestra aplicacion.

class RegistroRepository(
    private val registroDao: RegistroDao
) {

    // ========================================
    // OBTENER TODOS LOS REGISTROS
    // ========================================
    // Flow permite observar los cambios
    // que ocurren en la base de datos.

    val todosLosRegistros: Flow<List<RegistroEntity>> =
        registroDao.obtenerTodosLosRegistros()

    // ========================================
    // GUARDAR UN REGISTRO
    // ========================================

    suspend fun insertarRegistro(
        registro: RegistroEntity
    ): Long {
        return registroDao.insertarRegistro(registro)
    }

    // ========================================
    // ACTUALIZAR UN REGISTRO
    // ========================================

    suspend fun actualizarRegistro(
        registro: RegistroEntity
    ) {
        registroDao.actualizarRegistro(registro)
    }

    // ========================================
    // ELIMINAR UN REGISTRO
    // ========================================

    suspend fun eliminarRegistro(
        registro: RegistroEntity
    ) {
        registroDao.eliminarRegistro(registro)
    }

    // ========================================
    // BUSCAR REGISTRO POR ID
    // ========================================

    suspend fun obtenerRegistroPorId(
        id: Long
    ): RegistroEntity? {
        return registroDao.obtenerRegistroPorId(id)
    }

    // ========================================
    // REGISTROS DE UNA FECHA
    // ========================================

    fun obtenerRegistrosPorFecha(
        fecha: String
    ): Flow<List<RegistroEntity>> {
        return registroDao.obtenerRegistrosPorFecha(fecha)
    }

    // ========================================
    // REGISTROS ENTRE DOS FECHAS
    // ========================================
    // Servira para calcular estadisticas
    // semanales, mensuales y tendencias.

    fun obtenerRegistrosPorRango(
        inicio: String,
        fin: String
    ): Flow<List<RegistroEntity>> {
        return registroDao.obtenerRegistrosPorRango(
            inicio,
            fin
        )
    }

    // ========================================
    // CONTAR TODOS LOS REGISTROS
    // ========================================

    fun contarRegistros(): Flow<Int> {
        return registroDao.contarRegistros()
    }

    // ========================================
    // CONTAR DIAS CON REGISTROS
    // ========================================

    fun contarDiasRegistrados(): Flow<Int> {
        return registroDao.contarDiasRegistrados()
    }

    // ========================================
    // ELIMINAR TODOS LOS DATOS
    // ========================================
    // Importante para el requisito de
    // privacidad solicitado por el profesor.

    suspend fun eliminarTodosLosRegistros() {
        registroDao.eliminarTodosLosRegistros()
    }
}
