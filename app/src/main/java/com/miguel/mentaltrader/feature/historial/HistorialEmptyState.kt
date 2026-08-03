package com.miguel.mentaltrader.feature.historial

/**
 * HU-025: qué estado vacío mostrar en Historial, si alguno. Lógica PURA (sin Room/Paging/Compose)
 * para poder testearla en JVM sin `paging-testing`, mismo patrón ya usado en
 * [HistorialSeparators]/[HistorialGroupCollapse]/[HistorialFilterMatcher]. Distingue los DOS
 * mensajes que HU-025 exige tratar de forma distinta (Escenario 1 vs Escenario 2), en vez del
 * único mensaje genérico que mostraba `HistorialScreen` antes de este sub-slice:
 *
 * - [PRIMERA_VEZ]: el usuario nunca registró ninguna operación ([totalOperationCount] == 0) --
 *   invita a crear la primera, con acceso directo al mismo destino del FAB de HU-008 (HU-025
 *   Escenario 1/3).
 * - [SIN_RESULTADOS_FILTRO]: sí hay operaciones registradas, pero el filtro/búsqueda activo
 *   (HU-022/HU-023) no arroja ninguna coincidencia -- SIN el acceso directo de creación (las
 *   operaciones sí existen, solo están filtradas), invita a limpiar el filtro (HU-025 Escenario 2).
 *
 * `null` (ningún estado vacío) cuando el listado tiene items para mostrar, o cuando el listado
 * está vacío sin ningún filtro activo pese a haber operaciones registradas -- edge real detectado
 * durante este sub-slice (no un escenario explícito de HU-025): la ventana de "Deshacer" de HU-021
 * puede dejar el listado momentáneamente sin filas visibles (`pendingDeleteIds` oculta la última
 * fila pendiente de borrado de forma optimista en la UI, sin tocar Room todavía) sin que haya
 * ningún filtro que invitar a limpiar; se prefiere no mostrar ningún mensaje en ese caso transitorio
 * a mostrar uno potencialmente engañoso.
 */
enum class HistorialEmptyState {
    PRIMERA_VEZ,
    SIN_RESULTADOS_FILTRO;

    companion object {
        fun resolve(totalOperationCount: Int, itemCount: Int, filterActive: Boolean): HistorialEmptyState? = when {
            itemCount > 0 -> null
            totalOperationCount <= 0 -> PRIMERA_VEZ
            filterActive -> SIN_RESULTADOS_FILTRO
            else -> null
        }
    }
}
