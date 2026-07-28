package com.miguel.mentaltrader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// HU-033, Escenarios "La app abre en modo oscuro" y "El tema no varía según la configuración del SO":
// MentaltraderTheme no expone ningún parámetro de tema claro/dinámico, así que su colorScheme
// es constante independientemente del modo del sistema.
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elTemaSiempreExponeLaPaletaOscuraFija() {
        var backgroundCapturado by mutableStateOf(Color.Unspecified)

        composeTestRule.setContent {
            MentaltraderTheme {
                backgroundCapturado = MaterialTheme.colorScheme.background
                Text("contenido de prueba")
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(MentaltraderDarkColorScheme.background, backgroundCapturado)
        assertEquals(DarkBackground, backgroundCapturado)
    }
}
