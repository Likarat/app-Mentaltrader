package com.miguel.mentaltrader.feature.inicio

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.data.InicioFilterRepository
import com.miguel.mentaltrader.ui.theme.MentaltraderTheme
import org.junit.Rule
import org.junit.Test

/**
 * HU-029/AC2 (bug reportado 2026-10-06): el filtro de fecha personalizado de Inicio borraba toda
 * la fecha al editar un solo dígito -- `InicioCustomDateRangeFields` usaba
 * `remember(filterState.dateFrom)`, y cada tecleo que dejaba el texto incompleto producía un
 * parseo nulo que `InicioViewModel.onCustomDateRange` propagaba a `filterState.dateFrom = null`,
 * reiniciando el buffer local a "" por el cambio de key. Este test falla sin el fix (ver
 * `InicioScreen.kt`, `InicioCustomDateRangeFields`): borra el último dígito de una fecha completa
 * y confirma que el campo conserva el resto del texto en vez de vaciarse.
 */
class InicioFiltroFechaEdicionParcialTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun borrarUnDigitoDeLaFechaDesdeNoBorraTodoElCampo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val viewModel = InicioViewModel(database.operationDao(), InicioFilterRepository(context))

        composeTestRule.setContent {
            MentaltraderTheme {
                InicioScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Personalizado").performClick()
        composeTestRule.waitForIdle()

        val fechaCompleta = "01/01/2026"
        composeTestRule.onNodeWithText("Desde (dd/mm/aaaa)").performTextInput(fechaCompleta)
        composeTestRule.waitForIdle()

        // Simula borrar el último dígito del año (edición de un solo carácter, no reemplazo total).
        val fechaConUnDigitoBorrado = fechaCompleta.dropLast(1)
        composeTestRule.onNode(hasSetTextAction() and hasText(fechaCompleta))
            .performTextReplacement(fechaConUnDigitoBorrado)
        composeTestRule.waitForIdle()

        // Si el bug reaparece, `filterState.dateFrom` vuelve a `null` tras el parseo fallido y el
        // campo se reinicializa a "" -- este nodo dejaría de existir.
        composeTestRule.onNode(hasSetTextAction() and hasText(fechaConUnDigitoBorrado)).assertExists()

        database.close()
    }
}
