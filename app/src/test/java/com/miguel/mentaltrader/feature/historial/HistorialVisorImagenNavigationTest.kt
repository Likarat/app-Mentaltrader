package com.miguel.mentaltrader.feature.historial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HU-019 Escenario 1/3 (tasks.md 7.6, sub-slice EP-003-g): lógica pura de índice/límites de
 * página del visor de imagen -- sin Compose ni `HorizontalPager` real, mismo patrón que
 * [HistorialSeparatorsTest]/[HistorialGroupCollapseTest]. El zoom (Escenario 2) depende de gestos
 * reales y no se cubre aquí (tasks.md 7.6 lo deja explícitamente fuera de este test JVM).
 */
class HistorialVisorImagenNavigationTest {

    // HU-019 Escenario 1: tocar la imagen abre el visor exactamente en esa imagen.
    @Test
    fun `un indice dentro de rango se mantiene igual`() {
        assertEquals(1, HistorialVisorImagenNavigation.resolveInitialPage(imageCount = 3, requestedIndex = 1))
    }

    // Edge: un índice negativo (no debería ocurrir en producción real) cae al primero.
    @Test
    fun `un indice negativo cae en la primera imagen`() {
        assertEquals(0, HistorialVisorImagenNavigation.resolveInitialPage(imageCount = 3, requestedIndex = -5))
    }

    // Edge: un índice igual o mayor a la cantidad de imágenes (p. ej. la lista cambió mientras se
    // armaba la navegación) cae en la última imagen válida en vez de crashear.
    @Test
    fun `un indice fuera de rango por arriba cae en la ultima imagen valida`() {
        assertEquals(2, HistorialVisorImagenNavigation.resolveInitialPage(imageCount = 3, requestedIndex = 99))
    }

    // Edge: sin ninguna imagen no hay página válida.
    @Test
    fun `sin imagenes no hay pagina valida`() {
        assertEquals(-1, HistorialVisorImagenNavigation.resolveInitialPage(imageCount = 0, requestedIndex = 0))
    }

    // HU-019 Escenario 3: la primera de 2 imágenes muestra el indicador y permite avanzar.
    @Test
    fun `hasNextPage es verdadero si no es la ultima imagen`() {
        assertTrue(HistorialVisorImagenNavigation.hasNextPage(currentPage = 0, imageCount = 2))
    }

    // HU-019 Escenario 3: en la última imagen ya no hay página siguiente a la cual avanzar.
    @Test
    fun `hasNextPage es falso en la ultima imagen`() {
        assertFalse(HistorialVisorImagenNavigation.hasNextPage(currentPage = 1, imageCount = 2))
    }

    // Edge: con una única imagen tampoco hay página siguiente.
    @Test
    fun `hasNextPage es falso con una sola imagen`() {
        assertFalse(HistorialVisorImagenNavigation.hasNextPage(currentPage = 0, imageCount = 1))
    }

    // Complemento: en la primera imagen no hay página anterior.
    @Test
    fun `hasPreviousPage es falso en la primera imagen`() {
        assertFalse(HistorialVisorImagenNavigation.hasPreviousPage(currentPage = 0))
    }

    // Complemento: a partir de la segunda imagen sí hay página anterior.
    @Test
    fun `hasPreviousPage es verdadero a partir de la segunda imagen`() {
        assertTrue(HistorialVisorImagenNavigation.hasPreviousPage(currentPage = 1))
    }
}
