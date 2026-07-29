package com.miguel.mentaltrader

import android.app.Application
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.image.ImageProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Punto de entrada de la app. Instancia manualmente la base de datos local (sin Hilt/DI
 * framework en v1, decisión del PRD).
 */
class MentaltraderApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val imageProcessor: ImageProcessor by lazy { ImageProcessor(this) }
}
