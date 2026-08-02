package com.miguel.mentaltrader.core.image

import java.io.File

/**
 * HU-014: agrega el espacio en disco ocupado por rutas relativas ya guardadas por [ImageProcessor]
 * (imagen completa + miniatura). Cálculo aproximado (spec: "espacio total aproximado"): una ruta
 * cuyo archivo ya no existe (ej. tras eliminar la operación que la usaba) se ignora en silencio,
 * sin crashear -- no cuenta para el total.
 */
object DiskSpaceCalculator {
    fun totalBytes(baseDir: File, relativePaths: List<String>): Long =
        relativePaths.sumOf { path ->
            val file = File(baseDir, path)
            if (file.exists()) file.length() else 0L
        }
}
