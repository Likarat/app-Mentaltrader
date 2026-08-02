package com.miguel.mentaltrader.testutil

import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake de OperationDao compartido por los tests unitarios JVM (sin Room). [getAllOrderedByDateDesc]
 * deriva de un único [MutableStateFlow] de respaldo, mismo patrón que [FakeCatalogItemDao].
 */
class FakeOperationDao : OperationDao {
    private val allOperations = MutableStateFlow<List<Operation>>(emptyList())
    private var nextId = 1L

    val inserted: List<Operation> get() = allOperations.value

    override suspend fun insert(operation: Operation): Long {
        val withId = operation.copy(id = nextId++)
        allOperations.value += withId
        return withId.id
    }

    override fun getAllOrderedByDateDesc(): Flow<List<Operation>> =
        allOperations.map { list -> list.sortedByDescending { it.dateTime } }

    override suspend fun getById(id: Long): Operation? = allOperations.value.find { it.id == id }

    override suspend fun deleteAll() {
        allOperations.value = emptyList()
    }

    override suspend fun countUsageOfCatalogItem(id: Long): Int =
        allOperations.value.count {
            it.assetId == id || it.emotionBeforeId == id || it.emotionAfterId == id || it.errorId == id
        }
}
