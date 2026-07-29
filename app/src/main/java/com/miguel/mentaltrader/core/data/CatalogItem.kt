package com.miguel.mentaltrader.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miguel.mentaltrader.core.model.CatalogType

@Entity(tableName = "catalog_item")
data class CatalogItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: CatalogType,
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val SEED_ASSET_XAUUSD = "XAUUSD"
        const val SEED_ERROR_NINGUNO = "Ninguno"
        val SEED_EMOTIONS = listOf(
            "Confianza", "Nervios", "Ansiedad", "Euforia",
            "Frustración", "Alivio", "Miedo", "Calma"
        )
    }
}
