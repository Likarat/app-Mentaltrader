package com.miguel.mentaltrader.feature.inicio

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.data.InicioFilterRepository
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * HU-037/HU-038 (nuevo slice, post-construccion): gate `fidelity` -- verificación visual REAL
 * (no por inspección de código) de la gráfica interactiva y el alternador %/número, capturando
 * pantallazos reales del Composable `InicioScreen` corriendo en este dispositivo físico (misma
 * técnica de aislamiento que `OperationFormRenderedUiTest`: Room en memoria + DataStore real en un
 * archivo temporal propio, cero riesgo sobre datos reales). Los PNG se guardan en
 * `context.getExternalFilesDir(null)` para poder extraerlos con `adb pull` y observarlos.
 */
class InicioFidelityScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var viewModel: InicioViewModel
    private lateinit var outputDir: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        // Archivo UNICO por metodo de test (no un nombre fijo): DataStore no se cierra
        // explicitamente entre @Before de distintos @Test y falla con "multiple DataStores active
        // for the same file" si se reutiliza el mismo nombre.
        val dataStoreFile = File.createTempFile("inicio_fidelity_", ".preferences_pb", context.cacheDir)
        val dataStore = PreferenceDataStoreFactory.create { dataStoreFile }
        viewModel = InicioViewModel(database.operationDao(), InicioFilterRepository(dataStore))
        outputDir = context.getExternalFilesDir(null) ?: context.cacheDir

        runBlocking {
            val dao = database.operationDao()
            val resultados = listOf(
                ResultType.WIN, ResultType.WIN, ResultType.WIN, ResultType.WIN, ResultType.WIN, ResultType.WIN,
                ResultType.LOSS, ResultType.LOSS,
                ResultType.BREAK_EVEN
            )
            val signosR = listOf(2f, 1.5f, 0.5f, 1f, 2.5f, 0.8f, -1f, -2f, 0f)
            resultados.forEachIndexed { index, result ->
                dao.insert(
                    Operation(
                        dateTime = 1_700_000_000_000L + index * 86_400_000L,
                        assetId = 1L,
                        direction = Direction.BUY,
                        quality = 7f,
                        emotionBeforeId = 1L,
                        emotionAfterId = 1L,
                        errorId = 1L,
                        result = result,
                        resultInR = signosR[index],
                        entryDescription = "fidelity-test",
                        createdAt = 0L,
                        updatedAt = 0L
                    )
                )
            }
        }

        composeTestRule.setContent {
            InicioScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()
        // Deja que los Flow (metricsSummary/cumulativeSeries) emitan contra los datos ya insertados.
        Thread.sleep(500)
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun saveScreenshot(name: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(outputDir, name)
        java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun capturarModoPorcentajePorDefecto() {
        saveScreenshot("fidelity_01_modo_porcentaje.png")
    }

    @Test
    fun capturarModoNumeroTrasAlternar() {
        composeTestRule.onNodeWithText("Ver en número").performClick()
        composeTestRule.waitForIdle()

        saveScreenshot("fidelity_02_modo_numero.png")
    }

    @Test
    fun capturarTooltipDeLaGraficaDeRAcumuladoAlArrastrar() {
        composeTestRule.onNodeWithTag(RACUMULADO_CANVAS_TEST_TAG).performTouchInput {
            down(Offset(centerX, centerY))
            moveTo(Offset(0f, centerY))
        }
        composeTestRule.waitForIdle()

        saveScreenshot("fidelity_03_tooltip_racumulado.png")
    }
}
