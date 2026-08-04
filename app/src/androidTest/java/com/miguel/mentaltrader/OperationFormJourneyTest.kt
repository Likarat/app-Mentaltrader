package com.miguel.mentaltrader

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * journey_smoke de EP-001-a (tasks.md 1.12, sub-slice "registro completo de campos + Room
 * real"): recorre el journey completo de punta a punta sobre la app real (Compose + Room de
 * producción, sin fakes) — FAB en Inicio -> formulario "Nueva operación" -> completar los
 * campos obligatorios de HU-001/HU-002/HU-003 (incluida la descripción de entrada) -> "Guardar"
 * -> vuelve a Inicio -> la operación recién creada aparece en el listado mínimo de Historial.
 */
class OperationFormJourneyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun crearUnaOperacionDePuntaAPuntaYVerlaEnElListadoMinimo() {
        val descripcion = "Journey smoke EP-001-a: ruptura de rango en H1"

        // 1. Abrir "Nueva operación" desde el FAB (visible en Inicio, HU-008 ya cableado). El FAB
        // es solo-ícono: se identifica por contentDescription, no por texto visible.
        composeTestRule.onNodeWithContentDescription("Nueva operación").performClick()

        // 2. Fecha/Hora ya vienen prellenados por HU-005 (fecha/hora actual) — no hace falta
        // completarlos a mano.

        // 3. Activo: HU-005 Esc.2/3 solo prellena automáticamente si el catálogo NO tiene más que
        // la semilla protegida (XAUUSD) -- en este dispositivo real, compartido entre todos los
        // tests instrumentados de la suite (Room de producción, sin fake ni reset entre clases),
        // otras clases (ej. EtiquetasCatalogManagementTest) pueden haber agregado más activos
        // antes de que esta corra, lo que desactiva el prellenado automático. Se selecciona
        // explícitamente XAUUSD (semilla protegida, siempre presente) en vez de depender de que
        // el catálogo tenga exactamente un elemento.
        composeTestRule.onNodeWithText("Activo").performScrollTo().performClick()
        composeTestRule.onNodeWithText("XAUUSD").performClick()

        // El formulario es un Column con verticalScroll (no LazyColumn): todo está siempre
        // presente en el árbol de semántica, pero un click por coordenadas puede fallar si el
        // nodo está fuera del viewport visible — se hace scrollTo() antes de cada click en un
        // campo del formulario (no en los ítems de un menú desplegable, que viven en su propio
        // Popup y no depende del scroll de la columna principal).

        // 4. Dirección.
        composeTestRule.onNodeWithText("Compra").performScrollTo().performClick()

        // 5. Calidad.
        composeTestRule.onNodeWithText("Calidad (0.0-10.0)").performScrollTo().performTextInput("8.5")

        // 6. Emoción antes / Emoción después (semillas, valores distintos para no ambigüar).
        composeTestRule.onNodeWithText("Emoción antes").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Confianza").performClick()
        composeTestRule.onNodeWithText("Emoción después").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Calma").performClick()

        // 7. Error: única semilla disponible (Ninguno).
        composeTestRule.onNodeWithText("Error").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Ninguno").performClick()

        // 8. Resultado.
        composeTestRule.onNodeWithText("Ganada").performScrollTo().performClick()

        // 9. Descripción entrada (obligatoria, HU-003).
        composeTestRule.onNodeWithText("Descripción entrada").performScrollTo().performTextInput(descripcion)

        // 10. Guardar -> onSaved navega de vuelta (popBackStack).
        composeTestRule.onNodeWithText("Guardar").performScrollTo().performClick()

        // 11. La operación recién creada aparece en el listado real de Historial
        //     (INT-form-listado-minimo). Desde EP-003 la tarjeta ya no muestra la descripción de
        //     entrada (ver HistorialScreen.kt, HU-015 Escenario 3): se identifica por el
        //     Resultado, mismo criterio ya aplicado en AppNavigationTest.
        composeTestRule.onNodeWithText("Historial").performClick()
        // El listado real usa Paging 3 (HistorialViewModel.pagingSourceFiltered) -- en dispositivo
        // real, bajo la carga de correr la suite completa, la primera página puede tardar más de
        // un frame en llegar tras el insert real en Room. waitForIdle() no cubre ese delay porque
        // es I/O asíncrono fuera de la sincronización de Compose, no una recomposición pendiente.
        waitUntil(timeoutMs = 15_000) {
            composeTestRule.onAllNodesWithText("WIN", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("WIN", substring = true)[0].assertExists()
    }

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            check(System.currentTimeMillis() - start <= timeoutMs) { "Timeout esperando la condición" }
            Thread.sleep(20)
            composeTestRule.waitForIdle()
        }
    }
}
