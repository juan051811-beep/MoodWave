
package com.example.pp1jd

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Scaffold

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier

import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.pp1jd.ui.components.BottomNavigationBar
import com.example.pp1jd.ui.navigation.Screen

// ============================================
// IMPORTAR TODAS LAS PANTALLAS
// ============================================

import com.example.pp1jd.ui.screens.HomeScreen
import com.example.pp1jd.ui.screens.NewEntryScreen
import com.example.pp1jd.ui.screens.HistoryScreen
import com.example.pp1jd.ui.screens.CalendarScreen
import com.example.pp1jd.ui.screens.StatisticsScreen

// NUEVA PANTALLA DE INTELIGENCIA ARTIFICIAL
import com.example.pp1jd.ui.screens.AIScreen

// ============================================
// TEMA Y VIEWMODEL
// ============================================

import com.example.pp1jd.ui.theme.PP1JDTheme
import com.example.pp1jd.ui.viewmodel.DiaryViewModel

// ============================================
// ACTIVIDAD PRINCIPAL DE MOODWAVE
// ============================================

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        // Permite aprovechar el espacio
        // de la pantalla del dispositivo.
        enableEdgeToEdge()

        // ====================================
        // INICIAR JETPACK COMPOSE
        // ====================================

        setContent {

            // Tema visual de MoodWave.
            PP1JDTheme {

                // Aplicacion principal.
                MainApp()
            }
        }
    }
}

// ============================================
// APLICACION Y NAVEGACION PRINCIPAL
// ============================================

@Composable
fun MainApp() {

    // ========================================
    // CONTROLADOR DE NAVEGACION
    // ========================================

    val navController = rememberNavController()

    // ========================================
    // VIEWMODEL COMPARTIDO
    // ========================================
    //
    // Todas las pantallas utilizan
    // el mismo DiaryViewModel.
    //
    // Esto permite compartir:
    //
    // - Registros almacenados en Room
    // - Historial emocional
    // - Estado del formulario
    // - Estadisticas dinamicas
    // - Agrupamiento K-means
    // - Deteccion de anomalias
    //
    // No creamos un ViewModel distinto
    // para cada pantalla.

    val diaryViewModel: DiaryViewModel = viewModel()

    // ========================================
    // ESTRUCTURA VISUAL PRINCIPAL
    // ========================================

    Scaffold(
        modifier = Modifier.fillMaxSize(),

        // ====================================
        // BARRA DE NAVEGACION INFERIOR
        // ====================================

        bottomBar = {

            BottomNavigationBar(
                navController = navController
            )
        }

    ) { innerPadding ->

        // ====================================
        // CONTENEDOR DE PANTALLAS
        // ====================================

        NavHost(
            navController = navController,

            // Pantalla inicial de MoodWave.
            startDestination = Screen.Home.route,

            // Respetar espacios del Scaffold.
            modifier = Modifier.padding(
                innerPadding
            )
        ) {

            // =================================
            // PANTALLA 1: INICIO
            // =================================

            composable(
                route = Screen.Home.route
            ) {

                HomeScreen(
                    viewModel = diaryViewModel,

                    onNavigateToHistory = {

                        navController.navigate(
                            Screen.History.route
                        )
                    }
                )
            }

            // =================================
            // PANTALLA 2: NUEVO REGISTRO
            // =================================

            composable(
                route = Screen.NewEntry.route
            ) {

                NewEntryScreen(
                    viewModel = diaryViewModel,

                    onSaved = {

                        // Cuando se guarda un
                        // registro, navegamos
                        // al historial.

                        navController.navigate(
                            Screen.History.route
                        ) {

                            // Evitamos acumular
                            // pantallas anteriores.

                            popUpTo(
                                Screen.Home.route
                            )
                        }
                    }
                )
            }

            // =================================
            // PANTALLA 3: HISTORIAL
            // =================================

            composable(
                route = Screen.History.route
            ) {

                HistoryScreen(
                    viewModel = diaryViewModel,

                    onBack = {

                        navController.popBackStack()
                    }
                )
            }

            // =================================
            // PANTALLA 4: CALENDARIO
            // =================================

            composable(
                route = Screen.Calendar.route
            ) {

                CalendarScreen(
                    viewModel = diaryViewModel
                )
            }

            // =================================
            // PANTALLA 5: ESTADISTICAS
            // =================================
            //
            // Contiene:
            //
            // - Promedio, mediana y moda
            // - Desviacion estandar
            // - Energia promedio
            // - Racha maxima
            // - Tendencias
            // - Media movil
            // - Actividades frecuentes
            // - Mapa de calor
            // - Actividad vs. Animo

            composable(
                route = Screen.Statistics.route
            ) {

                StatisticsScreen(
                    viewModel = diaryViewModel
                )
            }

            // =================================
            // PANTALLA 6: INTELIGENCIA ARTIFICIAL
            // =================================
            //
            // NUEVA FUNCIONALIDAD
            //
            // La pantalla recibe el mismo
            // ViewModel que las demas.
            //
            // AIScreen consultara:
            //
            // diaryViewModel.estadoIA
            //
            // Este estado contiene los
            // resultados de los dos motores:
            //
            // 1. MotorAgrupamientoIA
            // 2. MotorAnomaliasIA
            //
            // Ambos se actualizan al
            // modificarse los registros
            // almacenados en Room.

            composable(
                route = Screen.AI.route
            ) {

                AIScreen(
                    viewModel = diaryViewModel
                )
            }
        }
    }
}
