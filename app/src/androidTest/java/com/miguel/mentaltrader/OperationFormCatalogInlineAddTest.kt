package com.miguel.mentaltrader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.feature.registro.OperationFormScreen
import com.miguel.mentaltrader.feature.registro.OperationFormViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests instrumentados de EP-002-b (tasks.md 2.6, HU-013): agregar una etiqueta inline desde el
 * formulario de operación, mismo estándar que `OperationFormRenderedUiTest` de EP-001 -- monta
 * `OperationFormScreen` de verdad con `ComposeTestRule` + un `OperationFormViewModel` real (Room
 * in-memory real, sin fakes), y asserta sobre NODOS RENDERIZADOS, no sobre `viewModel.state.value`.
 *
 * `CatalogRepository.addItem` (HU-010) no se revalida aquí -- estos tests prueban el ángulo propio
 * de HU-013: el campo inline aparece/desaparece, la selección automática, el foco/mensaje ante un
 * duplicado y que el resto de los datos ya ingresados en el formulario permanezca intacto.
 */
class OperationFormCatalogInlineAddTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: android.content.Context
    private lateinit var database: AppDatabase
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var viewModel: OperationFormViewModel

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        imageProcessor = ImageProcessor(context)
        viewModel = OperationFormViewModel(
            database.operationDao(),
            database.catalogItemDao(),
            database.operationImageDao(),
            imageProcessor
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun mount() {
        composeTestRule.setContent {
            OperationFormScreen(viewModel = viewModel, onSaved = {})
        }
        composeTestRule.waitForIdle()
    }

    private fun seedAsset(name: String, isDefault: Boolean = false): Long = runBlocking {
        val now = System.currentTimeMillis()
        database.catalogItemDao().insert(
            CatalogItem(type = CatalogType.ASSET, name = name, isDefault = isDefault, createdAt = now, updatedAt = now)
        )
    }

    private fun abrirAltaInline(label: String) {
        // El formulario es un Column con verticalScroll (no LazyColumn, mismo criterio ya
        // documentado en OperationFormJourneyTest): un click por coordenadas puede fallar en
        // dispositivo real si el nodo quedó fuera del viewport visible -- por ejemplo, tras escribir
        // en "Descripción entrada" (más abajo en el formulario), la vista se desplaza y "Activo"
        // (más arriba) puede quedar fuera de pantalla. Se hace scrollTo() antes del click, igual que
        // en el resto del formulario.
        composeTestRule.onNodeWithText(label).performScrollTo().performClick()
        composeTestRule.onNodeWithText("+ Agregar nueva").performClick()
    }

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            check(System.currentTimeMillis() - start <= timeoutMs) { "Timeout esperando la condición" }
            Thread.sleep(20)
            composeTestRule.waitForIdle()
        }
    }

    // HU-013 Escenario 1: tocar "+ Agregar nueva" muestra el campo inline, sin salir del formulario.
    @Test
    fun tocarAgregarNuevaEnActivoMuestraElCampoInlineDeAlta() {
        mount()

        abrirAltaInline("Activo")

        composeTestRule.onNodeWithText("Nuevo valor para Activo").assertIsDisplayed()
    }

    // HU-013 Escenario 2: confirmar la creación guarda el elemento, lo selecciona automáticamente
    // en "Activo" y el resto de los datos ya ingresados (Descripción) permanece intacto.
    @Test
    fun confirmarElAltaInlineDeUnActivoLoSeleccionaYConservaElRestoDelFormulario() {
        mount()
        val descripcion = "Entré por ruptura de rango, journey EP-002-b"
        composeTestRule.onNodeWithText("Descripción entrada").performTextInput(descripcion)

        abrirAltaInline("Activo")
        composeTestRule.onNodeWithText("Nuevo valor para Activo").performTextInput("EURJPY-inline")
        composeTestRule.onNodeWithText("Agregar").performClick()
        waitUntil { viewModel.state.value.assetId != null }

        composeTestRule.onNodeWithText("EURJPY-inline").assertIsDisplayed()
        // "Descripción entrada" quedó fuera del viewport tras el scrollTo("Activo") de
        // abrirAltaInline -- mismo criterio que el resto del formulario: scrollTo() antes de
        // verificar visibilidad real en pantalla, no solo existencia en el árbol.
        composeTestRule.onNodeWithText(descripcion).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Nuevo valor para Activo").assertDoesNotExist()
    }

    // HU-013 Escenario 3: nombre duplicado -- el campo inline permanece abierto con el mensaje de
    // HU-010, sin interrumpir ni navegar fuera del formulario y sin tocar el resto de los datos.
    @Test
    fun confirmarElAltaInlineConNombreDuplicadoMantieneElCampoAbiertoConElMensajeDeError() {
        seedAsset("XAUUSD", isDefault = true)
        mount()
        val descripcion = "No se pierde al fallar el alta inline"
        composeTestRule.onNodeWithText("Descripción entrada").performTextInput(descripcion)

        abrirAltaInline("Activo")
        composeTestRule.onNodeWithText("Nuevo valor para Activo").performTextInput("XAUUSD")
        composeTestRule.onNodeWithText("Agregar").performClick()
        waitUntil { viewModel.state.value.addCatalogError != null }

        composeTestRule.onNodeWithText("Este valor ya existe en el catálogo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nuevo valor para Activo").assertIsDisplayed()
        // Mismo criterio que en confirmarElAltaInlineDeUnActivoLoSeleccionaYConservaElRestoDelFormulario:
        // scrollTo() antes de verificar, la posición de scroll cambió al abrir el dropdown de "Activo".
        composeTestRule.onNodeWithText(descripcion).performScrollTo().assertIsDisplayed()
    }

    // HU-013 Escenario 4: cancelar el campo inline sin escribir nada no crea ningún elemento y el
    // selector vuelve a su estado anterior.
    @Test
    fun cancelarElCampoInlineCierraElCampoSinCrearNingunElemento() {
        mount()

        abrirAltaInline("Activo")
        composeTestRule.onNodeWithText("Cancelar").performClick()

        composeTestRule.onNodeWithText("Nuevo valor para Activo").assertDoesNotExist()
        assert(viewModel.state.value.assetId == null)
    }
}
