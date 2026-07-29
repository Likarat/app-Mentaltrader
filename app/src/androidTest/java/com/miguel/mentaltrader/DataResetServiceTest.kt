package com.miguel.mentaltrader

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.DataResetService
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationImage
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * "Borrar todo" (feature de Ajustes agregada tras el pedido del usuario, 2026-07-29): verifica
 * contra Room + filesystem reales que se borran operaciones e imágenes (filas y archivos) sin
 * tocar los catálogos.
 */
@RunWith(AndroidJUnit4::class)
class DataResetServiceTest {

    private lateinit var database: AppDatabase
    private lateinit var context: android.content.Context
    private lateinit var resetService: DataResetService

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        resetService = DataResetService(database.operationDao(), database.operationImageDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun borrarTodo_eliminaOperacionesEImagenesPeroConservaLosCatalogos() = runBlocking {
        val assetId = database.catalogItemDao().insert(
            CatalogItem(type = CatalogType.ASSET, name = "XAUUSD", isDefault = true, createdAt = 0L, updatedAt = 0L)
        )
        val emotionId = database.catalogItemDao().insert(
            CatalogItem(type = CatalogType.EMOTION, name = "Confianza", isDefault = true, createdAt = 0L, updatedAt = 0L)
        )
        val errorId = database.catalogItemDao().insert(
            CatalogItem(type = CatalogType.ERROR, name = "Ninguno", isDefault = true, createdAt = 0L, updatedAt = 0L)
        )

        val operationId = database.operationDao().insert(
            Operation(
                dateTime = 0L, assetId = assetId, direction = Direction.BUY, quality = 8f,
                emotionBeforeId = emotionId, emotionAfterId = emotionId, errorId = errorId,
                result = ResultType.WIN, entryDescription = "test", createdAt = 0L, updatedAt = 0L
            )
        )

        val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
        val imageFile = File(imagesDir, "test.jpg").apply { writeText("fake-image") }
        val thumbFile = File(imagesDir, "test_thumb.jpg").apply { writeText("fake-thumb") }
        database.operationImageDao().insert(
            OperationImage(operationId = operationId, filePath = "images/test.jpg", position = 0, createdAt = 0L)
        )

        resetService.deleteAllOperationsAndImages()

        assertTrue(database.operationDao().getAllOrderedByDateDesc().first().isEmpty())
        assertTrue(database.operationImageDao().getAll().isEmpty())
        assertFalse(imageFile.exists())
        assertFalse(thumbFile.exists())

        // Los catálogos NO se tocan.
        assertEquals(1, database.catalogItemDao().countByType(CatalogType.ASSET))
        assertEquals(1, database.catalogItemDao().countByType(CatalogType.EMOTION))
        assertEquals(1, database.catalogItemDao().countByType(CatalogType.ERROR))
    }
}
