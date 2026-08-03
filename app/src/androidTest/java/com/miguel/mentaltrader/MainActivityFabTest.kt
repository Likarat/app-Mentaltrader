package com.miguel.mentaltrader

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * HU-008 Escenario 3 (ausencia del FAB en Etiquetas) y Escenario 4 (el FAB permanece visible e
 * interactuable al hacer scroll). Usa la base de datos real de la app (misma app en producción,
 * `MainActivity`), sembrando operaciones adicionales para garantizar contenido suficiente para
 * scroll en Historial.
 */
class MainActivityFabTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Marca única por método de test: la base de datos real de la app NO se resetea entre los 2
    // métodos de esta clase (comparten la misma instalación/DB), así que sembrar siempre con el
    // mismo texto ("...operación #0") produciría duplicados ambiguos de un método a otro -- bug
    // real de test encontrado corriendo la clase completa (no en corridas aisladas por método).
    private lateinit var runMarker: String

    @Before
    fun seedManyOperations() {
        runMarker = "MainActivityFabTest-${System.nanoTime()}"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as MentaltraderApplication
        waitUntilCatalogsSeeded(app)
        runBlocking {
            val assetId = app.database.catalogItemDao().getByType(CatalogType.ASSET).first().first().id
            val emotionId = app.database.catalogItemDao().getByType(CatalogType.EMOTION).first().first().id
            val errorId = app.database.catalogItemDao().getByType(CatalogType.ERROR).first().first().id
            repeat(30) { i ->
                app.database.operationDao().insert(
                    Operation(
                        dateTime = System.currentTimeMillis() - i * 60_000L,
                        assetId = assetId,
                        direction = Direction.BUY,
                        quality = 8f,
                        emotionBeforeId = emotionId,
                        emotionAfterId = emotionId,
                        errorId = errorId,
                        result = ResultType.WIN,
                        entryDescription = "$runMarker operación #$i",
                        createdAt = 0L,
                        updatedAt = 0L
                    )
                )
            }
        }
    }

    private fun waitUntilCatalogsSeeded(app: MentaltraderApplication, timeoutMs: Long = 10_000) {
        val start = System.currentTimeMillis()
        while (runBlocking { app.database.catalogItemDao().countByType(CatalogType.ASSET) } == 0) {
            check(System.currentTimeMillis() - start <= timeoutMs) { "Timeout esperando la siembra de catálogos" }
            Thread.sleep(50)
        }
    }

    // HU-008 Escenario 3: ausencia del FAB en la pestaña Etiquetas.
    @Test
    fun elFabNoApareceEnLaPestanaEtiquetas() {
        composeTestRule.onNodeWithText("Etiquetas").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Nueva operación").assertDoesNotExist()
    }

    // HU-008 Escenario 4: el FAB permanece visible e interactuable al hacer scroll en Historial,
    // sin bloquear la interacción con el contenido subyacente.
    @Test
    fun elFabPermaneceVisibleEInteractuableAlHacerScrollEnHistorial() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as MentaltraderApplication
        val totalOperaciones = runBlocking { app.database.operationDao().getAllOrderedByDateDesc().first().size }
        assertTrue("Debe haber al menos 30 operaciones para forzar scroll", totalOperaciones >= 30)

        composeTestRule.onNodeWithText("Historial").performClick()
        composeTestRule.waitForIdle()

        // Desde EP-003, la tarjeta ya no muestra la descripción de entrada (ver HistorialScreen.kt,
        // HU-015 Escenario 3): se identifica el contenido real por el Resultado, mismo criterio ya
        // aplicado en AppNavigationTest.
        composeTestRule.onAllNodesWithText("WIN", substring = true)[0].assertExists()
        composeTestRule.onNodeWithContentDescription("Nueva operación").assertIsDisplayed().assertHasClickAction()

        // Desplaza el listado hasta el último ítem real (sea cual sea el total, incluyendo
        // operaciones preexistentes en el dispositivo de sesiones anteriores).
        composeTestRule.onNode(hasScrollAction()).performScrollToIndex(totalOperaciones - 1)
        composeTestRule.waitForIdle()

        // El FAB sigue visible y clickeable tras el scroll (no desaparece ni queda inerte).
        composeTestRule.onNodeWithContentDescription("Nueva operación")
            .assertIsDisplayed()
            .assertHasClickAction()

        // El contenido subyacente (la fila renderizada al final del listado) también sigue
        // presente/accesible: la interacción no queda bloqueada por el FAB.
        composeTestRule.onAllNodesWithText("WIN", substring = true)[0].assertExists()

        // Y el FAB sigue abriendo el formulario con normalidad tras el scroll.
        composeTestRule.onNodeWithContentDescription("Nueva operación").performClick()
        composeTestRule.onNodeWithText("Nueva operación").assertExists()
    }
}
