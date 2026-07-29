package com.miguel.mentaltrader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.model.CatalogType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifica el RoomDatabase.Callback.onCreate REAL (AppDatabase.kt, mismo código que corre en
 * producción vía MentaltraderApplication.database) sobre la base de datos real del dispositivo:
 * siembra XAUUSD (ASSET), "Ninguno" (ERROR) y las 8 emociones semilla la primera vez que se crea
 * la base de datos. Cierra INT-catalog-seed-entities/INT-catalog-seed-emotion.
 *
 * Para verificar genuinamente el "arranque limpio" (design.md, riesgo #1: siembra en una
 * corrutina separada del hilo principal de Room), este test se ejecutó tras
 * `adb shell pm clear com.miguel.mentaltrader` — ver progress_log de build-state.json para el
 * comando exacto y su evidencia. Si se re-ejecuta sin limpiar datos, sigue siendo válido: la
 * siembra es idempotente por catálogo (solo ocurre en onCreate, no se duplica en accesos
 * posteriores) siempre que ningún otro flujo de la app inserte manualmente elementos de
 * catálogo (no existe tal flujo todavía, se sembrará en EP-002).
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseSeedTest {

    @Test
    fun laBaseDeDatosRealSiembraLosCatalogosEnElPrimerArranque() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as MentaltraderApplication
        val catalogItemDao = app.database.catalogItemDao()

        waitUntil(timeoutMs = 10_000) {
            runBlocking { catalogItemDao.countByType(CatalogType.EMOTION) } >= CatalogItem.SEED_EMOTIONS.size
        }

        val assets = runBlocking { catalogItemDao.getByType(CatalogType.ASSET).first() }
        val emotions = runBlocking { catalogItemDao.getByType(CatalogType.EMOTION).first() }
        val errors = runBlocking { catalogItemDao.getByType(CatalogType.ERROR).first() }

        assertEquals(1, assets.size)
        assertEquals(CatalogItem.SEED_ASSET_XAUUSD, assets.first().name)
        assertTrue(assets.first().isDefault)

        assertEquals(CatalogItem.SEED_EMOTIONS.size, emotions.size)
        assertEquals(CatalogItem.SEED_EMOTIONS.toSet(), emotions.map { it.name }.toSet())
        assertTrue(emotions.all { it.isDefault })

        assertEquals(1, errors.size)
        assertEquals(CatalogItem.SEED_ERROR_NINGUNO, errors.first().name)
        assertTrue(errors.first().isDefault)
    }

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            check(System.currentTimeMillis() - start <= timeoutMs) { "Timeout esperando la siembra de catálogos" }
            Thread.sleep(50)
        }
    }
}
