package com.miguel.mentaltrader.feature.inicio

/**
 * HU-031 (sub-slice EP-004-c): qué estado vacío mostrar en Inicio, si alguno. Lógica PURA (sin
 * Room/Compose) para poder testearla en JVM, mismo patrón que
 * `com.miguel.mentaltrader.feature.historial.HistorialEmptyState` (HU-025/EP-003).
 *
 * - [PRIMERA_VEZ]: el usuario nunca registró ninguna operación ([totalOperationCount] == 0). HU-031
 *   Escenario 1 declara explícitamente que este caso REUTILIZA el mismo mensaje y acceso directo ya
 *   definidos en HU-025, sin redefinirlos aquí -- `InicioScreen` invoca directamente
 *   `HistorialEmptyStateContent(HistorialEmptyState.PRIMERA_VEZ, ...)` cuando este valor resuelve,
 *   en vez de duplicar ese mensaje/lógica (design.md decisión #6).
 * - [SIN_DATOS_PERIODO]: sí hay operaciones registradas ([totalOperationCount] > 0), pero ninguna
 *   cae dentro del periodo actualmente seleccionado ([operationCountInPeriod] == 0) -- caso PROPIO
 *   de esta historia (no existe en Historial, que no tiene concepto de "periodo" sin filtro
 *   explícito). Mensaje distinto, SIN acceso directo de creación (las operaciones sí existen, solo
 *   no en este rango, HU-031 Escenario 2).
 *
 * `null` cuando hay al menos una operación dentro del periodo seleccionado -- el panel de
 * métricas/gráficas/ranking se muestra normalmente (HU-031 Escenario 3: recuperación fluida al
 * cambiar a un periodo con datos, sin rastros del estado vacío anterior).
 */
enum class InicioEmptyState {
    PRIMERA_VEZ,
    SIN_DATOS_PERIODO;

    companion object {
        fun resolve(totalOperationCount: Int, operationCountInPeriod: Int): InicioEmptyState? = when {
            operationCountInPeriod > 0 -> null
            totalOperationCount <= 0 -> PRIMERA_VEZ
            else -> SIN_DATOS_PERIODO
        }
    }
}
