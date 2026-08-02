package com.miguel.mentaltrader.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationDao {
    @Insert
    suspend fun insert(operation: Operation): Long

    @Query("SELECT * FROM operation ORDER BY dateTime DESC")
    fun getAllOrderedByDateDesc(): Flow<List<Operation>>

    @Query("SELECT * FROM operation WHERE id = :id")
    suspend fun getById(id: Long): Operation?

    /** "Borrar todo" (Ajustes): elimina todas las operaciones, conserva los catálogos. */
    @Query("DELETE FROM operation")
    suspend fun deleteAll()

    /** HU-012: cuántas operaciones referencian este elemento de catálogo, en cualquiera de sus 4
     * roles posibles (Activo, Emoción antes/después, Error) -- usado para advertir antes de
     * eliminarlo, no para bloquear la eliminación en sí. */
    @Query(
        "SELECT COUNT(*) FROM operation WHERE assetId = :id OR emotionBeforeId = :id " +
            "OR emotionAfterId = :id OR errorId = :id"
    )
    suspend fun countUsageOfCatalogItem(id: Long): Int
}
