package com.miguel.mentaltrader.core.image

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests unitarios de DiskSpaceCalculator (tasks.md 4.4, sub-slice EP-002-d, HU-014): suma sobre
 * archivos reales de prueba (no mocks de File), caso cero, archivo faltante no crashea. Usa
 * TemporaryFolder (JUnit) en vez de Room/Android -- es una función pura sobre java.io.File.
 */
class DiskSpaceCalculatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // HU-014 Escenario 1 (a nivel de cálculo): suma el tamaño real de los archivos existentes.
    @Test
    fun `totalBytes suma el tamano real de los archivos existentes`() {
        tempFolder.newFile("a.jpg").writeBytes(ByteArray(100))
        tempFolder.newFile("b.jpg").writeBytes(ByteArray(250))

        val total = DiskSpaceCalculator.totalBytes(tempFolder.root, listOf("a.jpg", "b.jpg"))

        assertEquals(350L, total)
    }

    // HU-014 Escenario 2: ninguna ruta -> el total es 0, sin error.
    @Test
    fun `totalBytes es 0 cuando no hay ninguna ruta`() {
        assertEquals(0L, DiskSpaceCalculator.totalBytes(tempFolder.root, emptyList()))
    }

    // HU-014 Escenario 3: una ruta cuyo archivo ya no existe (ej. tras eliminar la operación que
    // la usaba) se ignora en silencio, sin crashear -- el total refleja solo lo que sigue en disco.
    @Test
    fun `totalBytes ignora en silencio una ruta cuyo archivo ya no existe`() {
        tempFolder.newFile("existe.jpg").writeBytes(ByteArray(500))

        val total = DiskSpaceCalculator.totalBytes(tempFolder.root, listOf("existe.jpg", "borrado.jpg"))

        assertEquals(500L, total)
    }
}
