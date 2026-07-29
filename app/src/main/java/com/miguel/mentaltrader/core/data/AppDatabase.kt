package com.miguel.mentaltrader.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.miguel.mentaltrader.core.model.CatalogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [Operation::class, CatalogItem::class, OperationImage::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun operationDao(): OperationDao
    abstract fun catalogItemDao(): CatalogItemDao
    abstract fun operationImageDao(): OperationImageDao

    companion object {
        const val DATABASE_NAME = "mentaltrader.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(SeedCallback(scope))
                    // Sin datos reales todavía (app en construcción, EP-001 en curso): la
                    // migración destructiva es más simple que escribir una Migration formal para
                    // un esquema que aún no se estabilizó. Revisar antes del primer release real.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class SeedCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        seedCatalogs(database.catalogItemDao())
                    }
                }
            }
        }

        private suspend fun seedCatalogs(dao: CatalogItemDao) {
            val now = System.currentTimeMillis()
            dao.insert(
                CatalogItem(
                    type = CatalogType.ASSET,
                    name = CatalogItem.SEED_ASSET_XAUUSD,
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
            dao.insert(
                CatalogItem(
                    type = CatalogType.ERROR,
                    name = CatalogItem.SEED_ERROR_NINGUNO,
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
            dao.insertAll(
                CatalogItem.SEED_EMOTIONS.map { name ->
                    CatalogItem(
                        type = CatalogType.EMOTION,
                        name = name,
                        isDefault = true,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            )
        }
    }
}
