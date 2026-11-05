package com.example.pp1jd.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction

import com.example.pp1jd.analytics.MotorAgrupamientoIA
import com.example.pp1jd.analytics.MotorAnomaliasIA
import com.example.pp1jd.analytics.RegistroParaIA
import com.example.pp1jd.analytics.ResultadoAgrupamientoIA
import com.example.pp1jd.analytics.ResultadoAnomaliasIA
import com.example.pp1jd.data.local.GeneradorDatosRealistas
import com.example.pp1jd.data.local.GeneradorDatosRealistasV2
import com.example.pp1jd.data.local.MoodWaveDatabase
import com.example.pp1jd.data.local.RegistroEntity
import com.example.pp1jd.data.repository.RegistroRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

// ============================================
// MODELOS UTILIZADOS POR LAS PANTALLAS
// ============================================

data class DiaryEntry(
    val id: Long,
    val date: String,
    val dateIso: String,
    val mood: String,
    val moodEmoji: String,
    val energy: Int,
    val activities: List<String>,
    val reflection: String,
    val hoursSleep: Double? = null,
    val steps: Long? = null,
    val isSynthetic: Boolean = false
)

data class EstadoIA(
    val calculando: Boolean = true,
    val agrupamiento: ResultadoAgrupamientoIA? = null,
    val anomalias: ResultadoAnomaliasIA? = null,
    val registrosProcesados: Int = 0,
    val error: String? = null
)

// ============================================
// VIEWMODEL PRINCIPAL DE MOODWAVE
// ============================================

