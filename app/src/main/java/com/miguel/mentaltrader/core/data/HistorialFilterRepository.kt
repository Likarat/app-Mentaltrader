package com.miguel.mentaltrader.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miguel.mentaltrader.core.model.ResultType
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** DataStore Preferences de un único `Context` (Application, vía [MentaltraderApplication])
 * dedicado a HU-024 -- distinto del que use cualquier otra preferencia futura de la app. */
private val Context.historialFilterDataStore: DataStore<Preferences> by preferencesDataStore(name = "historial_filter")

/**
 * HU-024: snapshot neutral (sin depender de ningún tipo de `feature.historial`, p.ej.
 * `HistorialFilterState`/`PeriodoFiltro`) del filtro/búsqueda de Historial persistido en disco --
 * `HistorialViewModel` lo traduce hacia/desde su propio `HistorialFilterState`
 * ([com.miguel.mentaltrader.feature.historial] es una capa por encima de `core.data`, así que este
 * snapshot solo usa tipos de `core.model`/primitivos). [result] es [ResultType] (`core.model`,
 * compartido con [OperationDao]).
 *
 * [periodName] guarda el `name` textual del `PeriodoFiltro` elegido (o `null` si no hay filtro de
 * fecha activo). [customRangeStart]/[customRangeEnd] SOLO tienen sentido cuando [periodName] es
 * `"PERSONALIZADO"` -- un periodo predefinido (p.ej. "Última semana") se recalcula relativo al
 * "hoy" real en el momento de restaurar (rolling window), no se restaura con una fecha vieja
 * congelada de la sesión anterior.
 */
data class HistorialFilterSnapshot(
    val periodName: String? = null,
    val customRangeStart: Long? = null,
    val customRangeEnd: Long? = null,
    val searchText: String? = null,
    val assetId: Long? = null,
    val result: ResultType? = null,
    val errorId: Long? = null,
    val emotionBeforeId: Long? = null,
    val emotionAfterId: Long? = null
)

/**
 * HU-024: persiste el último [HistorialFilterSnapshot] aplicado en Historial (DataStore
 * Preferences, design.md decisión #6), para restaurarlo automáticamente al reabrir la app
 * (Escenario 1) incluso tras un cierre completo del proceso (Escenario 2 -- la persistencia es en
 * disco, no en memoria de sesión). Sin ningún filtro guardado previamente, [load] devuelve
 * `HistorialFilterSnapshot()` (todos los campos por defecto -- Escenario 3).
 *
 * Claves ya fijadas por la spec (ver Contexto de HU-024 y design.md decisión #6):
 * `lastPeriodFilter`, `lastCustomRangeStart`/`lastCustomRangeEnd`, `lastSearchText`,
 * `lastSelectedAssets`, `lastSelectedResults`, `lastSelectedErrors`, `lastSelectedEmotionBefore`,
 * `lastSelectedEmotionAfter`. El modelo actual de filtro (`HistorialFilterState`, HU-022/HU-023)
 * solo admite UN id por etiqueta (`assetId`/`errorId`/`emotionBeforeId`/`emotionAfterId`: `Long?`,
 * `result`: `ResultType?` -- no listas), así que las claves plurales `lastSelectedAssets`/etc. se
 * guardan igual como CSV (nunca más de un elemento hoy) para respetar el formato de la spec sin
 * inventar selección múltiple (fuera del alcance de HU-022/HU-023/HU-024).
 *
 * Constructor primario recibe el [DataStore] ya construido (no un `Context`) para ser testeable en
 * JVM puro con `PreferenceDataStoreFactory.create` sobre un archivo temporal (ver
 * `HistorialFilterRepositoryTest`, sin Robolectric/emulador); el constructor secundario que sí
 * recibe un `Context` es el que usa producción (vía `MentaltraderApplication`), delegando en el
 * DataStore singleton de [historialFilterDataStore].
 */
class HistorialFilterRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.historialFilterDataStore)

    /** Último snapshot persistido, reactivo a cada `save` -- `HistorialFilterSnapshot()` (todos los
     * campos por defecto) si nunca se guardó ninguno (Escenario 3) o si la lectura falla (p.ej.
     * archivo corrupto: no debe crashear Historial, mismo criterio defensivo recomendado por
     * DataStore para `IOException` en `data`). */
    val snapshot: Flow<HistorialFilterSnapshot> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toSnapshot() }

    suspend fun load(): HistorialFilterSnapshot = snapshot.first()

    /** Reemplaza POR COMPLETO el snapshot guardado (limpia antes de escribir, HU-022 Escenario 4
     * "Limpiar filtros" también debe persistirse como "sin ningún filtro" -- no debe quedar ningún
     * campo viejo de una combinación anterior de criterios). */
    suspend fun save(snapshot: HistorialFilterSnapshot) {
        dataStore.edit { preferences ->
            preferences.clear()
            snapshot.periodName?.let { preferences[KEY_LAST_PERIOD_FILTER] = it }
            snapshot.customRangeStart?.let { preferences[KEY_LAST_CUSTOM_RANGE_START] = it }
            snapshot.customRangeEnd?.let { preferences[KEY_LAST_CUSTOM_RANGE_END] = it }
            snapshot.searchText?.let { preferences[KEY_LAST_SEARCH_TEXT] = it }
            preferences[KEY_LAST_SELECTED_ASSETS] = csvOf(snapshot.assetId?.toString())
            preferences[KEY_LAST_SELECTED_RESULTS] = csvOf(snapshot.result?.name)
            preferences[KEY_LAST_SELECTED_ERRORS] = csvOf(snapshot.errorId?.toString())
            preferences[KEY_LAST_SELECTED_EMOTION_BEFORE] = csvOf(snapshot.emotionBeforeId?.toString())
            preferences[KEY_LAST_SELECTED_EMOTION_AFTER] = csvOf(snapshot.emotionAfterId?.toString())
        }
    }

    private fun Preferences.toSnapshot(): HistorialFilterSnapshot = HistorialFilterSnapshot(
        periodName = this[KEY_LAST_PERIOD_FILTER],
        customRangeStart = this[KEY_LAST_CUSTOM_RANGE_START],
        customRangeEnd = this[KEY_LAST_CUSTOM_RANGE_END],
        searchText = this[KEY_LAST_SEARCH_TEXT],
        assetId = firstCsvLong(this[KEY_LAST_SELECTED_ASSETS]),
        result = firstCsvResultType(this[KEY_LAST_SELECTED_RESULTS]),
        errorId = firstCsvLong(this[KEY_LAST_SELECTED_ERRORS]),
        emotionBeforeId = firstCsvLong(this[KEY_LAST_SELECTED_EMOTION_BEFORE]),
        emotionAfterId = firstCsvLong(this[KEY_LAST_SELECTED_EMOTION_AFTER])
    )

    private fun csvOf(value: String?): String = value ?: ""

    private fun firstCsvLong(csv: String?): Long? =
        csv?.split(",")?.firstOrNull { it.isNotBlank() }?.toLongOrNull()

    private fun firstCsvResultType(csv: String?): ResultType? =
        csv?.split(",")?.firstOrNull { it.isNotBlank() }?.let { name ->
            ResultType.entries.firstOrNull { it.name == name }
        }

    companion object {
        private val KEY_LAST_PERIOD_FILTER = stringPreferencesKey("lastPeriodFilter")
        private val KEY_LAST_CUSTOM_RANGE_START = longPreferencesKey("lastCustomRangeStart")
        private val KEY_LAST_CUSTOM_RANGE_END = longPreferencesKey("lastCustomRangeEnd")
        private val KEY_LAST_SEARCH_TEXT = stringPreferencesKey("lastSearchText")
        private val KEY_LAST_SELECTED_ASSETS = stringPreferencesKey("lastSelectedAssets")
        private val KEY_LAST_SELECTED_RESULTS = stringPreferencesKey("lastSelectedResults")
        private val KEY_LAST_SELECTED_ERRORS = stringPreferencesKey("lastSelectedErrors")
        private val KEY_LAST_SELECTED_EMOTION_BEFORE = stringPreferencesKey("lastSelectedEmotionBefore")
        private val KEY_LAST_SELECTED_EMOTION_AFTER = stringPreferencesKey("lastSelectedEmotionAfter")
    }
}
