package com.miguel.mentaltrader.feature.inicio

import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * HU-037 (nuevo slice, post-construccion): MONTA `RAcumuladoLineChart` de verdad con
 * `ComposeTestRule` (mismo criterio que `OperationFormRenderedUiTest` -- asserta sobre NODOS
 * RENDERIZADOS, no solo sobre el estado interno de `highlightedIndex`) y ejercita el gesto REAL de
 * tocar/arrastrar vía `performTouchInput`. No usa Room/base de datos -- `points` es una lista fija
 * en memoria, cero riesgo sobre datos reales del dispositivo.
 *
 * Nota técnica: `detectDragGestures` (producción) solo dispara `onDragStart` tras superar el touch
 * slop -- un `down()`+`up()` SIN movimiento es un TAP (lo captura el `detectTapGestures` hermano,
 * que limpia el indicador), no un drag. Por eso cada gesto de "arrastre" de este test incluye un
 * `moveTo` real antes del `up()`, y cada gesto se completa en un único `performTouchInput { }`
 * (partial gestures entre bloques separados no se usan, para evitar estado de puntero a medias).
 */
class RAcumuladoDragGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val points = listOf(
        ResultInRCumulativeSeries.Point(index = 0, dateTime = 1_000L, cumulativeResultInR = 1f),
        ResultInRCumulativeSeries.Point(index = 1, dateTime = 2_000L, cumulativeResultInR = -0.5f),
        ResultInRCumulativeSeries.Point(index = 2, dateTime = 3_000L, cumulativeResultInR = 2f),
        ResultInRCumulativeSeries.Point(index = 3, dateTime = 4_000L, cumulativeResultInR = 1.5f)
    )

    private fun setContent(points: List<ResultInRCumulativeSeries.Point> = this.points, widthDp: androidx.compose.ui.unit.Dp = 320.dp) {
        composeTestRule.setContent {
            RAcumuladoLineChart(points = points, modifier = androidx.compose.ui.Modifier.width(widthDp))
        }
        composeTestRule.waitForIdle()
    }

    // Escenario 1 (implícito): antes de cualquier gesto, no hay tooltip.
    @Test
    fun sinGestoNoHayTooltipVisible() {
        setContent()

        composeTestRule.onNodeWithTag(RACUMULADO_TOOLTIP_TEST_TAG).assertDoesNotExist()
    }

    // Escenario 1: arrastrar hacia el primer punto muestra su fecha y R acumulado.
    @Test
    fun arrastrarHaciaElPrimerPuntoMuestraSuFechaYRAcumulado() {
        setContent()

        composeTestRule.onNodeWithTag(RACUMULADO_CANVAS_TEST_TAG).performTouchInput {
            down(Offset(centerX, centerY))
            moveTo(Offset(0f, centerY))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(RACUMULADO_TOOLTIP_TEST_TAG)
            .assertIsDisplayed()
            .assertTextContains("1.0R", substring = true)
    }

    // Escenario 2: arrastrar hacia otra posición actualiza el tooltip al nuevo punto más cercano.
    @Test
    fun arrastrarHaciaOtraPosicionActualizaElTooltipAlNuevoPunto() {
        setContent()

        composeTestRule.onNodeWithTag(RACUMULADO_CANVAS_TEST_TAG).performTouchInput {
            down(Offset(0f, centerY))
            moveTo(Offset(width.toFloat(), centerY))
        }
        composeTestRule.waitForIdle()

        // Ultimo punto (index 3): cumulativeResultInR = 1.5f.
        composeTestRule.onNodeWithTag(RACUMULADO_TOOLTIP_TEST_TAG)
            .assertIsDisplayed()
            .assertTextContains("1.5R", substring = true)
    }

    // Escenario 3: soltar el dedo deja el indicador fijo en el ultimo punto tocado.
    @Test
    fun soltarElDedoDejaElIndicadorFijoEnElUltimoPuntoTocado() {
        setContent()

        composeTestRule.onNodeWithTag(RACUMULADO_CANVAS_TEST_TAG).performTouchInput {
            down(Offset(centerX, centerY))
            moveTo(Offset(0f, centerY))
            up()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(RACUMULADO_TOOLTIP_TEST_TAG)
            .assertIsDisplayed()
            .assertTextContains("1.0R", substring = true)
    }

    // Escenario 3 (continuación): un toque simple (sin arrastre) sobre el propio Canvas limpia el
    // indicador ya fijado por un arrastre anterior.
    @Test
    fun unToqueSimpleSobreElCanvasLimpiaElIndicadorYaFijado() {
        setContent()

        composeTestRule.onNodeWithTag(RACUMULADO_CANVAS_TEST_TAG).performTouchInput {
            down(Offset(centerX, centerY))
            moveTo(Offset(0f, centerY))
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(RACUMULADO_TOOLTIP_TEST_TAG).assertIsDisplayed()

        composeTestRule.onNodeWithTag(RACUMULADO_CANVAS_TEST_TAG).performTouchInput {
            click(Offset(centerX, centerY))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(RACUMULADO_TOOLTIP_TEST_TAG).assertDoesNotExist()
    }

    // Escenario 4 (edge): arrastrar mas alla del borde derecho se fija (clamped) en el ultimo punto.
    @Test
    fun arrastrarMasAllaDelBordeDerechoSeFijaEnElUltimoPunto() {
        setContent()

        composeTestRule.onNodeWithTag(RACUMULADO_CANVAS_TEST_TAG).performTouchInput {
            down(Offset(centerX, centerY))
            moveTo(Offset(width.toFloat() + 500f, centerY))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(RACUMULADO_TOOLTIP_TEST_TAG)
            .assertIsDisplayed()
            .assertTextContains("1.5R", substring = true)
    }

    // Escenario 4 (edge, simétrico): arrastrar mas alla del borde izquierdo se fija en el primero.
    @Test
    fun arrastrarMasAllaDelBordeIzquierdoSeFijaEnElPrimerPunto() {
        setContent()

        composeTestRule.onNodeWithTag(RACUMULADO_CANVAS_TEST_TAG).performTouchInput {
            down(Offset(centerX, centerY))
            moveTo(Offset(-500f, centerY))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(RACUMULADO_TOOLTIP_TEST_TAG)
            .assertIsDisplayed()
            .assertTextContains("1.0R", substring = true)
    }

    // Escenario 5 (edge): con exactamente 2 puntos, el gesto alterna correctamente entre ambos.
    @Test
    fun conExactamenteDosPuntosElGestoLlegaAlSegundoPuntoSinErrores() {
        setContent(
            points = listOf(
                ResultInRCumulativeSeries.Point(index = 0, dateTime = 1_000L, cumulativeResultInR = 1f),
                ResultInRCumulativeSeries.Point(index = 1, dateTime = 2_000L, cumulativeResultInR = -2f)
            ),
            widthDp = 200.dp
        )

        composeTestRule.onNodeWithTag(RACUMULADO_CANVAS_TEST_TAG).performTouchInput {
            down(Offset(0f, centerY))
            moveTo(Offset(width.toFloat(), centerY))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(RACUMULADO_TOOLTIP_TEST_TAG)
            .assertIsDisplayed()
            .assertTextContains("-2.0R", substring = true)
    }
}
