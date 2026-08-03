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

    /** HU-017: resumen agregado por mes (conteo, % ganadas, R acumulado), calculado en SQL
     * (design.md decisión #2, "no en memoria") -- query aparte de [pagingSourceOrderedByDateDesc],
     * sin paginar (el número de meses reales es acotado, no de la escala de las operaciones).
     * `COUNT(*)` por grupo siempre es >= 1 (GROUP BY solo produce grupos con al menos una fila),
     * lo que evita división por cero en `winRatePercent` incluso cuando ninguna operación del
     * mes fue ganada (HU-017 Escenario 3). */
    @Query(
        """
        SELECT
            CAST(strftime('%Y', dateTime / 1000, 'unixepoch') AS INTEGER) AS year,
            CAST(strftime('%m', dateTime / 1000, 'unixepoch') AS INTEGER) AS month,
            COUNT(*) AS operationCount,
            CAST(ROUND(100.0 * SUM(CASE WHEN result = 'WIN' THEN 1 ELSE 0 END) / COUNT(*)) AS INTEGER) AS winRatePercent,
            COALESCE(SUM(resultInR), 0.0) AS totalResultInR
        FROM operation
        GROUP BY year, month
        ORDER BY year DESC, month DESC
        """
    )
    fun monthlySummaries(): Flow<List<MonthSummary>>

    /** "Borrar todo" (Ajustes): elimina todas las operaciones, conserva los catálogos. */
    @Query("DELETE FROM operation")
    suspend fun deleteAll()
}
