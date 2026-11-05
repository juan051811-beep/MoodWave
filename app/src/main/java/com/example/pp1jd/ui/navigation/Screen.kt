
package com.example.pp1jd.ui.navigation

// ============================================
// RUTAS DE NAVEGACION DE MOODWAVE
// ============================================
// Cada objeto representa una pantalla.
//
// route: identificador interno de navegacion.
// title: nombre que mostramos al usuario.

sealed class Screen(
    val route: String,
    val title: String
) {

    // ========================================
    // PANTALLA DE INICIO
    // ========================================

    object Home : Screen(
        route = "home",
        title = "Inicio"
    )

    // ========================================
    // NUEVO REGISTRO EMOCIONAL
    // ========================================

    object NewEntry : Screen(
        route = "new_entry",
        title = "Registro"
    )

    // ========================================
    // HISTORIAL DE REGISTROS
    // ========================================

    object History : Screen(
        route = "history",
        title = "Historial"
    )

    // ========================================
    // CALENDARIO EMOCIONAL
    // ========================================

    object Calendar : Screen(
        route = "calendar",
        title = "Calendario"
    )

    // ========================================
    // ESTADISTICAS Y GRAFICAS
    // ========================================

    object Statistics : Screen(
        route = "statistics",
        title = "Estadísticas"
    )

    // ========================================
    // NUEVA PANTALLA: INTELIGENCIA ARTIFICIAL
    // ========================================
    // Mostrara:
    //
    // 1. Agrupamiento de dias con K-means.
    // 2. Grafica de grupos similares.
    // 3. Comparacion con metodo base.
    // 4. Cambios emocionales sostenidos.
    // 5. Fechas y explicaciones.
    //
    // Los resultados vendran del ViewModel
    // y se actualizaran con Room.

    object AI : Screen(
        route = "ai",
        title = "IA"
    )
}
