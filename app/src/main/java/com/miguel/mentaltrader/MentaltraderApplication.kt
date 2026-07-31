package com.miguel.mentaltrader

import android.app.Application
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.data.CatalogRepository
import com.miguel.mentaltrader.core.data.CatalogSeeder
import com.miguel.mentaltrader.core.data.DataResetService
import com.miguel.mentaltrader.core.image.ImageProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Punto de entrada de la app. Instancia manualmente la base de datos local (sin Hilt/DI
 * framework en v1, decisión del PRD).
 */
class MentaltraderApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val imageProcessor: ImageProcessor by lazy { ImageProcessor(this) }
    val dataResetService: DataResetService by lazy {
        DataResetService(database.operationDao(), database.operationImageDao(), this)
    }
    val catalogRepository: CatalogRepository by lazy { CatalogRepository(database.catalogItemDao()) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { CatalogSeeder.ensureSeeded(database.catalogItemDao()) }
    }
}
