
package com.example.pp1jd.data.local

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

import androidx.sqlite.db.SupportSQLiteDatabase

// ============================================
// BASE DE DATOS PRINCIPAL DE MOODWAVE
// ============================================
//
// VERSION 1:
// Base de datos original de registros.
//
// VERSION 2:
// Se agrego la columna esSintetico
// para identificar datos de prueba.
//
// VERSION 3:
// Se agrega una tabla independiente
// para los pasos diarios obtenidos
// desde Health Connect.
//
// IMPORTANTE:
//
// Conservamos los registros anteriores.
//
// No eliminamos informacion.
//
// Los registros emocionales y los pasos
// se relacionaran mediante su fecha.
//
// No utilizamos migraciones destructivas.

// ============================================
// CONFIGURACION DE ROOM
// ============================================

@Database(
    entities = [

        // Tabla de registros emocionales.
        RegistroEntity::class,

        // Nueva tabla de pasos diarios.
        PasosDiariosEntity::class
    ],

    version = 3,

    exportSchema = false
)

abstract class MoodWaveDatabase : RoomDatabase() {

    // ========================================
    // DAO DE REGISTROS EMOCIONALES
    // ========================================
    //
    // Conservamos el acceso original.
    //
    // Lo utilizan:
    //
    // - Diario emocional.
    // - Historial.
    // - Calendario.
    // - Estadisticas.
    // - Inteligencia artificial.

    abstract fun registroDao(): RegistroDao

    // ========================================
    // DAO DE PASOS DIARIOS
    // ========================================
    //
    // NUEVO.
    //
    // Permite guardar y consultar
    // los pasos obtenidos mediante
    // Health Connect.
    //
    // Utiliza una tabla independiente
    // llamada pasos_diarios.

    abstract fun pasosDiariosDao(): PasosDiariosDao

    companion object {

        // ====================================
        // INSTANCIA UNICA DE ROOM
        // ====================================
        //
        // Evita crear varias conexiones
        // a la misma base de datos.

        @Volatile
        private var INSTANCE: MoodWaveDatabase? = null

        // ====================================
        // MIGRACION DE VERSION 1 A VERSION 2
        // ====================================
        //
        // CONSERVAMOS LA MIGRACION ORIGINAL.
        //
        // Agrega la columna esSintetico.
        //
        // 0 = registro personal
        // 1 = registro generado
        //
        // Los datos existentes se conservan.

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    // ========================
                    // AGREGAR COLUMNA NUEVA
                    // ========================
                    //
                    // Los registros antiguos
                    // quedan inicialmente
                    // como personales.

                    db.execSQL(
                        """
                        ALTER TABLE registros
                        ADD COLUMN esSintetico
                        INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    // ========================
                    // IDENTIFICAR DATOS DE PRUEBA
                    // ========================
                    //
                    // El generador original
                    // guardaba una marca en
                    // el texto de reflexion.
                    //
                    // La usamos para conservar
                    // la identificacion de
                    // registros sinteticos.

                    db.execSQL(
                        """
                        UPDATE registros
                        SET esSintetico = 1
                        WHERE reflexion LIKE '[DATOS DE PRUEBA]%'
                        """.trimIndent()
                    )
                }
            }

        // ====================================
        // MIGRACION DE VERSION 2 A VERSION 3
        // ====================================
        //
        // NUEVA MIGRACION.
        //
        // Agrega la tabla pasos_diarios.
        //
        // Esta tabla almacenara:
        //
        // fechaIso:
        // Fecha del registro yyyy-MM-dd.
        //
        // pasos:
        // Cantidad total de pasos.
        //
        // actualizadoEn:
        // Momento de la sincronizacion.
        //
        // IMPORTANTE:
        //
        // CREATE TABLE no modifica
        // los registros emocionales.
        //
        // La tabla registros permanece
        // exactamente como estaba.

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    // ========================
                    // CREAR TABLA DE PASOS
                    // ========================
                    //
                    // Usamos los mismos
                    // nombres y tipos que
                    // PasosDiariosEntity.
                    //
                    // fechaIso es la clave
                    // primaria porque solo
                    // guardaremos un total
                    // de pasos por cada dia.

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `pasos_diarios` (
                            `fechaIso` TEXT NOT NULL,
                            `pasos` INTEGER NOT NULL,
                            `actualizadoEn` INTEGER NOT NULL,
                            PRIMARY KEY(`fechaIso`)
                        )
                        """.trimIndent()
                    )
                }
            }

        // ====================================
        // OBTENER INSTANCIA DE ROOM
        // ====================================
        //
        // Devuelve la misma instancia
        // de base de datos durante
        // la ejecucion de MoodWave.
        //
        // Si la base de datos esta en
        // version 1, Room ejecutara:
        //
        // 1 -> 2 -> 3
        //
        // Si esta en version 2:
        //
        // 2 -> 3
        //
        // Si ya esta en version 3,
        // la abre normalmente.
        //
        // No necesitamos borrar datos.

        fun obtenerInstancia(
            context: Context
        ): MoodWaveDatabase {

            return INSTANCE ?: synchronized(this) {

                val instancia =
                    Room.databaseBuilder(

                        context.applicationContext,

                        MoodWaveDatabase::class.java,

                        "moodwave_database"
                    )

                        // ====================
                        // REGISTRAR MIGRACIONES
                        // ====================
                        //
                        // Ambas migraciones
                        // son necesarias para
                        // conservar datos de
                        // versiones anteriores.

                        .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3
                        )

                        // ====================
                        // CONSTRUIR LA BASE
                        // ====================

                        .build()

                // ============================
                // GUARDAR INSTANCIA
                // ============================

                INSTANCE = instancia

                instancia
            }
        }
    }
}
