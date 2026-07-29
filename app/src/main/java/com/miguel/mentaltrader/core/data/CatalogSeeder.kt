package com.miguel.mentaltrader.core.data

import com.miguel.mentaltrader.core.model.CatalogType

/**
 * Siembra XAUUSD (ASSET), Ninguno (ERROR) y las 8 emociones semilla.
 *
 * Antes vivía en un `RoomDatabase.Callback.onCreate` (ver historial de EP-001), pero un bug real
 * detectado en la primera corrida instrumentada real (2026-07-29) mostró que la siembra no
 * ocurría de forma confiable tras una migración destructiva (bump de versión 1->2 al agregar
 * `OperationImage`) — `AppDatabaseSeedTest` falló con catálogos vacíos. Idempotente por diseño
 * (chequea cada catálogo antes de insertar), así que puede invocarse siempre al arrancar la app
 * sin depender de en qué punto del ciclo de vida de Room se dispare `onCreate`.
 */
object CatalogSeeder {

    suspend fun ensureSeeded(dao: CatalogItemDao) {
        val now = System.currentTimeMillis()

        if (dao.countByType(CatalogType.ASSET) == 0) {
            dao.insert(
                CatalogItem(
                    type = CatalogType.ASSET,
                    name = CatalogItem.SEED_ASSET_XAUUSD,
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        if (dao.countByType(CatalogType.ERROR) == 0) {
            dao.insert(
                CatalogItem(
                    type = CatalogType.ERROR,
                    name = CatalogItem.SEED_ERROR_NINGUNO,
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        if (dao.countByType(CatalogType.EMOTION) == 0) {
            dao.insertAll(
                CatalogItem.SEED_EMOTIONS.map { name ->
                    CatalogItem(
                        type = CatalogType.EMOTION,
                        name = name,
                        isDefault = true,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            )
        }
    }
}
