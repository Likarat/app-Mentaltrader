package com.miguel.mentaltrader.feature.registro

import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.testutil.FakeCatalogItemDao
import com.miguel.mentaltrader.testutil.FakeOperationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios de OperationFormViewModel (tasks.md 1.10, sub-slice EP-001-a):
 * validación de campos obligatorios (HU-001 Esc.2, HU-002 Esc.2, HU-003 Esc.2) y mapeo de
 * estado -> Operation (HU-001 Esc.4/5, HU-002 Esc.5, HU-003 Esc.1/3). Usa fakes de OperationDao/
 * CatalogItemDao en vez de Room (Room se ejercita en los tests instrumentados, ver
 * OperationFormPersistenceTest).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OperationFormViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var operationDao: FakeOperationDao
    private lateinit var catalogItemDao: FakeCatalogItemDao

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        operationDao = FakeOperationDao()
        catalogItemDao = FakeCatalogItemDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(now: () -> java.time.LocalDateTime = java.time.LocalDateTime::now): OperationFormViewModel =
        OperationFormViewModel(operationDao, catalogItemDao, nowProvider = now)

    private fun OperationFormViewModel.fillMinimalRequiredFields(
        assetId: Long,
        emotionBeforeId: Long,
        emotionAfterId: Long,
        errorId: Long
    ) {
        onDateChange("28/07/2026")
        onTimeChange("10:30")
        onAssetSelected(assetId)
        onDirectionSelected(Direction.BUY)
        onQualityChange("8.5")
        onEmotionBeforeSelected(emotionBeforeId)
        onEmotionAfterSelected(emotionAfterId)
        onErrorSelected(errorId)
        onResultSelected(ResultType.WIN)
        onDescriptionChange("Entré por ruptura de rango")
    }

    // HU-001 Escenario 1 / HU-002 Escenario 1 / HU-003 Escenario 1
    @Test
    fun `guardar con todos los campos obligatorios completos persiste la operacion y marca isSaved`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionBeforeId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val emotionAfterId = catalogItemDao.seed(CatalogType.EMOTION, "Calma")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()

        viewModel.fillMinimalRequiredFields(assetId, emotionBeforeId, emotionAfterId, errorId)
        viewModel.save()

        assertEquals(1, operationDao.inserted.size)
        assertTrue(viewModel.state.value.isSaved)
        assertTrue(viewModel.state.value.fieldErrors.isEmpty())
    }

    // HU-001 Escenario 2: falta un campo mínimo (Activo)
    @Test
    fun `guardar sin un campo minimo lo impide y resalta el campo faltante sin cerrar el formulario`() = runTest {
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()

        // Activo (assetId) nunca seleccionado a propósito.
        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange("texto")

        viewModel.save()

        assertEquals(0, operationDao.inserted.size)
        assertFalse(viewModel.state.value.isSaved)
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_ASSET))
    }

    // HU-002 Escenario 2: falta un campo de catálogo obligatorio (Emoción antes)
    @Test
    fun `guardar sin completar un campo de catalogo obligatorio lo impide`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Calma")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        // Emoción antes queda sin seleccionar a propósito.
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange("texto")

        viewModel.save()

        assertEquals(0, operationDao.inserted.size)
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_EMOTION_BEFORE))
    }

    // HU-003 Escenario 2: descripción de entrada vacía
    @Test
    fun `guardar con descripcion vacia resalta el campo como obligatorio`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        viewModel.onDirectionSelected(Direction.BUY)
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        // entryDescription queda vacío a propósito.

        viewModel.save()

        assertEquals(0, operationDao.inserted.size)
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_DESCRIPTION))
    }

    // HU-001 Escenario 5 / HU-002 Escenario 5 (mapeo de estado): opcionales vacíos -> null
    @Test
    fun `los campos opcionales vacios se mapean a null en la operacion guardada`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        // Riesgo (%), Resultado en R, Ratio planeado y los 3 motivos quedan en blanco.

        viewModel.save()

        val saved = operationDao.inserted.single()
        assertNull(saved.riskPercentage)
        assertNull(saved.resultInR)
        assertNull(saved.plannedRatio)
        assertNull(saved.emotionBeforeReason)
        assertNull(saved.emotionAfterReason)
        assertNull(saved.errorReason)
    }

    // HU-001 Escenario 4 / HU-002 Escenario 5 (mapeo de estado): opcionales completos
    @Test
    fun `los campos opcionales completos se mapean con su valor en la operacion guardada`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onRiskPercentageChange("2.5")
        viewModel.onResultInRChange("-1.5")
        viewModel.onPlannedRatioChange("1:2")
        viewModel.onEmotionBeforeReasonChange("Vi la señal clara")
        viewModel.onEmotionAfterReasonChange("Cerré satisfecho")
        viewModel.onErrorReasonChange("Ninguno detectado")

        viewModel.save()

        val saved = operationDao.inserted.single()
        assertEquals(2.5f, saved.riskPercentage)
        assertEquals(-1.5f, saved.resultInR)
        assertEquals("1:2", saved.plannedRatio)
        assertEquals("Vi la señal clara", saved.emotionBeforeReason)
        assertEquals("Cerré satisfecho", saved.emotionAfterReason)
        assertEquals("Ninguno detectado", saved.errorReason)
    }

    // HU-003 Escenario 3: texto largo con múltiples saltos de línea, sin truncar
    @Test
    fun `la descripcion de entrada extensa con saltos de linea se guarda completa sin truncar`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        val textoLargo = (1..50).joinToString("\n") { "Párrafo $it con contenido de análisis de la operación." }
        viewModel.onDescriptionChange(textoLargo)

        viewModel.save()

        assertEquals(textoLargo, operationDao.inserted.single().entryDescription)
    }

    // Edge adicional descubierto durante la implementación: fecha/hora con formato inválido.
    @Test
    fun `fecha invalida bloquea el guardado con error en el campo fecha`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onDateChange("fecha-no-valida")

        viewModel.save()

        assertEquals(0, operationDao.inserted.size)
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_DATE))
    }

    // Bug real reportado por el usuario (2026-07-29): teclado en configuración regional
    // español usa coma como separador decimal; "7,5" se leía como inválido en silencio y el
    // guardado quedaba bloqueado aunque el campo se viera completo.
    @Test
    fun `calidad y riesgo con coma como separador decimal se guardan correctamente`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onQualityChange("7,5")
        viewModel.onRiskPercentageChange("2,5")
        viewModel.onResultInRChange("1,5")

        viewModel.save()

        assertTrue(viewModel.state.value.fieldErrors.isEmpty())
        assertTrue(viewModel.state.value.isSaved)
        val saved = operationDao.inserted.single()
        assertEquals(7.5f, saved.quality)
        assertEquals(2.5f, saved.riskPercentage)
        assertEquals(1.5f, saved.resultInR)
    }

    // Bug real reportado por el usuario: "Ratio planeado" aceptaba cualquier texto sin
    // ningún aviso (ej. "hhahajajkfkg").
    @Test
    fun `ratio planeado con formato invalido bloquea el guardado`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onPlannedRatioChange("hhahajajkfkg")

        viewModel.save()

        assertEquals(0, operationDao.inserted.size)
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_RATIO))
    }

    @Test
    fun `ratio planeado con formato valido se guarda correctamente`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onPlannedRatioChange("1:2")

        viewModel.save()

        assertTrue(viewModel.state.value.isSaved)
        assertEquals("1:2", operationDao.inserted.single().plannedRatio)
    }

    // Bug real reportado por el usuario: Dirección sin seleccionar no mostraba ningún error.
    @Test
    fun `direccion sin seleccionar bloquea el guardado y resalta el campo`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()

        viewModel.onDateChange("28/07/2026")
        viewModel.onTimeChange("10:30")
        viewModel.onAssetSelected(assetId)
        // Dirección queda sin seleccionar a propósito.
        viewModel.onQualityChange("8.5")
        viewModel.onEmotionBeforeSelected(emotionId)
        viewModel.onEmotionAfterSelected(emotionId)
        viewModel.onErrorSelected(errorId)
        viewModel.onResultSelected(ResultType.WIN)
        viewModel.onDescriptionChange("texto")

        viewModel.save()

        assertEquals(0, operationDao.inserted.size)
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_DIRECTION))
    }

    // HU-004 Escenario 1: valores dentro de rango se guardan sin error.
    @Test
    fun `guardar con calidad riesgo y R dentro de rango no muestra ningun error`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onQualityChange("7.5")
        viewModel.onRiskPercentageChange("2")
        viewModel.onResultInRChange("2")

        viewModel.save()

        assertTrue(viewModel.state.value.fieldErrors.isEmpty())
        assertTrue(viewModel.state.value.isSaved)
        assertEquals(2.0f, operationDao.inserted.single().resultInR)
    }

    // HU-004 Escenario 2: fecha/hora futura bloquea el guardado.
    @Test
    fun `fecha futura bloquea el guardado con el mensaje especifico`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        val futureDate = java.time.LocalDate.now().plusYears(1)
        viewModel.onDateChange(futureDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))

        viewModel.save()

        assertEquals(0, operationDao.inserted.size)
        assertEquals(
            "No se permiten fechas/horas futuras",
            viewModel.state.value.fieldErrors[OperationFormState.FIELD_DATE]
        )
    }

    // HU-004 Escenario 3: Calidad o Riesgo (%) fuera de rango, sin bloquear los demás campos.
    @Test
    fun `calidad fuera de rango bloquea el guardado y resalta solo ese campo`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onQualityChange("11")

        viewModel.save()

        assertEquals(0, operationDao.inserted.size)
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_QUALITY))
        assertFalse(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_DATE))
    }

    @Test
    fun `riesgo fuera de rango bloquea el guardado`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onRiskPercentageChange("150")

        viewModel.save()

        assertEquals(0, operationDao.inserted.size)
        assertTrue(viewModel.state.value.fieldErrors.containsKey(OperationFormState.FIELD_RISK))
    }

    // HU-004 Escenario 4: Resultado en R negativo de magnitud grande, sin límite inferior.
    @Test
    fun `resultado en R negativo de magnitud grande se acepta sin bloquear el guardado`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onResultInRSignChange(OperationFormState.SIGN_NEGATIVE)
        viewModel.onResultInRChange("15.5")

        viewModel.save()

        assertTrue(viewModel.state.value.isSaved)
        assertEquals(-15.5f, operationDao.inserted.single().resultInR)
    }

    @Test
    fun `formatResultInR aplica el signo del dropdown y el sufijo R fijo`() {
        assertEquals("-15.5R", OperationFormState.formatResultInR(OperationFormState.SIGN_NEGATIVE, "15.5"))
        assertEquals("+2.0R", OperationFormState.formatResultInR(OperationFormState.SIGN_POSITIVE, "2.0"))
        assertEquals(null, OperationFormState.formatResultInR(OperationFormState.SIGN_POSITIVE, ""))
    }

    // HU-005 Escenario 2: catálogo de Activos con solo la semilla -> se preselecciona.
    @Test
    fun `al crear el viewmodel se prellena fecha hora actuales y el activo semilla si el catalogo solo tiene la semilla`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, CatalogItem.SEED_ASSET_XAUUSD)
        val viewModel = createViewModel()

        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        assertEquals(today, viewModel.state.value.dateText)
        assertEquals(assetId, viewModel.state.value.assetId)
    }

    // HU-005 Escenario 3: catálogo con más de un elemento -> no fuerza ningún Activo por defecto.
    @Test
    fun `no se preselecciona ningun activo si el catalogo ya tiene mas de un elemento`() = runTest {
        catalogItemDao.seed(CatalogType.ASSET, CatalogItem.SEED_ASSET_XAUUSD)
        catalogItemDao.seed(CatalogType.ASSET, "EURUSD")
        val viewModel = createViewModel()

        assertNull(viewModel.state.value.assetId)
    }

    // HU-005 Escenario 4: editar manualmente Fecha/Hora ya prellenadas conserva el valor editado,
    // sin revertirlo automáticamente al valor por defecto.
    @Test
    fun `editar manualmente fecha y hora prellenadas conserva el valor editado sin revertirlo`() = runTest {
        catalogItemDao.seed(CatalogType.ASSET, CatalogItem.SEED_ASSET_XAUUSD)
        val viewModel = createViewModel()
        // Confirma que efectivamente arrancó prellenado (para no falsear el escenario: si no
        // hubiera prellenado, "conservar la edición" no probaría nada).
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        assertEquals(today, viewModel.state.value.dateText)

        viewModel.onDateChange("01/01/2020")
        viewModel.onTimeChange("00:00")

        assertEquals("01/01/2020", viewModel.state.value.dateText)
        assertEquals("00:00", viewModel.state.value.timeText)
    }

    // HU-005 Escenario 5: reloj del dispositivo desconfigurado -- el sistema prellena igual con
    // el valor (incorrecto) del reloj, sin intentar corregirlo ni validarlo en este punto (esa
    // responsabilidad es exclusiva de HU-004 al guardar). Se inyecta un reloj falso vía
    // `nowProvider` porque manipular de forma confiable el reloj real del sistema operativo del
    // dispositivo de pruebas no es viable en este entorno (ver progress_log de build-state.json).
    @Test
    fun `reloj del dispositivo desconfigurado hacia el pasado prellena igual sin validarlo`() = runTest {
        catalogItemDao.seed(CatalogType.ASSET, CatalogItem.SEED_ASSET_XAUUSD)
        val relojDesconfiguradoEnElPasado = java.time.LocalDateTime.of(1999, 1, 1, 0, 0)
        val viewModel = createViewModel(now = { relojDesconfiguradoEnElPasado })

        assertEquals("01/01/1999", viewModel.state.value.dateText)
        assertEquals("00:00", viewModel.state.value.timeText)
        // No se valida ni se bloquea en este punto: el formulario recién abre, sin fieldErrors.
        assertTrue(viewModel.state.value.fieldErrors.isEmpty())
    }

    @Test
    fun `reloj del dispositivo desconfigurado hacia el futuro tambien prellena sin validarlo`() = runTest {
        catalogItemDao.seed(CatalogType.ASSET, CatalogItem.SEED_ASSET_XAUUSD)
        val relojDesconfiguradoEnElFuturo = java.time.LocalDateTime.of(2099, 6, 15, 12, 0)
        val viewModel = createViewModel(now = { relojDesconfiguradoEnElFuturo })

        assertEquals("15/06/2099", viewModel.state.value.dateText)
        assertEquals("12:00", viewModel.state.value.timeText)
        assertTrue(viewModel.state.value.fieldErrors.isEmpty())
    }

    // HU-013 Escenario 1: tocar "+ Agregar nueva" muestra el campo inline sin salir del formulario
    // ni tocar el resto de los datos ya ingresados.
    @Test
    fun `iniciar el alta inline de Activo muestra el campo sin tocar el resto del estado`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onDescriptionChange("Entré por ruptura de rango")

        viewModel.onStartAddCatalogItem(OperationFormState.FIELD_ASSET)

        assertEquals(OperationFormState.FIELD_ASSET, viewModel.state.value.addingCatalogField)
        assertEquals("", viewModel.state.value.addCatalogText)
        assertNull(viewModel.state.value.addCatalogError)
        assertEquals(assetId, viewModel.state.value.assetId)
        assertEquals("Entré por ruptura de rango", viewModel.state.value.entryDescription)
    }

    // HU-013 Escenario 2: confirmar la creación guarda el elemento, lo selecciona automáticamente
    // y el resto de los datos ya ingresados permanece intacto.
    @Test
    fun `confirmar el alta inline de un Activo lo crea lo selecciona automaticamente y conserva el resto del formulario`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onDescriptionChange("Entré por ruptura de rango")
        viewModel.onStartAddCatalogItem(OperationFormState.FIELD_ASSET)
        viewModel.onAddCatalogTextChange("EURJPY")

        viewModel.onConfirmAddCatalogItem()

        val nuevoActivo = catalogItemDao.itemsOfType(CatalogType.ASSET).single { it.name == "EURJPY" }
        assertEquals(nuevoActivo.id, viewModel.state.value.assetId)
        assertNull(viewModel.state.value.addingCatalogField)
        assertEquals("", viewModel.state.value.addCatalogText)
        assertNull(viewModel.state.value.addCatalogError)
        assertEquals("Entré por ruptura de rango", viewModel.state.value.entryDescription)
        assertEquals(emotionId, viewModel.state.value.emotionBeforeId)
    }

    // HU-013 Escenario 3: nombre duplicado -- el campo inline permanece abierto con el mensaje de
    // HU-010 (misma regla de unicidad, no se revalida aquí), sin crear un duplicado ni tocar el
    // resto del formulario.
    @Test
    fun `confirmar el alta inline con nombre duplicado mantiene el campo abierto con el mensaje de error`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onStartAddCatalogItem(OperationFormState.FIELD_ASSET)
        viewModel.onAddCatalogTextChange("XAUUSD")

        viewModel.onConfirmAddCatalogItem()

        assertEquals(OperationFormState.FIELD_ASSET, viewModel.state.value.addingCatalogField)
        assertEquals(
            "Este valor ya existe en el catálogo",
            viewModel.state.value.addCatalogError
        )
        assertEquals(1, catalogItemDao.itemsOfType(CatalogType.ASSET).size)
        assertEquals(assetId, viewModel.state.value.assetId)
    }

    // HU-013 Escenario 4: cancelar el campo inline sin crear ningún elemento.
    @Test
    fun `cancelar el alta inline cierra el campo sin crear ningun elemento`() = runTest {
        catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val viewModel = createViewModel()
        viewModel.onStartAddCatalogItem(OperationFormState.FIELD_ASSET)
        viewModel.onAddCatalogTextChange("EURJPY")

        viewModel.onCancelAddCatalogItem()

        assertNull(viewModel.state.value.addingCatalogField)
        assertEquals("", viewModel.state.value.addCatalogText)
        assertNull(viewModel.state.value.addCatalogError)
        assertEquals(1, catalogItemDao.itemsOfType(CatalogType.ASSET).size)
    }

    // Nota técnica de HU-013: aplica a los 3 tipos de catálogo según cuál selector disparó la
    // acción -- Emoción antes/después comparten CatalogType.EMOTION pero son campos distintos del
    // formulario, y solo el campo que inició el alta debe verse afectado por la selección.
    @Test
    fun `el alta inline de Emocion despues usa el tipo EMOTION y solo selecciona ese campo`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionBeforeId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionBeforeId, emotionBeforeId, errorId)
        viewModel.onStartAddCatalogItem(OperationFormState.FIELD_EMOTION_AFTER)
        viewModel.onAddCatalogTextChange("Euforia")

        viewModel.onConfirmAddCatalogItem()

        val nuevaEmocion = catalogItemDao.itemsOfType(CatalogType.EMOTION).single { it.name == "Euforia" }
        assertEquals(nuevaEmocion.id, viewModel.state.value.emotionAfterId)
        assertEquals(emotionBeforeId, viewModel.state.value.emotionBeforeId)
    }

    @Test
    fun `el alta inline de Error usa el tipo ERROR`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val viewModel = createViewModel()
        viewModel.fillMinimalRequiredFields(assetId, emotionId, emotionId, errorId)
        viewModel.onStartAddCatalogItem(OperationFormState.FIELD_ERROR)
        viewModel.onAddCatalogTextChange("Sobreapalancamiento")

        viewModel.onConfirmAddCatalogItem()

        val nuevoError = catalogItemDao.itemsOfType(CatalogType.ERROR).single { it.name == "Sobreapalancamiento" }
        assertEquals(nuevoError.id, viewModel.state.value.errorId)
    }
}
