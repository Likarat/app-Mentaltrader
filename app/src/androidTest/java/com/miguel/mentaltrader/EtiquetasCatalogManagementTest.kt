package com.miguel.mentaltrader

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

/**
 * Tests instrumentados de EP-002-a (tasks.md 1.8, HU-010 + HU-011): administración completa de
 * catálogos sobre la app real (`MainActivity`, Room de producción en el dispositivo, sin fakes ni
 * base de datos in-memory) -- cierra `INT-etiquetas-screen-catalogitemdao` (la pantalla Etiquetas,
 * hoy placeholder de EP-005, queda cableada de verdad contra `CatalogItemDao` real vía
 * `CatalogRepository`) y las evidencias de nivel UI de HU-010/HU-011 (incluyendo el diálogo de
 * confirmación de eliminar, cross-cutting a toda la pestaña Etiquetas) que la evidencia unitaria
 * (`CatalogRepositoryTest`/`EtiquetasViewModelTest`, JVM) no alcanza a cerrar por sí sola.
 *
 * Marcador único por método (`System.nanoTime()`) para no colisionar con datos ya sembrados o
 * dejados por corridas manuales/anteriores en el mismo dispositivo -- mismo patrón que
 * `MainActivityFabTest`.
 *
 * La lista de cada catálogo es un `LazyColumn` (no un `Column` plano): si el dispositivo acumuló
 * elementos de corridas previas, el ítem que nos interesa puede no estar compuesto todavía (fuera
 * del viewport). Por eso NO se usa `onNodeWithText(...).assertExists()` directo sobre un ítem de la
 * lista -- se usa `performScrollToNode`, que además de desplazar sirve como la propia aserción de
 * existencia (falla si el nodo nunca aparece). El menú desplegable "Activo" del formulario de
 * registro, en cambio, es un `Column` normal (no lazy) dentro del `DropdownMenu` de Material3: ahí
 * si alcanza con `onNodeWithText(...).assertExists()`, igual que ya hacían `OperationFormJourneyTest`
 * sobre "Confianza"/"Ninguno".
 */
class EtiquetasCatalogManagementTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun abrirEtiquetas() {
        composeTestRule.onNodeWithText("Etiquetas").performClick()
        composeTestRule.waitForIdle()
    }

    private fun abrirPestana(label: String) {
        composeTestRule.onNodeWithText(label).performClick()
        composeTestRule.waitForIdle()
    }

    private fun agregarElemento(nombre: String) {
        composeTestRule.onNodeWithText("Agregar nuevo elemento").performTextInput(nombre)
        composeTestRule.onNodeWithText("Agregar").performClick()
    }

    /** Desplaza el `LazyColumn` del catálogo activo hasta que [texto] esté compuesto y visible;
     * también sirve como aserción de existencia real (lanza si nunca aparece). Reintenta durante
     * [timeoutMs] porque el alta/edición/baja pasa por una corrutina de Room (`viewModelScope`) no
     * sincronizada con el reloj de Compose -- `waitForIdle()` solo no alcanza a esperarla siempre. */
    private fun esperarYDesplazarHasta(texto: String, timeoutMs: Long = 5_000) {
        val start = System.currentTimeMillis()
        var lastError: AssertionError? = null
        while (System.currentTimeMillis() - start <= timeoutMs) {
            composeTestRule.waitForIdle()
            try {
                composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(texto))
                return
            } catch (e: AssertionError) {
                lastError = e
                Thread.sleep(50)
            }
        }
        throw lastError ?: AssertionError("Timeout esperando \"$texto\"")
    }

    private fun esperarHastaQueDesaparezca(texto: String, timeoutMs: Long = 5_000) {
        val start = System.currentTimeMillis()
        while (composeTestRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()) {
            check(System.currentTimeMillis() - start <= timeoutMs) { "Timeout esperando que \"$texto\" desaparezca" }
            Thread.sleep(50)
            composeTestRule.waitForIdle()
        }
    }

    // HU-010 Escenario 1 + INT-etiquetas-screen-catalogitemdao: agregar un elemento nuevo al
    // catálogo de Activos lo persiste vía CatalogRepository.addItem/CatalogItemDao reales y lo deja
    // disponible de inmediato en el selector "Activo" del formulario de registro (no solo en el
    // StateFlow de EtiquetasViewModel).
    @Test
    fun agregarUnActivoLoMuestraDeInmediatoEnElSelectorDelFormularioDeRegistro() {
        val nombre = "EURUSD-${System.nanoTime()}"

        abrirEtiquetas()
        agregarElemento(nombre)
        esperarYDesplazarHasta(nombre)

        // Vuelve a Inicio y abre "Nueva operación" para verificar el selector real.
        composeTestRule.onNodeWithText("Inicio").performClick()
        composeTestRule.onNodeWithContentDescription("Nueva operación").performClick()
        composeTestRule.onNodeWithText("Activo").performScrollTo().performClick()
        composeTestRule.onNodeWithText(nombre).assertExists()
    }

    // HU-011 Escenario 1: editar un elemento existente actualiza el nombre mostrado y desaparece el
    // nombre anterior -- reflejado en la próxima selección del formulario (la propia lista de
    // Etiquetas ya usa el mismo CatalogItemDao real que consume el formulario).
    @Test
    fun editarUnElementoActualizaElNombreMostrado() {
        val original = "Editable-${System.nanoTime()}"
        val editado = "Editado-${System.nanoTime()}"

        abrirEtiquetas()
        agregarElemento(original)
        esperarYDesplazarHasta(original)

        composeTestRule.onNodeWithContentDescription("Editar $original").performClick()
        // El diálogo de edición reutiliza el mismo texto "$original" como valor inicial del campo:
        // se apunta al nodo por hasSetTextAction() (solo el campo editable lo tiene) combinado con
        // hasText(original), para no ambigüar con el ListItem de la lista que queda detrás del
        // diálogo con el mismo texto.
        composeTestRule.onNode(hasSetTextAction() and hasText(original)).performTextReplacement(editado)
        composeTestRule.onNodeWithText("Guardar").performClick()

        esperarYDesplazarHasta(editado)
        composeTestRule.onAllNodesWithText(original).assertCountEquals(0)
    }

    // HU-011 Escenario 2 (revisado): eliminar un elemento sin uso primero pide confirmación
    // explícita ("¿Estás seguro que deseas eliminar...?") y solo se elimina al confirmar.
    @Test
    fun eliminarUnElementoSinUsoPideConfirmacionYLuegoLoElimina() {
        val nombre = "Eliminable-${System.nanoTime()}"

        abrirEtiquetas()
        agregarElemento(nombre)
        esperarYDesplazarHasta(nombre)

        composeTestRule.onNodeWithContentDescription("Eliminar $nombre").performClick()

        composeTestRule.onNodeWithText("Eliminar elemento").assertExists()
        composeTestRule.onNodeWithText("¿Estás seguro que deseas eliminar \"$nombre\"?").assertExists()
        // Sigue en el catálogo mientras el diálogo está abierto: no se eliminó todavía.
        esperarYDesplazarHasta(nombre)

        composeTestRule.onNodeWithText("Eliminar").performClick()

        composeTestRule.onNodeWithText("Eliminar elemento").assertDoesNotExist()
        esperarHastaQueDesaparezca(nombre)
    }

    // Cancelar el diálogo de confirmación no elimina el elemento.
    @Test
    fun cancelarLaConfirmacionDeEliminarDejaElElementoIntacto() {
        val nombre = "NoEliminar-${System.nanoTime()}"

        abrirEtiquetas()
        agregarElemento(nombre)
        esperarYDesplazarHasta(nombre)

        composeTestRule.onNodeWithContentDescription("Eliminar $nombre").performClick()
        composeTestRule.onNodeWithText("Cancelar").performClick()

        composeTestRule.onNodeWithText("Eliminar elemento").assertDoesNotExist()
        // Sigue en el catálogo: no se eliminó.
        esperarYDesplazarHasta(nombre)
    }

    // HU-011 Escenario 3: intentar eliminar el elemento semilla "Ninguno" (Errores, no eliminable
    // en ninguna condición) pide confirmación primero y, al confirmar, muestra el mensaje de
    // bloqueo visible en pantalla; el elemento permanece en el catálogo.
    @Test
    fun intentarEliminarLaSemillaNingunoMuestraElMensajeDeBloqueoVisibleEnPantalla() {
        abrirEtiquetas()
        abrirPestana("Errores")
        esperarYDesplazarHasta("Ninguno")

        composeTestRule.onNodeWithContentDescription("Eliminar Ninguno").performClick()
        composeTestRule.onNodeWithText("Eliminar").performClick()

        composeTestRule.onNodeWithText("No se puede eliminar").assertExists()
        composeTestRule.onNodeWithText("\"Ninguno\" no puede eliminarse.").assertExists()

        composeTestRule.onNodeWithText("Entendido").performClick()
        composeTestRule.onNodeWithText("No se puede eliminar").assertDoesNotExist()

        // Sigue en el catálogo: no se eliminó.
        esperarYDesplazarHasta("Ninguno")
    }
}
