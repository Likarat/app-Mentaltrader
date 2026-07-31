package com.miguel.mentaltrader

import android.graphics.Bitmap
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.feature.registro.OperationFormScreen
import com.miguel.mentaltrader.feature.registro.OperationFormViewModel
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * HU-006-AC1/AC2/AC3: a diferencia de `OperationFormImageProcessingTest` (que solo verifica el
 * `StateFlow` del ViewModel), esta clase MONTA `OperationFormScreen` de verdad con
 * `ComposeTestRule` + un `OperationFormViewModel` real (Room in-memory real, `ImageProcessor` con
 * contexto Android real) y asserta sobre NODOS RENDERIZADOS (`onNode*`), nunca sobre
 * `viewModel.state.value` directamente -- el AC exige que el usuario VEA algo, no solo que el
 * estado interno cambie.
 *
 * La selección real desde cámara/galería pasa por `ActivityResultContracts` (fuera del control de
 * un test instrumentado sin fixtures de Intents); igual que en `OperationFormImageProcessingTest`,
 * se simula el resultado de esa selección invocando `viewModel.onImageAdded(uri)` directamente con
 * una imagen real en disco -- lo que se verifica aquí es el paso posterior, ya cubierto por el
 * gap detectado: que ese cambio de estado se traduzca en algo renderizado y visible.
 */
class OperationFormRenderedUiTest {

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
        composeTestRule.setContent {
            OperationFormScreen(viewModel = viewModel, onSaved = {})
        }
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            check(System.currentTimeMillis() - start <= timeoutMs) { "Timeout esperando la condición" }
            Thread.sleep(20)
            composeTestRule.waitForIdle()
        }
    }

    /** Genera un archivo JPEG real (no un stub) en `cacheDir`, igual que
     * `OperationFormImageProcessingTest.realImageFile`. */
    private fun realImageFile(width: Int = 200, height: Int = 150, name: String = "${UUID.randomUUID()}.jpg"): Uri {
        val random = java.util.Random(7)
        val pixels = IntArray(width * height) { random.nextInt() }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return Uri.fromFile(file)
    }

    // HU-006 Escenario 1: tras adjuntar una imagen, se VE la miniatura sin procesar en el
    // formulario (no solo pendingImages.size == 1 en el estado interno).
    @Test
    fun trasAdjuntarUnaImagenSeVeLaMiniaturaSinProcesarEnElFormulario() {
        val uri = realImageFile(name = "miniatura_visible.jpg")

        viewModel.onImageAdded(uri)
        waitUntil { viewModel.state.value.pendingImages.size == 1 }

        composeTestRule.onNodeWithContentDescription("Imagen adjunta sin procesar")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // HU-006 Escenario 2: al intentar adjuntar una 3ra imagen (límite = 2), se VE el mensaje de
    // error en pantalla.
    @Test
    fun alIntentarUnaTerceraImagenSeVeElMensajeDeLimite() {
        val uri1 = realImageFile(name = "limite_1.jpg")
        val uri2 = realImageFile(name = "limite_2.jpg")
        val uri3 = realImageFile(name = "limite_3.jpg")

        viewModel.onImageAdded(uri1)
        waitUntil { viewModel.state.value.pendingImages.size == 1 }
        viewModel.onImageAdded(uri2)
        waitUntil { viewModel.state.value.pendingImages.size == 2 }
        viewModel.onImageAdded(uri3)
        waitUntil { viewModel.state.value.imageError != null }

        composeTestRule.onNodeWithText("Ya adjuntaste el máximo de 2 imágenes")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // HU-006 Escenario 3: al rechazar el permiso de cámara, se VE el mensaje de error
    // correspondiente y los botones Cámara/Galería siguen presentes e interactuables.
    @Test
    fun alRechazarElPermisoDeCamaraSeVeElMensajeYLosBotonesSiguenDisponibles() {
        viewModel.onCameraPermissionDenied()
        waitUntil { viewModel.state.value.imageError != null }

        composeTestRule.onNodeWithText(
            "Permiso de cámara rechazado. Podés continuar sin imagen o adjuntar desde la galería."
        ).performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("Cámara").performScrollTo().assertIsDisplayed().assertHasClickAction()
        composeTestRule.onNodeWithText("Galería").performScrollTo().assertIsDisplayed().assertHasClickAction()
    }
}
