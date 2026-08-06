package com.miguel.mentaltrader.core.data

import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.testutil.FakeCatalogItemDao
import com.miguel.mentaltrader.testutil.FakeOperationDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios de CatalogRepository (tasks.md 1.7/3.2, sub-slices EP-002-a/c): unicidad case-
 * insensitive/trim por tipo de catálogo (HU-010), protección/recreación de semillas (HU-011),
 * conteo de uso por operaciones reales (HU-012). Usa fakes de CatalogItemDao/OperationDao en vez
 * de Room (Room se ejercita en los tests instrumentados).
 */
class CatalogRepositoryTest {

    private val dao = FakeCatalogItemDao()
    private val operationDao = FakeOperationDao()
    private val repository = CatalogRepository(dao, operationDao)

    // HU-010 Escenario 1
    @Test
    fun `agregar un elemento nuevo lo persiste y lo hace disponible`() = runTest {
        val result = repository.addItem(CatalogType.ASSET, "EURUSD")

        assertTrue(result is CatalogAddResult.Added)
        val added = (result as CatalogAddResult.Added).item
        assertEquals("EURUSD", added.name)
        assertEquals(1, dao.itemsOfType(CatalogType.ASSET).size)
    }

    // HU-010 Escenario 2
    @Test
    fun `agregar un elemento duplicado case-insensitive y con espacios lo impide`() = runTest {
        repository.addItem(CatalogType.ASSET, "EURUSD")

        val result = repository.addItem(CatalogType.ASSET, "  eurusd  ")

        assertEquals(CatalogAddResult.Duplicate, result)
        assertEquals(1, dao.itemsOfType(CatalogType.ASSET).size)
    }

    // HU-010 Escenario 3
    @Test
    fun `el mismo nombre es valido en catalogos de tipo distinto`() = runTest {
        repository.addItem(CatalogType.ERROR, "Noticias")

        val result = repository.addItem(CatalogType.ASSET, "Noticias")

        assertTrue(result is CatalogAddResult.Added)
    }

    // HU-011 Escenario 1
    @Test
    fun `editar un elemento existente actualiza su nombre`() = runTest {
        val added = (repository.addItem(CatalogType.ASSET, "EURUSD") as CatalogAddResult.Added).item

        val result = repository.updateItem(added, "GBPUSD")

        assertEquals(CatalogUpdateResult.Updated, result)
        assertEquals("GBPUSD", dao.getById(added.id)?.name)
    }

    @Test
    fun `editar renombrando al mismo nombre no se considera duplicado contra si mismo`() = runTest {
        val added = (repository.addItem(CatalogType.ASSET, "EURUSD") as CatalogAddResult.Added).item

        val result = repository.updateItem(added, "eurusd")

        assertEquals(CatalogUpdateResult.Updated, result)
    }

    @Test
    fun `editar renombrando a un nombre ya usado por otro elemento lo impide`() = runTest {
        repository.addItem(CatalogType.ASSET, "EURUSD")
        val second = (repository.addItem(CatalogType.ASSET, "GBPUSD") as CatalogAddResult.Added).item

        val result = repository.updateItem(second, "EURUSD")

        assertEquals(CatalogUpdateResult.Duplicate, result)
    }

    // HU-011 Escenario 2
    @Test
    fun `eliminar un elemento sin uso y sin proteccion de semilla lo elimina de inmediato`() = runTest {
        dao.seed(CatalogType.ASSET, "XAUUSD", isDefault = true)
        val added = (repository.addItem(CatalogType.ASSET, "EURUSD") as CatalogAddResult.Added).item

        val result = repository.deleteItem(added)

        assertEquals(CatalogDeleteResult.Deleted, result)
        assertNull(dao.getById(added.id))
    }

    // HU-011 Escenario 3 (Ninguno, ERROR)
    @Test
    fun `eliminar el elemento semilla Ninguno de Errores lo impide siempre`() = runTest {
        val ninguno = dao.seed(CatalogType.ERROR, CatalogItem.SEED_ERROR_NINGUNO, isDefault = true)

        val result = repository.deleteItem(dao.getById(ninguno)!!)

        assertTrue(result is CatalogDeleteResult.Blocked)
        assertTrue(dao.getById(ninguno) != null)
    }

    // HU-011 Escenario 3 (XAUUSD, único activo)
    @Test
    fun `eliminar XAUUSD cuando es el unico activo lo impide`() = runTest {
        val xauusd = dao.seed(CatalogType.ASSET, CatalogItem.SEED_ASSET_XAUUSD, isDefault = true)

        val result = repository.deleteItem(dao.getById(xauusd)!!)

        assertTrue(result is CatalogDeleteResult.Blocked)
    }

    @Test
    fun `eliminar XAUUSD permitido cuando existe otro activo`() = runTest {
        val xauusd = dao.seed(CatalogType.ASSET, CatalogItem.SEED_ASSET_XAUUSD, isDefault = true)
        dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)

        val result = repository.deleteItem(dao.getById(xauusd)!!)

