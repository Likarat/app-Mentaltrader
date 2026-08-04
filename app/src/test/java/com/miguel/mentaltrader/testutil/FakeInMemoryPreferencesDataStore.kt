package com.miguel.mentaltrader.testutil

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake EN MEMORIA del `DataStore<Preferences>` subyacente (sin tocar el filesystem) -- mismo
 * contrato mínimo (2 miembros: `data` + `updateData`) que usan los tests de ViewModel que inyectan
 * un repositorio de filtro REAL (`HistorialFilterRepository`/`InicioFilterRepository`, no fakeados
 * ellos mismos) sin ejercitar la persistencia a disco en sí -- eso lo cubren aparte
 * `HistorialFilterRepositoryTest`/`InicioFilterRepositoryTest` con un DataStore real sobre archivo
 * temporal. Compartido en `testutil` (a diferencia de la copia privada histórica de
 * `HistorialViewModelTest`, anterior a este `testutil/` compartido) para no duplicar esta clase por
 * cada feature nueva que también persista un filtro (ver `InicioViewModelTest`, HU-030).
 */
class FakeInMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
