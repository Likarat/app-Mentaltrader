package com.miguel.mentaltrader.core.data

import android.content.Context
import com.miguel.mentaltrader.core.image.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * "Borrar todo" (Ajustes): elimina todas las operaciones registradas y sus imágenes (filas +
 * archivos en `filesDir`). Los catálogos (Activos/Emociones/Errores) NO se tocan — decisión de
 * producto explícita: el usuario puede querer volver a empezar a registrar sin perder los
 * catálogos que ya personalizó.
 */
class DataResetService(
    private val operationDao: OperationDao,
    private val operationImageDao: OperationImageDao,
    private val appContext: Context
) {
    suspend fun deleteAllOperationsAndImages() = withContext(Dispatchers.IO) {
        val images = operationImageDao.getAll()
        images.forEach { image ->
            File(appContext.filesDir, image.filePath).delete()
            File(appContext.filesDir, ImageProcessor.thumbnailPathFor(image.filePath)).delete()
        }
        operationImageDao.deleteAll()
        operationDao.deleteAll()
    }
}
