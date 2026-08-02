package com.miguel.mentaltrader.feature.etiquetas

import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogRepository
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.testutil.FakeCatalogItemDao
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
import org.junit.Test

/**
 * Tests unitarios de EtiquetasViewModel (tasks.md 1.7, sub-slice EP-002-a): manejo de estado de
 * agregar/editar/eliminar sobre CatalogRepository real (con FakeCatalogItemDao). La lógica de
 * negocio en sí (unicidad, protección de semillas) ya la cubre CatalogRepositoryTest — aquí solo
 * se verifica que el ViewModel refleje sus resultados en el estado de UI correctamente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EtiquetasViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: FakeCatalogItemDao
    private lateinit var viewModel: EtiquetasViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeCatalogItemDao()
        viewModel = EtiquetasViewModel(dao, CatalogRepository(dao))
    }

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
}
