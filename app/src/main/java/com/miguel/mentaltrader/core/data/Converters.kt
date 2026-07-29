package com.miguel.mentaltrader.core.data

import androidx.room.TypeConverter
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType

class Converters {
    @TypeConverter
    fun fromDirection(value: Direction): String = value.name

    @TypeConverter
    fun toDirection(value: String): Direction = Direction.valueOf(value)

    @TypeConverter
    fun fromResultType(value: ResultType): String = value.name

    @TypeConverter
    fun toResultType(value: String): ResultType = ResultType.valueOf(value)

    @TypeConverter
    fun fromCatalogType(value: CatalogType): String = value.name

    @TypeConverter
    fun toCatalogType(value: String): CatalogType = CatalogType.valueOf(value)
}
