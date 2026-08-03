package com.miguel.mentaltrader.feature.historial

/**
 * HU-019: lógica pura (sin Compose/Pager real) de navegación entre las imágenes de una operación
 * en el visor a pantalla completa -- mismo patrón que [HistorialSeparators]/[HistorialGroupCollapse]
 * (funciones sin dependencias de plataforma, testeadas en JVM). El zoom en sí (Escenario 2) depende
 * de gestos reales de `detectTransformGestures` y no se fuerza a un test JVM artificial (tasks.md 7.6).
 */
object HistorialVisorImagenNavigation {

    /**
     * HU-019 Escenario 1/3: resuelve la página inicial real del `HorizontalPager` a partir del
     * índice pedido por el llamador (la imagen que el usuario tocó en `HistorialDetalleScreen`).
     * Se acota (`coerceIn`) al rango válido `[0, imageCount - 1]` en vez de crashear si el índice
     * pedido queda fuera de rango (edge real: la lista de imágenes cambió entre que se armó la
     * navegación y que el visor terminó de cargar, p. ej. una imagen borrada por otra sesión).
     * Sin ninguna imagen, no hay página válida (`-1`) -- el llamador no debe montar el `Pager` en
     * ese caso; en producción real esto no ocurre porque el visor solo se abre desde una imagen ya
     * visible en el detalle (HU-018 Escenario 3 ya garantiza que la sección de imagen se omite por
     * completo sin imágenes).
     */
    fun resolveInitialPage(imageCount: Int, requestedIndex: Int): Int {
        if (imageCount <= 0) return -1
        return requestedIndex.coerceIn(0, imageCount - 1)
    }

    /**
     * HU-019 Escenario 3: la flecha "avanzar" (además del gesto de deslizar) solo tiene sentido si
     * existe una página siguiente real.
     */
    fun hasNextPage(currentPage: Int, imageCount: Int): Boolean = currentPage < imageCount - 1

    /**
     * Complemento simétrico de [hasNextPage] -- no exigido literalmente por ningún AC de HU-019
     * (el deslizar hacia atrás ya lo resuelve el propio `HorizontalPager`), pero necesario para
     * poder decidir si mostrar también una flecha "retroceder" sin dejarla habilitada en la
     * primera imagen.
     */
    fun hasPreviousPage(currentPage: Int): Boolean = currentPage > 0
}
