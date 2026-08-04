package com.miguel.mentaltrader.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** DataStore Preferences de un único `Context` (Application, vía [MentaltraderApplication])
 * dedicado a HU-030 -- ARCHIVO propio ("inicio_filter"), distinto del de
 * [HistorialFilterRepository] ("historial_filter", HU-024): son filtros conceptualmente
 * independientes (design.md decisión #7/#8 del change `metricas-deteccion-patrones-inicio`), no
 * comparten estado ni archivo ni claves. */
private val Context.inicioFilterDataStore: DataStore<Preferences> by preferencesDataStore(name = "inicio_filter")

/**
 * HU-030: snapshot neutral (sin depender de ningún tipo de `feature.inicio`, p.ej.
 * `InicioFilterState`/`PeriodoInicioFiltro`) del último filtro de periodo de Inicio persistido en
 * disco -- `InicioViewModel` lo traduce hacia/desde su propio `InicioFilterState`
 * ([com.miguel.mentaltrader.feature.inicio] es una capa por encima de `core.data`, mismo criterio
 * que [HistorialFilterSnapshot]).
 *
 * [periodName] guarda el `name` textual del `PeriodoInicioFiltro` elegido (o `null` si nunca se
 * guardó ninguno, o si el usuario deseleccionó el periodo la última vez). [customRangeStart]/
 * [customRangeEnd] SOLO tienen sentido cuando [periodName] es `"PERSONALIZADO"` -- un periodo
 * predefinido (p.ej. `"SEMANA"`) se recalcula relativo al "hoy" real en el momento de restaurar
 * (rolling/calendario), no se restaura con una fecha vieja congelada de la sesión anterior (mismo
 * criterio que [HistorialFilterSnapshot]).
 */
data class InicioFilterSnapshot(
    val periodName: String? = null,
    val customRangeStart: Long? = null,
    val customRangeEnd: Long? = null
)

/**
 * HU-030: persiste el último [InicioFilterSnapshot] aplicado en Inicio (DataStore Preferences,
 * mismo patrón mecánico que [HistorialFilterRepository]/HU-024, con su PROPIO archivo y claves --
 * design.md decisión #8 del change `metricas-deteccion-patrones-inicio`), para restaurarlo
 * automáticamente al reabrir la app (HU-030 Escenario 1/2) incluso tras un cierre completo del
 * proceso (la persistencia es en disco, no en memoria de sesión). Sin ningún filtro guardado
 * previamente, [load] devuelve `InicioFilterSnapshot()` (todos los campos en `null`) --
 * `InicioFilterState.fromSnapshot` lo traduce al periodo por defecto razonable de Inicio (HU-030
 * Escenario 3, ver su KDoc).
 *
 * Constructor primario recibe el [DataStore] ya construido (no un `Context`) para ser testeable en
 * JVM puro con `PreferenceDataStoreFactory.create` sobre un archivo temporal (ver
 * `InicioFilterRepositoryTest`, sin Robolectric/emulador); el constructor secundario que sí recibe
 * un `Context` es el que usa producción (vía `MentaltraderApplication`), delegando en el DataStore
 * singleton de [inicioFilterDataStore].
 */
class InicioFilterRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.inicioFilterDataStore)

    /** Último snapshot persistido, reactivo a cada `save` -- `InicioFilterSnapshot()` (todos los
     * campos en `null`) si nunca se guardó ninguno o si la lectura falla (p.ej. archivo corrupto:
     * no debe crashear Inicio, mismo criterio defensivo que [HistorialFilterRepository]). */
    val snapshot: Flow<InicioFilterSnapshot> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toSnapshot() }

    suspend fun load(): InicioFilterSnapshot = snapshot.first()

    /** Reemplaza POR COMPLETO el snapshot guardado (limpia antes de escribir) -- deseleccionar el
     * periodo también debe persistirse como "sin periodo", sin dejar un campo viejo de una
     * selección anterior, mismo criterio que [HistorialFilterRepository.save]. */
    suspend fun save(snapshot: InicioFilterSnapshot) {
        dataStore.edit { preferences ->
            preferences.clear()
            snapshot.periodName?.let { preferences[KEY_LAST_PERIOD_FILTER] = it }
            snapshot.customRangeStart?.let { preferences[KEY_LAST_CUSTOM_RANGE_START] = it }
            snapshot.customRangeEnd?.let { preferences[KEY_LAST_CUSTOM_RANGE_END] = it }
        }
    }

    private fun Preferences.toSnapshot(): InicioFilterSnapshot = InicioFilterSnapshot(
        periodName = this[KEY_LAST_PERIOD_FILTER],
        customRangeStart = this[KEY_LAST_CUSTOM_RANGE_START],
        customRangeEnd = this[KEY_LAST_CUSTOM_RANGE_END]
    )

    companion object {
        private val KEY_LAST_PERIOD_FILTER = stringPreferencesKey("lastInicioPeriodFilter")
        private val KEY_LAST_CUSTOM_RANGE_START = longPreferencesKey("lastInicioCustomRangeStart")
        private val KEY_LAST_CUSTOM_RANGE_END = longPreferencesKey("lastInicioCustomRangeEnd")
    }
}
