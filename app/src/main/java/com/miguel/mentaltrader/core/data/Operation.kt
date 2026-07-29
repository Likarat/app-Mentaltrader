package com.miguel.mentaltrader.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType

@Entity(tableName = "operation")
data class Operation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTime: Long,
    val assetId: Long,
    val direction: Direction,
    val quality: Float,
    val emotionBeforeId: Long,
    val emotionBeforeReason: String? = null,
    val emotionAfterId: Long,
    val emotionAfterReason: String? = null,
    val errorId: Long,
    val errorReason: String? = null,
    val result: ResultType,
    val riskPercentage: Float? = null,
    val resultInR: Float? = null,
    val plannedRatio: String? = null,
    val entryDescription: String,
    val createdAt: Long,
    val updatedAt: Long
)
