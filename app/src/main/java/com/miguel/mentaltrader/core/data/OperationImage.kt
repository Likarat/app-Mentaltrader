package com.miguel.mentaltrader.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operation_image")
data class OperationImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationId: Long,
    val filePath: String,
    val position: Int,
    val createdAt: Long
)
