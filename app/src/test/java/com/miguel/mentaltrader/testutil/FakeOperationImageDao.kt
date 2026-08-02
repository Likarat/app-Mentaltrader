package com.miguel.mentaltrader.testutil

import com.miguel.mentaltrader.core.data.OperationImage
import com.miguel.mentaltrader.core.data.OperationImageDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Fake de OperationImageDao compartido por los tests unitarios JVM (sin Room). */
class FakeOperationImageDao : OperationImageDao {
    private val allImages = MutableStateFlow<List<OperationImage>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(image: OperationImage): Long {
        val id = nextId++
        allImages.value += image.copy(id = id)
        return id
    }

    override suspend fun insertAll(images: List<OperationImage>) {
        images.forEach { insert(it) }
    }

    override fun getByOperationId(operationId: Long): Flow<List<OperationImage>> =
        allImages.map { list -> list.filter { it.operationId == operationId }.sortedBy { it.position } }

    override suspend fun getAll(): List<OperationImage> = allImages.value

    override suspend fun deleteAll() {
        allImages.value = emptyList()
    }
}
