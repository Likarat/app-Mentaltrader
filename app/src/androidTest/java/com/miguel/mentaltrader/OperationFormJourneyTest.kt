package com.miguel.mentaltrader

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        // 1. Abrir "Nueva operación" desde el FAB (visible en Inicio, HU-008 ya cableado).
        composeTestRule.onNodeWithText("Nueva operación").performClick()

        // 2. Fecha/Hora (HU-005 todavía no prellena, sub-slice b; se completan a mano).
        composeTestRule.onNodeWithText("Fecha (dd/MM/aaaa)").performTextInput("28/07/2026")
        composeTestRule.onNodeWithText("Hora (HH:mm)").performTextInput("10:30")

        // 3. Activo: única semilla disponible (XAUUSD).
        composeTestRule.onNodeWithText("Activo").performClick()
        composeTestRule.onNodeWithText("XAUUSD").performClick()

        // 4. Dirección.
        composeTestRule.onNodeWithText("Compra").performClick()

        // 5. Calidad.
        composeTestRule.onNodeWithText("Calidad (0.0-10.0)").performTextInput("8.5")

        // 6. Emoción antes / Emoción después (semillas, valores distintos para no ambigüar).
        composeTestRule.onNodeWithText("Emoción antes").performClick()
        composeTestRule.onNodeWithText("Confianza").performClick()
        composeTestRule.onNodeWithText("Emoción después").performClick()
        composeTestRule.onNodeWithText("Calma").performClick()

        // 7. Error: única semilla disponible (Ninguno).
        composeTestRule.onNodeWithText("Error").performClick()
        composeTestRule.onNodeWithText("Ninguno").performClick()

        // 8. Resultado.
        composeTestRule.onNodeWithText("Ganada").performClick()

        // 9. Descripción entrada (obligatoria, HU-003).
        composeTestRule.onNodeWithText("Descripción entrada").performTextInput(descripcion)

        // 10. Guardar -> onSaved navega de vuelta (popBackStack).
        composeTestRule.onNodeWithText("Guardar").performClick()

        // 11. La operación recién creada aparece en el listado mínimo de Historial
        //     (INT-form-listado-minimo).
        composeTestRule.onNodeWithText("Historial").performClick()
        composeTestRule.onAllNodesWithText(descripcion)[0].assertExists()
    }
}
