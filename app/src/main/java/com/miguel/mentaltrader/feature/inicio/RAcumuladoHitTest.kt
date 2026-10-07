package com.miguel.mentaltrader.feature.inicio

/**
 * HU-037 (nuevo slice, post-construccion): mapea la posición X de un gesto de arrastre sobre
 * `RAcumuladoLineChart` al índice del punto de datos más cercano, invirtiendo la MISMA escala
 * (`stepX = width / (count - 1)`) que ese Canvas ya usa para dibujar la polilínea -- no se
 * recalcula una escala distinta para el gesto (design.md decisión #1). Función pura, sin Compose:
 * el gesto real (`detectDragGestures`) solo llama a [nearestIndex] con la posición X reportada por
 * el framework, testeada aparte en JVM (`RAcumuladoHitTestTest`).
 *
 * HU-037 Escenario 4 (edge): [touchX] fuera de `[0, width]` se fija (clamped) en el primer o
 * último índice, nunca devuelve un índice fuera de rango.
 */
object RAcumuladoHitTest {

    fun nearestIndex(touchX: Float, width: Float, pointCount: Int): Int {
        if (pointCount <= 1) return 0
        val stepX = width / (pointCount - 1)
        val rawIndex = (touchX / stepX).let { if (it.isNaN()) 0f else it }
        return Math.round(rawIndex).coerceIn(0, pointCount - 1)
    }
}
