package com.miguel.mentaltrader.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.miguel.mentaltrader.core.model.CatalogType
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogItemDao {
    @Insert
    suspend fun insert(item: CatalogItem): Long

    @Insert
    suspend fun insertAll(items: List<CatalogItem>)

    @Query("SELECT * FROM catalog_item WHERE type = :type ORDER BY name ASC")
    fun getByType(type: CatalogType): Flow<List<CatalogItem>>

    @Query("SELECT COUNT(*) FROM catalog_item WHERE type = :type")
    suspend fun countByType(type: CatalogType): Int

    @Query("SELECT * FROM catalog_item WHERE id = :id")
    suspend fun getById(id: Long): CatalogItem?
}
