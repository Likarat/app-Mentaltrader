package com.miguel.mentaltrader.testutil

import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.model.CatalogType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake de CatalogItemDao compartido por los tests unitarios JVM (sin Room). [getByType] deriva de
 * un único [MutableStateFlow] de respaldo (no crea un Flow nuevo por llamada) para poder observar
 * altas/bajas/ediciones posteriores a la suscripción — igual que la Query reactiva real de Room.
 */
class FakeCatalogItemDao : CatalogItemDao {
    private val allItems = MutableStateFlow<List<CatalogItem>>(emptyList())
    private var nextId = 1L

    /** Atajo de test: siembra directamente un elemento de catálogo y devuelve su id. */
    fun seed(type: CatalogType, name: String, isDefault: Boolean = true): Long {
        val id = nextId++
        allItems.value += CatalogItem(id = id, type = type, name = name, isDefault = isDefault, createdAt = 0L, updatedAt = 0L)
        return id
    }

    fun itemsOfType(type: CatalogType): List<CatalogItem> = allItems.value.filter { it.type == type }

    override suspend fun insert(item: CatalogItem): Long {
        val id = nextId++
        allItems.value += item.copy(id = id)
        return id
    }

    override suspend fun insertAll(items: List<CatalogItem>) {
        items.forEach { insert(it) }
    }

    override suspend fun update(item: CatalogItem) {
        allItems.value = allItems.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun delete(item: CatalogItem) {
        allItems.value = allItems.value.filterNot { it.id == item.id }
    }

    override fun getByType(type: CatalogType): Flow<List<CatalogItem>> =
        allItems.map { list -> list.filter { it.type == type } }

    override suspend fun countByType(type: CatalogType): Int = allItems.value.count { it.type == type }

    override suspend fun getById(id: Long): CatalogItem? = allItems.value.find { it.id == id }

    override suspend fun countByTypeAndNameIgnoreCaseExcludingId(type: CatalogType, name: String, excludeId: Long): Int =
        allItems.value.count {
            it.type == type && it.id != excludeId && it.name.trim().equals(name.trim(), ignoreCase = true)
        }
}
