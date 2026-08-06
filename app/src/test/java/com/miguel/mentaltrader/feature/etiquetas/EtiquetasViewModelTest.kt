package com.miguel.mentaltrader.feature.etiquetas

import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogRepository
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationImage
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.testutil.FakeCatalogItemDao
import com.miguel.mentaltrader.testutil.FakeOperationDao
import com.miguel.mentaltrader.testutil.FakeOperationImageDao
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests unitarios de EtiquetasViewModel (tasks.md 1.7/4.4, sub-slices EP-002-a/d): manejo de
 * estado de agregar/editar/eliminar sobre CatalogRepository real (con FakeCatalogItemDao) y del
 * espacio en disco de HU-014. La lógica de negocio en sí (unicidad, protección de semillas,
 * agregación de bytes) ya la cubren CatalogRepositoryTest/DiskSpaceCalculatorTest — aquí solo se
 * verifica que el ViewModel refleje sus resultados en el estado de UI correctamente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EtiquetasViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: FakeCatalogItemDao
    private lateinit var operationDao: FakeOperationDao
    private lateinit var operationImageDao: FakeOperationImageDao
    private lateinit var viewModel: EtiquetasViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeCatalogItemDao()
        operationDao = FakeOperationDao()
        operationImageDao = FakeOperationImageDao()
        viewModel = createViewModel()
    }

    private fun createViewModel(imageDao: FakeOperationImageDao = operationImageDao): EtiquetasViewModel =
        EtiquetasViewModel(
            dao,
            CatalogRepository(dao, operationDao),
            imageDao,
            tempFolder.root,
            ioDispatcher = dispatcher
        )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `agregar un elemento exitoso limpia el campo y no deja error`() = runTest {
        viewModel.onAddFieldChange("EURUSD")

        viewModel.onConfirmAdd()

        assertEquals("", viewModel.state.value.addFieldValue)
        assertNull(viewModel.state.value.addError)
        assertEquals(1, viewModel.assets.value.size)
    }

    @Test
    fun `agregar un elemento duplicado deja el error visible sin limpiar el campo`() = runTest {
        viewModel.onAddFieldChange("EURUSD")
        viewModel.onConfirmAdd()
        viewModel.onAddFieldChange("eurusd")

        viewModel.onConfirmAdd()

        assertEquals(CatalogRepository.DUPLICATE_MESSAGE, viewModel.state.value.addError)
        assertEquals("eurusd", viewModel.state.value.addFieldValue)
        assertEquals(1, viewModel.assets.value.size)
    }

    @Test
    fun `cambiar de pestana limpia el campo de agregar y su error`() = runTest {
        viewModel.onAddFieldChange("EURUSD")
        viewModel.onConfirmAdd()
        viewModel.onAddFieldChange("eurusd")
        viewModel.onConfirmAdd()
        assertTrue(viewModel.state.value.addError != null)

        viewModel.onTypeSelected(CatalogType.EMOTION)

        assertEquals(CatalogType.EMOTION, viewModel.state.value.selectedType)
        assertEquals("", viewModel.state.value.addFieldValue)
        assertNull(viewModel.state.value.addError)
    }

    @Test
    fun `editar un elemento exitoso cierra el modo edicion`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)
        viewModel.onStartEdit(dao.getById(id)!!)

        viewModel.onEditFieldChange("GBPUSD")
        viewModel.onConfirmEdit()

        assertNull(viewModel.state.value.editingItem)
        assertEquals("GBPUSD", dao.getById(id)?.name)
    }

    @Test
    fun `editar a un nombre duplicado mantiene el modo edicion con error`() = runTest {
        dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)
        val id = dao.seed(CatalogType.ASSET, "GBPUSD", isDefault = false)
        viewModel.onStartEdit(dao.getById(id)!!)

        viewModel.onEditFieldChange("EURUSD")
        viewModel.onConfirmEdit()

        assertEquals(CatalogRepository.DUPLICATE_MESSAGE, viewModel.state.value.editError)
        assertTrue(viewModel.state.value.editingItem != null)
    }

    @Test
    fun `cancelar la edicion limpia el estado sin modificar el elemento`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)
        viewModel.onStartEdit(dao.getById(id)!!)
        viewModel.onEditFieldChange("GBPUSD")

        viewModel.onCancelEdit()

        assertNull(viewModel.state.value.editingItem)
        assertEquals("EURUSD", dao.getById(id)?.name)
    }

    @Test
    fun `solicitar eliminar un elemento lo deja pendiente de confirmacion sin eliminarlo todavia`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)

        viewModel.onRequestDelete(dao.getById(id)!!)

        assertEquals(id, viewModel.state.value.pendingDeleteItem?.id)
        assertTrue(dao.getById(id) != null)
    }

    @Test
    fun `cancelar la eliminacion pendiente no elimina el elemento y limpia el estado`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)
        viewModel.onRequestDelete(dao.getById(id)!!)

        viewModel.onCancelDelete()

        assertNull(viewModel.state.value.pendingDeleteItem)
        assertTrue(dao.getById(id) != null)
    }

    @Test
    fun `confirmar la eliminacion de un elemento permitido lo elimina sin dejar mensaje de bloqueo`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)
        viewModel.onRequestDelete(dao.getById(id)!!)

        viewModel.onConfirmDelete()

        assertNull(viewModel.state.value.pendingDeleteItem)
        assertNull(viewModel.state.value.blockedDeleteMessage)
        assertNull(dao.getById(id))
    }

    @Test
    fun `confirmar la eliminacion de una semilla protegida deja el mensaje de bloqueo visible`() = runTest {
        val ninguno = dao.seed(CatalogType.ERROR, CatalogItem.SEED_ERROR_NINGUNO, isDefault = true)
        viewModel.onRequestDelete(dao.getById(ninguno)!!)

        viewModel.onConfirmDelete()

        assertNull(viewModel.state.value.pendingDeleteItem)
        assertTrue(viewModel.state.value.blockedDeleteMessage != null)
        assertTrue(dao.getById(ninguno) != null)
    }

    @Test
    fun `descartar el mensaje de bloqueo lo limpia del estado`() = runTest {
        val ninguno = dao.seed(CatalogType.ERROR, CatalogItem.SEED_ERROR_NINGUNO, isDefault = true)
        viewModel.onRequestDelete(dao.getById(ninguno)!!)
        viewModel.onConfirmDelete()

        viewModel.onDismissBlockedDeleteMessage()

        assertNull(viewModel.state.value.blockedDeleteMessage)
    }

    // HU-012 Escenario 1: solicitar eliminar un elemento en uso deja el conteo de operaciones
    // disponible para el diálogo de confirmación (ya existente desde HU-011), sin eliminarlo.
    @Test
    fun `solicitar eliminar un elemento en uso deja el conteo de operaciones disponible sin eliminarlo`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)
        operationDao.insert(operacionConAsset(id))
        operationDao.insert(operacionConAsset(id))

        viewModel.onRequestDelete(dao.getById(id)!!)

        assertEquals(2, viewModel.state.value.pendingDeleteUsageCount)
        assertTrue(dao.getById(id) != null)
    }

    @Test
    fun `solicitar eliminar un elemento sin uso deja el conteo en 0`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)

        viewModel.onRequestDelete(dao.getById(id)!!)

        assertEquals(0, viewModel.state.value.pendingDeleteUsageCount)
    }

    // HU-012 Escenario 2: cancelar limpia también el conteo de uso, no solo el elemento pendiente.
    @Test
    fun `cancelar la eliminacion pendiente limpia tambien el conteo de uso`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)
        operationDao.insert(operacionConAsset(id))
        viewModel.onRequestDelete(dao.getById(id)!!)

        viewModel.onCancelDelete()

        assertNull(viewModel.state.value.pendingDeleteUsageCount)
    }

    // Fix (reemplaza el antiguo HU-012 Escenario 3): confirmar la eliminación de un elemento en
    // uso ya NO lo elimina -- queda bloqueado, con el elemento y la operación intactos. Este test
    // ejercita la defensa del ViewModel/Repository directamente (sin pasar por la UI, que ya no
    // ofrece "Eliminar" para un elemento en uso, ver `solicitar eliminar...`).
    @Test
    fun `confirmar la eliminacion de un elemento en uso lo bloquea y conserva el elemento y la operacion`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)
        val opId = operationDao.insert(operacionConAsset(id))
        viewModel.onRequestDelete(dao.getById(id)!!)

        viewModel.onConfirmDelete()

        assertNull(viewModel.state.value.pendingDeleteItem)
        assertNull(viewModel.state.value.pendingDeleteUsageCount)
        assertTrue(viewModel.state.value.blockedDeleteMessage != null)
        assertTrue(dao.getById(id) != null)
        assertEquals(id, operationDao.getById(opId)!!.assetId)
    }

    // Fix: solicitar eliminar un elemento en uso también trae la vista previa (fecha + activo) de
    // qué lo usa, para que la UI la muestre en el diálogo de bloqueo.
    @Test
    fun `solicitar eliminar un elemento en uso trae la vista previa con el nombre del activo`() = runTest {
        val assetId = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)
        operationDao.insert(operacionConAsset(assetId).copy(dateTime = 100L))

        viewModel.onRequestDelete(dao.getById(assetId)!!)

        val preview = viewModel.state.value.pendingDeleteUsagePreview
        assertEquals(1, preview.size)
        assertEquals("EURUSD", preview.first().assetName)
    }

    // Fix: solicitar eliminar un elemento sin uso no trae ninguna vista previa (no hace falta).
    @Test
    fun `solicitar eliminar un elemento sin uso deja la vista previa vacia`() = runTest {
        val id = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)

        viewModel.onRequestDelete(dao.getById(id)!!)

        assertTrue(viewModel.state.value.pendingDeleteUsagePreview.isEmpty())
    }

    private fun operacionConAsset(assetId: Long) = Operation(
        dateTime = 0L,
        assetId = assetId,
        direction = Direction.BUY,
        quality = 5f,
        emotionBeforeId = 1L,
        emotionAfterId = 1L,
        errorId = 1L,
        result = ResultType.WIN,
        entryDescription = "test",
        createdAt = 0L,
        updatedAt = 0L
    )

    // HU-014 Escenario 1: al abrir Etiquetas con imágenes reales, el estado expone el espacio
    // aproximado en disco (imagen + miniatura de cada OperationImage guardada).
    @Test
    fun `al abrir Etiquetas con imagenes reales expone el espacio total aproximado en disco`() = runTest {
        val filePath1 = crearImagenReal("foto1", bytesImagen = 1000, bytesThumbnail = 100)
        val filePath2 = crearImagenReal("foto2", bytesImagen = 2000, bytesThumbnail = 200)
        val images = FakeOperationImageDao()
        images.insert(OperationImage(operationId = 1L, filePath = filePath1, position = 0, createdAt = 0L))
        images.insert(OperationImage(operationId = 1L, filePath = filePath2, position = 1, createdAt = 0L))

        val vm = createViewModel(images)

        assertEquals(3300L, vm.state.value.diskSpaceBytes)
    }

    // HU-014 Escenario 2: sin ninguna imagen adjuntada, el espacio se muestra como 0 sin error.
    @Test
    fun `al abrir Etiquetas sin ninguna imagen el espacio en disco es 0`() = runTest {
        val vm = createViewModel(FakeOperationImageDao())

        assertEquals(0L, vm.state.value.diskSpaceBytes)
    }

    // HU-014 Escenario 3: tras eliminar una operación con imágenes (fila + archivo real
    // borrados), reabrir Etiquetas (= una nueva instancia del ViewModel, ya que HU-014 no
    // recalcula en tiempo real) refleja la reducción, sin contar los archivos ya eliminados.
    @Test
    fun `al reabrir Etiquetas tras eliminar una operacion con imagenes el espacio recalculado baja`() = runTest {
        val filePath1 = crearImagenReal("foto1", bytesImagen = 1000, bytesThumbnail = 100)
        val filePath2 = crearImagenReal("foto2", bytesImagen = 2000, bytesThumbnail = 200)
        val images = FakeOperationImageDao()
        images.insert(OperationImage(operationId = 1L, filePath = filePath1, position = 0, createdAt = 0L))
        images.insert(OperationImage(operationId = 2L, filePath = filePath2, position = 0, createdAt = 0L))
        val primeraApertura = createViewModel(images)
        assertEquals(3300L, primeraApertura.state.value.diskSpaceBytes)

        // Simula la eliminación real de la operación 1 y sus archivos -- sin UI de eliminar-
        // operación todavía (HU-021/EP-003 no construida), mismo patrón que DataResetServiceTest.
        File(tempFolder.root, filePath1).delete()
        File(tempFolder.root, ImageProcessor.thumbnailPathFor(filePath1)).delete()
        val imagesTrasEliminar = FakeOperationImageDao()
        imagesTrasEliminar.insert(OperationImage(operationId = 2L, filePath = filePath2, position = 0, createdAt = 0L))

        val segundaApertura = createViewModel(imagesTrasEliminar)

        assertEquals(2200L, segundaApertura.state.value.diskSpaceBytes)
    }

    @Test
    fun `formatDiskSpaceKbMb da 0 KB para cero bytes`() {
        assertEquals("0 KB", EtiquetasUiState.formatDiskSpaceKbMb(0L))
    }

    @Test
    fun `formatDiskSpaceKbMb formatea kilobytes sin decimales`() {
        assertEquals("2 KB", EtiquetasUiState.formatDiskSpaceKbMb(2048L))
    }

    @Test
    fun `formatDiskSpaceKbMb formatea megabytes con 1 decimal`() {
        assertEquals("1.5 MB", EtiquetasUiState.formatDiskSpaceKbMb((1.5 * 1024 * 1024).toLong()))
    }

    /** Crea un archivo de imagen real + su miniatura real bajo tempFolder (mismo convenio de
     * nombre que ImageProcessor: "<nombreBase>.jpg" + "<nombreBase>_thumb.jpg"), y devuelve la
     * ruta relativa de la imagen (la que se guarda en OperationImage.filePath). */
    private fun crearImagenReal(nombreBase: String, bytesImagen: Int, bytesThumbnail: Int): String {
        val dir = File(tempFolder.root, "images").apply { mkdirs() }
        File(dir, "$nombreBase.jpg").writeBytes(ByteArray(bytesImagen))
        File(dir, "${nombreBase}_thumb.jpg").writeBytes(ByteArray(bytesThumbnail))
        return "images/$nombreBase.jpg"
    }
}
