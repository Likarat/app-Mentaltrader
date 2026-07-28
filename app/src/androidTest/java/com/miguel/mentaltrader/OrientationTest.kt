package com.miguel.mentaltrader

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// HU-034, Escenarios "Orientación vertical en el uso general" y "Rechazo de forzado horizontal":
// el manifiesto fija screenOrientation="portrait" en la única Activity del shell.
@RunWith(AndroidJUnit4::class)
class OrientationTest {

    @Test
    fun laActividadPrincipalEstaFijadaEnVertical() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activityInfo = context.packageManager.getActivityInfo(
            android.content.ComponentName(context, MainActivity::class.java),
            PackageManager.GET_ACTIVITIES
        )
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityInfo.screenOrientation)
    }
}
