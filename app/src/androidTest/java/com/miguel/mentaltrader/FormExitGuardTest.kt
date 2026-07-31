package com.miguel.mentaltrader

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test

/**
 * HU-009: confirmar antes de salir del formulario con cambios sin guardar. Cubre los 5
 * escenarios completos: navegación a otra pestaña con cambios (Esc.1), gesto/botón "atrás" del
 * sistema con cambios (Esc.2, distinto mecanismo de disparo del mismo diálogo), Cancelar
 * conserva los datos (Esc.3), confirmar descarta y navega (Esc.4), y salida sin fricción cuando
 * no hay cambios pendientes, por ambos mecanismos (Esc.5).
 *
 * Nota técnica sobre el mecanismo de "atrás" (Escenario 2 y parte del 5): en este dispositivo
 * real (Android con predictive-back / `OnBackInvokedCallback`), ni `Espresso.pressBack()` ni
 * `activity.onBackPressedDispatcher.onBackPressed()` llegaban de forma confiable al
 * `BackHandler` de Compose -- se verificó con logging temporal que `isDirty` SÍ se computa
 * correctamente (`true` tras editar), pero el evento de "atrás" nunca disparaba el callback
 * registrado por nuestro `BackHandler` (competía con el callback de predictive-back que
 * `NavHost` de navigation-compose 2.9.x registra internamente). Se usa en su lugar
 * `UiDevice.pressBack()` (androidx.test.uiautomator): despacha un evento de tecla `KEYCODE_BACK`
 * real a nivel de sistema (el mismo mecanismo que un botón/gesto físico), que sí se resuelve
 * correctamente contra el dispatcher real de la Activity.
 */
class FormExitGuardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val descripcionParcial = "FormExitGuardTest: estaba completando esto"

    private fun pressSystemBack() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        composeTestRule.waitForIdle()
    }

    // HU-009 Escenario 1 + Escenario 3 + Escenario 4: navegar a otra pestaña con cambios
    // pendientes muestra el diálogo; "Cancelar" conserva los datos intactos; repetir y confirmar
    // descarta los cambios y navega de verdad.
    @Test
    fun navegarAOtraPestanaConCambiosPendientesMuestraElDialogoYRespetaCancelarYConfirmar() {
        composeTestRule.onNodeWithContentDescription("Nueva operación").performClick()
        composeTestRule.onNodeWithText("Descripción entrada").performScrollTo().performTextInput(descripcionParcial)

        // Intentar navegar a otra pestaña con cambios pendientes.
        composeTestRule.onNodeWithText("Historial").performClick()
        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertExists()

        // Cancelar: permanece en el formulario con los datos intactos.
        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertDoesNotExist()
        composeTestRule.onNodeWithText(descripcionParcial).assertExists()

        // Repetir el intento de salida y esta vez confirmar: descarta y navega de verdad.
        composeTestRule.onNodeWithText("Historial").performClick()
        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertExists()
        composeTestRule.onNodeWithText("Salir sin guardar").performClick()
        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertDoesNotExist()

        // Navegó de verdad: ya no estamos en el formulario (el título del TopAppBar cambia).
        composeTestRule.onNodeWithText("Nueva operación").assertDoesNotExist()
    }

    // HU-009 Escenario 2: el mismo diálogo se dispara también por el gesto/botón "atrás" del
    // sistema, no solo al tocar otra pestaña -- y respeta "Cancelar" igual que por navegación.
    @Test
    fun presionarAtrasConCambiosPendientesMuestraElMismoDialogoYRespetaCancelar() {
        composeTestRule.onNodeWithContentDescription("Nueva operación").performClick()
        composeTestRule.onNodeWithText("Descripción entrada").performScrollTo().performTextInput(descripcionParcial)
        composeTestRule.waitForIdle()

        pressSystemBack()

        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertExists()

        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertDoesNotExist()
        composeTestRule.onNodeWithText(descripcionParcial).assertExists()
    }

    // HU-009 Escenario 2 + Escenario 4 combinados: confirmar la salida por "atrás" también
    // descarta los cambios y navega (mismo comportamiento que confirmar por pestaña).
    @Test
    fun confirmarSalidaPorAtrasDescartaLosCambiosYNavega() {
        composeTestRule.onNodeWithContentDescription("Nueva operación").performClick()
        composeTestRule.onNodeWithText("Descripción entrada").performScrollTo().performTextInput(descripcionParcial)
        composeTestRule.waitForIdle()

        pressSystemBack()
        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertExists()

        composeTestRule.onNodeWithText("Salir sin guardar").performClick()
        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertDoesNotExist()
        composeTestRule.onNodeWithText("Nueva operación").assertDoesNotExist()
    }

    // HU-009 Escenario 5: sin cambios pendientes, salir por pestaña es inmediato y sin diálogo.
    @Test
    fun sinCambiosPendientesSalirEsInmediatoSinDialogo_porPestana() {
        composeTestRule.onNodeWithContentDescription("Nueva operación").performClick()
        // Deja que termine el prellenado de HU-005 (fecha/hora/activo) -- eso NO cuenta como
        // "sucio" (el snapshot inicial se actualiza junto con el prellenado).
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Historial").performClick()

        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertDoesNotExist()
        composeTestRule.onNodeWithText("Nueva operación").assertDoesNotExist()
    }

    // HU-009 Escenario 5: sin cambios pendientes, salir por "atrás" también es inmediato y sin
    // diálogo (mismo comportamiento por ambos mecanismos de salida).
    @Test
    fun sinCambiosPendientesSalirEsInmediatoSinDialogo_porAtras() {
        composeTestRule.onNodeWithContentDescription("Nueva operación").performClick()
        composeTestRule.waitForIdle()

        pressSystemBack()

        composeTestRule.onNodeWithText("¿Salir sin guardar?").assertDoesNotExist()
        composeTestRule.onNodeWithText("Nueva operación").assertDoesNotExist()
    }
}
