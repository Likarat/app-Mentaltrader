package com.miguel.mentaltrader.core.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Tests unitarios de tasks.md 3.7 (sub-slice EP-001-c): cálculo puro de redimensionado por
 * relación de aspecto. No depende de Bitmap/Android — corre en JVM sin emulador ni Robolectric.
 */
class ImageProcessorTest {

    // HU-007 Escenario 1: ancho objetivo entre ~1600-2000px, alto ajustado por relación de aspecto.
    @Test
    fun `una imagen mas ancha que el maximo se redimensiona manteniendo la relacion de aspecto`() {
        val (width, height) = ImageProcessor.calculateResizedDimensions(4000, 3000, maxWidth = 2000)
        assertEquals(2000, width)
        assertEquals(1500, height)
    }

    @Test
    fun `una imagen vertical mas ancha que el maximo tambien se redimensiona correctamente`() {
        val (width, height) = ImageProcessor.calculateResizedDimensions(3000, 6000, maxWidth = 2000)
        assertEquals(2000, width)
        assertEquals(4000, height)
    }

    // Edge: no se amplían imágenes ya más chicas que el máximo.
    @Test
    fun `una imagen mas chica que el maximo no se amplia`() {
        val (width, height) = ImageProcessor.calculateResizedDimensions(800, 600, maxWidth = 2000)
        assertEquals(800, width)
        assertEquals(600, height)
    }

    // Edge: ancho exactamente igual al máximo tampoco se toca.
    @Test
    fun `una imagen con el ancho exacto al maximo no se modifica`() {
        val (width, height) = ImageProcessor.calculateResizedDimensions(2000, 1000, maxWidth = 2000)
        assertEquals(2000, width)
        assertEquals(1000, height)
    }

    @Test
    fun `dimensiones invalidas lanzan excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageProcessor.calculateResizedDimensions(0, 100, maxWidth = 2000)
        }
    }

    @Test
    fun `la ruta de la miniatura se deriva de la ruta de la imagen completa`() {
        assertEquals("images/abc_thumb.jpg", ImageProcessor.thumbnailPathFor("images/abc.jpg"))
    }
}
