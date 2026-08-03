package com.miguel.mentaltrader.core.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.miguel.mentaltrader.core.model.ResultType
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

    /** HU-025: total real de operaciones en toda la base, SIN aplicar ningún filtro -- distingue
     * "nunca hubo ninguna operación" (Escenario 1) de "hay operaciones, pero el filtro/búsqueda
     * activo (HU-022/HU-023) no arroja resultados" (Escenario 2), ambos casos con
     * `pagingSourceFiltered`/`pagingSourceOrderedByDateDesc` devolviendo un listado vacío por igual.
     * Query aparte y reactiva (Room invalida el `Flow` en cada insert/delete de la tabla), igual
     * que [monthlySummaries]. */
    @Query("SELECT COUNT(*) FROM operation")
    fun countAll(): Flow<Int>

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

    /** HU-022/HU-023: misma fuente que [pagingSourceOrderedByDateDesc] (no cambia su contrato,
     * design.md sección "Non-Goals"), pero con todos los criterios de filtro/búsqueda como
     * parámetros OPCIONALES nulos -- patrón `(:param IS NULL OR columna = :param)`, design.md
     * decisión #5 (se descarta `@RawQuery`: el número de combinaciones de filtro es fijo y
     * pequeño, no justifica perder la verificación en tiempo de compilación de Room). Con todos
     * los parámetros en `null` se comporta exactamente igual que [pagingSourceOrderedByDateDesc]
     * (mismo orden, mismas filas). Todos los criterios se combinan con AND (HU-022 Escenario 3,
     * HU-023 Escenario 2). [searchText] (ya con los comodines `%` agregados por el llamador, o
     * `null` si no hay búsqueda) hace `LIKE` sobre los 4 campos de texto libre de la operación
     * ([Operation.entryDescription]/[Operation.emotionBeforeReason]/[Operation.emotionAfterReason]/
     * [Operation.errorReason]) Y TAMBIÉN sobre el nombre de catálogo (Activo/Emoción antes/Emoción
     * después/Error) ya asociado a esa operación -- HU-023 Escenario 1: "...o nombres de catálogo
     * seleccionados [de la operación] contengan esa palabra" -- de ahí los `LEFT JOIN` contra
     * `catalog_item`. Misma semántica exacta que [HistorialFilterMatcher.matches] (lógica pura
     * testeada en JVM, ver su KDoc) -- la equivalencia real de esta query contra Room/SQLite queda
     * pendiente de un test instrumentado (tasks.md 5.6), diferido a la pasada final única sin
     * dispositivo conectado en esta sesión. */
    @Query(
        """
        SELECT o.* FROM operation o
        LEFT JOIN catalog_item ca ON ca.id = o.assetId
        LEFT JOIN catalog_item ceb ON ceb.id = o.emotionBeforeId
        LEFT JOIN catalog_item cea ON cea.id = o.emotionAfterId
        LEFT JOIN catalog_item cer ON cer.id = o.errorId
        WHERE (:assetId IS NULL OR o.assetId = :assetId)
          AND (:result IS NULL OR o.result = :result)
          AND (:errorId IS NULL OR o.errorId = :errorId)
          AND (:emotionBeforeId IS NULL OR o.emotionBeforeId = :emotionBeforeId)
          AND (:emotionAfterId IS NULL OR o.emotionAfterId = :emotionAfterId)
          AND (:dateFrom IS NULL OR o.dateTime >= :dateFrom)
          AND (:dateTo IS NULL OR o.dateTime <= :dateTo)
          AND (
            :searchText IS NULL
            OR o.entryDescription LIKE :searchText
            OR o.emotionBeforeReason LIKE :searchText
            OR o.emotionAfterReason LIKE :searchText
            OR o.errorReason LIKE :searchText
            OR ca.name LIKE :searchText
            OR ceb.name LIKE :searchText
            OR cea.name LIKE :searchText
            OR cer.name LIKE :searchText
          )
        ORDER BY o.dateTime DESC
        """
    )
    fun pagingSourceFiltered(
        assetId: Long?,
        result: ResultType?,
        errorId: Long?,
        emotionBeforeId: Long?,
        emotionAfterId: Long?,
        dateFrom: Long?,
        dateTo: Long?,
        searchText: String?
    ): PagingSource<Int, Operation>

    /** "Borrar todo" (Ajustes): elimina todas las operaciones, conserva los catálogos. */
    @Query("DELETE FROM operation")
    suspend fun deleteAll()

    /** HU-026 Escenario 1: resumen agregado de TODAS las operaciones -- EP-004-a asume "el
     * periodo ya seleccionado" es el conjunto completo (design.md decisión #1: el selector real
     * de periodo llega recién con HU-028/EP-004-b, esta query se extenderá ahí con
     * dateFrom/dateTo opcionales, mismo patrón evolutivo que [pagingSourceOrderedByDateDesc] ->
     * [pagingSourceFiltered] en EP-003). Una sola fila agregada (un `GROUP BY` implícito de "todo
     * el universo"), calculada en SQL (design.md decisión #2, "no en memoria", misma consistencia
     * que [monthlySummaries]). El `CASE WHEN COUNT(*) = 0` y los `COALESCE` evitan tanto la
     * división por cero como los `NULL` de `AVG`/`SUM` sobre cero filas (HU-026 Escenario 4:
     * valores neutros, sin errores ni caídas). */
    @Query(
        """
        SELECT
            COUNT(*) AS operationCount,
            CASE WHEN COUNT(*) = 0 THEN 0 ELSE
                CAST(ROUND(100.0 * SUM(CASE WHEN result = 'WIN' THEN 1 ELSE 0 END) / COUNT(*)) AS INTEGER)
            END AS winPercent,
            CASE WHEN COUNT(*) = 0 THEN 0 ELSE
                CAST(ROUND(100.0 * SUM(CASE WHEN result = 'LOSS' THEN 1 ELSE 0 END) / COUNT(*)) AS INTEGER)
            END AS lossPercent,
            CASE WHEN COUNT(*) = 0 THEN 0 ELSE
                CAST(ROUND(100.0 * SUM(CASE WHEN result = 'BREAK_EVEN' THEN 1 ELSE 0 END) / COUNT(*)) AS INTEGER)
            END AS breakEvenPercent,
            COALESCE(AVG(quality), 0.0) AS avgQuality,
            COALESCE(SUM(resultInR), 0.0) AS totalResultInR,
            CASE WHEN COUNT(*) = 0 THEN 0.0 ELSE COALESCE(SUM(resultInR), 0.0) / COUNT(*) END AS avgResultInR
        FROM operation
        """
    )
    fun metricsSummary(): Flow<OperationMetricsSummary>

    /** HU-026 Escenario 2: valor de `resultInR` de cada operación, ordenado cronológicamente
     * ascendente (la más antigua primero) -- proyección liviana de una sola columna (NO la
     * entidad [Operation] completa, sigue el mandato de "no cargar todo a memoria" incluso para
     * esta lista ordenada) para que `InicioViewModel` construya el punto-a-punto del R acumulado
     * en Kotlin puro (`ResultInRCumulativeSeries`, testeada aparte en JVM sin Room). Se resuelve
     * así (post-procesamiento fuera de SQL) y no con una función de ventana (`SUM() OVER`, que
     * requeriría SQLite >= 3.25) porque el SQLite embebido de minSdk 27 (Android 8.1) no la
     * soporta de forma confiable en todo el rango de dispositivos objetivo -- ver design.md
     * decisión #1 de este change. */
    @Query("SELECT resultInR FROM operation ORDER BY dateTime ASC")
    fun resultInROrderedByDateAsc(): Flow<List<Float?>>

    /** HU-027 Escenario 1/3: ranking de emociones más frecuentes en TODAS las operaciones (mismo
     * alcance "periodo asumido" que [metricsSummary], design.md decisión #1). Cuenta CADA
     * aparición de un `CatalogItem` de tipo EMOTION en cualquiera de sus 2 roles posibles
     * ([Operation.emotionBeforeId] y [Operation.emotionAfterId]) vía `UNION ALL` -- agrupando por
     * `CatalogItem.id` sin distinguir el rol (HU-027 nota técnica: "agrupando por
     * `CatalogItem.id`"). Desempate determinístico por nombre ascendente (HU-027 Escenario 3:
     * mismo orden en cada render). */
    @Query(
        """
        SELECT ci.id AS id, ci.name AS name, COUNT(*) AS frequency
        FROM (
            SELECT emotionBeforeId AS emotionId FROM operation
            UNION ALL
            SELECT emotionAfterId AS emotionId FROM operation
        ) e
        JOIN catalog_item ci ON ci.id = e.emotionId
        GROUP BY ci.id
        ORDER BY frequency DESC, ci.name ASC
        """
    )
    fun emotionRanking(): Flow<List<CatalogRankingItem>>

    /** HU-027 Escenario 2/3: ranking de errores más frecuentes en TODAS las operaciones (mismo
     * alcance que [emotionRanking]). Agrupa por `CatalogItem.id` de [Operation.errorId] -- incluye
     * la semilla "Ninguno" (`CatalogItem.SEED_ERROR_NINGUNO`) como cualquier otro valor si es el
     * más usado, sin exclusión especial (HU-027 nota técnica). Mismo desempate determinístico que
     * [emotionRanking]. */
    @Query(
        """
        SELECT ci.id AS id, ci.name AS name, COUNT(*) AS frequency
        FROM operation o
        JOIN catalog_item ci ON ci.id = o.errorId
        GROUP BY ci.id
        ORDER BY frequency DESC, ci.name ASC
        """
    )
    fun errorRanking(): Flow<List<CatalogRankingItem>>

    /** HU-012: cuántas operaciones referencian este elemento de catálogo, en cualquiera de sus 4
     * roles posibles (Activo, Emoción antes/después, Error) -- usado para advertir antes de
     * eliminarlo, no para bloquear la eliminación en sí. */
    @Query(
        "SELECT COUNT(*) FROM operation WHERE assetId = :id OR emotionBeforeId = :id " +
            "OR emotionAfterId = :id OR errorId = :id"
    )
    suspend fun countUsageOfCatalogItem(id: Long): Int
}
