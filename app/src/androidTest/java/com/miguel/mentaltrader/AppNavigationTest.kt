package com.miguel.mentaltrader

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import org.junit.Rule
import org.junit.Test

// HU-032: navegación de 3 pestañas, apertura en Inicio, cambio inmediato, resaltado del activo.
// Los labels de la barra ("Inicio"/"Historial"/"Etiquetas") y el contenido de cada pantalla
// placeholder usan textos distintos a propósito, para que las aserciones no sean ambiguas.
class AppNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun laAppAbreEnInicio() {
        composeTestRule.onNodeWithText("Pantalla de Inicio (placeholder)").assertExists()
    }

    @Test
    fun tocarHistorialMuestraLaPantallaDeHistorialInmediatamente() {
        // Desde EP-003 (historial-estudio-operaciones), Historial ya muestra el listado real
        // agrupado y paginado (o su estado vacío) en vez del placeholder de EP-005 — ver
        // HistorialScreen.kt. El dispositivo real puede tener operaciones guardadas de pruebas
        // manuales previas, así que se acepta cualquiera de los dos: estado vacío o al menos una
        // tarjeta de operación real (identificable por el Resultado, siempre uno de estos 3
        // valores fijos del enum, a diferencia del Activo que es dato variable).
        composeTestRule.onNodeWithText("Historial").performClick()
        composeTestRule
            .onNode(
                hasText("Todavía no registraste ninguna operación") or
                    hasText("WIN", substring = true) or
                    hasText("LOSS", substring = true) or
                    hasText("BREAK_EVEN", substring = true)
            )
            .assertExists()
    }

    @Test
    fun tocarEtiquetasMuestraLaPantallaDeEtiquetas() {
        composeTestRule.onNodeWithText("Etiquetas").performClick()
        composeTestRule.onNodeWithText("Pantalla de Etiquetas (placeholder)").assertExists()
    }

    @Test
    fun elDestinoActivoQuedaMarcadoComoSeleccionadoYLosDemasNo() {
        // Por defecto, Inicio está seleccionado.
        composeTestRule.onNodeWithText("Inicio").assertIsSelected()
        composeTestRule.onNodeWithText("Historial").assertIsNotSelected()
        composeTestRule.onNodeWithText("Etiquetas").assertIsNotSelected()

        composeTestRule.onNodeWithText("Historial").performClick()

        composeTestRule.onNodeWithText("Historial").assertIsSelected()
        composeTestRule.onNodeWithText("Inicio").assertIsNotSelected()
    }
}
