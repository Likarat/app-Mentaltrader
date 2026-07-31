package com.miguel.mentaltrader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationImageDao
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.feature.registro.OperationFormViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Tests instrumentados de HU-006 (adjuntar imagen sin procesar) y HU-007 (redimensionar/
 * comprimir/miniatura) sobre componentes reales: `OperationFormViewModel` + `ImageProcessor`
 * (contexto Android real) + Room real (in-memory, aislada por test). Corre en dispositivo real
 * porque `ImageProcessor` usa `BitmapFactory`/`Bitmap` (no disponible en JVM plano, sin
 * Robolectric en este proyecto) y porque `android.net.Uri` no es usable de forma confiable en
 * tests unitarios JVM sin stub.
 */
@RunWith(AndroidJUnit4::class)
class OperationFormImageProcessingTest {

    private lateinit var context: android.content.Context
    private lateinit var database: AppDatabase
    private lateinit var operationDao: OperationDao
    private lateinit var catalogItemDao: CatalogItemDao
    private lateinit var operationImageDao: OperationImageDao
    private lateinit var imageProcessor: ImageProcessor

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        operationDao = database.operationDao()
        catalogItemDao = database.catalogItemDao()
        operationImageDao = database.operationImageDao()
        imageProcessor = ImageProcessor(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createViewModel(): OperationFormViewModel =
        OperationFormViewModel(operationDao, catalogItemDao, operationImageDao, imageProcessor)

    private suspend fun seed(type: CatalogType, name: String): Long =
        catalogItemDao.insert(CatalogItem(type = type, name = name, isDefault = true, createdAt = 0L, updatedAt = 0L))

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            check(System.currentTimeMillis() - start <= timeoutMs) { "Timeout esperando la condición" }
            Thread.sleep(20)
        }
    }

    /** Genera un archivo JPEG real y válido (contenido con ruido real, no un color plano, para
     * que la compresión JPEG sea representativa) en `cacheDir` y devuelve su Uri. */
    private fun realImageFile(width: Int = 2400, height: Int = 1600, name: String = "${UUID.randomUUID()}.jpg"): Uri {
        val random = java.util.Random(42)
        val pixels = IntArray(width * height) { random.nextInt() }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return Uri.fromFile(file)
    }

    private fun corruptImageFile(name: String = "corrupto_${UUID.randomUUID()}.jpg"): Uri {
        val file = File(context.cacheDir, name)
        file.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07))
        return Uri.fromFile(file)
    }

    // HU-006 Escenario 2: impedir una tercera imagen.
    @Test
    fun adjuntarUnaTerceraImagenSeImpide() {
        val viewModel = createViewModel()
        val uri1 = realImageFile(width = 200, height = 150, name = "img1.jpg")
        val uri2 = realImageFile(width = 200, height = 150, name = "img2.jpg")
        val uri3 = realImageFile(width = 200, height = 150, name = "img3.jpg")

        viewModel.onImageAdded(uri1)
        waitUntil { viewModel.state.value.pendingImages.size == 1 }
        viewModel.onImageAdded(uri2)
        waitUntil { viewModel.state.value.pendingImages.size == 2 }
        viewModel.onImageAdded(uri3)

        Thread.sleep(300)
        assertEquals(2, viewModel.state.value.pendingImages.size)
        assertEquals("Ya adjuntaste el máximo de 2 imágenes", viewModel.state.value.imageError)
    }

    // HU-006 Escenario 3: permiso de Cámara rechazado -- informa y permite continuar registrando
    // la operación con normalidad, sin imagen.
    @Test
    fun permisoDeCamaraRechazado_informaYPermiteGuardarSinImagen() = runBlocking {
        val assetId = seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = seed(CatalogType.EMOTION, "Confianza")
        val errorId = seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()

        viewModel.onCameraPermissionDenied()
        assertEquals(
            "Permiso de cámara rechazado. Podés continuar sin imagen o adjuntar desde la galería.",
            viewModel.state.value.imageError
        )
        assertTrue(viewModel.state.value.pendingImages.isEmpty())

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange("Sin imagen, permiso de cámara rechazado")
        viewModel.save()

        waitUntil { viewModel.state.value.isSaved }
        val saved = operationDao.getAllOrderedByDateDesc().first().single()
        assertTrue(operationImageDao.getAll().isEmpty())
        assertEquals("Sin imagen, permiso de cámara rechazado", saved.entryDescription)
    }

    // INT-permisos-camara-galeria: tras rechazar el permiso de Cámara, adjuntar desde Galería
    // sigue funcionando con normalidad (los dos flujos son independientes: no hay ninguna
    // bandera compartida que bloquee uno por culpa del otro) y el guardado con imagen persiste
    // igual, sin crashear.
    @Test
    fun trasRechazarCamaraLaGaleriaSigueFuncionandoSinCrash() = runBlocking {
        val assetId = seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = seed(CatalogType.EMOTION, "Confianza")
        val errorId = seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()

        viewModel.onCameraPermissionDenied()
        val gallerySelectedUri = realImageFile(width = 200, height = 150, name = "desde_galeria.jpg")
        viewModel.onImageAdded(gallerySelectedUri)
        waitUntil { viewModel.state.value.pendingImages.size == 1 }
        // El error de cámara queda limpio en cuanto la galería adjunta con éxito (degradación
        // graceful real, no un mensaje de error que persiste indefinidamente).
        assertNull(viewModel.state.value.imageError)

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange("Cámara rechazada, adjunté desde galería")
        viewModel.save()

        waitUntil { viewModel.state.value.isSaved }
        val images = operationImageDao.getAll()
        assertEquals(1, images.size)
        assertTrue(File(context.filesDir, images.first().filePath).exists())
    }

    // HU-007 Escenario 3: imagen de origen corrupta o ilegible -- se informa y no se adjunta,
    // permitiendo intentar con otra.
    @Test
    fun imagenCorrupta_seInformaYNoSeAdjunta() {
        val viewModel = createViewModel()
        val corruptUri = corruptImageFile()

        viewModel.onImageAdded(corruptUri)

        waitUntil { viewModel.state.value.imageError != null }
        assertTrue(viewModel.state.value.pendingImages.isEmpty())
        assertEquals(
            "No se pudo procesar la imagen (archivo dañado o formato no soportado)",
            viewModel.state.value.imageError
        )

        // Permite intentar con otra imagen (real) inmediatamente después, sin quedar bloqueado.
        val uriValida = realImageFile(width = 200, height = 150, name = "recuperacion.jpg")
        viewModel.onImageAdded(uriValida)
        waitUntil { viewModel.state.value.pendingImages.size == 1 }
        assertNull(viewModel.state.value.imageError)
    }

    @Test
    fun canDecodeDevuelveFalseParaUnArchivoCorrupto() = runBlocking {
        assertFalse(imageProcessor.canDecode(corruptImageFile()))
    }

    // HU-007 Escenario 1: redimensiona a ~1600-2000px de ancho y comprime en JPEG real (~85%).
    @Test
    fun processAndSave_redimensionaYComprimeUnaImagenRealAJpeg() = runBlocking {
        val originalWidth = 3000
        val originalHeight = 2000
        val uri = realImageFile(width = originalWidth, height = originalHeight, name = "original_grande.jpg")

        val result = imageProcessor.processAndSave(uri)

        assertNotNull(result)
        val savedFile = File(context.filesDir, result!!.filePath)
        val thumbFile = File(context.filesDir, result.thumbnailPath)
        assertTrue("El archivo procesado debe existir", savedFile.exists())
        assertTrue("La miniatura debe existir", thumbFile.exists())

        val (expectedWidth, expectedHeight) = ImageProcessor.calculateResizedDimensions(
            originalWidth, originalHeight, ImageProcessor.TARGET_MAX_WIDTH
        )
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(savedFile.path, options)
        assertEquals(expectedWidth, options.outWidth)
        assertEquals(expectedHeight, options.outHeight)
        assertEquals(2000, options.outWidth) // dentro del rango ~1600-2000px de la spec

        val (expectedThumbWidth, expectedThumbHeight) = ImageProcessor.calculateResizedDimensions(
            expectedWidth, expectedHeight, ImageProcessor.THUMBNAIL_MAX_WIDTH
        )
        val thumbOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(thumbFile.path, thumbOptions)
        assertEquals(expectedThumbWidth, thumbOptions.outWidth)
        assertEquals(expectedThumbHeight, thumbOptions.outHeight)

        // Compresión JPEG real (no una copia sin procesar): el archivo resultante pesa una
        // fracción chica del bitmap sin comprimir (3000x2000 ARGB_8888 = ~24MB crudo), incluso
        // usando una imagen de prueba con ruido de alta entropía (peor caso de compresión JPEG
        // que una foto real típica, que suele comprimir mejor todavía).
        val uncompressedSizeApprox = originalWidth.toLong() * originalHeight * 4
        assertTrue(
            "El archivo procesado (${savedFile.length()} bytes) debería ser sustancialmente " +
                "menor al bitmap sin comprimir (~$uncompressedSizeApprox bytes)",
            savedFile.length() in 1..(uncompressedSizeApprox / 4)
        )
    }

    // INT-imagen-pipeline-coil: tras guardar con imagen, `OperationImage.filePath` apunta al
    // archivo PROCESADO (no a la URI cruda del picker), y el archivo de miniatura que
    // `HistorialScreen.kt` resuelve vía `ImageProcessor.thumbnailPathFor()` existe realmente en
    // `filesDir` -- cierra el gap real detectado por el verificador: `persistImages()` nunca
    // corría en la suite previa porque el ViewModel de `OperationFormPersistenceTest` no
    // inyectaba `operationImageDao` ni `imageProcessor`.
    @Test
    fun guardarConImagenPersisteLaRutaProcesadaYGeneraLaMiniaturaQueHistorialCarga() = runBlocking {
        val assetId = seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = seed(CatalogType.EMOTION, "Confianza")
        val errorId = seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        val originalUri = realImageFile(width = 300, height = 200, name = "elegida_en_galeria.jpg")

        viewModel.onImageAdded(originalUri)
        waitUntil { viewModel.state.value.pendingImages.size == 1 }

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange("Con imagen desde galería")
        viewModel.save()

        waitUntil { viewModel.state.value.isSaved }
        val savedImage = operationImageDao.getAll().single()

        // No es la URI cruda del picker: el filePath persistido vive en el directorio de
        // procesamiento real (ImageProcessor.IMAGES_DIR), no el nombre del archivo original.
        assertTrue(savedImage.filePath.startsWith("${ImageProcessor.IMAGES_DIR}/"))
        assertFalse(savedImage.filePath.contains("elegida_en_galeria"))

        val thumbnailPath = ImageProcessor.thumbnailPathFor(savedImage.filePath)
        val thumbnailFile = File(context.filesDir, thumbnailPath)
        assertTrue(
            "La miniatura procesada (la que HistorialScreen.kt renderiza vía Coil) debe existir en filesDir",
            thumbnailFile.exists()
        )
        assertTrue(thumbnailFile.length() > 0)
    }
}