class DiaryViewModel(
    application: Application
) : AndroidViewModel(application) {

    // Room: misma base de datos usada por toda MoodWave.
    private val database =
        MoodWaveDatabase.obtenerInstancia(application)

    private val repository =
        RegistroRepository(database.registroDao())

    // Guardamos que esta version de los datos de prueba
    // ya se importo. No usa una tabla adicional de Room.
    private val preferenciasGenerador =
        application.getSharedPreferences(
            "moodwave_generador_realista_v1",
            Context.MODE_PRIVATE
        )

    // Segunda etapa: los 210 registros adicionales tienen
    // su propia marca de importacion.
    private val preferenciasGeneradorV2 =
        application.getSharedPreferences(
            "moodwave_generador_realista_v2",
            Context.MODE_PRIVATE
        )

    // ========================================
    // ESTADO DEL FORMULARIO EXISTENTE
    // ========================================

    var selectedMood by mutableStateOf("Bien")
        private set

    var selectedMoodEmoji by mutableStateOf("😊")
        private set

    var energyLevel by mutableFloatStateOf(7f)
        private set

    val selectedActivities = mutableStateListOf<String>()

    var reflectionText by mutableStateOf("")
        private set

    var hoursSleepText by mutableStateOf("")
        private set

    var isSaving by mutableStateOf(false)
        private set

    var isGeneratingTestData by mutableStateOf(false)
        private set

    // Si algo falla en la carga automatica,
    // este texto puede consultarse desde la interfaz.
    var errorGeneracion by mutableStateOf<String?>(null)
        private set

    // Todos los registros permanecen juntos:
    // personales y sinteticos, anteriores y nuevos.
    val historyEntries = mutableStateListOf<DiaryEntry>()

    var estadoIA by mutableStateOf(EstadoIA())
        private set

    init {
        observarRegistros()

        // Primero comprobamos los 90 registros de la etapa 1.
        // Solo al terminar, importamos los 210 de la etapa 2.
        // De esta forma no existe una carrera entre imports.
        generarDatosRealistas(
            onSuccess = {
                generarDatosRealistasV2()
            }
        )
    }

    // ========================================
    // OBSERVAR ROOM Y RECALCULAR LA IA
    // ========================================

    private fun observarRegistros() {
        viewModelScope.launch {
            repository.todosLosRegistros.collectLatest { registros ->

                // Actualizar el historial de todas las pantallas.
                historyEntries.clear()
                historyEntries.addAll(
                    registros.map(::convertirADiaryEntry)
                )

                estadoIA = EstadoIA(
                    calculando = true,
                    registrosProcesados = registros.size
                )

                try {
                    // Mantener el analisis matematico fuera
                    // del hilo principal de Android.
                    val resultados = withContext(Dispatchers.Default) {
                        val datosIA = registros.mapNotNull { registro ->
                            if (
                                registro.valorAnimo in 1..5 &&
                                registro.energia in 1..10
                            ) {
                                RegistroParaIA(
                                    fechaIso = registro.fecha,
                                    animo = registro.valorAnimo,
                                    energia = registro.energia,
                                    horasSueno = registro.horasSueno,
                                    actividades = registro.actividades
                                )
                            } else {
                                null
                            }
                        }

                        Pair(
                            MotorAgrupamientoIA.analizar(datosIA),
                            MotorAnomaliasIA.analizar(datosIA)
                        )
                    }

                    estadoIA = EstadoIA(
                        calculando = false,
                        agrupamiento = resultados.first,
                        anomalias = resultados.second,
                        registrosProcesados = registros.size
                    )

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {
                    estadoIA = EstadoIA(
                        calculando = false,
                        registrosProcesados = registros.size,
                        error = "No fue posible analizar los registros: " +
                                (e.localizedMessage ?: "Error desconocido")
                    )
                }
            }
        }
    }

    // ========================================
    // CONVERTIR ROOM A DATOS DE LA INTERFAZ
    // ========================================

    private fun convertirADiaryEntry(registro: RegistroEntity): DiaryEntry {
        val fechaMostrada = try {
            val formatoBase = SimpleDateFormat(
                "yyyy-MM-dd", Locale.US
            ).apply { isLenient = false }

            val fecha = formatoBase.parse(registro.fecha)
            if (fecha == null) {
                registro.fecha
            } else {
                SimpleDateFormat(
                    "d MMM yyyy", Locale.forLanguageTag("es-MX")
                ).format(fecha)
            }
        } catch (e: Exception) {
            registro.fecha
        }

        return DiaryEntry(
            id = registro.id,
            date = fechaMostrada,
            dateIso = registro.fecha,
            mood = registro.animo,
            moodEmoji = registro.emojiAnimo,
            energy = registro.energia,
            activities = registro.actividades,
            reflection = registro.reflexion,
            hoursSleep = registro.horasSueno,
            steps = registro.pasos,
            isSynthetic = registro.esSintetico
        )
    }

    // ========================================
    // ACCIONES DEL FORMULARIO (SIN CAMBIOS)
    // ========================================

    fun updateMood(mood: String, emoji: String) {
        selectedMood = mood
        selectedMoodEmoji = emoji
    }

    fun updateEnergy(energy: Float) {
        energyLevel = energy.coerceIn(1f, 10f)
    }

    fun toggleActivity(activity: String) {
        if (selectedActivities.contains(activity)) {
            selectedActivities.remove(activity)
        } else {
            selectedActivities.add(activity)
        }
    }

    fun updateReflection(text: String) {
        reflectionText = text
    }

    fun updateHoursSleep(text: String) {
        hoursSleepText = text
    }

    private fun obtenerValorAnimo(animo: String): Int = when (animo) {
        "Muy mal" -> 1
        "Mal" -> 2
        "Normal" -> 3
        "Bien" -> 4
        "Muy bien" -> 5
        else -> 3
    }

    private fun obtenerFechaActual(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ========================================
    // GUARDAR UN REGISTRO PERSONAL
    // ========================================
    // Nunca se marca como sintetico.
    // No guardamos pasos falsos.

    fun saveCurrentEntry(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (isSaving || isGeneratingTestData) return

        val textoHoras = hoursSleepText.trim().replace(',', '.')
        val horasSueno = if (textoHoras.isBlank()) {
            null
        } else {
            textoHoras.toDoubleOrNull()
        }

        if (
            textoHoras.isNotBlank() &&
            (horasSueno == null || horasSueno !in 0.0..24.0)
        ) {
            onError("Introduce horas de sueño entre 0 y 24.")
            return
        }

        // Capturar los valores actuales antes de abrir la corrutina.
        val nuevoRegistro = RegistroEntity(
            fecha = obtenerFechaActual(),
            animo = selectedMood,
            valorAnimo = obtenerValorAnimo(selectedMood),
            emojiAnimo = selectedMoodEmoji,
            energia = energyLevel.toInt(),
            actividades = selectedActivities.toList(),
            reflexion = reflectionText.trim(),
            horasSueno = horasSueno,
            pasos = null,
            esSintetico = false
        )

        isSaving = true
        viewModelScope.launch {
            try {
                repository.insertarRegistro(nuevoRegistro)
                reflectionText = ""
                selectedActivities.clear()
                hoursSleepText = ""
                onSuccess()

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                onError("No se pudo guardar el registro. Inténtalo de nuevo.")

            } finally {
                isSaving = false
            }
        }
    }

    // ========================================
    // NOMBRE ANTIGUO: LO CONSERVAMOS
    // ========================================
    // Si alguna pantalla llama generarDatosPrueba(),
    // ahora utilizara el generador nuevo.

    fun generarDatosPrueba(
        onSuccess: (Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        generarDatosRealistas(onSuccess, onError)
    }

    // ========================================
    // IMPORTACION DE LOS 90 DATOS REALISTAS
    // ========================================
    // 1. Generar el conjunto en memoria.
    // 2. Revisar cuales ya existen en Room.
    // 3. Insertar solamente los que falten.
    // 4. Marcar la importacion como terminada.
    //
    // Todo se hace en una transaccion para evitar
    // que una importacion fallida quede a medias.
    // No borramos ningun registro personal.

    fun generarDatosRealistas(
        onSuccess: (Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (isGeneratingTestData || isSaving) return

        // Si ya termino antes, no volver a generar.
        if (preferenciasGenerador.getBoolean("carga_completada", false)) {
            onSuccess(0)
            return
        }

        isGeneratingTestData = true
        errorGeneracion = null

        viewModelScope.launch {
            var cantidadExitosa: Int? = null

            try {
                val cantidad = withContext(Dispatchers.IO) {

                    // Fijamos la fecha base una sola vez.
                    // Si el proceso se interrumpe y se reintenta,
                    // las fechas y horas seran identicas.
                    val fechaGuardada = preferenciasGenerador
                        .getString("fecha_base", null)

                    val fechaBase = if (fechaGuardada != null) {
                        LocalDate.parse(fechaGuardada)
                    } else {
                        LocalDate.now().also { fecha ->
                            preferenciasGenerador.edit()
                                .putString("fecha_base", fecha.toString())
                                .commit()
                        }
                    }

                    val simulados =
                        GeneradorDatosRealistas.generar(hoy = fechaBase)

                    // Comprobamos la base de datos real, no
                    // una lista de la interfaz que pueda ir atrasada.
                    val existentes = repository.todosLosRegistros.first()

                    val clavesSinteticasExistentes = existentes
                        .filter { it.esSintetico }
                        .map { it.fecha to it.creadoEn }
                        .toSet()

                    val faltantes = simulados.filter { dato ->
                        (dato.fecha to dato.creadoEn) !in
                                clavesSinteticasExistentes
                    }

                    // Los 90 INSERT se confirman juntos.
                    database.withTransaction {
                        faltantes.forEach { dato ->
                            repository.insertarRegistro(
                                RegistroEntity(
                                    fecha = dato.fecha,
                                    creadoEn = dato.creadoEn,
                                    animo = dato.animo,
                                    valorAnimo = dato.valorAnimo,
                                    emojiAnimo = dato.emojiAnimo,
                                    energia = dato.energia,
                                    actividades = dato.actividades,
                                    reflexion = dato.reflexion,
                                    horasSueno = dato.horasSueno,
                                    pasos = null, // Los pasos vienen de Health Connect.
                                    esSintetico = true
                                )
                            )
                        }
                    }

                    // Solo despues de guardar correctamente.
                    preferenciasGenerador.edit()
                        .putBoolean("carga_completada", true)
                        .commit()

                    faltantes.size
                }

                cantidadExitosa = cantidad

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                val mensajeError =
                    "No se pudieron cargar los registros realistas: " +
                            (e.localizedMessage ?: "Error desconocido")

                errorGeneracion = mensajeError
                Log.e("MoodWaveDatos", mensajeError, e)
                onError(mensajeError)

            } finally {
                isGeneratingTestData = false
            }

            // El callback se ejecuta DESPUES del finally.
            // Asi V2 puede activar su propio estado de importacion
            // sin que V1 lo desactive accidentalmente.
            cantidadExitosa?.let(onSuccess)
        }
    }

    // ========================================
    // SEGUNDA ETAPA: AÑADIR 210 REGISTROS
    // ========================================
    //
    // No vuelve a generar los 90 originales.
    // Utiliza las mismas fechas de referencia de V1
    // para que los nuevos registros se coloquen
    // en los 105 a 364 dias anteriores.
    //
    // Importa en una sola transaccion Room.
    // Si Android interrumpe el proceso, la transaccion
    // se revierte. Si se ejecuta de nuevo, se omiten
    // las claves sinteticas que ya esten guardadas.
    //
    // No elimina ni altera los registros personales,
    // y no modifica los pasos de Health Connect.

    fun generarDatosRealistasV2(
        onSuccess: (Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (isGeneratingTestData || isSaving) return

        // Importacion completada con anterioridad:
        // no insertar de nuevo los mismos 210.
        if (
            preferenciasGeneradorV2.getBoolean(
                "carga_completada", false
            )
        ) {
            onSuccess(0)
            return
        }

        isGeneratingTestData = true
        errorGeneracion = null

        viewModelScope.launch {
            try {
                val cantidad = withContext(Dispatchers.IO) {

                    // Usamos la fecha base original de V1.
                    // Si por algun motivo falta, guardamos una
                    // fecha base exclusiva de V2 para reintentos.
                    val fechaGuardada =
                        preferenciasGenerador.getString(
                            "fecha_base", null
                        ) ?: preferenciasGeneradorV2.getString(
                            "fecha_base", null
                        )

                    val fechaBase = if (fechaGuardada != null) {
                        LocalDate.parse(fechaGuardada)
                    } else {
                        LocalDate.now().also { fecha ->
                            check(
                                preferenciasGeneradorV2.edit()
                                    .putString("fecha_base", fecha.toString())
                                    .commit()
                            ) { "No se pudo guardar la fecha base de V2." }
                        }
                    }

                    // Consultamos registros existentes directamente
                    // desde Room. Los personales no se usan para
                    // calcular las cantidades por estado emocional.
                    val existentes =
                        repository.todosLosRegistros.first()

                    // V2 calcula cuotas de emociones segun los
                    // registros sinteticos recientes de la etapa 1.
                    val nuevos = GeneradorDatosRealistasV2.generar(
                        registrosExistentes = existentes,
                        fechaBase = fechaBase
                    )

                    check(nuevos.size == GeneradorDatosRealistasV2.TOTAL_NUEVOS) {
                        "El generador V2 no devolvio 210 registros."
                    }

                    // Clave estable: fecha + hora de creacion.
                    // Se compara solo contra datos sinteticos.
                    val clavesExistentes = existentes
                        .asSequence()
                        .filter { it.esSintetico }
                        .map { it.fecha to it.creadoEn }
                        .toHashSet()

                    val faltantes = nuevos.filter { dato ->
                        (dato.fecha to dato.creadoEn) !in clavesExistentes
                    }

                    // Todo se guarda, o no se guarda nada.
                    database.withTransaction {
                        faltantes.forEach { dato ->
                            repository.insertarRegistro(
                                RegistroEntity(
                                    fecha = dato.fecha,
                                    creadoEn = dato.creadoEn,
                                    animo = dato.animo,
                                    valorAnimo = dato.valorAnimo,
                                    emojiAnimo = dato.emojiAnimo,
                                    energia = dato.energia,
                                    actividades = dato.actividades,
                                    reflexion = dato.reflexion,
                                    horasSueno = dato.horasSueno,
                                    pasos = null,
                                    esSintetico = true
                                )
                            )
                        }
                    }

                    // Solo marcamos V2 como completada cuando
                    // los registros se guardaron exitosamente.
                    check(
                        preferenciasGeneradorV2.edit()
                            .putBoolean("carga_completada", true)
                            .commit()
                    ) { "No se pudo registrar la importacion de V2." }

                    faltantes.size
                }

                onSuccess(cantidad)

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                val mensajeError =
                    "No se pudieron cargar los 210 registros adicionales: " +
                            (e.localizedMessage ?: "Error desconocido")
                errorGeneracion = mensajeError
                Log.e("MoodWaveDatosV2", mensajeError, e)
                onError(mensajeError)

            } finally {
                isGeneratingTestData = false
            }
        }
    }
}
