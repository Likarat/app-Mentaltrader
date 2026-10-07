package com.miguel.mentaltrader.feature.inicio

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.AppDatabase
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HU-038 Escenario 1/2/3 (tasks.md 1.5-bis, nuevo slice post-construccion): verifica
 * `OperationDao.metricsSummary()` CONTRA SQLITE REAL (Room en memoria, NO la base de datos
 * persistente del dispositivo -- mismo patron de aislamiento que `OperationFormRenderedUiTest`,
 * cero riesgo sobre datos reales de produccion). `FakeOperationDao.metricsSummary` lanza
 * `UnsupportedOperationException` (requiere agregacion SQL real) -- esta es la verificacion que
 * faltaba, diferida aqui porque necesita Room real, no derivable en JVM puro.
 */
@RunWith(AndroidJUnit4::class)
class OperationMetricsCountsInstrumentedTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun operacion(result: ResultType, dateTime: Long) = Operation(
        dateTime = dateTime,
        assetId = 1L,
        direction = Direction.BUY,
        quality = 8f,
        emotionBeforeId = 1L,
        emotionAfterId = 1L,
        errorId = 1L,
        result = result,
        resultInR = 1f,
        entryDescription = "test",
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun winCountLossCountBreakEvenCountSumanOperationCountYSonConsistentesConLosPorcentajes() = runBlocking {
        val dao = database.operationDao()
        // 9 operaciones: 6 WIN, 2 LOSS, 1 BREAK_EVEN -- porcentajes redondeados ya conocidos
        // (67%/22%/11%), el conteo absoluto NO debe derivarse de ellos (perderia precision).
        repeat(6) { dao.insert(operacion(ResultType.WIN, dateTime = it.toLong())) }
        repeat(2) { dao.insert(operacion(ResultType.LOSS, dateTime = 100L + it)) }
        dao.insert(operacion(ResultType.BREAK_EVEN, dateTime = 200L))

        val summary = dao.metricsSummary(dateFrom = null, dateTo = null).first()

        assertEquals(9, summary.operationCount)
        assertEquals(6, summary.winCount)
        assertEquals(2, summary.lossCount)
        assertEquals(1, summary.breakEvenCount)
        assertEquals(summary.operationCount, summary.winCount + summary.lossCount + summary.breakEvenCount)
        assertEquals(67, summary.winPercent)
        assertEquals(22, summary.lossPercent)
        assertEquals(11, summary.breakEvenPercent)
    }

    // HU-038 Escenario 3 (edge): periodo sin operaciones -- los 3 conteos en 0, sin errores.
    @Test
    fun sinOperacionesEnElPeriodoLosTresConteosQuedanEnCeroSinErrores() = runBlocking {
        val dao = database.operationDao()
        dao.insert(operacion(ResultType.WIN, dateTime = 1_000L))

        // Rango de fechas que no contiene ninguna operacion insertada.
        val summary = dao.metricsSummary(dateFrom = 50_000L, dateTo = 60_000L).first()

        assertEquals(0, summary.operationCount)
        assertEquals(0, summary.winCount)
        assertEquals(0, summary.lossCount)
        assertEquals(0, summary.breakEvenCount)
    }

    @Test
    fun conSoloOperacionesGanadasElConteoDeLasOtrasCategoriasEsCeroNoNull() = runBlocking {
        val dao = database.operationDao()
        repeat(3) { dao.insert(operacion(ResultType.WIN, dateTime = it.toLong())) }

        val summary = dao.metricsSummary(dateFrom = null, dateTo = null).first()

        assertEquals(3, summary.winCount)
        assertEquals(0, summary.lossCount)
        assertEquals(0, summary.breakEvenCount)
    }
}
