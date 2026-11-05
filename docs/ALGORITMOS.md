# Algoritmos y análisis de MoodWave

Este documento describe los motores presentes en `app/src/main/java/com/example/pp1jd/analytics/`.

## 1. Estadísticas descriptivas

`MotorEstadisticas.kt`, `MotorTendencias.kt` y las pantallas de estadísticas resumen ánimo, energía, frecuencias, rachas y evolución temporal. Se incluyen medidas como media, mediana, moda, desviación estándar, promedios móviles y visualizaciones en distintos períodos.

## 2. Relación actividad–ánimo y d de Cohen

`MotorActividadAnimo.kt` compara días **con** una actividad frente a días **sin** esa actividad. Para evaluar patrones, exige al menos **14 días válidos** en total y **5 días en cada grupo**.

Además de las medias, calcula el tamaño de efecto **d de Cohen** (diferencia de medias dividida por una desviación estándar combinada). Un efecto grande expresa una diferencia relativamente importante en la muestra, **no prueba causalidad**.

## 3. K-means (`MotorAgrupamientoIA.kt`)

Agrupa días semejantes a partir de **ánimo y energía**. Si hay varios registros para una fecha, los prepara a nivel de día. Requiere **14 días distintos** antes de calcular los grupos. El algoritmo asigna cada punto al centro más cercano, recalcula los centros y repite hasta la convergencia o un máximo de **100 iteraciones**. El número efectivo de grupos depende de la variación disponible.

El motor también calcula una comparación con un **método base (baseline)** que clasifica solamente por ánimo; sirve como referencia frente al agrupamiento conjunto por ánimo y energía.

Los grupos son patrones descriptivos obtenidos por el algoritmo: no equivalen a diagnósticos psicológicos.

## 4. Cambios sostenidos (`MotorAnomaliasIA.kt`)

Para cada día con datos suficientes, compara el estado observado con una línea base reciente:

- **Ventana histórica:** 21 días de calendario anteriores.
- **Mínimo para la referencia:** 14 días con información.
- **Cambio notable:** diferencia de al menos **1 punto de ánimo** o **2 puntos de energía**, en el sentido correspondiente.
- **Cambio sostenido:** al menos **3 días consecutivos** con una dirección compatible.

El motor puede describir **descensos** y **aumentos** sostenidos. Un día aislado no constituye por sí solo una racha sostenida.

## 5. Pasos frente a ánimo y energía (`MotorPasosAnimo.kt`)

Combina datos por fecha, usando los pasos que el usuario sincroniza desde **Health Connect**. Analiza intervalos de días y calcula correlaciones de **Pearson** entre pasos y ánimo y entre pasos y energía únicamente con fechas que tienen ambos datos. Para el cálculo requiere al menos **7 días comparables** y variación suficiente en ambas variables; de lo contrario, devuelve un resultado sin correlación en vez de inventarla.

- Correlación positiva: valores tienden a subir juntos.
- Correlación negativa: uno tiende a bajar cuando el otro sube.
- Correlación cercana a cero: asociación lineal débil o ausente en la muestra.

**Correlación no implica causalidad.** Las coincidencias con registros emocionales sintéticos deben interpretarse únicamente como demostración técnica.

## Consideraciones metodológicas

Las métricas describen únicamente los datos disponibles; no estiman automáticamente significancia estadística ni sirven para tomar decisiones médicas. La disponibilidad de pasos reales puede ser inferior a la de emociones sintéticas, de modo que una gráfica puede mostrar datos mientras una correlación permanece sin calcular.