        assertEquals(CatalogDeleteResult.Deleted, result)
    }

    @Test
    fun `eliminar una emocion semilla no esta protegida`() = runTest {
        dao.seed(CatalogType.EMOTION, "Confianza", isDefault = true)
        val calma = dao.seed(CatalogType.EMOTION, "Calma", isDefault = true)

        val result = repository.deleteItem(dao.getById(calma)!!)

        assertEquals(CatalogDeleteResult.Deleted, result)
    }

    // HU-011 Escenario 4: el usuario borra XAUUSD mientras existe otro activo (permitido), y luego
    // borra también ese otro activo -> el catálogo queda en 0 -> se recrea la semilla.
    @Test
    fun `vaciar por completo el catalogo de activos recrea automaticamente XAUUSD`() = runTest {
        val xauusd = dao.seed(CatalogType.ASSET, CatalogItem.SEED_ASSET_XAUUSD, isDefault = true)
        val eurusd = dao.seed(CatalogType.ASSET, "EURUSD", isDefault = false)

        repository.deleteItem(dao.getById(xauusd)!!)
        assertEquals(1, dao.itemsOfType(CatalogType.ASSET).size)

        repository.deleteItem(dao.getById(eurusd)!!)

        val activos = dao.itemsOfType(CatalogType.ASSET)
        assertEquals(1, activos.size)
        assertEquals(CatalogItem.SEED_ASSET_XAUUSD, activos.first().name)
        assertTrue(activos.first().isDefault)
    }

    // HU-012 Escenario 1 (a nivel de repositorio): cuenta operaciones que usan el elemento, en
    // cualquiera de sus 4 roles posibles (Activo, Emoción antes, Emoción después, Error).
    @Test
    fun `usageCountOf cuenta las operaciones que usan el elemento en cualquiera de sus 4 roles`() = runTest {
        val asset = (repository.addItem(CatalogType.ASSET, "EURUSD") as CatalogAddResult.Added).item
        val emotion = (repository.addItem(CatalogType.EMOTION, "Confianza") as CatalogAddResult.Added).item
        val error = (repository.addItem(CatalogType.ERROR, "Ninguno") as CatalogAddResult.Added).item
        operationDao.insert(operacionDe(assetId = asset.id, emotionId = emotion.id, errorId = error.id))
        operationDao.insert(operacionDe(assetId = asset.id, emotionId = emotion.id, errorId = error.id))

        assertEquals(2, repository.usageCountOf(asset))
        assertEquals(2, repository.usageCountOf(emotion))
        assertEquals(2, repository.usageCountOf(error))
    }

    @Test
    fun `usageCountOf es 0 para un elemento sin ninguna operacion asociada`() = runTest {
        val sinUso = (repository.addItem(CatalogType.ASSET, "SinUso") as CatalogAddResult.Added).item

        assertEquals(0, repository.usageCountOf(sinUso))
    }

    // Fix (reemplaza el antiguo HU-012 Escenario 3): un elemento en uso ya NO se puede eliminar --
    // antes se borraba igual dejando operaciones con una referencia huérfana; ahora deleteItem lo
    // bloquea y conserva tanto el elemento del catálogo como la operación que lo usaba, intactos.
    @Test
    fun `eliminar un elemento en uso lo bloquea y no toca ni el catalogo ni la operacion`() = runTest {
        val asset = (repository.addItem(CatalogType.ASSET, "EURUSD") as CatalogAddResult.Added).item
        val emotion = (repository.addItem(CatalogType.EMOTION, "Confianza") as CatalogAddResult.Added).item
        val error = (repository.addItem(CatalogType.ERROR, "Ninguno") as CatalogAddResult.Added).item
        val opId = operationDao.insert(operacionDe(assetId = asset.id, emotionId = emotion.id, errorId = error.id))

        val result = repository.deleteItem(asset)

        assertTrue(result is CatalogDeleteResult.Blocked)
        assertTrue(dao.getById(asset.id) != null)
        assertEquals(asset.id, operationDao.getById(opId)!!.assetId)
    }

    @Test
    fun `eliminar un elemento en uso bloquea tambien para emociones y errores`() = runTest {
        val emotion = (repository.addItem(CatalogType.EMOTION, "Confianza") as CatalogAddResult.Added).item
        operationDao.insert(operacionDe(assetId = 1L, emotionId = emotion.id, errorId = 1L))

        val result = repository.deleteItem(emotion)

        assertTrue(result is CatalogDeleteResult.Blocked)
        assertTrue(dao.getById(emotion.id) != null)
    }

    @Test
    fun `usagePreviewOf devuelve las operaciones que usan el elemento de la mas reciente a la mas antigua`() = runTest {
        val asset = (repository.addItem(CatalogType.ASSET, "EURUSD") as CatalogAddResult.Added).item
        val opAntigua = operationDao.insert(operacionDe(assetId = asset.id, emotionId = 1L, errorId = 1L, dateTime = 100L))
        val opReciente = operationDao.insert(operacionDe(assetId = asset.id, emotionId = 1L, errorId = 1L, dateTime = 200L))

        val preview = repository.usagePreviewOf(asset)

        assertEquals(listOf(opReciente, opAntigua), preview.map { it.id })
    }

    @Test
    fun `usagePreviewOf respeta el limite acotado`() = runTest {
        val asset = (repository.addItem(CatalogType.ASSET, "EURUSD") as CatalogAddResult.Added).item
        repeat(3) { operationDao.insert(operacionDe(assetId = asset.id, emotionId = 1L, errorId = 1L, dateTime = it.toLong())) }

        val preview = repository.usagePreviewOf(asset, limit = 2)

        assertEquals(2, preview.size)
    }

    private fun operacionDe(assetId: Long, emotionId: Long, errorId: Long, dateTime: Long = 0L) = Operation(
        dateTime = dateTime,
        assetId = assetId,
        direction = Direction.BUY,
        quality = 5f,
        emotionBeforeId = emotionId,
        emotionAfterId = emotionId,
        errorId = errorId,
        result = ResultType.WIN,
        entryDescription = "test",
        createdAt = 0L,
        updatedAt = 0L
    )
}
