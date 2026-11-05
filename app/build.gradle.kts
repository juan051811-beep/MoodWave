
plugins {

    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.compose)

    // =====================================
    // PROCESADOR DE ANOTACIONES PARA ROOM
    // =====================================

    alias(libs.plugins.ksp)
}

android {

    // =====================================
    // CONFIGURACION GENERAL
    // =====================================

    namespace = "com.example.pp1jd"

    // Conservamos Android SDK 37.

    compileSdk {
        version = release(37)
    }

    // =====================================
    // CONFIGURACION DE LA APLICACION
    // =====================================

    defaultConfig {

        applicationId = "com.example.pp1jd"

        // Health Connect 1.1.0 requiere
        // como minimo Android 8.0 (API 26).

        minSdk = 26

        // Conservamos el SDK objetivo.

        targetSdk = 37

        versionCode = 1

        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    // =====================================
    // TIPOS DE COMPILACION
    // =====================================

    buildTypes {

        release {

            optimization {
                enable = false
            }
        }
    }

    // =====================================
    // COMPATIBILIDAD JAVA
    // =====================================

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    // =====================================
    // HABILITAR JETPACK COMPOSE
    // =====================================

    buildFeatures {
        compose = true
    }
}

dependencies {

    // =====================================
    // JETPACK COMPOSE
    // =====================================

    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    // =====================================
    // ANDROID Y LIFECYCLE
    // =====================================

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.11.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0"
    )

    // =====================================
    // NAVEGACION ENTRE PANTALLAS
    // =====================================

    implementation(
        "androidx.navigation:navigation-compose:2.8.5"
    )

    // =====================================
    // ICONOS DE MATERIAL DESIGN
    // =====================================

    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    // =====================================
    // ROOM - BASE DE DATOS LOCAL
    // =====================================

    // Motor principal de Room.

    implementation(
        libs.androidx.room.runtime
    )

    // Extensiones de Kotlin y corrutinas.

    implementation(
        libs.androidx.room.ktx
    )

    // Generador de codigo de Room.

    ksp(
        libs.androidx.room.compiler
    )

    // =====================================
    // HEALTH CONNECT - NUEVA FUNCIONALIDAD
    // =====================================
    //
    // Integramos la biblioteca oficial
    // de Health Connect.
    //
    // La utilizaremos para:
    //
    // 1. Comprobar disponibilidad.
    // 2. Solicitar permiso de lectura.
    // 3. Consultar pasos diarios.
    // 4. Relacionar pasos con registros.
    //
    // No solicitaremos permiso para
    // escribir ni modificar datos.
    //
    // Version estable: 1.1.0

    implementation(
        "androidx.health.connect:connect-client:1.1.0"
    )

    // =====================================
    // PRUEBAS UNITARIAS
    // =====================================
    //
    // Conservamos JUnit para nuestras
    // pruebas de inteligencia artificial.

    testImplementation(
        libs.junit
    )

    // =====================================
    // PRUEBAS INSTRUMENTADAS
    // =====================================

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    // =====================================
    // HERRAMIENTAS DE DESARROLLO
    // =====================================

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}
