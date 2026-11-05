
package com.example.pp1jd.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

import com.example.pp1jd.ui.navigation.Screen

// ============================================
// ELEMENTOS DE LA BARRA INFERIOR
// ============================================
// Cada elemento tiene:
//
// screen = ruta de navegacion
// title  = texto mostrado
// icon   = icono de la pantalla

sealed class BottomBarItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
) {

    // ========================================
    // INICIO
    // ========================================

    object Home : BottomBarItem(
        screen = Screen.Home,
        title = "Inicio",
        icon = Icons.Default.Home
    )

    // ========================================
    // NUEVO REGISTRO
    // ========================================

    object NewEntry : BottomBarItem(
        screen = Screen.NewEntry,
        title = "Registro",
        icon = Icons.Default.AddCircle
    )

    // ========================================
    // CALENDARIO
    // ========================================

    object Calendar : BottomBarItem(
        screen = Screen.Calendar,
        title = "Calendario",
        icon = Icons.Default.DateRange
    )

    // ========================================
    // ESTADISTICAS
    // ========================================

    object Statistics : BottomBarItem(
        screen = Screen.Statistics,
        title = "Estadísticas",
        icon = Icons.Default.Star
    )

    // ========================================
    // NUEVO: INTELIGENCIA ARTIFICIAL
    // ========================================
    // Utiliza el icono de cerebro.
    //
    // En esta pantalla mostraremos:
    //
    // - Agrupamiento con K-means
    // - Dias similares
    // - Cambios sostenidos
    // - Comparacion con metodo base

    object AI : BottomBarItem(
        screen = Screen.AI,
        title = "IA",
        icon = Icons.Default.Psychology
    )
}

// ============================================
// BARRA DE NAVEGACION DE MOODWAVE
// ============================================

@Composable
fun BottomNavigationBar(
    navController: NavController
) {

    // ========================================
    // PANTALLAS QUE APARECEN EN LA BARRA
    // ========================================

    val items = listOf(
        BottomBarItem.Home,
        BottomBarItem.NewEntry,
        BottomBarItem.Calendar,
        BottomBarItem.Statistics,
        BottomBarItem.AI
    )

    // ========================================
    // BARRA INFERIOR
    // ========================================

    NavigationBar(
        containerColor = Color.White,
        contentColor = Color(0xFF7C3AED)
    ) {

        // ====================================
        // IDENTIFICAR PANTALLA ACTUAL
        // ====================================

        val navBackStackEntry =
            navController
                .currentBackStackEntryAsState()
                .value

        val currentRoute =
            navBackStackEntry
                ?.destination
                ?.route

        // ====================================
        // CREAR CADA BOTON
        // ====================================

        items.forEach { item ->

            val selected =
                currentRoute == item.screen.route

            NavigationBarItem(

                // =================================
                // ICONO
                // =================================

                icon = {

                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },

                // =================================
                // NOMBRE DE LA PANTALLA
                // =================================
                // Reducimos un poco el tamaño
                // para que quepan cinco botones.

                label = {

                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },

                // =================================
                // BOTON SELECCIONADO
                // =================================

                selected = selected,

                // =================================
                // NAVEGACION
                // =================================

                onClick = {

                    // No volvemos a abrir la
                    // misma pantalla si ya
                    // nos encontramos en ella.

                    if (
                        currentRoute !=
                        item.screen.route
                    ) {

                        navController.navigate(
                            item.screen.route
                        ) {

                            // Evita acumular muchas
                            // copias de pantallas.

                            popUpTo(
                                Screen.Home.route
                            ) {

                                saveState = true
                            }

                            // Evita abrir dos
                            // instancias iguales.

                            launchSingleTop = true

                            // Recupera el estado
                            // de la pantalla
                            // cuando es posible.

                            restoreState = true
                        }
                    }
                },

                // =================================
                // COLORES DE MOODWAVE
                // =================================

                colors =
                    NavigationBarItemDefaults.colors(

                        selectedIconColor =
                            Color(0xFF7C3AED),

                        selectedTextColor =
                            Color(0xFF7C3AED),

                        unselectedIconColor =
                            Color(0xFF9CA3AF),

                        unselectedTextColor =
                            Color(0xFF9CA3AF),

                        indicatorColor =
                            Color(0xFFF3E8FF)
                    )
            )
        }
    }
}
