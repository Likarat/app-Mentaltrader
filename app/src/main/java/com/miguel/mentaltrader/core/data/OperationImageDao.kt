package com.miguel.mentaltrader.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationImageDao {
    @Insert
    suspend fun insert(image: OperationImage): Long

    @Insert
    suspend fun insertAll(images: List<OperationImage>)

    @Query("SELECT * FROM operation_image WHERE operationId = :operationId ORDER BY position ASC")
    fun getByOperationId(operationId: Long): Flow<List<OperationImage>>

    /** "Borrar todo" (Ajustes): lista todas las imágenes para poder borrar sus archivos antes de
     * eliminar las filas. */
    @Query("SELECT * FROM operation_image")
    suspend fun getAll(): List<OperationImage>

    @Query("DELETE FROM operation_image")
    suspend fun deleteAll()
}
