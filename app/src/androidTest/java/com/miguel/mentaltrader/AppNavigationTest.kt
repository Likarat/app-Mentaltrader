package com.miguel.mentaltrader

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import org.junit.Assert.assertTrue
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
        // Desde EP-004 (metricas-deteccion-patrones-inicio), Inicio ya muestra el selector de
        // periodo real (HU-028) en vez del placeholder de EP-005 -- ver InicioScreen.kt. El
        // selector es el único elemento SIEMPRE visible en Inicio sin importar si hay datos
        // (a diferencia de las tarjetas/gráficas, que no se renderizan en el estado vacío de
        // HU-031), y su texto no colisiona con el FAB/CTA "Nueva operación" compartido con
        // Historial.
        composeTestRule.onNodeWithText("Últimos 3 meses").assertExists()
    }

    @Test
    fun tocarHistorialMuestraLaPantallaDeHistorialInmediatamente() {
        // Desde EP-003 (historial-estudio-operaciones), Historial ya muestra el listado real
        // agrupado y paginado (o su estado vacío) en vez del placeholder de EP-005 — ver
        // HistorialScreen.kt. El dispositivo real puede tener operaciones guardadas de pruebas
        // manuales previas (incluso varias -- bug real reportado por el usuario 2026-08-04:
        // `onNode(matcher).assertExists()` exige EXACTAMENTE 1 coincidencia, y con 2+ operaciones
        // reales ya rompía), así que se acepta cualquiera de los dos: estado vacío o al menos una
        // tarjeta de operación real (identificable por el Resultado, siempre uno de estos 3
        // valores fijos del enum, a diferencia del Activo que es dato variable) -- se verifica
        // "existe al menos una" por separado para cada texto candidato, sin importar cuántas
        // coincidencias haya en total.
        composeTestRule.onNodeWithText("Historial").performClick()
        val muestraEstadoVacioOAlgunaOperacion =
            composeTestRule.onAllNodesWithText("Todavía no registraste ninguna operación").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("WIN", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("LOSS", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("BREAK_EVEN", substring = true).fetchSemanticsNodes().isNotEmpty()
        assertTrue(muestraEstadoVacioOAlgunaOperacion)
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
