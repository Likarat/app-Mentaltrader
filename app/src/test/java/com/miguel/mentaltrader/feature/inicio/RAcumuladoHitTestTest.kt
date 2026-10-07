package com.miguel.mentaltrader.feature.inicio

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * HU-037 (nuevo slice, post-construccion): mapea la posición X de un gesto de arrastre sobre
 * `RAcumuladoLineChart` al índice del punto de datos más cercano, usando la MISMA escala
 * (`stepX = width / (count - 1)`) que ya usa ese Canvas para dibujar la polilínea (design.md
 * decisión #1) -- función pura, sin Compose, para no depender de un dispositivo/emulador para
 * verificar el cálculo de hit-testing en sí (el gesto real sobre el Canvas sí requiere
 * instrumentado, ver tasks.md 3.2+).
 */
class RAcumuladoHitTestTest {

    @Test
    fun `touchX en el primer punto devuelve indice 0`() {
        val index = RAcumuladoHitTest.nearestIndex(touchX = 0f, width = 300f, pointCount = 4)

        assertEquals(0, index)
    }

    @Test
    fun `touchX en el ultimo punto devuelve el ultimo indice`() {
        val index = RAcumuladoHitTest.nearestIndex(touchX = 300f, width = 300f, pointCount = 4)

        assertEquals(3, index)
    }

    @Test
    fun `touchX exactamente a mitad de camino redondea al punto mas cercano`() {
        // 4 puntos => stepX = 100. El punto 1 vive en x=100; touchX=120 esta mas cerca de el que del 2 (x=200).
        val index = RAcumuladoHitTest.nearestIndex(touchX = 120f, width = 300f, pointCount = 4)

        assertEquals(1, index)
    }

    // Escenario 4 (edge): arrastre fuera de los limites horizontales de la grafica -- se fija
    // (clamped) en el primer o ultimo punto, nunca un indice fuera de rango.
    @Test
    fun `touchX negativo (antes del primer punto) se fija (clamped) en el indice 0`() {
        val index = RAcumuladoHitTest.nearestIndex(touchX = -50f, width = 300f, pointCount = 4)

        assertEquals(0, index)
    }

    @Test
    fun `touchX mas alla del ancho (despues del ultimo punto) se fija (clamped) en el ultimo indice`() {
        val index = RAcumuladoHitTest.nearestIndex(touchX = 500f, width = 300f, pointCount = 4)

        assertEquals(3, index)
    }

    // Escenario 5 (edge): serie con exactamente 2 puntos -- alterna correctamente entre ambos.
    @Test
    fun `con exactamente 2 puntos alterna entre indice 0 y 1 segun la posicion del dedo`() {
        assertEquals(0, RAcumuladoHitTest.nearestIndex(touchX = 0f, width = 200f, pointCount = 2))
        assertEquals(0, RAcumuladoHitTest.nearestIndex(touchX = 80f, width = 200f, pointCount = 2))
        assertEquals(1, RAcumuladoHitTest.nearestIndex(touchX = 120f, width = 200f, pointCount = 2))
        assertEquals(1, RAcumuladoHitTest.nearestIndex(touchX = 200f, width = 200f, pointCount = 2))
    }

    @Test
    fun `con un unico punto siempre devuelve indice 0 sin dividir por cero`() {
        val index = RAcumuladoHitTest.nearestIndex(touchX = 150f, width = 300f, pointCount = 1)

        assertEquals(0, index)
    }
}
