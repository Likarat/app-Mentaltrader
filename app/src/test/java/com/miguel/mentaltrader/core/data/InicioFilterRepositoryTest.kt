package com.miguel.mentaltrader.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests unitarios de [InicioFilterRepository] (tasks.md 4.5, sub-slice EP-004-d, HU-030): usa un
 * DataStore Preferences REAL (`PreferenceDataStoreFactory.create`) sobre un archivo temporal
 * ([TemporaryFolder] de JUnit), no un fake en memoria -- así guardar/leer ejercita la serialización
 * real a disco, no solo un mapa en RAM. Mismo patrón exacto que `HistorialFilterRepositoryTest`
 * (HU-024/EP-003), incluyendo los gotchas de entorno Windows documentados ahí (`freshFile`, no
 * `tempFolder.newFile`).
 *
 * El Escenario 1/2 de HU-030 ("persistencia real entre reinicios completos, no solo en memoria de
 * sesión") se verifica a nivel JVM creando una instancia NUEVA de [InicioFilterRepository] (con su
 * propio [androidx.datastore.core.DataStore] nuevo, en su propio `CoroutineScope` cancelado tras
 * usarlo) apuntando al MISMO archivo en disco después de guardar, simulando que el proceso terminó
 * y volvió a arrancar. Un `connectedDebugAndroidTest` matando el proceso real de verdad queda
 * diferido a la pasada final única de instrumentados (sin dispositivo conectado en esta sesión).
 */
class InicioFilterRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Ver HistorialFilterRepositoryTest: NO usar `tempFolder.newFile(...)` (JUnit lo pre-crea vacío
    // en disco y el rename atómico interno de DataStore falla en Windows sobre un archivo existente).
    private fun freshFile(name: String): File = File(tempFolder.newFolder(), name)

    private fun repositoryOver(file: File, scope: CoroutineScope) =
        InicioFilterRepository(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))

    // HU-030 Escenario 3: primera vez, sin ningún filtro guardado previamente -- snapshot por defecto.
    @Test
    fun `sin ningun filtro guardado previamente load devuelve el snapshot por defecto (Escenario 3)`() = runTest {
        val file = freshFile("inicio_filter_default.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        val snapshot = repository.load()

        assertEquals(InicioFilterSnapshot(), snapshot)
        scope.cancel()
    }

    @Test
    fun `guardar y leer periodName persiste su valor real`() = runTest {
        val file = freshFile("inicio_filter_period.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(InicioFilterSnapshot(periodName = "SEMANA"))

        assertEquals("SEMANA", repository.load().periodName)
        scope.cancel()
    }

    // HU-030 Escenario 2: rango personalizado persistido con sus fechas exactas.
    @Test
    fun `guardar y leer customRangeStart y customRangeEnd persisten su valor real (Escenario 2)`() = runTest {
        val file = freshFile("inicio_filter_custom_range.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(InicioFilterSnapshot(periodName = "PERSONALIZADO", customRangeStart = 1000L, customRangeEnd = 2000L))

        val loaded = repository.load()
        assertEquals("PERSONALIZADO", loaded.periodName)
        assertEquals(1000L, loaded.customRangeStart)
        assertEquals(2000L, loaded.customRangeEnd)
        scope.cancel()
    }

    // Guardar un snapshot nuevo reemplaza por completo el anterior (no acumula campos viejos) --
    // mismo gotcha de entorno Windows que HistorialFilterRepositoryTest (segunda escritura sobre el
    // MISMO archivo ya existente: se borra entre ambas escrituras, ver esa clase para el detalle).
    @Test
    fun `guardar un nuevo snapshot reemplaza por completo el anterior, sin dejar campos viejos`() = runTest {
        val file = freshFile("inicio_filter_replace.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)
        repository.save(InicioFilterSnapshot(periodName = "PERSONALIZADO", customRangeStart = 1L, customRangeEnd = 2L))
        file.delete()

        repository.save(InicioFilterSnapshot(periodName = "MES"))

        val result = repository.load()
        assertNull(result.customRangeStart)
        assertNull(result.customRangeEnd)
        assertEquals("MES", result.periodName)
        scope.cancel()
    }

    // HU-030 Escenario 1/2 a nivel JVM: guardar -> crear una instancia NUEVA del repository (nuevo
    // DataStore, propio scope, mismo archivo en disco) simula un reinicio completo del proceso, no
    // solo releer del mismo objeto en memoria.
    @Test
    fun `un ciclo completo guardar y crear una instancia nueva del repository recupera el mismo estado (Escenario 1-2)`() = runTest {
        val file = freshFile("inicio_filter_restart.preferences_pb")
        val scope1 = newScope()
        val original = repositoryOver(file, scope1)
        val snapshot = InicioFilterSnapshot(periodName = "PERSONALIZADO", customRangeStart = 5000L, customRangeEnd = 9000L)
        original.save(snapshot)
        scope1.cancel()

        val scope2 = newScope()
        val afterRestart = repositoryOver(file, scope2)

        assertEquals(snapshot, afterRestart.load())
        scope2.cancel()
    }
}
