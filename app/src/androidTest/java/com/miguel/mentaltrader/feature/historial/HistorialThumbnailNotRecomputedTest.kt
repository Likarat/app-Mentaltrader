package com.miguel.mentaltrader.feature.historial

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.data.HistorialFilterRepository
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationImage
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.ui.theme.MentaltraderTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

/**
 * HU-007 Escenario 2: la miniatura se genera una sola vez al guardar y NO se recalcula en cada
 * render/recomposición del listado. Se fuerza la recomposición COMPLETA de `HistorialScreen`
 * (vía `key()`, que garantiza que no se salte por optimización de Compose y se re-ejecute de
 * verdad el cuerpo del composable, incluida la fila de la operación con imagen) varias veces
 * seguidas y se confirma que el archivo de miniatura en disco no cambia ni un byte ni su fecha
 * de modificación. Complementa la garantía de código (`HistorialScreen.kt` solo invoca
 * `ImageProcessor.thumbnailPathFor()`, una función pura de derivación de ruta, nunca
 * `processAndSave()`, en su camino de render).
 */
class HistorialThumbnailNotRecomputedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun laMiniaturaNoSeRecalculaEnRecomposicionesSucesivas() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val imageProcessor = ImageProcessor(context)
        val viewModel = HistorialViewModel(
            database.operationDao(),
            database.operationImageDao(),
            database.catalogItemDao(),
            HistorialFilterRepository(context)
        )

        val filePath = runBlocking {
            val operationId = database.operationDao().insert(
                Operation(
                    dateTime = 0L, assetId = 1L, direction = Direction.BUY, quality = 8f,
                    emotionBeforeId = 1L, emotionAfterId = 1L, errorId = 1L,
                    result = ResultType.WIN, entryDescription = "test miniatura",
                    createdAt = 0L, updatedAt = 0L
                )
            )
            val random = java.util.Random(7)
            val pixels = IntArray(400 * 300) { random.nextInt() }
            val bitmap = Bitmap.createBitmap(pixels, 400, 300, Bitmap.Config.ARGB_8888)
            val sourceFile = File(context.cacheDir, "fuente_miniatura_test.jpg")
            FileOutputStream(sourceFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            val processed = imageProcessor.processAndSave(Uri.fromFile(sourceFile))!!
            database.operationImageDao().insert(
                OperationImage(operationId = operationId, filePath = processed.filePath, position = 0, createdAt = 0L)
            )
            processed.filePath
        }

        val thumbnailFile = File(context.filesDir, ImageProcessor.thumbnailPathFor(filePath))
        val bytesAntes = thumbnailFile.readBytes()
        val lastModifiedAntes = thumbnailFile.lastModified()

        val recomposeTrigger = mutableIntStateOf(0)
        composeTestRule.setContent {
            MentaltraderTheme {
                Column {
                    // `key()` fuerza que Compose descarte y vuelva a montar TODO el subárbol
                    // (no puede "saltarse" la recomposición por igualdad estructural): garantiza
                    // que el cuerpo de HistorialScreen/OperationRow se re-ejecuta de verdad en
                    // cada uno de los 8 renders forzados a continuación.
                    key(recomposeTrigger.intValue) {
                        HistorialScreen(viewModel = viewModel)
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Miniatura de la operación").assertExists()

        repeat(8) {
            recomposeTrigger.intValue++
            composeTestRule.waitForIdle()
        }

        val bytesDespues = thumbnailFile.readBytes()
        assertEquals(
            "La miniatura no debe recalcularse: bytes idénticos tras 8 recomposiciones/remounts completos",
            bytesAntes.toList(),
            bytesDespues.toList()
        )
        assertEquals(lastModifiedAntes, thumbnailFile.lastModified())

        database.close()
    }
}
