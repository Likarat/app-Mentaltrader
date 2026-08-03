package com.miguel.mentaltrader.core.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationDao {
    @Insert
    suspend fun insert(operation: Operation): Long

    @Query("SELECT * FROM operation ORDER BY dateTime DESC")
    fun getAllOrderedByDateDesc(): Flow<List<Operation>>

    /** HU-015/HU-016: misma fuente que [getAllOrderedByDateDesc], pero paginada (Room genera un
     * PagingSource real por página, sin cargar el histórico completo a memoria) para el listado
     * real de Historial. */
    @Query("SELECT * FROM operation ORDER BY dateTime DESC")
    fun pagingSourceOrderedByDateDesc(): PagingSource<Int, Operation>

    @Query("SELECT * FROM operation WHERE id = :id")
    suspend fun getById(id: Long): Operation?

    /** HU-020: actualiza una operación existente (modo edición de OperationFormViewModel,
     * design.md decisión #3). No cambia el contrato de [insert] existente. */
    @Update
    suspend fun update(operation: Operation)

    /** HU-021: borrado físico definitivo de una operación, disparado por HistorialViewModel solo
     * tras expirar la ventana de "Deshacer" (soft-delete solo en memoria, design.md decisión #4 --
     * este método nunca se llama mientras esa ventana sigue activa). */
    @Query("DELETE FROM operation WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** "Borrar todo" (Ajustes): elimina todas las operaciones, conserva los catálogos. */
    @Query("DELETE FROM operation")
    suspend fun deleteAll()
}
