package com.miguel.mentaltrader.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.miguel.mentaltrader.core.model.ResultType
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
 * Tests unitarios de [HistorialFilterRepository] (tasks.md 8.3, sub-slice EP-003-h, HU-024): usa
 * un DataStore Preferences REAL (`PreferenceDataStoreFactory.create`) sobre un archivo temporal
 * ([TemporaryFolder] de JUnit, mismo patrón que el resto de tests de esta rama que necesitan
 * filesystem real), no un fake en memoria -- así guardar/leer ejercita la serialización real a
 * disco, no solo un mapa en RAM.
 *
 * El Escenario 2 de HU-024 ("persistencia real entre reinicios completos, no solo en memoria de
 * sesión") se verifica a nivel JVM creando una instancia NUEVA de [HistorialFilterRepository] (con
 * su propio [androidx.datastore.core.DataStore] nuevo, en su propio `CoroutineScope` cancelado tras
 * usarlo -- para no chocar con el registro interno de "archivo activo" de DataStore) apuntando al
 * MISMO archivo en disco después de guardar, simulando que el proceso terminó y volvió a arrancar.
 * Un `connectedDebugAndroidTest` matando el proceso real de verdad (no solo backgrounding) queda
 * diferido a la pasada final única de instrumentados (tasks.md 8.4, sin dispositivo conectado en
 * esta sesión).
 */
class HistorialFilterRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // NO usar `tempFolder.newFile(...)`: JUnit lo pre-crea vacío en disco, y el rename atómico
    // interno de DataStore sobre un archivo YA existente falla en Windows (NIO ATOMIC_MOVE +
    // REPLACE_EXISTING no soportado ahí), reportado con el mensaje engañoso "multiple instances of
    // DataStore for this file" -- gotcha de entorno Windows, no un bug real de HistorialFilterRepository
    // (confirmado corriendo la suite real: los 11 tests fallaban con `java.io.IOException: Unable
    // to rename ... .tmp to ...` hasta cambiar a una ruta que DataStore cree él mismo desde cero).
    private fun freshFile(name: String): File = File(tempFolder.newFolder(), name)

    private fun repositoryOver(file: File, scope: CoroutineScope) =
        HistorialFilterRepository(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))

    // HU-024 Escenario 3: primera vez, sin ningún filtro guardado previamente -- estado por defecto.
    @Test
    fun `sin ningun filtro guardado previamente load devuelve el snapshot por defecto (Escenario 3)`() = runTest {
        val file = freshFile("historial_filter_default.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        val snapshot = repository.load()

        assertEquals(HistorialFilterSnapshot(), snapshot)
        scope.cancel()
    }

    @Test
    fun `guardar y leer periodName persiste su valor real`() = runTest {
        val file = freshFile("historial_filter_period.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(HistorialFilterSnapshot(periodName = "ULTIMA_SEMANA"))

        assertEquals("ULTIMA_SEMANA", repository.load().periodName)
        scope.cancel()
    }

    @Test
    fun `guardar y leer customRangeStart y customRangeEnd persisten su valor real`() = runTest {
        val file = freshFile("historial_filter_custom_range.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(HistorialFilterSnapshot(periodName = "PERSONALIZADO", customRangeStart = 1000L, customRangeEnd = 2000L))

        val loaded = repository.load()
        assertEquals(1000L, loaded.customRangeStart)
        assertEquals(2000L, loaded.customRangeEnd)
        scope.cancel()
    }

    @Test
    fun `guardar y leer searchText persiste su valor real`() = runTest {
        val file = freshFile("historial_filter_search.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(HistorialFilterSnapshot(searchText = "ruptura"))

        assertEquals("ruptura", repository.load().searchText)
        scope.cancel()
    }

    @Test
    fun `guardar y leer assetId persiste su valor real`() = runTest {
        val file = freshFile("historial_filter_asset.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(HistorialFilterSnapshot(assetId = 10L))

        assertEquals(10L, repository.load().assetId)
        scope.cancel()
    }

    @Test
    fun `guardar y leer result persiste su valor real`() = runTest {
        val file = freshFile("historial_filter_result.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(HistorialFilterSnapshot(result = ResultType.LOSS))

        assertEquals(ResultType.LOSS, repository.load().result)
        scope.cancel()
    }

    @Test
    fun `guardar y leer errorId persiste su valor real`() = runTest {
        val file = freshFile("historial_filter_error.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(HistorialFilterSnapshot(errorId = 20L))

        assertEquals(20L, repository.load().errorId)
        scope.cancel()
    }

    @Test
    fun `guardar y leer emotionBeforeId persiste su valor real`() = runTest {
        val file = freshFile("historial_filter_emotion_before.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(HistorialFilterSnapshot(emotionBeforeId = 30L))

        assertEquals(30L, repository.load().emotionBeforeId)
        scope.cancel()
    }

    @Test
    fun `guardar y leer emotionAfterId persiste su valor real`() = runTest {
        val file = freshFile("historial_filter_emotion_after.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)

        repository.save(HistorialFilterSnapshot(emotionAfterId = 40L))

        assertEquals(40L, repository.load().emotionAfterId)
        scope.cancel()
    }

    // Todas las claves a la vez, en un único snapshot -- combinación real (no solo aisladas de a una).
    @Test
    fun `guardar y leer un snapshot con todas las claves a la vez persiste cada una`() = runTest {
        val file = freshFile("historial_filter_all.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)
        val snapshot = HistorialFilterSnapshot(
            periodName = "PERSONALIZADO",
            customRangeStart = 1000L,
            customRangeEnd = 2000L,
            searchText = "ruptura",
            assetId = 10L,
            result = ResultType.WIN,
            errorId = 20L,
            emotionBeforeId = 30L,
            emotionAfterId = 40L
        )

        repository.save(snapshot)

        assertEquals(snapshot, repository.load())
        scope.cancel()
    }

    // Guardar un snapshot nuevo reemplaza por completo el anterior (no acumula campos viejos).
    //
    // Nota de entorno (Windows, JVM unit test -- NO afecta el comportamiento real en Android):
    // `androidx.datastore:datastore-preferences-core:1.2.1` renombra su archivo `.tmp` sobre el
    // destino final con `File.renameTo()`, cuyo comportamiento es explícitamente
    // platform-dependent (javadoc de `java.io.File`); en Windows (a diferencia de Linux/Android)
    // `renameTo()` devuelve `false` cuando el destino YA EXISTE, y DataStore lo traduce en el
    // mismo `IOException` genérico ("multiple instances of DataStore for this file") que ya se
    // vio con `TemporaryFolder.newFile()`. Confirmado corriendo la suite real: la primera escritura
    // a un archivo NUEVO siempre funciona (destino no existe todavía), pero una SEGUNDA escritura
    // sobre el MISMO archivo ya existente falla determinísticamente en esta máquina. Como este test
    // necesita dos escrituras reales para verificar "reemplaza sin dejar campos viejos", se borra
    // el archivo entre ambas (`file.delete()`) -- el `DataStore` no depende de releer el archivo
    // para escribir (ya tiene el valor previo en memoria), así que el segundo `save()` solo
    // necesita crear el archivo de nuevo (destino ausente -> `renameTo()` funciona). En Android
    // real (filesystem Linux) esta limitación no existe: `renameTo()` sí sobrescribe destinos
    // existentes, tal como usa `HistorialViewModel` en producción con cada cambio de filtro.
    @Test
    fun `guardar un nuevo snapshot reemplaza por completo el anterior, sin dejar campos viejos`() = runTest {
        val file = freshFile("historial_filter_replace.preferences_pb")
        val scope = newScope()
        val repository = repositoryOver(file, scope)
        repository.save(HistorialFilterSnapshot(assetId = 1L, searchText = "vieja"))
        file.delete()

        repository.save(HistorialFilterSnapshot(errorId = 2L))

        val result = repository.load()
        assertNull(result.assetId)
        assertNull(result.searchText)
        assertEquals(2L, result.errorId)
        scope.cancel()
    }

    // HU-024 Escenario 2 a nivel JVM: guardar -> crear una instancia NUEVA del repository (nuevo
    // DataStore, propio scope, mismo archivo en disco) simula un reinicio completo del proceso, no
    // solo releer del mismo objeto en memoria.
    @Test
    fun `un ciclo completo guardar y crear una instancia nueva del repository recupera el mismo estado (Escenario 2)`() = runTest {
        val file = freshFile("historial_filter_restart.preferences_pb")
        val scope1 = newScope()
        val original = repositoryOver(file, scope1)
        val snapshot = HistorialFilterSnapshot(assetId = 99L, searchText = "xauusd", periodName = "ULTIMA_SEMANA")
        original.save(snapshot)
        scope1.cancel()

        val scope2 = newScope()
        val afterRestart = repositoryOver(file, scope2)

        assertEquals(snapshot, afterRestart.load())
        scope2.cancel()
    }
}
