
package com.example.pp1jd.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// ============================================
// ENTIDAD DE PASOS DIARIOS
// ============================================
//
// Esta clase representara una nueva tabla
// de nuestra base de datos Room.
//
// Guardaremos los pasos obtenidos desde
// Health Connect, organizados por fecha.
//
// Cada dia tendra un unico registro.
//
// IMPORTANTE:
//
// No reemplaza RegistroEntity.
//
// No modifica los registros emocionales.
//
// No distingue entre registros sinteticos
// y reales porque los pasos se relacionan
// directamente mediante la fecha.
//
// Si no existen datos de pasos, no se
// debe inventar el valor cero.

// ============================================
// TABLA: pasos_diarios
// ============================================

@Entity(
    tableName = "pasos_diarios"
)
data class PasosDiariosEntity(

    // ========================================
    // FECHA DEL REGISTRO
    // ========================================
    //
    // Ejemplo:
    //
    // "2026-11-04"
    //
    // Usamos el mismo formato ISO que
    // los registros emocionales.
    //
    // PrimaryKey significa que solo
    // puede existir una fila por fecha.

    @PrimaryKey
    val fechaIso: String,

    // ========================================
    // CANTIDAD TOTAL DE PASOS
    // ========================================
    //
    // Valor real consultado mediante
    // Health Connect.
    //
    // Ejemplo:
    //
    // 57L
    //
    // Si el usuario camina mas,
    // actualizaremos el total del dia.
    //
    // Nunca sumaremos el total nuevo
    // sobre el total anterior, porque
    // Health Connect ya devuelve el
    // acumulado del periodo solicitado.

    val pasos: Long,

    // ========================================
    // MOMENTO DE LA SINCRONIZACION
    // ========================================
    //
    // Guarda cuando se obtuvo por
    // ultima vez el total de pasos.
    //
    // Se representa como milisegundos
    // desde la epoca Unix.
    //
    // Esto permitira saber si los pasos
    // guardados deben actualizarse.

    val actualizadoEn: Long
)
