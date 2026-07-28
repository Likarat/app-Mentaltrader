package com.miguel.mentaltrader.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

// HU-032, Escenario 4: exactamente 3 destinos, sin una cuarta pestaña de "Métricas".
class DestinoTest {

    @Test
    fun `hay exactamente 3 destinos`() {
        assertEquals(3, Destino.items.size)
    }

    @Test
    fun `los destinos son Inicio, Historial y Etiquetas en ese orden`() {
        assertEquals(
            listOf(Destino.Inicio, Destino.Historial, Destino.Etiquetas),
            Destino.items
        )
    }

    @Test
    fun `Inicio es el primer destino (apertura por defecto)`() {
        assertEquals(Destino.Inicio, Destino.items.first())
    }
}
