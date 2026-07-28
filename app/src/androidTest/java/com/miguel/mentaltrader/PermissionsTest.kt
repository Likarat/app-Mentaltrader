package com.miguel.mentaltrader

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

// HU-035, Escenario "Sin dependencia HTTP ni permiso de red en el proyecto".
@RunWith(AndroidJUnit4::class)
class PermissionsTest {

    @Test
    fun laAppNoDeclaraElPermisoDeInternet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permisos = packageInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(
            "La app no debe declarar android.permission.INTERNET (offline-first, spec §1/§9)",
            permisos.contains(android.Manifest.permission.INTERNET)
        )
    }
}
