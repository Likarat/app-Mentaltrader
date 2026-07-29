package com.miguel.mentaltrader.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

data class ProcessedImage(val filePath: String, val thumbnailPath: String)

/**
 * Redimensiona (~1600-2000px de ancho) y comprime (JPEG ~85%) las imágenes adjuntas a una
 * operación (HU-007), guardándolas en `filesDir/images`. Sin librerías de compresión de terceros
 * (design.md decisión #4): solo `BitmapFactory`/`Bitmap`, ya parte de la plataforma.
 */
class ImageProcessor(private val appContext: Context) {

    /** Intenta decodificar solo las dimensiones (sin cargar el bitmap completo) para detectar
     * temprano un archivo corrupto o en formato no soportado (HU-006/HU-007 Esc. de error). */
    suspend fun canDecode(uri: Uri): Boolean = withContext(Dispatchers.Default) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            false
        }
    }

    /** Redimensiona, comprime y genera la miniatura de una imagen ya validada con [canDecode].
     * Devuelve null si el procesamiento falla (no bloquea el resto del guardado). */
    suspend fun processAndSave(uri: Uri): ProcessedImage? = withContext(Dispatchers.Default) {
        try {
            val original = appContext.contentResolver.openInputStream(uri)
                ?.use { BitmapFactory.decodeStream(it) }
                ?: return@withContext null

            val outputDir = File(appContext.filesDir, IMAGES_DIR).apply { mkdirs() }
            val fileName = "${UUID.randomUUID()}.jpg"

            val (targetWidth, targetHeight) =
                calculateResizedDimensions(original.width, original.height, TARGET_MAX_WIDTH)
            val resized = if (targetWidth != original.width) {
                Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
            } else {
                original
            }
            val outFile = File(outputDir, fileName)
            FileOutputStream(outFile).use { resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }

            val thumbnailName = thumbnailFileName(fileName)
            val (thumbWidth, thumbHeight) =
                calculateResizedDimensions(resized.width, resized.height, THUMBNAIL_MAX_WIDTH)
            val thumbnail = Bitmap.createScaledBitmap(resized, thumbWidth, thumbHeight, true)
            val thumbFile = File(outputDir, thumbnailName)
            FileOutputStream(thumbFile).use { thumbnail.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }

            ProcessedImage(
                filePath = "$IMAGES_DIR/$fileName",
                thumbnailPath = "$IMAGES_DIR/$thumbnailName"
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val IMAGES_DIR = "images"
        const val TARGET_MAX_WIDTH = 2000
        const val THUMBNAIL_MAX_WIDTH = 300
        const val JPEG_QUALITY = 85

        fun thumbnailFileName(fileName: String) = "${fileName.removeSuffix(".jpg")}_thumb.jpg"

        /** Deriva la ruta relativa de la miniatura cacheada a partir de la ruta de la imagen
         * completa guardada en `OperationImage.filePath` (mismo convenio de nombre que
         * [processAndSave], sin columna extra en el esquema). */
        fun thumbnailPathFor(filePath: String): String {
            val dir = filePath.substringBeforeLast('/', missingDelimiterValue = "")
            val name = filePath.substringAfterLast('/')
            val thumbnailName = thumbnailFileName(name)
            return if (dir.isEmpty()) thumbnailName else "$dir/$thumbnailName"
        }

        /** Calcula ancho/alto redimensionado manteniendo la relación de aspecto, sin ampliar
         * imágenes ya más chicas que [maxWidth] (tasks.md 3.7). Pura, sin dependencias de
         * Android — testeable en JVM. */
        fun calculateResizedDimensions(originalWidth: Int, originalHeight: Int, maxWidth: Int): Pair<Int, Int> {
            require(originalWidth > 0 && originalHeight > 0) {
                "Dimensiones inválidas: ${originalWidth}x$originalHeight"
            }
            if (originalWidth <= maxWidth) return originalWidth to originalHeight
            val ratio = maxWidth.toFloat() / originalWidth
            return maxWidth to (originalHeight * ratio).roundToInt()
        }
    }
}
