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
        // Desde EP-001 (registro-rapido-operaciones), Historial ya muestra contenido real
        // (o su estado vacío) en vez del placeholder de EP-005 — ver HistorialScreen.kt. El
        // dispositivo real puede tener operaciones guardadas de pruebas manuales previas, así
        // que se acepta cualquiera de los dos: estado vacío o al menos una operación listada.
        composeTestRule.onNodeWithText("Historial").performClick()
        composeTestRule
            .onNode(hasText("Todavía no registraste ninguna operación") or hasText("Operación #", substring = true))
            .assertExists()
    }

    @Test
    fun tocarEtiquetasMuestraLaPantallaDeEtiquetas() {
        // EP-002-a reemplazó el placeholder de EP-005 ("Pantalla de Etiquetas (placeholder)") por
        // la administración real de catálogos (SecondaryTabRow Activos/Emociones/Errores + alta) —
        // ver EtiquetasScreen.kt. "Agregar nuevo elemento" es el label del campo de alta, único de
        // esta pantalla real, y no depende de cuántos elementos tenga ya el catálogo en este
        // dispositivo (a diferencia de asertar sobre un nombre de ítem concreto de la lista).
        composeTestRule.onNodeWithText("Etiquetas").performClick()
        composeTestRule.onNodeWithText("Agregar nuevo elemento").assertExists()
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
