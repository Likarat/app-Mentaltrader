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
 * Verifica `CatalogSeeder.ensureSeeded()` REAL (core/data/CatalogSeeder.kt, disparado desde
 * `MentaltraderApplication.onCreate()`, mismo código que corre en producción) sobre la base de
 * datos real del dispositivo: siembra XAUUSD (ASSET), "Ninguno" (ERROR) y las 8 emociones semilla
 * la primera vez que se crea la base de datos. Cierra INT-catalog-seed-entities/
 * INT-catalog-seed-emotion.
 *
 * Nota histórica: la siembra vivió antes en un `RoomDatabase.Callback.onCreate`, pero un bug
 * real detectado en la primera corrida instrumentada (2026-07-29) mostró que ese mecanismo no
 * era confiable tras una migración destructiva (bump de versión al agregar `OperationImage`) —
 * se reemplazó por `CatalogSeeder.ensureSeeded()`, idempotente y disparado explícitamente desde
 * `Application.onCreate()`, sin depender de en qué punto del ciclo de vida de Room se dispare
 * `onCreate`. Ver `CatalogSeeder.kt` para el detalle.
 *
 * Para verificar genuinamente el "arranque limpio", este test se ejecutó tras
 * `adb shell pm clear com.miguel.mentaltrader` — ver progress_log de build-state.json para el
 * comando exacto y su evidencia. Si se re-ejecuta sin limpiar datos, sigue siendo válido: la
 * siembra es idempotente por catálogo (chequea count==0 antes de insertar, no se duplica en
 * accesos posteriores) siempre que ningún otro flujo de la app inserte manualmente elementos de
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

        // Bug real reportado por el usuario (2026-08-04): este test asumía un catálogo con
        // EXACTAMENTE los elementos semilla (arranque limpio), pero -- tal como esta misma clase
        // ya anticipaba en su KDoc ("siempre que ningún otro flujo de la app inserte manualmente
        // elementos de catálogo... se sembrará en EP-002") -- EP-002 ya existe y el usuario ya
        // agregó elementos reales de catálogo probando la app en este mismo dispositivo. Se
        // relaja la aserción a "la semilla existe y está marcada isDefault", sin asumir que sea
        // el ÚNICO elemento -- eso es lo que realmente verifica que la siembra ocurrió.
        val seedAsset = assets.singleOrNull { it.name == CatalogItem.SEED_ASSET_XAUUSD }
        assertTrue("Falta la semilla de Activo (${CatalogItem.SEED_ASSET_XAUUSD})", seedAsset != null)
        assertTrue(seedAsset!!.isDefault)

        val seedEmotionNames = emotions.filter { it.isDefault }.map { it.name }.toSet()
        assertEquals(CatalogItem.SEED_EMOTIONS.toSet(), seedEmotionNames)

        val seedError = errors.singleOrNull { it.name == CatalogItem.SEED_ERROR_NINGUNO }
        assertTrue("Falta la semilla de Error (${CatalogItem.SEED_ERROR_NINGUNO})", seedError != null)
        assertTrue(seedError!!.isDefault)
    }

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            check(System.currentTimeMillis() - start <= timeoutMs) { "Timeout esperando la siembra de catálogos" }
            Thread.sleep(50)
        }
    }
}
