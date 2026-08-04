package com.miguel.mentaltrader.feature.inicio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests unitarios de [InicioEmptyState] (tasks.md 3.5, sub-slice EP-004-c, HU-031): lógica PURA
 * (sin Room/Compose) que decide cuál de los DOS estados vacíos de Inicio mostrar -- o ninguno --,
 * mismo criterio que `HistorialEmptyStateTest` (HU-025/EP-003).
 */
class InicioEmptyStateTest {

    // HU-031 Escenario 1 (happy path): nunca hubo ninguna operación registrada -- PRIMERA_VEZ,
    // reutilizado tal cual desde HU-025 (ver InicioScreen, no se redefine el mensaje aquí).
    @Test
    fun `sin ninguna operacion registrada resuelve PRIMERA_VEZ`() {
        assertEquals(
            InicioEmptyState.PRIMERA_VEZ,
            InicioEmptyState.resolve(totalOperationCount = 0, operationCountInPeriod = 0)
        )
    }

    // HU-031 Escenario 2 (edge): hay operaciones registradas, pero ninguna cae en el periodo
    // actualmente seleccionado -- mensaje PROPIO de Inicio, distinto de PRIMERA_VEZ.
    @Test
    fun `con operaciones registradas pero ninguna en el periodo seleccionado resuelve SIN_DATOS_PERIODO`() {
        assertEquals(
            InicioEmptyState.SIN_DATOS_PERIODO,
            InicioEmptyState.resolve(totalOperationCount = 5, operationCountInPeriod = 0)
        )
    }

    // HU-031 Escenario 3 (edge, recuperación): con al menos una operación dentro del periodo
    // seleccionado, ningún estado vacío aplica -- el panel de métricas se muestra normalmente.
    @Test
    fun `con al menos una operacion en el periodo seleccionado no resuelve ningun estado vacio`() {
        assertNull(InicioEmptyState.resolve(totalOperationCount = 5, operationCountInPeriod = 3))
        assertNull(InicioEmptyState.resolve(totalOperationCount = 1, operationCountInPeriod = 1))
    }

    // Edge de precedencia: si nunca hubo operaciones, PRIMERA_VEZ tiene prioridad sin importar el
    // conteo del periodo (en la práctica operationCountInPeriod también sería 0 en ese caso, pero
    // la precedencia queda definida sin ambigüedad, mismo criterio que `HistorialEmptyStateTest`).
    @Test
    fun `sin ninguna operacion registrada PRIMERA_VEZ tiene precedencia`() {
        assertEquals(
            InicioEmptyState.PRIMERA_VEZ,
            InicioEmptyState.resolve(totalOperationCount = 0, operationCountInPeriod = 0)
        )
    }
}
