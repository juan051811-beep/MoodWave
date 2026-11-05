
package com.example.pp1jd.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray

// ============================================
// ENTIDAD PRINCIPAL DE MOODWAVE
// ============================================
// Representa cada registro emocional
// almacenado permanentemente en Room.

@Entity(
    tableName = "registros",
    indices = [Index(value = ["fecha"])]
)
@TypeConverters(ActividadesConverter::class)
data class RegistroEntity(

    // Identificador unico
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Fecha real del registro: yyyy-MM-dd
    val fecha: String,

    // Momento de creacion
    val creadoEn: Long = System.currentTimeMillis(),

    // Estado emocional
    val animo: String,

    // Escala emocional de 1 a 5
    val valorAnimo: Int,

    // Emoji correspondiente
    val emojiAnimo: String,

    // Nivel de energia de 1 a 10
    val energia: Int,

    // Actividades realizadas
    val actividades: List<String>,

    // Reflexion del dia
    val reflexion: String = "",

    // Horas de sueño manuales
    val horasSueno: Double? = null,

    // Pasos de Health Connect
    val pasos: Long? = null,

    // ========================================
    // NUEVO CAMPO: ORIGEN DEL REGISTRO
    // ========================================
    // false = registro personal real
    // true  = caso sintetico de demostracion
    //
    // Los dos se guardan permanentemente.
    // Este campo permite diferenciarlos,
    // filtrarlos y analizarlos correctamente.

    @ColumnInfo(defaultValue = "0")
    val esSintetico: Boolean = false
)

// ============================================
// CONVERSOR DE ACTIVIDADES
// ============================================

class ActividadesConverter {

    @TypeConverter
    fun listaAJson(
        actividades: List<String>
    ): String {

        return JSONArray(actividades).toString()
    }

    @TypeConverter
    fun jsonALista(
        json: String
    ): List<String> {

        val resultado = mutableListOf<String>()
        val arreglo = JSONArray(json)

        for (i in 0 until arreglo.length()) {
            resultado.add(arreglo.getString(i))
        }

        return resultado
    }
}
