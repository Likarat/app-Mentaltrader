package com.miguel.mentaltrader

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.feature.registro.OperationFormState
import com.miguel.mentaltrader.feature.registro.OperationFormViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados de persistencia real (tasks.md 1.11, sub-slice EP-001-a). A diferencia
 * de OperationFormViewModelTest (fakes, JVM), aquí se ejercita el OperationDao/CatalogItemDao
 * reales generados por KSP contra una base de datos Room real (in-memory, aislada por test,
 * corriendo en un dispositivo/emulador real) — cierra INT-form-persistencia e
 * INT-room-real-entities. La siembra automática (INT-catalog-seed-emotion) se verifica aparte
 * en AppDatabaseSeedTest, sobre la base de datos real de producción.
 */
@RunWith(AndroidJUnit4::class)
class OperationFormPersistenceTest {

    private lateinit var database: AppDatabase
    private lateinit var operationDao: OperationDao
    private lateinit var catalogItemDao: CatalogItemDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        operationDao = database.operationDao()
        catalogItemDao = database.catalogItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun seed(type: CatalogType, name: String): Long = runBlocking {
        catalogItemDao.insert(
            CatalogItem(type = type, name = name, isDefault = true, createdAt = 0L, updatedAt = 0L)
        )
    }

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            check(System.currentTimeMillis() - start <= timeoutMs) { "Timeout esperando la condición" }
            Thread.sleep(20)
        }
    }

    // HU-001 Esc.1 + HU-002 Esc.1 + HU-003 Esc.1 + INT-form-listado-minimo
    @Test
    fun guardarOperacionCompleta_persisteEnRoomYApareceEnElListado() {
        val assetId = seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = seed(CatalogType.EMOTION, "Confianza")
        val errorId = seed(CatalogType.ERROR, "Ninguno")
        val viewModel = OperationFormViewModel(operationDao, catalogItemDao)

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange("Entré por ruptura de rango")
        viewModel.save()

        waitUntil { viewModel.state.value.isSaved }

        val operations = runBlocking { operationDao.getAllOrderedByDateDesc().first() }
        assertEquals(1, operations.size)
        assertEquals(assetId, operations.first().assetId)
        assertEquals("Entré por ruptura de rango", operations.first().entryDescription)
    }

    // HU-001 Esc.2 + HU-002 Esc.2: campo obligatorio faltante no persiste nada en Room real.
    @Test
    fun guardarSinCampoObligatorio_noPersisteNadaEnRoom() {
        val assetId = seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = seed(CatalogType.EMOTION, "Confianza")
        val viewModel = OperationFormViewModel(operationDao, catalogItemDao)

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        // Error queda sin seleccionar a propósito.
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange("texto")
        viewModel.save()

        // No hay isSaved que esperar (el guardado se bloquea antes de lanzar la corrutina);
        // se da un margen corto y se confirma que Room real sigue vacío.
        Thread.sleep(200)
        val operations = runBlocking { operationDao.getAllOrderedByDateDesc().first() }
        assertTrue(operations.isEmpty())
        assertFalse(viewModel.state.value.isSaved)
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_ERROR))
    }

    // HU-002 Esc.5: motivos en texto libre se persisten tal cual en Room real.
    @Test
    fun guardarConMotivosEnTextoLibre_sePersistenTalCualEnRoom() {
        val assetId = seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = seed(CatalogType.EMOTION, "Confianza")
        val errorId = seed(CatalogType.ERROR, "Ninguno")
        val viewModel = OperationFormViewModel(operationDao, catalogItemDao)

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionBeforeReasonChange("Vi la señal clara en H4")
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onEmotionAfterReasonChange("Cerré satisfecho con el resultado")
        viewModel.onErrorSelected(errorId)
        viewModel.onErrorReasonChange("Ninguno detectado en esta ejecución")
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange("Entré por ruptura de rango")
        viewModel.save()

        waitUntil { viewModel.state.value.isSaved }

        val saved = runBlocking { operationDao.getAllOrderedByDateDesc().first() }.single()
        assertEquals("Vi la señal clara en H4", saved.emotionBeforeReason)
        assertEquals("Cerré satisfecho con el resultado", saved.emotionAfterReason)
        assertEquals("Ninguno detectado en esta ejecución", saved.errorReason)
    }

    // HU-003 Esc.2: descripción vacía no persiste en Room real.
    @Test
    fun guardarConDescripcionVacia_noPersisteNadaEnRoom() {
        val assetId = seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = seed(CatalogType.EMOTION, "Confianza")
        val errorId = seed(CatalogType.ERROR, "Ninguno")
        val viewModel = OperationFormViewModel(operationDao, catalogItemDao)

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        // entryDescription queda vacío a propósito.
        viewModel.save()

        Thread.sleep(200)
        val operations = runBlocking { operationDao.getAllOrderedByDateDesc().first() }
        assertTrue(operations.isEmpty())
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_DESCRIPTION))
    }

    // HU-003 Esc.3: texto largo con múltiples saltos de línea, sin truncar (round-trip real por SQLite).
    @Test
    fun guardarDescripcionLargaConSaltosDeLinea_sePersisteCompletaSinTruncarEnRoom() {
        val assetId = seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = seed(CatalogType.EMOTION, "Confianza")
        val errorId = seed(CatalogType.ERROR, "Ninguno")
        val viewModel = OperationFormViewModel(operationDao, catalogItemDao)
        val textoLargo = (1..200).joinToString("\n") {
            "Párrafo $it: análisis detallado de la operación y del estado emocional."
        }

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange(textoLargo)
        viewModel.save()

        waitUntil { viewModel.state.value.isSaved }

        val saved = runBlocking { operationDao.getAllOrderedByDateDesc().first() }.single()
        assertEquals(textoLargo, saved.entryDescription)
        assertEquals(200, saved.entryDescription.lines().size)
    }

    // HU-001 Esc.3 + HU-002 Esc.3/4: catálogo recién inicializado, solo con valores semilla.
    @Test
    fun guardarConSoloValoresSemillaDeActivoEmocionYError_persisteConLosIdsSemilla() {
        val assetId = seed(CatalogType.ASSET, CatalogItem.SEED_ASSET_XAUUSD)
        val emotionBeforeId = seed(CatalogType.EMOTION, CatalogItem.SEED_EMOTIONS.first())
        val emotionAfterId = seed(CatalogType.EMOTION, CatalogItem.SEED_EMOTIONS.last())
        val errorId = seed(CatalogType.ERROR, CatalogItem.SEED_ERROR_NINGUNO)
        // Confirma que en este catálogo recién inicializado no hay otro elemento manual.
        assertEquals(1, runBlocking { catalogItemDao.countByType(CatalogType.ASSET) })
        assertEquals(1, runBlocking { catalogItemDao.countByType(CatalogType.ERROR) })

        val viewModel = OperationFormViewModel(operationDao, catalogItemDao)
        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.SELL)
        viewModel.onQualityChange("6.0")
        viewModel.onEmotionBeforeSelected(emotionBeforeId)
        viewModel.onEmotionAfterSelected(emotionAfterId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.LOSS)
        viewModel.onDescriptionChange("Operación con solo semillas de catálogo")
        viewModel.save()

        waitUntil { viewModel.state.value.isSaved }

        val saved = runBlocking { operationDao.getAllOrderedByDateDesc().first() }.single()
        assertEquals(assetId, saved.assetId)
        assertEquals(emotionBeforeId, saved.emotionBeforeId)
        assertEquals(emotionAfterId, saved.emotionAfterId)
        assertEquals(errorId, saved.errorId)
    }
}
