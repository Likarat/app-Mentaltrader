package com.miguel.mentaltrader.core.data

import com.miguel.mentaltrader.core.model.CatalogType

sealed interface CatalogAddResult {
    data class Added(val item: CatalogItem) : CatalogAddResult
    data object Duplicate : CatalogAddResult
    data object Blank : CatalogAddResult
}

sealed interface CatalogUpdateResult {
    data object Updated : CatalogUpdateResult
    data object Duplicate : CatalogUpdateResult
}

sealed interface CatalogDeleteResult {
    data object Deleted : CatalogDeleteResult
    data class Blocked(val reason: String) : CatalogDeleteResult
}

/**
 * Punto único de creación/edición/eliminación de [CatalogItem], reutilizado tanto por la
 * administración de catálogos (HU-010/011, pestaña Etiquetas) como por la creación inline desde
 * el formulario de registro (HU-013) — ninguno de los dos reimplementa la validación de unicidad
 * ni la protección de semillas.
 */
class CatalogRepository(
    private val catalogItemDao: CatalogItemDao,
    private val operationDao: OperationDao
) {

    /** HU-012: cuántas operaciones ya registradas usan [item] -- para advertir antes de eliminar,
     * no para bloquear (la eliminación en sí la sigue decidiendo el usuario vía [deleteItem]). */
    suspend fun usageCountOf(item: CatalogItem): Int = operationDao.countUsageOfCatalogItem(item.id)

    suspend fun addItem(type: CatalogType, name: String): CatalogAddResult {
        val trimmed = name.trim()
        // Bug real reportado por el usuario (2026-08-04): con el campo vacío (o solo espacios) se
        // creaba igual un elemento de catálogo vacío -- antes de esto solo se validaban duplicados.
        if (trimmed.isEmpty()) {
            return CatalogAddResult.Blank
        }
        if (catalogItemDao.countByTypeAndNameIgnoreCaseExcludingId(type, trimmed, excludeId = NO_EXCLUSION) > 0) {
            return CatalogAddResult.Duplicate
        }
        val now = System.currentTimeMillis()
        val id = catalogItemDao.insert(
            CatalogItem(type = type, name = trimmed, isDefault = false, createdAt = now, updatedAt = now)
        )
        return CatalogAddResult.Added(
            CatalogItem(id = id, type = type, name = trimmed, isDefault = false, createdAt = now, updatedAt = now)
        )
    }

    suspend fun updateItem(item: CatalogItem, newName: String): CatalogUpdateResult {
        val trimmed = newName.trim()
        if (catalogItemDao.countByTypeAndNameIgnoreCaseExcludingId(item.type, trimmed, excludeId = item.id) > 0) {
            return CatalogUpdateResult.Duplicate
        }
        catalogItemDao.update(item.copy(name = trimmed, updatedAt = System.currentTimeMillis()))
        return CatalogUpdateResult.Updated
    }

    suspend fun deleteItem(item: CatalogItem): CatalogDeleteResult {
        if (item.isDefault) {
            when (item.type) {
                CatalogType.ERROR -> return CatalogDeleteResult.Blocked(SEED_ERROR_BLOCKED_MESSAGE)
                CatalogType.ASSET -> if (catalogItemDao.countByType(CatalogType.ASSET) <= 1) {
                    return CatalogDeleteResult.Blocked(SEED_ASSET_BLOCKED_MESSAGE)
                }
                CatalogType.EMOTION -> Unit // las emociones semilla no están protegidas (spec: set inicial editable)
            }
        }
        catalogItemDao.delete(item)
        if (catalogItemDao.countByType(item.type) == 0) {
            CatalogSeeder.ensureSeeded(catalogItemDao)
        }
        return CatalogDeleteResult.Deleted
    }

    companion object {
        const val DUPLICATE_MESSAGE = "Este valor ya existe en el catálogo"
        const val BLANK_MESSAGE = "No se ha registrado ningún valor"
        private const val NO_EXCLUSION = 0L
        private const val SEED_ERROR_BLOCKED_MESSAGE = "\"Ninguno\" no puede eliminarse."
        private const val SEED_ASSET_BLOCKED_MESSAGE = "\"XAUUSD\" no puede eliminarse mientras sea el único activo."
    }
}
